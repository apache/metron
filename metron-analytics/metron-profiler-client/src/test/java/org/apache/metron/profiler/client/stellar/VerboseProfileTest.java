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

package org.apache.metron.profiler.client.stellar;

import org.apache.hadoop.hbase.client.Table;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.client.ProfileWriter;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.profiler.hbase.SaltyRowKeyBuilder;
import org.apache.metron.profiler.hbase.ValueOnlyColumnBuilder;
import org.apache.metron.stellar.common.DefaultStellarStatefulExecutor;
import org.apache.metron.stellar.common.StellarStatefulExecutor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.functions.resolver.SimpleFunctionResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.*;
import static org.apache.metron.profiler.client.stellar.VerboseProfile.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the VerboseProfile class.
 */
public class VerboseProfileTest {
  private static final long periodDuration = 15;
  private static final TimeUnit periodUnits = TimeUnit.MINUTES;
  private static final int saltDivisor = 1000;
  private static final String tableName = "profiler";
  private static final String columnFamily = "P";
  private StellarStatefulExecutor executor;
  private Map<String, Object> state;
  private ProfileWriter profileWriter;

  private <T> T run(String expression, Class<T> clazz) {
    return executor.execute(expression, state, clazz);
  }

  private Map<String, Object> globals;

  @BeforeEach
  public void setup() {
    state = new HashMap<>();
    final Table table = MockHBaseTableProvider.addToCache(tableName, columnFamily);
    TableProvider provider = new MockHBaseTableProvider();

    // used to write values to be read during testing
    long periodDurationMillis = TimeUnit.MINUTES.toMillis(15);
    RowKeyBuilder rowKeyBuilder = new SaltyRowKeyBuilder();
    ColumnBuilder columnBuilder = new ValueOnlyColumnBuilder(columnFamily);
    profileWriter = new ProfileWriter(rowKeyBuilder, columnBuilder, provider, periodDurationMillis,
        tableName, null);

    // global properties
    globals = new HashMap<String, Object>() {{
      put(PROFILER_HBASE_TABLE.getKey(), tableName);
      put(PROFILER_COLUMN_FAMILY.getKey(), columnFamily);
      put(PROFILER_HBASE_TABLE_PROVIDER.getKey(), MockHBaseTableProvider.class.getName());
      put(PROFILER_PERIOD.getKey(), Long.toString(periodDuration));
      put(PROFILER_PERIOD_UNITS.getKey(), periodUnits.toString());
      put(PROFILER_SALT_DIVISOR.getKey(), Integer.toString(saltDivisor));
    }};

    // create the stellar execution environment
    executor = new DefaultStellarStatefulExecutor(
            new SimpleFunctionResolver()
                    .withClass(VerboseProfile.class)
                    .withClass(FixedLookback.class),
            new Context.Builder()
                    .with(Context.Capabilities.GLOBAL_CONFIG, () -> globals)
                    .build());
  }

  @Test
  public void shouldReturnMeasurementsWhenNotGrouped() {
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

    // expect to see all values over the past 4 hours
    List<Map<String, Object>> results;
    results = run("PROFILE_VERBOSE('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'))", List.class);
    assertEquals(count, results.size());
    for(Map<String, Object> actual: results) {
      assertEquals("profile1", actual.get(PROFILE_KEY));
      assertEquals("entity1", actual.get(ENTITY_KEY));
      assertNotNull(actual.get(PERIOD_KEY));
      assertNotNull(actual.get(PERIOD_START_KEY));
      assertNotNull(actual.get(PERIOD_END_KEY));
      assertNotNull(actual.get(GROUPS_KEY));
      assertEquals(expectedValue, actual.get(VALUE_KEY));
    }
  }

  @Test
  public void shouldReturnMeasurementsWhenGrouped() {
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

    // expect to see all values over the past 4 hours for the group
    List<Map<String, Object>> results;
    results = run("PROFILE_VERBOSE('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'), groups)", List.class);
    assertEquals(count, results.size());
    for(Map<String, Object> actual: results) {
      assertEquals("profile1", actual.get(PROFILE_KEY));
      assertEquals("entity1", actual.get(ENTITY_KEY));
      assertNotNull(actual.get(PERIOD_KEY));
      assertNotNull(actual.get(PERIOD_START_KEY));
      assertNotNull(actual.get(PERIOD_END_KEY));
      assertNotNull(actual.get(GROUPS_KEY));
      assertEquals(expectedValue, actual.get(VALUE_KEY));
    }
  }

  @Test
  public void shouldReturnNothingWhenNoMeasurementsExist() {
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

    // expect to get NO measurements over the past 4 seconds
    List<Map<String, Object>> result;
    result = run("PROFILE_VERBOSE('profile1', 'entity1', PROFILE_FIXED(4, 'SECONDS'))", List.class);
    assertEquals(0, result.size());
  }

  @Test
  public void shouldReturnDefaultValueWhenNoMeasurementsExist() {
    // set a default value
    String defaultVal = "this is the default value";
    globals.put("profiler.default.value", defaultVal);

    // no profiles exist
    String expr = "PROFILE_VERBOSE('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'))";
    List<Map<String, Object>> results = run(expr, List.class);

    // expect to get the default value instead of no results
    assertTrue(results.size() == 16 || results.size() == 17);
    for(Map<String, Object> actual: results) {
      assertEquals("profile1", actual.get(PROFILE_KEY));
      assertEquals("entity1", actual.get(ENTITY_KEY));
      assertNotNull(actual.get(PERIOD_KEY));
      assertNotNull(actual.get(PERIOD_START_KEY));
      assertNotNull(actual.get(PERIOD_END_KEY));
      assertNotNull(actual.get(GROUPS_KEY));

      // expect the default value
      assertEquals(defaultVal, actual.get(VALUE_KEY));
    }

  }
}
