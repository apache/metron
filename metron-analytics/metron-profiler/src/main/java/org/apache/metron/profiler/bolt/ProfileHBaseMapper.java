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

package org.apache.metron.profiler.bolt;

import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.hbase.bolt.mapper.ColumnList;
import org.apache.metron.hbase.bolt.mapper.HBaseMapper;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.profiler.hbase.SaltyRowKeyBuilder;
import org.apache.metron.profiler.hbase.ValueOnlyColumnBuilder;
import org.apache.storm.tuple.Tuple;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * An HbaseMapper that defines how a ProfileMeasurement is persisted within an HBase table.
 */
public class ProfileHBaseMapper implements HBaseMapper {

  /**
   * Generates the row keys necessary to store profile data in HBase.
   */
  private RowKeyBuilder rowKeyBuilder;

  /**
   * Generates the ColumnList necesary to store profile data in HBase.
   */
  private ColumnBuilder columnBuilder;

  public ProfileHBaseMapper() {
    setRowKeyBuilder(new SaltyRowKeyBuilder());
    setColumnBuilder(new ValueOnlyColumnBuilder());
  }

  public ProfileHBaseMapper(RowKeyBuilder rowKeyBuilder, ColumnBuilder columnBuilder) {
    setRowKeyBuilder(rowKeyBuilder);
    setColumnBuilder(columnBuilder);
  }

  /**
   * Defines the HBase row key that will be used when writing the data from a
   * tuple to HBase.
   *
   * @param tuple The tuple to map to HBase.
   */
  @Override
  public byte[] rowKey(Tuple tuple) {
    ProfileMeasurement measurement = (ProfileMeasurement) tuple.getValueByField("measurement");
    return rowKeyBuilder.rowKey(measurement);
  }

  /**
   * Defines the columnar structure that will be used when writing the data
   * from a tuple to HBase.
   *
   * @param tuple The tuple to map to HBase.
   */
  @Override
  public ColumnList columns(Tuple tuple) {
    ProfileMeasurement measurement = (ProfileMeasurement) tuple.getValueByField("measurement");
    return columnBuilder.columns(measurement);
  }

  /**
   * Defines the TTL (time-to-live) that will be used when writing the data
   * from a tuple to HBase.  After the TTL, the data will expire and will be
   * purged.
   *
   * @param tuple The tuple to map to HBase.
   * @return The TTL in milliseconds.
   */
  @Override
  public Optional<Long> getTTL(Tuple tuple) {
    Optional<Long> expiresMillis = Optional.empty();

    ProfileMeasurement measurement = (ProfileMeasurement) tuple.getValueByField("measurement");
    ProfileConfig profileConfig = measurement.getDefinition();

    if(profileConfig.getExpires() != null) {

      // a profile's `expires` field is in days, but hbase expects milliseconds
      long expiresDays = profileConfig.getExpires();
      expiresMillis = Optional.of(TimeUnit.DAYS.toMillis(expiresDays));
    }

    return expiresMillis;
  }

  public void setRowKeyBuilder(RowKeyBuilder rowKeyBuilder) {
    this.rowKeyBuilder = rowKeyBuilder;
  }

  public void setColumnBuilder(ColumnBuilder columnBuilder) {
    this.columnBuilder = columnBuilder;
  }
}
