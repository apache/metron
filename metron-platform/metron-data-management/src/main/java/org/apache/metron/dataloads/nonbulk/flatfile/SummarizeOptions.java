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
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.metron.common.utils.cli.CLIOptions;
import org.apache.metron.common.utils.cli.OptionHandler;
import org.apache.metron.dataloads.nonbulk.flatfile.importer.Summarizers;
import org.apache.metron.dataloads.nonbulk.flatfile.writer.Writers;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

public enum SummarizeOptions implements CLIOptions<SummarizeOptions> {
  HELP(new CommonOptions.Help<>())
  ,QUIET(new CommonOptions.Quiet<>())
  , IMPORT_MODE(new CommonOptions.ImportMode<>(Summarizers.values(), Summarizers.LOCAL, mode -> Optional.of(Summarizers.getStrategy(mode).orElse(Summarizers.LOCAL))))
  , OUTPUT_MODE(new OptionHandler<SummarizeOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "output_mode", true
                           , "The output mode to use: " + Joiner.on(",").join(Writers.values())
                           + ".  Default: " + Writers.LOCAL
                           );
      o.setArgName("MODE");
      o.setRequired(false);
      return o;
    }

    @Override
    public Optional<Object> getValue(SummarizeOptions option, CommandLine cli) {
      String mode = option.get(cli);
      return Optional.of(Writers.getStrategy(mode).orElse(Writers.LOCAL));
    }

    @Override
    public String getShortCode() {
      return "om";
    }
  })
  ,EXTRACTOR_CONFIG(new CommonOptions.ExtractorConfig<>())
  ,LOG4J_PROPERTIES(new CommonOptions.Log4jProperties<>())
  ,NUM_THREADS(new CommonOptions.NumThreads<>())
  ,BATCH_SIZE(new CommonOptions.BatchSize<>())
  ,INPUT(new CommonOptions.Input<>())
  ,OUTPUT(new OptionHandler<SummarizeOptions>() {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "output", true, "The output file to write");
      o.setArgName("FILE");
      o.setRequired(false);
      return o;
    }

    @Override
    public Optional<Object> getValue(SummarizeOptions option, CommandLine cli) {
      return Optional.ofNullable(option.get(cli));
    }

    @Override
    public String getShortCode() {
      return "o";
    }
  })
  ;
  Option option;
  String shortCode;
  OptionHandler<SummarizeOptions> handler;
  SummarizeOptions(OptionHandler<SummarizeOptions> optionHandler) {
    this.shortCode = optionHandler.getShortCode();
    this.handler = optionHandler;
    this.option = optionHandler.apply(shortCode);
  }

  @Override
  public OptionHandler<SummarizeOptions> getHandler() {
    return handler;
  }

  public Option getOption() {
    return option;
  }

  public boolean has(CommandLine cli) {
    return cli.hasOption(shortCode);
  }

  public String get(CommandLine cli) {
    return cli.getOptionValue(shortCode);
  }

  public static CommandLine parse(CommandLineParser parser, String[] args) {
    return OptionHandler.parse("SimpleFlatFileSummarizer", parser, args, values(), HELP);
  }

  public static EnumMap<SummarizeOptions, Optional<Object> > createConfig(CommandLine cli) {
    return OptionHandler.createConfig(cli, values(), SummarizeOptions.class);
  }
}
