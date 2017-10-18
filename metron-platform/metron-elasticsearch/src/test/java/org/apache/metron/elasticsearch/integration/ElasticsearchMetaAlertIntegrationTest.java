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
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SortField;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.ReplaceRequest;
import org.junit.AfterClass;
import org.junit.Assert;
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
  private static IndexDao metaDao;
  private static ElasticSearchComponent es;

  @BeforeClass
  public static void setup() throws Exception {
    // setup the client
    es = new ElasticSearchComponent.Builder()
        .withHttpPort(9211)
        .withIndexDir(new File(INDEX_DIR))
        .build();
    es.start();

    es.createIndexWithMapping(MetaAlertDao.METAALERTS_INDEX, MetaAlertDao.METAALERT_DOC,
        buildMetaMappingSource());

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

    esDao = new ElasticsearchDao();
    esDao.init(accessConfig);
    metaDao = new ElasticsearchMetaAlertDao(esDao);
  }

  @AfterClass
  public static void teardown() {
    if (es != null) {
      es.stop();
    }
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

  /**
   {
     "guid": "active_metaalert",
     "source:type": "metaalert",
     "alert": [],
     "status": "active"
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

  @Test
  public void shouldSearchByStatus() throws Exception {
    List<Map<String, Object>> metaInputData = new ArrayList<>();
    Map<String, Object> activeMetaAlertJSON = JSONUtils.INSTANCE.load(activeMetaAlert, new TypeReference<Map<String, Object>>() {});
    metaInputData.add(activeMetaAlertJSON);
    Map<String, Object> inactiveMetaAlertJSON = JSONUtils.INSTANCE.load(inactiveMetaAlert, new TypeReference<Map<String, Object>>() {});
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
        setSort(Collections.singletonList(new SortField(){{ setField(Constants.GUID); }}));
      }
    });
    Assert.assertEquals(1, searchResponse.getTotal());
    Assert.assertEquals(MetaAlertStatus.ACTIVE.getStatusString(), searchResponse.getResults().get(0).getSource().get(MetaAlertDao.STATUS_FIELD));
  }

  /**
   {
   "guid": "search_by_nested_alert_active_0",
   "source:type": "test",
   "ip_src_addr": "192.168.1.1",
   "ip_src_port": 8010
   }
   */
  @Multiline
  public static String searchByNestedAlertActive0;

  /**
   {
   "guid": "search_by_nested_alert_inactive_1",
   "source:type": "test",
   "ip_src_addr": "192.168.1.2",
   "ip_src_port": 8009
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

  @Test
  public void shouldSearchByNestedAlert() throws Exception {
    // Create alerts
    List<Map<String, Object>> alerts = new ArrayList<>();
    Map<String, Object> searchByNestedAlertActive0JSON = JSONUtils.INSTANCE.load(searchByNestedAlertActive0, new TypeReference<Map<String, Object>>() {});
    alerts.add(searchByNestedAlertActive0JSON);
    Map<String, Object> searchByNestedAlertActive1JSON = JSONUtils.INSTANCE.load(searchByNestedAlertActive1, new TypeReference<Map<String, Object>>() {});
    alerts.add(searchByNestedAlertActive1JSON);
    Map<String, Object> searchByNestedAlertInactive0JSON = JSONUtils.INSTANCE.load(searchByNestedAlertInactive0, new TypeReference<Map<String, Object>>() {});
    alerts.add(searchByNestedAlertInactive0JSON);
    Map<String, Object> searchByNestedAlertInactive1JSON = JSONUtils.INSTANCE.load(searchByNestedAlertInactive1, new TypeReference<Map<String, Object>>() {});
    alerts.add(searchByNestedAlertInactive1JSON);
    elasticsearchAdd(alerts, INDEX, SENSOR_NAME);
    // Wait for updates to persist
    findUpdatedDoc(searchByNestedAlertInactive1JSON, "search_by_nested_alert_inactive_1", SENSOR_NAME);

    // Create metaalerts
    Map<String, Object> activeMetaAlertJSON = JSONUtils.INSTANCE.load(activeMetaAlert, new TypeReference<Map<String, Object>>() {});
    activeMetaAlertJSON.put("alert", Arrays.asList(searchByNestedAlertActive0JSON, searchByNestedAlertActive1JSON));
    Map<String, Object> inactiveMetaAlertJSON = JSONUtils.INSTANCE.load(inactiveMetaAlert, new TypeReference<Map<String, Object>>() {});
    inactiveMetaAlertJSON.put("alert", Arrays.asList(searchByNestedAlertInactive0JSON, searchByNestedAlertInactive1JSON));

    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    elasticsearchAdd(Arrays.asList(activeMetaAlertJSON, inactiveMetaAlertJSON), MetaAlertDao.METAALERTS_INDEX, MetaAlertDao.METAALERT_TYPE);
    // Wait for updates to persist
    findUpdatedDoc(activeMetaAlertJSON, "active_metaalert", MetaAlertDao.METAALERT_TYPE);

    SearchResponse searchResponse = metaDao.search(new SearchRequest() {
      {
        setQuery("(ip_src_addr:192.168.1.1 AND ip_src_port:8009) OR (alert.ip_src_addr:192.168.1.1 AND alert.ip_src_port:8009)");
        setIndices(Collections.singletonList(MetaAlertDao.METAALERT_TYPE));
        setFrom(0);
        setSize(5);
        setSort(Collections.singletonList(new SortField(){{ setField(Constants.GUID); }}));
      }
    });
    // Should not have results because nested alerts shouldn't be flattened
    Assert.assertEquals(0, searchResponse.getTotal());

    searchResponse = metaDao.search(new SearchRequest() {
      {
        setQuery("(ip_src_addr:192.168.1.1 AND ip_src_port:8010) OR (alert.ip_src_addr:192.168.1.1 AND alert.ip_src_port:8010)");
        setIndices(Collections.singletonList(MetaAlertDao.METAALERT_TYPE));
        setFrom(0);
        setSize(5);
        setSort(Collections.singletonList(new SortField(){{ setField(Constants.GUID); }}));
      }
    });
    // Nested query should match a nested alert
    Assert.assertEquals(1, searchResponse.getTotal());
    Assert.assertEquals("active_metaalert", searchResponse.getResults().get(0).getSource().get("guid"));
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
