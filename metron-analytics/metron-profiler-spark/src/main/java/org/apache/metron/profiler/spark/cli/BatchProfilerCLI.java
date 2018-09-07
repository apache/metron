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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.IOUtils;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.profiler.spark.BatchProfiler;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Properties;

import static org.apache.metron.profiler.spark.cli.BatchProfilerCLIOptions.PROFILER_PROPS_FILE;
import static org.apache.metron.profiler.spark.cli.BatchProfilerCLIOptions.GLOBALS_FILE;
import static org.apache.metron.profiler.spark.cli.BatchProfilerCLIOptions.PROFILE_DEFN_FILE;
import static org.apache.metron.profiler.spark.cli.BatchProfilerCLIOptions.READER_PROPS_FILE;
import static org.apache.metron.profiler.spark.cli.BatchProfilerCLIOptions.parse;

/**
 * The main entry point which launches the Batch Profiler in Spark.
 *
 * With this class the Batch Profiler can be submitted using the following command.
 *
 * <pre>{@code
 *  $SPARK_HOME/bin/spark-submit \
 *    --class org.apache.metron.profiler.spark.cli.BatchProfilerCLI \
 *     --properties-file spark.properties \
 *     metron-profiler-spark-<version>.jar \
 *     --config profiler.properties \
 *     --globals global.properties \
 *     --profiles profiles.json \
 *     --reader reader.properties
 * }</pre>
 */
public class BatchProfilerCLI implements Serializable {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static Properties globals;
  public static Properties profilerProps;
  public static Properties readerProps;
  public static ProfilerConfig profiles;

  public static void main(String[] args) throws IOException, org.apache.commons.cli.ParseException {
    // parse the command line
    CommandLine commandLine = parseCommandLine(args);
    profilerProps = handleProfilerProperties(commandLine);
    globals = handleGlobals(commandLine);
    profiles = handleProfileDefinitions(commandLine);
    readerProps = handleReaderProperties(commandLine);

    // the batch profiler must use 'event time'
    if(!profiles.getTimestampField().isPresent()) {
      throw new IllegalArgumentException("The Batch Profiler must use event time. The 'timestampField' must be defined.");
    }

    // one or more profiles must be defined
    if(profiles.getProfiles().size() == 0) {
      throw new IllegalArgumentException("No profile definitions found.");
    }

    SparkSession spark = SparkSession
            .builder()
            .config(new SparkConf())
            .getOrCreate();

    BatchProfiler profiler = new BatchProfiler();
    long count = profiler.run(spark, profilerProps, globals, readerProps, profiles);
    LOG.info("Profiler produced {} profile measurement(s)", count);
  }

  /**
   * Load the Stellar globals from a file.
   *
   * @param commandLine The command line.
   */
  private static Properties handleGlobals(CommandLine commandLine) throws IOException {
    Properties globals = new Properties();
    if(GLOBALS_FILE.has(commandLine)) {
      String globalsPath = GLOBALS_FILE.get(commandLine);

      LOG.info("Loading global properties from '{}'", globalsPath);
      globals.load(new FileInputStream(globalsPath));

      LOG.info("Globals = {}", globals);
    }
    return globals;
  }

  /**
   * Load the Profiler configuration from a file.
   *
   * @param commandLine The command line.
   */
  private static Properties handleProfilerProperties(CommandLine commandLine) throws IOException {
    Properties config = new Properties();
    if(PROFILER_PROPS_FILE.has(commandLine)) {
      String propertiesPath = PROFILER_PROPS_FILE.get(commandLine);

      LOG.info("Loading profiler properties from '{}'", propertiesPath);
      config.load(new FileInputStream(propertiesPath));

      LOG.info("Profiler properties = {}", config.toString());
    }
    return config;
  }

  /**
   * Load the properties for the {@link org.apache.spark.sql.DataFrameReader}.
   *
   * @param commandLine The command line.
   */
  private static Properties handleReaderProperties(CommandLine commandLine) throws IOException {
    Properties config = new Properties();
    if(READER_PROPS_FILE.has(commandLine)) {
      String readerPropsPath = READER_PROPS_FILE.get(commandLine);

      LOG.info("Loading reader properties from '{}'", readerPropsPath);
      config.load(new FileInputStream(readerPropsPath));

      LOG.info("Reader properties = {}", config.toString());
    }
    return config;
  }

  /**
   * Load the profile definitions from a file.
   *
   * @param commandLine The command line.
   */
  private static ProfilerConfig handleProfileDefinitions(CommandLine commandLine) throws IOException {
    ProfilerConfig profiles;
    if(PROFILE_DEFN_FILE.has(commandLine)) {
      String profilePath = PROFILE_DEFN_FILE.get(commandLine);

      LOG.info("Loading profiles from '{}'", profilePath);
      String contents = IOUtils.toString(new FileInputStream(profilePath));

      profiles = ProfilerConfig.fromJSON(contents);
      LOG.info("Loaded {} profile(s)", profiles.getProfiles().size());

    } else {
      throw new IllegalArgumentException("No profile(s) defined");
    }
    return profiles;
  }

  /**
   * Parse the command line arguments submitted by the user.
   * @param args The command line arguments to parse.
   * @throws org.apache.commons.cli.ParseException
   */
  private static CommandLine parseCommandLine(String[] args) throws ParseException {
    CommandLineParser parser = new PosixParser();
    return parse(parser, args);
  }

  public static Properties getGlobals() {
    return globals;
  }

  public static Properties getProfilerProps() {
    return profilerProps;
  }

  public static ProfilerConfig getProfiles() {
    return profiles;
  }

  public static Properties getReaderProps() {
    return readerProps;
  }
}
