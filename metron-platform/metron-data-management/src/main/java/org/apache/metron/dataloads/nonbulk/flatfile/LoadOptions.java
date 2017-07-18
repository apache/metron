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
package org.apache.metron.dataloads.nonbulk.flatfile;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.common.utils.cli.OptionHandler;
import org.apache.metron.dataloads.nonbulk.flatfile.importer.ImportStrategy;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

public enum LoadOptions {
  HELP("h", new OptionHandler<LoadOptions>() {

    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      return new Option(s, "help", false, "Generate Help screen");
    }
  })
  ,QUIET("q", new OptionHandler<LoadOptions>() {

    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      return new Option(s, "quiet", false, "Do not update progress");
    }

    @Override
    public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
      return Optional.of(option.has(cli));
    }
  })
  , IMPORT_MODE("m", new OptionHandler<LoadOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "import_mode", true
                           , "The Import mode to use: " + Joiner.on(",").join(ImportStrategy.values())
                           + ".  Default: " + ImportStrategy.LOCAL
                           );
      o.setArgName("MODE");
      o.setRequired(false);
      return o;
    }

    @Override
    public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
      String mode = option.get(cli);
      return Optional.of(ImportStrategy.getStrategy(mode).orElse(ImportStrategy.LOCAL));
    }
  })
  ,HBASE_TABLE("t", new OptionHandler<LoadOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "hbase_table", true, "HBase table to ingest the data into.");
      o.setArgName("TABLE");
      o.setRequired(true);
      return o;
    }

    @Override
    public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
      return Optional.ofNullable(option.get(cli).trim());
    }
  })
  ,HBASE_CF("c", new OptionHandler<LoadOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "hbase_cf", true, "HBase column family to ingest the data into.");
      o.setArgName("CF");
      o.setRequired(true);
      return o;
    }

    @Override
    public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
      return Optional.ofNullable(option.get(cli).trim());
    }
  })
  ,EXTRACTOR_CONFIG("e", new OptionHandler<LoadOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "extractor_config", true, "JSON Document describing the extractor for this input data source");
      o.setArgName("JSON_FILE");
      o.setRequired(true);
      return o;
    }

    @Override
    public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
      try {
        return Optional.ofNullable(FileUtils.readFileToString(new File(option.get(cli).trim())));
      } catch (IOException e) {
        throw new IllegalStateException("Unable to retrieve extractor config from " + option.get(cli) + ": " + e.getMessage(), e);
      }
    }
  })
  ,ENRICHMENT_CONFIG("n", new OptionHandler<LoadOptions>() {
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
  ,LOG4J_PROPERTIES("l", new OptionHandler<LoadOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "log4j", true, "The log4j properties file to load");
      o.setArgName("FILE");
      o.setRequired(false);
      return o;
    }
  })
  ,NUM_THREADS("p", new OptionHandler<LoadOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "threads", true, "The number of threads to use when extracting data.  The default is the number of cores of your machine.");
      o.setArgName("NUM_THREADS");
      o.setRequired(false);
      return o;
    }

    @Override
    public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
      int numThreads = Runtime.getRuntime().availableProcessors();
      if(option.has(cli)) {
        numThreads = ConversionUtils.convert(option.get(cli), Integer.class);
      }
      return Optional.of(numThreads);
    }
  })
  ,BATCH_SIZE("b", new OptionHandler<LoadOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "batchSize", true, "The batch size to use for HBase puts");
      o.setArgName("SIZE");
      o.setRequired(false);
      return o;
    }

    @Override
    public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
      int batchSize = 128;
      if(option.has(cli)) {
        batchSize = ConversionUtils.convert(option.get(cli), Integer.class);
      }
      return Optional.of(batchSize);
    }
  })
  ,INPUT("i", new OptionHandler<LoadOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "input", true, "The CSV File to load");
      o.setArgName("FILE");
      o.setRequired(true);
      return o;
    }

    @Override
    public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
      List<String> inputs = new ArrayList<>();
      for(String input : Splitter.on(",").split(Optional.ofNullable(option.get(cli)).orElse(""))) {
        inputs.add(input.trim());
      }
      return Optional.of(inputs);
    }
  })
  ;
  Option option;
  String shortCode;
  OptionHandler<LoadOptions> handler;
  LoadOptions(String shortCode, OptionHandler<LoadOptions> optionHandler) {
    this.shortCode = shortCode;
    this.handler = optionHandler;
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
      if(HELP.has(cli)) {
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

  public static EnumMap<LoadOptions, Optional<Object> > createConfig(CommandLine cli) {
    EnumMap<LoadOptions, Optional<Object> > ret = new EnumMap<>(LoadOptions.class);
    for(LoadOptions option : values()) {
      ret.put(option, option.handler.getValue(option, cli));
    }
    return ret;
  }

  public static void printHelp() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp( "SimpleEnrichmentFlatFileLoader", getOptions());
  }

  public static Options getOptions() {
    Options ret = new Options();
    for(LoadOptions o : LoadOptions.values()) {
      ret.addOption(o.option);
    }
    return ret;
  }
}
