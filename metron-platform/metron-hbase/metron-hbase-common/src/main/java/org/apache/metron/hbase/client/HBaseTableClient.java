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
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.hbase.ColumnList;
import org.apache.metron.hbase.HBaseProjectionCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.size;

/**
 * An {@link HBaseClient} that uses the {@link Table} API to interact with HBase.
 */
public class HBaseTableClient implements HBaseClient {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private List<Mutation> mutations;
  private List<Get> gets;
  private Connection connection;
  private Table table;

  /**
   * @param connectionFactory Creates connections to HBase.
   * @param configuration The HBase configuration.
   * @param tableName The name of the HBase table.
   */
  public HBaseTableClient(HBaseConnectionFactory connectionFactory, Configuration configuration, String tableName) throws IOException {
    gets = new ArrayList<>();
    mutations = new ArrayList<>();
    connection = connectionFactory.createConnection(configuration);
    table = connection.getTable(TableName.valueOf(tableName));
  }

  @Override
  public void close() throws IOException {
    if(table != null) {
      table.close();
    }
    if(connection != null) {
      connection.close();
    }
  }

  @Override
  public void addGet(byte[] rowKey, HBaseProjectionCriteria criteria) {
    Get get = new Get(rowKey);

    // define which column families and columns are needed
    if (criteria != null) {
      criteria.getColumnFamilies().forEach(cf -> get.addFamily(cf));
      criteria.getColumns().forEach(col -> get.addColumn(col.getColumnFamily(), col.getQualifier()));
    }

    // queue the get
    this.gets.add(get);
  }

  @Override
  public Result[] getAll() {
    try {
      return table.get(gets);

    } catch (Exception e) {
      String msg = String.format("'%d' HBase read(s) failed on table '%s'", size(gets), tableName(table));
      LOG.error(msg, e);
      throw new RuntimeException(msg, e);

    } finally {
      gets.clear();
    }
  }

  @Override
  public void clearGets() {
    gets.clear();
  }

  @Override
  public List<String> scanRowKeys() throws IOException {
    List<String> rowKeys = new ArrayList<>();
    ResultScanner scanner = getScanner();
    for (Result r = scanner.next(); r != null; r = scanner.next()) {
      String rowKeyAsString = Bytes.toString(r.getRow());
      rowKeys.add(rowKeyAsString);
    }
    return rowKeys;
  }

  @Override
  public Result[] scan(int numRows) throws IOException {
    return getScanner().next(numRows);
  }

  private ResultScanner getScanner() throws IOException {
    Scan scan = new Scan();
    return table.getScanner(scan);
  }

  /**
   * Returns the name of the HBase table.
   * <p>Attempts to avoid any null pointers that might be encountered along the way.
   * @param table The table to retrieve the name of.
   * @return The name of the table
   */
  private static String tableName(Table table) {
    String tableName = "null";
    if(table != null) {
      if(table.getName() != null) {
        tableName = table.getName().getNameAsString();
      }
    }
    return tableName;
  }

  @Override
  public void addMutation(byte[] rowKey, ColumnList cols) {
    HBaseWriterParams params = new HBaseWriterParams();
    addMutation(rowKey, cols, params);
  }

  @Override
  public void addMutation(byte[] rowKey, ColumnList cols, Durability durability) {
    HBaseWriterParams params = new HBaseWriterParams()
            .withDurability(durability);
    addMutation(rowKey, cols, params);
  }

  @Override
  public void addMutation(byte[] rowKey, ColumnList cols, Durability durability, Long timeToLiveMillis) {
    HBaseWriterParams params = new HBaseWriterParams()
            .withDurability(durability)
            .withTimeToLive(timeToLiveMillis);
    addMutation(rowKey, cols, params);
  }

  private void addMutation(byte[] rowKey, ColumnList cols, HBaseWriterParams params) {
    if (cols.hasColumns()) {
      Put put = createPut(rowKey, params);
      addColumns(cols, put);
      mutations.add(put);
    }
    if (cols.hasCounters()) {
      Increment inc = createIncrement(rowKey, params);
      addColumns(cols, inc);
      mutations.add(inc);
    }
  }

  @Override
  public void clearMutations() {
    mutations.clear();
  }

  @Override
  public int mutate() {
    int mutationCount = mutations.size();
    if(mutationCount > 0) {
      doMutate();
    }

    return mutationCount;
  }

  @Override
  public void delete(byte[] rowKey) {
    try {
      Delete delete = new Delete(rowKey);
      table.delete(delete);

    } catch (Exception e) {
      String msg = String.format("Unable to delete; table=%s", tableName(table));
      LOG.error(msg, e);
      throw new RuntimeException(msg, e);
    }
  }

  @Override
  public void delete(byte[] rowKey, ColumnList columnList) {
    try {
      Delete delete = new Delete(rowKey);
      for(ColumnList.Column column: columnList.getColumns()) {
        delete.addColumn(column.getFamily(), column.getQualifier());
      }
      table.delete(delete);

    } catch (Exception e) {
      String msg = String.format("Unable to delete; table=%s", tableName(table));
      LOG.error(msg, e);
      throw new RuntimeException(msg, e);
    }
  }

  private void doMutate() {
    Object[] result = new Object[mutations.size()];
    try {
      table.batch(mutations, result);

    } catch (Exception e) {
      String msg = String.format("'%d' HBase write(s) failed on table '%s'", size(mutations), tableName(table));
      LOG.error(msg, e);
      throw new RuntimeException(msg, e);

    } finally {
      mutations.clear();
    }
  }

  private Put createPut(byte[] rowKey, HBaseWriterParams params) {
    Put put = new Put(rowKey);
    if(params.getTimeToLiveMillis() > 0) {
      put.setTTL(params.getTimeToLiveMillis());
    }
    put.setDurability(params.getDurability());
    return put;
  }

  private void addColumns(ColumnList cols, Put put) {
    for (ColumnList.Column col: cols.getColumns()) {
      if (col.getTs() > 0) {
        put.addColumn(col.getFamily(), col.getQualifier(), col.getTs(), col.getValue());
      } else {
        put.addColumn(col.getFamily(), col.getQualifier(), col.getValue());
      }
    }
  }

  private void addColumns(ColumnList cols, Increment inc) {
    cols.getCounters().forEach(cnt ->
            inc.addColumn(cnt.getFamily(), cnt.getQualifier(), cnt.getIncrement()));
  }

  private Increment createIncrement(byte[] rowKey, HBaseWriterParams params) {
    Increment inc = new Increment(rowKey);
    if(params.getTimeToLiveMillis() > 0) {
      inc.setTTL(params.getTimeToLiveMillis());
    }
    inc.setDurability(params.getDurability());
    return inc;
  }
}
