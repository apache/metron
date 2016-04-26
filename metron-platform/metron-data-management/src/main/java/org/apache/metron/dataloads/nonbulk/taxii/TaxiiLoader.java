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

package org.apache.metron.dataloads.nonbulk.taxii;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import org.apache.commons.cli.*;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.log4j.PropertyConfigurator;
import org.apache.metron.dataloads.extractor.Extractor;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.dataloads.extractor.stix.StixExtractor;
import org.apache.metron.common.configuration.EnrichmentConfig;
import org.apache.metron.common.utils.JSONUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.text.*;
import java.util.Date;
import java.util.Timer;

public class TaxiiLoader {
  private static abstract class OptionHandler implements Function<String, Option> {}
  private enum TaxiiOptions {
    HELP("h", new OptionHandler() {

      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        return new Option(s, "help", false, "Generate Help screen");
      }
    })
    ,EXTRACTOR_CONFIG("e", new OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "extractor_config", true, "JSON Document describing the extractor for this input data source");
        o.setArgName("JSON_FILE");
        o.setRequired(true);
        return o;
      }
    })
    ,CONNECTION_CONFIG("c", new OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "taxii_connection_config", true, "The JSON config file to configure the connection");
        o.setArgName("config_file");
        o.setRequired(true);
        return o;
      }
    })
    ,TIME_BETWEEN_POLLS("p", new OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "time_between_polls", true, "The time between polls (in ms)");
        o.setArgName("MS");
        o.setRequired(false);
        return o;
      }
    })
    ,BEGIN_TIME("b", new OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "begin_time", true, "Start time to poll the Taxii server (all data from that point will be gathered in the first pull).");
        o.setArgName(DATE_FORMAT.toPattern());
        o.setRequired(false);
        return o;
      }
    })
    ,LOG4J_PROPERTIES("l", new OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "log4j", true, "The log4j properties file to load");
        o.setArgName("FILE");
        o.setRequired(false);
        return o;
      }
    })
    ,ENRICHMENT_CONFIG("n", new OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "enrichment_config", true
                , "JSON Document describing the enrichment configuration details." +
                "  This is used to associate an enrichment type with a field type in zookeeper."
        );
        o.setArgName("JSON_FILE");
        o.setRequired(false);
        return o;
      }
    })
    ;
    Option option;
    String shortCode;
    TaxiiOptions(String shortCode, OptionHandler optionHandler) {
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
        if(TaxiiOptions.HELP.has(cli)) {
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
      formatter.printHelp( "TaxiiLoader", getOptions());
    }

    public static Options getOptions() {
      Options ret = new Options();
      for(TaxiiOptions o : TaxiiOptions.values()) {
        ret.addOption(o.option);
      }
      return ret;
    }
  }
  public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  public static final long ONE_HR_IN_MS = 60*60*1000;
  public static final long DEFAULT_TIME_BETWEEN_POLLS = ONE_HR_IN_MS;


  public static void main(String... argv) throws Exception {
    Configuration conf = HBaseConfiguration.create();
    String zkQuorum = conf.get(HConstants.ZOOKEEPER_QUORUM);
    String[] otherArgs = new GenericOptionsParser(conf, argv).getRemainingArgs();

    CommandLine cli = TaxiiOptions.parse(new PosixParser(), otherArgs);
    if(TaxiiOptions.LOG4J_PROPERTIES.has(cli)) {
      PropertyConfigurator.configure(TaxiiOptions.LOG4J_PROPERTIES.get(cli));
    }
    ExtractorHandler handler = ExtractorHandler.load(FileUtils.readFileToString(new File(TaxiiOptions.EXTRACTOR_CONFIG.get(cli))));
    Extractor e = handler.getExtractor();
    EnrichmentConfig enrichmentConfig = null;
    if(TaxiiOptions.ENRICHMENT_CONFIG.has(cli)) {
      enrichmentConfig = JSONUtils.INSTANCE.load( new File(TaxiiOptions.ENRICHMENT_CONFIG.get(cli))
              , EnrichmentConfig.class
      );
      enrichmentConfig.updateSensorConfigs();
    }

    Timer timer = new Timer();
    if(e instanceof StixExtractor) {
      StixExtractor extractor = (StixExtractor)e;
      TaxiiConnectionConfig connectionConfig = TaxiiConnectionConfig.load(FileUtils.readFileToString(new File(TaxiiOptions.CONNECTION_CONFIG.get(cli))));
      if(TaxiiOptions.BEGIN_TIME.has(cli)) {
        Date d = DATE_FORMAT.parse(TaxiiOptions.BEGIN_TIME.get(cli));
        connectionConfig.withBeginTime(d);
      }
      long timeBetween = DEFAULT_TIME_BETWEEN_POLLS;
      if(TaxiiOptions.TIME_BETWEEN_POLLS.has(cli)) {
        timeBetween = Long.parseLong(TaxiiOptions.TIME_BETWEEN_POLLS.get(cli));
      }
      timer.scheduleAtFixedRate(new TaxiiHandler(connectionConfig, extractor, conf), 0, timeBetween);
    }
    else {
      throw new IllegalStateException("Extractor must be a STIX Extractor");
    }
  }
}
