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

import static org.apache.metron.indexing.dao.metaalert.MetaAlertConstants.ALERT_FIELD;
import static org.apache.metron.indexing.dao.metaalert.MetaAlertConstants.METAALERT_FIELD;
import static org.apache.metron.indexing.dao.metaalert.MetaAlertConstants.METAALERT_TYPE;
import static org.apache.metron.indexing.dao.metaalert.MetaAlertConstants.THREAT_FIELD_DEFAULT;
import static org.apache.metron.indexing.dao.metaalert.MetaAlertConstants.THREAT_SORT_DEFAULT;
import static org.apache.metron.solr.SolrConstants.SOLR_ZOOKEEPER;
import static org.apache.metron.solr.dao.SolrMetaAlertDao.METAALERTS_COLLECTION;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConfig;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConstants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertIntegrationTest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SortField;
import org.apache.metron.solr.client.SolrClientFactory;
import org.apache.metron.solr.dao.SolrDao;
import org.apache.metron.solr.dao.SolrMetaAlertDao;
import org.apache.metron.solr.dao.SolrMetaAlertRetrieveLatestDao;
import org.apache.metron.solr.dao.SolrMetaAlertSearchDao;
import org.apache.metron.solr.dao.SolrMetaAlertUpdateDao;
import org.apache.metron.solr.integration.components.SolrComponent;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.zookeeper.KeeperException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SolrMetaAlertIntegrationTest extends MetaAlertIntegrationTest {

  private static final String COLLECTION = "test";

  private static SolrDao solrDao;
  private static SolrComponent solr;

  @BeforeClass
  public static void setupBefore() throws Exception {
    // Solr doesn't need retries, it'll show up after a commit.

    MAX_RETRIES = 1;
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
        put(SOLR_ZOOKEEPER, solr.getZookeeperUrl());
      }
    };
    accessConfig.setMaxSearchResults(1000);
    accessConfig.setGlobalConfigSupplier(() -> globalConfig);
    accessConfig.setMaxSearchGroups(100);
    // Just use sensorType directly as the collection name.
    accessConfig.setIndexSupplier(s -> s);

    solrDao = new SolrDao();
    solrDao.init(accessConfig);

    MetaAlertConfig config = new MetaAlertConfig(METAALERTS_COLLECTION
                             , THREAT_SORT_DEFAULT
                             , () -> ImmutableMap.of(Constants.SENSOR_TYPE_FIELD_PROPERTY, Constants.SENSOR_TYPE
                                                    , Constants.THREAT_SCORE_FIELD_PROPERTY, THREAT_FIELD_DEFAULT
                                                    )
    ) {

      @Override
      protected String getDefaultThreatTriageField() {
        return THREAT_FIELD_DEFAULT.replace(':', '.');
      }

      @Override
      protected String getDefaultSourceTypeField() {
        return Constants.SENSOR_TYPE;
      }
    };

    SolrClient solrClient = SolrClientFactory.create(globalConfig);
    SolrMetaAlertSearchDao searchDao = new SolrMetaAlertSearchDao(
        solrClient,
        solrDao.getSolrSearchDao(), config);
    SolrMetaAlertRetrieveLatestDao retrieveLatestDao = new SolrMetaAlertRetrieveLatestDao(solrClient, solrDao);
    SolrMetaAlertUpdateDao updateDao = new SolrMetaAlertUpdateDao(solrClient, solrDao, searchDao,
        retrieveLatestDao, config);
    metaDao = new SolrMetaAlertDao(solrDao, searchDao, updateDao, retrieveLatestDao);
  }

  @Before
  public void setup()
      throws IOException, InterruptedException, SolrServerException, KeeperException {
    solr.addCollection(METAALERTS_COLLECTION,
        "../metron-solr/src/main/config/schema//metaalert");
    solr.addCollection(SENSOR_NAME, "../metron-solr/src/test/resources/config/test/conf");
  }

  @AfterClass
  public static void teardown() {
    SolrClientFactory.close();
    if (solr != null) {
      solr.stop();
    }
  }

  @After
  public void reset() {
    solr.reset();
  }

  @Test
  @Override
  @SuppressWarnings("unchecked")
  public void shouldSearchByNestedAlert() throws Exception {
    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(4);
    alerts.get(0).put(METAALERT_FIELD, Collections.singletonList("meta_active"));
    alerts.get(0).put("ip_src_addr", "192.168.1.1");
    alerts.get(0).put("ip_src_port", 8010);
    alerts.get(1).put(METAALERT_FIELD, Collections.singletonList("meta_active"));
    alerts.get(1).put("ip_src_addr", "192.168.1.2");
    alerts.get(1).put("ip_src_port", 8009);
    alerts.get(2).put("ip_src_addr", "192.168.1.3");
    alerts.get(2).put("ip_src_port", 8008);
    alerts.get(3).put("ip_src_addr", "192.168.1.4");
    alerts.get(3).put("ip_src_port", 8007);
    addRecords(alerts, getTestIndexName(), SENSOR_NAME);

    // Put the nested type into the test index, so that it'll match appropriately
    setupTypings();

    // Load metaAlerts
    Map<String, Object> activeMetaAlert = buildMetaAlert("meta_active", MetaAlertStatus.ACTIVE,
        Optional.of(Arrays.asList(alerts.get(0), alerts.get(1))));
    Map<String, Object> inactiveMetaAlert = buildMetaAlert("meta_inactive",
        MetaAlertStatus.INACTIVE,
        Optional.of(Arrays.asList(alerts.get(2), alerts.get(3))));
    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    addRecords(Arrays.asList(activeMetaAlert, inactiveMetaAlert), METAALERTS_COLLECTION,
        METAALERT_TYPE);

    // Verify load was successful
    findCreatedDocs(Arrays.asList(
        new GetRequest("message_0", SENSOR_NAME),
        new GetRequest("message_1", SENSOR_NAME),
        new GetRequest("message_2", SENSOR_NAME),
        new GetRequest("message_3", SENSOR_NAME),
        new GetRequest("meta_active", METAALERT_TYPE),
        new GetRequest("meta_inactive", METAALERT_TYPE)));

    SearchResponse searchResponse = metaDao.search(new SearchRequest() {
      {
        setQuery(
            "ip_src_addr:192.168.1.1 AND ip_src_port:8010");
        setIndices(Collections.singletonList(METAALERT_TYPE));
        setFrom(0);
        setSize(5);
        setSort(Collections.singletonList(new SortField() {
          {
            setField(Constants.GUID);
          }
        }));
      }
    });
    // Should have one result because Solr will return the parent.
    Assert.assertEquals(1, searchResponse.getTotal());
    // Ensure we returned the child alerts
    List<Map<String, Object>> actualAlerts = (List<Map<String, Object>>) searchResponse.getResults()
        .get(0).getSource()
        .get(MetaAlertConstants.ALERT_FIELD);
    Assert.assertEquals(2, actualAlerts.size());
    Assert.assertEquals("meta_active",
        searchResponse.getResults().get(0).getSource().get("guid"));

    // Query against all indices. Only the single active meta alert should be returned.
    // The child alerts should be hidden.
    searchResponse = metaDao.search(new SearchRequest() {
      {
        setQuery(
            "ip_src_addr:192.168.1.1 AND ip_src_port:8010");
        setIndices(queryIndices);
        setFrom(0);
        setSize(5);
        setSort(Collections.singletonList(new SortField() {
          {
            setField(Constants.GUID);
          }
        }));
      }
    });

    // Query should match a parent alert
    Assert.assertEquals(1, searchResponse.getTotal());
    // Ensure we returned the child alerts
    actualAlerts = (List<Map<String, Object>>) searchResponse.getResults().get(0).getSource()
        .get(MetaAlertConstants.ALERT_FIELD);
    Assert.assertEquals(2, actualAlerts.size());
    Assert.assertEquals("meta_active",
        searchResponse.getResults().get(0).getSource().get("guid"));

    // Query against all indices. The child alert has no actual attached meta alerts, and should
    // be returned on its own.
    searchResponse = metaDao.search(new SearchRequest() {
      {
        setQuery(
            "ip_src_addr:192.168.1.3 AND ip_src_port:8008");
        setIndices(queryIndices);
        setFrom(0);
        setSize(1);
        setSort(Collections.singletonList(new SortField() {
          {
            setField(Constants.GUID);
          }
        }));
      }
    });

    // Query should match a plain alert
    Assert.assertEquals(1, searchResponse.getTotal());
    // Ensure we have no child alerts
    actualAlerts = (List<Map<String, Object>>) searchResponse.getResults()
        .get(0).getSource()
        .get(MetaAlertConstants.ALERT_FIELD);
    Assert.assertNull(actualAlerts);
    Assert.assertEquals("message_2",
        searchResponse.getResults().get(0).getSource().get("guid"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldNotRetrieveFullChildrenWithoutSourceType() throws Exception {
    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(1);
    alerts.get(0).put(METAALERT_FIELD, Collections.singletonList("meta_active"));
    alerts.get(0).put("ip_src_addr", "192.168.1.1");
    alerts.get(0).put("ip_src_port", 8010);
    addRecords(alerts, getTestIndexName(), SENSOR_NAME);

    // Put the nested type into the test index, so that it'll match appropriately
    setupTypings();

    // Load metaAlerts
    Map<String, Object> activeMetaAlert = buildMetaAlert("meta_active", MetaAlertStatus.ACTIVE,
        Optional.of(Arrays.asList(alerts.get(0))));
    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    addRecords(Collections.singletonList(activeMetaAlert), METAALERTS_COLLECTION, METAALERT_TYPE);

    // Verify load was successful
    findCreatedDocs(Collections.singletonList(new GetRequest("meta_active", METAALERT_TYPE)));

    SearchResponse searchResponse = metaDao.search(new SearchRequest() {
      {
        setQuery(
            "ip_src_addr:192.168.1.1 AND ip_src_port:8010");
        setIndices(Collections.singletonList(METAALERT_TYPE));
        setFrom(0);
        setSize(5);
        setFields(Collections.singletonList(Constants.GUID));
        setSort(Collections.singletonList(new SortField() {
          {
            setField(Constants.GUID);
          }
        }));
      }
    });

    // Should have one result because Solr will return the parent.
    Assert.assertEquals(1, searchResponse.getTotal());
    // Ensure we returned didn't return the child alerts
    List<Map<String, Object>> actualAlerts = (List<Map<String, Object>>) searchResponse.getResults()
        .get(0).getSource()
        .get(MetaAlertConstants.ALERT_FIELD);
    Assert.assertNull(actualAlerts);
    Assert.assertEquals("meta_active",
        searchResponse.getResults().get(0).getSource().get("guid"));
  }

  @Override
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

  @Override
  protected long getMatchingMetaAlertCount(String fieldName, String fieldValue)
      throws InterruptedException {
    long cnt = 0;
    for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t, Thread.sleep(SLEEP_MS)) {
      List<Map<String, Object>> docs = solr.getAllIndexedDocs(METAALERTS_COLLECTION);
      cnt = docs
          .stream()
          .filter(d -> {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> alerts = (List<Map<String, Object>>) d
                .get(ALERT_FIELD);

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
  protected String getMetaAlertIndex() {
    return METAALERTS_COLLECTION;
  }

  @Override
  protected String getSourceTypeField() {
    return Constants.SENSOR_TYPE;
  }

  @Override
  protected void commit() throws IOException {
    try {
      List<String> collections = solr.getSolrClient().listCollections();
      for (String collection : collections) {
        solr.getSolrClient().commit(collection);
      }
    } catch (SolrServerException e) {
      throw new IOException("Unable to commit", e);
    }
  }

  @Override
  protected void setEmptiedMetaAlertField(Map<String, Object> docMap) {
    docMap.remove(METAALERT_FIELD);
  }

  @Override
  protected boolean isFiniteDoubleOnly() {
    return false;
  }

  @Override
  protected boolean isEmptyMetaAlertList() {
    return false;
  }
}
