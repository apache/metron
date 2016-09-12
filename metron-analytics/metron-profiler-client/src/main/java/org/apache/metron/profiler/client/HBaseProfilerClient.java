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
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.profiler.hbase.Serializer;

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
   * @param profile The name of the profile.
   * @param entity The name of the entity.
   * @param durationAgo How far in the past to fetch values from.
   * @param unit The time unit of 'durationAgo'.
   * @param groups The groups
   * @param <T> The type of values stored by the Profile.
   * @return A list of profile values.
   */
  @Override
  public <T> List<T> fetch(String profile, String entity, long durationAgo, TimeUnit unit, Class<T> clazz, List<Object> groups) {

    // find all the row keys that satisfy this fetch
    List<byte[]> keysToFetch = rowKeyBuilder.rowKeys(profile, entity, groups, durationAgo, unit);
    byte[] columnFamilyBytes = Bytes.toBytes(columnBuilder.getColumnFamily());
    byte[] columnQualifier = columnBuilder.getColumnQualifier("value");

    // create a Get for each of the row keys
    List<Get> gets = keysToFetch
            .stream()
            .map(k -> new Get(k).addColumn(columnFamilyBytes, columnQualifier))
            .collect(Collectors.toList());

    // submit the gets to HBase
    try {
      List<T> values = new ArrayList<>();

      Result[] results = table.get(gets);
      Arrays.stream(results)
              .filter(r -> r.containsColumn(columnFamilyBytes, columnQualifier))
              .map(r -> r.getValue(columnFamilyBytes, columnQualifier))
              .forEach(val -> values.add(Serializer.fromBytes(val, clazz)));

      return values;

    } catch(IOException e) {
      throw new RuntimeException(e);
    }
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