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
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.elasticsearch.dao.ElasticsearchDao;
import org.apache.metron.elasticsearch.dao.ElasticsearchMetaAlertDao;
import org.apache.metron.elasticsearch.dao.MetaAlertStatus;
import org.apache.metron.elasticsearch.integration.components.ElasticSearchComponent;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MetaAlertDao;
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
        Document doc = metaDao.getLatest(guid, SENSOR_NAME);
        Assert.assertEquals(message0, doc.getDocument());
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

      Document doc = metaDao.getLatest(guid, SENSOR_NAME);
      Assert.assertEquals(message0, doc.getDocument());
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
