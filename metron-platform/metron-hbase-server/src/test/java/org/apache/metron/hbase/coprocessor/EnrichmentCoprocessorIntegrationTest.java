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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.adrianwalker.multilinestring.Multiline;
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
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.dataloads.hbase.mr.HBaseUtil;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.hbase.helper.HelperDao;
import org.apache.metron.integration.BaseIntegrationTest;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.UnableToStartException;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.test.utils.UnitTestHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class EnrichmentCoprocessorIntegrationTest extends BaseIntegrationTest {

  private static final String ENRICHMENT_TABLE = "enrichment";
  private static final String ENRICHMENT_LIST_TABLE = "enrichment_list";
  private static final String COLUMN_FAMILY = "c";

  private static Level originalLog4jRootLoggerLevel;
  private static java.util.logging.Level originalJavaLoggerLevel;
  private static ZKServerComponent zookeeperComponent;
  private static ComponentRunner componentRunner;
  private static HBaseTestingUtility testUtil;
  private static HTable enrichmentTable;
  private static HTable enrichmentListTable;
  private static Configuration hBaseConfig;

  /*
   * Test Setup
   */

  /**
   * {
   *    "enrichment.list.hbase.provider.impl" : "org.apache.metron.hbase.HTableProvider",
   *    "enrichment.list.hbase.table" : "%TABLE_NAME%",
   *    "enrichment.list.hbase.cf" : "%COLUMN_FAMILY%"
   * }
   */
  @Multiline
  private static String globalConfig;

  @BeforeClass
  public static void setupAll() throws Exception {
    silenceLogging();
    // don't need the properties for anything else now, but could extract var if desired.
    startZookeeper(new Properties());
    globalConfig = globalConfig.replace("%TABLE_NAME%", ENRICHMENT_LIST_TABLE)
        .replace("%COLUMN_FAMILY%", COLUMN_FAMILY);
    uploadGlobalConfigToZK(globalConfig);
    configureAndStartHBase();
    addCoprocessor(enrichmentTable.getName());
  }

  /**
   * log4j and java logging set to ERROR, SEVERE respectively.
   */
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

  /**
   * Starts zookeeper.
   * @param properties the zk setup will modify properties arg with the setup detail.
   * @throws UnableToStartException zk fails to start.
   */
  private static void startZookeeper(Properties properties) throws UnableToStartException {
    zookeeperComponent = getZKServerComponent(properties);
    componentRunner = new ComponentRunner.Builder()
        .withComponent("zk", zookeeperComponent)
        .withMillisecondsBetweenAttempts(15000)
        .withNumRetries(10)
        .build();
    componentRunner.start();
  }

  private static void uploadGlobalConfigToZK(String config) throws Exception {
    ConfigurationsUtils.writeGlobalConfigToZookeeper(config.getBytes(StandardCharsets.UTF_8),
        zookeeperComponent.getConnectionString());
  }

  /**
   * Start HBase.
   * Create enrichment and enrichment list tables.
   */
  private static void configureAndStartHBase() throws Exception {
    Configuration extraConfig = new Configuration();
    extraConfig.set(EnrichmentCoprocessor.ZOOKEEPER_URL, zookeeperComponent.getConnectionString());
    Map.Entry<HBaseTestingUtility, Configuration> kv = HBaseUtil.INSTANCE.create(true, extraConfig);
    testUtil = kv.getKey();
    hBaseConfig = kv.getValue();
    enrichmentTable = testUtil.createTable(Bytes.toBytes(ENRICHMENT_TABLE), Bytes.toBytes(
        COLUMN_FAMILY));
    enrichmentListTable = testUtil
        .createTable(Bytes.toBytes(ENRICHMENT_LIST_TABLE), Bytes.toBytes(COLUMN_FAMILY));

    for (Result r : enrichmentTable.getScanner(Bytes.toBytes(COLUMN_FAMILY))) {
      Delete d = new Delete(r.getRow());
      enrichmentTable.delete(d);
    }
    for (Result r : enrichmentListTable.getScanner(Bytes.toBytes(COLUMN_FAMILY))) {
      Delete d = new Delete(r.getRow());
      enrichmentListTable.delete(d);
    }
  }

  private static void addCoprocessor(TableName tableName) throws IOException {
    // https://hbase.apache.org/1.1/book.html#cp_loading
    Admin hbaseAdmin = testUtil.getConnection().getAdmin();
    hbaseAdmin.disableTable(tableName);
    HTableDescriptor htd = new HTableDescriptor(tableName);
    htd.addFamily(new HColumnDescriptor(COLUMN_FAMILY));
    htd.addCoprocessor(EnrichmentCoprocessor.class.getCanonicalName());
    hbaseAdmin.modifyTable(tableName, htd);
    hbaseAdmin.enableTable(tableName);
  }

  @AfterClass
  public static void teardown() throws Exception {
    HBaseUtil.INSTANCE.teardown(testUtil);
    componentRunner.stop();
    resetLogging();
  }

  private static void resetLogging() {
    UnitTestHelper.setLog4jLevel(originalLog4jRootLoggerLevel);
    UnitTestHelper.setJavaLoggingLevel(originalJavaLoggerLevel);
  }

  /*
   * Tests
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
      HelperDao.insertRecord(enrichmentTable, new EnrichmentKey(type, indicator), COLUMN_FAMILY,
          "{ \"apache\" : \"metron\" }");
    }
    List<String> enrichmentsList = HelperDao.readRecords(enrichmentListTable);
    assertThat(new HashSet<String>(enrichmentsList), equalTo(expectedEnrichmentTypes));
  }

}
