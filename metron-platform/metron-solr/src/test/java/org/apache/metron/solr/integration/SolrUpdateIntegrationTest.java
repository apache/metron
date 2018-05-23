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
package org.apache.metron.solr.integration;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.hbase.mock.MockHTable;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.HBaseDao;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MultiIndexDao;
import org.apache.metron.indexing.dao.UpdateIntegrationTest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.solr.dao.SolrDao;
import org.apache.metron.solr.integration.components.SolrComponent;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SolrUpdateIntegrationTest extends UpdateIntegrationTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  private static SolrComponent solrComponent;

  private static final String TABLE_NAME = "modifications";
  private static final String CF = "p";
  private static MockHTable table;
  private static IndexDao hbaseDao;

  @BeforeClass
  public static void setupBeforeClass() throws Exception {
    solrComponent = new SolrComponent.Builder().build();
    solrComponent.start();
  }

  @Before
  public void setup() throws Exception {
    solrComponent.addCollection(SENSOR_NAME, "../metron-solr/src/test/resources/config/test/conf");
    solrComponent.addCollection("error", "../metron-solr/src/main/config/schema/error");

    Configuration config = HBaseConfiguration.create();
    MockHBaseTableProvider tableProvider = new MockHBaseTableProvider();
    MockHBaseTableProvider.addToCache(TABLE_NAME, CF);
    table = (MockHTable) tableProvider.getTable(config, TABLE_NAME);

    hbaseDao = new HBaseDao();
    AccessConfig accessConfig = new AccessConfig();
    accessConfig.setTableProvider(tableProvider);
    Map<String, Object> globalConfig = createGlobalConfig();
    globalConfig.put(HBaseDao.HBASE_TABLE, TABLE_NAME);
    globalConfig.put(HBaseDao.HBASE_CF, CF);
    accessConfig.setGlobalConfigSupplier(() -> globalConfig);

    dao = new MultiIndexDao(hbaseDao, new SolrDao());
    dao.init(accessConfig);
  }

  @After
  public void reset() {
    solrComponent.reset();
    table.clear();
  }

  @AfterClass
  public static void teardown() {
    solrComponent.stop();
  }

  @Override
  protected String getIndexName() {
    return SENSOR_NAME;
  }

  @Override
  protected MockHTable getMockHTable() {
    return table;
  }

  private static Map<String, Object> createGlobalConfig() {
    return new HashMap<String, Object>() {{
      put("solr.zookeeper", solrComponent.getZookeeperUrl());
    }};
  }

  @Override
  protected void addTestData(String indexName, String sensorType,
      List<Map<String, Object>> docs) throws Exception {
    solrComponent.addDocs(indexName, docs);
  }

  @Override
  protected List<Map<String, Object>> getIndexedTestData(String indexName, String sensorType) {
    return solrComponent.getAllIndexedDocs(indexName);
  }

  @Test
  public void suppress_expanded_fields() throws Exception {
    Map<String, Object> fields = new HashMap<>();
    fields.put("guid", "bro_1");
    fields.put("source.type", SENSOR_NAME);
    fields.put("ip_src_port", 8010);
    fields.put("long_field", 10000);
    fields.put("latitude", 48.5839);
    fields.put("score", 10.0);
    fields.put("is_alert", true);
    fields.put("field.location_point", "48.5839,7.7455");

    Document document = new Document(fields, "bro_1", SENSOR_NAME, 0L);
    dao.update(document, Optional.of(SENSOR_NAME));

    Document indexedDocument = dao.getLatest("bro_1", SENSOR_NAME);

    // assert no extra expanded fields are included
    assertEquals(8, indexedDocument.getDocument().size());
  }

  @Test
  public void testHugeErrorFields() throws Exception {
    String hugeString = StringUtils.repeat("test ", 1_000_000);
    String hugeStringTwo = hugeString + "-2";

    Map<String, Object> documentMap = new HashMap<>();
    documentMap.put("guid", "error_guid");
    // Needs to be over 32kb
    documentMap.put("raw_message", hugeString);
    documentMap.put("raw_message_1", hugeStringTwo);
    Document errorDoc = new Document(documentMap, "error", "error", 0L);
    dao.update(errorDoc, Optional.of("error"));

    // Ensure that the huge string is returned when not a string field
    Document latest = dao.getLatest("error_guid", "error");
    @SuppressWarnings("unchecked")
    String actual = (String) latest.getDocument().get("raw_message");
    assertEquals(actual, hugeString);
    String actualTwo = (String) latest.getDocument().get("raw_message_1");
    assertEquals(actualTwo, hugeStringTwo);

    // Validate that error occurs for string fields.
    documentMap.put("error_hash", hugeString);
    errorDoc = new Document(documentMap, "error", "error", 0L);

    exception.expect(IOException.class);
    exception.expectMessage("Document contains at least one immense term in field=\"error_hash\"");
    dao.update(errorDoc, Optional.of("error"));
  }
}
