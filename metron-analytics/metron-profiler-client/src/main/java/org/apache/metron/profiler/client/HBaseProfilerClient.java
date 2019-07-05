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

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.utils.SerDeUtils;
import org.apache.metron.hbase.HBaseProjectionCriteria;
import org.apache.metron.hbase.client.HBaseClient;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.ProfilePeriod;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * The default implementation of a ProfilerClient that fetches profile data persisted in HBase.
 */
public class HBaseProfilerClient implements ProfilerClient {

  /**
   * Used to access the profile data stored in HBase.
   */
  private HBaseClient hbaseClient;

  /**
   * Generates the row keys necessary to scan HBase.
   */
  private RowKeyBuilder rowKeyBuilder;

  /**
   * Knows how profiles are organized by columns in HBase.
   */
  private ColumnBuilder columnBuilder;

  /**
   * The period duration in milliseconds for the profiles that will be read by this client.
   */
  private long periodDurationMillis;

  public HBaseProfilerClient(HBaseClient hbaseClient,
                             RowKeyBuilder rowKeyBuilder,
                             ColumnBuilder columnBuilder,
                             long periodDurationMillis) {
    this.rowKeyBuilder = rowKeyBuilder;
    this.columnBuilder = columnBuilder;
    this.hbaseClient = hbaseClient;
    this.periodDurationMillis = periodDurationMillis;
  }

  /**
   * Fetch the values stored in a profile based on a start and end timestamp.
   *
   * @param clazz   The type of values stored by the profile.
   * @param profile The name of the profile.
   * @param entity  The name of the entity.
   * @param groups  The groups used to sort the profile data.
   * @param start   The start time in epoch milliseconds.
   * @param end     The end time in epoch milliseconds.
   * @param defaultValue The default value to specify.  If empty, the result will be sparse.
   * @param <T>     The type of values stored by the profile.
   * @return A list of values.
   */
  @Override
  public <T> List<ProfileMeasurement> fetch(Class<T> clazz,
                                            String profile,
                                            String entity,
                                            List<Object> groups,
                                            long start,
                                            long end,
                                            Optional<T> defaultValue) {
    List<ProfilePeriod> periods = ProfilePeriod.visitPeriods(
            start,
            end,
            periodDurationMillis,
            TimeUnit.MILLISECONDS,
            Optional.empty(),
            period -> period);
    return fetch(clazz, profile, entity, groups, periods, defaultValue);
  }



  /**
   * Fetch the values stored in a profile based on a set of timestamps.
   *
   * @param clazz      The type of values stored by the profile.
   * @param profile    The name of the profile.
   * @param entity     The name of the entity.
   * @param groups     The groups used to sort the profile data.
   * @param periods    The set of profile measurement periods
   * @param defaultValue The default value to specify.  If empty, the result will be sparse.
   * @return A list of values.
   */
  @Override
  public <T> List<ProfileMeasurement> fetch(Class<T> clazz,
                                            String profile,
                                            String entity,
                                            List<Object> groups,
                                            Iterable<ProfilePeriod> periods,
                                            Optional<T> defaultValue) {
    // create a list of profile measurements that need fetched
    List<ProfileMeasurement> toFetch = new ArrayList<>();
    for(ProfilePeriod period: periods) {
      toFetch.add(new ProfileMeasurement()
              .withProfileName(profile)
              .withEntity(entity)
              .withPeriod(period)
              .withGroups(groups));
    }

    // retrieve the measurement values from HBase
    return doFetch(toFetch, clazz, defaultValue);
  }

  @Override
  public void close() throws IOException {
    if(hbaseClient != null) {
      hbaseClient.close();
    }
  }

  private <T> List<ProfileMeasurement> doFetch(List<ProfileMeasurement> measurements,
                                               Class<T> clazz,
                                               Optional<T> defaultValue) {
    List<ProfileMeasurement> values = new ArrayList<>();

    // define which columns need fetched
    byte[] columnFamily = Bytes.toBytes(columnBuilder.getColumnFamily());
    byte[] columnQualifier = columnBuilder.getColumnQualifier("value");
    HBaseProjectionCriteria.ColumnMetaData column = new HBaseProjectionCriteria.ColumnMetaData(columnFamily, columnQualifier);
    HBaseProjectionCriteria criteria = new HBaseProjectionCriteria().addColumn(column);

    for(ProfileMeasurement measurement: measurements) {
      byte[] rowKey = rowKeyBuilder.rowKey(measurement);
      hbaseClient.addGet(rowKey, criteria);
    }

    // query HBase
    Result[] results = hbaseClient.getAll();
    for(int i = 0; i < results.length; ++i) {
      Result result = results[i];
      ProfileMeasurement measurement = measurements.get(i);

      boolean exists = result.containsColumn(columnFamily, columnQualifier);
      if(exists) {
        // value found
        byte[] value = result.getValue(columnFamily, columnQualifier);
        measurement.withProfileValue(SerDeUtils.fromBytes(value, clazz));
        values.add(measurement);

      } else if(defaultValue.isPresent()) {
        // no value found, use default value provided
        measurement.withProfileValue(defaultValue.get());
        values.add(measurement);

      } else {
        // no value found and no default provided. nothing to do
      }
    }

    return values;
  }

  protected HBaseClient getHbaseClient() {
    return hbaseClient;
  }

  protected RowKeyBuilder getRowKeyBuilder() {
    return rowKeyBuilder;
  }

  protected void setRowKeyBuilder(RowKeyBuilder rowKeyBuilder) {
    this.rowKeyBuilder = rowKeyBuilder;
  }

  protected ColumnBuilder getColumnBuilder() {
    return columnBuilder;
  }

  protected void setColumnBuilder(ColumnBuilder columnBuilder) {
    this.columnBuilder = columnBuilder;
  }

  protected long getPeriodDurationMillis() {
    return periodDurationMillis;
  }
}
