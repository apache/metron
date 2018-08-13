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

import org.apache.hadoop.hbase.client.Put;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.hbase.mock.MockHTable;
import org.apache.metron.profiler.client.stellar.FixedLookback;
import org.apache.metron.profiler.client.stellar.GetProfile;
import org.apache.metron.profiler.client.stellar.ProfilerClientConfig;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_COLUMN_FAMILY;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_TABLE_NAME;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_TABLE_PROVIDER;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.TELEMETRY_INPUT_FORMAT;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.TELEMETRY_INPUT_PATH;
import static org.junit.Assert.assertEquals;

import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.*;

/**
 * An integration test for the {@link BatchProfiler}.
 */
public class BatchProfilerIntegrationTest {

  private static SparkSession spark;
  private MockHTable profilerTable;
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

    // define the source of the input telemetry
    profilerProperties.put(TELEMETRY_INPUT_PATH.getKey(), "src/test/resources/telemetry.json");
    profilerProperties.put(TELEMETRY_INPUT_FORMAT.getKey(), "text");

    // define where the output will go
    String tableName = HBASE_TABLE_NAME.get(profilerProperties, String.class);
    String columnFamily = HBASE_COLUMN_FAMILY.get(profilerProperties, String.class);
    profilerProperties.put(HBASE_TABLE_PROVIDER.getKey(), MockHBaseTableProvider.class.getName());

    // create the mock hbase table
    profilerTable = (MockHTable) MockHBaseTableProvider.addToCache(tableName, columnFamily);

    // global properties
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

  @Test
  public void testBatchProfiler() {

    // the 'window' over which values are looked-up needs to match the timestamps contained in the data
    assign("maxTimestamp", "1530978728982L");
    assign("window", "PROFILE_WINDOW('5 hours', maxTimestamp)");

    // run the batch profiler
    BatchProfiler profiler = new BatchProfiler();
    profiler.run(spark, profilerProperties, getGlobals(), getProfile());

    // validate the values written by the batch profiler using `PROFILE_GET`
    {
      // there are 26 messages where ip_src_addr = 192.168.66.1
      List<Integer> result = execute("PROFILE_GET('counter', '192.168.66.1', window)", List.class);
      assertEquals(1, result.size());
      assertEquals(26, result.get(0).intValue());
    }
    {
      // there are 74 messages where ip_src_addr = 192.168.138.158
      List<Integer> result = execute("PROFILE_GET('counter', '192.168.138.158', window)", List.class);
      assertEquals(1, result.size());
      assertEquals(74, result.get(0).intValue());
    }
  }

  private ProfilerConfig getProfile() {
    ProfileConfig profile = new ProfileConfig()
            .withProfile("counter")
            .withForeach("ip_src_addr")
            .withUpdate("count", "count + 1")
            .withResult("count");
    return new ProfilerConfig()
            .withProfile(profile)
            .withTimestampField(Optional.of("timestamp"));
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
