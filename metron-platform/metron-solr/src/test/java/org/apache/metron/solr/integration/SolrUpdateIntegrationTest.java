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
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.common.zookeeper.ZKConfigurationsCache;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.hbase.mock.MockHTable;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.HBaseDao;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MultiIndexDao;
import org.apache.metron.indexing.dao.UpdateIntegrationTest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.ReplaceRequest;
import org.apache.metron.indexing.util.IndexingCacheUtil;
import org.apache.metron.solr.dao.SolrDao;
import org.apache.metron.solr.integration.components.SolrComponent;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;

import static org.apache.metron.solr.SolrConstants.SOLR_ZOOKEEPER;
import static org.junit.Assert.assertEquals;

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
    accessConfig.setIndexSupplier(s -> s);

    CuratorFramework client = ConfigurationsUtils
        .getClient(solrComponent.getZookeeperUrl());
    client.start();
    ZKConfigurationsCache cache = new ZKConfigurationsCache(client);
    cache.start();
    accessConfig.setIndexSupplier(IndexingCacheUtil.getIndexLookupFunction(cache, "solr"));

    MultiIndexDao dao = new MultiIndexDao(hbaseDao, new SolrDao());
    dao.init(accessConfig);
    setDao(dao);
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
    getDao().update(errorDoc, Optional.of("error"));
  }

  @Test
  @Override
  public void test() throws Exception {
    List<Map<String, Object>> inputData = new ArrayList<>();
    for(int i = 0; i < 10;++i) {
      final String name = "message" + i;
      inputData.add(
              new HashMap<String, Object>() {{
                put("source.type", SENSOR_NAME);
                put("name" , name);
                put("timestamp", System.currentTimeMillis());
                put(Constants.GUID, name);
              }}
      );
    }
    addTestData(getIndexName(), SENSOR_NAME, inputData);
    List<Map<String,Object>> docs = null;
    for(int t = 0;t < MAX_RETRIES;++t, Thread.sleep(SLEEP_MS)) {
      docs = getIndexedTestData(getIndexName(), SENSOR_NAME);
      if(docs.size() >= 10) {
        break;
      }
    }
    Assert.assertEquals(10, docs.size());
    //modify the first message and add a new field
    {
      Map<String, Object> message0 = new HashMap<String, Object>(inputData.get(0)) {{
        put("new-field", "metron");
      }};
      String guid = "" + message0.get(Constants.GUID);
      Document update = getDao().replace(new ReplaceRequest(){{
        setReplacement(message0);
        setGuid(guid);
        setSensorType(SENSOR_NAME);
        setIndex(getIndexName());
      }}, Optional.empty());

      Assert.assertEquals(message0, update.getDocument());
      Assert.assertEquals(1, getMockHTable().size());
      findUpdatedDoc(message0, guid, SENSOR_NAME);
      {
        //ensure hbase is up to date
        Get g = new Get(HBaseDao.Key.toBytes(new HBaseDao.Key(guid, SENSOR_NAME)));
        Result r = getMockHTable().get(g);
        NavigableMap<byte[], byte[]> columns = r.getFamilyMap(CF.getBytes());
        Assert.assertEquals(1, columns.size());
        Assert.assertEquals(message0
                , JSONUtils.INSTANCE.load(new String(columns.lastEntry().getValue())
                        , JSONUtils.MAP_SUPPLIER)
        );
      }
      {
        //ensure ES is up-to-date
        long cnt = 0;
        for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t, Thread.sleep(SLEEP_MS)) {
          docs = getIndexedTestData(getIndexName(), SENSOR_NAME);
          cnt = docs
                  .stream()
                  .filter(d -> message0.get("new-field").equals(d.get("new-field")))
                  .count();
        }
        Assert.assertNotEquals("Data store is not updated!", cnt, 0);
      }
    }
    //modify the same message and modify the new field
    {
      Map<String, Object> message0 = new HashMap<String, Object>(inputData.get(0)) {{
        put("new-field", "metron2");
      }};
      String guid = "" + message0.get(Constants.GUID);
      Document update = getDao().replace(new ReplaceRequest(){{
        setReplacement(message0);
        setGuid(guid);
        setSensorType(SENSOR_NAME);
        setIndex(getIndexName());
      }}, Optional.empty());
      Assert.assertEquals(message0, update.getDocument());
      Assert.assertEquals(1, getMockHTable().size());
      Document doc = getDao().getLatest(guid, SENSOR_NAME);
      Assert.assertEquals(message0, doc.getDocument());
      findUpdatedDoc(message0, guid, SENSOR_NAME);
      {
        //ensure hbase is up to date
        Get g = new Get(HBaseDao.Key.toBytes(new HBaseDao.Key(guid, SENSOR_NAME)));
        Result r = getMockHTable().get(g);
        NavigableMap<byte[], byte[]> columns = r.getFamilyMap(CF.getBytes());
        Assert.assertEquals(2, columns.size());
        Assert.assertEquals(message0, JSONUtils.INSTANCE.load(new String(columns.lastEntry().getValue())
                , JSONUtils.MAP_SUPPLIER)
        );
        Assert.assertNotEquals(message0, JSONUtils.INSTANCE.load(new String(columns.firstEntry().getValue())
                , JSONUtils.MAP_SUPPLIER)
        );
      }
      {
        //ensure ES is up-to-date
        long cnt = 0;
        for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t,Thread.sleep(SLEEP_MS)) {
          docs = getIndexedTestData(getIndexName(), SENSOR_NAME);
          cnt = docs
                  .stream()
                  .filter(d -> message0.get("new-field").equals(d.get("new-field")))
                  .count();
        }

        Assert.assertNotEquals("Data store is not updated!", cnt, 0);
      }
    }
  }
}
