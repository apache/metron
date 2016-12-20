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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.functions.DateFunctions;
import org.apache.metron.common.dsl.functions.TimeFunctions;
import org.apache.metron.common.dsl.functions.resolver.SimpleFunctionResolver;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.client.ProfileWriter;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.profiler.hbase.SaltyRowKeyBuilder;
import org.apache.metron.profiler.hbase.ValueOnlyColumnBuilder;
import org.apache.metron.profiler.stellar.DefaultStellarExecutor;
import org.apache.metron.profiler.stellar.StellarExecutor;
import org.apache.metron.test.mock.MockHTable;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.metron.profiler.client.stellar.ProfileFunctions.PROFILER_COLUMN_FAMILY;
import static org.apache.metron.profiler.client.stellar.ProfileFunctions.PROFILER_HBASE_TABLE;
import static org.apache.metron.profiler.client.stellar.ProfileFunctions.PROFILER_HBASE_TABLE_PROVIDER;
import static org.apache.metron.profiler.client.stellar.ProfileFunctions.PROFILER_PERIOD;
import static org.apache.metron.profiler.client.stellar.ProfileFunctions.PROFILER_PERIOD_UNITS;
import static org.apache.metron.profiler.client.stellar.ProfileFunctions.PROFILER_SALT_DIVISOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the GetProfile class.
 */
public class ProfileGetFromTest {

  // define period duration
  private static final int periodsPerHour = 4;
  private static final long periodDuration = 15;
  private static final TimeUnit periodUnits = TimeUnit.MINUTES;

  // defines how many profile values are written
  private static final int days = 4;
  private static final long startTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
  private static final int count = periodsPerHour * 24 * days;

  private static final long expectedValue = 2302;
  private static final int saltDivisor = 1000;
  private static final String tableName = "profiler";
  private static final String columnFamily = "P";

  private StellarExecutor executor;
  private Map<String, Object> state;
  private ProfileWriter profileWriter;

  @Before
  public void setup() {
    state = new HashMap<>();
    final HTableInterface table = MockHTable.Provider.addToCache(tableName, columnFamily);

    // used to write values to be read during testing
    RowKeyBuilder rowKeyBuilder = new SaltyRowKeyBuilder();
    ColumnBuilder columnBuilder = new ValueOnlyColumnBuilder(columnFamily);
    profileWriter = new ProfileWriter(rowKeyBuilder, columnBuilder, table, periodUnits.toMillis(periodDuration));

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
                    .withClass(ProfileFunctions.ProfileGetFrom.class)
                    .withClass(DateFunctions.Now.class)
                    .withClass(TimeFunctions.Millis.class),
            new Context.Builder()
                    .with(Context.Capabilities.GLOBAL_CONFIG, () -> global)
                    .build());

    // write some profile data to be read later
    final List<Object> group = Collections.emptyList();
    ProfileMeasurement m = new ProfileMeasurement()
            .withProfileName("profile1")
            .withEntity("entity1")
            .withPeriod(startTime, periodDuration, periodUnits);
    profileWriter.write(m, count, group, val -> expectedValue);
  }

  @SuppressWarnings("unchecked")
  private List<Long> run(String expression) {
    return executor.execute(expression, Collections.emptyMap(), List.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testProfileGetFromWith15MinuteLookBack() {
    {
      // expect data from today
      List<Long> results = run("PROFILE_GET_FROM('profile1', 'entity1', MILLIS(15, 'MINUTES'), NOW())");
      assertEquals(2, results.size());
      assertTrue(results.stream().allMatch(val -> val == expectedValue));
    }
    {
      // expect data from 1 day back
      List<Long> results = run("PROFILE_GET_FROM('profile1', 'entity1', MILLIS(15, 'MINUTES'), NOW() - MILLIS(1, 'DAYS'))");
      assertEquals(2, results.size());
      assertTrue(results.stream().allMatch(val -> val == expectedValue));
    }
    {
      // expect data from 2 days back
      List<Long> results = run("PROFILE_GET_FROM('profile1', 'entity1', MILLIS(15, 'MINUTES'), NOW() - MILLIS(2, 'DAYS'))");
      assertEquals(2, results.size());
      assertTrue(results.stream().allMatch(val -> val == expectedValue));
    }
    {
      // expect data from 3 days back
      List<Long> results = run("PROFILE_GET_FROM('profile1', 'entity1', MILLIS(15, 'MINUTES'), NOW() - MILLIS(3, 'DAYS'))");
      assertEquals(2, results.size());
      assertTrue(results.stream().allMatch(val -> val == expectedValue));
    }
    {
      // do not expect any data from 4 days back
      List<Long> results = run("PROFILE_GET_FROM('profile1', 'entity1', MILLIS(15, 'MINUTES'), NOW() - MILLIS(4, 'DAYS'))");
      assertEquals(0, results.size());
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testProfileGetFromWith30MinuteLookBack() {
    {
      // expect data from today
      List<Long> results = run("PROFILE_GET_FROM('profile1', 'entity1', MILLIS(30, 'MINUTES'), NOW())");
      assertEquals(3, results.size());
      assertTrue(results.stream().allMatch(val -> val == expectedValue));
    }
    {
      // expect data from 1 day back
      List<Long> results = run("PROFILE_GET_FROM('profile1', 'entity1', MILLIS(30, 'MINUTES'), NOW() - MILLIS(1, 'DAYS'))");
      assertEquals(3, results.size());
      assertTrue(results.stream().allMatch(val -> val == expectedValue));
    }
    {
      // expect data from 2 days back
      List<Long> results = run("PROFILE_GET_FROM('profile1', 'entity1', MILLIS(30, 'MINUTES'), NOW() - MILLIS(2, 'DAYS'))");
      assertEquals(3, results.size());
      assertTrue(results.stream().allMatch(val -> val == expectedValue));
    }
    {
      // expect data from 3 days back
      List<Long> results = run("PROFILE_GET_FROM('profile1', 'entity1', MILLIS(30, 'MINUTES'), NOW() - MILLIS(3, 'DAYS'))");
      assertEquals(3, results.size());
      assertTrue(results.stream().allMatch(val -> val == expectedValue));
    }
    {
      // do not expect any data from 4 days back
      List<Long> results = run("PROFILE_GET_FROM('profile1', 'entity1', MILLIS(30, 'MINUTES'), NOW() - MILLIS(4, 'DAYS'))");
      assertEquals(0, results.size());
    }
  }

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
}
