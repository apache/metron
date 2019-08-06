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
package org.apache.metron.profiler.spark.function;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.metron.hbase.client.HBaseClient;
import org.apache.metron.hbase.client.HBaseClientFactory;
import org.apache.metron.hbase.client.HBaseConnectionFactory;
import org.apache.metron.hbase.client.HBaseTableClient;
import org.apache.metron.hbase.client.HBaseTableClientFactory;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.profiler.hbase.SaltyRowKeyBuilder;
import org.apache.metron.profiler.hbase.ValueOnlyColumnBuilder;
import org.apache.metron.profiler.spark.ProfileMeasurementAdapter;
import org.apache.spark.api.java.function.MapPartitionsFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_CLIENT_FACTORY;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_COLUMN_FAMILY;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_CONNECTION_FACTORY;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_SALT_DIVISOR;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_TABLE_NAME;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_WRITE_DURABILITY;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.PERIOD_DURATION;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.PERIOD_DURATION_UNITS;

/**
 * Writes the profile measurements to HBase in Spark.
 */
public class HBaseWriterFunction implements MapPartitionsFunction<ProfileMeasurementAdapter, Integer> {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static class Builder {
    private HBaseConnectionFactory connectionFactory;
    private HBaseClientFactory hBaseClientFactory;
    private String tableName;
    private Durability durability;
    private RowKeyBuilder rowKeyBuilder;
    private ColumnBuilder columnBuilder;

    public Builder withConnectionFactory(HBaseConnectionFactory connectionFactory) {
      this.connectionFactory = connectionFactory;
      return this;
    }

    public Builder withClientFactory(HBaseClientFactory clientFactory) {
      this.hBaseClientFactory = clientFactory;
      return this;
    }

    public Builder withRowKeyBuilder(RowKeyBuilder rowKeyBuilder) {
      this.rowKeyBuilder = rowKeyBuilder;
      return this;
    }

    public Builder withColumnBuilder(ColumnBuilder columnBuilder) {
      this.columnBuilder = columnBuilder;
      return this;
    }

    public Builder withProperties(Properties properties) {
      // row key builder
      int saltDivisor = HBASE_SALT_DIVISOR.get(properties, Integer.class);
      int periodDuration = PERIOD_DURATION.get(properties, Integer.class);
      TimeUnit periodDurationUnits = TimeUnit.valueOf(PERIOD_DURATION_UNITS.get(properties, String.class));
      rowKeyBuilder = new SaltyRowKeyBuilder(saltDivisor, periodDuration, periodDurationUnits);

      // column builder
      String columnFamily = HBASE_COLUMN_FAMILY.get(properties, String.class);
      columnBuilder = new ValueOnlyColumnBuilder(columnFamily);

      // hbase
      tableName = HBASE_TABLE_NAME.get(properties, String.class);
      durability = HBASE_WRITE_DURABILITY.get(properties, Durability.class);

      // connection factory
      String factoryImpl = HBASE_CONNECTION_FACTORY.get(properties, String.class);
      connectionFactory = createConnectionFactory(factoryImpl);

      // client creator
      String creatorImpl = HBASE_CLIENT_FACTORY.get(properties, String.class);
      hBaseClientFactory = HBaseClientFactory.byName(creatorImpl, () -> new HBaseTableClientFactory());

      return this;
    }

    private static HBaseConnectionFactory createConnectionFactory(String factoryImpl) {
      LOG.trace("Creating table provider; className={}", factoryImpl);

      // if class name not defined, use a reasonable default
      if(StringUtils.isEmpty(factoryImpl) || factoryImpl.charAt(0) == '$') {
        return new HBaseConnectionFactory();
      }

      // instantiate the table provider
      return HBaseConnectionFactory.byName(factoryImpl);
    }

    public HBaseWriterFunction build() {
      HBaseWriterFunction function = new HBaseWriterFunction();
      function.connectionFactory = connectionFactory;
      function.hBaseClientFactory = hBaseClientFactory;
      function.tableName = tableName;
      function.durability = durability;
      function.rowKeyBuilder = rowKeyBuilder;
      function.columnBuilder = columnBuilder;
      return function;
    }
  }

  /**
   * Establishes connections to HBase.
   */
  private HBaseConnectionFactory connectionFactory;

  /**
   * Creates the {@link HBaseTableClient} when it is needed.
   */
  private HBaseClientFactory hBaseClientFactory;

  /**
   * The name of the HBase table to write to.
   */
  private String tableName;

  /**
   * The durability guarantee when writing to HBase.
   */
  private Durability durability;

  /**
   * Builds the HBase row key.
   */
  private RowKeyBuilder rowKeyBuilder;

  /**
   * Assembles the columns for HBase.
   */
  private ColumnBuilder columnBuilder;

  /**
   * Use the {@link HBaseWriterFunction.Builder} instead.
   */
  private HBaseWriterFunction() {
    // nothing to do
  }

  /**
   * Writes a set of measurements to HBase.
   *
   * @param iterator The measurements to write.
   * @return The number of measurements written to HBase.
   */
  @Override
  public Iterator<Integer> call(Iterator<ProfileMeasurementAdapter> iterator) throws Exception {
    int count = 0;
    LOG.debug("About to write profile measurement(s) to HBase");

    // do not open hbase connection, if nothing to write
    List<ProfileMeasurementAdapter> measurements = IteratorUtils.toList(iterator);
    if(measurements.size() > 0) {

      // open an HBase connection
      try (HBaseClient client = hBaseClientFactory.create(connectionFactory, HBaseConfiguration.create(), tableName)) {

        for (ProfileMeasurementAdapter adapter : measurements) {
          ProfileMeasurement m = adapter.toProfileMeasurement();
          client.addMutation(rowKeyBuilder.rowKey(m), columnBuilder.columns(m), durability);
        }
        count = client.mutate();

      } catch (IOException e) {
        LOG.error("Unable to open connection to HBase", e);
        throw new RuntimeException(e);
      }
    }

    LOG.debug("{} profile measurement(s) written to HBase", count);
    return IteratorUtils.singletonIterator(count);
  }
}
