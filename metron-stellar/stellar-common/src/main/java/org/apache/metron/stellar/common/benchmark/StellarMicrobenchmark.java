/*
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
package org.apache.metron.stellar.common.benchmark;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import org.apache.commons.cli.*;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.MapVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.stellar.common.utils.JSONUtils;
import org.apache.metron.stellar.common.utils.cli.OptionHandler;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.*;

public class StellarMicrobenchmark {

  public static int DEFAULT_WARMUP = 100;
  public static int DEFAULT_NUM_TIMES = 1000;
  public static Double[] DEFAULT_PERCENTILES = new Double[] {
    50d, 75d, 95d, 99d
  };

  enum BenchmarkOptions {
    HELP("h", new OptionHandler<BenchmarkOptions>() {

      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        return new Option(s, "help", false, "Generate Help screen");
      }
    }),
    WARMUP("w", new OptionHandler<BenchmarkOptions>() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "warmup", true, "Number of times for warmup per expression. Default: " + DEFAULT_WARMUP);
        o.setArgName("NUM");
        o.setRequired(false);
        return o;
      }

      @Override
      public Optional<Object> getValue(BenchmarkOptions option, CommandLine cli) {
        return Optional.ofNullable(option.get(cli).trim());
      }
    }),
    PERCENTILES("p", new OptionHandler<BenchmarkOptions>() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "percentiles", true
                             , "Percentiles to calculate per run. Default: " + Joiner.on(",").join(Arrays.asList(DEFAULT_PERCENTILES))
                             );
        o.setArgName("NUM");
        o.setRequired(false);
        return o;
      }

      @Override
      public Optional<Object> getValue(BenchmarkOptions option, CommandLine cli) {
        return Optional.ofNullable(option.get(cli).trim());
      }
    }),
    NUM_TIMES("n", new OptionHandler<BenchmarkOptions>() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "num_times", true, "Number of times to run per expression (after warmup). Default: " + DEFAULT_NUM_TIMES);
        o.setArgName("NUM");
        o.setRequired(false);
        return o;
      }

      @Override
      public Optional<Object> getValue(BenchmarkOptions option, CommandLine cli) {
        return Optional.ofNullable(option.get(cli).trim());
      }
    }),
    EXPRESSIONS("e", new OptionHandler<BenchmarkOptions>() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "expressions", true, "Stellar expressions");
        o.setArgName("FILE");
        o.setRequired(false);
        return o;
      }

      @Override
      public Optional<Object> getValue(BenchmarkOptions option, CommandLine cli) {
        return Optional.ofNullable(option.get(cli).trim());
      }
    }),
    VARIABLES("v", new OptionHandler<BenchmarkOptions>() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "variables", true, "File containing a JSON Map of variables to use");
        o.setArgName("FILE");
        o.setRequired(false);
        return o;
      }

      @Override
      public Optional<Object> getValue(BenchmarkOptions option, CommandLine cli) {
        return Optional.ofNullable(option.get(cli).trim());
      }
    }),
    OUTPUT("o", new OptionHandler<BenchmarkOptions>() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "output", true, "File to write output.");
        o.setArgName("FILE");
        o.setRequired(false);
        return o;
      }

      @Override
      public Optional<Object> getValue(BenchmarkOptions option, CommandLine cli) {
        return Optional.ofNullable(option.get(cli).trim());
      }
    })
    ;
    ;
    Option option;
    String shortCode;
    OptionHandler<BenchmarkOptions> handler;
    BenchmarkOptions(String shortCode, OptionHandler<BenchmarkOptions> optionHandler) {
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
      } catch (org.apache.commons.cli.ParseException e) {
        System.err.println("Unable to parse args: " + Joiner.on(' ').join(args));
        e.printStackTrace(System.err);
        printHelp();
        System.exit(-1);
        return null;
      }
    }

    public static EnumMap<BenchmarkOptions, Optional<Object>> createConfig(CommandLine cli) {
      EnumMap<BenchmarkOptions, Optional<Object> > ret = new EnumMap<>(BenchmarkOptions.class);
      for(BenchmarkOptions option : values()) {
        ret.put(option, option.handler.getValue(option, cli));
      }
      return ret;
    }

    public static void printHelp() {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp( "StellarBenchmark", getOptions());
    }

    public static Options getOptions() {
      Options ret = new Options();
      for(BenchmarkOptions o : BenchmarkOptions.values()) {
        ret.addOption(o.option);
      }
      return ret;
    }
  }

  public static void main(String... argv) throws IOException {
    CommandLine cli = BenchmarkOptions.parse(new PosixParser(), argv);
    if(!BenchmarkOptions.EXPRESSIONS.has(cli)) {
      throw new IllegalStateException("You must at least specify an expressions file.");
    }
    File expressionsFile = new File(BenchmarkOptions.EXPRESSIONS.get(cli));
    Optional<File> variablesFile = Optional.ofNullable(!BenchmarkOptions.VARIABLES.has(cli)
                                                      ?null
                                                      :new File(BenchmarkOptions.VARIABLES.get(cli))
                                                      );
    Optional<File> output = Optional.ofNullable(!BenchmarkOptions.OUTPUT.has(cli)
                                             ?null
                                             :new File(BenchmarkOptions.OUTPUT.get(cli))
                                             );
    List<String> lines = Files.readLines(expressionsFile, Charset.defaultCharset());
    Map<String, Object> variables = new HashMap<>();
    if(variablesFile.isPresent()) {
      variables = JSONUtils.INSTANCE.load(new FileInputStream(variablesFile.get()), new TypeReference<Map<String, Object>>() {
      });
    }
    int numTimes = DEFAULT_NUM_TIMES;
    if(BenchmarkOptions.NUM_TIMES.has(cli)) {
      numTimes = Integer.parseInt(BenchmarkOptions.NUM_TIMES.get(cli));
    }
    int warmup = DEFAULT_WARMUP;
    if(BenchmarkOptions.WARMUP.has(cli)) {
      warmup = Integer.parseInt(BenchmarkOptions.WARMUP.get(cli));
    }
    Double[] percentiles = DEFAULT_PERCENTILES;
    if(BenchmarkOptions.PERCENTILES.has(cli)) {
      List<Double> percentileList = new ArrayList<>();
      for(String token : Splitter.on(",").split(BenchmarkOptions.PERCENTILES.get(cli))) {
        if(token.trim().isEmpty()) {
          continue;
        }
        Double d = Double.parseDouble(token.trim());
        percentileList.add(d);
      }
      percentiles = (Double[])percentileList.toArray();
    }
    PrintWriter out = new PrintWriter(System.out);
    if(output.isPresent()) {
      out = new PrintWriter(output.get());
    }
    for(String statement : lines) {
      if(statement.trim().startsWith("#") || statement.trim().isEmpty()) {
        continue;
      }
      Microbenchmark.StellarStatement s = new Microbenchmark.StellarStatement();
      s.context = Context.EMPTY_CONTEXT();
      s.expression = statement;
      s.functionResolver = StellarFunctions.FUNCTION_RESOLVER();
      s.variableResolver = new MapVariableResolver(variables);
      DescriptiveStatistics stats = Microbenchmark.run(s, warmup, numTimes);
      out.println("Expression: " + statement);
      out.println(Microbenchmark.describe(stats, percentiles));
    }
    if(argv.length > 2) {
      out.close();
    }
  }
}
