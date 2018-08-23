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
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_COLUMN_FAMILY;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_TABLE_NAME;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_TABLE_PROVIDER;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.TELEMETRY_INPUT_FORMAT;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.TELEMETRY_INPUT_PATH;
import static org.junit.Assert.assertEquals;

public class BatchProfilerIntegrationTest {

  private static SparkSession spark;
  private MockHTable profilerTable;
  private Properties profilerProperties;

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
  }

  @Test
  public void testBatchProfiler() {

    // run the batch profiler
    BatchProfiler profiler = new BatchProfiler();
    profiler.run(spark, profilerProperties, getGlobals(), getProfile());

    List<Put> puts = profilerTable.getPutLog();
    assertEquals(2, puts.size());
  }


  private ProfilerConfig getProfile() {
    ProfileConfig profile = new ProfileConfig()
            .withProfile("profile1")
            .withForeach("ip_src_addr")
            .withUpdate("count", "count + 1")
            .withResult("count");
    return new ProfilerConfig()
            .withProfile(profile);
  }

  private Properties getGlobals() {
    return new Properties();
  }
}
