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
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.hbase.ColumnList;
import org.apache.metron.hbase.HBaseProjectionCriteria;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests the {@link FakeHBaseClient} class.
 */
public class FakeHBaseClientTest {
  private static final String columnFamily = "W";
  private static final byte[] columnFamilyB = Bytes.toBytes(columnFamily);
  private static final String columnQualifier = "column";
  private static final byte[] columnQualifierB = Bytes.toBytes(columnQualifier);
  private static final String rowKey1String = "row-key-1";
  private static final byte[] rowKey1 = Bytes.toBytes(rowKey1String);
  private static final String rowKey2String = "row-key-2";
  private static final byte[] rowKey2 = Bytes.toBytes(rowKey2String);

  private FakeHBaseClient client;

  @Before
  public void setup() {
    client = new FakeHBaseClient();
    client.deleteAll();
  }

  @Test
  public void testMutate() throws Exception {
    // write some values
    ColumnList columns = new ColumnList()
            .addColumn(columnFamily, columnQualifier, "value1");
    client.addMutation(rowKey1, columns, Durability.SKIP_WAL);
    client.mutate();

    // read back the value
    client.addGet(rowKey1, new HBaseProjectionCriteria().addColumnFamily(columnFamily));
    Result[] results = client.getAll();

    // validate
    assertEquals(1, results.length);
    assertEquals("value1", getValue(results[0], columnFamily, columnQualifier));
  }

  @Test
  public void testMutateMultipleColumns() throws Exception {
    // write some values
    ColumnList columns = new ColumnList()
            .addColumn(columnFamily, "col1", "value1")
            .addColumn(columnFamily, "col2", "value2");
    client.addMutation(rowKey1, columns, Durability.SKIP_WAL);
    client.mutate();

    // read back the value
    client.addGet(rowKey1, new HBaseProjectionCriteria().addColumnFamily(columnFamily));
    Result[] results = client.getAll();

    // validate
    assertEquals(1, results.length);
    assertEquals("value1", getValue(results[0], columnFamily, "col1"));
    assertEquals("value2", getValue(results[0], columnFamily, "col2"));
  }

  @Test
  public void testNoMutations() throws Exception {
    // do not add any mutations before attempting to write
    int count = client.mutate();
    Assert.assertEquals(0, count);

    // attempt to read
    HBaseProjectionCriteria criteria = new HBaseProjectionCriteria().addColumnFamily(columnFamily);
    client.addGet(rowKey1, criteria);
    client.addGet(rowKey2, criteria);
    Result[] results = client.getAll();

    // nothing should have been read
    assertEquals(2, results.length);
    for(Result result : results) {
      Assert.assertTrue(result.isEmpty());
    }
  }

  /**
   * Unfortunately, the {@link Result} returned by the {@link FakeHBaseClient} is a mock and needs
   * to respond like an actual {@link Result}.  This test ensures that {@link Result#getFamilyMap(byte[])}
   * works correctly.
   */
  @Test
  public void testResultFamilyMap() {
    // write some values
    ColumnList columns = new ColumnList()
            .addColumn(columnFamily, "col1", "value1")
            .addColumn(columnFamily, "col2", "value2");
    client.addMutation(rowKey1, columns, Durability.SKIP_WAL);
    client.mutate();

    // read back the value
    HBaseProjectionCriteria criteria = new HBaseProjectionCriteria()
            .addColumnFamily(columnFamily);
    client.addGet(rowKey1, criteria);
    Result[] results = client.getAll();

    // validate
    assertEquals(1, results.length);
    Result result = results[0];

    // the first column
    String value1 = Bytes.toString(result.getFamilyMap(columnFamilyB).get(Bytes.toBytes("col1")));
    assertEquals("value1", value1);

    // the second column
    String value2 = Bytes.toString(result.getFamilyMap(columnFamilyB).get(Bytes.toBytes("col2")));
    assertEquals("value2", value2);
  }

  @Test
  public void testScan() throws Exception {
    // write some values
    client.addMutation(rowKey1, new ColumnList().addColumn(columnFamily, columnQualifier, "value1"), Durability.SKIP_WAL);
    client.mutate();

    // scan the table
    Result[] results = client.scan(10);
    assertEquals(1, results.length);

    assertArrayEquals(rowKey1, results[0].getRow());
    String actual1 = Bytes.toString(results[0].getValue(columnFamilyB, columnQualifierB));
    assertEquals("value1", actual1);
  }

  @Test
  public void testScanLimit() throws Exception {
    // write some values
    client.addMutation(rowKey1, new ColumnList().addColumn(columnFamily, columnQualifier, "value1"), Durability.SKIP_WAL);
    client.addMutation(rowKey2, new ColumnList().addColumn(columnFamily, columnQualifier, "value2"), Durability.SKIP_WAL);
    client.mutate();

    // scan the table, but limit to 1 result
    Result[] results = client.scan(1);
    assertEquals(1, results.length);
  }

  @Test
  public void testScanNothing() throws Exception {
    // scan the table, but there is nothing there
    Result[] results = client.scan(1);
    assertEquals(0, results.length);
  }

  @Test
  public void testScanRowKeys() throws Exception {
    // write some values
    client.addMutation(rowKey1, new ColumnList().addColumn(columnFamily, columnQualifier, "value1"), Durability.SKIP_WAL);
    client.addMutation(rowKey2, new ColumnList().addColumn(columnFamily, columnQualifier, "value2"), Durability.SKIP_WAL);
    client.mutate();

    // scan the table
    List<String> rowKeys = client.scanRowKeys();
    List<String> expected = Arrays.asList(rowKey1String, rowKey2String);
    assertEquals(new HashSet<>(expected), new HashSet<>(rowKeys));
  }

  @Test
  public void testDelete() {
    // write some values
    client.addMutation(rowKey1, new ColumnList().addColumn(columnFamily, columnQualifier, "value1"), Durability.SKIP_WAL);
    client.addMutation(rowKey2, new ColumnList().addColumn(columnFamily, columnQualifier, "value2"), Durability.SKIP_WAL);
    client.mutate();

    client.delete(rowKey1);

    // the deleted row key should no longer exist
    client.addGet(rowKey1, new HBaseProjectionCriteria().addColumnFamily(columnFamily));
    Assert.assertTrue(client.getAll()[0].isEmpty());

    // the other row key should remain
    client.addGet(rowKey2, new HBaseProjectionCriteria().addColumnFamily(columnFamily));
    Assert.assertFalse(client.getAll()[0].isEmpty());
  }

  @Test
  public void testDeleteNothing() {
    // nothing should blow-up if we attempt to delete something that does not exist
    client.delete(rowKey1);
  }

  @Test
  public void testDeleteColumn() {
    // write some values
    ColumnList columns = new ColumnList()
            .addColumn(columnFamily, "col1", "value1")
            .addColumn(columnFamily, "col2", "value2");
    client.addMutation(rowKey1, columns, Durability.SKIP_WAL);
    client.mutate();

    // delete a column
    client.delete(rowKey1, new ColumnList().addColumn(columnFamily, "col1"));

    // read back the value
    client.addGet(rowKey1, new HBaseProjectionCriteria().addColumnFamily(columnFamily));
    Result[] results = client.getAll();

    // validate
    assertEquals(1, results.length);
    assertNull(getValue(results[0], columnFamily, "col1"));
    assertEquals("value2", getValue(results[0], columnFamily, "col2"));
  }

  @Test
  public void testDeleteAllColumns() {
    // write some values
    ColumnList columns = new ColumnList()
            .addColumn(columnFamily, "col1", "value1")
            .addColumn(columnFamily, "col2", "value2");
    client.addMutation(rowKey1, columns, Durability.SKIP_WAL);
    client.mutate();

    // delete both columns individually
    client.delete(rowKey1, new ColumnList().addColumn(columnFamily, "col1"));
    client.delete(rowKey1, new ColumnList().addColumn(columnFamily, "col2"));

    // read back the value
    client.addGet(rowKey1, new HBaseProjectionCriteria().addColumnFamily(columnFamily));
    Result[] results = client.getAll();

    // validate
    assertEquals(1, results.length);
    assertNull(getValue(results[0], columnFamily, "col1"));
    assertNull(getValue(results[0], columnFamily, "col2"));
  }

  private String getValue(Result result, String columnFamily, String columnQualifier) {
    byte[] value = result.getValue(Bytes.toBytes(columnFamily), Bytes.toBytes(columnQualifier));
    return Bytes.toString(value);
  }
}
