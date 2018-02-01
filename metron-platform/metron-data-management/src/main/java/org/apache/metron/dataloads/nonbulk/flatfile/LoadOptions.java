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
import org.apache.metron.common.utils.cli.CLIOptions;
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

public enum LoadOptions implements CLIOptions<LoadOptions> {
  HELP(new CommonOptions.Help<> ())
  ,QUIET(new CommonOptions.Quiet<>())
  , IMPORT_MODE(new CommonOptions.ImportMode<>( ImportStrategy.values()
                                              , ImportStrategy.LOCAL
                                              , mode -> Optional.ofNullable(ImportStrategy.getStrategy(mode).orElse(ImportStrategy.LOCAL)))
               )
  ,HBASE_TABLE(new OptionHandler<LoadOptions>() {
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

    @Override
    public String getShortCode() {
      return "t";
    }
  })
  ,HBASE_CF(new OptionHandler<LoadOptions>() {
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

    @Override
    public String getShortCode() {
      return "c";
    }
  })
  ,EXTRACTOR_CONFIG(new CommonOptions.ExtractorConfig<>())
  ,ENRICHMENT_CONFIG(new OptionHandler<LoadOptions>() {
    @Override
    public String getShortCode() {
      return "n";
    }

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
  ,LOG4J_PROPERTIES(new CommonOptions.Log4jProperties<>())
  ,NUM_THREADS(new CommonOptions.NumThreads<>())
  ,BATCH_SIZE(new CommonOptions.BatchSize<>())
  ,INPUT(new CommonOptions.Input<>())
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
  public OptionHandler<LoadOptions> getHandler() {
    return handler;
  }


  @Override
  public Option getOption() {
    return option;
  }

  @Override
  public boolean has(CommandLine cli) {
    return cli.hasOption(shortCode);
  }

  @Override
  public String get(CommandLine cli) {
    return cli.getOptionValue(shortCode);
  }

  public static CommandLine parse(CommandLineParser parser, String[] args) {
    return OptionHandler.parse("SimpleEnrichmentFlatFileLoader", parser, args, values(), HELP);
  }

  public static EnumMap<LoadOptions, Optional<Object> > createConfig(CommandLine cli) {
    return OptionHandler.createConfig(cli, values(), LoadOptions.class);
  }
}
