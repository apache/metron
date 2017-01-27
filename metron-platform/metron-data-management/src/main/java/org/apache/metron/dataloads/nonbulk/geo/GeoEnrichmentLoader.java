/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.dataloads.nonbulk.geo;


import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.enrichment.adapters.geo.GeoLiteDatabase;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

public class GeoEnrichmentLoader {
  private static abstract class OptionHandler implements Function<String, Option> {
  }

  public enum GeoEnrichmentOptions {
    HELP("h", new GeoEnrichmentLoader.OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        return new Option(s, "help", false, "Generate Help screen");
      }
    }), GEO_URL("g", new GeoEnrichmentLoader.OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "geo_url", true, "GeoIP URL - defaults to http://geolite.maxmind.com/download/geoip/database/GeoLite2-City.mmdb.gz");
        o.setArgName("GEO_URL");
        o.setRequired(false);
        return o;
      }
    }), REMOTE_DIR("r", new GeoEnrichmentLoader.OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "remote_dir", true, "HDFS directory to land formatted GeoIP file - defaults to /apps/metron/geo/<epoch millis>/");
        o.setArgName("REMOTE_DIR");
        o.setRequired(false);
        return o;
      }
    }), TMP_DIR("t", new GeoEnrichmentLoader.OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "tmp_dir", true, "Directory for landing the temporary GeoIP data - defaults to /tmp");
        o.setArgName("TMP_DIR");
        o.setRequired(false);
        return o;
      }
    }), ZK_QUORUM("z", new GeoEnrichmentLoader.OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "zk_quorum", true, "Zookeeper Quorum URL (zk1:port,zk2:port,...)");
        o.setArgName("ZK_QUORUM");
        o.setRequired(true);
        return o;
      }
    });
    Option option;
    String shortCode;

    GeoEnrichmentOptions(String shortCode, GeoEnrichmentLoader.OptionHandler optionHandler) {
      this.shortCode = shortCode;
      this.option = optionHandler.apply(shortCode);
    }

    public boolean has(CommandLine cli) {
      return cli.hasOption(shortCode);
    }

    public String get(CommandLine cli) {
      return cli.getOptionValue(shortCode);
    }

    public String get(CommandLine cli, String defaultValue) {
      return cli.getOptionValue(shortCode, defaultValue);
    }

    public static CommandLine parse(CommandLineParser parser, String[] args) {
      try {
        CommandLine cli = parser.parse(getOptions(), args);

        if (GeoEnrichmentLoader.GeoEnrichmentOptions.HELP.has(cli)) {
          printHelp();
          System.exit(0);
        }
        return cli;
      } catch (ParseException e) {
        System.err.println("Unable to parse args: " + Joiner.on(' ').join(args));
        e.printStackTrace(System.err);
        printHelp();
        System.exit(-1);
        return null;
      }
    }

    public static void printHelp() {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("GeoEnrichmentLoader", getOptions());
    }

    public static Options getOptions() {
      Options ret = new Options();
      for (GeoEnrichmentLoader.GeoEnrichmentOptions o : GeoEnrichmentLoader.GeoEnrichmentOptions.values()) {
        ret.addOption(o.option);
      }
      return ret;
    }
  }

  protected void loadGeoIpDatabase(CommandLine cli) throws IOException {
    // Retrieve the database file
    System.out.println("Retrieving GeoLite2 archive");
    String url = GeoEnrichmentOptions.GEO_URL.get(cli, "http://geolite.maxmind.com/download/geoip/database/GeoLite2-City.mmdb.gz");
    String tmpDir = GeoEnrichmentOptions.TMP_DIR.get(cli, "/tmp") + "/"; // Make sure there's a file separator at the end
    File localGeoFile = downloadGeoFile(url, tmpDir);
    // Want to delete the tar in event of failure
    localGeoFile.deleteOnExit();
    System.out.println("GeoIP files downloaded successfully");

    // Push the file to HDFS and update Configs to ensure clients get new view
    String zookeeper = GeoEnrichmentOptions.ZK_QUORUM.get(cli);
    long millis = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();
    String hdfsLoc = GeoEnrichmentOptions.REMOTE_DIR.get(cli, "/apps/metron/geo/" + millis);
    System.out.println("Putting GeoLite2 file into HDFS at: " + hdfsLoc);

    // Put into HDFS
    Path srcPath = new Path(localGeoFile.getAbsolutePath());
    Path dstPath = new Path(hdfsLoc);
    putDbFile(srcPath, dstPath);
    pushConfig(srcPath, dstPath, zookeeper);

    System.out.println("GeoLite2 file placement complete");
    System.out.println("Successfully created and updated new GeoIP information");
  }

  protected File downloadGeoFile(String urlStr, String tmpDir) {
    File localFile = null;
    try {
      URL url = new URL(urlStr);
      localFile = new File(tmpDir + new File(url.getPath()).getName());

      System.out.println("Downloading " + url.toString() + " to " + localFile.getAbsolutePath());
      if (localFile.exists() && !localFile.delete()) {
        System.err.println("File already exists locally and can't be deleted.  Please delete before continuing");
        System.exit(3);
      }
      FileUtils.copyURLToFile(url, localFile, 5000, 10000);
    } catch (MalformedURLException e) {
      System.err.println("Malformed URL - aborting: " + e);
      e.printStackTrace();
      System.exit(4);
    } catch (IOException e) {
      System.err.println("Unable to copy remote GeoIP database to local file: " + e);
      e.printStackTrace();
      System.exit(5);
    }
    return localFile;
  }

  protected void pushConfig(Path srcPath, Path dstPath, String zookeeper) {
    System.out.println("Beginning update of global configs");
    try (CuratorFramework client = ConfigurationsUtils.getClient(zookeeper)) {
      client.start();
      // Use the parent and place a new file.  Has to be a new file so we can update the configs and trigger updates.
      // Fetch the global configuration
      Map<String, Object> global = JSONUtils.INSTANCE.load(
              new ByteArrayInputStream(ConfigurationsUtils.readGlobalConfigBytesFromZookeeper(client)),
              new TypeReference<Map<String, Object>>() {
              });

      // Update the global config and push it back
      global.put(GeoLiteDatabase.GEO_HDFS_FILE, dstPath.toString() + "/" + srcPath.getName());
      ConfigurationsUtils.writeGlobalConfigToZookeeper(global, client);
    } catch (Exception e) {
      System.err.println("Unable to load new GeoIP info into HDFS: " + e);
      e.printStackTrace();
      System.exit(2);
    }
    System.out.println("Finished update of global configs");
  }

  protected void putDbFile(Path src, Path dst) throws IOException {
    Configuration conf = new Configuration();
    FileSystem fileSystem = FileSystem.get(conf);
    System.out.println("Putting: " + src + " onto HDFS at: " + dst);
    fileSystem.mkdirs(dst);
    fileSystem.copyFromLocalFile(true, true, src, dst);
    System.out.println("Done putting GeoLite file into HDFS");
  }

  public static void main(String... argv) throws IOException {
    String[] otherArgs = new GenericOptionsParser(argv).getRemainingArgs();
    CommandLine cli = GeoEnrichmentOptions.parse(new PosixParser(), otherArgs);

    GeoEnrichmentLoader loader = new GeoEnrichmentLoader();
    loader.loadGeoIpDatabase(cli);
  }
}
