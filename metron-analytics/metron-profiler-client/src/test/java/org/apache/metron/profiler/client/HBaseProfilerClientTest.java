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

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.utils.SerDeUtils;
import org.apache.metron.hbase.client.HBaseTableClient;
import org.apache.metron.hbase.client.HBaseClient;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.ProfilePeriod;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link HBaseProfilerClient}.
 */
public class HBaseProfilerClientTest {
  private static final String tableName = "profiler";
  private static final String columnFamily = "P";
  private static final byte[] columnFamilyB = Bytes.toBytes(columnFamily);
  private static final byte[] columnQualifier = Bytes.toBytes("column");
  private static final long periodDuration = 15;
  private static final TimeUnit periodUnits = TimeUnit.MINUTES;
  private static final int periodsPerHour = 4;
  private static final byte[] expectedRowKey = Bytes.toBytes("some-row-key");
  private static final String profileName = "profile1";
  private static final String entityName = "entity1";
  private static final int profileValue = 1231121;
  private static final byte[] profileValueB = SerDeUtils.toBytes(profileValue);
  private long periodDurationMillis = periodUnits.toMillis(periodDuration);

  private HBaseClient hbaseClient;
  private HBaseProfilerClient profilerClient;
  private ProfileMeasurement expected;
  private RowKeyBuilder rowKeyBuilder;
  private ColumnBuilder columnBuilder;
  private Result expectedResult;
  private Result emptyResult;

  @Before
  public void setup() {
    // create a profile measurement used in the tests
    expected = new ProfileMeasurement()
            .withProfileName(profileName)
            .withEntity(entityName)
            .withPeriod(System.currentTimeMillis(), 5, TimeUnit.MINUTES)
            .withProfileValue(profileValue);

    // mock row key builder needs to return a row key for the profile measurement used in the tests
    rowKeyBuilder = mock(RowKeyBuilder.class);
    when(rowKeyBuilder.rowKey(any())).thenReturn(expectedRowKey);

    // mock column builder - column family/qualifier comes from the column builder
    columnBuilder = mock(ColumnBuilder.class);
    when(columnBuilder.getColumnFamily()).thenReturn(columnFamily);
    when(columnBuilder.getColumnQualifier(eq("value"))).thenReturn(columnQualifier);

    // this mock is used to feed data to the profiler client while testing
    hbaseClient = mock(HBaseTableClient.class);

    // a result that matches the expected profile measurement that can be return by the mock hbase client
    expectedResult = mock(Result.class);
    when(expectedResult.containsColumn(eq(columnFamilyB), eq(columnQualifier))).thenReturn(true);
    when(expectedResult.getValue(eq(columnFamilyB), eq(columnQualifier))).thenReturn(profileValueB);

    // an empty result to use in the tests
    emptyResult = mock(Result.class);
    when(emptyResult.containsColumn(any(), any())).thenReturn(false);

    // create the profiler client that will be tested
    profilerClient = new HBaseProfilerClient(hbaseClient, rowKeyBuilder, columnBuilder, periodDurationMillis);
  }

  @Test
  public void shouldFetchProfileMeasurement() {
    // need the hbase client to return a Result matching the expected profile measurement value
    Result[] results = new Result[] { expectedResult };
    when(hbaseClient.getAll()).thenReturn(results);

    List<ProfileMeasurement> measurements = profilerClient.fetch(
            Object.class,
            expected.getProfileName(),
            expected.getEntity(),
            expected.getGroups(),
            Arrays.asList(expected.getPeriod()),
            Optional.empty());
    assertEquals(1, measurements.size());
    assertEquals(expected, measurements.get(0));
  }

  @Test
  public void shouldFetchNothingWhenNothingThere() {
    // the hbase client will indicate their are no hits
    Result[] results = new Result[] { emptyResult };
    when(hbaseClient.getAll()).thenReturn(results);

    List<ProfileMeasurement> measurements = profilerClient.fetch(
            Object.class,
            expected.getProfileName(),
            expected.getEntity(),
            expected.getGroups(),
            Arrays.asList(expected.getPeriod()),
            Optional.empty());
    assertEquals(0, measurements.size());
  }

  @Test
  public void shouldFetchDefaultValueWhenNothingThere() {
    // the hbase client will indicate their are no hits
    Result[] results = new Result[] { emptyResult };
    when(hbaseClient.getAll()).thenReturn(results);

    List<ProfileMeasurement> measurements = profilerClient.fetch(
            Object.class,
            expected.getProfileName(),
            expected.getEntity(),
            expected.getGroups(),
            Arrays.asList(expected.getPeriod()),
            Optional.of(profileValue));

    // expect the default value to be returned
    assertEquals(1, measurements.size());
    assertEquals(expected, measurements.get(0));
  }

  @Test
  public void shouldFetchMultipleProfilePeriods() {
    // need the hbase client to return a Result matching the expected profile measurement value
    Result[] results = new Result[] { expectedResult, expectedResult, expectedResult, expectedResult };
    when(hbaseClient.getAll()).thenReturn(results);

    // fetching across multiple periods
    ProfilePeriod start = ProfilePeriod.fromPeriodId(1L, 15L, TimeUnit.MINUTES);
    List<ProfilePeriod> periods = new ArrayList<ProfilePeriod>() {{
      add(start);
      add(start.next());
      add(start.next());
      add(start.next());
    }};

    List<ProfileMeasurement> measurements = profilerClient.fetch(
            Object.class,
            expected.getProfileName(),
            expected.getEntity(),
            expected.getGroups(),
            periods,
            Optional.empty());

    // the row key builder should be called once for each profile period
    ArgumentCaptor<ProfileMeasurement> captor = new ArgumentCaptor<>();
    verify(rowKeyBuilder, times(4)).rowKey(captor.capture());

    // the profile periods should match those originally submited
    List<ProfileMeasurement> submitted = captor.getAllValues();
    assertEquals(periods.get(0), submitted.get(0).getPeriod());
    assertEquals(periods.get(1), submitted.get(1).getPeriod());
    assertEquals(periods.get(2), submitted.get(2).getPeriod());
    assertEquals(periods.get(3), submitted.get(3).getPeriod());

    assertEquals(4, measurements.size());
  }

  @Test
  public void shouldCloseHBaseClient() throws IOException {
    profilerClient.close();
    verify(hbaseClient, times(1)).close();
  }
}