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

package org.apache.metron.indexing.dao;

import static org.apache.metron.indexing.dao.MetaAlertDao.ALERT_FIELD;
import static org.apache.metron.indexing.dao.MetaAlertDao.METAALERT_FIELD;
import static org.apache.metron.indexing.dao.MetaAlertDao.METAALERT_TYPE;
import static org.apache.metron.indexing.dao.MetaAlertDao.STATUS_FIELD;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.collections4.MapUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
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
import org.junit.Assert;
import org.junit.Test;

public abstract class MetaAlertIntegrationTest {

  // To change back after testing
  protected static final int MAX_RETRIES = 1;
  protected static final int SLEEP_MS = 500;
  protected static final String SENSOR_NAME = "test";

  protected static final String NEW_FIELD = "new-field";
  protected static final String NAME_FIELD = "name";
  protected static final String DATE_FORMAT = "yyyy.MM.dd.HH";

  protected ArrayList<String> allIndices = new ArrayList<String>() {
    {
      add(getTestIndexName());
      add(METAALERT_TYPE);
    }
  };

  protected static MetaAlertDao metaDao;

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


  @Test
  public void shouldGetAllMetaAlertsForAlert() throws Exception {
    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(3);
    addRecords(alerts, getTestIndexName(), SENSOR_NAME);

    // Load metaAlerts
    List<Map<String, Object>> metaAlerts = buildMetaAlerts(12, MetaAlertStatus.ACTIVE,
        Optional.of(Collections.singletonList(alerts.get(0))));
    metaAlerts.add(buildMetaAlert("meta_active_12", MetaAlertStatus.ACTIVE,
        Optional.of(Arrays.asList(alerts.get(0), alerts.get(2)))));
    metaAlerts.add(buildMetaAlert("meta_inactive", MetaAlertStatus.INACTIVE,
        Optional.of(Arrays.asList(alerts.get(0), alerts.get(2)))));
    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    addRecords(metaAlerts, metaDao.getMetaAlertIndex(), METAALERT_TYPE);

    // Verify load was successful
    List<GetRequest> createdDocs = metaAlerts.stream().map(metaAlert ->
        new GetRequest((String) metaAlert.get(Constants.GUID), METAALERT_TYPE))
        .collect(Collectors.toList());
    createdDocs.addAll(alerts.stream().map(alert ->
        new GetRequest((String) alert.get(Constants.GUID), SENSOR_NAME))
        .collect(Collectors.toList()));
    findCreatedDocs(createdDocs);

    int previousPageSize = metaDao.getPageSize();
    metaDao.setPageSize(5);

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
    metaDao.setPageSize(previousPageSize);
  }

  @Test
  public void getAllMetaAlertsForAlertShouldThrowExceptionForEmptyGuid() throws Exception {
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
    addRecords(alerts, getTestIndexName(), SENSOR_NAME);

    // Verify load was successful
    findCreatedDocs(Arrays.asList(
        new GetRequest("message_0", SENSOR_NAME),
        new GetRequest("message_1", SENSOR_NAME),
        new GetRequest("message_2", SENSOR_NAME)));

    {
      MetaAlertCreateRequest metaAlertCreateRequest = new MetaAlertCreateRequest() {{
        setAlerts(new ArrayList<GetRequest>() {{
          add(new GetRequest("message_1", SENSOR_NAME));
          add(new GetRequest("message_2", SENSOR_NAME, getTestIndexName()));
        }});
        setGroups(Collections.singletonList("group"));
      }};
      MetaAlertCreateResponse metaAlertCreateResponse = metaDao
          .createMetaAlert(metaAlertCreateRequest);
      {
        // Verify metaAlert was created
        findCreatedDoc(metaAlertCreateResponse.getGuid(), METAALERT_TYPE);
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
        Object metaAlertField = alert.getDocument().get(METAALERT_FIELD);
        if (metaAlertField instanceof List) {
          Assert.assertEquals(1, ((List) alert.getDocument().get(METAALERT_FIELD)).size());
          Assert.assertEquals(metaAlertCreateResponse.getGuid(),
              ((List) alert.getDocument().get(METAALERT_FIELD)).get(0));
        } else {
          // Solr in particular will return this as a single string, rather than a list.
          // TODO confirm this is a Solr pecularity, rather than just a mistake somewhere else.
          Assert.assertTrue(metaAlertField instanceof String);
          Assert.assertEquals(metaAlertCreateResponse.getGuid(),
              alert.getDocument().get(METAALERT_FIELD));
        }
      }
      {
        // Verify alert 2 was properly updated with metaalert field
        Document alert = metaDao.getLatest("message_2", SENSOR_NAME);
        Assert.assertEquals(5, alert.getDocument().size());
        Object metaAlertField = alert.getDocument().get(METAALERT_FIELD);
        if (metaAlertField instanceof List) {
          Assert.assertEquals(1, ((List) alert.getDocument().get(METAALERT_FIELD)).size());
          Assert.assertEquals(metaAlertCreateResponse.getGuid(),
              ((List) alert.getDocument().get(METAALERT_FIELD)).get(0));
        } else {
          // Solr in particular will return this as a single string, rather than a list.
          Assert.assertTrue(metaAlertField instanceof String);
          Assert.assertEquals(metaAlertCreateResponse.getGuid(),
              alert.getDocument().get(METAALERT_FIELD));
        }
      }
    }
  }

  @Test
  public void shouldAddAlertsToMetaAlert() throws Exception {
    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(4);
    alerts.get(0).put(METAALERT_FIELD, Collections.singletonList("meta_alert"));
    addRecords(alerts, getTestIndexName(), SENSOR_NAME);

    // Load metaAlert
    Map<String, Object> metaAlert = buildMetaAlert("meta_alert", MetaAlertStatus.ACTIVE,
        Optional.of(Collections.singletonList(alerts.get(0))));
    addRecords(Collections.singletonList(metaAlert), metaDao.getMetaAlertIndex(), METAALERT_TYPE);

    // Verify load was successful
    findCreatedDocs(Arrays.asList(
        new GetRequest("message_0", SENSOR_NAME),
        new GetRequest("message_1", SENSOR_NAME),
        new GetRequest("message_2", SENSOR_NAME),
        new GetRequest("message_3", SENSOR_NAME),
        new GetRequest("meta_alert", metaDao.getMetAlertSensorName())));

    // Build expected metaAlert after alerts are added
    Map<String, Object> expectedMetaAlert = new HashMap<>(metaAlert);

    // Verify the proper alerts were added
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> metaAlertAlerts = new ArrayList<>(
        (List<Map<String, Object>>) expectedMetaAlert.get(ALERT_FIELD));
    // Alert 0 is already in the metaalert. Add alerts 1 and 2.
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
    expectedMetaAlert.put(metaDao.getThreatTriageField(), 3.0d);

    {
      // Verify alerts were successfully added to the meta alert
      Assert.assertTrue(metaDao.addAlertsToMetaAlert("meta_alert", Arrays
          .asList(new GetRequest("message_1", SENSOR_NAME),
              new GetRequest("message_2", SENSOR_NAME))));
      findUpdatedDoc(expectedMetaAlert, "meta_alert", metaDao.getMetAlertSensorName());
    }

    {
      // Verify False when alerts are already in a meta alert and no new alerts are added
      Assert.assertFalse(metaDao.addAlertsToMetaAlert("meta_alert", Arrays
          .asList(new GetRequest("message_0", SENSOR_NAME),
              new GetRequest("message_1", SENSOR_NAME))));
      findUpdatedDoc(expectedMetaAlert, "meta_alert", metaDao.getMetAlertSensorName());
    }

    {
      // Verify only 1 alert is added when a list of alerts only contains 1 alert that is not in the meta alert
      metaAlertAlerts = (List<Map<String, Object>>) expectedMetaAlert.get(ALERT_FIELD);
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
      expectedMetaAlert.put(metaDao.getThreatTriageField(), 6.0d);

      Assert.assertTrue(metaDao.addAlertsToMetaAlert("meta_alert", Arrays
          .asList(new GetRequest("message_2", SENSOR_NAME),
              new GetRequest("message_3", SENSOR_NAME))));
      findUpdatedDoc(expectedMetaAlert, "meta_alert", metaDao.getMetAlertSensorName());
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldRemoveAlertsFromMetaAlert() throws Exception {
    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(4);
    alerts.get(0).put(METAALERT_FIELD, Collections.singletonList("meta_alert"));
    alerts.get(1).put(METAALERT_FIELD, Collections.singletonList("meta_alert"));
    alerts.get(2).put(METAALERT_FIELD, Collections.singletonList("meta_alert"));
    alerts.get(3).put(METAALERT_FIELD, Collections.singletonList("meta_alert"));
    addRecords(alerts, getTestIndexName(), SENSOR_NAME);

    // Load metaAlert
    Map<String, Object> metaAlert = buildMetaAlert("meta_alert", MetaAlertStatus.ACTIVE,
        Optional.of(Arrays.asList(alerts.get(0), alerts.get(1), alerts.get(2), alerts.get(3))));
    addRecords(Collections.singletonList(metaAlert), metaDao.getMetaAlertIndex(), METAALERT_TYPE);

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
    List<Map<String, Object>> metaAlertAlerts = new ArrayList<>(
        (List<Map<String, Object>>) expectedMetaAlert.get(ALERT_FIELD));
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
    expectedMetaAlert.put(metaDao.getThreatTriageField(), 5.0d);

    {
      // Verify a list of alerts are removed from a meta alert
      Assert.assertTrue(metaDao.removeAlertsFromMetaAlert("meta_alert", Arrays
          .asList(new GetRequest("message_0", SENSOR_NAME),
              new GetRequest("message_1", SENSOR_NAME))));
      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);
    }

    {
      // Verify False when alerts are not present in a meta alert and no alerts are removed
      Assert.assertFalse(metaDao.removeAlertsFromMetaAlert("meta_alert", Arrays
          .asList(new GetRequest("message_0", SENSOR_NAME),
              new GetRequest("message_1", SENSOR_NAME))));
      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);
    }

    {
      // Verify only 1 alert is removed when a list of alerts only contains 1 alert that is in the meta alert
      metaAlertAlerts = new ArrayList<>(
          (List<Map<String, Object>>) expectedMetaAlert.get(ALERT_FIELD));
      metaAlertAlerts.remove(0);
      expectedMetaAlert.put(ALERT_FIELD, metaAlertAlerts);

      expectedMetaAlert.put("average", 3.0d);
      expectedMetaAlert.put("min", 3.0d);
      expectedMetaAlert.put("median", 3.0d);
      expectedMetaAlert.put("max", 3.0d);
      expectedMetaAlert.put("count", 1);
      expectedMetaAlert.put("sum", 3.0d);
      expectedMetaAlert.put(metaDao.getThreatTriageField(), 3.0d);

      Assert.assertTrue(metaDao.removeAlertsFromMetaAlert("meta_alert", Arrays
          .asList(new GetRequest("message_0", SENSOR_NAME),
              new GetRequest("message_2", SENSOR_NAME))));
      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);
    }

    {
      // Verify all alerts are removed from a metaAlert
      metaAlertAlerts = new ArrayList<>(
          (List<Map<String, Object>>) expectedMetaAlert.get(ALERT_FIELD));
      metaAlertAlerts.remove(0);
      expectedMetaAlert.put(ALERT_FIELD, metaAlertAlerts);

      expectedMetaAlert.put("average", 0.0d);
      // TODO handle +/- INF and NaN properly for Solr and ES
      expectedMetaAlert.put("min", Double.POSITIVE_INFINITY);
      expectedMetaAlert.put("median", Double.NaN);
      expectedMetaAlert.put("max", Double.NEGATIVE_INFINITY);
      expectedMetaAlert.put("count", 0);
      expectedMetaAlert.put("sum", 0.0d);
      expectedMetaAlert.put(metaDao.getThreatTriageField(), 0.0d);

      Assert.assertTrue(metaDao.removeAlertsFromMetaAlert("meta_alert",
          Collections.singletonList(new GetRequest("message_3", SENSOR_NAME))));
      findUpdatedDocTest(expectedMetaAlert, "meta_alert", METAALERT_TYPE);
    }

  }

  @Test
  public void addRemoveAlertsShouldThrowExceptionForInactiveMetaAlert() throws Exception {
    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(2);
    alerts.get(0).put(METAALERT_FIELD, Collections.singletonList("meta_alert"));
    addRecords(alerts, getTestIndexName(), SENSOR_NAME);

    // Load metaAlert
    Map<String, Object> metaAlert = buildMetaAlert("meta_alert", MetaAlertStatus.INACTIVE,
        Optional.of(Collections.singletonList(alerts.get(0))));
    addRecords(Collections.singletonList(metaAlert), metaDao.getMetaAlertIndex(), METAALERT_TYPE);

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
        Assert.assertEquals("Adding alerts to an INACTIVE meta alert is not allowed",
            ise.getMessage());
      }
    }

    {
      // Verify alerts cannot be removed from an INACTIVE meta alert
      try {
        metaDao.removeAlertsFromMetaAlert("meta_alert",
            Collections.singletonList(new GetRequest("message_0", SENSOR_NAME)));
        Assert.fail("Removing alerts from an inactive meta alert should throw an exception");
      } catch (IllegalStateException ise) {
        Assert.assertEquals("Removing alerts from an INACTIVE meta alert is not allowed",
            ise.getMessage());
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
    addRecords(alerts, getTestIndexName(), SENSOR_NAME);

    // Load metaAlerts
    Map<String, Object> metaAlert = buildMetaAlert("meta_alert", MetaAlertStatus.ACTIVE,
        Optional.of(childAlerts));
    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    addRecords(Collections.singletonList(metaAlert), metaDao.getMetaAlertIndex(),
        METAALERT_TYPE);

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
        // TODO expect this might need to handle empty metaalert representations.
        // e.g. ES expects empty list, Solr expects nothing
//        expectedAlert.put("metaalerts", new ArrayList<>());
        expectedAlert.remove("metaalerts");
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
      // Verify status changed to active and child alerts are updated
      Assert.assertTrue(metaDao.updateMetaAlertStatus("meta_alert", MetaAlertStatus.ACTIVE));

      Map<String, Object> expectedMetaAlert = new HashMap<>(metaAlert);
      expectedMetaAlert.put(STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString());

      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);

      System.out.println("*** SEARCHING FOR CHILD ALERTS");
      for (int i = 0; i < numChildAlerts; ++i) {
        Map<String, Object> expectedAlert = new HashMap<>(alerts.get(i));
        expectedAlert.put("metaalerts", Collections.singletonList("meta_alert"));
        System.out.println("*** LOOKING FOR ALERT: " + i);
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
  }

  @Test
  public void shouldSearchByStatus() throws Exception {
    // Load alert
    List<Map<String, Object>> alerts = buildAlerts(1);
    alerts.get(0).put(METAALERT_FIELD, Collections.singletonList("meta_active"));
    alerts.get(0).put("ip_src_addr", "192.168.1.1");
    alerts.get(0).put("ip_src_port", 8010);

    // Load metaAlerts
    Map<String, Object> activeMetaAlert = buildMetaAlert("meta_active", MetaAlertStatus.ACTIVE,
        Optional.of(Collections.singletonList(alerts.get(0))));
    Map<String, Object> inactiveMetaAlert = buildMetaAlert("meta_inactive",
        MetaAlertStatus.INACTIVE,
        Optional.empty());

    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    addRecords(Arrays.asList(activeMetaAlert, inactiveMetaAlert), metaDao.getMetaAlertIndex(),
        METAALERT_TYPE);

    // Verify load was successful
    findCreatedDocs(Arrays.asList(
        new GetRequest("meta_active", METAALERT_TYPE),
        new GetRequest("meta_inactive", METAALERT_TYPE)));

    SearchResponse searchResponse = metaDao.search(new SearchRequest() {
      {
        setQuery("*:*");
        setIndices(Collections.singletonList(METAALERT_TYPE));
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
        searchResponse.getResults().get(0).getSource().get(STATUS_FIELD));
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
    addRecords(Arrays.asList(activeMetaAlert, inactiveMetaAlert), metaDao.getMetaAlertIndex(),
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
            "(ip_src_addr:192.168.1.1 AND ip_src_port:8009) OR (alert.ip_src_addr:192.168.1.1 AND alert.ip_src_port:8009)");
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
    // Should not have results because nested alerts shouldn't be flattened
    Assert.assertEquals(0, searchResponse.getTotal());

    // Query against all indices. Only the single active meta alert should be returned.
    // The child alerts should be hidden.
    searchResponse = metaDao.search(new SearchRequest() {
      {
        setQuery(
            "(ip_src_addr:192.168.1.1 AND ip_src_port:8010)"
                + " OR (alert.ip_src_addr:192.168.1.1 AND alert.ip_src_port:8010)");
        setIndices(allIndices);
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
                + " OR (alert.ip_src_addr:192.168.1.3 AND alert.ip_src_port:8008)");
        setIndices(allIndices);
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
    addRecords(alerts, getTestIndexName(), SENSOR_NAME);

    // Put the nested type into the test index, so that it'll match appropriately
    setupTypings();

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
        setIndices(allIndices);
        setScoreField("score_field");
        setGroups(groupList);
      }
    });

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
    addRecords(alerts, getTestIndexName(), SENSOR_NAME);

    // Load metaAlerts
    Map<String, Object> activeMetaAlert = buildMetaAlert("meta_active", MetaAlertStatus.ACTIVE,
        Optional.of(Collections.singletonList(alerts.get(0))));
    Map<String, Object> inactiveMetaAlert = buildMetaAlert("meta_inactive",
        MetaAlertStatus.INACTIVE,
        Optional.of(Collections.singletonList(alerts.get(0))));
    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    addRecords(Arrays.asList(activeMetaAlert, inactiveMetaAlert), metaDao.getMetaAlertIndex(),
        METAALERT_TYPE);

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
          // TODO does this break ES?  This shouldn't be a string
          put(MetaAlertDao.THREAT_FIELD_DEFAULT, 10.0d);
        }
      };
      String guid = "" + message0.get(Constants.GUID);
      // TODO keep the index name here?
      metaDao
          .update(new Document(message0, guid, SENSOR_NAME, null), Optional.of(getTestIndexName()));

      {
        // Verify alerts are up-to-date
        findUpdatedDoc(message0, guid, SENSOR_NAME);
        long cnt = getMatchingAlertCount(NEW_FIELD, message0.get(NEW_FIELD));
        if (cnt == 0) {
          Assert.fail("Alert not updated!");
        }
      }

      {
        // Verify meta alerts are up-to-date
        long cnt = getMatchingMetaAlertCount(NEW_FIELD, "metron");
        if (cnt == 0) {
          Assert.fail("Active metaalert was not updated!");
        }
        if (cnt != 1) {
          Assert.fail("Metaalerts not updated correctly!");
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
        // Verify index is up-to-date
        findUpdatedDoc(message0, guid, SENSOR_NAME);
        long cnt = getMatchingAlertCount(NEW_FIELD, message0.get(NEW_FIELD));
        if (cnt == 0) {
          Assert.fail("Alert not updated!");
        }
      }
      {
        // Verify meta alerts are up-to-date
        long cnt = getMatchingMetaAlertCount(NEW_FIELD, "metron2");
        if (cnt == 0) {
          Assert.fail("Active metaalert was not updated!");
        }
        if (cnt != 1) {
          Assert.fail("Metaalerts not updated correctly!");
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
    addRecords(alerts, getTestIndexName(), SENSOR_NAME);

    // Put the nested type into the test index, so that it'll match appropriately
    setupTypings();

    // Load metaAlerts
    Map<String, Object> metaAlert = buildMetaAlert("meta_alert", MetaAlertStatus.ACTIVE,
        Optional.of(Arrays.asList(alerts.get(0), alerts.get(1))));
    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    addRecords(Collections.singletonList(metaAlert), metaDao.getMetaAlertIndex(), METAALERT_TYPE);

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
                + "Please use the add/remove alert or update status functions instead.",
            iae.getMessage());
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
                + "Please use the add/remove alert or update status functions instead.",
            iae.getMessage());
      }

      // Verify the metaAlert was not updated
      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);
    }
  }

  protected void findUpdatedDoc(Map<String, Object> message0, String guid, String sensorType)
      throws InterruptedException, IOException, OriginalNotFoundException {
    commit();
    System.out.println("GUID: " + guid);
    MapUtils.debugPrint(System.out, "expected", message0);
    for (int t = 0; t < MAX_RETRIES; ++t, Thread.sleep(SLEEP_MS)) {
      Document doc = metaDao.getLatest(guid, sensorType);
      MapUtils.debugPrint(System.out, "actual", doc.getDocument());
      // Change the underlying document alerts lists to sets to avoid ordering issues.
      convertAlertsFieldToSet(doc.getDocument());
      convertAlertsFieldToSet(message0);

      if (doc.getDocument() != null && message0.equals(doc.getDocument())) {
        convertAlertsFieldToList(doc.getDocument());
        convertAlertsFieldToList(message0);
        return;
      }
    }

    throw new OriginalNotFoundException(
        "Count not find " + guid + " after " + MAX_RETRIES + " tries");
  }


  // TODO kill this whole method when done debugging
  protected void findUpdatedDocTest(Map<String, Object> message0, String guid, String sensorType)
      throws InterruptedException, IOException, OriginalNotFoundException {
    commit();
    System.out.println("GUID: " + guid);
    MapUtils.debugPrint(System.out, "expected", message0);
    for (int t = 0; t < MAX_RETRIES; ++t, Thread.sleep(SLEEP_MS)) {
      Document doc = metaDao.getLatest(guid, sensorType);
      MapUtils.debugPrint(System.out, "actual", doc.getDocument());
      // Change the underlying document alerts lists to sets to avoid ordering issues.
      convertAlertsFieldToSet(doc.getDocument());
      convertAlertsFieldToSet(message0);

      if (doc.getDocument() != null && message0.equals(doc.getDocument())) {
        convertAlertsFieldToList(doc.getDocument());
        convertAlertsFieldToList(message0);
        return;
      }
    }

//    while(true) {
//
//    }
    throw new OriginalNotFoundException(
        "Count not find " + guid + " after " + MAX_RETRIES + " tries");
  }

  protected void convertAlertsFieldToSet(Map<String, Object> document) {
    if (document.get(ALERT_FIELD) instanceof List) {
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> message0AlertField = (List<Map<String, Object>>) document
          .get(ALERT_FIELD);
      Set<Map<String, Object>> message0AlertSet = new HashSet<>(message0AlertField);
      document.put(ALERT_FIELD, message0AlertSet);
    }
  }

  protected void convertAlertsFieldToList(Map<String, Object> document) {
    if (document.get(ALERT_FIELD) instanceof Set) {
      @SuppressWarnings("unchecked")
      Set<Map<String, Object>> message0AlertField = (Set<Map<String, Object>>) document
          .get(ALERT_FIELD);
      List<Map<String, Object>> message0AlertList = new ArrayList<>(message0AlertField);
      message0AlertList.sort(Comparator.comparing(o -> ((String) o.get(Constants.GUID))));
      document.put(ALERT_FIELD, message0AlertList);
    }
  }

  protected boolean findCreatedDoc(String guid, String sensorType)
      throws InterruptedException, IOException, OriginalNotFoundException {
    for (int t = 0; t < MAX_RETRIES; ++t, Thread.sleep(SLEEP_MS)) {
      Document doc = metaDao.getLatest(guid, sensorType);
      if (doc != null) {
        return true;
      }
    }
    throw new OriginalNotFoundException(
        "Count not find " + guid + " after " + MAX_RETRIES + "tries");
  }

  protected boolean findCreatedDocs(List<GetRequest> getRequests)
      throws InterruptedException, IOException, OriginalNotFoundException {
    for (int t = 0; t < MAX_RETRIES; ++t, Thread.sleep(SLEEP_MS)) {
      Iterable<Document> docs = metaDao.getAllLatest(getRequests);
      if (docs != null) {
        int docCount = 0;
        for (Document doc : docs) {
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
      alerts.put(metaDao.getSourceTypeField(), SENSOR_NAME);
      // TODO Why does this need to be cast for Solr? Does it need for ES?
      alerts.put(MetaAlertDao.THREAT_FIELD_DEFAULT, (double) i);
      alerts.put("timestamp", System.currentTimeMillis());
      inputData.add(alerts);
    }
    return inputData;
  }

  protected List<Map<String, Object>> buildMetaAlerts(int count, MetaAlertStatus status,
      Optional<List<Map<String, Object>>> alerts) {
    List<Map<String, Object>> inputData = new ArrayList<>();
    for (int i = 0; i < count; ++i) {
      final String guid = "meta_" + status.getStatusString() + "_" + i;
      inputData.add(buildMetaAlert(guid, status, alerts));
    }
    return inputData;
  }

  protected Map<String, Object> buildMetaAlert(String guid, MetaAlertStatus status,
      Optional<List<Map<String, Object>>> alerts) {
    Map<String, Object> metaAlert = new HashMap<>();
    metaAlert.put(Constants.GUID, guid);
    metaAlert.put(metaDao.getSourceTypeField(), METAALERT_TYPE);
    metaAlert.put(STATUS_FIELD, status.getStatusString());
    if (alerts.isPresent()) {
      List<Map<String, Object>> alertsList = alerts.get();
      metaAlert.put(ALERT_FIELD, alertsList);
    }
    return metaAlert;
  }

  protected abstract long getMatchingAlertCount(String fieldName, Object fieldValue)
      throws IOException, InterruptedException;

  protected abstract void addRecords(List<Map<String, Object>> inputData, String index,
      String docType) throws IOException;

  protected abstract long getMatchingMetaAlertCount(String fieldName, String fieldValue)
      throws IOException, InterruptedException;

  protected abstract void setupTypings();

  protected abstract String getTestIndexName();

  // Allow for impls to do any commit they need to do.
  protected void commit() throws IOException {
  }
}
