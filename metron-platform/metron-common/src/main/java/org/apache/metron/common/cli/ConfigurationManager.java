/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.common.cli;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.io.Files;
import org.apache.commons.cli.*;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.ConfigurationType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;

public class ConfigurationManager {
  public enum ConfigurationOptions {
    HELP("h", s -> new Option(s, "help", false, "Generate Help screen"))
   ,INPUT("i", s -> OptionBuilder.isRequired(false)
                                 .withLongOpt("input_dir")
                                 .hasArg()
                                 .withArgName("DIR")
                                 .withDescription("The input directory containing the configuration files named like \"$source.json\"")
                                 .create(s)
         )
    ,OUTPUT("o", s -> OptionBuilder.isRequired(false)
                                 .hasArg()
                                 .withLongOpt("output_dir")
                                 .withArgName("DIR")
                                 .withDescription("The output directory which will store the JSON configuration from Zookeeper")
                                 .create(s)
         )
    ,ZK_QUORUM("z", s -> OptionBuilder.isRequired(true)
                                 .hasArg()
                                 .withLongOpt("zk_quorum")
                                 .withArgName("host:port,[host:port]*")
                                 .withDescription("Zookeeper Quorum URL (zk1:port,zk2:port,...)")
                                 .create(s)
         )
    ,MODE("m", s -> OptionBuilder.isRequired(true)
                                 .hasArg()
                                 .withLongOpt("mode")
                                 .withArgName("MODE")
                                 .withDescription("The mode of operation: DUMP, PULL, PUSH")
                                 .create(s)
         )
    ,FORCE("f", s -> new Option(s, "force", false, "Force operation"))
    ;
    Option option;
    String shortCode;
    ConfigurationOptions(String shortCode, Function<String, Option> optionHandler) {
      this.shortCode = shortCode;
      this.option = optionHandler.apply(shortCode);
    }

    public boolean has(CommandLine cli) {
      return cli.hasOption(shortCode);
    }

    public String get(CommandLine cli) {
      return cli.getOptionValue(shortCode);
    }

    public static CommandLine parse(CommandLineParser parser, String[] args) {
      try {
        CommandLine cli = parser.parse(getOptions(), args);
        if(ConfigurationOptions.HELP.has(cli)) {
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
      formatter.printHelp( "configuration_manager", getOptions());
    }

    public static Options getOptions() {
      Options ret = new Options();
      for(ConfigurationOptions o : ConfigurationOptions.values()) {
        ret.addOption(o.option);
      }
      return ret;
    }
  }

  public void dump(CuratorFramework client) throws Exception {
    ConfigurationsUtils.dumpConfigs(System.out, client);
  }



  public void pull(CuratorFramework client, String outFileStr, final boolean force) throws Exception {
    final File outputDir = new File(outFileStr);
    if (!outputDir.exists()) {
      if (!outputDir.mkdirs()) {
        throw new IllegalStateException("Unable to make directories: " + outputDir.getAbsolutePath());
      }
    }

    ConfigurationsUtils.visitConfigs(client, new ConfigurationsUtils.ConfigurationVisitor() {
      @Override
      public void visit(ConfigurationType configurationType, String name, String data) {
        File out = getFile(outputDir, configurationType, name);
        if (!out.exists() || force) {
          if(!out.exists()) {
            out.getParentFile().mkdirs();
          }
          try {
            Files.write(data, out, Charset.defaultCharset());
          } catch (IOException e) {
            throw new RuntimeException("Sorry, something bad happened writing the config to " + out.getAbsolutePath() + ": " + e.getMessage(), e);
          }
        }
        else if(out.exists() && !force) {
          throw new IllegalStateException("Unable to overwrite existing file (" + out.getAbsolutePath() + ") without the force flag (-f or --force) being set.");
        }
      }
    });
  }

  public void push(String inputDirStr, CuratorFramework client) throws Exception {
      final File inputDir = new File(inputDirStr);

      if(!inputDir.exists() || !inputDir.isDirectory()) {
        throw new IllegalStateException("Input directory: " + inputDir + " does not exist or is not a directory.");
      }
      ConfigurationsUtils.uploadConfigsToZookeeper(inputDirStr, client);
  }

  public void run(CommandLine cli) throws Exception {
    try(CuratorFramework client = ConfigurationsUtils.getClient(ConfigurationOptions.ZK_QUORUM.get(cli))) {
      client.start();
      run(client, cli);
    }
  }
  public void run(CuratorFramework client, CommandLine cli) throws Exception {
    final boolean force = ConfigurationOptions.FORCE.has(cli);
    String mode = ConfigurationOptions.MODE.get(cli);

    if (mode.toLowerCase().equals("push")) {
      String inputDirStr = ConfigurationOptions.INPUT.get(cli);
      push(inputDirStr, client);
    }
    else {

      switch (mode.toLowerCase()) {

        case "dump":
          dump(client);
          break;

        case "pull":
          pull(client, ConfigurationOptions.OUTPUT.get(cli), force);
          break;

        default:
          throw new IllegalStateException("Invalid mode: " + mode + " expected DUMP, PULL or PUSH");
      }
    }
  }

  private static File getFile(File baseDir, ConfigurationType configurationType, String name) {
    return new File(new File(baseDir, configurationType.getDirectory()), name + ".json");
  }

  public static void main(String... argv) throws Exception {
    CommandLineParser parser = new PosixParser();
    CommandLine cli = ConfigurationOptions.parse(parser, argv);
    ConfigurationManager manager = new ConfigurationManager();
    manager.run(cli);
  }
}
