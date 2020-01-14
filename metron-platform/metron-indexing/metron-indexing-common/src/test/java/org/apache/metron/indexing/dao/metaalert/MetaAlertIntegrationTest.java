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

package org.apache.metron.indexing.dao.metaalert;

import static org.apache.metron.indexing.dao.metaalert.MetaAlertConstants.ALERT_FIELD;
import static org.apache.metron.indexing.dao.metaalert.MetaAlertConstants.METAALERT_FIELD;
import static org.apache.metron.indexing.dao.metaalert.MetaAlertConstants.METAALERT_TYPE;
import static org.apache.metron.indexing.dao.metaalert.MetaAlertConstants.STATUS_FIELD;
import static org.apache.metron.indexing.dao.metaalert.MetaAlertConstants.THREAT_FIELD_DEFAULT;
import static org.apache.metron.integration.utils.TestUtils.assertEventually;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


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
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
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
import org.apache.metron.indexing.dao.search.SortOrder;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.OriginalNotFoundException;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

public abstract class MetaAlertIntegrationTest {

  private static final String META_INDEX_FLAG = "%META_INDEX%";
  // To change back after testing
  protected static int MAX_RETRIES = 10;
  protected static final int SLEEP_MS = 500;
  protected static final String SENSOR_NAME = "test";

  protected static final String NEW_FIELD = "new-field";
  protected static final String NAME_FIELD = "name";
  protected static final String DATE_FORMAT = "yyyy.MM.dd.HH";

  // Separate the raw indices from the query indices. ES for example, modifies the indices to
  // have a separator
  protected ArrayList<String> allIndices = new ArrayList<String>() {
    {
      add(getTestIndexName());
      add(getMetaAlertIndex());
    }
  };

  protected ArrayList<String> queryIndices = allIndices;

  protected static MetaAlertDao metaDao;

  /**
   {
   "guid": "meta_alert",
   "index": "%META_INDEX%",
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
   "index": "%META_INDEX%",
   "patch": [
   {
   "op": "add",
   "path": "/name",
   "value": "New Meta Alert"
   },
   {
   "op": "add",
   "path": "/metron_alert",
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
   "index": "%META_INDEX%",
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
    addRecords(alerts, getTestIndexFullName(), SENSOR_NAME);

    // Load metaAlerts
    List<Map<String, Object>> metaAlerts = buildMetaAlerts(12, MetaAlertStatus.ACTIVE,
        Optional.of(Collections.singletonList(alerts.get(0))));
    metaAlerts.add(buildMetaAlert("meta_active_12", MetaAlertStatus.ACTIVE,
        Optional.of(Arrays.asList(alerts.get(0), alerts.get(2)))));
    metaAlerts.add(buildMetaAlert("meta_inactive", MetaAlertStatus.INACTIVE,
        Optional.of(Arrays.asList(alerts.get(0), alerts.get(2)))));
    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    addRecords(metaAlerts, getMetaAlertIndex(), METAALERT_TYPE);

    // Verify load was successful
    List<GetRequest> createdDocs = metaAlerts.stream().map(metaAlert ->
        new GetRequest((String) metaAlert.get(Constants.GUID), METAALERT_TYPE))
        .collect(Collectors.toList());
    createdDocs.addAll(alerts.stream().map(alert ->
        new GetRequest((String) alert.get(Constants.GUID), SENSOR_NAME))
        .collect(Collectors.toList()));
    findCreatedDocs(createdDocs);

    {
      // Verify searches successfully return more than 10 results
      SearchResponse searchResponse0 = metaDao.getAllMetaAlertsForAlert("message_0");
      List<SearchResult> searchResults0 = searchResponse0.getResults();
      assertEquals(13, searchResults0.size());
      Set<Map<String, Object>> resultSet = new HashSet<>();
      Iterables.addAll(resultSet, Iterables.transform(searchResults0, r -> r.getSource()));
      StringBuffer reason = new StringBuffer("Unable to find " + metaAlerts.get(0) + "\n");
      reason.append(Joiner.on("\n").join(resultSet));
      assertTrue(resultSet.contains(metaAlerts.get(0)), reason.toString());

      // Verify no meta alerts are returned because message_1 was not added to any
      SearchResponse searchResponse1 = metaDao.getAllMetaAlertsForAlert("message_1");
      List<SearchResult> searchResults1 = searchResponse1.getResults();
      assertEquals(0, searchResults1.size());

      // Verify only the meta alert message_2 was added to is returned
      SearchResponse searchResponse2 = metaDao.getAllMetaAlertsForAlert("message_2");
      List<SearchResult> searchResults2 = searchResponse2.getResults();
      assertEquals(1, searchResults2.size());
      assertEquals(metaAlerts.get(12), searchResults2.get(0).getSource());
    }
  }

  @Test
  public void shouldSortByThreatTriageScore() throws Exception {
    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(2);
    alerts.get(0).put(METAALERT_FIELD, "meta_active_0");
    addRecords(alerts, getTestIndexFullName(), SENSOR_NAME);

    // Load metaAlerts
    List<Map<String, Object>> metaAlerts = buildMetaAlerts(1, MetaAlertStatus.ACTIVE,
        Optional.of(Collections.singletonList(alerts.get(0))));
    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    addRecords(metaAlerts, getMetaAlertIndex(), METAALERT_TYPE);

    // Verify load was successful
    List<GetRequest> createdDocs = metaAlerts.stream().map(metaAlert ->
        new GetRequest((String) metaAlert.get(Constants.GUID), METAALERT_TYPE))
        .collect(Collectors.toList());
    createdDocs.addAll(alerts.stream().map(alert ->
        new GetRequest((String) alert.get(Constants.GUID), SENSOR_NAME))
        .collect(Collectors.toList()));
    findCreatedDocs(createdDocs);

    // Test descending
    SortField sf = new SortField();
    sf.setField(getThreatTriageField());
    sf.setSortOrder(SortOrder.DESC.getSortOrder());
    SearchRequest sr = new SearchRequest();
    sr.setQuery("*:*");
    sr.setSize(5);
    sr.setIndices(Arrays.asList(getTestIndexName(), METAALERT_TYPE));
    sr.setSort(Collections.singletonList(sf));

    SearchResponse result = metaDao.search(sr);
    List<SearchResult> results = result.getResults();
    assertEquals(2, results.size());
    assertEquals("meta_active_0", results.get((0)).getSource().get(Constants.GUID));
    assertEquals("message_1", results.get((1)).getSource().get(Constants.GUID));

    // Test ascending
    SortField sfAsc = new SortField();
    sfAsc.setField(getThreatTriageField());
    sfAsc.setSortOrder(SortOrder.ASC.getSortOrder());
    SearchRequest srAsc = new SearchRequest();
    srAsc.setQuery("*:*");
    srAsc.setSize(2);
    srAsc.setIndices(Arrays.asList(getTestIndexName(), METAALERT_TYPE));
    srAsc.setSort(Collections.singletonList(sfAsc));
    result = metaDao.search(srAsc);
    results = result.getResults();
    assertEquals("message_1", results.get((0)).getSource().get(Constants.GUID));
    assertEquals("meta_active_0", results.get((1)).getSource().get(Constants.GUID));
    assertEquals(2, results.size());
  }

  @Test
  public void getAllMetaAlertsForAlertShouldThrowExceptionForEmptyGuid() {
    InvalidSearchException ise = assertThrows(InvalidSearchException.class, () -> metaDao.getAllMetaAlertsForAlert(""));
    assertEquals("Guid cannot be empty", ise.getMessage());
  }

  @Test
  public void shouldCreateMetaAlert() throws Exception {
    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(3);
    addRecords(alerts, getTestIndexFullName(), SENSOR_NAME);

    // Verify load was successful
    findCreatedDocs(Arrays.asList(
        new GetRequest("message_0", SENSOR_NAME),
        new GetRequest("message_1", SENSOR_NAME),
        new GetRequest("message_2", SENSOR_NAME)));

    {
      MetaAlertCreateRequest metaAlertCreateRequest = new MetaAlertCreateRequest() {{
        setAlerts(new ArrayList<GetRequest>() {{
          add(new GetRequest("message_1", SENSOR_NAME));
          add(new GetRequest("message_2", SENSOR_NAME, getTestIndexFullName()));
        }});
        setGroups(Collections.singletonList("group"));
      }};

      Document actualMetaAlert = metaDao
          .createMetaAlert(metaAlertCreateRequest);

      // Build expected metaAlert after alerts are added
      Map<String, Object> expectedMetaAlert = new HashMap<>();

      expectedMetaAlert.put(Constants.GUID, actualMetaAlert.getGuid());
      expectedMetaAlert.put(getSourceTypeField(), METAALERT_TYPE);
      expectedMetaAlert.put(STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString());
      // Verify the proper alerts were added
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> metaAlertAlerts = new ArrayList<>();
      // Alert 0 is already in the metaalert. Add alerts 1 and 2.
      Map<String, Object> expectedAlert1 = alerts.get(1);
      expectedAlert1.put(METAALERT_FIELD, Collections.singletonList(actualMetaAlert.getGuid()));
      metaAlertAlerts.add(expectedAlert1);
      Map<String, Object> expectedAlert2 = alerts.get(2);
      expectedAlert2.put(METAALERT_FIELD, Collections.singletonList(actualMetaAlert.getGuid()));
      metaAlertAlerts.add(expectedAlert2);
      expectedMetaAlert.put(ALERT_FIELD, metaAlertAlerts);

      // Verify the counts were properly updated
      expectedMetaAlert.put("average", 1.5d);
      expectedMetaAlert.put("min", 1.0d);
      expectedMetaAlert.put("median", 1.5d);
      expectedMetaAlert.put("max", 2.0d);
      expectedMetaAlert.put("count", 2);
      expectedMetaAlert.put("sum", 3.0d);
      expectedMetaAlert.put(getThreatTriageField(), 3.0d);
      {
        // Verify metaAlert was created
        assertDocumentEquals(expectedMetaAlert, actualMetaAlert.getDocument());
        findCreatedDoc(actualMetaAlert.getGuid(), METAALERT_TYPE);
      }
      {
        // Verify alert 0 was not updated with metaalert field
        Document alert = metaDao.getLatest("message_0", SENSOR_NAME);
        assertEquals(4, alert.getDocument().size());
        assertNull(alert.getDocument().get(METAALERT_FIELD));
      }
      {
        // Verify alert 1 was properly updated with metaalert field
        Map<String, Object> expectedAlert = new HashMap<>(alerts.get(1));
        expectedAlert
            .put(METAALERT_FIELD, Collections.singletonList(actualMetaAlert.getGuid()));
        findUpdatedDoc(expectedAlert, "message_1", SENSOR_NAME);
      }
      {
        // Verify alert 2 was properly updated with metaalert field
        Map<String, Object> expectedAlert = new HashMap<>(alerts.get(2));
        expectedAlert
            .put(METAALERT_FIELD, Collections.singletonList(actualMetaAlert.getGuid()));
        findUpdatedDoc(expectedAlert, "message_2", SENSOR_NAME);
      }
    }
  }

  @Test
  public void shouldAddAlertsToMetaAlert() throws Exception {
    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(4);
    alerts.get(0).put(METAALERT_FIELD, Collections.singletonList("meta_alert"));
    addRecords(alerts, getTestIndexFullName(), SENSOR_NAME);

    // Load metaAlert
    Map<String, Object> metaAlert = buildMetaAlert("meta_alert", MetaAlertStatus.ACTIVE,
        Optional.of(Collections.singletonList(alerts.get(0))));
    addRecords(Collections.singletonList(metaAlert), getMetaAlertIndex(), METAALERT_TYPE);

    // Verify load was successful
    findCreatedDocs(Arrays.asList(
        new GetRequest("message_0", SENSOR_NAME),
        new GetRequest("message_1", SENSOR_NAME),
        new GetRequest("message_2", SENSOR_NAME),
        new GetRequest("message_3", SENSOR_NAME),
        new GetRequest("meta_alert", METAALERT_TYPE)
    ));

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
    expectedMetaAlert.put(getThreatTriageField(), 3.0d);

    {
      // Verify alerts were successfully added to the meta alert
      Document actualMetaAlert = metaDao.addAlertsToMetaAlert("meta_alert", Arrays
              .asList(new GetRequest("message_1", SENSOR_NAME),
                      new GetRequest("message_2", SENSOR_NAME)));
      assertDocumentEquals(expectedMetaAlert, actualMetaAlert.getDocument());
      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);
    }

    {
      // Verify False when alerts are already in a meta alert and no new alerts are added
      Document actualMetaAlert = metaDao.addAlertsToMetaAlert("meta_alert", Arrays
              .asList(new GetRequest("message_0", SENSOR_NAME),
                      new GetRequest("message_1", SENSOR_NAME)));
      assertDocumentEquals(expectedMetaAlert, actualMetaAlert.getDocument());
      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);
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
      expectedMetaAlert.put(getThreatTriageField(), 6.0d);

      Document actualMetaAlert = metaDao.addAlertsToMetaAlert("meta_alert", Arrays
              .asList(new GetRequest("message_2", SENSOR_NAME),
                      new GetRequest("message_3", SENSOR_NAME)));
      assertDocumentEquals(expectedMetaAlert, actualMetaAlert.getDocument());
      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);
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
    addRecords(alerts, getTestIndexFullName(), SENSOR_NAME);

    // Load metaAlert
    Map<String, Object> metaAlert = buildMetaAlert("meta_alert", MetaAlertStatus.ACTIVE,
        Optional.of(Arrays.asList(alerts.get(0), alerts.get(1), alerts.get(2), alerts.get(3))));
    addRecords(Collections.singletonList(metaAlert), getMetaAlertIndex(), METAALERT_TYPE);

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
    expectedMetaAlert.put(getThreatTriageField(), 5.0d);

    {
      // Verify a list of alerts are removed from a meta alert
      Document actualMetaAlert = metaDao.removeAlertsFromMetaAlert("meta_alert", Arrays
              .asList(new GetRequest("message_0", SENSOR_NAME),
                      new GetRequest("message_1", SENSOR_NAME)));
      assertDocumentEquals(expectedMetaAlert, actualMetaAlert.getDocument());
      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);
    }

    {
      // Verify False when alerts are not present in a meta alert and no alerts are removed
      Document actualMetaAlert = metaDao.removeAlertsFromMetaAlert("meta_alert", Arrays
              .asList(new GetRequest("message_0", SENSOR_NAME),
                      new GetRequest("message_1", SENSOR_NAME)));
      assertDocumentEquals(expectedMetaAlert, actualMetaAlert.getDocument());
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
      expectedMetaAlert.put(getThreatTriageField(), 3.0d);

      Document actualMetaAlert = metaDao.removeAlertsFromMetaAlert("meta_alert", Arrays
              .asList(new GetRequest("message_0", SENSOR_NAME),
                      new GetRequest("message_2", SENSOR_NAME)));
      assertDocumentEquals(expectedMetaAlert, actualMetaAlert.getDocument());
      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);
    }

    {
      // Verify all alerts are removed from a metaAlert
      metaAlertAlerts = new ArrayList<>(
          (List<Map<String, Object>>) expectedMetaAlert.get(ALERT_FIELD));
      metaAlertAlerts.remove(0);
      if (isEmptyMetaAlertList()) {
        expectedMetaAlert.put(ALERT_FIELD, metaAlertAlerts);
      } else {
        expectedMetaAlert.remove(ALERT_FIELD);
      }

      expectedMetaAlert.put("average", 0.0d);
      expectedMetaAlert.put("count", 0);
      expectedMetaAlert.put("sum", 0.0d);
      expectedMetaAlert.put(getThreatTriageField(), 0.0d);

      // Handle the cases with non-finite Double values on a per store basis
      if (isFiniteDoubleOnly()) {
        expectedMetaAlert.put("min", String.valueOf(Double.POSITIVE_INFINITY));
        expectedMetaAlert.put("median", String.valueOf(Double.NaN));
        expectedMetaAlert.put("max", String.valueOf(Double.NEGATIVE_INFINITY));
      } else {
        expectedMetaAlert.put("min", Double.POSITIVE_INFINITY);
        expectedMetaAlert.put("median", Double.NaN);
        expectedMetaAlert.put("max", Double.NEGATIVE_INFINITY);
      }

      // Verify removing alerts cannot result in an empty meta alert
      IllegalStateException ise = assertThrows(IllegalStateException.class, () -> metaDao.removeAlertsFromMetaAlert("meta_alert",
          Collections.singletonList(new GetRequest("message_3", SENSOR_NAME))));
      assertEquals("Removing these alerts will result in an empty meta alert.  Empty meta alerts are not allowed.",
          ise.getMessage());
    }
  }

  @Test
  public void addRemoveAlertsShouldThrowExceptionForInactiveMetaAlert() throws Exception {
    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(2);
    alerts.get(0).put(METAALERT_FIELD, Collections.singletonList("meta_alert"));
    addRecords(alerts, getTestIndexFullName(), SENSOR_NAME);

    // Load metaAlert
    Map<String, Object> metaAlert = buildMetaAlert("meta_alert", MetaAlertStatus.INACTIVE,
        Optional.of(Collections.singletonList(alerts.get(0))));
    addRecords(Collections.singletonList(metaAlert), getMetaAlertIndex(), METAALERT_TYPE);

    // Verify load was successful
    findCreatedDocs(Arrays.asList(
        new GetRequest("message_0", SENSOR_NAME),
        new GetRequest("message_1", SENSOR_NAME),
        new GetRequest("meta_alert", METAALERT_TYPE)));

    // Verify alerts cannot be added to an INACTIVE meta alert
    IllegalStateException ise = assertThrows(IllegalStateException.class, () -> metaDao.addAlertsToMetaAlert("meta_alert",
        Collections.singletonList(new GetRequest("message_1", SENSOR_NAME))));
    assertEquals("Adding alerts to an INACTIVE meta alert is not allowed", ise.getMessage());

    // Verify alerts cannot be removed from an INACTIVE meta alert
    ise = assertThrows(IllegalStateException.class, () -> metaDao.removeAlertsFromMetaAlert("meta_alert",
        Collections.singletonList(new GetRequest("message_0", SENSOR_NAME))));
    assertEquals("Removing alerts from an INACTIVE meta alert is not allowed", ise.getMessage());
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
    addRecords(alerts, getTestIndexFullName(), SENSOR_NAME);

    // Load metaAlerts
    Map<String, Object> metaAlert = buildMetaAlert("meta_alert", MetaAlertStatus.ACTIVE,
        Optional.of(childAlerts));
    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    addRecords(Collections.singletonList(metaAlert), getMetaAlertIndex(),
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
      Map<String, Object> expectedMetaAlert = new HashMap<>(metaAlert);
      expectedMetaAlert.put(STATUS_FIELD, MetaAlertStatus.INACTIVE.getStatusString());

      Document actualMetaAlert = metaDao.updateMetaAlertStatus("meta_alert", MetaAlertStatus.INACTIVE);
      assertEquals(expectedMetaAlert, actualMetaAlert.getDocument());
      findUpdatedDoc(expectedMetaAlert, "meta_alert", METAALERT_TYPE);

      for (int i = 0; i < numChildAlerts; ++i) {
        Map<String, Object> expectedAlert = new HashMap<>(childAlerts.get(i));
        setEmptiedMetaAlertField(expectedAlert);
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
      Map<String, Object> expectedMetaAlert = new HashMap<>(metaAlert);
      expectedMetaAlert.put(STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString());

      Document actualMetaAlert = metaDao.updateMetaAlertStatus("meta_alert", MetaAlertStatus.ACTIVE);
      assertEquals(expectedMetaAlert, actualMetaAlert.getDocument());
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
    {
      {
        // Verify status changed to current status has no effect
        Map<String, Object> expectedMetaAlert = new HashMap<>(metaAlert);
        expectedMetaAlert.put(STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString());

        Document actualMetaAlert = metaDao.updateMetaAlertStatus("meta_alert", MetaAlertStatus.ACTIVE);
        assertEquals(expectedMetaAlert, actualMetaAlert.getDocument());
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
    addRecords(Arrays.asList(activeMetaAlert, inactiveMetaAlert), getMetaAlertIndex(),
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
    assertEquals(1, searchResponse.getTotal());
    assertEquals(MetaAlertStatus.ACTIVE.getStatusString(),
        searchResponse.getResults().get(0).getSource().get(STATUS_FIELD));
  }

  @Test
  public void shouldSortMetaAlertsByAlertStatus() throws Exception {
    final String guid = "meta_alert";
    setupTypings();

    // should be able to sort meta-alert search results by 'alert_status'
    SortField sortField = new SortField();
    sortField.setField("alert_status");
    sortField.setSortOrder("asc");

    // when no meta-alerts exist, it should work
    assertEquals(0, searchForSortedMetaAlerts(sortField).getTotal());

    // when meta-alert just created, it should work
    createMetaAlert(guid);
    assertEquals(1, searchForSortedMetaAlerts(sortField).getTotal());

    // when meta-alert 'esclated', it should work
    escalateMetaAlert(guid);
    assertEquals(1, searchForSortedMetaAlerts(sortField).getTotal());
  }

  private Map<String, Object> createMetaAlert(String guid) throws Exception {
    // create and index 2 normal alerts
    List<Map<String, Object>> alerts = buildAlerts(2);
    alerts.get(0).put(METAALERT_FIELD, Collections.singletonList(guid));
    alerts.get(1).put(METAALERT_FIELD, Collections.singletonList(guid));
    addRecords(alerts, getTestIndexFullName(), SENSOR_NAME);

    // create and index a meta-alert
    Map<String, Object> metaAlert = buildMetaAlert(guid, MetaAlertStatus.ACTIVE, Optional.of(alerts));
    addRecords(Collections.singletonList(metaAlert), getMetaAlertIndex(), METAALERT_TYPE);

    // ensure the test alerts were loaded
    findCreatedDocs(Arrays.asList(
            new GetRequest("message_0", SENSOR_NAME),
            new GetRequest("message_1", SENSOR_NAME),
            new GetRequest("meta_alert", METAALERT_TYPE)));
    return metaAlert;
  }

  private void escalateMetaAlert(String guid) throws Exception {
    // create the patch that 'escalates' the meta-alert
    Map<String, Object> patch = new HashMap<>();
    patch.put("op", "add");
    patch.put("path", "/alert_status");
    patch.put("value", "escalate");

    // 'escalate' the meta-alert
    PatchRequest patchRequest = new PatchRequest();
    patchRequest.setGuid(guid);
    patchRequest.setIndex(getMetaAlertIndex());
    patchRequest.setSensorType(METAALERT_TYPE);
    patchRequest.setPatch(Collections.singletonList(patch));
    metaDao.patch(metaDao, patchRequest, Optional.of(System.currentTimeMillis()));

    // ensure the alert status was changed to 'escalate'
    assertEventually(() -> {
      Document updated = metaDao.getLatest(guid, METAALERT_TYPE);
      assertEquals("escalate", updated.getDocument().get("alert_status"));
    });
  }

  private SearchResponse searchForSortedMetaAlerts(SortField sortBy) throws InvalidSearchException {
    SearchRequest searchRequest = new SearchRequest();
    searchRequest.setFrom(0);
    searchRequest.setSize(10);
    searchRequest.setIndices(Arrays.asList(getTestIndexName(), METAALERT_TYPE));
    searchRequest.setQuery("*:*");
    searchRequest.setSort(Collections.singletonList(sortBy));
    return metaDao.search(searchRequest);
  }

  @Test
  public void shouldHidesAlertsOnGroup() throws Exception {
    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(2);
    alerts.get(0).put(METAALERT_FIELD, Collections.singletonList("meta_active"));
    alerts.get(0).put("ip_src_addr", "192.168.1.1");
    alerts.get(0).put("score", 1);
    alerts.get(1).put("ip_src_addr", "192.168.1.1");
    alerts.get(1).put("score", 10);
    addRecords(alerts, getTestIndexFullName(), SENSOR_NAME);

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
        setIndices(queryIndices);
        setScoreField("score");
        setGroups(groupList);
      }
    });

    // Should only return the standalone alert in the group
    GroupResult result = groupResponse.getGroupResults().get(0);
    assertEquals(1, result.getTotal());
    assertEquals("192.168.1.1", result.getKey());
    // No delta, since no ops happen
    assertEquals(10.0d, result.getScore(), 0.0d);
  }

  // This test is important enough that everyone should implement it, but is pretty specific to
  // implementation
  // The function is provided for any store specific translation of queryIndice in parameterized tests, e.g. ES appends "_index"
  // and "*"
  @Test
  public abstract void shouldSearchByNestedAlert() throws Exception;

  /**
   * If a meta-alert is active, any updates to alerts associated with a meta-alert
   * should be reflected in both the original alert and the copy contained within
   * the meta-alert.
   */
  @Test
  public void shouldUpdateMetaAlertOnAlertUpdate() throws Exception {
    final String expectedFieldValue = "metron";
    {
      // create 2 'regular' alerts that will be associated with meta-alerts
      List<Map<String, Object>> alerts = buildAlerts(2);
      alerts.get(0).put(METAALERT_FIELD, Arrays.asList("meta_active", "meta_inactive"));
      addRecords(alerts, getTestIndexFullName(), SENSOR_NAME);

      // the active meta-alert should be updated when an associated alert is updated
      Map<String, Object> activeMetaAlert = buildMetaAlert("meta_active", MetaAlertStatus.ACTIVE,
              Optional.of(Collections.singletonList(alerts.get(0))));

      // the inactive meta-alert should NOT be updated when an associated alert is updated
      Map<String, Object> inactiveMetaAlert = buildMetaAlert("meta_inactive", MetaAlertStatus.INACTIVE,
              Optional.of(Collections.singletonList(alerts.get(0))));

      // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
      addRecords(Arrays.asList(activeMetaAlert, inactiveMetaAlert), getMetaAlertIndex(), METAALERT_TYPE);

      // Verify load was successful
      findCreatedDocs(Arrays.asList(
              new GetRequest("message_0", SENSOR_NAME),
              new GetRequest("message_1", SENSOR_NAME),
              new GetRequest("meta_active", METAALERT_TYPE),
              new GetRequest("meta_inactive", METAALERT_TYPE)));
    }
    {
      // modify the 'normal' alert by adding a field
      Document message0 = metaDao.getLatest("message_0", SENSOR_NAME);
      message0.getDocument().put(NEW_FIELD, expectedFieldValue);
      message0.getDocument().put(THREAT_FIELD_DEFAULT, 10.0d);
      metaDao.update(message0, Optional.of(getTestIndexFullName()));
    }

    // ensure the original 'normal' alert was itself updated
    assertEventually(() -> {
      Document message0 = metaDao.getLatest("message_0", SENSOR_NAME);
      assertNotNull(message0);
      assertEquals(expectedFieldValue, message0.getDocument().get(NEW_FIELD));
    });

    // the 'active' meta-alert, which contains a copy of the updated alert should also be updated
    assertEventually(() -> {
      Document active = metaDao.getLatest("meta_active", METAALERT_TYPE);
      Object value = active.getDocument().get(ALERT_FIELD);
      List<Map<String, Object>> children = List.class.cast(value);
      assertNotNull(children);
      assertEquals(1, children.size());
      assertEquals(expectedFieldValue, children.get(0).get(NEW_FIELD));
    });

    // the 'inactive' meta-alert, which contains a copy of the updated alert should NOT be updated
    assertEventually(() -> {
      Document inactive = metaDao.getLatest("meta_inactive", METAALERT_TYPE);
      Object value = inactive.getDocument().get(ALERT_FIELD);
      List<Map<String, Object>> children = List.class.cast(value);
      assertNotNull(children);
      assertEquals(1, children.size());
      assertFalse(children.get(0).containsKey(NEW_FIELD));
    });
  }

  @Test
  public void shouldThrowExceptionOnMetaAlertUpdate() throws Exception {
    Document metaAlert = new Document(new HashMap<>(), "meta_alert", METAALERT_TYPE, 0L);
    // Verify a meta alert cannot be updated in the meta alert dao
    UnsupportedOperationException uoe = assertThrows(UnsupportedOperationException.class,
        () -> metaDao.update(metaAlert, Optional.empty()));
    assertEquals("Meta alerts cannot be directly updated", uoe.getMessage());
  }

  @Test
  public void shouldPatchMetaAlertFields() throws Exception {
    // Load alerts
    List<Map<String, Object>> alerts = buildAlerts(2);
    alerts.get(0).put(METAALERT_FIELD, Collections.singletonList("meta_active"));
    alerts.get(1).put(METAALERT_FIELD, Collections.singletonList("meta_active"));
    addRecords(alerts, getTestIndexFullName(), SENSOR_NAME);

    // Put the nested type into the test index, so that it'll match appropriately
    setupTypings();

    // Load metaAlerts
    Map<String, Object> metaAlert = buildMetaAlert("meta_alert", MetaAlertStatus.ACTIVE,
            Optional.of(Arrays.asList(alerts.get(0), alerts.get(1))));
    // We pass MetaAlertDao.METAALERT_TYPE, because the "_doc" gets appended automatically.
    addRecords(Collections.singletonList(metaAlert), getMetaAlertIndex(), METAALERT_TYPE);

    // ensure the test data was loaded
    findCreatedDocs(Arrays.asList(
            new GetRequest("message_0", SENSOR_NAME),
            new GetRequest("message_1", SENSOR_NAME),
            new GetRequest("meta_alert", METAALERT_TYPE)));

    // patch the name field
    String namePatch = namePatchRequest.replace(META_INDEX_FLAG, getMetaAlertIndex());
    PatchRequest patchRequest = JSONUtils.INSTANCE.load(namePatch, PatchRequest.class);
    metaDao.patch(metaDao, patchRequest, Optional.of(System.currentTimeMillis()));

    // ensure the alert was patched
    assertEventually(() -> {
      Document updated = metaDao.getLatest("meta_alert", METAALERT_TYPE);
      assertEquals("New Meta Alert", updated.getDocument().get(NAME_FIELD));
    });
  }

  @Test
  public void shouldThrowExceptionIfPatchAlertField() throws Exception {
    setupTypings();

    // add 2 alerts to an active meta-alert
    List<Map<String, Object>> alerts = buildAlerts(2);
    alerts.get(0).put(METAALERT_FIELD, Collections.singletonList("meta_active"));
    alerts.get(1).put(METAALERT_FIELD, Collections.singletonList("meta_active"));
    addRecords(alerts, getTestIndexFullName(), SENSOR_NAME);

    // create an active meta-alert
    Map<String, Object> metaAlert = buildMetaAlert("meta_alert", MetaAlertStatus.ACTIVE,
            Optional.of(Arrays.asList(alerts.get(0), alerts.get(1))));
    addRecords(Collections.singletonList(metaAlert), getMetaAlertIndex(), METAALERT_TYPE);

    // ensure the test data was loaded
    findCreatedDocs(Arrays.asList(
            new GetRequest("message_0", SENSOR_NAME),
            new GetRequest("message_1", SENSOR_NAME),
            new GetRequest("meta_alert", METAALERT_TYPE)));

    // attempt to patch the alert field
    String alertPatch = alertPatchRequest.replace(META_INDEX_FLAG, getMetaAlertIndex());
    PatchRequest patchRequest = JSONUtils.INSTANCE.load(alertPatch, PatchRequest.class);
    IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
        () -> metaDao.patch(metaDao, patchRequest, Optional.of(System.currentTimeMillis())));
    assertEquals("Meta alert patches are not allowed for /alert or /status paths.  "
            + "Please use the add/remove alert or update status functions instead.",
        iae.getMessage());

    // ensure the alert field was NOT changed
    assertEventually(() -> {
      Document updated = metaDao.getLatest("meta_alert", METAALERT_TYPE);
      assertEquals(metaAlert.get(ALERT_FIELD), updated.getDocument().get(ALERT_FIELD));
    });
  }

  @Test
  public void shouldThrowExceptionIfPatchStatusField() throws Exception {
    setupTypings();

    // add 2 alerts to an active meta-alert
    List<Map<String, Object>> alerts = buildAlerts(2);
    alerts.get(0).put(METAALERT_FIELD, Collections.singletonList("meta_active"));
    alerts.get(1).put(METAALERT_FIELD, Collections.singletonList("meta_active"));
    addRecords(alerts, getTestIndexFullName(), SENSOR_NAME);

    // create an active meta-alert
    Map<String, Object> metaAlert = buildMetaAlert("meta_alert", MetaAlertStatus.ACTIVE,
            Optional.of(Arrays.asList(alerts.get(0), alerts.get(1))));
    addRecords(Collections.singletonList(metaAlert), getMetaAlertIndex(), METAALERT_TYPE);

    // ensure the test data was loaded
    findCreatedDocs(Arrays.asList(
            new GetRequest("message_0", SENSOR_NAME),
            new GetRequest("message_1", SENSOR_NAME),
            new GetRequest("meta_alert", METAALERT_TYPE)));

    // Verify a patch to a status field should throw an exception
    String statusPatch = statusPatchRequest.replace(META_INDEX_FLAG, getMetaAlertIndex());
    PatchRequest patchRequest = JSONUtils.INSTANCE.load(statusPatch, PatchRequest.class);
    IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
        () -> metaDao.patch(metaDao, patchRequest, Optional.of(System.currentTimeMillis())));

    assertEquals("Meta alert patches are not allowed for /alert or /status paths.  "
            + "Please use the add/remove alert or update status functions instead.",
        iae.getMessage());

    // ensure the status field was NOT changed
    assertEventually(() -> {
      Document updated = metaDao.getLatest("meta_alert", METAALERT_TYPE);
      assertEquals(metaAlert.get(STATUS_FIELD), updated.getDocument().get(STATUS_FIELD));
    });
  }

  protected void findUpdatedDoc(Map<String, Object> message0, String guid, String sensorType)
      throws InterruptedException, IOException, OriginalNotFoundException {
    commit();
    for (int t = 0; t < MAX_RETRIES; ++t, Thread.sleep(SLEEP_MS)) {
      Document doc = metaDao.getLatest(guid, sensorType);
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

  @SuppressWarnings("unchecked")
  protected void assertDocumentEquals(Map<String, Object> expected, Map<String, Object> actual) {
    assertEquals(expected.get(Constants.GUID), actual.get(Constants.GUID));
    assertEquals(expected.get(getSourceTypeField()), actual.get(getSourceTypeField()));
    Double actualThreatTriageField = actual.get(getThreatTriageField()) instanceof Float ?
            ((Float) actual.get(getThreatTriageField())).doubleValue() : (Double) actual.get(getThreatTriageField());
    assertEquals(expected.get(getThreatTriageField()), actualThreatTriageField);

    List<Map<String, Object>> expectedAlerts = (List<Map<String, Object>>) expected.get(ALERT_FIELD);
    List<Map<String, Object>> actualAlerts = (List<Map<String, Object>>) actual.get(ALERT_FIELD);
    expectedAlerts.sort(Comparator.comparing(o -> ((String) o.get(Constants.GUID))));
    actualAlerts.sort(Comparator.comparing(o -> ((String) o.get(Constants.GUID))));
    assertEquals(expectedAlerts, actualAlerts);
    assertEquals(expected.get(STATUS_FIELD), actual.get(STATUS_FIELD));
    assertEquals(expected.get("average"), actual.get("average"));
    assertEquals(expected.get("min"), actual.get("min"));
    assertEquals(expected.get("median"), actual.get("median"));
    assertEquals(expected.get("max"), actual.get("max"));
    Integer actualCountField = actual.get("count") instanceof Long ? ((Long) actual.get("count")).intValue() :
            (Integer) actual.get("count");
    assertEquals(expected.get("count"), actualCountField);
    assertEquals(expected.get("sum"), actual.get("sum"));
  }

  protected List<Map<String, Object>> buildAlerts(int count) {
    List<Map<String, Object>> inputData = new ArrayList<>();
    for (int i = 0; i < count; ++i) {
      final String guid = "message_" + i;
      Map<String, Object> alerts = new HashMap<>();
      alerts.put(Constants.GUID, guid);
      alerts.put(getSourceTypeField(), SENSOR_NAME);
      alerts.put(THREAT_FIELD_DEFAULT, (double) i);
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
    metaAlert.put(getSourceTypeField(), METAALERT_TYPE);
    metaAlert.put(STATUS_FIELD, status.getStatusString());
    metaAlert.put(getThreatTriageField(), 100.0d);
    if (alerts.isPresent()) {
      List<Map<String, Object>> alertsList = alerts.get();
      metaAlert.put(ALERT_FIELD, alertsList);
    }
    return metaAlert;
  }

  protected abstract long getMatchingAlertCount(String fieldName, Object fieldValue)
      throws IOException, InterruptedException;

  protected abstract void addRecords(List<Map<String, Object>> inputData, String index,
      String docType) throws IOException, ParseException;

  protected abstract long getMatchingMetaAlertCount(String fieldName, String fieldValue)
      throws IOException, InterruptedException;

  protected abstract void setupTypings() throws IOException;

  // Get the base index name without any adjustments (e.g. without ES's "_index")
  protected abstract String getTestIndexName();

  // Get the full name of the test index.  E.g. Elasticsearch appends "_index"
  protected String getTestIndexFullName() {
    return getTestIndexName();
  }

  protected abstract String getMetaAlertIndex();

  protected abstract String getSourceTypeField();

  protected String getThreatTriageField() {
    return THREAT_FIELD_DEFAULT;
  }

  // Allow for impls to do any commit they need to do.
  protected void commit() throws IOException {
  }

  // Different stores can have different representations of empty metaalerts field.
  // E.g. Solr expects the field to not be present, ES expects it to be empty.
  protected abstract void setEmptiedMetaAlertField(Map<String, Object> docMap);

  // Different stores may choose to store non finite double values as Strings.
  // E.g. NaN may be a string, not a double value.
  protected abstract boolean isFiniteDoubleOnly();

  // Different stores may choose to return empty alerts lists differently.
  // E.g. It may be missing completely, or may be an empty list
  protected abstract boolean isEmptyMetaAlertList();
}
