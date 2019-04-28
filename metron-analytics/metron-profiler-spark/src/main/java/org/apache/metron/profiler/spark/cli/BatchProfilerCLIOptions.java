/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.metron.profiler.spark.cli;

import com.google.common.base.Joiner;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.function.Supplier;

/**
 * Defines the command line interface (CLI) options accepted by the Batch
 * Profiler.
 */
public enum BatchProfilerCLIOptions {
  PROFILE_ZK(() -> {
    Option o = new Option("z", "zookeeper", true, "Zookeeper quorum for profile definitions");
    o.setRequired(false);
    return o;
  }),

  PROFILE_TIMESTAMP_FLD(() -> {
    Option o = new Option("t", "timestampfield", true,
            "The name of a field to source event time from");
    o.setRequired(false);
    return o;
  }),

  PROFILE_DEFN_FILE(() -> {
    Option o = new Option("p", "profiles", true, "Path to the profile definitions.");
    o.setRequired(false);
    return o;
  }),

  PROFILER_PROPS_FILE(() -> {
    Option o = new Option("c", "config", true, "Path to the profiler properties file.");
    o.setRequired(false);
    return o;
  }),

  GLOBALS_FILE(() -> {
    Option o = new Option("g", "globals", true, "Path to the Stellar global config file.");
    o.setRequired(false);
    return o;
  }),

  READER_PROPS_FILE(() -> {
    Option o = new Option("r", "reader", true, "Path to properties for the DataFrameReader.");
    o.setRequired(false);
    return o;
  }),

  HELP(() -> {
    Option o = new Option("h", "help", false, "Usage instructions.");
    o.setRequired(false);
    return o;
  });

  private Option option;

  BatchProfilerCLIOptions(Supplier<Option> optionSupplier) {
    this.option = optionSupplier.get();
  }

  /**
   * Returns true, if the command line contains this option.
   *
   * @param cli The command line to parse.
   */
  public boolean has(CommandLine cli) {
    return cli.hasOption(option.getOpt());
  }

  /**
   * Returns the option value from the command line.
   *
   * @param cli The command line to parse.
   */
  public String get(CommandLine cli) {
    return cli.getOptionValue(option.getOpt());
  }

  /**
   * Returns the option value from the command line.  The default value is
   * returned if the value is not provided on the command line.
   *
   * @param cli The command line to parse.
   * @param defaultValue The default value.
   */
  public String get(CommandLine cli, String defaultValue) {
    return has(cli) ? cli.getOptionValue(option.getOpt()) : defaultValue;
  }

  /**
   * Extracts the valid command line options from a given command line.
   *
   * @param parser The command line parser.
   * @param args The command line to parse.
   * @return The command line options.
   * @throws ParseException If the command line cannot be parsed successfully.
   */
  public static CommandLine parse(CommandLineParser parser, String[] args) throws ParseException {
    try {
      CommandLine cli = parser.parse(getOptions(), args);
      if(HELP.has(cli)) {
        printHelp();
        System.exit(0);
      }
      return cli;

    } catch (ParseException e) {
      System.err.println("invalid arguments: " + Joiner.on(' ').join(args));
      e.printStackTrace(System.err);
      printHelp();
      throw e;
    }
  }

  /**
   * Print a help screen.
   */
  public static void printHelp() {
    HelpFormatter formatter = new HelpFormatter();
    String header = "options:";
    String footer = "";
    String cmd = String.format("spark-submit --class %s --properties-file [spark.properties] [jar] [options]",
            BatchProfilerCLI.class.getCanonicalName());
    formatter.printHelp(cmd, header, getOptions(), footer);
  }

  /**
   * Returns all valid CLI options.
   */
  public static Options getOptions() {
    Options allOptions = new Options();
    for(BatchProfilerCLIOptions o : BatchProfilerCLIOptions.values()) {
      allOptions.addOption(o.option);
    }
    return allOptions;
  }
}
