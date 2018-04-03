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

import static org.apache.metron.common.Constants.GUID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.metron.indexing.dao.RetrieveLatestDao;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.OriginalNotFoundException;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.apache.metron.indexing.dao.update.UpdateDao;

public interface MetaAlertUpdateDao extends UpdateDao, DeferredMetaAlertIndexDao,
    RetrieveLatestDao {

  String STATUS_PATH = "/status";
  String ALERT_PATH = "/alert";

  @Override
  default void batchUpdate(Map<Document, Optional<String>> updates) {
    throw new UnsupportedOperationException("Meta alerts do not allow for bulk updates");
  }

  /**
   * Performs a patch operation on a document based on the result of @{link #isPatchAllowed(PatchRequest)}
   *
   * @param request The patch request.
   * @param timestamp Optionally a timestamp to set. If not specified then current time is used.
   * @throws OriginalNotFoundException If no original document is found to patch.
   * @throws IOException If an error occurs performing the patch.
   */
  @Override
  default void patch(PatchRequest request, Optional<Long> timestamp)
      throws OriginalNotFoundException, IOException {
    if (isPatchAllowed(request)) {
      Document d = getPatchedDocument(request, timestamp);
      getIndexDao().update(d, Optional.ofNullable(request.getIndex()));
    } else {
      throw new IllegalArgumentException(
          "Meta alert patches are not allowed for /alert or /status paths.  "
              + "Please use the add/remove alert or update status functions instead.");
    }
  }

  /**
   * Determines if a given patch request is allowed or not. By default patching the 'alert' or
   * 'status' fields are not allowed, because they should be updated via the specific methods.
   * @param request The patch request to examine
   * @return True if patch can be performed, false otherwise
   */
  default boolean isPatchAllowed(PatchRequest request) {
    if (request.getPatch() != null && !request.getPatch().isEmpty()) {
      for (Map<String, Object> patch : request.getPatch()) {
        Object pathObj = patch.get("path");
        if (pathObj != null && pathObj instanceof String) {
          String path = (String) pathObj;
          if (STATUS_PATH.equals(path) || ALERT_PATH.equals(path)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  /**
   * Calls the single update variant if there's only one update, otherwise calls batch.
   * MetaAlerts may defer to an implementation specific IndexDao.
   * @param updates The list of updates to run
   * @throws IOException If there's an update error
   */
  default void update(Map<Document, Optional<String>> updates)
      throws IOException {
    if (updates.size() == 1) {
      Entry<Document, Optional<String>> singleUpdate = updates.entrySet().iterator().next();
      getIndexDao().update(singleUpdate.getKey(), singleUpdate.getValue());
    } else if (updates.size() > 1) {
      getIndexDao().batchUpdate(updates);
    } // else we have no updates, so don't do anything
  }

  default boolean addAlertsToMetaAlert(String metaAlertGuid, List<GetRequest> alertRequests)
      throws IOException {
    Map<Document, Optional<String>> updates = new HashMap<>();
    Document metaAlert = getLatest(metaAlertGuid, getMetAlertSensorName());
    if (MetaAlertStatus.ACTIVE.getStatusString()
        .equals(metaAlert.getDocument().get(MetaAlertConstants.STATUS_FIELD))) {
      Iterable<Document> alerts = getAllLatest(alertRequests);
      boolean metaAlertUpdated = addAlertsToMetaAlert(metaAlert, alerts);
      if (metaAlertUpdated) {
        MetaScores.calculateMetaScores(metaAlert, getThreatTriageField(), getThreatSort());
        updates.put(metaAlert, Optional.of(getMetaAlertIndex()));
        for (Document alert : alerts) {
          if (addMetaAlertToAlert(metaAlert.getGuid(), alert)) {
            updates.put(alert, Optional.empty());
          }
        }
        update(updates);
      }
      return metaAlertUpdated;
    } else {
      throw new IllegalStateException("Adding alerts to an INACTIVE meta alert is not allowed");
    }
  }

  default boolean addAlertsToMetaAlert(Document metaAlert, Iterable<Document> alerts) {
    boolean alertAdded = false;
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> currentAlerts = (List<Map<String, Object>>) metaAlert.getDocument()
        .get(MetaAlertConstants.ALERT_FIELD);
    Set<String> currentAlertGuids = currentAlerts.stream().map(currentAlert ->
        (String) currentAlert.get(GUID)).collect(Collectors.toSet());
    for (Document alert : alerts) {
      String alertGuid = alert.getGuid();
      // Only add an alert if it isn't already in the meta alert
      if (!currentAlertGuids.contains(alertGuid)) {
        currentAlerts.add(alert.getDocument());
        alertAdded = true;
      }
    }
    return alertAdded;
  }

  default boolean removeAlertsFromMetaAlert(Document metaAlert, Collection<String> alertGuids) {
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> currentAlerts = (List<Map<String, Object>>) metaAlert.getDocument()
        .get(MetaAlertConstants.ALERT_FIELD);
    int previousSize = currentAlerts.size();
    // Only remove an alert if it is in the meta alert
    currentAlerts.removeIf(currentAlert -> alertGuids.contains((String) currentAlert.get(GUID)));
    return currentAlerts.size() != previousSize;
  }

  default boolean removeAlertsFromMetaAlert(String metaAlertGuid, List<GetRequest> alertRequests)
      throws IOException {
    Map<Document, Optional<String>> updates = new HashMap<>();
    Document metaAlert = getLatest(metaAlertGuid, MetaAlertConstants.METAALERT_TYPE);
    List<Map<String, Object>> alertsBefore = new ArrayList<>();
    alertsBefore
        .addAll((List<Map<String, Object>>) metaAlert.getDocument()
            .get(MetaAlertConstants.ALERT_FIELD));
    if (MetaAlertStatus.ACTIVE.getStatusString()
        .equals(metaAlert.getDocument().get(MetaAlertConstants.STATUS_FIELD))) {
      Iterable<Document> alerts = getAllLatest(alertRequests);
      Collection<String> alertGuids = alertRequests.stream().map(GetRequest::getGuid).collect(
          Collectors.toList());
      boolean metaAlertUpdated = removeAlertsFromMetaAlert(metaAlert, alertGuids);
      if (metaAlertUpdated) {
        List<Map<String, Object>> alertsAfter = (List<Map<String, Object>>) metaAlert.getDocument()
            .get(MetaAlertConstants.ALERT_FIELD);
        // If we have no alerts left, we might need to handle the deletes manually. Thanks Solr.
        if (alertsAfter.size() < alertsBefore.size() && alertsAfter.size() == 0) {
          deleteRemainingMetaAlerts(alertsBefore);
        }
        MetaScores.calculateMetaScores(metaAlert, getThreatTriageField(), getThreatSort());
        updates.put(metaAlert, Optional.of(getMetaAlertIndex()));
        for (Document alert : alerts) {
          if (removeMetaAlertFromAlert(metaAlert.getGuid(), alert)) {
            updates.put(alert, Optional.empty());
          }
        }
        update(updates);
      }
      return metaAlertUpdated;
    } else {
      throw new IllegalStateException("Removing alerts from an INACTIVE meta alert is not allowed");
    }
  }

  // Do nothing by default.  It's implementation weirdness can be handled.
  default void deleteRemainingMetaAlerts(
      List<Map<String, Object>> alertsBefore) throws IOException {
  }

  default boolean removeMetaAlertFromAlert(String metaAlertGuid, Document alert) {
    List<String> metaAlertField = new ArrayList<>();
    @SuppressWarnings("unchecked")
    List<String> alertField = (List<String>) alert.getDocument()
        .get(MetaAlertConstants.METAALERT_FIELD);
    if (alertField != null) {
      metaAlertField.addAll(alertField);
    }
    boolean metaAlertRemoved = metaAlertField.remove(metaAlertGuid);
    if (metaAlertRemoved) {
      alert.getDocument().put(MetaAlertConstants.METAALERT_FIELD, metaAlertField);
    }
    return metaAlertRemoved;
  }

  /**
   * The meta alert status field can be set to either 'active' or 'inactive' and will control whether or not meta alerts
   * (and child alerts) appear in search results.  An 'active' status will cause meta alerts to appear in search
   * results instead of it's child alerts and an 'inactive' status will suppress the meta alert from search results
   * with child alerts appearing in search results as normal.  A change to 'inactive' will cause the meta alert GUID to
   * be removed from all it's child alert's "metaalerts" field.  A change back to 'active' will have the opposite effect.
   *
   * @param metaAlertGuid The GUID of the meta alert
   * @param status A status value of 'active' or 'inactive'
   * @return True or false depending on if the status was changed
   * @throws IOException if an error occurs during the update.
   */
  default boolean updateMetaAlertStatus(String metaAlertGuid, MetaAlertStatus status)
      throws IOException {
    Map<Document, Optional<String>> updates = new HashMap<>();
    Document metaAlert = getLatest(metaAlertGuid, MetaAlertConstants.METAALERT_TYPE);
    String currentStatus = (String) metaAlert.getDocument().get(MetaAlertConstants.STATUS_FIELD);
    boolean metaAlertUpdated = !status.getStatusString().equals(currentStatus);
    if (metaAlertUpdated) {
      metaAlert.getDocument().put(MetaAlertConstants.STATUS_FIELD, status.getStatusString());
      updates.put(metaAlert, Optional.of(getMetaAlertIndex()));
      List<GetRequest> getRequests = new ArrayList<>();
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> currentAlerts = (List<Map<String, Object>>) metaAlert.getDocument()
          .get(MetaAlertConstants.ALERT_FIELD);
      currentAlerts.stream().forEach(currentAlert -> {
        getRequests.add(new GetRequest((String) currentAlert.get(GUID),
            (String) currentAlert.get(MetaAlertConstants.SOURCE_TYPE)));
      });
      Iterable<Document> alerts = getAllLatest(getRequests);
      for (Document alert : alerts) {
        boolean metaAlertAdded = false;
        boolean metaAlertRemoved = false;
        // If we're making it active add add the meta alert guid for every alert.
        if (MetaAlertStatus.ACTIVE.equals(status)) {
          metaAlertAdded = addMetaAlertToAlert(metaAlert.getGuid(), alert);
        }
        // If we're making it inactive, remove the meta alert guid from every alert.
        if (MetaAlertStatus.INACTIVE.equals(status)) {
          metaAlertRemoved = removeMetaAlertFromAlert(metaAlert.getGuid(), alert);
        }
        if (metaAlertAdded || metaAlertRemoved) {
          updates.put(alert, Optional.empty());
        }
      }
    }
    if (metaAlertUpdated) {
      update(updates);
    }
    return metaAlertUpdated;
  }

  default boolean addMetaAlertToAlert(String metaAlertGuid, Document alert) {
    List<String> metaAlertField = new ArrayList<>();
    @SuppressWarnings("unchecked")
    List<String> alertField = (List<String>) alert.getDocument()
        .get(MetaAlertConstants.METAALERT_FIELD);
    if (alertField != null) {
      metaAlertField.addAll(alertField);
    }

    boolean metaAlertAdded = !metaAlertField.contains(metaAlertGuid);
    if (metaAlertAdded) {
      metaAlertField.add(metaAlertGuid);
      alert.getDocument().put(MetaAlertConstants.METAALERT_FIELD, metaAlertField);
    }
    return metaAlertAdded;
  }
}
