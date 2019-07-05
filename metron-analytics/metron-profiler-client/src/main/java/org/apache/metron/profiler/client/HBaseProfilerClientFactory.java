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
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.metron.hbase.client.HBaseClient;
import org.apache.metron.hbase.client.HBaseClientFactory;
import org.apache.metron.hbase.client.HBaseConnectionFactory;
import org.apache.metron.hbase.client.HBaseTableClientFactory;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.profiler.hbase.SaltyRowKeyBuilder;
import org.apache.metron.profiler.hbase.ValueOnlyColumnBuilder;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_COLUMN_FAMILY;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_HBASE_TABLE;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_HBASE_CONNECTION_FACTORY;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_SALT_DIVISOR;
import static org.apache.metron.profiler.client.stellar.Util.getPeriodDurationInMillis;

/**
 * Creates an {@link HBaseProfilerClient}.
 */
public class HBaseProfilerClientFactory implements ProfilerClientFactory {

  private HBaseClientFactory hBaseClientFactory;

  public HBaseProfilerClientFactory() {
    this(new HBaseTableClientFactory());
  }

  public HBaseProfilerClientFactory(HBaseClientFactory hBaseClientFactory) {
    this.hBaseClientFactory = hBaseClientFactory;
  }

  @Override
  public HBaseProfilerClient create(Map<String, Object> globals) {
    // create the hbase client
    String tableName = PROFILER_HBASE_TABLE.get(globals, String.class);
    HBaseConnectionFactory connFactory = getConnectionFactory(globals);
    Configuration config = HBaseConfiguration.create();
    HBaseClient hbaseClient = hBaseClientFactory.create(connFactory, config, tableName);

    // create the profiler client
    RowKeyBuilder rowKeyBuilder = getRowKeyBuilder(globals);
    ColumnBuilder columnBuilder = getColumnBuilder(globals);
    long periodDuration = getPeriodDurationInMillis(globals);
    return new HBaseProfilerClient(hbaseClient, rowKeyBuilder, columnBuilder, periodDuration);
  }

  /**
   * Creates the ColumnBuilder to use in accessing the profile data.
   * @param global The global configuration.
   */
  private static ColumnBuilder getColumnBuilder(Map<String, Object> global) {
    String columnFamily = PROFILER_COLUMN_FAMILY.get(global, String.class);
    return new ValueOnlyColumnBuilder(columnFamily);
  }

  /**
   * Creates the ColumnBuilder to use in accessing the profile data.
   * @param global The global configuration.
   */
  private static RowKeyBuilder getRowKeyBuilder(Map<String, Object> global) {
    Integer saltDivisor = PROFILER_SALT_DIVISOR.get(global, Integer.class);
    return new SaltyRowKeyBuilder(saltDivisor, getPeriodDurationInMillis(global), TimeUnit.MILLISECONDS);
  }

  /**
   * Create the {@link HBaseConnectionFactory} to use when accessing HBase.
   * @param global The global configuration.
   */
  private static HBaseConnectionFactory getConnectionFactory(Map<String, Object> global) {
    String clazzName = PROFILER_HBASE_CONNECTION_FACTORY.get(global, String.class);
    return HBaseConnectionFactory.byName(clazzName);
  }
}
