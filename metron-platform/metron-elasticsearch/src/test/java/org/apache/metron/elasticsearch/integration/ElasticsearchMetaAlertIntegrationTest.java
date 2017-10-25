/*
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

package org.apache.metron.elasticsearch.integration;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.elasticsearch.dao.ElasticsearchDao;
import org.apache.metron.elasticsearch.dao.ElasticsearchMetaAlertDao;
import org.apache.metron.elasticsearch.dao.MetaAlertStatus;
import org.apache.metron.elasticsearch.integration.components.ElasticSearchComponent;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MetaAlertDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.search.Group;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.GroupResult;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.metron.indexing.dao.search.SortField;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.apache.metron.indexing.dao.update.ReplaceRequest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ElasticsearchMetaAlertIntegrationTest {

  private static final int MAX_RETRIES = 10;
  private static final int SLEEP_MS = 500;
  private static final String SENSOR_NAME = "test";
  private static final String INDEX_DIR = "target/elasticsearch_meta";
  private static final String DATE_FORMAT = "yyyy.MM.dd.HH";
  private static final String INDEX =
      SENSOR_NAME + "_index_" + new SimpleDateFormat(DATE_FORMAT).format(new Date());
  private static final String NEW_FIELD = "new-field";

  private static IndexDao esDao;
  private static MetaAlertDao metaDao;
  private static ElasticSearchComponent es;

  /**
   {
   "guid": "update_metaalert_alert_0",
   "source:type": "test",
   "field": "value 0"
   }
   */
  @Multiline
  public static String updateMetaAlertAlert0;

  /**
   {
   "guid": "update_metaalert_alert_1",
   "source:type": "test",
   "field":"value 1"
   }
   */
  @Multiline
  public static String updateMetaAlertAlert1;

  /**
   {
   "guid": "update_metaalert_alert_0",
   "patch": [
   {
   "op": "add",
   "path": "/field",
   "value": "patched value 0"
   }
   ],
   "sensorType": "test"
   }
   */
  @Multiline
  public static String updateMetaAlertPatchRequest;

  /**
   {
   "guid": "update_metaalert_alert_0",
   "replacement": {
   "guid": "update_metaalert_alert_0",
   "source:type": "test",
   "field": "replaced value 0"
   },
   "sensorType": "test"
   }
   */
  @Multiline
  public static String updateMetaAlertReplaceRequest;

  /**
   {
   "guid": "active_metaalert",
   "source:type": "metaalert",
   "alert": [],
   "status": "active",
   "timestamp": 0
   }
   */
  @Multiline
  public static String activeMetaAlert;

  /**
   {
   "guid": "inactive_metaalert",
   "source:type": "metaalert",
   "alert": [],
   "status": "inactive"
   }
   */
  @Multiline
  public static String inactiveMetaAlert;

  /**
   {
   "guid": "search_by_nested_alert_active_0",
   "source:type": "test",
   "ip_src_addr": "192.168.1.1",
   "ip_src_port": 8010,
   "metaalerts": ["active_metaalert"],
   "timestamp": 0
   }
   */
  @Multiline
  public static String searchByNestedAlertActive0;

  /**
   {
   "guid": "search_by_nested_alert_active_1",
   "source:type": "test",
   "ip_src_addr": "192.168.1.2",
   "ip_src_port": 8009,
   "metaalerts": ["active_metaalert"],
   "timestamp": 0
   }
   */
  @Multiline
  public static String searchByNestedAlertActive1;

  /**
   {
   "guid": "search_by_nested_alert_inactive_0",
   "source:type": "test",
   "ip_src_addr": "192.168.1.3",
   "ip_src_port": 8008
   }
   */
  @Multiline
  public static String searchByNestedAlertInactive0;

  /**
   {
   "guid": "search_by_nested_alert_inactive_1",
   "source:type": "test",
   "ip_src_addr": "192.168.1.4",
   "ip_src_port": 8007
   }
   */
  @Multiline
  public static String searchByNestedAlertInactive1;

  /**
   {
     "properties": {
       "alert": {
         "type": "nested"
       }
     }
   }
   */
  @Multiline
  public static String nestedAlertMapping;

  /**
   {
   "guid": "group_by_child_alert",
   "source:type": "test",
   "ip_src_addr": "192.168.1.1",
   "ip_src_port": 8010,
   "score_field": 1,
   "metaalerts": ["active_metaalert"]
   }
   */
  @Multiline
  public static String groupByChildAlert;

  /**
   {
   "guid": "group_by_standalone_alert",
   "source:type": "test",
   "ip_src_addr": "192.168.1.1",
   "ip_src_port": 8010,
   "score_field": 10
   }
   */
  @Multiline
  public static String groupByStandaloneAlert;

  @BeforeClass
  public static void setupBefore() throws Exception {
    // setup the client
    es = new ElasticSearchComponent.Builder()
        .withHttpPort(9211)
        .withIndexDir(new File(INDEX_DIR))
        .build();
    es.start();

    AccessConfig accessConfig = new AccessConfig();
    Map<String, Object> globalConfig = new HashMap<String, Object>() {
      {
        put("es.clustername", "metron");
        put("es.port", "9300");
        put("es.ip", "localhost");
        put("es.date.format", DATE_FORMAT);
      }
    };
    accessConfig.setMaxSearchResults(1000);
    accessConfig.setGlobalConfigSupplier(() -> globalConfig);
    accessConfig.setMaxSearchGroups(100);

    esDao = new ElasticsearchDao();
    esDao.init(accessConfig);
    metaDao = new ElasticsearchMetaAlertDao(esDao);
  }

  @Before
  public void setup() throws IOException {
    es.createIndexWithMapping(MetaAlertDao.METAALERTS_INDEX, MetaAlertDao.METAALERT_DOC,
        buildMetaMappingSource());
  }

  @AfterClass
  public static void teardown() {
    if (es != null) {
      es.stop();
    }
  }

  @After
  public void reset() {
    es.reset();
  }

  protected static String buildMetaMappingSource() throws IOException {
    return jsonBuilder().prettyPrint()
        .startObject()
        .startObject(MetaAlertDao.METAALERT_DOC)
        .startObject("properties")
        .startObject("guid")
        .field("type", "string")
        .field("index", "not_analyzed")
        .endObject()
        .startObject("score")
        .field("type", "integer")
        .field("index", "not_analyzed")
        .endObject()
        .startObject("alert")
        .field("type", "nested")
        .endObject()
        .endObject()
        .endObject()
        .endObject()
        .string();
  }


  @SuppressWarnings("unchecked")
  @Test
  public void test() throws Exception {
    List<Map<String, Object>> inputData = new ArrayList<>();
    for (int i = 0; i < 2; ++i) {
      final String name = "message" + i;
      int finalI = i;
      inputData.add(
          new HashMap<String, Object>() {
            {
              put("source:type", SENSOR_NAME);
              put("name", name);
              put(MetaAlertDao.THREAT_FIELD_DEFAULT, finalI);
              put("timestamp", System.currentTimeMillis());
              put(Constants.GUID, name);
            }
          }
      );
    }

    elasticsearchAdd(inputData, INDEX, SENSOR_NAME);

    List<Map<String, Object>> metaInputData = new ArrayList<>();
    final String name = "meta_message";
    Map<String, Object>[] alertArray = new Map[1];
    alertArray[0] = inputData.get(0);
    metaInputData.add(
        new HashMap<String, Object>() {
          {
            put("source:type", SENSOR_NAME);
            put("alert", alertArray);
            put(Constants.GUID, name + "_active");
            put(MetaAlertDao.STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString());
          }
        }
    );
    // Add an inactive message
    metaInputData.add(
        new HashMap<String, Object>() {
          {
            put("source:type", SENSOR_NAME);
            put("alert", alertArray);
            put(Constants.GUID, name + "_inactive");
            put(MetaAlertDao.STATUS_FIELD, MetaAlertStatus.INACTIVE.getStatusString());
          }
        }
    );

    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    elasticsearchAdd(metaInputData, MetaAlertDao.METAALERTS_INDEX, MetaAlertDao.METAALERT_TYPE);

    List<Map<String, Object>> docs = null;
    for (int t = 0; t < MAX_RETRIES; ++t, Thread.sleep(SLEEP_MS)) {
      docs = es.getAllIndexedDocs(INDEX, SENSOR_NAME + "_doc");
      if (docs.size() >= 10) {
        break;
      }
    }
    Assert.assertEquals(2, docs.size());
    {
      //modify the first message and add a new field
      Map<String, Object> message0 = new HashMap<String, Object>(inputData.get(0)) {
        {
          put(NEW_FIELD, "metron");
          put(MetaAlertDao.THREAT_FIELD_DEFAULT, "10");
        }
      };
      String guid = "" + message0.get(Constants.GUID);
      metaDao.replace(new ReplaceRequest() {
        {
          setReplacement(message0);
          setGuid(guid);
          setSensorType(SENSOR_NAME);
        }
      }, Optional.empty());

      {
        //ensure alerts in ES are up-to-date
        boolean found = findUpdatedDoc(message0, guid, SENSOR_NAME);
        Assert.assertTrue("Unable to find updated document", found);
        long cnt = 0;
        for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t, Thread.sleep(SLEEP_MS)) {
          docs = es.getAllIndexedDocs(INDEX, SENSOR_NAME + "_doc");
          cnt = docs
              .stream()
              .filter(d -> {
                Object newfield = d.get(NEW_FIELD);
                return newfield != null && newfield.equals(message0.get(NEW_FIELD));
              }).count();
        }
        if (cnt == 0) {
          Assert.fail("Elasticsearch is not updated!");
        }
      }

      {
        //ensure meta alerts in ES are up-to-date
        long cnt = 0;
        for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t, Thread.sleep(SLEEP_MS)) {
          docs = es.getAllIndexedDocs(MetaAlertDao.METAALERTS_INDEX, MetaAlertDao.METAALERT_DOC);
          cnt = docs
              .stream()
              .filter(d -> {
                List<Map<String, Object>> alerts = (List<Map<String, Object>>) d
                    .get(MetaAlertDao.ALERT_FIELD);

                for (Map<String, Object> alert : alerts) {
                  Object newField = alert.get(NEW_FIELD);
                  if (newField != null && newField.equals(message0.get(NEW_FIELD))) {
                    return true;
                  }
                }

                return false;
              }).count();
        }
        if (cnt == 0) {
          Assert.fail("Elasticsearch metaalerts not updated!");
        }
      }
    }
    //modify the same message and modify the new field
    {
      Map<String, Object> message0 = new HashMap<String, Object>(inputData.get(0)) {
        {
          put(NEW_FIELD, "metron2");
        }
      };
      String guid = "" + message0.get(Constants.GUID);
      metaDao.replace(new ReplaceRequest() {
        {
          setReplacement(message0);
          setGuid(guid);
          setSensorType(SENSOR_NAME);
        }
      }, Optional.empty());

      boolean found = findUpdatedDoc(message0, guid, SENSOR_NAME);
      Assert.assertTrue("Unable to find updated document", found);
      {
        //ensure ES is up-to-date
        long cnt = 0;
        for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t, Thread.sleep(SLEEP_MS)) {
          docs = es.getAllIndexedDocs(INDEX, SENSOR_NAME + "_doc");
          cnt = docs
              .stream()
              .filter(d -> message0.get(NEW_FIELD).equals(d.get(NEW_FIELD)))
              .count();
        }
        Assert.assertNotEquals("Elasticsearch is not updated!", cnt, 0);
        if (cnt == 0) {
          Assert.fail("Elasticsearch is not updated!");
        }
      }
      {
        //ensure meta alerts in ES are up-to-date
        long cnt = 0;
        for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t, Thread.sleep(SLEEP_MS)) {
          docs = es.getAllIndexedDocs(MetaAlertDao.METAALERTS_INDEX, MetaAlertDao.METAALERT_DOC);
          cnt = docs
              .stream()
              .filter(d -> {
                List<Map<String, Object>> alerts = (List<Map<String, Object>>) d
                    .get(MetaAlertDao.ALERT_FIELD);

                for (Map<String, Object> alert : alerts) {
                  Object newField = alert.get(NEW_FIELD);
                  if (newField != null && newField.equals(message0.get(NEW_FIELD))) {
                    return true;
                  }
                }

                return false;
              }).count();
        }
        if (cnt == 0) {
          Assert.fail("Elasticsearch metaalerts not updated!");
        }
      }
    }
  }


  @Test
  public void shouldSearchByStatus() throws Exception {
    List<Map<String, Object>> metaInputData = new ArrayList<>();
    Map<String, Object> activeMetaAlertJSON = JSONUtils.INSTANCE
        .load(activeMetaAlert, new TypeReference<Map<String, Object>>() {
        });
    metaInputData.add(activeMetaAlertJSON);
    Map<String, Object> inactiveMetaAlertJSON = JSONUtils.INSTANCE
        .load(inactiveMetaAlert, new TypeReference<Map<String, Object>>() {
        });
    metaInputData.add(inactiveMetaAlertJSON);

    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    elasticsearchAdd(metaInputData, MetaAlertDao.METAALERTS_INDEX, MetaAlertDao.METAALERT_TYPE);
    // Wait for updates to persist
    findUpdatedDoc(inactiveMetaAlertJSON, "inactive_metaalert", MetaAlertDao.METAALERT_TYPE);

    SearchResponse searchResponse = metaDao.search(new SearchRequest() {
      {
        setQuery("*");
        setIndices(Collections.singletonList(MetaAlertDao.METAALERT_TYPE));
        setFrom(0);
        setSize(5);
        setSort(Collections.singletonList(new SortField() {{
          setField(Constants.GUID);
        }}));
      }
    });
    Assert.assertEquals(1, searchResponse.getTotal());
    Assert.assertEquals(MetaAlertStatus.ACTIVE.getStatusString(),
        searchResponse.getResults().get(0).getSource().get(MetaAlertDao.STATUS_FIELD));
  }


  @Test
  public void shouldSearchByNestedAlert() throws Exception {
    // Create alerts
    List<Map<String, Object>> alerts = new ArrayList<>();
    Map<String, Object> searchByNestedAlertActive0JSON = JSONUtils.INSTANCE
        .load(searchByNestedAlertActive0, new TypeReference<Map<String, Object>>() {
        });
    alerts.add(searchByNestedAlertActive0JSON);
    Map<String, Object> searchByNestedAlertActive1JSON = JSONUtils.INSTANCE
        .load(searchByNestedAlertActive1, new TypeReference<Map<String, Object>>() {
        });
    alerts.add(searchByNestedAlertActive1JSON);
    Map<String, Object> searchByNestedAlertInactive0JSON = JSONUtils.INSTANCE
        .load(searchByNestedAlertInactive0, new TypeReference<Map<String, Object>>() {
        });
    alerts.add(searchByNestedAlertInactive0JSON);
    Map<String, Object> searchByNestedAlertInactive1JSON = JSONUtils.INSTANCE
        .load(searchByNestedAlertInactive1, new TypeReference<Map<String, Object>>() {
        });
    alerts.add(searchByNestedAlertInactive1JSON);
    elasticsearchAdd(alerts, INDEX, SENSOR_NAME);
    // Wait for updates to persist
    findUpdatedDoc(searchByNestedAlertActive0JSON, "search_by_nested_alert_active_0",
        SENSOR_NAME);
    findUpdatedDoc(searchByNestedAlertActive1JSON, "search_by_nested_alert_active_1",
        SENSOR_NAME);
    findUpdatedDoc(searchByNestedAlertInactive0JSON, "search_by_nested_alert_inactive_0",
        SENSOR_NAME);
    findUpdatedDoc(searchByNestedAlertInactive1JSON, "search_by_nested_alert_inactive_1",
        SENSOR_NAME);

    // Put the nested type into the test index, so that it'll match appropriately
    ((ElasticsearchDao) esDao).getClient().admin().indices().preparePutMapping(INDEX)
        .setType("test_doc")
        .setSource(nestedAlertMapping)
        .get();

    // Create metaalerts
    Map<String, Object> activeMetaAlertJSON = JSONUtils.INSTANCE
        .load(activeMetaAlert, new TypeReference<Map<String, Object>>() {
        });
    activeMetaAlertJSON.put("alert",
        Arrays.asList(searchByNestedAlertActive0JSON, searchByNestedAlertActive1JSON));
    Map<String, Object> inactiveMetaAlertJSON = JSONUtils.INSTANCE
        .load(inactiveMetaAlert, new TypeReference<Map<String, Object>>() {
        });
    inactiveMetaAlertJSON.put("alert",
        Arrays.asList(searchByNestedAlertInactive0JSON, searchByNestedAlertInactive1JSON));

    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    elasticsearchAdd(Arrays.asList(activeMetaAlertJSON, inactiveMetaAlertJSON),
        MetaAlertDao.METAALERTS_INDEX, MetaAlertDao.METAALERT_TYPE);
    // Wait for updates to persist
    findUpdatedDoc(activeMetaAlertJSON, "active_metaalert", MetaAlertDao.METAALERT_TYPE);

    SearchResponse searchResponse = metaDao.search(new SearchRequest() {
      {
        setQuery(
            "(ip_src_addr:192.168.1.1 AND ip_src_port:8009) OR (alert.ip_src_addr:192.168.1.1 AND alert.ip_src_port:8009)");
        setIndices(Collections.singletonList(MetaAlertDao.METAALERT_TYPE));
        setFrom(0);
        setSize(5);
        setSort(Collections.singletonList(new SortField() {
          {
            setField(Constants.GUID);
          }
        }));
      }
    });
    // Should not have results because nested alerts shouldn't be flattened
    Assert.assertEquals(0, searchResponse.getTotal());

    // Query against all indices. Only the single active meta alert should be returned.
    // The child alerts should be hidden.
    searchResponse = metaDao.search(new SearchRequest() {
      {
        setQuery(
            "(ip_src_addr:192.168.1.1 AND ip_src_port:8010)"
                + " OR (alert.ip_src_addr:192.168.1.1 AND alert.ip_src_port:8010)");
        setIndices(Collections.singletonList("*"));
        setFrom(0);
        setSize(5);
        setSort(Collections.singletonList(new SortField() {
          {
            setField(Constants.GUID);
          }
        }));
      }
    });

    // Nested query should match a nested alert
    Assert.assertEquals(1, searchResponse.getTotal());
    Assert.assertEquals("active_metaalert",
        searchResponse.getResults().get(0).getSource().get("guid"));

    // Query against all indices. The child alert has no actual attached meta alerts, and should
    // be returned on its own.
    searchResponse = metaDao.search(new SearchRequest() {
      {
        setQuery(
            "(ip_src_addr:192.168.1.3 AND ip_src_port:8008)"
                + " OR (alert.ip_src_addr:192.168.1.3 AND alert.ip_src_port:8008)");
        setIndices(Collections.singletonList("*"));
        setFrom(0);
        setSize(5);
        setSort(Collections.singletonList(new SortField() {
          {
            setField(Constants.GUID);
          }
        }));
      }
    });

    // Nested query should match a plain alert
    Assert.assertEquals(1, searchResponse.getTotal());
    Assert.assertEquals("search_by_nested_alert_inactive_0",
        searchResponse.getResults().get(0).getSource().get("guid"));
  }

  @Test
  public void shouldGroupHidesAlert() throws Exception {
    // Create alerts
    List<Map<String, Object>> alerts = new ArrayList<>();
    Map<String, Object> groupByChildAlertJson = JSONUtils.INSTANCE
        .load(groupByChildAlert, new TypeReference<Map<String, Object>>() {
        });
    alerts.add(groupByChildAlertJson);
    Map<String, Object> groupByStandaloneAlertJson = JSONUtils.INSTANCE
        .load(groupByStandaloneAlert, new TypeReference<Map<String, Object>>() {
        });
    alerts.add(groupByStandaloneAlertJson);
    elasticsearchAdd(alerts, INDEX, SENSOR_NAME);
    // Wait for updates to persist
    findUpdatedDoc(groupByChildAlertJson, "group_by_child_alert",
        SENSOR_NAME);
    findUpdatedDoc(groupByStandaloneAlertJson, "group_by_standalone_alert",
        SENSOR_NAME);

    // Put the nested type into the test index, so that it'll match appropriately
    ((ElasticsearchDao) esDao).getClient().admin().indices().preparePutMapping(INDEX)
        .setType("test_doc")
        .setSource(nestedAlertMapping)
        .get();

    // Don't need any meta alerts to actually exist, since we've populated the field on the alerts.

    // Build our group request
    Group searchGroup = new Group();
    searchGroup.setField("ip_src_addr");
    List<Group> groupList = new ArrayList<>();
    groupList.add(searchGroup);
    GroupResponse groupResponse = metaDao.group(new GroupRequest() {
      {
        setQuery("ip_src_addr:192.168.1.1");
        setIndices(Collections.singletonList("*"));
        setScoreField("score_field");
        setGroups(groupList);
    }});

    // Should only return the standalone alert in the group
    GroupResult result = groupResponse.getGroupResults().get(0);
    Assert.assertEquals(1, result.getTotal());
    Assert.assertEquals("192.168.1.1", result.getKey());
    // No delta, since no ops happen
    Assert.assertEquals(10.0d, result.getScore(), 0.0d);
  }

  @Test
  public void testStatusChanges() throws Exception {
    // Create alerts
    List<Map<String, Object>> alerts = new ArrayList<>();
    Map<String, Object> searchByNestedAlertActive0Json = JSONUtils.INSTANCE
        .load(searchByNestedAlertActive0, new TypeReference<Map<String, Object>>() {
        });
    alerts.add(searchByNestedAlertActive0Json);
    Map<String, Object> searchByNestedAlertActive1Json = JSONUtils.INSTANCE
        .load(searchByNestedAlertActive1, new TypeReference<Map<String, Object>>() {
        });
    alerts.add(searchByNestedAlertActive1Json);
    elasticsearchAdd(alerts, INDEX, SENSOR_NAME);
    // Wait for updates to persist
    findUpdatedDoc(searchByNestedAlertActive0Json, "search_by_nested_alert_active_0",
        SENSOR_NAME);
    findUpdatedDoc(searchByNestedAlertActive1Json, "search_by_nested_alert_active_1",
        SENSOR_NAME);

    // Put the nested type into the test index, so that it'll match appropriately
    ((ElasticsearchDao) esDao).getClient().admin().indices().preparePutMapping(INDEX)
        .setType("test_doc")
        .setSource(nestedAlertMapping)
        .get();

    // Create metaalerts
    Map<String, Object> activeMetaAlertJSON = JSONUtils.INSTANCE
        .load(activeMetaAlert, new TypeReference<Map<String, Object>>() {
        });
    activeMetaAlertJSON.put("alert",
        Arrays.asList(searchByNestedAlertActive0Json, searchByNestedAlertActive1Json));

    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    elasticsearchAdd(Collections.singletonList(activeMetaAlertJSON),
        MetaAlertDao.METAALERTS_INDEX, MetaAlertDao.METAALERT_TYPE);
    // Wait for updates to persist
    findUpdatedDoc(activeMetaAlertJSON, "active_metaalert", MetaAlertDao.METAALERT_TYPE);

    // Build our update request to inactive status
    Map<String, Object> documentMap = new HashMap<>();

    documentMap.put("status", MetaAlertStatus.INACTIVE.getStatusString());
    Document document = new Document(documentMap, "active_metaalert", MetaAlertDao.METAALERT_TYPE,
        0L);
    metaDao.update(document, Optional.of(MetaAlertDao.METAALERTS_INDEX));

    Map<String, Object> expectedMetaDoc = new HashMap<>();
    expectedMetaDoc.putAll(activeMetaAlertJSON);
    expectedMetaDoc.put("status", MetaAlertStatus.INACTIVE.getStatusString());

    // Make sure the update has gone through on the meta alert and the child alerts.
    Assert.assertTrue(
        findUpdatedDoc(expectedMetaDoc, "active_metaalert", MetaAlertDao.METAALERT_TYPE));

    Map<String, Object> expectedAlertDoc0 = new HashMap<>();
    expectedAlertDoc0.putAll(searchByNestedAlertActive0Json);
    expectedAlertDoc0.put("metaalerts", new ArrayList<>());
    Assert.assertTrue(
        findUpdatedDoc(expectedAlertDoc0, "search_by_nested_alert_active_0", SENSOR_NAME));

    Map<String, Object> expectedAlertDoc1 = new HashMap<>();
    expectedAlertDoc1.putAll(searchByNestedAlertActive1Json);
    expectedAlertDoc1.put("metaalerts", new ArrayList<>());
    Assert.assertTrue(
        findUpdatedDoc(expectedAlertDoc1, "search_by_nested_alert_active_1", SENSOR_NAME));

    // Search against the indices. Should return the two alerts, but not the inactive metaalert.
    SearchRequest searchRequest = new SearchRequest();
    ArrayList<String> indices = new ArrayList<>();
    indices.add(SENSOR_NAME);
    indices.add(MetaAlertDao.METAALERT_TYPE);
    searchRequest.setIndices(indices);
    searchRequest.setSize(5);
    searchRequest.setQuery("*");

    // Validate our results
    SearchResult expected0 = new SearchResult();
    expected0.setId((String) expectedAlertDoc0.get(Constants.GUID));
    expected0.setIndex(INDEX);
    expected0.setSource(expectedAlertDoc0);
    expected0.setScore(1.0f);

    SearchResult expected1 = new SearchResult();
    expected1.setId((String) expectedAlertDoc1.get(Constants.GUID));
    expected1.setIndex(INDEX);
    expected1.setSource(expectedAlertDoc1);
    expected1.setScore(1.0f);

    ArrayList<SearchResult> expectedResults = new ArrayList<>();
    expectedResults.add(expected0);
    expectedResults.add(expected1);

    SearchResponse result = metaDao.search(searchRequest);
    Assert.assertEquals(2, result.getTotal());
    // Use set comparison to avoid ordering issues. We already checked counts.
    Assert.assertEquals(new HashSet<>(expectedResults), new HashSet<>(result.getResults()));

    // Build our update request back to active status
    documentMap.put("status", MetaAlertStatus.ACTIVE.getStatusString());
    document = new Document(documentMap, "active_metaalert", MetaAlertDao.METAALERT_TYPE, 0L);
    metaDao.update(document, Optional.of(MetaAlertDao.METAALERTS_INDEX));

    expectedMetaDoc = new HashMap<>();
    expectedMetaDoc.putAll(activeMetaAlertJSON);

    // Make sure the update has gone through on the meta alert and the child alerts.
    Assert.assertTrue(
        findUpdatedDoc(expectedMetaDoc, "active_metaalert", MetaAlertDao.METAALERT_TYPE));

    expectedAlertDoc0 = new HashMap<>();
    expectedAlertDoc0.putAll(searchByNestedAlertActive0Json);
    Assert.assertTrue(
        findUpdatedDoc(expectedAlertDoc0, "search_by_nested_alert_active_0", SENSOR_NAME));

    expectedAlertDoc1 = new HashMap<>();
    expectedAlertDoc1.putAll(searchByNestedAlertActive1Json);
    Assert.assertTrue(
        findUpdatedDoc(expectedAlertDoc1, "search_by_nested_alert_active_1", SENSOR_NAME));

    // Search against the indices. Should return just the active metaalert.
    SearchResult expectedMeta = new SearchResult();
    expectedMeta.setId((String) activeMetaAlertJSON.get(Constants.GUID));
    expectedMeta.setIndex(MetaAlertDao.METAALERTS_INDEX);
    expectedMeta.setSource(activeMetaAlertJSON);
    expectedMeta.setScore(1.0f);

    expectedResults = new ArrayList<>();
    expectedResults.add(expectedMeta);

    result = metaDao.search(searchRequest);
    Assert.assertEquals(1, result.getTotal());
    Assert.assertEquals(expectedResults, result.getResults());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldUpdateMetaAlertOnAlertPatchOrReplace() throws Exception {
    List<Map<String, Object>> inputData = new ArrayList<>();
    Map<String, Object> updateMetaAlertAlert0JSON = JSONUtils.INSTANCE
        .load(updateMetaAlertAlert0, new TypeReference<Map<String, Object>>() {
        });
    inputData.add(updateMetaAlertAlert0JSON);
    Map<String, Object> updateMetaAlertAlert1JSON = JSONUtils.INSTANCE
        .load(updateMetaAlertAlert1, new TypeReference<Map<String, Object>>() {
        });
    inputData.add(updateMetaAlertAlert1JSON);
    elasticsearchAdd(inputData, INDEX, SENSOR_NAME);
    // Wait for updates to persist
    findUpdatedDoc(updateMetaAlertAlert1JSON, "update_metaalert_alert_1", SENSOR_NAME);

    MetaAlertCreateResponse metaAlertCreateResponse = metaDao
        .createMetaAlert(new MetaAlertCreateRequest() {{
          setGuidToIndices(new HashMap<String, String>() {{
            put("update_metaalert_alert_0", INDEX);
            put("update_metaalert_alert_1", INDEX);
          }});
          setGroups(Collections.singletonList("group"));
        }});
    // Wait for updates to persist
    findCreatedDoc(metaAlertCreateResponse.getGuid(), MetaAlertDao.METAALERT_TYPE);

    // Patch alert
    metaDao.patch(JSONUtils.INSTANCE.load(updateMetaAlertPatchRequest, PatchRequest.class),
        Optional.empty());

    // Wait for updates to persist
    updateMetaAlertAlert0JSON.put("field", "patched value 0");
    findUpdatedDoc(updateMetaAlertAlert0JSON, "update_metaalert_alert_0", SENSOR_NAME);

    Map<String, Object> metaalert = metaDao
        .getLatest(metaAlertCreateResponse.getGuid(), MetaAlertDao.METAALERT_TYPE).getDocument();
    List<Map<String, Object>> alerts = (List<Map<String, Object>>) metaalert.get("alert");
    Assert.assertEquals(2, alerts.size());
    Assert.assertEquals("update_metaalert_alert_1", alerts.get(0).get("guid"));
    Assert.assertEquals("value 1", alerts.get(0).get("field"));
    Assert.assertEquals("update_metaalert_alert_0", alerts.get(1).get("guid"));
    Assert.assertEquals("patched value 0", alerts.get(1).get("field"));

    // Replace alert
    metaDao.replace(JSONUtils.INSTANCE.load(updateMetaAlertReplaceRequest, ReplaceRequest.class),
        Optional.empty());

    // Wait for updates to persist
    updateMetaAlertAlert0JSON.put("field", "replaced value 0");
    findUpdatedDoc(updateMetaAlertAlert0JSON, "update_metaalert_alert_0", SENSOR_NAME);

    metaalert = metaDao.getLatest(metaAlertCreateResponse.getGuid(), MetaAlertDao.METAALERT_TYPE)
        .getDocument();
    alerts = (List<Map<String, Object>>) metaalert.get("alert");
    Assert.assertEquals(2, alerts.size());
    Assert.assertEquals("update_metaalert_alert_1", alerts.get(0).get("guid"));
    Assert.assertEquals("value 1", alerts.get(0).get("field"));
    Assert.assertEquals("update_metaalert_alert_0", alerts.get(1).get("guid"));
    Assert.assertEquals("replaced value 0", alerts.get(1).get("field"));
  }

  protected boolean findUpdatedDoc(Map<String, Object> message0, String guid, String sensorType)
      throws InterruptedException, IOException {
    boolean found = false;
    for (int t = 0; t < MAX_RETRIES && !found; ++t, Thread.sleep(SLEEP_MS)) {
      Document doc = metaDao.getLatest(guid, sensorType);
      if (doc != null && message0.equals(doc.getDocument())) {
        found = true;
      }
    }
    return found;
  }

  protected boolean findCreatedDoc(String guid, String sensorType)
      throws InterruptedException, IOException {
    boolean found = false;
    for (int t = 0; t < MAX_RETRIES && !found; ++t, Thread.sleep(SLEEP_MS)) {
      Document doc = metaDao.getLatest(guid, sensorType);
      if (doc != null) {
        found = true;
      }
    }
    return found;
  }

  protected void elasticsearchAdd(List<Map<String, Object>> inputData, String index, String docType)
      throws IOException {
    es.add(index, docType, inputData.stream().map(m -> {
          try {
            return JSONUtils.INSTANCE.toJSON(m, true);
          } catch (JsonProcessingException e) {
            throw new IllegalStateException(e.getMessage(), e);
          }
        }
        ).collect(Collectors.toList())
    );
  }
}
