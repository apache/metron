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
import org.apache.metron.profiler.hbase.DecodableRowKeyBuilder;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.functions.resolver.SimpleFunctionResolver;
import org.apache.metron.stellar.dsl.functions.resolver.SingletonFunctionResolver;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.client.stellar.FixedLookback;
import org.apache.metron.profiler.client.stellar.GetProfile;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.profiler.hbase.SaltyRowKeyBuilder;
import org.apache.metron.profiler.hbase.ValueOnlyColumnBuilder;
import org.apache.metron.stellar.common.DefaultStellarStatefulExecutor;
import org.apache.metron.stellar.common.StellarStatefulExecutor;
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

import static org.apache.metron.profiler.client.stellar.ProfilerConfig.*;

/**
 * Tests the GetProfile class.
 */
public class GetProfileTest {

  private static final String tableName = "profiler";
  private static final String columnFamily = "P";

  private static final long periodDuration = 30;
  private static final TimeUnit periodUnits = TimeUnit.MINUTES;
  private static final int saltDivisor = 10;

  // different values of period and salt divisor, used to test config_overrides feature
  private static final long periodDuration2 = 1;
  private static final TimeUnit periodUnits2 = TimeUnit.HOURS;
  private static final int saltDivisor2 = 2050;

  private HTableInterface table;

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

  @Before
  public void setup() {
    table = MockHTable.Provider.addToCache(tableName, columnFamily);
  }

  /**
   * Creates a ProfileWriter that can be used to write Profile data for testing.
   *
   * @return A ProfileWriter
   */
  private ProfileWriter getProfileWriter(int saltDivisor, long periodDuration, TimeUnit periodUnits) {

    // used to write values to be read during testing
    DecodableRowKeyBuilder rowKeyBuilder = new DecodableRowKeyBuilder();
    rowKeyBuilder.setSaltDivisor(saltDivisor);
    rowKeyBuilder.setPeriodDuration(periodDuration, periodUnits);

    ColumnBuilder columnBuilder = new ValueOnlyColumnBuilder(columnFamily);
    return new ProfileWriter(rowKeyBuilder, columnBuilder, table);
  }

  /**
   * Returns sensible default global properties.
   */
  private Map<String, Object> getDefaultProperties() {
    Map<String, Object> defaults = new HashMap<String, Object>() {{
      put(PROFILER_HBASE_TABLE.getKey(), tableName);
      put(PROFILER_COLUMN_FAMILY.getKey(), columnFamily);
      put(PROFILER_HBASE_TABLE_PROVIDER.getKey(), MockTableProvider.class.getName());
      put(PROFILER_PERIOD.getKey(), Long.toString(periodDuration));
      put(PROFILER_PERIOD_UNITS.getKey(), periodUnits.toString());
      put(PROFILER_SALT_DIVISOR.getKey(), Integer.toString(saltDivisor));
      put(PROFILER_ROW_KEY_BUILDER.getKey(), "org.apache.metron.profiler.hbase.DecodableRowKeyBuilder");
    }};

    return defaults;
  }

  /**
   * Creates a Stellar executor using a set of default global properties.
   */
  private StellarStatefulExecutor getExecutor() {
    return getExecutor(getDefaultProperties());
  }

  /**
   * Creates a Stellar executor.
   *
   * @param global The global properties.
   */
  private StellarStatefulExecutor getExecutor(Map<String, Object> global) {
    return new DefaultStellarStatefulExecutor(
            new SimpleFunctionResolver()
                    .withClass(GetProfile.class)
                    .withClass(FixedLookback.class),
            new Context.Builder()
                    .with(Context.Capabilities.GLOBAL_CONFIG, () -> global)
                    .build());
  }

  /**
   * Calculates the number of profile values that need to be written.
   *
   * @param hours          The number of hours that values are needed for.
   * @param periodDuration The period duration.
   * @param periodUnits    The period duration units.
   * @return The number of profile values to write.
   */
  private int howManyToWrite(int hours, long periodDuration, TimeUnit periodUnits) {
    final long millisPerHour = TimeUnit.HOURS.toMillis(1);
    final long periodsPerHour = millisPerHour / periodUnits.toMillis(periodDuration);
    return new Long(hours * periodsPerHour).intValue();
  }

  /**
   * Ensure that `PROFILE_GET` functions when used with no groups.  This is the most common scenario.
   */
  @Test
  public void testProfileGet() {
    final int expectedValue = 2302;
    final int hours = 4;
    final long startTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours);
    final List<Object> group = Collections.emptyList();
    Map<String, Object> state = new HashMap<>();

    // setup - write some measurements to be read later
    ProfileMeasurement prototype = new ProfileMeasurement()
            .withProfileName("profile1")
            .withEntity("entity1")
            .withPeriod(startTime, periodDuration, periodUnits);
    final int expected = howManyToWrite(hours, periodDuration, periodUnits);
    getProfileWriter(saltDivisor, periodDuration, periodUnits).write(prototype, expected, group, val -> expectedValue);

    // execute
    String expr = "PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'))";
    List<Integer> result = getExecutor().execute(expr, state, List.class);

    // validate - expect to see all profile values over the past 4 hours
    Assert.assertEquals(expected, result.size());
  }

  /**
   * Ensure that `PROFILE_GET` functions when used with one group.
   */
  @Test
  public void testProfileGetWithOneGroup() {

    final int expectedValue = 2302;
    final int hours = 2;
    final long startTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours);
    final List<Object> group = Arrays.asList("weekends");

    // setup - write some measurements to be read later
    ProfileMeasurement prototype = new ProfileMeasurement()
            .withProfileName("profile1")
            .withEntity("entity1")
            .withPeriod(startTime, periodDuration, periodUnits);
    final int expected = howManyToWrite(hours, periodDuration, periodUnits);
    getProfileWriter(saltDivisor, periodDuration, periodUnits).write(prototype, expected, group, val -> expectedValue);

    // create a variable that contains the groups to use
    Map<String, Object> state = new HashMap<>();
    state.put("groups", group);

    {
      // execute
      String expr = "PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'), ['weekends'])";
      List<Integer> result = getExecutor().execute(expr, state, List.class);
      Assert.assertEquals(expected, result.size());
    }
    {
      // test the deprecated but allowed "varargs" form of groups specification
      String expr = "PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'), 'weekends')";
      List<Integer> result = getExecutor().execute(expr, state, List.class);
      Assert.assertEquals(expected, result.size());
    }
  }

  /**
   * Values should be retrievable that have been stored within a 'group'.
   */
  @Test
  public void testProfileGetWithTwoGroups() {

    final int expectedValue = 2302;
    final int hours = 2;
    final long startTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours);
    final List<Object> group = Arrays.asList("weekdays", "tuesday");

    // setup - write some measurements to be read later
    ProfileMeasurement prototype = new ProfileMeasurement()
            .withProfileName("profile1")
            .withEntity("entity1")
            .withPeriod(startTime, periodDuration, periodUnits);
    final int expected = howManyToWrite(hours, periodDuration, periodUnits);
    getProfileWriter(saltDivisor, periodDuration, periodUnits).write(prototype, expected, group, val -> expectedValue);

    // create a variable that contains the groups to use
    Map<String, Object> state = new HashMap<>();
    state.put("groups", group);

    {
      // execute the test
      String expr = "PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'), ['weekdays', 'tuesday'])";
      List<Integer> result = getExecutor().execute(expr, state, List.class);
      Assert.assertEquals(expected, result.size());
    }
    {
      // test the deprecated but allowed "varargs" form of groups specification
      String expr = "PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'), 'weekdays', 'tuesday')";
      List<Integer> result = getExecutor().execute(expr, state, List.class);
      Assert.assertEquals(expected, result.size());
    }
  }

  /**
   * If the time horizon specified does not include any profile measurements, then
   * none should be returned.
   */
  @Test
  public void testProfileGetOutsideTimeHorizon() {
    final int expectedValue = 2302;
    final int hours = 2;
    final long startTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours);
    Map<String, Object> state = new HashMap<>();

    // setup - write 2 values starting from 2 hours ago
    ProfileMeasurement prototype = new ProfileMeasurement()
            .withProfileName("profile1")
            .withEntity("entity1")
            .withPeriod(startTime, periodDuration, periodUnits);
    getProfileWriter(saltDivisor, periodDuration, periodUnits).write(prototype, 2, Collections.emptyList(), val -> expectedValue);

    // execute - there is no profile data within the past 4 seconds
    String expr = "PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'SECONDS'))";
    List<Integer> result = getExecutor().execute(expr, state, List.class);
    Assert.assertEquals(0, result.size());
  }

  /**
   * Profile values should be retrievable that were written with configuration different than current global config.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testProfileGetWithConfigOverride() {
    final int expectedValue = 2302;
    final int hours = 2;
    final long startTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours);
    final List<Object> group = Collections.emptyList();
    Map<String, Object> state = new HashMap<>();

    // setup - write some measurements using different period duration values
    ProfileMeasurement prototype = new ProfileMeasurement()
            .withProfileName("profile1")
            .withEntity("entity1")
            .withPeriod(startTime, periodDuration2, periodUnits2);
    final int expected = howManyToWrite(hours, periodDuration2, periodUnits2);
    getProfileWriter(saltDivisor2, periodDuration2, periodUnits2).write(prototype, expected, group, val -> expectedValue);

    {
      // execute - should not be able to read any values since the period duration and salt used is different
      String expr = "PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'))";
      List<Integer> result = getExecutor().execute(expr, state, List.class);
      Assert.assertEquals(0, result.size());
    }
    {
      // execute - use the configuration override to use the correct period and salt values
      String overrides = String.format(
              "{" +
                      "'profiler.client.period.duration': '%s', " +
                      "'profiler.client.period.duration.units': '%s', " +
                      "'profiler.client.salt.divisor': %d " +
                      "}", periodDuration2, periodUnits2.toString(), saltDivisor2);
      String expr = String.format("PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS', %s), [], %s)", overrides, overrides);
      List<Integer> result = getExecutor().execute(expr, state, List.class);
      Assert.assertEquals(expected, result.size());
    }
  }

  /**
   * Profile values should be retrievable that were written with configuration different than current global config.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testProfileGetWithOneGroupAndConfigOverride() {
    final int expectedValue = 2302;
    final int hours = 2;
    final long startTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours);
    final List<Object> group = Arrays.asList("weekends");
    Map<String, Object> state = new HashMap<>();

    // setup - write some measurements using different period duration values
    ProfileMeasurement prototype = new ProfileMeasurement()
            .withProfileName("profile1")
            .withEntity("entity1")
            .withPeriod(startTime, periodDuration2, periodUnits2);
    final int expected = howManyToWrite(hours, periodDuration2, periodUnits2);
    getProfileWriter(saltDivisor2, periodDuration2, periodUnits2).write(prototype, expected, group, val -> expectedValue);

    {
      // execute - should not be able to read any values since the period duration and salt used is different
      String expr = "PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'))";
      List<Integer> result = getExecutor().execute(expr, state, List.class);
      Assert.assertEquals(0, result.size());
    }
    {
      // execute - use the configuration override to use the correct period and salt values
      String overrides = String.format(
              "{" +
                      "'profiler.client.period.duration': '%s', " +
                      "'profiler.client.period.duration.units': '%s', " +
                      "'profiler.client.salt.divisor': %d " +
                      "}", periodDuration2, periodUnits2.toString(), saltDivisor2);
      String expr = String.format("PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS', %s), ['weekends'], %s)", overrides, overrides);
      List<Integer> result = getExecutor().execute(expr, state, List.class);
      Assert.assertEquals(expected, result.size());
    }
  }
}