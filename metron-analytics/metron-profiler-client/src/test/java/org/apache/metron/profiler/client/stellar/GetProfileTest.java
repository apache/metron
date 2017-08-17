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

import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.profiler.client.ProfileWriter;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.functions.resolver.SimpleFunctionResolver;
import org.apache.metron.stellar.dsl.functions.resolver.SingletonFunctionResolver;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.client.stellar.FixedLookback;
import org.apache.metron.profiler.client.stellar.GetProfile;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.profiler.hbase.SaltyRowKeyBuilder;
import org.apache.metron.profiler.hbase.ValueOnlyColumnBuilder;
import org.apache.metron.stellar.common.DefaultStellarStatefulExecutor;
import org.apache.metron.stellar.common.StellarStatefulExecutor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.*;

/**
 * Tests the GetProfile class.
 */
public class GetProfileTest {

  private static final long periodDuration = 15;
  private static final TimeUnit periodUnits = TimeUnit.MINUTES;
  private static final int saltDivisor = 1000;
  private static final String tableName = "profiler";
  private static final String columnFamily = "P";
  private StellarStatefulExecutor executor;
  private Map<String, Object> state;
  private ProfileWriter profileWriter;
  // different values of period and salt divisor, used to test config_overrides feature
  private static final long periodDuration2 = 1;
  private static final TimeUnit periodUnits2 = TimeUnit.HOURS;
  private static final int saltDivisor2 = 2050;

  private <T> T run(String expression, Class<T> clazz) {
    return executor.execute(expression, state, clazz);
  }

  /**
   * This method sets up the configuration context for both writing profile data
   * (using profileWriter to mock the complex process of what the Profiler topology
   * actually does), and then reading that profile data (thereby testing the PROFILE_GET
   * Stellar client implemented in GetProfile).
   *
   * It runs at @Before time, and sets testclass global variables used by the writers and readers.
   * The various writers and readers are in each test case, not here.
   *
   * @return void
   */
  @Before
  public void setup() {
    state = new HashMap<>();
    final HTableInterface table = MockHBaseTableProvider.addToCache(tableName, columnFamily);

    // used to write values to be read during testing
    RowKeyBuilder rowKeyBuilder = new SaltyRowKeyBuilder();
    ColumnBuilder columnBuilder = new ValueOnlyColumnBuilder(columnFamily);
    profileWriter = new ProfileWriter(rowKeyBuilder, columnBuilder, table);

    // global properties
    Map<String, Object> global = new HashMap<String, Object>() {{
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
                    .withClass(GetProfile.class)
                    .withClass(FixedLookback.class),
            new Context.Builder()
                    .with(Context.Capabilities.GLOBAL_CONFIG, () -> global)
                    .build());
  }

  /**
   * This method is similar to setup(), in that it sets up profiler configuration context,
   * but only for the client.  Additionally, it uses periodDuration2, periodUnits2
   * and saltDivisor2, instead of periodDuration, periodUnits and saltDivisor respectively.
   *
   * This is used in the unit tests that test the config_overrides feature of PROFILE_GET.
   * In these tests, the context from @Before setup() is used to write the data, then the global
   * context is changed to context2 (from this method).  Each test validates that a default read
   * using global context2 then gets no valid results (as expected), and that a read using
   * original context values in the PROFILE_GET config_overrides argument gets all expected results.
   *
   * @return context2 - The profiler client configuration context created by this method.
   *    The context2 values are also set in the configuration of the StellarStatefulExecutor
   *    stored in the global variable 'executor'.  However, there is no API for querying the
   *    context values from a StellarStatefulExecutor, so we output the context2 Context object itself,
   *    for validation purposes (so that its values can be validated as being significantly
   *    different from the setup() settings).
   */
  private Context setup2() {
    state = new HashMap<>();

    // global properties
    Map<String, Object> global = new HashMap<String, Object>() {{
      put(PROFILER_HBASE_TABLE.getKey(), tableName);
      put(PROFILER_COLUMN_FAMILY.getKey(), columnFamily);
      put(PROFILER_HBASE_TABLE_PROVIDER.getKey(), MockHBaseTableProvider.class.getName());
      put(PROFILER_PERIOD.getKey(), Long.toString(periodDuration2));
      put(PROFILER_PERIOD_UNITS.getKey(), periodUnits2.toString());
      put(PROFILER_SALT_DIVISOR.getKey(), Integer.toString(saltDivisor2));
    }};

    // create the modified context
    Context context2 = new Context.Builder()
            .with(Context.Capabilities.GLOBAL_CONFIG, () -> global)
            .build();

    // create the stellar execution environment
    executor = new DefaultStellarStatefulExecutor(
            new SimpleFunctionResolver()
                    .withClass(GetProfile.class)
                    .withClass(FixedLookback.class),
            context2);

    return context2; //because there is no executor.getContext() method
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
    String expr = "PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'))";
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
    String expr = "PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'), ['weekends'])";
    @SuppressWarnings("unchecked")
    List<Integer> result = run(expr, List.class);

    // validate - expect to read all values from the past 4 hours
    Assert.assertEquals(count, result.size());

    // test the deprecated but allowed "varargs" form of groups specification
    expr = "PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'), 'weekends')";
    result = run(expr, List.class);

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
    String expr = "PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'), ['weekdays', 'tuesday'])";
    @SuppressWarnings("unchecked")
    List<Integer> result = run(expr, List.class);

    // validate - expect to read all values from the past 4 hours
    Assert.assertEquals(count, result.size());

    // test the deprecated but allowed "varargs" form of groups specification
    expr = "PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'), 'weekdays', 'tuesday')";
    result = run(expr, List.class);

    // validate - expect to read all values from the past 4 hours
    Assert.assertEquals(count, result.size());
  }

  /**
   * Initialization should fail if the required context values are missing.
   */
  @Test(expected = IllegalStateException.class)
  public void testMissingContext() {
    Context empty = Context.EMPTY_CONTEXT();

    // 'unset' the context that was created during setup()
    executor.setContext(empty);

    // force re-initialization with no context
    SingletonFunctionResolver.getInstance().initialize(empty);

    // validate - function should be unable to initialize
    String expr = "PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(1000, 'SECONDS'), groups)";
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
    String expr = "PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'SECONDS'))";
    @SuppressWarnings("unchecked")
    List<Integer> result = run(expr, List.class);

    // validate - there should be no values from only 4 seconds ago
    Assert.assertEquals(0, result.size());
  }

  /**
   * Values should be retrievable that were written with configuration different than current global config.
   */
  @Test
  public void testWithConfigOverride() {
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

    // now change the executor configuration
    Context context2 = setup2();
    // validate it is changed in significant way
    @SuppressWarnings("unchecked")
    Map<String, Object> global = (Map<String, Object>) context2.getCapability(Context.Capabilities.GLOBAL_CONFIG).get();
    Assert.assertEquals(PROFILER_PERIOD.get(global), periodDuration2);
    Assert.assertNotEquals(periodDuration, periodDuration2);

    // execute - read the profile values - with (wrong) default global config values.
    // No error message at this time, but returns empty results list, because
    // row keys are not correctly calculated.
    String expr = "PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'))";
    @SuppressWarnings("unchecked")
    List<Integer> result = run(expr, List.class);

    // validate - expect to fail to read any values
    Assert.assertEquals(0, result.size());

    // execute - read the profile values - with config_override.
    // first two override values are strings, third is deliberately a number.
    String overrides = "{'profiler.client.period.duration' : '" + periodDuration + "', "
            + "'profiler.client.period.duration.units' : '" + periodUnits.toString() + "', "
            + "'profiler.client.salt.divisor' : " + saltDivisor + " }";
    expr = "PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS', " + overrides + "), [], " + overrides + ")"
            ;
    result = run(expr, List.class);

    // validate - expect to read all values from the past 4 hours
    Assert.assertEquals(count, result.size());
  }

  /**
   * Values should be retrievable that have been stored within a 'group', with
   * configuration different than current global config.
   * This time put the config_override case before the non-override case.
   */
  @Test
  public void testWithConfigAndOneGroup() {
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

    // now change the executor configuration
    Context context2 = setup2();
    // validate it is changed in significant way
    @SuppressWarnings("unchecked")
    Map<String, Object> global = (Map<String, Object>) context2.getCapability(Context.Capabilities.GLOBAL_CONFIG).get();
    Assert.assertEquals(global.get(PROFILER_PERIOD.getKey()), Long.toString(periodDuration2));
    Assert.assertNotEquals(periodDuration, periodDuration2);

    // execute - read the profile values - with config_override.
    // first two override values are strings, third is deliberately a number.
    String overrides = "{'profiler.client.period.duration' : '" + periodDuration + "', "
            + "'profiler.client.period.duration.units' : '" + periodUnits.toString() + "', "
            + "'profiler.client.salt.divisor' : " + saltDivisor + " }";
    String expr = "PROFILE_GET('profile1', 'entity1'" +
            ", PROFILE_FIXED(4, 'HOURS', " + overrides + "), ['weekends'], " +
            overrides + ")";
    @SuppressWarnings("unchecked")
    List<Integer> result = run(expr, List.class);

    // validate - expect to read all values from the past 4 hours
    Assert.assertEquals(count, result.size());

    // execute - read the profile values - with (wrong) default global config values.
    // No error message at this time, but returns empty results list, because
    // row keys are not correctly calculated.
    expr = "PROFILE_GET('profile1', 'entity1', PROFILE_FIXED(4, 'HOURS'), ['weekends'])";
    result = run(expr, List.class);

    // validate - expect to fail to read any values
    Assert.assertEquals(0, result.size());
  }

}
