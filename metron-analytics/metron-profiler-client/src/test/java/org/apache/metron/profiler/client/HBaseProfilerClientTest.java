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
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    // writes values to be read during testing
    long periodDurationMillis = periodUnits.toMillis(periodDuration);
    RowKeyBuilder rowKeyBuilder = new SaltyRowKeyBuilder();
    ColumnBuilder columnBuilder = new ValueOnlyColumnBuilder(columnFamily);
    profileWriter = new ProfileWriter(rowKeyBuilder, columnBuilder, table, periodDurationMillis);

    client = new HBaseProfilerClient(table, rowKeyBuilder, columnBuilder, periodDurationMillis);
  }

  @After
  public void tearDown() throws Exception {
    table.clear();
  }

  @Test
  public void Should_ReturnMeasurements_When_DataExistsForAGroup() throws Exception {
    final String profile = "profile1";
    final String entity = "entity1";
    final int expectedValue = 2302;
    final int hours = 2;
    final int count = hours * periodsPerHour + 1;
    final long startTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours);

    // setup - write two groups of measurements - 'weekends' and 'weekdays'
    ProfileMeasurement prototype = new ProfileMeasurement()
            .withProfileName(profile)
            .withEntity(entity)
            .withPeriod(startTime, periodDuration, periodUnits);
    profileWriter.write(prototype, count, Arrays.asList("weekdays"), val -> expectedValue);
    profileWriter.write(prototype, count, Arrays.asList("weekends"), val -> 0);

    long end = System.currentTimeMillis();
    long start = end - TimeUnit.HOURS.toMillis(2);
    {
      //validate "weekday" results
      List<Object> groups = Arrays.asList("weekdays");
      List<ProfileMeasurement> results = client.fetch(Integer.class, profile, entity, groups, start, end, Optional.empty());
      assertEquals(count, results.size());
      results.forEach(actual -> {
        assertEquals(profile, actual.getProfileName());
        assertEquals(entity, actual.getEntity());
        assertEquals(groups, actual.getGroups());
        assertEquals(expectedValue, actual.getProfileValue());
      });
    }
    {
      //validate "weekend" results
      List<Object> groups = Arrays.asList("weekends");
      List<ProfileMeasurement> results = client.fetch(Integer.class, profile, entity, groups, start, end, Optional.empty());
      assertEquals(count, results.size());
      results.forEach(actual -> {
        assertEquals(profile, actual.getProfileName());
        assertEquals(entity, actual.getEntity());
        assertEquals(groups, actual.getGroups());
        assertEquals(0, actual.getProfileValue());
      });
    }
  }

  @Test
  public void Should_ReturnResultFromGroup_When_MultipleGroupsExist() throws Exception {
    final String profile = "profile1";
    final String entity = "entity1";
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

    List<Object> weekdays = Arrays.asList("weekdays");
    List<ProfileMeasurement> results = client.fetch(Integer.class, profile, entity, weekdays, startTime, endTime, Optional.empty());

    // should only return results from 'weekdays' group
    assertEquals(count, results.size());
    results.forEach(actual -> assertEquals(weekdays, actual.getGroups()));
  }

  @Test
  public void Should_ReturnNoResults_When_GroupDoesNotExist() {
    final String profile = "profile1";
    final String entity = "entity1";
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

    // should return no results when the group does not exist
    List<Object> groups = Arrays.asList("does-not-exist");
    List<ProfileMeasurement> results = client.fetch(Integer.class, profile, entity, groups, startTime, endTime, Optional.empty());
    assertEquals(0, results.size());
  }

  @Test
  public void Should_ReturnNoResults_When_NoDataInStartToEnd() throws Exception {
    final String profile = "profile1";
    final String entity = "entity1";
    final int hours = 2;
    int numberToWrite = hours * periodsPerHour;
    final List<Object> group = Arrays.asList("weekends");
    final long measurementTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);

    // write some data with a timestamp of s1 day ago
    ProfileMeasurement prototype = new ProfileMeasurement()
            .withProfileName("profile1")
            .withEntity("entity1")
            .withPeriod(measurementTime, periodDuration, periodUnits);
    profileWriter.write(prototype, numberToWrite, group, val -> 1000);

    // should return no results when [start,end] is long after when test data was written
    final long endFetchAt = System.currentTimeMillis();
    final long startFetchAt = endFetchAt - TimeUnit.MILLISECONDS.toMillis(30);
    List<ProfileMeasurement> results = client.fetch(Integer.class, profile, entity, group, startFetchAt, endFetchAt, Optional.empty());
    assertEquals(0, results.size());
  }
}