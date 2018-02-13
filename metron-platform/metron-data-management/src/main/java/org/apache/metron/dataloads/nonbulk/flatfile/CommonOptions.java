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
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.io.FileUtils;
import org.apache.metron.common.utils.cli.CLIOptions;
import org.apache.metron.common.utils.cli.OptionHandler;
import org.apache.metron.stellar.common.utils.ConversionUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class CommonOptions {
  public static class Help<OPT_T extends Enum<OPT_T> & CLIOptions<OPT_T>> extends OptionHandler<OPT_T> {

    @Override
    public String getShortCode() {
      return "h";
    }


    @Nullable
    @Override
    public Option apply(@Nullable String input) {
      return new Option(getShortCode(), "help", false, "Generate Help screen");
    }
  }

  public static class Quiet<OPT_T extends Enum<OPT_T> & CLIOptions<OPT_T>> extends OptionHandler<OPT_T> {

    @Override
    public String getShortCode() {
      return "q";
    }

    @Nullable
    @Override
    public Option apply(@Nullable String input) {
      return new Option(getShortCode(), "quiet", false, "Do not update progress");
    }

    @Override
    public Optional<Object> getValue(OPT_T option, CommandLine cli) {
      return Optional.of(option.has(cli));
    }
  }

  public static class ImportMode<OPT_T extends Enum<OPT_T> & CLIOptions<OPT_T>> extends OptionHandler<OPT_T> {
    Object[] importModes;
    Object defaultMode;
    Function<String, Optional<Object>> resolver;
    public ImportMode(Object[] importModes, Object defaultMode, Function<String, Optional<Object>> resolver) {
      this.importModes = importModes;
      this.defaultMode = defaultMode;
      this.resolver = resolver;
    }

    @Override
    public String getShortCode() {
      return "m";
    }

    @Nullable
    @Override
    public Option apply(@Nullable String input) {
      Option o = new Option(getShortCode(), "import_mode", true
                           , "The Import mode to use: " + Joiner.on(",").join(importModes)
                           + ".  Default: " +defaultMode
                           );
      o.setArgName("MODE");
      o.setRequired(false);
      return o;
    }

    @Override
    public Optional<Object> getValue(OPT_T option, CommandLine cli) {
      String mode = option.get(cli);
      return resolver.apply(mode);
    }
  }

  public static class ExtractorConfig<OPT_T extends Enum<OPT_T> & CLIOptions<OPT_T>> extends OptionHandler<OPT_T> {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "extractor_config", true, "JSON Document describing the extractor for this input data source");
      o.setArgName("JSON_FILE");
      o.setRequired(true);
      return o;
    }

    @Override
    public Optional<Object> getValue(OPT_T option, CommandLine cli) {
      try {
        return Optional.ofNullable(FileUtils.readFileToString(new File(option.get(cli).trim())));
      } catch (IOException e) {
        throw new IllegalStateException("Unable to retrieve extractor config from " + option.get(cli) + ": " + e.getMessage(), e);
      }
    }

    @Override
    public String getShortCode() {
      return "e";
    }
  }


  public static class Log4jProperties<OPT_T extends Enum<OPT_T> & CLIOptions<OPT_T>> extends OptionHandler<OPT_T> {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "log4j", true, "The log4j properties file to load");
      o.setArgName("FILE");
      o.setRequired(false);
      return o;
    }

    @Override
    public String getShortCode() {
      return "l";
    }
  }


  public static class NumThreads<OPT_T extends Enum<OPT_T> & CLIOptions<OPT_T>> extends OptionHandler<OPT_T> {

    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "threads", true, "The number of threads to use when extracting data.  The default is the number of cores of your machine.");
      o.setArgName("NUM_THREADS");
      o.setRequired(false);
      return o;
    }

    @Override
    public Optional<Object> getValue(OPT_T option, CommandLine cli) {
      int numThreads = Runtime.getRuntime().availableProcessors();
      if(option.has(cli)) {
        numThreads = ConversionUtils.convert(option.get(cli), Integer.class);
      }
      return Optional.of(numThreads);
    }

    @Override
    public String getShortCode() {
      return "p";
    }
  }

  public static class BatchSize<OPT_T extends Enum<OPT_T> & CLIOptions<OPT_T>> extends OptionHandler<OPT_T> {

    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "batchSize", true, "The batch size to use for HBase puts");
      o.setArgName("SIZE");
      o.setRequired(false);
      return o;
    }

    @Override
    public Optional<Object> getValue(OPT_T option, CommandLine cli) {
      int batchSize = 128;
      if(option.has(cli)) {
        batchSize = ConversionUtils.convert(option.get(cli), Integer.class);
      }
      return Optional.of(batchSize);
    }

    @Override
    public String getShortCode() {
      return "b";
    }
  }

  public static class Input<OPT_T extends Enum<OPT_T> & CLIOptions<OPT_T>> extends OptionHandler<OPT_T> {
    @Nullable
    @Override
    public Option apply(@Nullable String s) {
      Option o = new Option(s, "input", true, "The CSV File to load");
      o.setArgName("FILE");
      o.setRequired(true);
      return o;
    }

    @Override
    public Optional<Object> getValue(OPT_T option, CommandLine cli) {
      List<String> inputs = new ArrayList<>();
      for(String input : Splitter.on(",").split(Optional.ofNullable(option.get(cli)).orElse(""))) {
        inputs.add(input.trim());
      }
      return Optional.of(inputs);
    }

    @Override
    public String getShortCode() {
      return "i";
    }
  }
}
