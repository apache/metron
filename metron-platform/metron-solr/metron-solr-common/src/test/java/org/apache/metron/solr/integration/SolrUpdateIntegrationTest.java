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

import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.zookeeper.ZKConfigurationsCache;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.HBaseDao;
import org.apache.metron.indexing.dao.UpdateIntegrationTest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.util.IndexingCacheUtil;
import org.apache.metron.solr.client.SolrClientFactory;
import org.apache.metron.solr.dao.SolrDao;
import org.apache.metron.solr.integration.components.SolrComponent;
import org.apache.solr.common.SolrException;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.metron.solr.SolrConstants.SOLR_ZOOKEEPER;
import static org.junit.jupiter.api.Assertions.*;

public class SolrUpdateIntegrationTest extends UpdateIntegrationTest {
  private static SolrComponent solrComponent;

  private static final String TABLE_NAME = "modifications";
  private static final String CF = "p";

  @BeforeAll
  public static void setupBeforeClass() throws Exception {
    solrComponent = new SolrComponent.Builder().build();
    solrComponent.start();
  }

  @BeforeEach
  public void setup() throws Exception {
    solrComponent.addCollection(SENSOR_NAME, "./src/test/resources/config/test/conf");
    solrComponent.addCollection("error", "./src/main/config/schema/error");

    Map<String, Object> globalConfig = createGlobalConfig();
    globalConfig.put(HBaseDao.HBASE_TABLE, TABLE_NAME);
    globalConfig.put(HBaseDao.HBASE_CF, CF);

    CuratorFramework client = ConfigurationsUtils.getClient(solrComponent.getZookeeperUrl());
    client.start();
    ZKConfigurationsCache cache = new ZKConfigurationsCache(client);
    cache.start();

    AccessConfig accessConfig = new AccessConfig();
    accessConfig.setGlobalConfigSupplier(() -> globalConfig);
    accessConfig.setIndexSupplier(s -> s);
    accessConfig.setIndexSupplier(IndexingCacheUtil.getIndexLookupFunction(cache, "solr"));

    SolrDao dao = new SolrDao();
    dao.init(accessConfig);
    setDao(dao);
  }

  @AfterEach
  public void reset() {
    solrComponent.reset();
  }

  @AfterAll
  public static void teardown() {
    SolrClientFactory.close();
    solrComponent.stop();
  }

  @Override
  protected String getIndexName() {
    return SENSOR_NAME;
  }

  private static Map<String, Object> createGlobalConfig() {
    return new HashMap<String, Object>() {{
      put(SOLR_ZOOKEEPER, solrComponent.getZookeeperUrl());
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
    getDao().update(document, Optional.of(SENSOR_NAME));

    Document indexedDocument = getDao().getLatest("bro_1", SENSOR_NAME);

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
    getDao().update(errorDoc, Optional.of("error"));

    // Ensure that the huge string is returned when not a string field
    Document latest = getDao().getLatest("error_guid", "error");
    String actual = (String) latest.getDocument().get("raw_message");
    assertEquals(actual, hugeString);
    String actualTwo = (String) latest.getDocument().get("raw_message_1");
    assertEquals(actualTwo, hugeStringTwo);

    // Validate that error occurs for string fields.
    documentMap.put("error_hash", hugeString);
    Document errorDoc2= new Document(documentMap, "error", "error", 0L);

    SolrException e = assertThrows(SolrException.class, () -> getDao().update(errorDoc2, Optional.of("error")));
    assertTrue(e.getMessage().contains("Document contains at least one immense term in field=\"error_hash\""));
  }
}
