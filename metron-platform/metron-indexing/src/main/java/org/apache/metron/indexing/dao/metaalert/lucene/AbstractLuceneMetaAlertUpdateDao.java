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

package org.apache.metron.indexing.dao.metaalert.lucene;

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
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.indexing.dao.RetrieveLatestDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConfig;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConstants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertRetrieveLatestDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.metaalert.MetaAlertUpdateDao;
import org.apache.metron.indexing.dao.metaalert.MetaScores;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.OriginalNotFoundException;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.apache.metron.indexing.dao.update.UpdateDao;

public abstract class AbstractLuceneMetaAlertUpdateDao implements MetaAlertUpdateDao {

  private UpdateDao updateDao;
  private MetaAlertRetrieveLatestDao retrieveLatestDao;
  private MetaAlertConfig config;

  protected AbstractLuceneMetaAlertUpdateDao(
      UpdateDao updateDao,
      MetaAlertRetrieveLatestDao retrieveLatestDao,
      MetaAlertConfig config) {
    this.updateDao = updateDao;
    this.retrieveLatestDao = retrieveLatestDao;
    this.config = config;
  }

  public UpdateDao getUpdateDao() {
    return updateDao;
  }

  public MetaAlertRetrieveLatestDao getRetrieveLatestDao() {
    return retrieveLatestDao;
  }

  public MetaAlertConfig getConfig() {
    return config;
  }

  /**
   * Performs a patch operation on a document based on the result of @{link #isPatchAllowed(PatchRequest)}
   *
   * @param retrieveLatestDao DAO to retrieve the item to be patched
   * @param request The patch request.
   * @param timestamp Optionally a timestamp to set. If not specified then current time is used.
   * @throws OriginalNotFoundException If no original document is found to patch.
   * @throws IOException If an error occurs performing the patch.
   */
  @Override
  public void patch(RetrieveLatestDao retrieveLatestDao, PatchRequest request,
      Optional<Long> timestamp)
      throws OriginalNotFoundException, IOException {
    if (isPatchAllowed(request)) {
      updateDao.patch(retrieveLatestDao, request, timestamp);
    } else {
      throw new IllegalArgumentException(
          "Meta alert patches are not allowed for /alert or /status paths.  "
              + "Please use the add/remove alert or update status functions instead.");
    }
  }

  @Override
  public void batchUpdate(Map<Document, Optional<String>> updates) {
    throw new UnsupportedOperationException("Meta alerts do not allow for bulk updates");
  }

  /**
   * Build the Document representing a meta alert to be created.
   * @param alerts The Elasticsearch results for the meta alerts child documents
   * @param groups The groups used to create this meta alert
   * @return A Document representing the new meta alert
   */
  protected Document buildCreateDocument(Iterable<Document> alerts, List<String> groups,
      String alertField) {
    // Need to create a Document from the multiget. Scores will be calculated later
    Map<String, Object> metaSource = new HashMap<>();
    List<Map<String, Object>> alertList = new ArrayList<>();
    for (Document alert : alerts) {
      alertList.add(alert.getDocument());
    }
    metaSource.put(alertField, alertList);

    // Add any meta fields
    String guid = UUID.randomUUID().toString();
    metaSource.put(GUID, guid);
    metaSource.put(Constants.Fields.TIMESTAMP.getName(), System.currentTimeMillis());
    metaSource.put(MetaAlertConstants.GROUPS_FIELD, groups);
    metaSource.put(MetaAlertConstants.STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString());

    return new Document(metaSource, guid, MetaAlertConstants.METAALERT_TYPE,
        System.currentTimeMillis());
  }

  /**
   * Builds the set of updates when alerts are removed from a meta alert
   * @param metaAlert The meta alert to remove alerts from
   * @param alerts The alert Documents to be removed
   * @return The updates to be run
   * @throws IOException If an error is thrown.
   */
  @SuppressWarnings("unchecked")
  protected Map<Document, Optional<String>> buildRemoveAlertsFromMetaAlert(Document metaAlert,
      Iterable<Document> alerts)
      throws IOException {
    Map<Document, Optional<String>> updates = new HashMap<>();

    List<String> alertGuids = new ArrayList<>();
    for (Document alert : alerts) {
      alertGuids.add(alert.getGuid());
    }
    List<Map<String, Object>> alertsBefore = new ArrayList<>();
    Map<String, Object> documentBefore = metaAlert.getDocument();
    if (documentBefore.containsKey(MetaAlertConstants.ALERT_FIELD)) {
      alertsBefore
          .addAll((List<Map<String, Object>>) documentBefore.get(MetaAlertConstants.ALERT_FIELD));
    }
    boolean metaAlertUpdated = removeAlertsFromMetaAlert(metaAlert, alertGuids);
    if (metaAlertUpdated) {
      List<Map<String, Object>> alertsAfter = (List<Map<String, Object>>) metaAlert.getDocument()
          .get(MetaAlertConstants.ALERT_FIELD);
      if (alertsAfter.size() < alertsBefore.size() && alertsAfter.size() == 0) {
        throw new IllegalStateException("Removing these alerts will result in an empty meta alert.  Empty meta alerts are not allowed.");
      }
      MetaScores
          .calculateMetaScores(metaAlert, config.getThreatTriageField(), config.getThreatSort());
      updates.put(metaAlert, Optional.of(config.getMetaAlertIndex()));
      for (Document alert : alerts) {
        if (removeMetaAlertFromAlert(metaAlert.getGuid(), alert)) {
          updates.put(alert, Optional.empty());
        }
      }
    }
    return updates;
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean removeAlertsFromMetaAlert(String metaAlertGuid, List<GetRequest> alertRequests)
      throws IOException {
    Document metaAlert = retrieveLatestDao
        .getLatest(metaAlertGuid, MetaAlertConstants.METAALERT_TYPE);
    if (metaAlert == null) {
      return false;
    }
    if (MetaAlertStatus.ACTIVE.getStatusString()
        .equals(metaAlert.getDocument().get(MetaAlertConstants.STATUS_FIELD))) {
      Iterable<Document> alerts = retrieveLatestDao.getAllLatest(alertRequests);
      Map<Document, Optional<String>> updates = buildRemoveAlertsFromMetaAlert(metaAlert, alerts);
      update(updates);
      return updates.size() != 0;
    } else {
      throw new IllegalStateException("Removing alerts from an INACTIVE meta alert is not allowed");
    }
  }

  /**
   * Removes a given set of alerts from a given alert. AlertGuids that are not found are ignored.
   * @param metaAlert The metaalert to be mutated.
   * @param alertGuids The alerts to remove from the metaaelrt.
   * @return True if the metaAlert changed, false otherwise.
   */
  protected boolean removeAlertsFromMetaAlert(Document metaAlert, Collection<String> alertGuids) {
    // If we don't have child alerts or nothing is being removed, immediately return false.
    if (!metaAlert.getDocument().containsKey(MetaAlertConstants.ALERT_FIELD)
        || alertGuids.size() == 0) {
      return false;
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> currentAlerts = (List<Map<String, Object>>) metaAlert.getDocument()
        .get(MetaAlertConstants.ALERT_FIELD);
    int previousSize = currentAlerts.size();
    // Only remove an alert if it is in the meta alert
    currentAlerts.removeIf(currentAlert -> alertGuids.contains(currentAlert.get(GUID)));
    return currentAlerts.size() != previousSize;
  }

  @Override
  public boolean updateMetaAlertStatus(String metaAlertGuid, MetaAlertStatus status)
      throws IOException {
    Document metaAlert = retrieveLatestDao
        .getLatest(metaAlertGuid, MetaAlertConstants.METAALERT_TYPE);
    String currentStatus = (String) metaAlert.getDocument().get(MetaAlertConstants.STATUS_FIELD);
    boolean metaAlertUpdated = !status.getStatusString().equals(currentStatus);
    if (metaAlertUpdated) {
      List<GetRequest> getRequests = new ArrayList<>();
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> currentAlerts = (List<Map<String, Object>>) metaAlert.getDocument()
          .get(MetaAlertConstants.ALERT_FIELD);
      currentAlerts.stream()
          .forEach(currentAlert -> getRequests.add(new GetRequest((String) currentAlert.get(GUID),
              (String) currentAlert.get(config.getSourceTypeField()))));
      Iterable<Document> alerts = retrieveLatestDao.getAllLatest(getRequests);
      Map<Document, Optional<String>> updates = buildStatusChangeUpdates(metaAlert, alerts, status);
      update(updates);
    }
    return metaAlertUpdated;
  }

  /**
   * Given a Metaalert and a status change, builds the set of updates to be run.
   * @param metaAlert The metaalert to have status changed
   * @param alerts The alerts to change status for
   * @param status The status to change to
   * @return The updates to be run
   */
  protected Map<Document, Optional<String>> buildStatusChangeUpdates(Document metaAlert,
      Iterable<Document> alerts,
      MetaAlertStatus status) {
    metaAlert.getDocument().put(MetaAlertConstants.STATUS_FIELD, status.getStatusString());

    Map<Document, Optional<String>> updates = new HashMap<>();
    updates.put(metaAlert, Optional.of(config.getMetaAlertIndex()));

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
    return updates;
  }

  /**
   * Builds the updates to be run based on a given metaalert and a set of new alerts for the it.
   * @param metaAlert The base metaalert we're building updates for
   * @param alerts The alerts being added
   * @return The set of resulting updates.
   */
  protected Map<Document, Optional<String>> buildAddAlertToMetaAlertUpdates(Document metaAlert,
      Iterable<Document> alerts) {
    Map<Document, Optional<String>> updates = new HashMap<>();
    boolean metaAlertUpdated = addAlertsToMetaAlert(metaAlert, alerts);
    if (metaAlertUpdated) {
      MetaScores
          .calculateMetaScores(metaAlert, config.getThreatTriageField(), config.getThreatSort());
      updates.put(metaAlert, Optional.of(config.getMetaAlertIndex()));
      for (Document alert : alerts) {
        if (addMetaAlertToAlert(metaAlert.getGuid(), alert)) {
          updates.put(alert, Optional.empty());
        }
      }
    }
    return updates;
  }

  /**
   * Adds the provided alerts to a given metaalert.
   * @param metaAlert The metaalert to be given new children.
   * @param alerts The alerts to be added as children
   * @return True if metaalert is modified, false otherwise.
   */
  protected boolean addAlertsToMetaAlert(Document metaAlert, Iterable<Document> alerts) {
    boolean alertAdded = false;
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> currentAlerts = (List<Map<String, Object>>) metaAlert.getDocument()
        .get(MetaAlertConstants.ALERT_FIELD);
    if (currentAlerts == null) {
      currentAlerts = new ArrayList<>();
      metaAlert.getDocument().put(MetaAlertConstants.ALERT_FIELD, currentAlerts);
    }
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

  /**
   * Calls the single update variant if there's only one update, otherwise calls batch.
   * MetaAlerts may defer to an implementation specific IndexDao.
   * @param updates The list of updates to run
   * @throws IOException If there's an update error
   */
  protected void update(Map<Document, Optional<String>> updates)
      throws IOException {
    if (updates.size() == 1) {
      Entry<Document, Optional<String>> singleUpdate = updates.entrySet().iterator().next();
      updateDao.update(singleUpdate.getKey(), singleUpdate.getValue());
    } else if (updates.size() > 1) {
      updateDao.batchUpdate(updates);
    } // else we have no updates, so don't do anything
  }

}
