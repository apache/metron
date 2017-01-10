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
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.functions.resolver.SimpleFunctionResolver;
import org.apache.metron.common.dsl.functions.resolver.SingletonFunctionResolver;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.client.stellar.GetProfile;
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

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.metron.profiler.client.stellar.GetProfile.PROFILER_COLUMN_FAMILY;
import static org.apache.metron.profiler.client.stellar.GetProfile.PROFILER_HBASE_TABLE;
import static org.apache.metron.profiler.client.stellar.GetProfile.PROFILER_HBASE_TABLE_PROVIDER;
import static org.apache.metron.profiler.client.stellar.GetProfile.PROFILER_PERIOD;
import static org.apache.metron.profiler.client.stellar.GetProfile.PROFILER_PERIOD_UNITS;
import static org.apache.metron.profiler.client.stellar.GetProfile.PROFILER_SALT_DIVISOR;

/**
 * Tests the GetProfile class.
 */
public class GetProfileTest {

  private static final long periodDuration = 15;
  private static final TimeUnit periodUnits = TimeUnit.MINUTES;
  private static final int saltDivisor = 1000;
  private static final String tableName = "profiler";
  private static final String columnFamily = "P";
  private StellarExecutor executor;
  private Map<String, Object> state;
  private ProfileWriter profileWriter;

  /**
   * A TableProvider that allows us to mock HBase.
   */
  public static class MockTableProvider implements TableProvider, Serializable {

    MockHTable.Provider provider = new MockHTable.Provider();

    @Override
    public HTableInterface getTable(Configuration config, String tableName) throws IOException {
      return provider.getTable(config, tableName);
    }
  }

  private <T> T run(String expression, Class<T> clazz) {
    return executor.execute(expression, state, clazz);
  }

  @Before
  public void setup() {
    state = new HashMap<>();
    final HTableInterface table = MockHTable.Provider.addToCache(tableName, columnFamily);

    // used to write values to be read during testing
    RowKeyBuilder rowKeyBuilder = new SaltyRowKeyBuilder();
    ColumnBuilder columnBuilder = new ValueOnlyColumnBuilder(columnFamily);
    profileWriter = new ProfileWriter(rowKeyBuilder, columnBuilder, table);

    // global properties
    Map<String, Object> global = new HashMap<String, Object>() {{
      put(PROFILER_HBASE_TABLE, tableName);
      put(PROFILER_COLUMN_FAMILY, columnFamily);
      put(PROFILER_HBASE_TABLE_PROVIDER, MockTableProvider.class.getName());
      put(PROFILER_PERIOD, Long.toString(periodDuration));
      put(PROFILER_PERIOD_UNITS, periodUnits.toString());
      put(PROFILER_SALT_DIVISOR, Integer.toString(saltDivisor));
    }};

    // create the stellar execution environment
    executor = new DefaultStellarExecutor(
            new SimpleFunctionResolver()
                    .withClass(GetProfile.class),
            new Context.Builder()
                    .with(Context.Capabilities.GLOBAL_CONFIG, () -> global)
                    .build());
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
    ProfileMeasurement m = new ProfileMeasurement()
            .withProfileName("profile1")
            .withEntity("entity1")
            .withPeriod(startTime, periodDuration, periodUnits);

    profileWriter.write(m, count, group, val -> expectedValue);

    // execute - read the profile values - no groups
    String expr = "PROFILE_GET('profile1', 'entity1', 4, 'HOURS')";
    @SuppressWarnings("unchecked")
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
    ProfileMeasurement m = new ProfileMeasurement()
            .withProfileName("profile1")
            .withEntity("entity1")
            .withPeriod(startTime, periodDuration, periodUnits);
    profileWriter.write(m, count, group, val -> expectedValue);

    // create a variable that contains the groups to use
    state.put("groups", group);

    // execute - read the profile values
    String expr = "PROFILE_GET('profile1', 'entity1', 4, 'HOURS', 'weekends')";
    @SuppressWarnings("unchecked")
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
    ProfileMeasurement m = new ProfileMeasurement()
            .withProfileName("profile1")
            .withEntity("entity1")
            .withPeriod(startTime, periodDuration, periodUnits);
    profileWriter.write(m, count, group, val -> expectedValue);

    // create a variable that contains the groups to use
    state.put("groups", group);

    // execute - read the profile values
    String expr = "PROFILE_GET('profile1', 'entity1', 4, 'HOURS', 'weekdays', 'tuesday')";
    @SuppressWarnings("unchecked")
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
    SingletonFunctionResolver.getInstance().initialize(empty);

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
    final int expectedValue = 2302;
    final int hours = 2;
    final long startTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours);
    final List<Object> group = Collections.emptyList();

    // setup - write a single value from 2 hours ago
    ProfileMeasurement m = new ProfileMeasurement()
            .withProfileName("profile1")
            .withEntity("entity1")
            .withPeriod(startTime, periodDuration, periodUnits);
    profileWriter.write(m, 1, group, val -> expectedValue);

    // create a variable that contains the groups to use
    state.put("groups", group);

    // execute - read the profile values
    String expr = "PROFILE_GET('profile1', 'entity1', 4, 'SECONDS')";
    @SuppressWarnings("unchecked")
    List<Integer> result = run(expr, List.class);

    // validate - there should be no values from only 4 seconds ago
    Assert.assertEquals(0, result.size());
  }
}
