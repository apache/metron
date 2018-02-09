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

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.elasticsearch.dao.ElasticsearchDao;
import org.apache.metron.elasticsearch.dao.ElasticsearchMetaAlertDao;
import org.apache.metron.elasticsearch.integration.components.ElasticSearchComponent;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MetaAlertDao;
import org.apache.metron.indexing.dao.MetaAlertIntegrationTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public class ElasticsearchMetaAlertIntegrationTest extends MetaAlertIntegrationTest {
  private static IndexDao esDao;
  private static ElasticSearchComponent es;

  protected static final String INDEX_DIR = "target/elasticsearch_meta";

  protected static final String INDEX =
      SENSOR_NAME + "_index_" + new SimpleDateFormat(DATE_FORMAT).format(new Date());

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
   * {
       "%MAPPING_NAME%_doc" : {
         "properties" : {
           "guid" : {
             "type" : "keyword"
           },
           "ip_src_addr" : {
             "type" : "keyword"
           },
           "score" : {
             "type" : "integer"
           },
           "alert" : {
             "type" : "nested"
           }
         }
       }
   }
   */
  @Multiline
  public static String template;

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
    es.createIndexWithMapping(metaDao.getMetaAlertIndex(), MetaAlertDao.METAALERT_DOC, template.replace("%MAPPING_NAME%", "metaalert"));
    es.createIndexWithMapping(INDEX, "index_doc", template.replace("%MAPPING_NAME%", "index"));
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

  protected long getMatchingAlertCount(String fieldName, Object fieldValue)
      throws IOException, InterruptedException {
    long cnt = 0;
    for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t, Thread.sleep(SLEEP_MS)) {
      List<Map<String, Object>> docs = es.getAllIndexedDocs(INDEX, SENSOR_NAME + "_doc");
      cnt = docs
          .stream()
          .filter(d -> {
            Object newfield = d.get(fieldName);
            return newfield != null && newfield.equals(fieldValue);
          }).count();
    }
    return cnt;
  }

  protected long getMatchingMetaAlertCount(String fieldName, String fieldValue)
      throws IOException, InterruptedException {
    long cnt = 0;
    for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t, Thread.sleep(SLEEP_MS)) {
      List<Map<String, Object>> docs = es
          .getAllIndexedDocs(metaDao.getMetaAlertIndex(), MetaAlertDao.METAALERT_DOC);
      cnt = docs
          .stream()
          .filter(d -> {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> alerts = (List<Map<String, Object>>) d
                .get(MetaAlertDao.ALERT_FIELD);

            for (Map<String, Object> alert : alerts) {
              Object newField = alert.get(fieldName);
              if (newField != null && newField.equals(fieldValue)) {
                return true;
              }
            }

            return false;
          }).count();
    }
    return cnt;
  }

  protected void addRecords(List<Map<String, Object>> inputData, String index, String docType)
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

  protected void setupTypings() {
    ((ElasticsearchDao) esDao).getClient().admin().indices().preparePutMapping(INDEX)
        .setType("test_doc")
        .setSource(nestedAlertMapping)
        .get();
  }

  @Override
  protected String getTestIndexName() {
    return INDEX;
  }
}
