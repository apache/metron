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
package org.apache.metron.common.utils.cli;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import org.apache.commons.cli.*;

import java.util.EnumMap;
import java.util.Optional;

public abstract class OptionHandler<OPT_T extends Enum<OPT_T> & CLIOptions<OPT_T>> implements Function<String, Option>
{
  public Optional<Object> getValue(OPT_T option, CommandLine cli) {
    return Optional.empty();
  }

  public abstract String getShortCode();

  public static Options getOptions(CLIOptions[] values) {
    Options ret = new Options();
    for(CLIOptions o : values) {
      ret.addOption(o.getOption());
    }
    return ret;
  }

  public static void printHelp(String name, CLIOptions[] values) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp( name, getOptions(values));
  }

  public static <OPT_T extends Enum<OPT_T> & CLIOptions<OPT_T>>
  EnumMap<OPT_T, Optional<Object> > createConfig(CommandLine cli, OPT_T[] values, Class<OPT_T> clazz) {
    EnumMap<OPT_T, Optional<Object> > ret = new EnumMap<>(clazz);
    for(OPT_T option : values) {
      ret.put(option, option.getHandler().getValue(option, cli));
    }
    return ret;
  }

  public static CommandLine parse(String name, CommandLineParser parser, String[] args, CLIOptions[] values, CLIOptions helpOption) {
    try {
      CommandLine cli = parser.parse(getOptions(values), args);
      if(helpOption.has(cli)) {
        printHelp(name, values);
        System.exit(0);
      }
      return cli;
    } catch (ParseException e) {
      System.err.println("Unable to parse args: " + Joiner.on(' ').join(args));
      e.printStackTrace(System.err);
      printHelp(name, values);
      System.exit(-1);
      return null;
    }
  }
}
