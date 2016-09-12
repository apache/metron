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
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.hbase.client.HBaseClient;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.ProfilePeriod;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.SaltyRowKeyBuilder;
import org.apache.metron.profiler.hbase.ValueOnlyColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.profiler.stellar.DefaultStellarExecutor;
import org.apache.metron.profiler.stellar.StellarExecutor;
import org.apache.storm.hbase.common.ColumnList;
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
 * The naming used in this test attempts to be as similar to how the 'groupBy' functionality might be used 'in
 * the wild'.  This test involves reading and writing two separate groups originating from the same Profile and
 * Entity.  There is a 'weekdays' group which contains all measurements taken on weekdays.  There is also a
 * 'weekend' group which contains all measurements taken on weekends.
 */
public class HBaseProfilerClientTest {

  private static final String tableName = "profiler";
  private static final String columnFamily = "P";

  private HBaseProfilerClient client;
  private HBaseClient hbaseClient;
  private RowKeyBuilder rowKeyBuilder;
  private ColumnBuilder columnBuilder;
  private HTableInterface table;
  private StellarExecutor executor;
  private static HBaseTestingUtility util;

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

    // setup all of the necessary dependencies
    table = util.createTable(Bytes.toBytes(tableName), Bytes.toBytes(columnFamily));
    hbaseClient = new HBaseClient((c,t) -> table, table.getConfiguration(), tableName);
    executor = new DefaultStellarExecutor();
    rowKeyBuilder = new SaltyRowKeyBuilder();
    columnBuilder = new ValueOnlyColumnBuilder(columnFamily);

    // what we're actually testing
    client = new HBaseProfilerClient(table, rowKeyBuilder, columnBuilder);
  }

  /**
   * Writes profile measurements that can be used for testing.
   * @param count The number of profile measurements to write.
   * @param profileName Name of the profile.
   * @param entityName Name of the entity.
   * @param value The measurement value.
   * @param periodsPerHour Number of profile periods per hour.
   * @param startTime When measurements should start to be written.
   * @param group The name of the group.
   */
  private void writeMeasurements(int count, String profileName, String entityName, int value, int periodsPerHour, long startTime, List<Object> group) {

    // create the first measurement
    ProfileMeasurement m = new ProfileMeasurement(profileName, entityName, startTime, periodsPerHour);
    m.setValue(value);

    for(int i=0; i<count; i++) {

      // create a measurement for the next profile period
      ProfilePeriod next = m.getPeriod().next();
      m = new ProfileMeasurement(profileName, entityName, next.getTimeInMillis(), periodsPerHour);
      m.setValue(value);

      // write the measurement
      write(m, group);
    }
  }

  @After
  public void tearDown() throws Exception {
    util.deleteTable(tableName);
  }

  /**
   * Write a ProfileMeasurement.
   * @param m The ProfileMeasurement to write.
   * @param groups The groups to use when writing the ProfileMeasurement.
   */
  private void write(ProfileMeasurement m, List<Object> groups) {

    byte[] rowKey = rowKeyBuilder.rowKey(m, groups);
    ColumnList cols = columnBuilder.columns(m);

    List<Mutation> mutations = hbaseClient.constructMutationReq(rowKey, cols, Durability.SKIP_WAL);
    hbaseClient.batchMutate(mutations);
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
    writeMeasurements(count, "profile1", "entity1", expectedValue, periodsPerHour, startTime, Arrays.asList("weekdays"));
    writeMeasurements(count, "profile1", "entity1", 0, periodsPerHour, startTime, Arrays.asList("weekends"));

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
    writeMeasurements(count, "profile1", "entity1", expectedValue, periodsPerHour, startTime, Arrays.asList("weekdays"));
    writeMeasurements(count, "profile1", "entity1", 0, periodsPerHour, startTime, Arrays.asList("weekends"));

    // execute
    List<Integer> results = client.fetch("profile1", "entity1", hours, TimeUnit.HOURS, Integer.class, Arrays.asList("does-not-exist"));

    // validate
    assertEquals(0, results.size());
  }

  /**
   * Profile data only within 'milliseconds ago' should be fetched.  Data outside of that time horizon should
   * not be fetched.
   */
  @Test
  public void testFetchOutsideTimeWindow() throws Exception {

    // setup - create some measurement values from a day ago
    final int periodsPerHour = 4;
    final int hours = 2;
    final List<Object> group = Arrays.asList("weekends");
    final long startTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
    writeMeasurements(hours * periodsPerHour, "profile1", "entity1", 1000, 4, startTime, group);

    // execute
    List<Integer> results = client.fetch("profile1", "entity1", 2, TimeUnit.MILLISECONDS, Integer.class, group);

    // validate - there should NOT be any results from just 2 milliseconds ago
    assertEquals(0, results.size());
  }
}
