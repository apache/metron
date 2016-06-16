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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Provides commmon required fields for the PCAP filter jobs
 */
public class CliParser {

  public Options buildOptions() {
    Options options = new Options();
    options.addOption(newOption("h", false, "Display help"));
    options.addOption(newOption("basePath", true, String.format("Base PCAP data path. Default is '%s'", CliConfig.BASE_PATH_DEFAULT)));
    options.addOption(newOption("baseOutputPath", true, String.format("Query result output path. Default is '%s'", CliConfig.BASE_OUTPUT_PATH_DEFAULT)));
    options.addOption(newOption("startTime", true, "Packet start time range. Default is '0'"));
    options.addOption(newOption("endTime", true, "Packet end time range. Default is current system time."));
    return options;
  }

  protected Option newOption(String opt, boolean hasArg, String desc) {
    return newOption(opt, hasArg, desc, false);
  }

  protected Option newOption(String opt, boolean hasArg, String desc, boolean required) {
    Option option = new Option(opt, hasArg, desc);
    option.setRequired(required);
    return option;
  }

  public void parse(CommandLine commandLine, CliConfig config) {
    if (commandLine.hasOption("h")) {
      config.setShowHelp(true);
    }
    if (commandLine.hasOption("basePath")) {
      config.setBasePath(commandLine.getOptionValue("basePath"));
    }
    if (commandLine.hasOption("baseOutputPath")) {
      config.setBaseOutputPath(commandLine.getOptionValue("baseOutputPath"));
    }
    if (commandLine.hasOption("startTime")) {
      try {
        long startTime = Long.parseLong(commandLine.getOptionValue("startTime"));
        config.setStartTime(startTime);
      } catch (NumberFormatException nfe) {
        //no-op
      }
    }
    if (commandLine.hasOption("endTime")) {
      try {
        long endTime = Long.parseLong(commandLine.getOptionValue("endTime"));
        config.setEndTime(endTime);
      } catch (NumberFormatException nfe) {
        //no-op
      }
    }
  }

  public void printHelp(String msg, Options opts) {
    new HelpFormatter().printHelp(msg, opts);
  }

}
