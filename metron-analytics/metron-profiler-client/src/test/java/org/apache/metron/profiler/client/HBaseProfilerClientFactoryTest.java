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

import org.apache.metron.hbase.client.FakeHBaseConnectionFactory;
import org.apache.metron.profiler.hbase.SaltyRowKeyBuilder;
import org.apache.metron.profiler.hbase.ValueOnlyColumnBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_COLUMN_FAMILY;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_HBASE_TABLE;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_HBASE_CONNECTION_FACTORY;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_PERIOD;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_PERIOD_UNITS;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_SALT_DIVISOR;
import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link HBaseProfilerClientFactory}.
 */
public class HBaseProfilerClientFactoryTest {

  private static final String tableName = "table";
  private static final String columnFamily = "columnFamily";
  private static final Integer periodDuration = 23;
  private static final TimeUnit periodDurationUnits = TimeUnit.MINUTES;
  private static final Integer saltDivisor = 1000;
  private static final long periodDurationMillis = periodDurationUnits.toMillis(23);
  private HBaseProfilerClientFactory creator;

  @Before
  public void setup() {
    creator = new HBaseProfilerClientFactory();
  }

  @Test
  public void testCreate() {
    Map<String, Object> globals = new HashMap<String, Object>() {{
      put(PROFILER_HBASE_TABLE.getKey(), tableName);
      put(PROFILER_COLUMN_FAMILY.getKey(), columnFamily);
      put(PROFILER_SALT_DIVISOR.getKey(), saltDivisor.toString());
      put(PROFILER_HBASE_CONNECTION_FACTORY.getKey(), FakeHBaseConnectionFactory.class.getName());
      put(PROFILER_PERIOD.getKey(), periodDuration.toString());
      put(PROFILER_PERIOD_UNITS.getKey(), periodDurationUnits.toString());
    }};

    HBaseProfilerClient client = creator.create(globals);
    assertEquals(periodDurationMillis, client.getPeriodDurationMillis());

    // validate the row key builder that is created
    SaltyRowKeyBuilder rowKeyBuilder = (SaltyRowKeyBuilder) client.getRowKeyBuilder();
    assertEquals(saltDivisor, (Integer) rowKeyBuilder.getSaltDivisor());
    assertEquals(periodDurationMillis, rowKeyBuilder.getPeriodDurationMillis());

    // validate the column builder that is created
    ValueOnlyColumnBuilder columnBuilder = (ValueOnlyColumnBuilder) client.getColumnBuilder();
    assertEquals(columnFamily, columnBuilder.getColumnFamily());
  }

  @Test
  public void testCreateUsingDefaultValues() {
    Map<String, Object> globals = new HashMap<String, Object>() {{
      // without using a mock connection factory, the test will hang trying to connect to HBase
      put(PROFILER_HBASE_CONNECTION_FACTORY.getKey(), FakeHBaseConnectionFactory.class.getName());
    }};

    // find what the default values should be
    final long defaultPeriodDuration = (Long) PROFILER_PERIOD.getDefault();
    final TimeUnit defaultPeriodDurationUnits = TimeUnit.valueOf((String) PROFILER_PERIOD_UNITS.getDefault());
    final long defaultPeriodDurationMillis = defaultPeriodDurationUnits.toMillis(defaultPeriodDuration);
    final long defaultSaltDivisor = (Long) PROFILER_SALT_DIVISOR.getDefault();
    final String defaultColumnFamily = (String) PROFILER_COLUMN_FAMILY.getDefault();

    HBaseProfilerClient client = creator.create(globals);
    assertEquals(defaultPeriodDurationMillis, client.getPeriodDurationMillis());

    // validate the row key builder that is created
    SaltyRowKeyBuilder rowKeyBuilder = (SaltyRowKeyBuilder) client.getRowKeyBuilder();
    assertEquals(defaultSaltDivisor, rowKeyBuilder.getSaltDivisor());
    assertEquals(defaultPeriodDurationMillis, rowKeyBuilder.getPeriodDurationMillis());

    // validate the column builder that is created
    ValueOnlyColumnBuilder columnBuilder = (ValueOnlyColumnBuilder) client.getColumnBuilder();
    assertEquals(defaultColumnFamily, columnBuilder.getColumnFamily());
  }
}
