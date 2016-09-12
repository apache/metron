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

package org.apache.metron.profiler.client;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.DefaultSerializer;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.profiler.hbase.SaltyRowKeyBuilder;
import org.apache.metron.profiler.hbase.Serializer;
import org.apache.metron.profiler.hbase.ValueOnlyColumnBuilder;
import org.apache.metron.profiler.stellar.DefaultStellarExecutor;
import org.apache.metron.profiler.stellar.StellarExecutor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Tests the HBaseProfilerClient.
 *
 * The naming used in this test attempts to be as similar to how the 'groupBy'
 * functionality might be used 'in the wild'.  This test involves reading and
 * writing two separate groups originating from the same Profile and Entity.
 * There is a 'weekdays' group which contains all measurements taken on weekdays.
 * There is also a 'weekend' group which contains all measurements taken on weekends.
 */
public class HBaseProfilerClientTest {

  private static final String tableName = "profiler";
  private static final String columnFamily = "P";

  private HBaseProfilerClient client;
  private HTableInterface table;
  private StellarExecutor executor;
  private static HBaseTestingUtility util;
  private ProfileWriter profileWriter;

  @BeforeClass
  public static void startHBase() throws Exception {
    Configuration config = HBaseConfiguration.create();
    config.set("hbase.master.hostname", "localhost");
    config.set("hbase.regionserver.hostname", "localhost");
    util = new HBaseTestingUtility(config);
    util.startMiniCluster();
  }

  @AfterClass
  public static void stopHBase() throws Exception {
    util.shutdownMiniCluster();
    util.cleanupTestDir();
  }

  @Before
  public void setup() throws Exception {
    Serializer serializer = new DefaultSerializer();
    table = util.createTable(Bytes.toBytes(tableName), Bytes.toBytes(columnFamily));
    executor = new DefaultStellarExecutor();

    // used to write values to be read during testing
    RowKeyBuilder rowKeyBuilder = new SaltyRowKeyBuilder();
    ColumnBuilder columnBuilder = new ValueOnlyColumnBuilder(columnFamily, serializer);
    profileWriter = new ProfileWriter(rowKeyBuilder, columnBuilder, table);

    // what we're actually testing
    client = new HBaseProfilerClient(table, rowKeyBuilder, columnBuilder, serializer);
  }

  @After
  public void tearDown() throws Exception {
    util.deleteTable(tableName);
  }

  /**
   * The client should be able to distinguish between groups and only fetch those in the correct group.
   */
  @Test
  public void testFetchOneGroup() throws Exception {
    final int periodsPerHour = 4;
    final int expectedValue = 2302;
    final int hours = 2;
    final int count = hours * periodsPerHour;
    final long startTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours);

    // setup - write two groups of measurements - 'weekends' and 'weekdays'
    ProfileMeasurement m = new ProfileMeasurement("profile1", "entity1", startTime, periodsPerHour);
    profileWriter.write(m, count, Arrays.asList("weekdays"), val -> expectedValue);
    profileWriter.write(m, count, Arrays.asList("weekends"), val -> 0);

    // execute
    List<Integer> results = client.fetch("profile1", "entity1", hours, TimeUnit.HOURS, Integer.class, Arrays.asList("weekdays"));

    // validate
    assertEquals(count, results.size());
    results.forEach(actual -> assertEquals(expectedValue, (int) actual));
  }

  /**
   * Attempt to fetch a group that does not exist.
   */
  @Test
  public void testFetchNoGroup() {

    // setup
    final int periodsPerHour = 4;
    final int expectedValue = 2302;
    final int hours = 2;
    final int count = hours * periodsPerHour;
    final long startTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours);

    // create two groups of measurements - one on weekdays and one on weekends
    ProfileMeasurement m = new ProfileMeasurement("profile1", "entity1", startTime, periodsPerHour);
    profileWriter.write(m, count, Arrays.asList("weekdays"), val -> expectedValue);
    profileWriter.write(m, count, Arrays.asList("weekends"), val -> 0);

    // execute
    List<Object> doesNotExist = Arrays.asList("does-not-exist");
    List<Integer> results = client.fetch("profile1", "entity1", hours, TimeUnit.HOURS, Integer.class, doesNotExist);

    // validate
    assertEquals(0, results.size());
  }

  /**
   * Profile data only within 'milliseconds ago' should be fetched.  Data outside of that time horizon should
   * not be fetched.
   */
  @Test
  public void testFetchOutsideTimeWindow() throws Exception {
    final int periodsPerHour = 4;
    final int hours = 2;
    int numberToWrite = hours * periodsPerHour;
    final List<Object> group = Arrays.asList("weekends");
    final long startTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);

    // setup - write some values to read later
    ProfileMeasurement m = new ProfileMeasurement("profile1", "entity1", startTime, periodsPerHour);
    profileWriter.write(m, numberToWrite, group, val -> 1000);

    // execute
    List<Integer> results = client.fetch("profile1", "entity1", 2, TimeUnit.MILLISECONDS, Integer.class, group);

    // validate - there should NOT be any results from just 2 milliseconds ago
    assertEquals(0, results.size());
  }
}
