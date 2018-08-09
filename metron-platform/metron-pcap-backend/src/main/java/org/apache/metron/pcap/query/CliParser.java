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

package org.apache.metron.pcap.query;

import static org.apache.metron.pcap.config.PcapGlobalDefaults.BASE_INTERIM_RESULT_PATH_DEFAULT;
import static org.apache.metron.pcap.config.PcapGlobalDefaults.BASE_INPUT_PATH_DEFAULT;
import static org.apache.metron.pcap.config.PcapGlobalDefaults.NUM_RECORDS_PER_FILE_DEFAULT;
import static org.apache.metron.pcap.config.PcapGlobalDefaults.NUM_REDUCERS_DEFAULT;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.metron.pcap.config.PcapConfig;

/**
 * Provides commmon required fields for the PCAP filter jobs
 */
public class CliParser {
  private CommandLineParser parser;
  protected PcapConfig.PrefixStrategy prefixStrategy;

  public CliParser(PcapConfig.PrefixStrategy prefixStrategy) {
    this.prefixStrategy = prefixStrategy;
    parser = new PosixParser();
  }

  public Options buildOptions() {
    Options options = new Options();
    options.addOption(newOption("h", "help", false, "Display help"));
    options.addOption(newOption("bp", "base_path", true, String.format("Base PCAP data path. Default is '%s'",
        BASE_INPUT_PATH_DEFAULT)));
    options.addOption(newOption("bop", "base_output_path", true, String.format("Query result output path. Default is '%s'",
        BASE_INTERIM_RESULT_PATH_DEFAULT)));
    options.addOption(newOption("st", "start_time", true, "(required) Packet start time range.", true));
    options.addOption(newOption("nr", "num_reducers", true, String.format("Number of reducers to use (defaults to %s)", NUM_REDUCERS_DEFAULT)));
    options.addOption(newOption("rpf", "records_per_file", true, String.format("Number of records to include in each output pcap file (defaults to %s)", NUM_RECORDS_PER_FILE_DEFAULT)));
    options.addOption(newOption("et", "end_time", true, "Packet end time range. Default is current system time."));
    options.addOption(newOption("df", "date_format", true, "Date format to use for parsing start_time and end_time. Default is to use time in millis since the epoch."));
    return options;
  }

  protected Option newOption(String opt, String longOpt, boolean hasArg, String desc) {
    return newOption(opt, longOpt, hasArg, desc, false);
  }

  protected Option newOption(String opt, String longOpt, boolean hasArg, String desc, boolean required) {
    Option option = new Option(opt, longOpt, hasArg, desc);
    option.setRequired(required);
    return option;
  }

  public void parse(CommandLine commandLine, PcapConfig config) throws java.text.ParseException {
    if (commandLine.hasOption("help")) {
      config.setShowHelp(true);
    }
    if (commandLine.hasOption("date_format")) {
      config.setDateFormat(commandLine.getOptionValue("date_format"));
    }
    if (commandLine.hasOption("base_path")) {
      config.setBasePath(commandLine.getOptionValue("base_path"));
    } else {
      config.setBasePath(BASE_INPUT_PATH_DEFAULT);
    }
    if (commandLine.hasOption("base_output_path")) {
      config.setBaseInterimResultPath(commandLine.getOptionValue("base_output_path"));
    } else {
      config.setBaseInterimResultPath(BASE_INTERIM_RESULT_PATH_DEFAULT);
    }
    if (commandLine.hasOption("start_time")) {
      try {
        if (commandLine.hasOption("date_format")) {
          long startTime = config.getDateFormat().parse(commandLine.getOptionValue("start_time")).getTime();
          config.setStartTimeMs(startTime);
        } else {
          long startTime = Long.parseLong(commandLine.getOptionValue("start_time"));
          config.setStartTimeMs(startTime);
        }
      } catch (NumberFormatException nfe) {
        //no-op
      }
    }
    if (commandLine.hasOption("num_reducers")) {
      int numReducers = Integer.parseInt(commandLine.getOptionValue("num_reducers"));
      config.setNumReducers(numReducers);
    }
    else {
      config.setNumReducers(NUM_REDUCERS_DEFAULT);
    }
    if (commandLine.hasOption("records_per_file")) {
      int numRecordsPerFile = Integer.parseInt(commandLine.getOptionValue("records_per_file"));
      config.setNumRecordsPerFile(numRecordsPerFile);
    }
    else {
      config.setNumRecordsPerFile(NUM_RECORDS_PER_FILE_DEFAULT);
    }
    if (commandLine.hasOption("end_time")) {
      try {
        if (commandLine.hasOption("date_format")) {
          long endTime = config.getDateFormat().parse(commandLine.getOptionValue("end_time")).getTime();
          config.setEndTimeMs(endTime);
        } else {
          long endTime = Long.parseLong(commandLine.getOptionValue("end_time"));
          config.setEndTimeMs(endTime);
        }
      } catch (NumberFormatException nfe) {
        //no-op
      }
    }
  }

  public void printHelp(String msg, Options opts) {
    new HelpFormatter().printHelp(msg, opts);
  }

  protected CommandLineParser getParser() {
    return parser;
  }
}
