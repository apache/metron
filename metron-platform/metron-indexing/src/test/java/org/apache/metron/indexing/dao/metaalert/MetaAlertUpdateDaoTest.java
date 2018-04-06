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
import static org.apache.metron.indexing.dao.metaalert.MetaAlertStatus.ACTIVE;
import static org.apache.metron.indexing.dao.metaalert.MetaAlertStatus.INACTIVE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.math.util.MathUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MetaAlertUpdateDaoTest {

  private static final double EPS = 0.00001;
  @Mock
  IndexDao indexDao;

  TestMetaAlertUpdateDao dao = new TestMetaAlertUpdateDao();

  private class TestMetaAlertUpdateDao implements MetaAlertUpdateDao {

    Map<String, Document> documents = new HashMap<>();

    TestMetaAlertUpdateDao() {
      Document active = new Document(
          new HashMap<>(),
          ACTIVE.getStatusString(),
          METAALERT_TYPE,
          0L
      );
      documents.put(ACTIVE.getStatusString(), active);

      Document inactive = new Document(
          new HashMap<>(),
          INACTIVE.getStatusString(),
          METAALERT_TYPE,
          0L
      );
      inactive.getDocument().put(
          STATUS_FIELD,
          INACTIVE.getStatusString()
      );
      documents.put(INACTIVE.getStatusString(), inactive);
    }

    @Override
    public Document getLatest(String guid, String sensorType) {
      return documents.get(guid);
    }

    @Override
    public Iterable<Document> getAllLatest(List<GetRequest> getRequests) {
      return null;
    }

    @Override
    public IndexDao getIndexDao() {
      return indexDao;
    }

    @Override
    public String getMetAlertSensorName() {
      return METAALERT_TYPE;
    }

    @Override
    public String getMetaAlertIndex() {
      return "metaalert_index";
    }

    @Override
    public void update(Document update, Optional<String> index) {

    }
  }

  /**
   {
   "guid": "meta_alert",
   "index": "metaalert_index",
   "patch": [
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
   "value": []
   }
   ],
   "sensorType": "metaalert"
   }
   */
  @Multiline
  public static String statusPatchRequest;

  /**
   {
   "guid": "meta_alert",
   "index": "metaalert_index",
   "patch": [
   {
   "op": "add",
   "path": "/name",
   "value": []
   }
   ],
   "sensorType": "metaalert"
   }
   */
  @Multiline
  public static String namePatchRequest;

  @Test(expected = UnsupportedOperationException.class)
  public void testBatchUpdateThrowsException() {
    dao.batchUpdate(null);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testPatchNotAllowedAlert() throws ParseException {
    PatchRequest pr = new PatchRequest();
    Map<String, Object> patch = (JSONObject) new JSONParser().parse(alertPatchRequest);
    pr.setPatch(Collections.singletonList((JSONObject) ((JSONArray) patch.get("patch")).get(0)));
    assertFalse(dao.isPatchAllowed(pr));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testPatchNotAllowedStatus() throws ParseException {
    PatchRequest pr = new PatchRequest();
    Map<String, Object> patch = (JSONObject) new JSONParser().parse(statusPatchRequest);
    pr.setPatch(Collections.singletonList((JSONObject) ((JSONArray) patch.get("patch")).get(0)));
    assertFalse(dao.isPatchAllowed(pr));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testPatchAllowedName() throws ParseException {
    PatchRequest pr = new PatchRequest();
    Map<String, Object> patch = (JSONObject) new JSONParser().parse(namePatchRequest);
    pr.setPatch(Collections.singletonList((JSONObject) ((JSONArray) patch.get("patch")).get(0)));
    assertTrue(dao.isPatchAllowed(pr));
  }

  @Test
  public void testUpdateSingle() throws IOException {
    Map<Document, Optional<String>> updates = new HashMap<>();
    Document document = new Document(new HashMap<>(), "guid", "sensor", 0L);
    updates.put(document, Optional.empty());
    dao.update(updates);
    verify(indexDao, times(1)).update(document, Optional.empty());
  }

  @Test
  public void testUpdateMultiple() throws IOException {
    Map<Document, Optional<String>> updates = new HashMap<>();
    Document documentOne = new Document(new HashMap<>(), "guid", "sensor", 0L);
    updates.put(documentOne, Optional.empty());
    Document documentTwo = new Document(new HashMap<>(), "guid2", "sensor", 0L);
    updates.put(documentTwo, Optional.empty());
    dao.update(updates);
    verify(indexDao, times(1)).batchUpdate(updates);
  }

  @Test(expected = IllegalStateException.class)
  public void testAddAlertsToMetaAlertInactive() throws IOException {
    dao.addAlertsToMetaAlert(INACTIVE.getStatusString(), null);
  }

  @Test
  public void testBuildAddAlertToMetaAlertUpdatesEmpty() {
    Document hasChild = new Document(
        new HashMap<>(),
        "has_child",
        METAALERT_TYPE,
        0L
    );
    List<Map<String, Object>> alerts = new ArrayList<>();
    alerts.add(new HashMap<String, Object>() {
                 {
                   put(Constants.GUID, "child");
                 }
               }
    );
    hasChild.getDocument().put(
        ALERT_FIELD,
        alerts
    );
    Map<Document, Optional<String>> actual = dao
        .buildAddAlertToMetaAlertUpdates(hasChild, new ArrayList<>());
    assertEquals(0, actual.size());
  }

  @Test
  public void testBuildAddAlertToMetaAlertUpdates() {
    List<Map<String, Object>> alerts = new ArrayList<>();
    HashMap<String, Object> existingAlert = new HashMap<>();
    existingAlert.put(Constants.GUID, "child");
    existingAlert.put(METAALERT_FIELD, Collections.singletonList("has_child"));
    existingAlert.put(THREAT_FIELD_DEFAULT, 0.0f);
    alerts.add(existingAlert);

    Map<String, Object> metaAlertDocument = new HashMap<>();
    metaAlertDocument.put(ALERT_FIELD, alerts);
    metaAlertDocument.put(Constants.GUID, "has_child");
    Document hasChild = new Document(
        metaAlertDocument,
        "has_child",
        METAALERT_TYPE,
        0L
    );

    List<Document> newAlerts = new ArrayList<>();
    Map<String, Object> newAlert = new HashMap<>();
    newAlert.put(Constants.GUID, "alert1");
    Map<String, Object> newAlert2 = new HashMap<>();
    newAlert2.put(Constants.GUID, "alert2");
    newAlerts.add(new Document(newAlert, "alert1", "test", 0L));
    newAlerts.add(new Document(newAlert2, "alert2", "test", 0L));

    Map<Document, Optional<String>> actual = dao
        .buildAddAlertToMetaAlertUpdates(hasChild, newAlerts);
    assertEquals(3, actual.size());

    HashMap<String, Object> expectedExistingAlert = new HashMap<>();
    expectedExistingAlert.put(Constants.GUID, "child");
    expectedExistingAlert.put(METAALERT_FIELD, Collections.singletonList("has_child"));
    expectedExistingAlert.put(THREAT_FIELD_DEFAULT, 0.0f);

    Map<String, Object> expectedNewAlert = new HashMap<>();
    expectedNewAlert.put(Constants.GUID, "alert1");
    expectedNewAlert
        .put(MetaAlertConstants.METAALERT_FIELD, Collections.singletonList("has_child"));
    Document expectedNewDocument = new Document(expectedNewAlert, "alert1", "test", 0L);

    Map<String, Object> expectedNewAlert2 = new HashMap<>();
    expectedNewAlert2.put(Constants.GUID, "alert2");
    expectedNewAlert2
        .put(MetaAlertConstants.METAALERT_FIELD, Collections.singletonList("has_child"));
    Document expectedNewDocument2 = new Document(expectedNewAlert2, "alert2", "test", 0L);

    List<Map<String, Object>> expectedAlerts = new ArrayList<>();
    expectedAlerts.add(expectedExistingAlert);
    expectedAlerts.add(expectedNewAlert);
    expectedAlerts.add(expectedNewAlert2);

    Map<String, Object> expectedMetaAlertMap = new HashMap<>();
    expectedMetaAlertMap.put(Constants.GUID, "has_child");
    expectedMetaAlertMap.put(ALERT_FIELD, expectedAlerts);
    expectedMetaAlertMap.put(THREAT_FIELD_DEFAULT, 0.0f);
    expectedMetaAlertMap.putAll(new MetaScores(Collections.singletonList(0.0d)).getMetaScores());
    Document expectedMetaAlertDoc = new Document(expectedMetaAlertMap, "has_child", METAALERT_TYPE,
        0L);

    Map<Document, Optional<String>> expected = new HashMap<>();
    expected.put(expectedMetaAlertDoc, Optional.of("metaalert_index"));
    expected.put(expectedNewDocument, Optional.empty());
    expected.put(expectedNewDocument2, Optional.empty());

    Iterator<Entry<Document, Optional<String>>> actualIter = actual.entrySet().iterator();
    Entry<Document, Optional<String>> firstActual = actualIter.next();
    Entry<Document, Optional<String>> secondActual = actualIter.next();
    Entry<Document, Optional<String>> thirdActual = actualIter.next();

    Iterator<Entry<Document, Optional<String>>> expectedIter = expected.entrySet().iterator();
    Entry<Document, Optional<String>> firstExpected = expectedIter.next();
    Entry<Document, Optional<String>> secondExpected = expectedIter.next();
    Entry<Document, Optional<String>> thirdExpected = expectedIter.next();

    assertEquals(firstExpected, firstActual);
    assertEquals(secondExpected, secondActual);

    assertTrue(metaAlertDocumentEquals(thirdExpected.getKey(), thirdActual.getKey()));
    assertEquals(thirdExpected.getValue(), thirdActual.getValue());
  }

  @Test
  public void testRemoveAlertsFromMetaAlert() throws IOException {
    List<Map<String, Object>> alerts = new ArrayList<>();
    HashMap<String, Object> existingAlert = new HashMap<>();
    existingAlert.put(Constants.GUID, "child");
    existingAlert.put(METAALERT_FIELD, Collections.singletonList("has_child"));
    existingAlert.put(THREAT_FIELD_DEFAULT, 0.0f);
    alerts.add(existingAlert);
    HashMap<String, Object> existingAlert2 = new HashMap<>();
    existingAlert2.put(Constants.GUID, "child2");
    existingAlert2.put(METAALERT_FIELD, Collections.singletonList("has_child"));
    existingAlert2.put(THREAT_FIELD_DEFAULT, 0.0f);
    alerts.add(existingAlert2);
    HashMap<String, Object> existingAlert3 = new HashMap<>();
    existingAlert3.put(Constants.GUID, "child3");
    existingAlert3.put(METAALERT_FIELD, Collections.singletonList("has_child"));
    existingAlert3.put(THREAT_FIELD_DEFAULT, 0.0f);
    alerts.add(existingAlert3);

    Map<String, Object> metaAlertDocument = new HashMap<>();
    metaAlertDocument.put(ALERT_FIELD, alerts);
    metaAlertDocument.put(Constants.GUID, "has_child");
    Document hasChild = new Document(
        metaAlertDocument,
        "has_child",
        METAALERT_TYPE,
        0L
    );

    List<Document> deletedAlerts = new ArrayList<>();
    Document existingDocument = new Document(existingAlert, "child", "test", 0L);
    deletedAlerts.add(existingDocument);
    Document existingDocument3 = new Document(existingAlert3, "child3", "test", 0L);
    deletedAlerts.add(existingDocument3);

    Map<Document, Optional<String>> actual = dao
        .buildRemoveAlertsFromMetaAlert(hasChild, deletedAlerts);
    assertEquals(3, actual.size());

    Map<String, Object> expectedDeletedAlert = new HashMap<>();
    expectedDeletedAlert.put(Constants.GUID, "child");
    expectedDeletedAlert.put(THREAT_FIELD_DEFAULT, 0.0f);
    expectedDeletedAlert
        .put(MetaAlertConstants.METAALERT_FIELD, new ArrayList<>());
    Document expectedDeletedDocument = new Document(expectedDeletedAlert, "child", "test", 0L);

    Map<String, Object> expectedDeletedAlert3 = new HashMap<>();
    expectedDeletedAlert3.put(Constants.GUID, "child3");
    expectedDeletedAlert3.put(THREAT_FIELD_DEFAULT, 0.0f);
    expectedDeletedAlert3
        .put(MetaAlertConstants.METAALERT_FIELD, new ArrayList<>());
    Document expectedDeletedDocument2 = new Document(expectedDeletedAlert3, "child3", "test", 0L);

    List<Map<String, Object>> expectedAlerts = new ArrayList<>();
    expectedAlerts.add(existingAlert2);

    Map<String, Object> expectedMetaAlertMap = new HashMap<>();
    expectedMetaAlertMap.put(Constants.GUID, "has_child");
    expectedMetaAlertMap.put(ALERT_FIELD, expectedAlerts);
    expectedMetaAlertMap.put(THREAT_FIELD_DEFAULT, 0.0f);
    expectedMetaAlertMap.putAll(new MetaScores(Collections.singletonList(0.0d)).getMetaScores());
    Document expectedMetaAlertDoc = new Document(expectedMetaAlertMap, "has_child", METAALERT_TYPE,
        0L);

    Map<Document, Optional<String>> expected = new HashMap<>();
    expected.put(expectedDeletedDocument, Optional.empty());
    expected.put(expectedDeletedDocument2, Optional.empty());
    expected.put(expectedMetaAlertDoc, Optional.of("metaalert_index"));

    Iterator<Entry<Document, Optional<String>>> actualIter = actual.entrySet().iterator();
    Entry<Document, Optional<String>> firstActual = actualIter.next();
    Entry<Document, Optional<String>> secondActual = actualIter.next();
    Entry<Document, Optional<String>> thirdActual = actualIter.next();

    Iterator<Entry<Document, Optional<String>>> expectedIter = expected.entrySet().iterator();
    Entry<Document, Optional<String>> firstExpected = expectedIter.next();
    Entry<Document, Optional<String>> secondExpected = expectedIter.next();
    Entry<Document, Optional<String>> thirdExpected = expectedIter.next();

    assertTrue(metaAlertDocumentEquals(firstExpected.getKey(), firstActual.getKey()));
    assertEquals(firstExpected.getValue(), firstActual.getValue());

    assertEquals(secondExpected, secondActual);
    assertEquals(thirdExpected, thirdActual);
  }

  @Test
  public void testRemoveAlertsFromMetaAlertNoChildAlerts() {
    Document empty = new Document(new HashMap<>(), "empty", METAALERT_TYPE, 0L);
    boolean actual = dao.removeAlertsFromMetaAlert(empty, Collections.singletonList("child"));
    assertFalse(actual);
  }

  @Test
  public void testRemoveAlertsFromMetaAlertEmptyRemoveList() {
    Document hasChild = new Document(
        new HashMap<>(),
        "has_child",
        METAALERT_TYPE,
        0L
    );
    hasChild.getDocument().put(
        STATUS_FIELD,
        ACTIVE.getStatusString()
    );
    hasChild.getDocument().put(
        ALERT_FIELD,
        new HashMap<String, Object>() {{
          put(Constants.GUID, "child");
        }}
    );
    boolean actual = dao.removeAlertsFromMetaAlert(hasChild, new ArrayList<>());
    assertFalse(actual);
  }

  @Test
  public void testRemoveAlertsFromMetaAlertEmptyRemoveSingle() {
    Document hasChild = new Document(
        new HashMap<>(),
        "has_child",
        METAALERT_TYPE,
        0L
    );
    hasChild.getDocument().put(
        STATUS_FIELD,
        ACTIVE.getStatusString()
    );
    List<Map<String, Object>> alerts = new ArrayList<>();
    alerts.add(new HashMap<String, Object>() {
                 {
                   put(Constants.GUID, "child");
                 }
               }
    );
    hasChild.getDocument().put(
        ALERT_FIELD,
        alerts
    );
    boolean actual = dao.removeAlertsFromMetaAlert(hasChild, Collections.singletonList("child"));

    Document expected = new Document(
        new HashMap<>(),
        "has_child",
        METAALERT_TYPE,
        0L
    );
    expected.getDocument().put(
        STATUS_FIELD,
        ACTIVE.getStatusString()
    );
    expected.getDocument().put(ALERT_FIELD, new ArrayList<>());
    assertTrue(actual);
    assertEquals(expected, hasChild);
  }

  @Test
  public void testBuildStatusChangeUpdatesToInactive() {
    List<Map<String, Object>> alerts = new ArrayList<>();
    HashMap<String, Object> existingAlert = new HashMap<>();
    existingAlert.put(Constants.GUID, "child");
    existingAlert.put(METAALERT_FIELD, Collections.singletonList("has_child"));
    existingAlert.put(THREAT_FIELD_DEFAULT, 0.0f);
    alerts.add(existingAlert);
    Document alert = new Document(existingAlert, "child", "test", 0L);
    HashMap<String, Object> existingAlert2 = new HashMap<>();
    existingAlert2.put(Constants.GUID, "child2");
    existingAlert2.put(METAALERT_FIELD, Collections.singletonList("has_child"));
    existingAlert2.put(THREAT_FIELD_DEFAULT, 0.0f);
    alerts.add(existingAlert2);
    Document alert2 = new Document(existingAlert2, "child2", "test", 0L);
    List<Document> documents = new ArrayList<>();
    documents.add(alert);
    documents.add(alert2);

    Map<String, Object> metaAlertDocument = new HashMap<>();
    metaAlertDocument.put(ALERT_FIELD, alerts);
    metaAlertDocument.put(Constants.GUID, "has_child");
    metaAlertDocument.put(STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString());
    Document hasChild = new Document(
        metaAlertDocument,
        "has_child",
        METAALERT_TYPE,
        0L
    );

    List<Document> deletedAlerts = new ArrayList<>();
    Document existingDocument = new Document(existingAlert, "child", "test", 0L);
    deletedAlerts.add(existingDocument);

    Map<Document, Optional<String>> actual = dao
        .buildStatusChangeUpdates(hasChild, documents, MetaAlertStatus.INACTIVE);
    assertEquals(3, actual.size());

    Map<String, Object> expectedDeletedAlert = new HashMap<>();
    expectedDeletedAlert.put(Constants.GUID, "child");
    expectedDeletedAlert.put(THREAT_FIELD_DEFAULT, 0.0f);
    expectedDeletedAlert
        .put(MetaAlertConstants.METAALERT_FIELD, new ArrayList<>());
    Document expectedDeletedDocument = new Document(expectedDeletedAlert, "child", "test", 0L);

    Map<String, Object> expectedDeletedAlert2 = new HashMap<>();
    expectedDeletedAlert2.put(Constants.GUID, "child2");
    expectedDeletedAlert2.put(THREAT_FIELD_DEFAULT, 0.0f);
    expectedDeletedAlert2
        .put(MetaAlertConstants.METAALERT_FIELD, new ArrayList<>());
    Document expectedDeletedDocument2 = new Document(expectedDeletedAlert2, "child2", "test", 0L);

    List<Map<String, Object>> expectedAlerts = new ArrayList<>();
    expectedAlerts.add(existingAlert);
    expectedAlerts.add(existingAlert2);

    Map<String, Object> expectedMetaAlertMap = new HashMap<>();
    expectedMetaAlertMap.put(Constants.GUID, "has_child");
    expectedMetaAlertMap.put(ALERT_FIELD, expectedAlerts);
    expectedMetaAlertMap.put(STATUS_FIELD, MetaAlertStatus.INACTIVE.getStatusString());
    Document expectedMetaAlertDoc = new Document(expectedMetaAlertMap, "has_child", METAALERT_TYPE,
        0L);

    Map<Document, Optional<String>> expected = new HashMap<>();
    expected.put(expectedMetaAlertDoc, Optional.of("metaalert_index"));
    expected.put(expectedDeletedDocument, Optional.empty());
    expected.put(expectedDeletedDocument2, Optional.empty());

    Iterator<Entry<Document, Optional<String>>> actualIter = actual.entrySet().iterator();
    Entry<Document, Optional<String>> firstActual = actualIter.next();
    Entry<Document, Optional<String>> secondActual = actualIter.next();
    Entry<Document, Optional<String>> thirdActual = actualIter.next();

    Iterator<Entry<Document, Optional<String>>> expectedIter = expected.entrySet().iterator();
    Entry<Document, Optional<String>> firstExpected = expectedIter.next();
    Entry<Document, Optional<String>> secondExpected = expectedIter.next();
    Entry<Document, Optional<String>> thirdExpected = expectedIter.next();

    assertTrue(metaAlertDocumentEquals(firstExpected.getKey(), firstActual.getKey()));
    assertEquals(secondExpected, secondActual);
    assertEquals(thirdExpected, thirdActual);
  }

  @Test
  public void testBuildStatusChangeUpdatesToActive() {
    List<Map<String, Object>> alerts = new ArrayList<>();
    HashMap<String, Object> existingAlert = new HashMap<>();
    existingAlert.put(Constants.GUID, "child");
    existingAlert.put(METAALERT_FIELD, Collections.singletonList("has_child"));
    existingAlert.put(THREAT_FIELD_DEFAULT, 0.0f);
    alerts.add(existingAlert);
    Document alert = new Document(existingAlert, "child", "test", 0L);
    HashMap<String, Object> existingAlert2 = new HashMap<>();
    existingAlert2.put(Constants.GUID, "child2");
    existingAlert2.put(METAALERT_FIELD, Collections.singletonList("has_child"));
    existingAlert2.put(THREAT_FIELD_DEFAULT, 0.0f);
    alerts.add(existingAlert2);
    Document alert2 = new Document(existingAlert2, "child2", "test", 0L);
    List<Document> documents = new ArrayList<>();
    documents.add(alert);
    documents.add(alert2);

    Map<String, Object> metaAlertDocument = new HashMap<>();
    metaAlertDocument.put(ALERT_FIELD, alerts);
    metaAlertDocument.put(Constants.GUID, "has_child");
    metaAlertDocument.put(STATUS_FIELD, MetaAlertStatus.INACTIVE.getStatusString());
    Document hasChild = new Document(
        metaAlertDocument,
        "has_child",
        METAALERT_TYPE,
        0L
    );

    List<Document> deletedAlerts = new ArrayList<>();
    Document existingDocument = new Document(existingAlert, "child", "test", 0L);
    deletedAlerts.add(existingDocument);

    Map<Document, Optional<String>> actual = dao
        .buildStatusChangeUpdates(hasChild, documents, MetaAlertStatus.ACTIVE);
    assertEquals(1, actual.size());

    Map<String, Object> expectedDeletedAlert = new HashMap<>();
    expectedDeletedAlert.put(Constants.GUID, "child");
    expectedDeletedAlert.put(THREAT_FIELD_DEFAULT, 0.0f);
    expectedDeletedAlert
        .put(MetaAlertConstants.METAALERT_FIELD, new ArrayList<>());
    Document expectedDeletedDocument = new Document(expectedDeletedAlert, "child", "test", 0L);

    Map<String, Object> expectedDeletedAlert2 = new HashMap<>();
    expectedDeletedAlert2.put(Constants.GUID, "child2");
    expectedDeletedAlert2.put(THREAT_FIELD_DEFAULT, 0.0f);
    expectedDeletedAlert2
        .put(MetaAlertConstants.METAALERT_FIELD, new ArrayList<>());
    Document expectedDeletedDocument2 = new Document(expectedDeletedAlert2, "child2", "test", 0L);

    List<Map<String, Object>> expectedAlerts = new ArrayList<>();
    expectedAlerts.add(existingAlert);
    expectedAlerts.add(existingAlert2);

    Map<String, Object> expectedMetaAlertMap = new HashMap<>();
    expectedMetaAlertMap.put(Constants.GUID, "has_child");
    expectedMetaAlertMap.put(ALERT_FIELD, expectedAlerts);
    expectedMetaAlertMap.put(STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString());
    Document expectedMetaAlertDoc = new Document(expectedMetaAlertMap, "has_child", METAALERT_TYPE,
        0L);

    Map<Document, Optional<String>> expected = new HashMap<>();
    expected.put(expectedMetaAlertDoc, Optional.of("metaalert_index"));
    expected.put(expectedDeletedDocument, Optional.empty());
    expected.put(expectedDeletedDocument2, Optional.empty());

    Iterator<Entry<Document, Optional<String>>> actualIter = actual.entrySet().iterator();
    Entry<Document, Optional<String>> firstActual = actualIter.next();

    Iterator<Entry<Document, Optional<String>>> expectedIter = expected.entrySet().iterator();
    Entry<Document, Optional<String>> firstExpected = expectedIter.next();

    assertTrue(metaAlertDocumentEquals(firstExpected.getKey(), firstActual.getKey()));
  }

  @Test
  public void testRemoveAlertsFromMetaAlertEmptyRemoveMultiple() {
    Document hasChild = new Document(new HashMap<>(), "has_child", METAALERT_TYPE, 0L);
    hasChild.getDocument().put(STATUS_FIELD, ACTIVE.getStatusString());
    List<Map<String, Object>> alerts = new ArrayList<>();
    alerts.add(new HashMap<String, Object>() {{
                 put(Constants.GUID, "child");
               }}
    );
    alerts.add(new HashMap<String, Object>() {{
                 put(Constants.GUID, "child2");
               }}
    );
    alerts.add(new HashMap<String, Object>() {{
                 put(Constants.GUID, "child3");
               }}
    );
    hasChild.getDocument().put(ALERT_FIELD, alerts);
    List<String> removeGuids = new ArrayList<>();
    removeGuids.add("child");
    removeGuids.add("child3");
    removeGuids.add("child_doesn't_exist");

    boolean actual = dao.removeAlertsFromMetaAlert(hasChild, removeGuids);

    Document expected = new Document(new HashMap<>(), "has_child", METAALERT_TYPE, 0L);
    expected.getDocument().put(STATUS_FIELD, ACTIVE.getStatusString());
    List<Map<String, Object>> alertsExpected = new ArrayList<>();
    alertsExpected.add(new HashMap<String, Object>() {{
                         put(Constants.GUID, "child2");
                       }}
    );

    expected.getDocument().put(ALERT_FIELD, alertsExpected);
    assertTrue(actual);
    assertEquals(expected, hasChild);
  }

  @Test(expected = IllegalStateException.class)
  public void testRemoveAlertsFromMetaAlertInactive() throws IOException {
    dao.removeAlertsFromMetaAlert(INACTIVE.getStatusString(), null);
  }

  @Test
  public void testRemoveMetaAlertFromAlertSuccess() {
    List<String> metaAlertGuids = new ArrayList<>();
    metaAlertGuids.add("metaalert1");
    metaAlertGuids.add("metaalert2");
    Map<String, Object> alertFields = new HashMap<>();
    alertFields.put(METAALERT_FIELD, metaAlertGuids);
    Document alert = new Document(alertFields, "alert", "test", 0L);

    Document expected = new Document(new HashMap<>(), "alert", "test", 0L);
    List<String> expectedMetaAlertGuids = new ArrayList<>();
    expectedMetaAlertGuids.add("metaalert2");
    expected.getDocument().put(METAALERT_FIELD, expectedMetaAlertGuids);

    boolean actual = dao.removeMetaAlertFromAlert("metaalert1", alert);
    assertTrue(actual);
    assertEquals(expected, alert);
  }

  @Test
  public void testRemoveMetaAlertFromAlertMissing() {
    List<String> metaAlertGuids = new ArrayList<>();
    metaAlertGuids.add("metaalert1");
    metaAlertGuids.add("metaalert2");
    Map<String, Object> alertFields = new HashMap<>();
    alertFields.put(METAALERT_FIELD, metaAlertGuids);
    Document alert = new Document(alertFields, "alert", "test", 0L);

    boolean actual = dao.removeMetaAlertFromAlert("metaalert3", alert);
    assertFalse(actual);
  }

  @Test
  public void testAddMetaAlertToAlertEmpty() {
    Map<String, Object> alertFields = new HashMap<>();
    alertFields.put(METAALERT_FIELD, new ArrayList<>());
    Document alert = new Document(alertFields, "alert", "test", 0L);

    Document expected = new Document(new HashMap<>(), "alert", "test", 0L);
    List<String> expectedMetaAlertGuids = new ArrayList<>();
    expectedMetaAlertGuids.add("metaalert1");
    expected.getDocument().put(METAALERT_FIELD, expectedMetaAlertGuids);

    boolean actual = dao.addMetaAlertToAlert("metaalert1", alert);
    assertTrue(actual);
    assertEquals(expected, alert);
  }

  @Test
  public void testAddMetaAlertToAlertNonEmpty() {
    List<String> metaAlertGuids = new ArrayList<>();
    metaAlertGuids.add("metaalert1");
    Map<String, Object> alertFields = new HashMap<>();
    alertFields.put(METAALERT_FIELD, metaAlertGuids);
    Document alert = new Document(alertFields, "alert", "test", 0L);

    Document expected = new Document(new HashMap<>(), "alert", "test", 0L);
    List<String> expectedMetaAlertGuids = new ArrayList<>();
    expectedMetaAlertGuids.add("metaalert1");
    expectedMetaAlertGuids.add("metaalert2");
    expected.getDocument().put(METAALERT_FIELD, expectedMetaAlertGuids);

    boolean actual = dao.addMetaAlertToAlert("metaalert2", alert);
    assertTrue(actual);
    assertEquals(expected, alert);
  }

  @Test
  public void testAddMetaAlertToAlertDuplicate() {
    List<String> metaAlertGuids = new ArrayList<>();
    metaAlertGuids.add("metaalert1");
    Map<String, Object> alertFields = new HashMap<>();
    alertFields.put(METAALERT_FIELD, metaAlertGuids);
    Document alert = new Document(alertFields, "alert", "test", 0L);

    boolean actual = dao.addMetaAlertToAlert("metaalert1", alert);
    assertFalse(actual);
  }


  // Utility method to ensure that the floating point values contained in a metaalert don't get
  // incorrectly evaluated as not equal.
  private boolean metaAlertDocumentEquals(Document expected, Document actual) {
    if (!expected.getGuid().equals(actual.getGuid())) {
      return false;
    }
    if (!expected.getSensorType().equals(actual.getSensorType())) {
      return false;
    }
    if (!expected.getTimestamp().equals(actual.getTimestamp())) {
      return false;
    }

    // The underlying documents have to be compared more thoroughly since it has floating point
    Map<String, Object> expectedDocument = expected.getDocument();
    Map<String, Object> actualDocument = actual.getDocument();

    if (expectedDocument.size() != actualDocument.size()) {
      return false;
    }

    for (Entry<String, Object> entry : expectedDocument.entrySet()) {
      Object value = entry.getValue();
      Object actualValue = actual.getDocument().get(entry.getKey());
      if (value instanceof Float) {
        if (!MathUtils.equals((Float) value, (Float) actualValue, EPS)) {
          return false;
        }
      } else if (value instanceof Double) {
        if (!MathUtils.equals((Double) value, (Double) actualValue, EPS)) {
          return false;
        }
      } else {
        if (!value.equals(actual.getDocument().get(entry.getKey()))) {
          return false;
        }
      }
    }

    return true;
  }
}
