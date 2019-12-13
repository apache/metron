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
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.metron.hbase.HTableProvider;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.hbase.client.HBaseClient;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.profiler.hbase.SaltyRowKeyBuilder;
import org.apache.metron.profiler.hbase.ValueOnlyColumnBuilder;
import org.apache.spark.api.java.function.MapPartitionsFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_COLUMN_FAMILY;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_SALT_DIVISOR;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_TABLE_NAME;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_TABLE_PROVIDER;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_WRITE_DURABILITY;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.PERIOD_DURATION;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.PERIOD_DURATION_UNITS;

/**
 * Writes the profile measurements to HBase in Spark.
 */
public class HBaseWriterFunction implements MapPartitionsFunction<ProfileMeasurement, Integer> {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private TableProvider tableProvider;

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

  public HBaseWriterFunction(Properties properties) {
    tableName = HBASE_TABLE_NAME.get(properties, String.class);
    durability = HBASE_WRITE_DURABILITY.get(properties, Durability.class);

    // row key builder
    int saltDivisor = HBASE_SALT_DIVISOR.get(properties, Integer.class);
    int periodDuration = PERIOD_DURATION.get(properties, Integer.class);
    TimeUnit periodDurationUnits = TimeUnit.valueOf(PERIOD_DURATION_UNITS.get(properties, String.class));
    rowKeyBuilder = new SaltyRowKeyBuilder(saltDivisor, periodDuration, periodDurationUnits);

    // column builder
    String columnFamily = HBASE_COLUMN_FAMILY.get(properties, String.class);
    columnBuilder = new ValueOnlyColumnBuilder(columnFamily);

    // hbase table provider
    String providerImpl = HBASE_TABLE_PROVIDER.get(properties, String.class);
    tableProvider = createTableProvider(providerImpl);
  }

  /**
   * Writes a set of measurements to HBase.
   *
   * @param iterator The measurements to write.
   * @return The number of measurements written to HBase.
   */
  @Override
  public Iterator<Integer> call(Iterator<ProfileMeasurement> iterator) throws Exception {
    int count = 0;
    LOG.debug("About to write profile measurement(s) to HBase");

    // do not open hbase connection, if nothing to write
    List<ProfileMeasurement> measurements = IteratorUtils.toList(iterator);
    if(measurements.size() > 0) {

      // open an HBase connection
      Configuration config = HBaseConfiguration.create();
      try (HBaseClient client = new HBaseClient(tableProvider, config, tableName)) {

        for (ProfileMeasurement m : measurements) {
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

  /**
   * Set the {@link TableProvider} using the class name of the provider.
   * @param providerImpl The name of the class.
   * @return
   */
  public HBaseWriterFunction withTableProviderImpl(String providerImpl) {
    this.tableProvider = createTableProvider(providerImpl);
    return this;
  }

  /**
   * Creates a TableProvider based on a class name.
   * @param providerImpl The class name of a TableProvider
   */
  private static TableProvider createTableProvider(String providerImpl) {
    LOG.trace("Creating table provider; className={}", providerImpl);

    // if class name not defined, use a reasonable default
    if(StringUtils.isEmpty(providerImpl) || providerImpl.charAt(0) == '$') {
      return new HTableProvider();
    }

    // instantiate the table provider
    try {
      Class<? extends TableProvider> clazz = (Class<? extends TableProvider>) Class.forName(providerImpl);
      return clazz.getConstructor().newInstance();

    } catch (InstantiationException | IllegalAccessException | IllegalStateException |
            InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
      throw new IllegalStateException("Unable to instantiate connector", e);
    }
  }
}
