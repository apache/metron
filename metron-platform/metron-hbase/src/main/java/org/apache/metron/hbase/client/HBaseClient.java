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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.hbase.bolt.mapper.ColumnList;
import org.apache.metron.hbase.bolt.mapper.HBaseProjectionCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A client that interacts with HBase.
 */
public class HBaseClient implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(HBaseClient.class);

  /**
   * The batch of queued Mutations.
   */
  List<Mutation> mutations;

  /**
   * The batch of queued Gets.
   */
  List<Get> gets;

  /**
   * The HBase table this client interacts with.
   */
  private HTableInterface table;

  public HBaseClient(TableProvider provider, final Configuration configuration, final String tableName) {
    this.mutations = new ArrayList<>();
    this.gets = new ArrayList<>();
    try {
      this.table = provider.getTable(configuration, tableName);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Add a Mutation such as a Put or Increment to the batch.  The Mutation is only queued for
   * later execution.
   *
   * @param rowKey     The row key of the Mutation.
   * @param cols       The columns affected by the Mutation.
   * @param durability The durability of the mutation.
   */
  public void addMutation(byte[] rowKey, ColumnList cols, Durability durability) {

    if (cols.hasColumns()) {
      Put put = createPut(rowKey, cols, durability);
      mutations.add(put);
    }

    if (cols.hasCounters()) {
      Increment inc = createIncrement(rowKey, cols, durability);
      mutations.add(inc);
    }

    if (mutations.isEmpty()) {
      mutations.add(new Put(rowKey));
    }
  }

  /**
   * Adds a Mutation such as a Put or Increment with a time to live.  The Mutation is only queued
   * for later execution.
   *
   * @param rowKey           The row key of the Mutation.
   * @param cols             The columns affected by the Mutation.
   * @param durability       The durability of the mutation.
   * @param timeToLiveMillis The time to live in milliseconds.
   */
  public void addMutation(byte[] rowKey, ColumnList cols, Durability durability, Long timeToLiveMillis) {

    if (cols.hasColumns()) {
      Put put = createPut(rowKey, cols, durability, timeToLiveMillis);
      mutations.add(put);
    }

    if (cols.hasCounters()) {
      Increment inc = createIncrement(rowKey, cols, durability, timeToLiveMillis);
      mutations.add(inc);
    }

    if (mutations.isEmpty()) {
      Put put = new Put(rowKey);
      put.setTTL(timeToLiveMillis);
      mutations.add(put);
    }
  }

  /**
   * Remove all queued Mutations from the batch.
   */
  public void clearMutations() {
    mutations.clear();
  }

  /**
   * Submits all queued Mutations.
   */
  public void mutate() {
    Object[] result = new Object[mutations.size()];
    try {
      table.batch(mutations, result);
      mutations.clear();

    } catch (InterruptedException | IOException e) {
      LOG.warn("Error performing a mutation to HBase.", e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Adds a Get to the batch.
   *
   * @param rowKey   The row key of the Get
   * @param criteria Defines the columns/families that will be retrieved.
   */
  public void addGet(byte[] rowKey, HBaseProjectionCriteria criteria) {
    Get get = new Get(rowKey);

    if (criteria != null) {
      criteria.getColumnFamilies().forEach(cf -> get.addFamily(cf));
      criteria.getColumns().forEach(col -> get.addColumn(col.getColumnFamily(), col.getQualifier()));
    }

    // queue the get
    this.gets.add(get);
  }

  /**
   * Clears all queued Gets from the batch.
   */
  public void clearGets() {
    gets.clear();
  }

  /**
   * Submit all queued Gets.
   *
   * @return The Result of each queued Get.
   */
  public Result[] getAll() {
    try {
      Result[] results = table.get(gets);
      gets.clear();
      return results;

    } catch (Exception e) {
      LOG.warn("Could not perform HBase lookup.", e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Close the table.
   */
  @Override
  public void close() throws IOException {
    table.close();
  }

  /**
   * Creates an HBase Put.
   *
   * @param rowKey     The row key.
   * @param cols       The columns to put.
   * @param durability The durability of the put.
   */
  private Put createPut(byte[] rowKey, ColumnList cols, Durability durability) {
    Put put = new Put(rowKey);
    put.setDurability(durability);
    addColumns(cols, put);
    return put;
  }

  /**
   * Creates an HBase Put.
   *
   * @param rowKey           The row key.
   * @param cols             The columns to put.
   * @param durability       The durability of the put.
   * @param timeToLiveMillis The TTL in milliseconds.
   */
  private Put createPut(byte[] rowKey, ColumnList cols, Durability durability, long timeToLiveMillis) {
    Put put = new Put(rowKey);
    put.setDurability(durability);
    put.setTTL(timeToLiveMillis);
    addColumns(cols, put);
    return put;
  }

  /**
   * Adds the columns to the Put
   *
   * @param cols The columns to add.
   * @param put  The Put.
   */
  private void addColumns(ColumnList cols, Put put) {
    for (ColumnList.Column col : cols.getColumns()) {

      if (col.getTs() > 0) {
        put.add(col.getFamily(), col.getQualifier(), col.getTs(), col.getValue());

      } else {
        put.add(col.getFamily(), col.getQualifier(), col.getValue());
      }
    }
  }

  /**
   * Creates an HBase Increment for a counter.
   *
   * @param rowKey     The row key.
   * @param cols       The columns to include.
   * @param durability The durability of the increment.
   */
  private Increment createIncrement(byte[] rowKey, ColumnList cols, Durability durability) {
    Increment inc = new Increment(rowKey);
    inc.setDurability(durability);
    cols.getCounters().forEach(cnt -> inc.addColumn(cnt.getFamily(), cnt.getQualifier(), cnt.getIncrement()));
    return inc;
  }

  /**
   * Creates an HBase Increment for a counter.
   *
   * @param rowKey     The row key.
   * @param cols       The columns to include.
   * @param durability The durability of the increment.
   */
  private Increment createIncrement(byte[] rowKey, ColumnList cols, Durability durability, long timeToLiveMillis) {
    Increment inc = new Increment(rowKey);
    inc.setDurability(durability);
    inc.setTTL(timeToLiveMillis);
    cols.getCounters().forEach(cnt -> inc.addColumn(cnt.getFamily(), cnt.getQualifier(), cnt.getIncrement()));
    return inc;
  }
}
