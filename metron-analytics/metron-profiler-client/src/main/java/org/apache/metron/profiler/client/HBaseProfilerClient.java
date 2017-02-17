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

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.profiler.ProfilePeriod;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.common.utils.SerDeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * The default implementation of a ProfilerClient that fetches profile data persisted in HBase.
 */
public class HBaseProfilerClient implements ProfilerClient {

  /**
   * Used to access the profile data stored in HBase.
   */
  private HTableInterface table;

  /**
   * Generates the row keys necessary to scan HBase.
   */
  private RowKeyBuilder rowKeyBuilder;

  /**
   * Knows how profiles are organized in HBase.
   */
  private ColumnBuilder columnBuilder;

  public HBaseProfilerClient(HTableInterface table, RowKeyBuilder rowKeyBuilder, ColumnBuilder columnBuilder) {
    setTable(table);
    setRowKeyBuilder(rowKeyBuilder);
    setColumnBuilder(columnBuilder);
  }

  /**
   * Fetches all of the data values associated with a Profile.
   *
   * @param clazz       The type of values stored by the profile.
   * @param profile     The name of the profile.
   * @param entity      The name of the entity.
   * @param groups      The groups used to sort the profile data.
   * @param durationAgo How far in the past to fetch values from.
   * @param unit        The time unit of 'durationAgo'.
   * @param <T>         The type of values stored by the Profile.
   * @return A list of values.
   */
  @Override
  public <T> List<T> fetch(Class<T> clazz, String profile, String entity, List<Object> groups, long durationAgo, TimeUnit unit) {
    long end = System.currentTimeMillis();
    long start = end - unit.toMillis(durationAgo);
    return fetch(clazz, profile, entity, groups, start, end);
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
   * @param <T>     The type of values stored by the profile.
   * @return A list of values.
   */
  @Override
  public <T> List<T> fetch(Class<T> clazz, String profile, String entity, List<Object> groups, long start, long end) {
    byte[] columnFamily = Bytes.toBytes(columnBuilder.getColumnFamily());
    byte[] columnQualifier = columnBuilder.getColumnQualifier("value");

    // find all the row keys that satisfy this fetch
    List<byte[]> keysToFetch = rowKeyBuilder.rowKeys(profile, entity, groups, start, end);

    // create a Get for each of the row keys
    List<Get> gets = keysToFetch
            .stream()
            .map(k -> new Get(k).addColumn(columnFamily, columnQualifier))
            .collect(Collectors.toList());

    // get the 'gets'
    return get(gets, columnQualifier, columnFamily, clazz);
  }

  /**
   * Fetch the values stored in a profile based on a set of timestamps.
   *
   * @param clazz      The type of values stored by the profile.
   * @param profile    The name of the profile.
   * @param entity     The name of the entity.
   * @param groups     The groups used to sort the profile data.
   * @param periods    The set of profile measurement periods
   * @return A list of values.
   */
  @Override
  public <T> List<T> fetch(Class<T> clazz, String profile, String entity, List<Object> groups, Iterable<ProfilePeriod> periods) {
    byte[] columnFamily = Bytes.toBytes(columnBuilder.getColumnFamily());
    byte[] columnQualifier = columnBuilder.getColumnQualifier("value");

    // find all the row keys that satisfy this fetch
    List<byte[]> keysToFetch = rowKeyBuilder.rowKeys(profile, entity, groups, periods);

    // create a Get for each of the row keys
    List<Get> gets = keysToFetch
            .stream()
            .map(k -> new Get(k).addColumn(columnFamily, columnQualifier))
            .collect(Collectors.toList());

    // get the 'gets'
    return get(gets, columnQualifier, columnFamily, clazz);
  }

  /**
   * Submits multiple Gets to HBase and deserialize the results.
   *
   * @param gets            The gets to submit to HBase.
   * @param columnQualifier The column qualifier.
   * @param columnFamily    The column family.
   * @param clazz           The type expected in return.
   * @param <T>             The type expected in return.
   * @return
   */
  private <T> List<T> get(List<Get> gets, byte[] columnQualifier, byte[] columnFamily, Class<T> clazz) {
    List<T> values = new ArrayList<>();

    try {
      Result[] results = table.get(gets);
      Arrays.stream(results)
              .filter(r -> r.containsColumn(columnFamily, columnQualifier))
              .map(r -> r.getValue(columnFamily, columnQualifier))
              .forEach(val -> values.add(SerDeUtils.fromBytes(val, clazz)));

    } catch(IOException e) {
      throw new RuntimeException(e);
    }

    return values;
  }

  public void setTable(HTableInterface table) {
    this.table = table;
  }

  public void setRowKeyBuilder(RowKeyBuilder rowKeyBuilder) {
    this.rowKeyBuilder = rowKeyBuilder;
  }

  public void setColumnBuilder(ColumnBuilder columnBuilder) {
    this.columnBuilder = columnBuilder;
  }
}
