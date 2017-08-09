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

import org.apache.metron.hbase.mock.MockHTable;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.profiler.hbase.SaltyRowKeyBuilder;
import org.apache.metron.profiler.hbase.ValueOnlyColumnBuilder;
import org.apache.metron.stellar.common.DefaultStellarStatefulExecutor;
import org.apache.metron.stellar.common.StellarStatefulExecutor;
import org.junit.After;
import org.junit.Before;
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
  private static final long periodDuration = 15;
  private static final TimeUnit periodUnits = TimeUnit.MINUTES;
  private static final int periodsPerHour = 4;

  private HBaseProfilerClient client;
  private StellarStatefulExecutor executor;
  private MockHTable table;
  private ProfileWriter profileWriter;

  @Before
  public void setup() throws Exception {

    table = new MockHTable(tableName, columnFamily);
    executor = new DefaultStellarStatefulExecutor();

    // used to write values to be read during testing
    RowKeyBuilder rowKeyBuilder = new SaltyRowKeyBuilder();
    ColumnBuilder columnBuilder = new ValueOnlyColumnBuilder(columnFamily);
    profileWriter = new ProfileWriter(rowKeyBuilder, columnBuilder, table);

    // what we're actually testing
    client = new HBaseProfilerClient(table, rowKeyBuilder, columnBuilder);
  }

  @After
  public void tearDown() throws Exception {
    table.clear();
  }

  /**
   * The client should be able to distinguish between groups and only fetch those in the correct group.
   */
  @Test
  public void testFetchWithDurationAgoAndOneGroup() throws Exception {
    final int expectedValue = 2302;
    final int hours = 2;
    final int count = hours * periodsPerHour;
    final long startTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours);

    // setup - write two groups of measurements - 'weekends' and 'weekdays'
    ProfileMeasurement m = new ProfileMeasurement()
            .withProfileName("profile1")
            .withEntity("entity1")
            .withPeriod(startTime, periodDuration, periodUnits);

    profileWriter.write(m, count, Arrays.asList("weekdays"), val -> expectedValue);
    profileWriter.write(m, count, Arrays.asList("weekends"), val -> 0);

    // execute
    List<Integer> results = client.fetch(Integer.class, "profile1", "entity1", Arrays.asList("weekdays"), hours, TimeUnit.HOURS);

    // validate
    assertEquals(count, results.size());
    results.forEach(actual -> assertEquals(expectedValue, (int) actual));
  }

  /**
   * Attempt to fetch a group that does not exist.
   */
  @Test
  public void testFetchWithDurationAgoAndNoGroup() {

    // setup
    final int expectedValue = 2302;
    final int hours = 2;
    final long startTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours);

    // create two groups of measurements - one on weekdays and one on weekends
    ProfileMeasurement m = new ProfileMeasurement()
            .withProfileName("profile1")
            .withEntity("entity1")
            .withPeriod(startTime, periodDuration, periodUnits);
    profileWriter.write(m, hours * periodsPerHour, Arrays.asList("weekdays"), val -> expectedValue);
    profileWriter.write(m, hours * periodsPerHour, Arrays.asList("weekends"), val -> 0);

    // execute
    List<Object> doesNotExist = Arrays.asList("does-not-exist");
    List<Integer> results = client.fetch(Integer.class, "profile1", "entity1", doesNotExist, hours, TimeUnit.HOURS);

    // validate
    assertEquals(0, results.size());
  }

  /**
   * Profile data only within 'milliseconds ago' should be fetched.  Data outside of that time horizon should
   * not be fetched.
   */
  @Test
  public void testFetchWithDurationAgoAndOutsideTimeWindow() throws Exception {
    final int hours = 2;
    final List<Object> group = Arrays.asList("weekends");
    final long startTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);

    // setup - write some values to read later
    ProfileMeasurement m = new ProfileMeasurement()
            .withProfileName("profile1")
            .withEntity("entity1")
            .withPeriod(startTime, periodDuration, periodUnits);
    profileWriter.write(m, hours * periodsPerHour, group, val -> 1000);

    // execute
    List<Integer> results = client.fetch(Integer.class, "profile1", "entity1", group, 2, TimeUnit.MILLISECONDS);

    // validate - there should NOT be any results from just 2 milliseconds ago
    assertEquals(0, results.size());
  }

  /**
   * The client should be able to distinguish between groups and only fetch those in the correct group.
   */
  @Test
  public void testFetchWithStartEndAndOneGroup() throws Exception {
    final int periodsPerHour = 4;
    final int expectedValue = 2302;
    final int hours = 2;
    final int count = hours * periodsPerHour;
    final long endTime = System.currentTimeMillis();
    final long startTime = endTime - TimeUnit.HOURS.toMillis(hours);

    // setup - write two groups of measurements - 'weekends' and 'weekdays'
    ProfileMeasurement m = new ProfileMeasurement()
            .withProfileName("profile1")
            .withEntity("entity1")
            .withPeriod(startTime, periodDuration, periodUnits);
    profileWriter.write(m, count, Arrays.asList("weekdays"), val -> expectedValue);
    profileWriter.write(m, count, Arrays.asList("weekends"), val -> 0);

    // execute
    List<Integer> results = client.fetch(Integer.class, "profile1", "entity1", Arrays.asList("weekdays"), startTime, endTime);

    // validate
    assertEquals(count, results.size());
    results.forEach(actual -> assertEquals(expectedValue, (int) actual));
  }

  /**
   * Attempt to fetch a group that does not exist.
   */
  @Test
  public void testFetchWithStartEndAndNoGroup() {

    // setup
    final int periodsPerHour = 4;
    final int expectedValue = 2302;
    final int hours = 2;
    final int count = hours * periodsPerHour;
    final long endTime = System.currentTimeMillis();
    final long startTime = endTime - TimeUnit.HOURS.toMillis(hours);

    // create two groups of measurements - one on weekdays and one on weekends
    ProfileMeasurement m = new ProfileMeasurement()
            .withProfileName("profile1")
            .withEntity("entity1")
            .withPeriod(startTime, periodDuration, periodUnits);
    profileWriter.write(m, count, Arrays.asList("weekdays"), val -> expectedValue);
    profileWriter.write(m, count, Arrays.asList("weekends"), val -> 0);

    // execute
    List<Object> doesNotExist = Arrays.asList("does-not-exist");
    List<Integer> results = client.fetch(Integer.class, "profile1", "entity1", doesNotExist, startTime, endTime);

    // validate
    assertEquals(0, results.size());
  }

  /**
   * Profile data only within 'milliseconds ago' should be fetched.  Data outside of that time horizon should
   * not be fetched.
   */
  @Test
  public void testFetchWithStartEndAndOutsideTimeWindow() throws Exception {

    final int hours = 2;
    int numberToWrite = hours * periodsPerHour;
    final List<Object> group = Arrays.asList("weekends");
    final long measurementTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);

    // setup - write some values to read later
    ProfileMeasurement m = new ProfileMeasurement()
            .withProfileName("profile1")
            .withEntity("entity1")
            .withPeriod(measurementTime, periodDuration, periodUnits);
    profileWriter.write(m, numberToWrite, group, val -> 1000);

    // execute
    final long endFetchAt = System.currentTimeMillis();
    final long startFetchAt = endFetchAt - TimeUnit.MILLISECONDS.toMillis(30);
    List<Integer> results = client.fetch(Integer.class, "profile1", "entity1", group, startFetchAt, endFetchAt);

    // validate - there should NOT be any results from just 2 milliseconds ago
    assertEquals(0, results.size());
  }
}