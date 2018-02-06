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

package org.apache.metron.solr.integration;

import static org.apache.metron.indexing.dao.MetaAlertDao.ALERT_FIELD;
import static org.apache.metron.indexing.dao.MetaAlertDao.METAALERT_TYPE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MetaAlertDao;
import org.apache.metron.indexing.dao.MetaAlertIntegrationTest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.OriginalNotFoundException;
import org.apache.metron.solr.dao.SolrDao;
import org.apache.metron.solr.dao.SolrMetaAlertDao;
import org.apache.metron.solr.integration.components.SolrComponent;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public class SolrMetaAlertIntegrationTest extends MetaAlertIntegrationTest {

  private static final String COLLECTION = "metron";

  private static IndexDao solrDao;
  private static SolrComponent solr;

  @BeforeClass
  public static void setupBefore() throws Exception {
    // setup the client
    solr = new SolrComponent.Builder()
        .addCollection(COLLECTION, "../metron-solr/src/test/resources/solr/conf")
        .build();
    solr.start();

    AccessConfig accessConfig = new AccessConfig();
    Map<String, Object> globalConfig = new HashMap<String, Object>() {
      {
        put("solr.clustername", "metron");
        put("solr.port", "9300");
        put("solr.ip", "localhost");
        put("solr.date.format", DATE_FORMAT);
      }
    };
    accessConfig.setMaxSearchResults(1000);
    accessConfig.setGlobalConfigSupplier(() -> globalConfig);
    accessConfig.setMaxSearchGroups(100);

    solrDao = new SolrDao();
    solrDao.init(accessConfig);
    metaDao = new SolrMetaAlertDao(solrDao);
  }

  @Before
  public void setup() throws IOException {
//    solr.createIndexWithMapping(METAALERTS_INDEX, MetaAlertDao.METAALERT_DOC, template.replace("%MAPPING_NAME%", "metaalert"));
//    solr.createIndexWithMapping(INDEX, "index_doc", template.replace("%MAPPING_NAME%", "index"));
  }

  protected InMemoryComponent startIndex() throws Exception {
    solrComponent = new SolrComponent.Builder()
        .addCollection("bro", "../metron-solr/src/test/resources/config/bro/conf")
        .addCollection("snort", "../metron-solr/src/test/resources/config/snort/conf")
        .build();
    solrComponent.start();
    return solrComponent;
  }

  @AfterClass
  public static void teardown() {
    if (solr != null) {
      solr.stop();
    }
  }

  @After
  public void reset() {
    solr.reset();
  }

  protected long getMatchingAlertCount(String fieldName, Object fieldValue) throws IOException, InterruptedException {
    long cnt = 0;
//    for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t, Thread.sleep(SLEEP_MS)) {
//      List<Map<String, Object>> docs = solr.getAllIndexedDocs(INDEX, SENSOR_NAME + "_doc");
//      cnt = docs
//          .stream()
//          .filter(d -> {
//            Object newfield = d.get(fieldName);
//            return newfield != null && newfield.equals(fieldValue);
//          }).count();
//    }
    return cnt;
  }



  protected long getMatchingMetaAlertCount(String fieldName, String fieldValue) throws IOException, InterruptedException {
    long cnt = 0;
//    for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t, Thread.sleep(SLEEP_MS)) {
//      List<Map<String, Object>> docs = solr.getAllIndexedDocs(METAALERTS_INDEX, MetaAlertDao.METAALERT_DOC);
//      cnt = docs
//          .stream()
//          .filter(d -> {
//            List<Map<String, Object>> alerts = (List<Map<String, Object>>) d
//                .get(MetaAlertDao.ALERT_FIELD);
//
//            for (Map<String, Object> alert : alerts) {
//              Object newField = alert.get(fieldName);
//              if (newField != null && newField.equals(fieldValue)) {
//                return true;
//              }
//            }
//
//            return false;
//          }).count();
//    }
    return cnt;
  }



  protected void findUpdatedDoc(Map<String, Object> message0, String guid, String sensorType)
      throws InterruptedException, IOException, OriginalNotFoundException {
    for (int t = 0; t < MAX_RETRIES; ++t, Thread.sleep(SLEEP_MS)) {
      Document doc = metaDao.getLatest(guid, sensorType);
      if (doc != null && message0.equals(doc.getDocument())) {
        return;
      }
    }
    throw new OriginalNotFoundException("Count not find " + guid + " after " + MAX_RETRIES + " tries");
  }

  protected boolean findCreatedDoc(String guid, String sensorType)
      throws InterruptedException, IOException, OriginalNotFoundException {
    for (int t = 0; t < MAX_RETRIES; ++t, Thread.sleep(SLEEP_MS)) {
      Document doc = metaDao.getLatest(guid, sensorType);
      if (doc != null) {
        return true;
      }
    }
    throw new OriginalNotFoundException("Count not find " + guid + " after " + MAX_RETRIES + "tries");
  }

  protected boolean findCreatedDocs(List<GetRequest> getRequests)
      throws InterruptedException, IOException, OriginalNotFoundException {
    for (int t = 0; t < MAX_RETRIES; ++t, Thread.sleep(SLEEP_MS)) {
      Iterable<Document> docs = metaDao.getAllLatest(getRequests);
      if (docs != null) {
        int docCount = 0;
        for (Document doc: docs) {
          docCount++;
        }
        if (getRequests.size() == docCount) {
          return true;
        }
      }
    }
    throw new OriginalNotFoundException("Count not find guids after " + MAX_RETRIES + "tries");
  }

  protected List<Map<String, Object>> buildAlerts(int count) {
    List<Map<String, Object>> inputData = new ArrayList<>();
    for (int i = 0; i < count; ++i) {
      final String guid = "message_" + i;
      Map<String, Object> alerts = new HashMap<>();
      alerts.put(Constants.GUID, guid);
      alerts.put("source:type", SENSOR_NAME);
      alerts.put(MetaAlertDao.THREAT_FIELD_DEFAULT, i);
      alerts.put("timestamp", System.currentTimeMillis());
      inputData.add(alerts);
    }
    return inputData;
  }

  protected List<Map<String, Object>> buildMetaAlerts(int count, MetaAlertStatus status, Optional<List<Map<String, Object>>> alerts) {
    List<Map<String, Object>> inputData = new ArrayList<>();
    for (int i = 0; i < count; ++i) {
      final String guid = "meta_" + status.getStatusString() + "_" + i;
      inputData.add(buildMetaAlert(guid, status, alerts));
    }
    return inputData;
  }

  protected Map<String, Object> buildMetaAlert(String guid, MetaAlertStatus status, Optional<List<Map<String, Object>>> alerts) {
    Map<String, Object> metaAlert = new HashMap<>();
    metaAlert.put(Constants.GUID, guid);
    metaAlert.put("source:type", METAALERT_TYPE);
    metaAlert.put(MetaAlertDao.STATUS_FIELD, status.getStatusString());
    if (alerts.isPresent()) {
      List<Map<String, Object>> alertsList = alerts.get();
      metaAlert.put(ALERT_FIELD, alertsList);
    }
    return metaAlert;
  }

  protected void SolrAdd(List<Map<String, Object>> inputData, String index, String docType)
      throws IOException {
//    solr.add(index, docType, inputData.stream().map(m -> {
//          try {
//            return JSONUtils.INSTANCE.toJSON(m, true);
//          } catch (JsonProcessingException e) {
//            throw new IllegalStateException(e.getMessage(), e);
//          }
//        }
//        ).collect(Collectors.toList())
//    );
  }


  @Override
  protected void addRecords(List<Map<String, Object>> inputData, String index, String docType)
      throws IOException {

  }

  @Override
  protected void setupTypings() {

  }
}
