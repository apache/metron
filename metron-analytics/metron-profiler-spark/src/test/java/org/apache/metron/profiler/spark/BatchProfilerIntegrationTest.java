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
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.profiler.client.stellar.FixedLookback;
import org.apache.metron.profiler.client.stellar.GetProfile;
import org.apache.metron.profiler.client.stellar.WindowLookback;
import org.apache.metron.stellar.common.DefaultStellarStatefulExecutor;
import org.apache.metron.stellar.common.StellarStatefulExecutor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.functions.resolver.SimpleFunctionResolver;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_COLUMN_FAMILY;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_HBASE_TABLE;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_HBASE_TABLE_PROVIDER;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_COLUMN_FAMILY;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_TABLE_NAME;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_TABLE_PROVIDER;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.TELEMETRY_INPUT_FORMAT;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.TELEMETRY_INPUT_PATH;
import static org.junit.Assert.assertTrue;

/**
 * An integration test for the {@link BatchProfiler}.
 */
public class BatchProfilerIntegrationTest {

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
  private static SparkSession spark;
  private Properties profilerProperties;
  private StellarStatefulExecutor executor;

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
    profilerProperties = new Properties();

    // the input telemetry is read from the local filesystem
    profilerProperties.put(TELEMETRY_INPUT_PATH.getKey(), "src/test/resources/telemetry.json");
    profilerProperties.put(TELEMETRY_INPUT_FORMAT.getKey(), "text");

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
   * This test uses the Batch Profiler to seed two profiles using archived telemetry.
   *
   * The first profile counts the number of messages by 'ip_src_addr'.  The second profile counts the total number
   * of messages.
   *
   * The archived telemetry contains timestamps from around July 7, 2018.  All of the measurements
   * produced will center around this date.
   */
  @Test
  public void testBatchProfiler() throws Exception {
    // run the batch profiler
    BatchProfiler profiler = new BatchProfiler();
    profiler.run(spark, profilerProperties, getGlobals(), getProfile());

    // validate the measurements written by the batch profiler using `PROFILE_GET`
    // the 'window' looks up to 5 hours before the last timestamp contained in the telemetry
    assign("lastTimestamp", "1530978728982L");
    assign("window", "PROFILE_WINDOW('from 5 hours ago', lastTimestamp)");

    // there are 26 messages where ip_src_addr = 192.168.66.1
    assertTrue(execute("[26] == PROFILE_GET('count-by-ip', '192.168.66.1', window)", Boolean.class));

    // there are 74 messages where ip_src_addr = 192.168.138.158
    assertTrue(execute("[74] == PROFILE_GET('count-by-ip', '192.168.138.158', window)", Boolean.class));

    // there are 100 messages in all
    assertTrue(execute("[100] == PROFILE_GET('total-count', 'total', window)", Boolean.class));
  }

  private ProfilerConfig getProfile() throws IOException {
    return ProfilerConfig.fromJSON(profileJson);
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
    return executor.execute(expression, Collections.emptyMap(), clazz);
  }
}
