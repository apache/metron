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
import com.google.common.collect.Iterables;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.elasticsearch.dao.ElasticsearchDao;
import org.apache.metron.elasticsearch.dao.ElasticsearchMetaAlertDao;
import org.apache.metron.elasticsearch.integration.components.ElasticSearchComponent;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.HBaseDao;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MultiIndexDao;
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
  private static final String TABLE_NAME = "modifications";
  private static final String CF = "p";
  private static final String METAALERT_NAME = "metaalert";
  private static String indexDir = "target/elasticsearch_meta";
  private static String dateFormat = "yyyy.MM.dd.HH";
  private static String index =
      SENSOR_NAME + "_index_" + new SimpleDateFormat(dateFormat).format(new Date());
  private static String metaAlertIndex = "metaalerts";
  private static String metaTypeMapping = "metaalert_doc";

  private static String metaMappingSource;
  private static IndexDao esDao;
  private static MultiIndexDao dao;
  private static ElasticSearchComponent es;

  @BeforeClass
  public static void setup() throws Exception {
    buildMetaMappingSource();
    // setup the client
    es = new ElasticSearchComponent.Builder()
        .withHttpPort(9211)
        .withIndexDir(new File(indexDir))
        .build();
    es.start();

    es.createIndexWithMapping(metaAlertIndex, metaTypeMapping, metaMappingSource);

    AccessConfig accessConfig = new AccessConfig();
    Map<String, Object> globalConfig = new HashMap<String, Object>() {{
      put("es.clustername", "metron");
      put("es.port", "9300");
      put("es.ip", "localhost");
      put("es.date.format", dateFormat);
      put(HBaseDao.HBASE_TABLE, TABLE_NAME);
      put(HBaseDao.HBASE_CF, CF);
    }};
    accessConfig.setGlobalConfigSupplier(() -> globalConfig);

    esDao = new ElasticsearchDao();

    dao = new MultiIndexDao(Collections.singletonList(esDao), dao -> {
      if (dao instanceof ElasticsearchDao) {
        System.out.println("Returning new wrapped DAO: " + dao.getClass());
        return new ElasticsearchMetaAlertDao(dao, "threat_triage_level");
      } else {
        System.out.println("Returning new unwrapped DAO: " + dao.getClass());
        return dao;
      }
    }
    );
    dao.init(accessConfig);

  }

  protected static void buildMetaMappingSource() throws IOException {
    metaMappingSource = jsonBuilder().prettyPrint()
        .startObject()
        .startObject(metaTypeMapping)
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
        .endObject().string();
  }

  @AfterClass
  public static void teardown() {
    if (es != null) {
      es.stop();
    }
  }

  @Test
  public void test() throws Exception {
    List<Map<String, Object>> inputData = new ArrayList<>();
    for (int i = 0; i < 2; ++i) {
      final String name = "message" + i;
      int finalI = i;
      inputData.add(
          new HashMap<String, Object>() {{
            put("source:type", SENSOR_NAME);
            put("name", name);
            put("threat_triage_level", finalI);
            put("timestamp", System.currentTimeMillis());
            put(Constants.GUID, name);
          }}
      );
    }

    es.add(index, SENSOR_NAME
        , Iterables.transform(inputData,
            m -> {
              try {
                return JSONUtils.INSTANCE.toJSON(m, true);
              } catch (JsonProcessingException e) {
                throw new IllegalStateException(e.getMessage(), e);
              }
            }
        )
    );

    List<Map<String, Object>> metaInputData = new ArrayList<>();
    final String name = "meta_message";
    Map<String, Object>[] alertArray = new Map[1];
    alertArray[0] = inputData.get(0);
    metaInputData.add(
        new HashMap<String, Object>() {{
          put("source:type", SENSOR_NAME);
          put("name", name);

          put("alert", alertArray);
          put(Constants.GUID, name);
        }}
    );

    es.add(metaAlertIndex, METAALERT_NAME
        , Iterables.transform(metaInputData,
            m -> {
              try {
                return JSONUtils.INSTANCE.toJSON(m, true);
              } catch (JsonProcessingException e) {
                throw new IllegalStateException(e.getMessage(), e);
              }
            }
        )
    );

    List<Map<String, Object>> docs = null;
    for (int t = 0; t < MAX_RETRIES; ++t, Thread.sleep(SLEEP_MS)) {
      docs = es.getAllIndexedDocs(index, SENSOR_NAME + "_doc");
      if (docs.size() >= 10) {
        break;
      }
    }
    Assert.assertEquals(2, docs.size());
    //modify the first message and add a new field
    {
      Map<String, Object> message0 = new HashMap<String, Object>(inputData.get(0)) {{
        put("new-field", "metron");
        put("threat_triage_level", "10");
      }};
      String guid = "" + message0.get(Constants.GUID);
      dao.replace(new ReplaceRequest() {
        {
          setReplacement(message0);
          setGuid(guid);
          setSensorType(SENSOR_NAME);
        }
      }, Optional.empty());
      Thread.sleep(2000);

      {
        //ensure alerts in ES are up-to-date
        Document doc = dao.getLatest(guid, SENSOR_NAME);
        Assert.assertEquals(message0, doc.getDocument());
        long cnt = 0;
        for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t, Thread.sleep(SLEEP_MS)) {
          docs = es.getAllIndexedDocs(index, SENSOR_NAME + "_doc");
          cnt = docs
              .stream()
              .filter(d -> {
                Object newfield = d.get("new-field");
                return newfield != null && newfield.equals(message0.get("new-field"));
              }).count();
        }
        if (cnt == 0) {
          Assert.fail("Elasticsearch is not updated!");
        }
      }

      {
        //ensure meta alerts in ES are up-to-date
//        Document doc = dao.getLatest(guid, METAALERT_NAME);
//        Assert.assertEquals(message0, doc.getDocument());
        long cnt = 0;
        for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t, Thread.sleep(SLEEP_MS)) {
          docs = es.getAllIndexedDocs(metaAlertIndex, METAALERT_NAME + "_doc");
          System.out.println("META DOCS: " + docs);
          cnt = docs
              .stream()
              .filter(d -> {
                Object newfield = d.get("new-field");
                return newfield != null && newfield.equals(message0.get("new-field"));
              }).count();
        }
        if (cnt == 0) {
          Assert.fail("Elasticsearch is not updated!");
        }
      }
    }
    //modify the same message and modify the new field
    {
      Map<String, Object> message0 = new HashMap<String, Object>(inputData.get(0)) {{
        put("new-field", "metron2");
      }};
      String guid = "" + message0.get(Constants.GUID);
      dao.replace(new ReplaceRequest() {
        {
          setReplacement(message0);
          setGuid(guid);
          setSensorType(SENSOR_NAME);
        }
      }, Optional.empty());

      Document doc = dao.getLatest(guid, SENSOR_NAME);
      Assert.assertEquals(message0, doc.getDocument());
      {
        //ensure ES is up-to-date
        long cnt = 0;
        for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t, Thread.sleep(SLEEP_MS)) {
          docs = es.getAllIndexedDocs(index, SENSOR_NAME + "_doc");
          cnt = docs
              .stream()
              .filter(d -> message0.get("new-field").equals(d.get("new-field")))
              .count();
        }
        Assert.assertNotEquals("Elasticsearch is not updated!", cnt, 0);
        if (cnt == 0) {
          Assert.fail("Elasticsearch is not updated!");
        }
      }
    }
  }
}
