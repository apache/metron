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

import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.FunctionResolverSingleton;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.profiler.hbase.SaltyRowKeyBuilder;
import org.apache.metron.profiler.hbase.ValueOnlyColumnBuilder;
import org.apache.metron.profiler.stellar.DefaultStellarExecutor;
import org.apache.metron.profiler.stellar.StellarExecutor;
import org.apache.metron.test.mock.MockHTable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.metron.common.dsl.Context.Capabilities.PROFILER_HBASE_TABLE;
import static org.apache.metron.common.dsl.Context.Capabilities.PROFILER_ROW_KEY_BUILDER;
import static org.apache.metron.common.dsl.Context.Capabilities.PROFILER_COLUMN_BUILDER;

/**
 * Tests the GetProfile class.
 */
public class GetProfileTest {

  private static final String tableName = "profiler";
  private static final String columnFamily = "P";
  private StellarExecutor executor;
  private Map<String, Object> state;
  private ProfileWriter profileWriter;

  private <T> T run(String expression, Class<T> clazz) {
    return executor.execute(expression, state, clazz);
  }

  @Before
  public void setup() {
    state = new HashMap<>();
    final HTableInterface table = new MockHTable(tableName, columnFamily);

    // used to write values to be read during testing
    RowKeyBuilder rowKeyBuilder = new SaltyRowKeyBuilder();
    ColumnBuilder columnBuilder = new ValueOnlyColumnBuilder(columnFamily);
    profileWriter = new ProfileWriter(rowKeyBuilder, columnBuilder, table);

    // create the necessary context used during initialization
    Context context = new Context.Builder()
            .with(PROFILER_ROW_KEY_BUILDER, () -> new SaltyRowKeyBuilder())
            .with(PROFILER_COLUMN_BUILDER, () -> new ValueOnlyColumnBuilder(columnFamily))
            .with(PROFILER_HBASE_TABLE, () -> table)
            .build();

    executor = new DefaultStellarExecutor();
    executor.setContext(context);

    // force re-initialization before each test
    FunctionResolverSingleton.getInstance().reset();
  }

  /**
   * Values should be retrievable that have NOT been stored within a group.
   */
  @Test
  public void testWithNoGroups() {
    final int periodsPerHour = 4;
    final int expectedValue = 2302;
    final int hours = 2;
    final long startTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours);
    final List<Object> group = Collections.emptyList();

    // setup - write some measurements to be read later
    final int count = hours * periodsPerHour;
    ProfileMeasurement m = new ProfileMeasurement("profile1", "entity1", startTime, periodsPerHour);
    profileWriter.write(m, count, group, val -> expectedValue);

    // execute - read the profile values - no groups
    String expr = "PROFILE_GET('profile1', 'entity1', 4, 'HOURS')";
    List<Integer> result = run(expr, List.class);

    // validate - expect to read all values from the past 4 hours
    Assert.assertEquals(count, result.size());
  }

  /**
   * Values should be retrievable that have been stored within a 'group'.
   */
  @Test
  public void testWithOneGroup() {
    final int periodsPerHour = 4;
    final int expectedValue = 2302;
    final int hours = 2;
    final long startTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours);
    final List<Object> group = Arrays.asList("weekends");

    // setup - write some measurements to be read later
    final int count = hours * periodsPerHour;
    ProfileMeasurement m = new ProfileMeasurement("profile1", "entity1", startTime, periodsPerHour);
    profileWriter.write(m, count, group, val -> expectedValue);

    // create a variable that contains the groups to use
    state.put("groups", group);

    // execute - read the profile values
    String expr = "PROFILE_GET('profile1', 'entity1', 4, 'HOURS', 'weekends')";
    List<Integer> result = run(expr, List.class);

    // validate - expect to read all values from the past 4 hours
    Assert.assertEquals(count, result.size());
  }

  /**
   * Values should be retrievable that have been stored within a 'group'.
   */
  @Test
  public void testWithTwoGroups() {
    final int periodsPerHour = 4;
    final int expectedValue = 2302;
    final int hours = 2;
    final long startTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours);
    final List<Object> group = Arrays.asList("weekdays", "tuesday");

    // setup - write some measurements to be read later
    final int count = hours * periodsPerHour;
    ProfileMeasurement m = new ProfileMeasurement("profile1", "entity1", startTime, periodsPerHour);
    profileWriter.write(m, count, group, val -> expectedValue);

    // create a variable that contains the groups to use
    state.put("groups", group);

    // execute - read the profile values
    String expr = "PROFILE_GET('profile1', 'entity1', 4, 'HOURS', 'weekdays', 'tuesday')";
    List<Integer> result = run(expr, List.class);

    // validate - expect to read all values from the past 4 hours
    Assert.assertEquals(count, result.size());
  }

  /**
   * Initialization should fail if the required context values are missing.
   */
  @Test(expected = ParseException.class)
  public void testMissingContext() {
    Context empty = Context.EMPTY_CONTEXT();

    // 'unset' the context that was created during setup()
    executor.setContext(empty);

    // force re-initialization with no context
    FunctionResolverSingleton.getInstance().initialize(empty);

    // validate - function should be unable to initialize
    String expr = "PROFILE_GET('profile1', 'entity1', 1000, 'SECONDS', groups)";
    run(expr, List.class);
  }

  /**
   * If the time horizon specified does not include any profile measurements, then
   * none should be returned.
   */
  @Test
  public void testOutsideTimeHorizon() {
    final int periodsPerHour = 4;
    final int expectedValue = 2302;
    final int hours = 2;
    final long startTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours);
    final List<Object> group = Collections.emptyList();

    // setup - write a single value from 2 hours ago
    ProfileMeasurement m = new ProfileMeasurement("profile1", "entity1", startTime, periodsPerHour);
    profileWriter.write(m, 1, group, val -> expectedValue);

    // create a variable that contains the groups to use
    state.put("groups", group);

    // execute - read the profile values
    String expr = "PROFILE_GET('profile1', 'entity1', 4, 'SECONDS')";
    List<Integer> result = run(expr, List.class);

    // validate - there should be no values from only 4 seconds ago
    Assert.assertEquals(0, result.size());
  }
}
