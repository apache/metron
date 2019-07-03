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
package org.apache.metron.hbase.client;

import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Result;
import org.apache.metron.hbase.ColumnList;
import org.apache.metron.hbase.HBaseProjectionCriteria;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * A client that interacts with HBase.
 */
public interface HBaseClient extends Closeable {

  /**
   * Enqueues a 'get' request that will be submitted when {@link #getAll()} is called.
   * @param rowKey The row key to be retrieved.
   */
  void addGet(byte[] rowKey, HBaseProjectionCriteria criteria);

  /**
   * Submits all pending get operations and returns the result of each.
   * @return The result of each pending get request.
   */
  Result[] getAll();

  /**
   * Clears all pending get operations.
   */
  void clearGets();

  /**
   * Scans an entire table returning all row keys as a List of Strings.
   *
   * <p><b>**WARNING**:</b> Do not use this method unless you're absolutely crystal clear about the performance
   * impact. Doing full table scans in HBase can adversely impact performance.
   *
   * @return List of all row keys as Strings for this table.
   */
  List<String> scanRowKeys() throws IOException;

  /**
   * Scans the table and returns each result.
   *
   * <p><b>**WARNING**:</b> Do not use this method unless you're absolutely crystal clear about the performance
   * impact. Doing full table scans in HBase can adversely impact performance.
   *
   * @return The results from the scan.
   * @throws IOException
   */
  Result[] scan(int numRows) throws IOException;

  /**
   * Enqueues a {@link org.apache.hadoop.hbase.client.Mutation} such as a put or
   * increment.  The operation is enqueued for later execution.
   *
   * @param rowKey     The row key of the Mutation.
   * @param cols       The columns affected by the Mutation.
   */
  void addMutation(byte[] rowKey, ColumnList cols);

  /**
   * Enqueues a {@link org.apache.hadoop.hbase.client.Mutation} such as a put or
   * increment.  The operation is enqueued for later execution.
   *
   * @param rowKey     The row key of the Mutation.
   * @param cols       The columns affected by the Mutation.
   * @param durability The durability of the mutation.
   */
  void addMutation(byte[] rowKey, ColumnList cols, Durability durability);

  /**
   * Enqueues a {@link org.apache.hadoop.hbase.client.Mutation} such as a put or
   * increment.  The operation is enqueued for later execution.
   *
   * @param rowKey           The row key of the Mutation.
   * @param cols             The columns affected by the Mutation.
   * @param durability       The durability of the mutation.
   * @param timeToLiveMillis The time to live in milliseconds.
   */
  void addMutation(byte[] rowKey, ColumnList cols, Durability durability, Long timeToLiveMillis);

  /**
   * Ensures that all pending mutations have completed.
   *
   * @return The number of operations completed.
   */
  int mutate();

  /**
   * Clears all pending mutations.
   */
  void clearMutations();

  /**
   * Delete a record by row key.
   *
   * @param rowKey The row key to delete.
   */
  void delete(byte[] rowKey);

  /**
   * Delete a column or set of columns by row key.
   *
   * @param rowKey The row key to delete.
   * @param columnList The set of columns to delete.
   */
  void delete(byte[] rowKey, ColumnList columnList);
}
