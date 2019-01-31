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
import java.util.Map;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
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
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Level;
import org.apache.metron.dataloads.hbase.mr.HBaseUtil;
import org.apache.metron.test.utils.UnitTestHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class EnrichmentCoprocessorIntegrationTest {

  private static final String ENRICHMENT_TABLE = "enrichment";
  private static final String CF = "cf";
  private static final String CQ = "cq-1";

  private static Level originalRootLoggerLevel;
  private static HBaseTestingUtility testUtil;
  private static HTable testTable;
  private static Configuration config;
  private static TestingServer testZkServer;
  private static String zookeeperUrl;
  private static CuratorFramework client;


  @BeforeClass
  public static void setupAll() throws Exception {
    originalRootLoggerLevel = UnitTestHelper.getLog4jLevel();
    UnitTestHelper.setLog4jLevel(Level.ERROR);
    UnitTestHelper.setJavaLoggingLevel(java.util.logging.Level.SEVERE);
//    Configuration extraConfig = new Configuration();
//    // https://hbase.apache.org/1.1/book.html#cp_loading
//    extraConfig.set("hbase.coprocessor.region.classes", "org.apache.metron.hbase.coprocessor.EnrichmentsCoprocessor");
    Map.Entry<HBaseTestingUtility, Configuration> kv = HBaseUtil.INSTANCE.create(true);
    testUtil = kv.getKey();
    config = kv.getValue();
    testTable = testUtil.createTable(Bytes.toBytes(ENRICHMENT_TABLE), Bytes.toBytes(CF));
    zookeeperUrl = getZookeeperUrl(config.get("hbase.zookeeper.quorum"),
        testUtil.getZkCluster().getClientPort());

    for (Result r : testTable.getScanner(Bytes.toBytes(CF))) {
      Delete d = new Delete(r.getRow());
      testTable.delete(d);
    }
    Admin hbaseAdmin = testUtil.getConnection().getAdmin();
    TableName tableName = testTable.getName();
    hbaseAdmin.disableTable(tableName);
    HTableDescriptor htd = new HTableDescriptor(tableName);
    htd.addFamily(new HColumnDescriptor(CF));
    htd.addCoprocessor(EnrichmentsCoprocessor.class.getCanonicalName());
    hbaseAdmin.modifyTable(tableName, htd);
    hbaseAdmin.enableTable(tableName);
  }

  private static String getZookeeperUrl(String host, int port) {
    return host + ":" + port;
  }

  @AfterClass
  public static void teardown() throws Exception {
    HBaseUtil.INSTANCE.teardown(testUtil);
    UnitTestHelper.setLog4jLevel(originalRootLoggerLevel);
  }

  @Test
  public void load_enrichments() throws Exception {
    final String rowKey1 = "rowkey-1";
    final String value1 = "value-1";
    MyHBaseDAO.insertRecord(testTable, rowKey1, value1);
    Result result = MyHBaseDAO.readRecord(testTable, rowKey1);
    assertThat(Bytes.toString(result.getRow()), equalTo(rowKey1));
    assertThat(Bytes.toString(result.value()), equalTo(value1));
    MyHBaseDAO.deleteRecord(testTable, rowKey1);
    result = MyHBaseDAO.readRecord(testTable, rowKey1);
    assertThat(result.isEmpty(), equalTo(true));
  }

  public static class MyHBaseDAO {

    static void insertRecord(Table table, String rowKey, String value) throws Exception {
      Put put = createPut(rowKey, value);
      table.put(put);
    }

    private static Put createPut(String rowKey, String value) {
      Put put = new Put(Bytes.toBytes(rowKey));
      put.addColumn(Bytes.toBytes(CF), Bytes.toBytes(CQ), Bytes.toBytes(value));
      return put;
    }

    static Result readRecord(Table table, String rowKey) throws Exception {
      Get get = new Get(Bytes.toBytes(rowKey));
      return table.get(get);
    }

    public static void deleteRecord(HTable table, String rowKey) throws IOException {
      Delete delete = new Delete(Bytes.toBytes(rowKey));
      table.delete(delete);
    }
  }

}
