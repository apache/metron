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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Level;
import org.apache.metron.dataloads.hbase.mr.HBaseUtil;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.hbase.coprocessor.config.CoprocessorOptions;
import org.apache.metron.hbase.helper.HelperDao;
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
    Configuration extraConfig = new Configuration();
    extraConfig.set(CoprocessorOptions.TABLE_NAME.getKey(), "enrichment_list");
    extraConfig.set(CoprocessorOptions.COLUMN_FAMILY.getKey(), "c");
    extraConfig.set(CoprocessorOptions.COLUMN_QUALIFIER.getKey(), "q");
    Map.Entry<HBaseTestingUtility, Configuration> kv = HBaseUtil.INSTANCE.create(true, extraConfig);
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
    // uncomment below for finer-grained logging
    /*
    UnitTestHelper.setLog4jLevel(EnrichmentCoprocessor.class, Level.DEBUG);
    UnitTestHelper.setLog4jLevel(HBaseCacheWriter.class, Level.DEBUG);
    */
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
      HelperDao.insertRecord(enrichmentTable, new EnrichmentKey(type, indicator), CF,
          "{ \"apache\" : \"metron\" }");
    }
    List<String> enrichmentsList = HelperDao.readRecords(enrichmentListTable);
    assertThat(new HashSet<String>(enrichmentsList), equalTo(expectedEnrichmentTypes));
  }

  // TODO - test exception doesn't disable the coprocessor

}
