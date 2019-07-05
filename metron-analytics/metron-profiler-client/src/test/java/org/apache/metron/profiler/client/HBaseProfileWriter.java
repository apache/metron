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

import org.apache.hadoop.hbase.client.Durability;
import org.apache.metron.hbase.ColumnList;
import org.apache.metron.hbase.client.HBaseClient;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;

/**
 * Writes ProfileMeasurement values that can be read during automated testing.
 */
public class HBaseProfileWriter implements ProfileWriter {

  private RowKeyBuilder rowKeyBuilder;
  private ColumnBuilder columnBuilder;
  private HBaseClient hbaseClient;

  /**
   * @param rowKeyBuilder Builds the row key for a {@link ProfileMeasurement}.
   * @param columnBuilder Builds the columns associated with a {@link ProfileMeasurement}.
   * @param hbaseClient Writes the {@link ProfileMeasurement} values to HBase.
   */
  public HBaseProfileWriter(RowKeyBuilder rowKeyBuilder, ColumnBuilder columnBuilder, HBaseClient hbaseClient) {
    this.rowKeyBuilder = rowKeyBuilder;
    this.columnBuilder = columnBuilder;
    this.hbaseClient = hbaseClient;
  }

  /**
   * Write a ProfileMeasurement.
   * @param m The ProfileMeasurement to write.
   */
  @Override
  public void write(ProfileMeasurement m) {
    byte[] rowKey = rowKeyBuilder.rowKey(m);
    ColumnList cols = columnBuilder.columns(m);
    hbaseClient.addMutation(rowKey, cols, Durability.SKIP_WAL);
    hbaseClient.mutate();
  }


}
