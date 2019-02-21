/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.hbase.coprocessor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Level;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.dataloads.hbase.mr.HBaseUtil;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.test.utils.UnitTestHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class EnrichmentCoprocessorIntegrationTest {

  private static final String ENRICHMENT_TABLE = "enrichment";
  private static final String ENRICHMENT_LIST_TABLE = "enrichment_list";
  private static final String CF = "c";
  private static final String CQ = "q";

  private static Level originalLog4jRootLoggerLevel;
  private static java.util.logging.Level originalJavaLoggerLevel;
  private static HBaseTestingUtility testUtil;
  private static HTable enrichmentTable;
  private static HTable enrichmentListTable;
  private static Configuration config;

  @BeforeClass
  public static void setupAll() throws Exception {
    silenceLogging();
    Map.Entry<HBaseTestingUtility, Configuration> kv = HBaseUtil.INSTANCE.create(true);
    testUtil = kv.getKey();
    config = kv.getValue();
    enrichmentTable = testUtil.createTable(Bytes.toBytes(ENRICHMENT_TABLE), Bytes.toBytes(CF));
    enrichmentListTable = testUtil
        .createTable(Bytes.toBytes(ENRICHMENT_LIST_TABLE), Bytes.toBytes(CF));

    for (Result r : enrichmentTable.getScanner(Bytes.toBytes(CF))) {
      Delete d = new Delete(r.getRow());
      enrichmentTable.delete(d);
    }
    for (Result r : enrichmentListTable.getScanner(Bytes.toBytes(CF))) {
      Delete d = new Delete(r.getRow());
      enrichmentListTable.delete(d);
    }

    // https://hbase.apache.org/1.1/book.html#cp_loading
    Admin hbaseAdmin = testUtil.getConnection().getAdmin();
    TableName tableName = enrichmentTable.getName();
    hbaseAdmin.disableTable(tableName);
    HTableDescriptor htd = new HTableDescriptor(tableName);
    htd.addFamily(new HColumnDescriptor(CF));
    for (int j = 1; j <= 2; j++) {
      String colFamily = "cf-" + j;
      HColumnDescriptor colDesc = new HColumnDescriptor(colFamily);
      htd.addFamily(colDesc);
    }
    htd.addCoprocessor(EnrichmentCoprocessor.class.getCanonicalName());
    hbaseAdmin.modifyTable(tableName, htd);
    hbaseAdmin.enableTable(tableName);
  }

  private static void silenceLogging() {
    originalLog4jRootLoggerLevel = UnitTestHelper.getLog4jLevel();
    originalJavaLoggerLevel = UnitTestHelper.getJavaLoggingLevel();
    UnitTestHelper.setLog4jLevel(Level.ERROR);
    UnitTestHelper.setLog4jLevel(EnrichmentCoprocessor.class, Level.DEBUG);
    UnitTestHelper.setLog4jLevel(HBaseCacheWriter.class, Level.DEBUG);
    UnitTestHelper.setJavaLoggingLevel(java.util.logging.Level.SEVERE);
  }

  @AfterClass
  public static void teardown() throws Exception {
    HBaseUtil.INSTANCE.teardown(testUtil);
    resetLogging();
  }

  private static void resetLogging() {
    UnitTestHelper.setLog4jLevel(originalLog4jRootLoggerLevel);
    UnitTestHelper.setJavaLoggingLevel(originalJavaLoggerLevel);
  }

  /*
  @Test
  public void load_enrichments() throws Exception {
    Map<String, String> rows = new HashMap<>();
    for (int i = 0; i < 5; i++) {
      String rowKey = "rowkey-" + i;
      String value = "value-" + i;
      rows.put(rowKey, value);
      MyHBaseDAO.insertRecord(enrichmentTable, rowKey, value);
      Result result = MyHBaseDAO.readRecord(enrichmentTable, rowKey);
      assertThat(Bytes.toString(result.getRow()), equalTo(rowKey));
      assertThat(Bytes.toString(result.value()), equalTo(value));
      MyHBaseDAO.deleteRecord(enrichmentTable, rowKey);
      result = MyHBaseDAO.readRecord(enrichmentTable, rowKey);
      assertThat(result.isEmpty(), equalTo(true));
    }
  }

  @Test
  public void load_enrichments_with_multiple_cf() throws Exception {
    System.out.println("loading data");
    for (int i = 1; i <= 5; i++) {
      String rowKey = "rowkey-" + i;
      for (int j = 1; j <= 2; j++) {
        String colFamily = "cf-" + j;
        for (int k = 1; k <= 2; k++) {
          String colQualifier = "cq-" + k;
          String value = "value-" + i + "" + j + "" + k;
          MyHBaseDAO.insertRecord(enrichmentTable, rowKey, colFamily, colQualifier, value);
        }
      }
    }
    MyHBaseDAO.readRecords(enrichmentListTable);
    for (int i = 1; i <= 5; i++) {
      String rowKey = "rowkey-" + i;
      Result result = MyHBaseDAO.readRecord(enrichmentTable, rowKey);
      assertThat(Bytes.toString(result.getRow()), equalTo(rowKey));
      printResults(result);
      result = MyHBaseDAO.readRecord(testTable, "rowkey-1", "cf-1");
      printResults(result);
    }

    System.out.println("Updating a record");
    MyHBaseDAO.insertRecord(enrichmentTable, "rowkey-1", "cf-1", "cq-1", "some-new-value");
    System.out.println("Done updating record");

    MyHBaseDAO.deleteRecord(testTable, rowKey);
    result = MyHBaseDAO.readRecord(testTable, rowKey);
    assertThat(result.isEmpty(), equalTo(true));
  }
*/
  @Test
  public void enrichments_loaded_in_list_table() throws Exception {
    // indicator, type
    Map<String, String> enrichments = new HashMap<String, String>() {{
      put("111", "foo");
      put("222", "foo");
      put("333", "bar");
      put("444", "bar");
      put("555", "baz");
      put("666", "baz");
    }};
    Set<String> expectedEnrichmentTypes = new HashSet<>();
    for (Map.Entry<String, String> enrichKV : enrichments.entrySet()) {
      String indicator = enrichKV.getKey();
      String type = enrichKV.getValue();
      expectedEnrichmentTypes.add(type);
      MyHBaseDAO.insertRecord(enrichmentTable, new EnrichmentKey(type, indicator), "{ \"apache\" : \"metron\" }");
    }
    List<String> enrichmentsList = MyHBaseDAO.readRecords(enrichmentListTable);
    assertThat(new HashSet<String>(enrichmentsList), equalTo(expectedEnrichmentTypes));
    System.out.println("Enrichments table");
    printResults(MyHBaseDAO.readRecordsAll(enrichmentTable));
    System.out.println("Enrichments list table");
    printResults(MyHBaseDAO.readRecordsAll(enrichmentListTable));
  }

  private void printResults(List<Result> results) {
    for (Result result : results) {
      printResults(result);
    }
  }

  private void printResults(final Result result) {
    final String tab = "    ";
    final String rowKey = Bytes.toString(result.getRow());
    System.out.println("[ START rowKey: " + rowKey + "]");
    int i = 0;
    for (Entry<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> family : result.getMap()
        .entrySet()) {
      System.out.println(tab + (++i) + ". family: " + Bytes.toString(family.getKey()));
      int j = 0;
      for (Entry<byte[], NavigableMap<Long, byte[]>> qualifier : family.getValue().entrySet()) {
        System.out
            .println(tab + tab + (++j) + ". qualifier: " + Bytes.toString(qualifier.getKey()));
        System.out.println(tab + tab + " num versions: " + qualifier.getValue().entrySet().size());
        int k = 0;
        for (Entry<Long, byte[]> value : qualifier.getValue().entrySet()) {
          System.out.println(tab + tab + tab + (++k) + ". timestamp: " + value.getKey());
          System.out
              .println(tab + tab + tab + (k) + ". value: " + Bytes.toString(value.getValue()));
          System.out.println("");
        }
      }
    }
    System.out.println("[ END rowKey: " + rowKey + "]");
    System.out.println("");
  }

  public static class MyHBaseDAO {

    static void insertRecord(Table table, EnrichmentKey key, String value) throws IOException {
      Put put = createPut(key, value);
      System.out.println("Insert record put json: " + put.toJSON());
      table.put(put);
    }

    private static Put createPut(EnrichmentKey rowKey, String value) throws IOException {
      return new EnrichmentConverter().toPut(CF, rowKey, new EnrichmentValue(JSONUtils.INSTANCE.load(value, JSONUtils.MAP_SUPPLIER)));
    }

    static void insertRecord(Table table, String rowKey, String value) throws Exception {
      insertRecord(table, rowKey, CF, CQ, value);
    }

    static void insertRecord(Table table, String rowKey, String cf, String value) throws Exception {
      insertRecord(table, rowKey, cf, CQ, value);
    }

    static void insertRecord(Table table, String rowKey, String cf, String cq, String value)
        throws Exception {
      Put put = createPut(rowKey, cf, cq, value);
      table.put(put);
    }

    private static Put createPut(String rowKey, String cf, String cq, String value) {
      Put put = new Put(Bytes.toBytes(rowKey));
      put.addColumn(Bytes.toBytes(cf), Bytes.toBytes(cq), Bytes.toBytes(value));
      return put;
    }

    static Result readRecord(Table table, String rowKey) throws Exception {
      Get get = new Get(Bytes.toBytes(rowKey));
      get.setMaxVersions();
      return table.get(get);
    }

    static Result readRecord(Table table, String rowKey, String cf) throws Exception {
      Get get = new Get(Bytes.toBytes(rowKey));
      get.addFamily(Bytes.toBytes(cf));
      return table.get(get);
    }

    static Result readRecord(Table table, String rowKey, String cf, String cq) throws Exception {
      Get get = new Get(Bytes.toBytes(rowKey));
      get.addColumn(Bytes.toBytes(cf), Bytes.toBytes(cq));
      get.setMaxVersions();
      return table.get(get);
    }

    static List<Result> readRecordsAll(Table table) throws Exception {
      Scan scan = new Scan();
      ResultScanner scanner = table.getScanner(scan);
      List<Result> rows = new ArrayList<>();
      for (Result r = scanner.next(); r != null; r = scanner.next()) {
        rows.add(r);
      }
      return rows;
    }

    static List<String> readRecords(Table table) throws Exception {
      Scan scan = new Scan();
      ResultScanner scanner = table.getScanner(scan);
      List<String> rows = new ArrayList<>();
      for (Result r = scanner.next(); r != null; r = scanner.next()) {
        rows.add(Bytes.toString(r.getRow()));
      }
      return rows;
    }

    public static void deleteRecord(HTable table, String rowKey) throws IOException {
      Delete delete = new Delete(Bytes.toBytes(rowKey));
      table.delete(delete);
    }

  }

}
