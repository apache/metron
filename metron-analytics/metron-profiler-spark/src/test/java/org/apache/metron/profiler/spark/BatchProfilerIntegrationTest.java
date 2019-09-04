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
package org.apache.metron.profiler.spark;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.profiler.client.stellar.FixedLookback;
import org.apache.metron.profiler.client.stellar.GetProfile;
import org.apache.metron.profiler.client.stellar.WindowLookback;
import org.apache.metron.stellar.common.DefaultStellarStatefulExecutor;
import org.apache.metron.stellar.common.StellarStatefulExecutor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.functions.resolver.SimpleFunctionResolver;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkException;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.apache.metron.common.configuration.profiler.ProfilerConfig.fromJSON;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_COLUMN_FAMILY;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_HBASE_TABLE;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_HBASE_TABLE_PROVIDER;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_COLUMN_FAMILY;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_TABLE_NAME;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_TABLE_PROVIDER;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.TELEMETRY_INPUT_BEGIN;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.TELEMETRY_INPUT_END;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.TELEMETRY_INPUT_FORMAT;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.TELEMETRY_INPUT_PATH;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.TELEMETRY_INPUT_READER;
import static org.apache.metron.profiler.spark.reader.TelemetryReaders.JSON;
import static org.apache.metron.profiler.spark.reader.TelemetryReaders.ORC;
import static org.apache.metron.profiler.spark.reader.TelemetryReaders.PARQUET;
import static org.junit.Assert.assertTrue;

/**
 * An integration test for the {@link BatchProfiler}.
 */
public class BatchProfilerIntegrationTest {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static SparkSession spark;
  private Properties profilerProperties;
  private Properties readerProperties;
  private StellarStatefulExecutor executor;

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @BeforeClass
  public static void setupSpark() {
    SparkConf conf = new SparkConf()
            .setMaster("local")
            .setAppName("BatchProfilerIntegrationTest")
            .set("spark.sql.shuffle.partitions", "8");
    spark = SparkSession
            .builder()
            .config(conf)
            .getOrCreate();
  }

  @AfterClass
  public static void tearDownSpark() {
    if(spark != null) {
      spark.close();
    }
  }

  @Before
  public void setup() {
    readerProperties = new Properties();
    profilerProperties = new Properties();

    // the output will be written to a mock HBase table
    String tableName = HBASE_TABLE_NAME.get(profilerProperties, String.class);
    String columnFamily = HBASE_COLUMN_FAMILY.get(profilerProperties, String.class);
    profilerProperties.put(HBASE_TABLE_PROVIDER.getKey(), MockHBaseTableProvider.class.getName());

    // create the mock hbase table
    MockHBaseTableProvider.addToCache(tableName, columnFamily);

    // define the globals required by `PROFILE_GET`
    Map<String, Object> global = new HashMap<String, Object>() {{
      put(PROFILER_HBASE_TABLE.getKey(), tableName);
      put(PROFILER_COLUMN_FAMILY.getKey(), columnFamily);
      put(PROFILER_HBASE_TABLE_PROVIDER.getKey(), MockHBaseTableProvider.class.getName());
    }};

    // create the stellar execution environment
    executor = new DefaultStellarStatefulExecutor(
            new SimpleFunctionResolver()
                    .withClass(GetProfile.class)
                    .withClass(FixedLookback.class)
                    .withClass(WindowLookback.class),
            new Context.Builder()
                    .with(Context.Capabilities.GLOBAL_CONFIG, () -> global)
                    .build());
  }

  /**
   * {
   *   "timestampField": "timestamp",
   *   "profiles": [
   *      {
   *        "profile": "count-by-ip",
   *        "foreach": "ip_src_addr",
   *        "init": { "count": 0 },
   *        "update": { "count" : "count + 1" },
   *        "result": "count"
   *      },
   *      {
   *        "profile": "total-count",
   *        "foreach": "'total'",
   *        "init": { "count": 0 },
   *        "update": { "count": "count + 1" },
   *        "result": "count"
   *      }
   *   ]
   * }
   */
  @Multiline
  private static String profileJson;

  /**
   * This test uses the Batch Profiler to seed two profiles using archived telemetry.
   *
   * The first profile counts the number of messages by 'ip_src_addr'.  The second profile counts the total number
   * of messages.
   *
   * The archived telemetry contains timestamps from around July 7, 2018.  All of the measurements
   * produced will center around this date.
   */
  @Test
  public void testBatchProfilerWithJSON() throws Exception {
    // the input telemetry is text/json stored in the local filesystem
    profilerProperties.put(TELEMETRY_INPUT_READER.getKey(), JSON.toString());
    profilerProperties.put(TELEMETRY_INPUT_PATH.getKey(), "src/test/resources/telemetry.json");

    BatchProfiler profiler = new BatchProfiler();
    profiler.run(spark, profilerProperties, getGlobals(), readerProperties, fromJSON(profileJson));

    validateProfiles();
  }

  @Test
  public void testBatchProfilerWithORC() throws Exception {
    // re-write the test data as column-oriented ORC
    String pathToORC = tempFolder.getRoot().getAbsolutePath();
    spark.read()
            .format("json")
            .load("src/test/resources/telemetry.json")
            .write()
            .mode("overwrite")
            .format("org.apache.spark.sql.execution.datasources.orc")
            .save(pathToORC);

    // tell the profiler to use the ORC input data
    profilerProperties.put(TELEMETRY_INPUT_READER.getKey(), ORC.toString());
    profilerProperties.put(TELEMETRY_INPUT_PATH.getKey(), pathToORC);

    BatchProfiler profiler = new BatchProfiler();
    profiler.run(spark, profilerProperties, getGlobals(), readerProperties, fromJSON(profileJson));

    validateProfiles();
  }

  @Test
  public void testBatchProfilerWithParquet() throws Exception {
    // re-write the test data as column-oriented ORC
    String inputPath = tempFolder.getRoot().getAbsolutePath();
    spark.read()
            .format("json")
            .load("src/test/resources/telemetry.json")
            .write()
            .mode("overwrite")
            .format("parquet")
            .save(inputPath);

    // tell the profiler to use the ORC input data
    profilerProperties.put(TELEMETRY_INPUT_READER.getKey(), PARQUET.toString());
    profilerProperties.put(TELEMETRY_INPUT_PATH.getKey(), inputPath);

    BatchProfiler profiler = new BatchProfiler();
    profiler.run(spark, profilerProperties, getGlobals(), readerProperties, fromJSON(profileJson));

    validateProfiles();
  }

  @Test
  public void testBatchProfilerWithCSV() throws Exception {
    // re-write the test data as a CSV with a header record
    String pathToCSV = tempFolder.getRoot().getAbsolutePath();
    spark.read()
            .format("text")
            .load("src/test/resources/telemetry.json")
            .as(Encoders.STRING())
            .write()
            .mode("overwrite")
            .option("header", "true")
            .format("csv")
            .save(pathToCSV);

    // tell the profiler to use the CSV input data
    // CSV is an example of needing to define both the reader and the input format
    profilerProperties.put(TELEMETRY_INPUT_PATH.getKey(), pathToCSV);
    profilerProperties.put(TELEMETRY_INPUT_READER.getKey(), "text");
    profilerProperties.put(TELEMETRY_INPUT_FORMAT.getKey(), "csv");

    // set a reader property; tell the reader to expect a header
    readerProperties.put("header", "true");

    BatchProfiler profiler = new BatchProfiler();
    profiler.run(spark, profilerProperties, getGlobals(), readerProperties, fromJSON(profileJson));

    validateProfiles();
  }

  @Test
  public void testBatchProfilerWithEndTimeConstraint() throws Exception {
    // the input telemetry is text/json stored in the local filesystem
    profilerProperties.put(TELEMETRY_INPUT_PATH.getKey(), "src/test/resources/telemetry.json");
    profilerProperties.put(TELEMETRY_INPUT_FORMAT.getKey(), "text");

    // there are 40 messages before "2018-07-07T15:51:48Z" in the test data
    profilerProperties.put(TELEMETRY_INPUT_BEGIN.getKey(), "");
    profilerProperties.put(TELEMETRY_INPUT_END.getKey(), "2018-07-07T15:51:48Z");

    BatchProfiler profiler = new BatchProfiler();
    profiler.run(spark, profilerProperties, getGlobals(), readerProperties, fromJSON(profileJson));

    // the max timestamp in the data is around July 7, 2018
    assign("maxTimestamp", "1530978728982L");

    // the 'window' looks up to 5 hours before the max timestamp
    assign("window", "PROFILE_WINDOW('from 5 hours ago', maxTimestamp)");

    assertTrue(execute("[12] == PROFILE_GET('count-by-ip', '192.168.66.1', window)", Boolean.class));
    assertTrue(execute("[28] == PROFILE_GET('count-by-ip', '192.168.138.158', window)", Boolean.class));
    assertTrue(execute("[40] == PROFILE_GET('total-count', 'total', window)", Boolean.class));
  }

  @Test
  public void testBatchProfilerWithBeginTimeConstraint() throws Exception {
    // the input telemetry is text/json stored in the local filesystem
    profilerProperties.put(TELEMETRY_INPUT_PATH.getKey(), "src/test/resources/telemetry.json");
    profilerProperties.put(TELEMETRY_INPUT_FORMAT.getKey(), "text");

    // there are 60 messages after "2018-07-07T15:51:48Z" in the test data
    profilerProperties.put(TELEMETRY_INPUT_BEGIN.getKey(), "2018-07-07T15:51:48Z");
    profilerProperties.put(TELEMETRY_INPUT_END.getKey(), "");

    BatchProfiler profiler = new BatchProfiler();
    profiler.run(spark, profilerProperties, getGlobals(), readerProperties, fromJSON(profileJson));

    // the max timestamp in the data is around July 7, 2018
    assign("maxTimestamp", "1530978728982L");

    // the 'window' looks up to 5 hours before the max timestamp
    assign("window", "PROFILE_WINDOW('from 5 hours ago', maxTimestamp)");

    assertTrue(execute("[14] == PROFILE_GET('count-by-ip', '192.168.66.1', window)", Boolean.class));
    assertTrue(execute("[46] == PROFILE_GET('count-by-ip', '192.168.138.158', window)", Boolean.class));
    assertTrue(execute("[60] == PROFILE_GET('total-count', 'total', window)", Boolean.class));
  }

  /**
   * {
   *   "timestampField": "timestamp",
   *   "profiles": [
   *      {
   *        "profile": "count-by-ip",
   *        "foreach": "ip_src_addr",
   *        "init": { "count": 0 },
   *        "update": { "count" : "count + 1" },
   *        "result": "count"
   *      },
   *      {
   *        "profile": "invalid-profile",
   *        "foreach": "'total'",
   *        "init": { "count": 0 },
   *        "update": { "count": "count + 1" },
   *        "result": "INVALID_FUNCTION(count)"
   *      }
   *   ]
   * }
   */
  @Multiline
  private static String invalidProfileJson;

  @Test(expected = SparkException.class)
  public void testBatchProfilerWithInvalidProfile() throws Exception {
    profilerProperties.put(TELEMETRY_INPUT_READER.getKey(), JSON.toString());
    profilerProperties.put(TELEMETRY_INPUT_PATH.getKey(), "src/test/resources/telemetry.json");

    // the batch profiler should error out, if there is a bug in *any* of the profiles
    BatchProfiler profiler = new BatchProfiler();
    profiler.run(spark, profilerProperties, getGlobals(), readerProperties, fromJSON(invalidProfileJson));
  }

  /**
    * {
    *   "timestampField": "timestamp",
    *   "profiles": [
    *      {
    *        "profile": "count-by-ip",
    *        "foreach": "ip_src_addr",
    *        "init": { "count": "STATS_INIT()" },
    *        "update": { "count" : "STATS_ADD(count, 1)" },
    *        "result": "TO_INTEGER(STATS_COUNT(count))"
    *      },
    *      {
    *        "profile": "total-count",
    *        "foreach": "'total'",
    *        "init": { "count": "STATS_INIT()" },
    *        "update": { "count": "STATS_ADD(count, 1)" },
    *        "result": "TO_INTEGER(STATS_COUNT(count))"
    *      }
    *   ]
    * }
    */
  @Multiline
  private static String statsProfileJson;

  @Test
  public void testBatchProfilerWithStatsFunctions() throws Exception {
    profilerProperties.put(TELEMETRY_INPUT_READER.getKey(), JSON.toString());
    profilerProperties.put(TELEMETRY_INPUT_PATH.getKey(), "src/test/resources/telemetry.json");

    BatchProfiler profiler = new BatchProfiler();
    profiler.run(spark, profilerProperties, getGlobals(), readerProperties, fromJSON(statsProfileJson));

    // the profiles do the exact same counting, but using the STATS functions
    validateProfiles();
  }

  /**
   * Validates the profiles that were built.
   *
   * These tests use the Batch Profiler to seed two profiles with archived telemetry.  The first profile
   * called 'count-by-ip', counts the number of messages by 'ip_src_addr'.  The second profile called
   * 'total-count', counts the total number of messages.
   */
  private void validateProfiles() {
    // the max timestamp in the data is around July 7, 2018
    assign("maxTimestamp", "1530978728982L");

    // the 'window' looks up to 5 hours before the max timestamp
    assign("window", "PROFILE_WINDOW('from 5 hours ago', maxTimestamp)");

    // there are 26 messages where ip_src_addr = 192.168.66.1
    assertTrue(execute("[26] == PROFILE_GET('count-by-ip', '192.168.66.1', window)", Boolean.class));

    // there are 74 messages where ip_src_addr = 192.168.138.158
    assertTrue(execute("[74] == PROFILE_GET('count-by-ip', '192.168.138.158', window)", Boolean.class));

    // there are 100 messages in all
    assertTrue(execute("[100] == PROFILE_GET('total-count', 'total', window)", Boolean.class));
  }

  private Properties getGlobals() {
    return new Properties();
  }

  /**
   * Assign a value to the result of an expression.
   *
   * @param var The variable to assign.
   * @param expression The expression to execute.
   */
  private void assign(String var, String expression) {
    executor.assign(var, expression, Collections.emptyMap());
  }

  /**
   * Execute a Stellar expression.
   *
   * @param expression The Stellar expression to execute.
   * @param clazz
   * @param <T>
   * @return The result of executing the Stellar expression.
   */
  private <T> T execute(String expression, Class<T> clazz) {
    T results = executor.execute(expression, Collections.emptyMap(), clazz);
    LOG.debug("{} = {}", expression, results);
    return results;
  }
}
