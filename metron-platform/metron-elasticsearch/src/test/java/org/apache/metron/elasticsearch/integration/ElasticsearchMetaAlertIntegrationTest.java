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

import static org.apache.metron.common.Constants.SENSOR_TYPE;
import static org.apache.metron.common.Constants.SENSOR_TYPE_FIELD_PROPERTY;
import static org.apache.metron.indexing.dao.MetaAlertDao.ALERT_FIELD;
import static org.apache.metron.indexing.dao.MetaAlertDao.METAALERTS_INDEX;
import static org.apache.metron.indexing.dao.MetaAlertDao.METAALERT_FIELD;
import static org.apache.metron.indexing.dao.MetaAlertDao.METAALERT_TYPE;
import static org.apache.metron.indexing.dao.MetaAlertDao.STATUS_FIELD;
import static org.apache.metron.indexing.dao.MetaAlertDao.THREAT_FIELD_PROPERTY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
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
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.elasticsearch.dao.ElasticsearchDao;
import org.apache.metron.elasticsearch.dao.ElasticsearchMetaAlertDao;
import org.apache.metron.elasticsearch.integration.components.ElasticSearchComponent;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MetaAlertDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.Group;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.GroupResult;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.metron.indexing.dao.search.SortField;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.OriginalNotFoundException;
import org.apache.metron.indexing.dao.update.PatchRequest;
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
  private static final String NAME_FIELD = "name";

  private static ElasticsearchDao esDao;
  private static MetaAlertDao metaDao;
  private static ElasticSearchComponent es;

  /**
   {
     "properties": {
       "metron_alert": {
         "type": "nested"
       }
     }
   }
   */
  @Multiline
  public static String nestedAlertMapping;

  /**
   {
     "guid": "meta_alert",
     "index": "metaalert_index",
     "patch": [
       {
         "op": "add",
         "path": "/name",
         "value": "New Meta Alert"
       }
     ],
     "sensorType": "metaalert"
   }
   */
  @Multiline
  public static String namePatchRequest;

  /**
   {
     "guid": "meta_alert",
     "index": "metaalert_index",
     "patch": [
       {
         "op": "add",
         "path": "/name",
         "value": "New Meta Alert"
       },
       {
         "op": "add",
         "path": "/alert",
         "value": []
       }
     ],
     "sensorType": "metaalert"
   }
   */
  @Multiline
  public static String alertPatchRequest;

  /**
   {
     "guid": "meta_alert",
     "index": "metaalert_index",
     "patch": [
       {
         "op": "add",
         "path": "/status",
         "value": "inactive"
       },
       {
         "op": "add",
         "path": "/name",
         "value": "New Meta Alert"
       }
     ],
     "sensorType": "metaalert"
   }
   */
  @Multiline
  public static String statusPatchRequest;

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
           "metron_alert" : {
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
  }

  @Before
  public void setup() throws IOException {
    es.createIndexWithMapping(METAALERTS_INDEX, MetaAlertDao.METAALERT_DOC, template.replace("%MAPPING_NAME%", "metaalert"));
    es.createIndexWithMapping(INDEX, "index_doc", template.replace("%MAPPING_NAME%", "index"));

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


  @Test
  public void shouldGetAllMetaAlertsForAlert() throws Exception {
    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(3);
    elasticsearchAdd(alerts, INDEX, SENSOR_NAME);

    // Load metaAlerts
    List<Map<String, Object>> metaAlerts = buildMetaAlerts(12, MetaAlertStatus.ACTIVE,
        Optional.of(Collections.singletonList(alerts.get(0))));
    metaAlerts.add(buildMetaAlert("meta_active_12", MetaAlertStatus.ACTIVE,
        Optional.of(Arrays.asList(alerts.get(0), alerts.get(2)))));
    metaAlerts.add(buildMetaAlert("meta_inactive", MetaAlertStatus.INACTIVE,
        Optional.of(Arrays.asList(alerts.get(0), alerts.get(2)))));
    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    elasticsearchAdd(metaAlerts, METAALERTS_INDEX, MetaAlertDao.METAALERT_TYPE);

    // Verify load was successful
    List<GetRequest> createdDocs = metaAlerts.stream().map(metaAlert ->
        new GetRequest((String) metaAlert.get(Constants.GUID), METAALERT_TYPE))
        .collect(Collectors.toList());
    createdDocs.addAll(alerts.stream().map(alert ->
        new GetRequest((String) alert.get(Constants.GUID), SENSOR_NAME))
        .collect(Collectors.toList()));
    findCreatedDocs(createdDocs);

    int previousPageSize = ((ElasticsearchMetaAlertDao) metaDao).getPageSize();
    ((ElasticsearchMetaAlertDao) metaDao).setPageSize(5);

    {
      // Verify searches successfully return more than 10 results
      SearchResponse searchResponse0 = metaDao.getAllMetaAlertsForAlert("message_0");
      List<SearchResult> searchResults0 = searchResponse0.getResults();
      Assert.assertEquals(13, searchResults0.size());
      Set<Map<String, Object>> resultSet = new HashSet<>();
      Iterables.addAll(resultSet, Iterables.transform(searchResults0, r -> r.getSource()));
      StringBuffer reason = new StringBuffer("Unable to find " + metaAlerts.get(0) + "\n");
      reason.append(Joiner.on("\n").join(resultSet));
      Assert.assertTrue(reason.toString(), resultSet.contains(metaAlerts.get(0)));

      // Verify no meta alerts are returned because message_1 was not added to any
      SearchResponse searchResponse1 = metaDao.getAllMetaAlertsForAlert("message_1");
      List<SearchResult> searchResults1 = searchResponse1.getResults();
      Assert.assertEquals(0, searchResults1.size());

      // Verify only the meta alert message_2 was added to is returned
      SearchResponse searchResponse2 = metaDao.getAllMetaAlertsForAlert("message_2");
      List<SearchResult> searchResults2 = searchResponse2.getResults();
      Assert.assertEquals(1, searchResults2.size());
      Assert.assertEquals(metaAlerts.get(12), searchResults2.get(0).getSource());
    }
    ((ElasticsearchMetaAlertDao) metaDao).setPageSize(previousPageSize);
  }

  @Test
  public void getAllMetaAlertsForAlertShouldThrowExceptionForEmtpyGuid() throws Exception {
    try {
      metaDao.getAllMetaAlertsForAlert("");
      Assert.fail("An exception should be thrown for empty guid");
    } catch (InvalidSearchException ise) {
      Assert.assertEquals("Guid cannot be empty", ise.getMessage());
    }
  }

  @Test
  public void shouldCreateMetaAlert() throws Exception {
    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(3);
    elasticsearchAdd(alerts, INDEX, SENSOR_NAME);

    // Verify load was successful
    findCreatedDocs(Arrays.asList(
        new GetRequest("message_0", SENSOR_NAME),
        new GetRequest("message_1", SENSOR_NAME),
        new GetRequest("message_2", SENSOR_NAME)));

    {
      MetaAlertCreateRequest metaAlertCreateRequest = new MetaAlertCreateRequest() {{
        setAlerts(new ArrayList<GetRequest>() {{
          add(new GetRequest("message_1", SENSOR_NAME));
          add(new GetRequest("message_2", SENSOR_NAME, INDEX));
        }});
        setGroups(Collections.singletonList("group"));
      }};
      MetaAlertCreateResponse metaAlertCreateResponse = metaDao.createMetaAlert(metaAlertCreateRequest);
      {
        // Verify metaAlert was created
        findCreatedDoc(metaAlertCreateResponse.getGuid(), MetaAlertDao.METAALERT_TYPE);
      }
      {
        // Verify metaalert has the default field names
        Document metaAlert = metaDao.getLatest(metaAlertCreateResponse.getGuid(), MetaAlertDao.METAALERT_TYPE);
        Assert.assertTrue(metaAlert.getDocument().containsKey(ElasticsearchMetaAlertDao.SOURCE_TYPE));
        Assert.assertTrue(metaAlert.getDocument().containsKey(ElasticsearchMetaAlertDao.THREAT_TRIAGE_FIELD));
      }
      {
        // Verify alert 0 was not updated with metaalert field
        Document alert = metaDao.getLatest("message_0", SENSOR_NAME);
        Assert.assertEquals(4, alert.getDocument().size());
        Assert.assertNull(alert.getDocument().get(METAALERT_FIELD));
      }
      {
        // Verify alert 1 was properly updated with metaalert field
        Document alert = metaDao.getLatest("message_1", SENSOR_NAME);
        Assert.assertEquals(5, alert.getDocument().size());
        Assert.assertEquals(1, ((List) alert.getDocument().get(METAALERT_FIELD)).size());
        Assert.assertEquals(metaAlertCreateResponse.getGuid(), ((List) alert.getDocument().get(METAALERT_FIELD)).get(0));
      }
      {
        // Verify alert 2 was properly updated with metaalert field
        Document alert = metaDao.getLatest("message_2", SENSOR_NAME);
        Assert.assertEquals(5, alert.getDocument().size());
        Assert.assertEquals(1, ((List) alert.getDocument().get(METAALERT_FIELD)).size());
        Assert.assertEquals(metaAlertCreateResponse.getGuid(), ((List) alert.getDocument().get(METAALERT_FIELD)).get(0));
      }
    }
  }

  @Test
  public void shouldCreateMetaAlertWithConfiguredFieldNames() throws Exception {
    // Configure field names
    AccessConfig accessConfig = esDao.getAccessConfig();
    accessConfig.setGlobalConfigSupplier(() -> new HashMap<String, Object>() {{
      put("es.date.format", DATE_FORMAT);
      put(SENSOR_TYPE_FIELD_PROPERTY, SENSOR_TYPE);
      put(THREAT_FIELD_PROPERTY, MetaAlertDao.THREAT_FIELD_DEFAULT);
    }});

    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(1);
    elasticsearchAdd(alerts, INDEX, SENSOR_NAME);

    // Verify load was successful
    findCreatedDocs(Collections.singletonList(
            new GetRequest("message_0", SENSOR_NAME)));

    {
      MetaAlertCreateRequest metaAlertCreateRequest = new MetaAlertCreateRequest() {{
        setAlerts(new ArrayList<GetRequest>() {{
          add(new GetRequest("message_0", SENSOR_NAME));
        }});
        setGroups(Collections.singletonList("group"));
      }};
      MetaAlertCreateResponse metaAlertCreateResponse = metaDao.createMetaAlert(metaAlertCreateRequest);
      {
        // Verify metaAlert was created
        findCreatedDoc(metaAlertCreateResponse.getGuid(), MetaAlertDao.METAALERT_TYPE);
      }
      {
        // Verify alert 0 was not updated with metaalert field
        Document metaAlert = metaDao.getLatest(metaAlertCreateResponse.getGuid(), MetaAlertDao.METAALERT_TYPE);
        Assert.assertTrue(metaAlert.getDocument().containsKey(SENSOR_TYPE));
        Assert.assertTrue(metaAlert.getDocument().containsKey(MetaAlertDao.THREAT_FIELD_DEFAULT));
      }
    }
  }

  @Test
  public void shouldAddAlertsToMetaAlert() throws Exception {
    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(4);
    alerts.get(0).put(METAALERT_FIELD, Collections.singletonList("meta_alert"));
    elasticsearchAdd(alerts, INDEX, SENSOR_NAME);

    // Load metaAlert
    Map<String, Object> metaAlert = buildMetaAlert("meta_alert", MetaAlertStatus.ACTIVE,
        Optional.of(Collections.singletonList(alerts.get(0))));
    elasticsearchAdd(Collections.singletonList(metaAlert), METAALERTS_INDEX, METAALERT_TYPE);

    // Verify load was successful
    findCreatedDocs(Arrays.asList(
        new GetRequest("message_0", SENSOR_NAME),
        new GetRequest("message_1", SENSOR_NAME),
        new GetRequest("message_2", SENSOR_NAME),
        new GetRequest("message_3", SENSOR_NAME),
        new GetRequest("meta_alert", METAALERT_TYPE)));

    // Build expected metaAlert after alerts are added
    Map<String, Object> expectedMetaAlert = new HashMap<>(metaAlert);

    // Verify the proper alerts were added
    List<Map<String, Object>> metaAlertAlerts = new ArrayList<>((List<Map<String, Object>>) expectedMetaAlert.get(ALERT_FIELD));
    Map<String, Object> expectedAlert0 = alerts.get(0);
    Map<String, Object> expectedAlert1 = alerts.get(1);
    expectedAlert1.put(METAALERT_FIELD, Collections.singletonList("meta_alert"));
    metaAlertAlerts.add(expectedAlert1);
    Map<String, Object> expectedAlert2 = alerts.get(2);
    expectedAlert2.put(METAALERT_FIELD, Collections.singletonList("meta_alert"));
    metaAlertAlerts.add(expectedAlert2);
    expectedMetaAlert.put(ALERT_FIELD, metaAlertAlerts);

    // Verify the counts were properly updated
    expectedMetaAlert.put("average", 1.0d);
    expectedMetaAlert.put("min", 0.0d);
    expectedMetaAlert.put("median", 1.0d);
    expectedMetaAlert.put("max", 2.0d);
    expectedMetaAlert.put("count", 3);
    expectedMetaAlert.put("sum", 3.0d);
    expectedMetaAlert.put("threat:triage:score", 3.0d);

    {
      // Verify alerts were successfully added to the meta alert
      Assert.assertTrue(metaDao.addAlertsToMetaAlert("meta_alert", Arrays.asList(new GetRequest("message_1", SENSOR_NAME), new GetRequest("message_2", SENSOR_NAME))));
      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);
    }

    {
      // Verify False when alerts are already in a meta alert and no new alerts are added
      Assert.assertFalse(metaDao.addAlertsToMetaAlert("meta_alert", Arrays.asList(new GetRequest("message_0", SENSOR_NAME), new GetRequest("message_1", SENSOR_NAME))));
      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);
    }

    {
      // Verify only 1 alert is added when a list of alerts only contains 1 alert that is not in the meta alert
      metaAlertAlerts = new ArrayList<>((List<Map<String, Object>>) expectedMetaAlert.get(ALERT_FIELD));
      Map<String, Object> expectedAlert3 = alerts.get(3);
      expectedAlert3.put(METAALERT_FIELD, Collections.singletonList("meta_alert"));
      metaAlertAlerts.add(expectedAlert3);
      expectedMetaAlert.put(ALERT_FIELD, metaAlertAlerts);

      expectedMetaAlert.put("average", 1.5d);
      expectedMetaAlert.put("min", 0.0d);
      expectedMetaAlert.put("median", 1.5d);
      expectedMetaAlert.put("max", 3.0d);
      expectedMetaAlert.put("count", 4);
      expectedMetaAlert.put("sum", 6.0d);
      expectedMetaAlert.put("threat:triage:score", 6.0d);

      Assert.assertTrue(metaDao.addAlertsToMetaAlert("meta_alert", Arrays.asList(new GetRequest("message_2", SENSOR_NAME), new GetRequest("message_3", SENSOR_NAME))));
      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);
    }

  }

  @Test
  public void shouldRemoveAlertsFromMetaAlert() throws Exception {
    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(4);
    alerts.get(0).put(METAALERT_FIELD, Collections.singletonList("meta_alert"));
    alerts.get(1).put(METAALERT_FIELD, Collections.singletonList("meta_alert"));
    alerts.get(2).put(METAALERT_FIELD, Collections.singletonList("meta_alert"));
    alerts.get(3).put(METAALERT_FIELD, Collections.singletonList("meta_alert"));
    elasticsearchAdd(alerts, INDEX, SENSOR_NAME);

    // Load metaAlert
    Map<String, Object> metaAlert = buildMetaAlert("meta_alert", MetaAlertStatus.ACTIVE,
        Optional.of(Arrays.asList(alerts.get(0), alerts.get(1), alerts.get(2), alerts.get(3))));
    elasticsearchAdd(Collections.singletonList(metaAlert), METAALERTS_INDEX, METAALERT_TYPE);

    // Verify load was successful
    findCreatedDocs(Arrays.asList(
        new GetRequest("message_0", SENSOR_NAME),
        new GetRequest("message_1", SENSOR_NAME),
        new GetRequest("message_2", SENSOR_NAME),
        new GetRequest("message_3", SENSOR_NAME),
        new GetRequest("meta_alert", METAALERT_TYPE)));

    // Build expected metaAlert after alerts are added
    Map<String, Object> expectedMetaAlert = new HashMap<>(metaAlert);

    // Verify the proper alerts were added
    List<Map<String, Object>> metaAlertAlerts = new ArrayList<>((List<Map<String, Object>>) expectedMetaAlert.get(ALERT_FIELD));
    metaAlertAlerts.remove(0);
    metaAlertAlerts.remove(0);
    expectedMetaAlert.put(ALERT_FIELD, metaAlertAlerts);

    // Verify the counts were properly updated
    expectedMetaAlert.put("average", 2.5d);
    expectedMetaAlert.put("min", 2.0d);
    expectedMetaAlert.put("median", 2.5d);
    expectedMetaAlert.put("max", 3.0d);
    expectedMetaAlert.put("count", 2);
    expectedMetaAlert.put("sum", 5.0d);
    expectedMetaAlert.put("threat:triage:score", 5.0d);


    {
      // Verify a list of alerts are removed from a meta alert
      Assert.assertTrue(metaDao.removeAlertsFromMetaAlert("meta_alert", Arrays.asList(new GetRequest("message_0", SENSOR_NAME), new GetRequest("message_1", SENSOR_NAME))));
      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);
    }

    {
      // Verify False when alerts are not present in a meta alert and no alerts are removed
      Assert.assertFalse(metaDao.removeAlertsFromMetaAlert("meta_alert", Arrays.asList(new GetRequest("message_0", SENSOR_NAME), new GetRequest("message_1", SENSOR_NAME))));
      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);
    }

    {
      // Verify only 1 alert is removed when a list of alerts only contains 1 alert that is in the meta alert
      metaAlertAlerts = new ArrayList<>((List<Map<String, Object>>) expectedMetaAlert.get(ALERT_FIELD));
      metaAlertAlerts.remove(0);
      expectedMetaAlert.put(ALERT_FIELD, metaAlertAlerts);

      expectedMetaAlert.put("average", 3.0d);
      expectedMetaAlert.put("min", 3.0d);
      expectedMetaAlert.put("median", 3.0d);
      expectedMetaAlert.put("max", 3.0d);
      expectedMetaAlert.put("count", 1);
      expectedMetaAlert.put("sum", 3.0d);
      expectedMetaAlert.put("threat:triage:score", 3.0d);

      Assert.assertTrue(metaDao.removeAlertsFromMetaAlert("meta_alert", Arrays.asList(new GetRequest("message_0", SENSOR_NAME), new GetRequest("message_2", SENSOR_NAME))));
      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);
    }

    {
      // Verify all alerts are removed from a metaAlert
      metaAlertAlerts = new ArrayList<>((List<Map<String, Object>>) expectedMetaAlert.get(ALERT_FIELD));
      metaAlertAlerts.remove(0);
      expectedMetaAlert.put(ALERT_FIELD, metaAlertAlerts);

      expectedMetaAlert.put("average", 0.0d);
      expectedMetaAlert.put("min", "Infinity");
      expectedMetaAlert.put("median", "NaN");
      expectedMetaAlert.put("max", "-Infinity");
      expectedMetaAlert.put("count", 0);
      expectedMetaAlert.put("sum", 0.0d);
      expectedMetaAlert.put("threat:triage:score", 0.0d);

      Assert.assertTrue(metaDao.removeAlertsFromMetaAlert("meta_alert",
          Collections.singletonList(new GetRequest("message_3", SENSOR_NAME))));
      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);
    }

  }

  @Test
  public void addRemoveAlertsShouldThrowExceptionForInactiveMetaAlert() throws Exception {
    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(2);
    alerts.get(0).put(METAALERT_FIELD, Collections.singletonList("meta_alert"));
    elasticsearchAdd(alerts, INDEX, SENSOR_NAME);

    // Load metaAlert
    Map<String, Object> metaAlert = buildMetaAlert("meta_alert", MetaAlertStatus.INACTIVE,
        Optional.of(Collections.singletonList(alerts.get(0))));
    elasticsearchAdd(Collections.singletonList(metaAlert), METAALERTS_INDEX, METAALERT_TYPE);

    // Verify load was successful
    findCreatedDocs(Arrays.asList(
        new GetRequest("message_0", SENSOR_NAME),
        new GetRequest("message_1", SENSOR_NAME),
        new GetRequest("meta_alert", METAALERT_TYPE)));

    {
      // Verify alerts cannot be added to an INACTIVE meta alert
      try {
        metaDao.addAlertsToMetaAlert("meta_alert",
            Collections.singletonList(new GetRequest("message_1", SENSOR_NAME)));
        Assert.fail("Adding alerts to an inactive meta alert should throw an exception");
      } catch (IllegalStateException ise) {
        Assert.assertEquals("Adding alerts to an INACTIVE meta alert is not allowed", ise.getMessage());
      }
    }

    {
      // Verify alerts cannot be removed from an INACTIVE meta alert
      try {
        metaDao.removeAlertsFromMetaAlert("meta_alert",
            Collections.singletonList(new GetRequest("message_0", SENSOR_NAME)));
        Assert.fail("Removing alerts from an inactive meta alert should throw an exception");
      } catch (IllegalStateException ise) {
        Assert.assertEquals("Removing alerts from an INACTIVE meta alert is not allowed", ise.getMessage());
      }
    }
  }

  @Test
  public void shouldUpdateMetaAlertStatus() throws Exception {
    int numChildAlerts = 25;
    int numUnrelatedAlerts = 25;
    int totalAlerts = numChildAlerts + numUnrelatedAlerts;

    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(totalAlerts);
    List<Map<String, Object>> childAlerts = alerts.subList(0, numChildAlerts);
    List<Map<String, Object>> unrelatedAlerts = alerts.subList(numChildAlerts, totalAlerts);
    for (Map<String, Object> alert : childAlerts) {
      alert.put(METAALERT_FIELD, Collections.singletonList("meta_alert"));
    }
    elasticsearchAdd(alerts, INDEX, SENSOR_NAME);

    // Load metaAlerts
    Map<String, Object> metaAlert = buildMetaAlert("meta_alert", MetaAlertStatus.ACTIVE,
        Optional.of(childAlerts));
    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    elasticsearchAdd(Collections.singletonList(metaAlert), METAALERTS_INDEX,
        MetaAlertDao.METAALERT_TYPE);

    List<GetRequest> requests = new ArrayList<>();
    for (int i = 0; i < numChildAlerts; ++i) {
      requests.add(new GetRequest("message_" + i, SENSOR_NAME));
    }
    requests.add(new GetRequest("meta_alert", METAALERT_TYPE));

    // Verify load was successful
    findCreatedDocs(requests);

    {
      // Verify status changed to inactive and child alerts are updated
      Assert.assertTrue(metaDao.updateMetaAlertStatus("meta_alert", MetaAlertStatus.INACTIVE));

      Map<String, Object> expectedMetaAlert = new HashMap<>(metaAlert);
      expectedMetaAlert.put(STATUS_FIELD, MetaAlertStatus.INACTIVE.getStatusString());

      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);

      for (int i = 0; i < numChildAlerts; ++i) {
        Map<String, Object> expectedAlert = new HashMap<>(childAlerts.get(i));
        expectedAlert.put("metaalerts", new ArrayList());
        findUpdatedDoc(expectedAlert, "message_" + i, SENSOR_NAME);
      }

      // Ensure unrelated alerts are unaffected
      for (int i = 0; i < numUnrelatedAlerts; ++i) {
        Map<String, Object> expectedAlert = new HashMap<>(unrelatedAlerts.get(i));
        // Make sure to handle the guid offset from creation
        findUpdatedDoc(expectedAlert, "message_" + (i + numChildAlerts), SENSOR_NAME);
      }
    }

    {
      // Verify status changed to active and metaalerts fields on child alerts are updated
      Assert.assertTrue(metaDao.updateMetaAlertStatus("meta_alert", MetaAlertStatus.ACTIVE));

      Map<String, Object> expectedMetaAlert = new HashMap<>(metaAlert);
      expectedMetaAlert.put(STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString());

      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);

      for (int i = 0; i < numChildAlerts; ++i) {
        Map<String, Object> expectedAlert = new HashMap<>(alerts.get(i));
        expectedAlert.put("metaalerts", Collections.singletonList("meta_alert"));
        findUpdatedDoc(expectedAlert, "message_" + i, SENSOR_NAME);
      }

      // Ensure unrelated alerts are unaffected
      for (int i = 0; i < numUnrelatedAlerts; ++i) {
        Map<String, Object> expectedAlert = new HashMap<>(unrelatedAlerts.get(i));
        // Make sure to handle the guid offset from creation
        findUpdatedDoc(expectedAlert, "message_" + (i + numChildAlerts), SENSOR_NAME);
      }

      {
        // Verify status changed to current status has no effect
        Assert.assertFalse(metaDao.updateMetaAlertStatus("meta_alert", MetaAlertStatus.ACTIVE));

        findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);

        for (int i = 0; i < numChildAlerts; ++i) {
          Map<String, Object> expectedAlert = new HashMap<>(alerts.get(i));
          expectedAlert.put("metaalerts", Collections.singletonList("meta_alert"));
          findUpdatedDoc(expectedAlert, "message_" + i, SENSOR_NAME);
        }

        // Ensure unrelated alerts are unaffected
        for (int i = 0; i < numUnrelatedAlerts; ++i) {
          Map<String, Object> expectedAlert = new HashMap<>(unrelatedAlerts.get(i));
          // Make sure to handle the guid offset from creation
          findUpdatedDoc(expectedAlert, "message_" + (i + numChildAlerts), SENSOR_NAME);
        }
      }
    }

    {
      // Verify status changed to active and meta alert child alerts are refreshed
      Assert.assertTrue(metaDao.updateMetaAlertStatus("meta_alert", MetaAlertStatus.INACTIVE));

      Map<String, Object> expectedMetaAlert = new HashMap<>(metaAlert);
      expectedMetaAlert.put(STATUS_FIELD, MetaAlertStatus.INACTIVE.getStatusString());

      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);

      {
        // Update a child alert by adding a field
        Document alert0 = esDao.getLatest("message_0", SENSOR_NAME);
        alert0.getDocument().put("field", "value");
        esDao.update(alert0, Optional.empty());

        findUpdatedDoc(alert0.getDocument(), "message_0", SENSOR_NAME);

        // Change the status to active
        Assert.assertTrue(metaDao.updateMetaAlertStatus("meta_alert", MetaAlertStatus.ACTIVE));

        // Expect the first child alert to also contain the update
        expectedMetaAlert.put(STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString());
        List<Map<String, Object>> expectedAlerts = (List<Map<String, Object>>) expectedMetaAlert.get(ALERT_FIELD);
        expectedAlerts.get(0).put("field", "value");

        // Verify the metaalert child alerts were refreshed and the new field is present
        findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);
      }

    }
  }

  @Test
  public void shouldSearchByStatus() throws Exception {
    // Load metaAlerts
    Map<String, Object> activeMetaAlert = buildMetaAlert("meta_active", MetaAlertStatus.ACTIVE,
        Optional.empty());
    Map<String, Object> inactiveMetaAlert = buildMetaAlert("meta_inactive", MetaAlertStatus.INACTIVE,
        Optional.empty());


    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    elasticsearchAdd(Arrays.asList(activeMetaAlert, inactiveMetaAlert), METAALERTS_INDEX, MetaAlertDao.METAALERT_TYPE);

    // Verify load was successful
    findCreatedDocs(Arrays.asList(
        new GetRequest("meta_active", METAALERT_TYPE),
        new GetRequest("meta_inactive", METAALERT_TYPE)));

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

    // Verify only active meta alerts are returned
    Assert.assertEquals(1, searchResponse.getTotal());
    Assert.assertEquals(MetaAlertStatus.ACTIVE.getStatusString(),
        searchResponse.getResults().get(0).getSource().get(MetaAlertDao.STATUS_FIELD));
  }


  @Test
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
    elasticsearchAdd(alerts, INDEX, SENSOR_NAME);

    // Put the nested type into the test index, so that it'll match appropriately
    ((ElasticsearchDao) esDao).getClient().admin().indices().preparePutMapping(INDEX)
        .setType("test_doc")
        .setSource(nestedAlertMapping)
        .get();

    // Load metaAlerts
    Map<String, Object> activeMetaAlert = buildMetaAlert("meta_active", MetaAlertStatus.ACTIVE,
        Optional.of(Arrays.asList(alerts.get(0), alerts.get(1))));
    Map<String, Object> inactiveMetaAlert = buildMetaAlert("meta_inactive", MetaAlertStatus.INACTIVE,
        Optional.of(Arrays.asList(alerts.get(2), alerts.get(3))));
    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    elasticsearchAdd(Arrays.asList(activeMetaAlert, inactiveMetaAlert), METAALERTS_INDEX, MetaAlertDao.METAALERT_TYPE);

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
            "(ip_src_addr:192.168.1.1 AND ip_src_port:8009) OR (metron_alert.ip_src_addr:192.168.1.1 AND metron_alert.ip_src_port:8009)");
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
                + " OR (metron_alert.ip_src_addr:192.168.1.1 AND metron_alert.ip_src_port:8010)");
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
    Assert.assertEquals("meta_active",
        searchResponse.getResults().get(0).getSource().get("guid"));

    // Query against all indices. The child alert has no actual attached meta alerts, and should
    // be returned on its own.
   searchResponse = metaDao.search(new SearchRequest() {
      {
        setQuery(
            "(ip_src_addr:192.168.1.3 AND ip_src_port:8008)"
                + " OR (metron_alert.ip_src_addr:192.168.1.3 AND metron_alert.ip_src_port:8008)");
        setIndices(Collections.singletonList("*"));
        setFrom(0);
        setSize(1);
        setSort(Collections.singletonList(new SortField() {
          {
            setField(Constants.GUID);
          }
        }));
      }
    });

    // Nested query should match a plain alert
    Assert.assertEquals(1, searchResponse.getTotal());
    Assert.assertEquals("message_2",
        searchResponse.getResults().get(0).getSource().get("guid"));
  }

  @Test
  public void shouldHidesAlertsOnGroup() throws Exception {
    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(2);
    alerts.get(0).put(METAALERT_FIELD, Collections.singletonList("meta_active"));
    alerts.get(0).put("ip_src_addr", "192.168.1.1");
    alerts.get(0).put("score_field", 1);
    alerts.get(1).put("ip_src_addr", "192.168.1.1");
    alerts.get(1).put("score_field", 10);
    elasticsearchAdd(alerts, INDEX, SENSOR_NAME);


    // Put the nested type into the test index, so that it'll match appropriately
    ((ElasticsearchDao) esDao).getClient().admin().indices().preparePutMapping(INDEX)
        .setType("test_doc")
        .setSource(nestedAlertMapping)
        .get();

    // Don't need any meta alerts to actually exist, since we've populated the field on the alerts.

    // Verify load was successful
    findCreatedDocs(Arrays.asList(
        new GetRequest("message_0", SENSOR_NAME),
        new GetRequest("message_1", SENSOR_NAME)));

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

  @SuppressWarnings("unchecked")
  @Test
  public void shouldUpdateMetaAlertOnAlertUpdate() throws Exception {
    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(2);
    alerts.get(0).put(METAALERT_FIELD, Arrays.asList("meta_active", "meta_inactive"));
    elasticsearchAdd(alerts, INDEX, SENSOR_NAME);

    // Load metaAlerts
    Map<String, Object> activeMetaAlert = buildMetaAlert("meta_active", MetaAlertStatus.ACTIVE,
        Optional.of(Collections.singletonList(alerts.get(0))));
    Map<String, Object> inactiveMetaAlert = buildMetaAlert("meta_inactive", MetaAlertStatus.INACTIVE,
        Optional.of(Collections.singletonList(alerts.get(0))));
    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    elasticsearchAdd(Arrays.asList(activeMetaAlert, inactiveMetaAlert), METAALERTS_INDEX, METAALERT_TYPE);

    // Verify load was successful
    findCreatedDocs(Arrays.asList(
        new GetRequest("message_0", SENSOR_NAME),
        new GetRequest("message_1", SENSOR_NAME),
        new GetRequest("meta_active", METAALERT_TYPE),
        new GetRequest("meta_inactive", METAALERT_TYPE)));

    {
      // Modify the first message and add a new field
      Map<String, Object> message0 = new HashMap<String, Object>(alerts.get(0)) {
        {
          put(NEW_FIELD, "metron");
          put(MetaAlertDao.THREAT_FIELD_DEFAULT, "10");
        }
      };
      String guid = "" + message0.get(Constants.GUID);
      metaDao.update(new Document(message0, guid, SENSOR_NAME, null), Optional.empty());

      {
        // Verify alerts in ES are up-to-date
        findUpdatedDoc(message0, guid, SENSOR_NAME);
        long cnt = getMatchingAlertCount(NEW_FIELD, message0.get(NEW_FIELD));
        if (cnt == 0) {
          Assert.fail("Elasticsearch alert not updated!");
        }
      }

      {
        // Verify meta alerts in ES are up-to-date
        long cnt = getMatchingMetaAlertCount(NEW_FIELD, "metron");
        if (cnt == 0) {
          Assert.fail("Active metaalert was not updated!");
        }
        if (cnt != 1) {
          Assert.fail("Elasticsearch metaalerts not updated correctly!");
        }
      }
    }
    //modify the same message and modify the new field
    {
      Map<String, Object> message0 = new HashMap<String, Object>(alerts.get(0)) {
        {
          put(NEW_FIELD, "metron2");
        }
      };
      String guid = "" + message0.get(Constants.GUID);
      metaDao.update(new Document(message0, guid, SENSOR_NAME, null), Optional.empty());

      {
        // Verify ES is up-to-date
        findUpdatedDoc(message0, guid, SENSOR_NAME);
        long cnt = getMatchingAlertCount(NEW_FIELD, message0.get(NEW_FIELD));
        if (cnt == 0) {
          Assert.fail("Elasticsearch alert not updated!");
        }
      }
      {
        // Verify meta alerts in ES are up-to-date
        long cnt = getMatchingMetaAlertCount(NEW_FIELD, "metron2");
        if (cnt == 0) {
          Assert.fail("Active metaalert was not updated!");
        }
        if (cnt != 1) {
          Assert.fail("Elasticsearch metaalerts not updated correctly!");
        }
      }
    }
    //modify the same message and modify the new field with the patch method
    {
      Map<String, Object> message0 = new HashMap<String, Object>(alerts.get(0)) {
        {
          put(NEW_FIELD, "metron3");
        }
      };
      String guid = "" + message0.get(Constants.GUID);
      PatchRequest patchRequest = new PatchRequest();
      patchRequest.setGuid(guid);
      patchRequest.setIndex(INDEX);
      patchRequest.setSensorType(SENSOR_NAME);
      patchRequest.setPatch(Collections.singletonList(new HashMap<String, Object>() {{
        put("op", "replace");
        put("path", "/" + NEW_FIELD);
        put("value", "metron3");
      }}));

      metaDao.patch(patchRequest, Optional.empty());

      {
        // Verify ES is up-to-date
        findUpdatedDoc(message0, guid, SENSOR_NAME);
        long cnt = getMatchingAlertCount(NEW_FIELD, message0.get(NEW_FIELD));
        if (cnt == 0) {
          Assert.fail("Elasticsearch alert not updated!");
        }
      }
      {
        // Verify meta alerts in ES are up-to-date
        long cnt = getMatchingMetaAlertCount(NEW_FIELD, "metron3");
        if (cnt == 0) {
          Assert.fail("Active metaalert was not updated!");
        }
        if (cnt != 1) {
          Assert.fail("Elasticsearch metaalerts not updated correctly!");
        }
      }
    }
  }

  @Test
  public void shouldThrowExceptionOnMetaAlertUpdate() throws Exception {
    Document metaAlert = new Document(new HashMap<>(), "meta_alert", METAALERT_TYPE, 0L);
    try {
      // Verify a meta alert cannot be updated in the meta alert dao
      metaDao.update(metaAlert, Optional.empty());
      Assert.fail("Direct meta alert update should throw an exception");
    } catch (UnsupportedOperationException uoe) {
      Assert.assertEquals("Meta alerts cannot be directly updated", uoe.getMessage());
    }
  }
  @Test
  public void shouldPatchAllowedMetaAlerts() throws Exception {
    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(2);
    alerts.get(0).put(METAALERT_FIELD, Collections.singletonList("meta_active"));
    alerts.get(1).put(METAALERT_FIELD, Collections.singletonList("meta_active"));
    elasticsearchAdd(alerts, INDEX, SENSOR_NAME);

    // Put the nested type into the test index, so that it'll match appropriately
    ((ElasticsearchDao) esDao).getClient().admin().indices().preparePutMapping(INDEX)
        .setType("test_doc")
        .setSource(nestedAlertMapping)
        .get();

    // Load metaAlerts
    Map<String, Object> metaAlert = buildMetaAlert("meta_alert", MetaAlertStatus.ACTIVE,
        Optional.of(Arrays.asList(alerts.get(0), alerts.get(1))));
    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    elasticsearchAdd(Collections.singletonList(metaAlert), METAALERTS_INDEX, MetaAlertDao.METAALERT_TYPE);

    // Verify load was successful
    findCreatedDocs(Arrays.asList(
        new GetRequest("message_0", SENSOR_NAME),
        new GetRequest("message_1", SENSOR_NAME),
        new GetRequest("meta_alert", METAALERT_TYPE)));

    Map<String, Object> expectedMetaAlert = new HashMap<>(metaAlert);
    expectedMetaAlert.put(NAME_FIELD, "New Meta Alert");
    {
      // Verify a patch to a field other than "status" or "alert" can be patched
      PatchRequest patchRequest = JSONUtils.INSTANCE.load(namePatchRequest, PatchRequest.class);
      metaDao.patch(patchRequest, Optional.of(System.currentTimeMillis()));

      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);
    }

    {
      // Verify a patch to an alert field should throw an exception
      try {
        PatchRequest patchRequest = JSONUtils.INSTANCE.load(alertPatchRequest, PatchRequest.class);
        metaDao.patch(patchRequest, Optional.of(System.currentTimeMillis()));

        Assert.fail("A patch on the alert field should throw an exception");
      } catch (IllegalArgumentException iae) {
        Assert.assertEquals("Meta alert patches are not allowed for /alert or /status paths.  "
            + "Please use the add/remove alert or update status functions instead.", iae.getMessage());
      }

      // Verify the metaAlert was not updated
      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);
    }

    {
      // Verify a patch to a status field should throw an exception
      try {
        PatchRequest patchRequest = JSONUtils.INSTANCE.load(statusPatchRequest, PatchRequest.class);
        metaDao.patch(patchRequest, Optional.of(System.currentTimeMillis()));

        Assert.fail("A patch on the status field should throw an exception");
      } catch (IllegalArgumentException iae) {
        Assert.assertEquals("Meta alert patches are not allowed for /alert or /status paths.  "
            + "Please use the add/remove alert or update status functions instead.", iae.getMessage());
      }

      // Verify the metaAlert was not updated
      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);
    }
  }

  protected long getMatchingAlertCount(String fieldName, Object fieldValue) throws IOException, InterruptedException {
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

  protected long getMatchingMetaAlertCount(String fieldName, String fieldValue) throws IOException, InterruptedException {
    long cnt = 0;
    for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t, Thread.sleep(SLEEP_MS)) {
      List<Map<String, Object>> docs = es.getAllIndexedDocs(METAALERTS_INDEX, MetaAlertDao.METAALERT_DOC);
      cnt = docs
          .stream()
          .filter(d -> {
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

  protected void findUpdatedDoc(Map<String, Object> message0, String guid, String sensorType)
      throws InterruptedException, IOException, OriginalNotFoundException {
    for (int t = 0; t < MAX_RETRIES; ++t, Thread.sleep(SLEEP_MS)) {
      Document doc = metaDao.getLatest(guid, sensorType);
      if (doc != null && compareDocs(message0, doc.getDocument())) {
        return;
      }
    }
    throw new OriginalNotFoundException("Count not find " + guid + " after " + MAX_RETRIES + " tries");
  }

  private boolean compareDocs(Map<String, Object> expected, Map<String, Object> actual) {
    if (expected.size() != actual.size()) {
      return false;
    }
    for(String key: expected.keySet()) {
      if (ALERT_FIELD.equals(key)) {
        List<Map<String, Object>> expectedAlerts = (List<Map<String, Object>>) expected.get(MetaAlertDao.ALERT_FIELD);
        ArrayList<Map<String, Object>> actualAlerts = (ArrayList<Map<String, Object>>) actual.get(MetaAlertDao.ALERT_FIELD);
        if (!expectedAlerts.containsAll(actualAlerts) || !actualAlerts.containsAll(expectedAlerts)) {
          return false;
        }
      } else if (!expected.get(key).equals(actual.get(key))){
        return false;
      }
    }
    return true;
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
      alerts.put(ElasticsearchMetaAlertDao.THREAT_TRIAGE_FIELD, i);
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
