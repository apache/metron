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
package org.apache.metron.performance.load;

import com.google.common.base.Joiner;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.apache.commons.cli.*;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.common.utils.cli.CLIOptions;
import org.apache.metron.performance.sampler.BiasedSampler;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.common.utils.cli.OptionHandler;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

public enum LoadOptions implements CLIOptions<LoadOptions> {
  HELP(new OptionHandler<LoadOptions>() {

    @Override
    public String getShortCode() {
      return "h";
    }

    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      return new Option(s, "help", false, "Generate Help screen");
    }
  }),
  ZK(new OptionHandler<LoadOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "zk_quorum", true, "zookeeper quorum");
      o.setArgName("QUORUM");
      o.setRequired(false);
      return o;
    }

    @Override
    public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
      if(option.has(cli)) {
        return Optional.ofNullable(option.get(cli));
      }
      else {
        return Optional.empty();
      }
    }

    @Override
    public String getShortCode() {
      return "z";
    }
  }),
  CONSUMER_GROUP(new OptionHandler<LoadOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "consumer_group", true, "Consumer Group.  The default is " + LoadGenerator.CONSUMER_GROUP);
      o.setArgName("GROUP_ID");
      o.setRequired(false);
      return o;
    }

    @Override
    public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
      if(option.has(cli)) {
        return Optional.ofNullable(option.get(cli));
      }
      else {
        return Optional.of(LoadGenerator.CONSUMER_GROUP);
      }
    }

    @Override
    public String getShortCode() {
      return "cg";
    }
  }),
  BIASED_SAMPLE(new OptionHandler<LoadOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "sample_bias", true, "The discrete distribution to bias the sampling. " +
              "This is a CSV of 2 columns.  The first column is the % of the templates " +
              "and the 2nd column is the probability (0-100) that it's chosen.  For instance:\n" +
              "  20,80\n" +
              "  80,20\n" +
              "implies that 20% of the templates will comprise 80% of the output and the remaining 80% of the templates will comprise 20% of the output.");
      o.setArgName("BIAS_FILE");
      o.setRequired(false);
      return o;
    }

    @Override
    public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
      if(!option.has(cli)) {
        return Optional.empty();
      }
      File discreteDistributionFile  = new File(option.get(cli));
      if(discreteDistributionFile.exists()) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(discreteDistributionFile),
            StandardCharsets.UTF_8))){
          return Optional.ofNullable(BiasedSampler.readDistribution(br));
        } catch (IOException e) {
          throw new IllegalStateException("Unable to read distribution file: " + option.get(cli), e);
        }
      }
      else {
        throw new IllegalStateException("Unable to read distribution file: " + option.get(cli) + " file doesn't exist.");
      }
    }

    @Override
    public String getShortCode() {
      return "bs";
    }
  })
  ,CSV(new OptionHandler<LoadOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "csv", true, "A CSV file to emit monitoring data to.  " +
              "The format is a CSV with the following schema: timestamp, (name, eps, historical_mean, historical_stddev)+");
      o.setArgName("CSV_FILE");
      o.setRequired(false);
      return o;
    }

    @Override
    public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
      if(!option.has(cli)) {
        return Optional.empty();
      }
      return Optional.of(new File(option.get(cli)));
    }

    @Override
    public String getShortCode() {
      return "c";
    }
  })
  ,TEMPLATE(new OptionHandler<LoadOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "template", true, "The template file to use for generation.  This should be a file with a template per line with $METRON_TS and $METRON_GUID in the spots for timestamp and guid, if you so desire them.");
      o.setArgName("TEMPLATE_FILE");
      o.setRequired(false);
      return o;
    }

    @Override
    public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
      if(!option.has(cli)) {
        return Optional.empty();
      }
      File templateFile = new File(option.get(cli));
      if(templateFile.exists()) {
        List<String> templates = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(templateFile), StandardCharsets.UTF_8))) {
          for(String line = null;(line = br.readLine()) != null;) {
            templates.add(line);
          }
          return Optional.of(templates);
        } catch (IOException e) {
          throw new IllegalStateException("Unable to read template file: " + option.get(cli), e);
        }
      }
      else {
        throw new IllegalStateException("Unable to read template file: " + option.get(cli) + " file doesn't exist.");
      }
    }

    @Override
    public String getShortCode() {
      return "t";
    }
  })
  ,SUMMARY_LOOKBACK(new OptionHandler<LoadOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "lookback", true, "When summarizing, how many monitoring periods should we summarize over?  If 0, then no summary.  Default: 5");
      o.setArgName("LOOKBACK");
      o.setRequired(false);
      return o;
    }

    @Override
    public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
      if(option.has(cli)) {
        return Optional.of(ConversionUtils.convert(option.get(cli), Integer.class));
      }
      else {
        return Optional.of(5);
      }
    }

    @Override
    public String getShortCode() {
      return "l";
    }
  })
  ,EPS(new OptionHandler<LoadOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "eps", true, "The target events per second");
      o.setArgName("EPS");
      o.setRequired(false);
      return o;
    }

    @Override
    public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
      if(option.has(cli)) {
        return Optional.of(ConversionUtils.convert(option.get(cli), Long.class));
      }
      else {
        return Optional.empty();
      }
    }

    @Override
    public String getShortCode() {
      return "e";
    }
  })
  ,KAFKA_CONFIG(new OptionHandler<LoadOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "kafka_config", true, "The kafka config.  This is a file containing a JSON map with the kafka config.");
      o.setArgName("CONFIG_FILE");
      o.setRequired(false);
      return o;
    }

    @Override
    public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
      if(!option.has(cli)) {
        return Optional.empty();
      }
      File configFile = new File(option.get(cli));
      if(configFile.exists()) {
        try {
          return Optional.ofNullable(JSONUtils.INSTANCE.load(configFile, JSONUtils.MAP_SUPPLIER));
        } catch (FileNotFoundException e) {
          throw new IllegalStateException("Unable to read file: " + option.get(cli), e);
        } catch (IOException e) {
          throw new IllegalStateException("Unable to read file: " + option.get(cli), e);
        }
      }
      else {
        throw new IllegalStateException("Unable to read file: " + option.get(cli) + " file doesn't exist.");
      }
    }

    @Override
    public String getShortCode() {
      return "k";
    }
  }),
  SEND_DELTA(new OptionHandler<LoadOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "send_delta_ms", true, "The time (in ms) between sending a batch of messages. Default is " + LoadGenerator.SEND_PERIOD_MS);
      o.setArgName("TIME_IN_MS");
      o.setRequired(false);
      return o;
    }

    @Override
    public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
      if(option.has(cli)) {
        Object res = option.get(cli);
        return Optional.ofNullable(ConversionUtils.convert(res, Long.class));
      }
      return Optional.of(LoadGenerator.SEND_PERIOD_MS);

    }

    @Override
    public String getShortCode() {
      return "sd";
    }
  }),
  MONITOR_DELTA(new OptionHandler<LoadOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "monitor_delta_ms", true, "The time (in ms) between monitoring output. Default is " + LoadGenerator.MONITOR_PERIOD_MS);
      o.setArgName("TIME_IN_MS");
      o.setRequired(false);
      return o;
    }

    @Override
    public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
      if(option.has(cli)) {
        Object res = option.get(cli);
        return Optional.ofNullable(ConversionUtils.convert(res, Long.class));
      }
      return Optional.of(LoadGenerator.MONITOR_PERIOD_MS);

    }

    @Override
    public String getShortCode() {
      return "md";
    }
  })
  ,TIME_LIMIT(new OptionHandler<LoadOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "time_limit_ms", true, "The total amount of time to run this in milliseconds.  By default, it never stops.");
      o.setArgName("MS");
      o.setRequired(false);
      return o;
    }

    @Override
    public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
      if(option.has(cli)) {
        Object res = option.get(cli);
        Long timeMs = ConversionUtils.convert(res, Long.class);
        return Optional.ofNullable(timeMs);
      }
      return Optional.empty();

    }

    @Override
    public String getShortCode() {
      return "tl";
    }
  })
  ,NUM_THREADS(new OptionHandler<LoadOptions>() {
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
        Object res = option.get(cli);
        if(res instanceof String && res.toString().toUpperCase().endsWith("C")) {
          numThreads *= ConversionUtils.convert(res.toString().trim().replace("C", ""), Integer.class);
        }
        else {
          numThreads = ConversionUtils.convert(res, Integer.class);
        }
      }
      return Optional.of(numThreads);

    }

    @Override
    public String getShortCode() {
      return "p";
    }
  })
  ,OUTPUT_TOPIC(new OptionHandler<LoadOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "output_topic", true, "The kafka topic to write to");
      o.setArgName("TOPIC");
      o.setRequired(false);
      return o;
    }

    @Override
    public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
      return Optional.ofNullable(option.get(cli));
    }

    @Override
    public String getShortCode() {
      return "ot";
    }
  }),
  MONITOR_TOPIC(new OptionHandler<LoadOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "monitor_topic", true, "The kafka topic to monitor.");
      o.setArgName("TOPIC");
      o.setRequired(false);
      return o;
    }

    @Override
    public Optional<Object> getValue(LoadOptions option, CommandLine cli) {
      return Optional.ofNullable(option.get(cli));
    }

    @Override
    public String getShortCode() {
      return "mt";
    }
  }),
  ;
  Option option;
  String shortCode;
  OptionHandler<LoadOptions> handler;
  LoadOptions(OptionHandler<LoadOptions> optionHandler) {
    this.shortCode = optionHandler.getShortCode();
    this.handler = optionHandler;
    this.option = optionHandler.apply(shortCode);
  }

  @Override
  public Option getOption() {
    return option;
  }

  public boolean has(CommandLine cli) {
    return cli.hasOption(shortCode);
  }

  public String get(CommandLine cli) {
    return cli.getOptionValue(shortCode);
  }

  @Override
  public OptionHandler<LoadOptions> getHandler() {
    return null;
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
    formatter.printHelp( "Generator", getOptions());
  }

  public static Options getOptions() {
    Options ret = new Options();
    for(LoadOptions o : LoadOptions.values()) {
      ret.addOption(o.option);
    }
    return ret;
  }
}
