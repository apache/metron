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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MetaAlertDao;
import org.apache.metron.indexing.dao.MetaAlertIntegrationTest;
import org.apache.metron.solr.dao.SolrDao;
import org.apache.metron.solr.dao.SolrMetaAlertDao;
import org.apache.metron.solr.integration.components.SolrComponent;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.zookeeper.KeeperException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public class SolrMetaAlertIntegrationTest extends MetaAlertIntegrationTest {
  private static final String COLLECTION = "test";

  private static IndexDao solrDao;
  private static SolrComponent solr;

  @BeforeClass
  public static void setupBefore() throws Exception {
    // setup the client
    solr = new SolrComponent.Builder().build();
    solr.start();

    AccessConfig accessConfig = new AccessConfig();
    Map<String, Object> globalConfig = new HashMap<String, Object>() {
      {
        put("solr.clustername", "metron");
        put("solr.port", "9300");
        put("solr.ip", "localhost");
        put("solr.date.format", DATE_FORMAT);
        put("solr.zookeeper", solr.getZookeeperUrl());
      }
    };
    accessConfig.setMaxSearchResults(1000);
    accessConfig.setGlobalConfigSupplier(() -> globalConfig);
    accessConfig.setMaxSearchGroups(100);
    // Just use sensorType directly as the collection name.
    accessConfig.setIndexSupplier(s -> s);

    solrDao = new SolrDao();
    solrDao.init(accessConfig);
    metaDao = new SolrMetaAlertDao(solrDao);
  }

  @Before
  public void setup()
      throws IOException, InterruptedException, SolrServerException, KeeperException {
    // TODO what are we naming the collections?
    solr.addCollection(metaDao.getMetaAlertIndex(), "../metron-solr/src/test/resources/config/metaalert/conf");
    solr.addCollection(SENSOR_NAME, "../metron-solr/src/test/resources/config/test/conf");
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

  protected long getMatchingAlertCount(String fieldName, Object fieldValue)
      throws InterruptedException {
    long cnt = 0;
    for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t, Thread.sleep(SLEEP_MS)) {
      List<Map<String, Object>> docs = solr.getAllIndexedDocs(getTestIndexName());
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
      throws InterruptedException {
    long cnt = 0;
    for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t, Thread.sleep(SLEEP_MS)) {
      List<Map<String, Object>> docs = solr.getAllIndexedDocs(metaDao.getMetaAlertIndex());
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

  @Override
  protected void addRecords(List<Map<String, Object>> inputData, String index, String docType)
      throws IOException {
    // Ignore docType for Solr. It's unused.
    try {
      solr.addDocs(index, inputData);
    } catch (SolrServerException e) {
      throw new IOException("Unable to load Solr Docs", e);
    }
  }

  @Override
  protected void setupTypings() {

  }

  @Override
  protected String getTestIndexName() {
    return COLLECTION;
  }

  @Override
  protected void commit() throws IOException {
    try {
      List<String> collections = solr.getSolrClient().listCollections();
      for (String collection : collections) {
        // TODO remove this
        System.out.println("***** COMMITTING COLLECTION: " + collection);
        solr.getSolrClient().commit(collection);
      }
    } catch (SolrServerException e) {
      throw new IOException("Unable to commit", e);
    }
  }
}
