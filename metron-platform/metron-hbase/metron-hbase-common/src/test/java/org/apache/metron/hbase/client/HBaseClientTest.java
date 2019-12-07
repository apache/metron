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
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.hbase.ColumnList;
import org.apache.metron.hbase.HBaseProjectionCriteria;
import org.apache.metron.hbase.TableProvider;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the HBaseClient
 */
public class HBaseClientTest {

  private static final String tableName = "table";

  private static HBaseTestingUtility util;
  private static HBaseClient client;
  private static Table table;
  private static Admin admin;
  private static byte[] cf = Bytes.toBytes("cf");
  private static byte[] column = Bytes.toBytes("column");
  byte[] rowKey1;
  byte[] rowKey2;
  byte[] value1 = Bytes.toBytes("value1");
  byte[] value2 = Bytes.toBytes("value2");
  ColumnList cols1;
  ColumnList cols2;

  @BeforeAll
  public static void startHBase() throws Exception {
    Configuration config = HBaseConfiguration.create();
    config.set("hbase.master.hostname", "localhost");
    config.set("hbase.regionserver.hostname", "localhost");
    util = new HBaseTestingUtility(config);
    util.startMiniCluster();
    admin = util.getHBaseAdmin();
    // create the table
    table = util.createTable(Bytes.toBytes(tableName), cf);
    util.waitTableEnabled(table.getName());
    // setup the client
    client = new HBaseClient((c,t) -> table, table.getConfiguration(), tableName);
  }

  @AfterAll
  public static void stopHBase() throws Exception {
    util.deleteTable(tableName);
    util.shutdownMiniCluster();
    util.cleanupTestDir();
  }

  @AfterEach
  public void clearTable() throws Exception {
    List<Delete> deletions = new ArrayList<>();
    for(Result r : table.getScanner(new Scan())) {
      deletions.add(new Delete(r.getRow()));
    }
    table.delete(deletions);
  }

  @BeforeEach
  public void setupTuples() {
    rowKey1 = Bytes.toBytes("rowKey1");
    cols1 = new ColumnList();
    cols1.addColumn(cf, column, value1);

    rowKey2 = Bytes.toBytes("rowKey2");
    cols2 = new ColumnList();
    cols2.addColumn(cf, column, value2);
  }

  /**
   * Should be able to read/write a single row.
   */
  @Test
  public void testWrite() throws Exception {

    // add a tuple to the batch
    client.addMutation(rowKey1, cols1, Durability.SYNC_WAL);
    client.mutate();

    HBaseProjectionCriteria criteria = new HBaseProjectionCriteria();
    criteria.addColumnFamily(Bytes.toString(cf));

    // read back the tuple
    client.addGet(rowKey1, criteria);
    Result[] results = client.getAll();
    assertEquals(1, results.length);

    // validate
    assertEquals(1, results.length);
    assertArrayEquals(rowKey1, results[0].getRow());
    assertArrayEquals(value1, results[0].getValue(cf, column));
  }

  /**
   * Should be able to read/write multiple rows in a batch.
   */
  @Test
  public void testBatchWrite() {

    // add two mutations to the queue
    client.addMutation(rowKey1, cols1, Durability.SYNC_WAL);
    client.addMutation(rowKey2, cols2, Durability.SYNC_WAL);
    int count = client.mutate();

    // there were two mutations
    assertEquals(2, count);

    HBaseProjectionCriteria criteria = new HBaseProjectionCriteria();
    criteria.addColumnFamily(Bytes.toString(cf));

    // read back both tuples
    client.addGet(rowKey1, criteria);
    client.addGet(rowKey2, criteria);
    Result[] results = client.getAll();

    // validate
    assertEquals(2, results.length);
    assertArrayEquals(rowKey1, results[0].getRow());
    assertArrayEquals(value1, results[0].getValue(cf, column));
    assertArrayEquals(rowKey1, results[0].getRow());
    assertArrayEquals(value2, results[1].getValue(cf, column));
  }

  /**
   * What happens when there is nothing in the batch to write?
   */
  @Test
  public void testEmptyBatch() {

    // do not add any mutations before attempting to write
    int count = client.mutate();
    assertEquals(0, count);

    HBaseProjectionCriteria criteria = new HBaseProjectionCriteria();
    criteria.addColumnFamily(Bytes.toString(cf));

    // read back both
    client.addGet(rowKey1, criteria);
    client.addGet(rowKey2, criteria);
    Result[] results = client.getAll();

    // validate - there should be nothing to find
    assertEquals(2, results.length);
    for(Result result : results) {
      assertTrue(result.isEmpty());
    }
  }

  /**
   * Should be able to read back rows that were written with a TTL 30 days out.
   */
  @Test
  public void testWriteWithTimeToLive() {
    long timeToLive = TimeUnit.DAYS.toMillis(30);

    // add two mutations to the queue
    client.addMutation(rowKey1, cols1, Durability.SYNC_WAL, timeToLive);
    client.addMutation(rowKey2, cols2, Durability.SYNC_WAL, timeToLive);
    client.mutate();

    HBaseProjectionCriteria criteria = new HBaseProjectionCriteria();
    criteria.addColumnFamily(Bytes.toString(cf));

    // read back both tuples
    client.addGet(rowKey1, criteria);
    client.addGet(rowKey2, criteria);
    Result[] results = client.getAll();

    // validate
    assertEquals(2, results.length);
    assertArrayEquals(rowKey1, results[0].getRow());
    assertArrayEquals(value1, results[0].getValue(cf, column));
    assertArrayEquals(rowKey1, results[0].getRow());
    assertArrayEquals(value2, results[1].getValue(cf, column));
  }

  /**
   * Should NOT be able to read rows that are expired due to the TTL.
   */
  @Test
  public void testExpiredRows() throws Exception {
    long timeToLive = TimeUnit.MILLISECONDS.toMillis(1);

    // add two mutations to the queue
    client.addMutation(rowKey1, cols1, Durability.SYNC_WAL, timeToLive);
    client.addMutation(rowKey2, cols2, Durability.SYNC_WAL, timeToLive);
    client.mutate();

    HBaseProjectionCriteria criteria = new HBaseProjectionCriteria();
    criteria.addColumnFamily(Bytes.toString(cf));

    // wait for a second to ensure the TTL has expired
    Thread.sleep(TimeUnit.SECONDS.toMillis(2));

    // read back both rows
    client.addGet(rowKey1, criteria);
    client.addGet(rowKey2, criteria);
    Result[] results = client.getAll();

    // validate - the TTL should have expired all rows
    assertEquals(2, results.length);
    assertTrue(results[0].isEmpty());
    assertTrue(results[1].isEmpty());
  }

  @Test
  public void testUnableToOpenConnection() throws IOException {
    // used to trigger a failure condition
    TableProvider tableProvider = mock(TableProvider.class);
    when(tableProvider.getTable(any(), any())).thenThrow(new IllegalArgumentException("test exception"));

    assertThrows(
        RuntimeException.class,
        () -> client = new HBaseClient(tableProvider, HBaseConfiguration.create(), tableName));
  }

  @Test
  public void testFailureToMutate() throws IOException, InterruptedException {
    // used to trigger a failure condition in `HbaseClient.mutate`
    Table table = mock(Table.class);
    doThrow(new IOException("exception!")).when(table).batch(any(), any());

    TableProvider tableProvider = mock(TableProvider.class);
    when(tableProvider.getTable(any(), any())).thenReturn(table);

    client = new HBaseClient(tableProvider, HBaseConfiguration.create(), tableName);
    client.addMutation(rowKey1, cols1, Durability.SYNC_WAL);
    assertThrows(RuntimeException.class, () -> client.mutate());
  }

  @Test
  public void testFailureToGetAll() throws IOException {
    // used to trigger a failure condition in `HbaseClient.getAll`
    Table table = mock(Table.class);
    when(table.get(anyList())).thenThrow(new IOException("exception!"));

    TableProvider tableProvider = mock(TableProvider.class);
    when(tableProvider.getTable(any(), any())).thenReturn(table);

    HBaseProjectionCriteria criteria = new HBaseProjectionCriteria();
    criteria.addColumnFamily(Bytes.toString(cf));

    client = new HBaseClient(tableProvider, HBaseConfiguration.create(), tableName);
    client.addGet(rowKey1, criteria);
    client.addGet(rowKey2, criteria);
    assertThrows(RuntimeException.class, () -> client.getAll());
  }
}
