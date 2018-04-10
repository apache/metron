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

package org.apache.metron.solr.dao;

import static org.apache.metron.common.Constants.GUID;
import static org.apache.metron.solr.dao.SolrMetaAlertDao.METAALERTS_COLLECTION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.RetrieveLatestDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConstants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.metaalert.MetaAlertSearchDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.metaalert.MetaAlertUpdateDao;
import org.apache.metron.indexing.dao.metaalert.MetaScores;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.InvalidCreateException;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.OriginalNotFoundException;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.apache.metron.indexing.dao.update.UpdateDao;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;

public class SolrMetaAlertUpdateDao implements MetaAlertUpdateDao, UpdateDao {

  private SolrClient solrClient;
  private UpdateDao updateDao;
  private MetaAlertSearchDao metaAlertSearchDao;
  private RetrieveLatestDao retrieveLatestDao;
  private static final String CHILD_DOCUMENTS = "_childDocuments_";

  public SolrMetaAlertUpdateDao(SolrClient solrClient, UpdateDao updateDao,
      MetaAlertSearchDao metaAlertSearchDao, RetrieveLatestDao retrieveLatestDao) {
    this.solrClient = solrClient;
    this.updateDao = updateDao;
    this.metaAlertSearchDao = metaAlertSearchDao;
    this.retrieveLatestDao = retrieveLatestDao;
  }

  @Override
  public MetaAlertCreateResponse createMetaAlert(MetaAlertCreateRequest request)
      throws InvalidCreateException, IOException {
    List<GetRequest> alertRequests = request.getAlerts();
    if (request.getAlerts().isEmpty()) {
      throw new InvalidCreateException("MetaAlertCreateRequest must contain alerts");
    }
    if (request.getGroups().isEmpty()) {
      throw new InvalidCreateException("MetaAlertCreateRequest must contain UI groups");
    }

    // Retrieve the documents going into the meta alert and build it
    Iterable<Document> alerts = retrieveLatestDao.getAllLatest(alertRequests);

    Document metaAlert = buildCreateDocument(alerts, request.getGroups(), CHILD_DOCUMENTS);
    MetaScores.calculateMetaScores(metaAlert, threatTriageField, threatSort);

    // Add source type to be consistent with other sources and allow filtering
    metaAlert.getDocument().put(MetaAlertConstants.SOURCE_TYPE, MetaAlertConstants.METAALERT_TYPE);

    // Start a list of updates / inserts we need to run
    Map<Document, Optional<String>> updates = new HashMap<>();
    updates.put(metaAlert, Optional.of(METAALERTS_COLLECTION));

    try {
      // We need to update the associated alerts with the new meta alerts, making sure existing
      // links are maintained.
      Map<String, Optional<String>> guidToIndices = alertRequests.stream().collect(Collectors.toMap(
          GetRequest::getGuid, GetRequest::getIndex));
      Map<String, String> guidToSensorTypes = alertRequests.stream().collect(Collectors.toMap(
          GetRequest::getGuid, GetRequest::getSensorType));
      for (Document alert : alerts) {
        if (addMetaAlertToAlert(metaAlert.getGuid(), alert)) {
          // Use the index in the request if it exists
          Optional<String> index = guidToIndices.get(alert.getGuid());
          if (!index.isPresent()) {
            index = Optional.ofNullable(guidToSensorTypes.get(alert.getGuid()));
            if (!index.isPresent()) {
              throw new IllegalArgumentException("Could not find index for " + alert.getGuid());
            }
          }
          updates.put(alert, index);
        }
      }

      // Kick off any updates.
      update(updates);

      MetaAlertCreateResponse createResponse = new MetaAlertCreateResponse();
      createResponse.setCreated(true);
      createResponse.setGuid(metaAlert.getGuid());
      solrClient.commit(METAALERTS_COLLECTION);
      return createResponse;
    } catch (IOException | SolrServerException e) {
      throw new InvalidCreateException("Unable to create meta alert", e);
    }
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
   * Updates a document in Solr for a given collection.  Collection is not optional for Solr.
   * @param update The update to be run
   * @param collection The index to be updated. Mandatory for Solr
   * @throws IOException Thrown when an error occurs during the write.
   */
  @Override
  public void update(Document update, Optional<String> collection) throws IOException {
    if (MetaAlertConstants.METAALERT_TYPE.equals(update.getSensorType())) {
      // We've been passed an update to the meta alert.
      throw new UnsupportedOperationException("Meta alerts cannot be directly updated");
    }
    // Index can't be optional, or it won't be committed

    Map<Document, Optional<String>> updates = new HashMap<>();
    updates.put(update, collection);

    // We need to update an alert itself. It cannot be delegated in Solr; we need to retrieve all
    // metaalerts and update the entire document for each.
    SearchResponse searchResponse;
    try {
      searchResponse = metaAlertSearchDao.getAllMetaAlertsForAlert(update.getGuid());
    } catch (InvalidSearchException e) {
      throw new IOException("Unable to retrieve metaalerts for alert", e);
    }

    ArrayList<Document> metaAlerts = new ArrayList<>();
    for (SearchResult searchResult : searchResponse.getResults()) {
      Document doc = new Document(searchResult.getSource(), searchResult.getId(),
          MetaAlertConstants.METAALERT_TYPE, 0L);
      metaAlerts.add(doc);
    }

    for (Document metaAlert : metaAlerts) {
      if (replaceAlertInMetaAlert(metaAlert, update)) {
        updates.put(metaAlert, Optional.of(METAALERTS_COLLECTION));
      }
    }

    // Run the alert's update
    updateDao.batchUpdate(updates);

    try {
      solrClient.commit(METAALERTS_COLLECTION);
      if (collection.isPresent()) {
        solrClient.commit(collection.get());
      }
    } catch (SolrServerException e) {
      throw new IOException("Unable to update document", e);
    }
  }

  @Override
  public void batchUpdate(Map<Document, Optional<String>> updates) {
    throw new UnsupportedOperationException("Meta alerts do not allow for bulk updates");
  }

  protected boolean replaceAlertInMetaAlert(Document metaAlert, Document alert) {
    boolean metaAlertUpdated = removeAlertsFromMetaAlert(metaAlert,
        Collections.singleton(alert.getGuid()));
    if (metaAlertUpdated) {
      addAlertsToMetaAlert(metaAlert, Collections.singleton(alert));
    }
    return metaAlertUpdated;
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

  @Override
  public boolean addAlertsToMetaAlert(String metaAlertGuid, List<GetRequest> alertRequests)
      throws IOException {
    boolean success;
    Document metaAlert = retrieveLatestDao.getLatest(metaAlertGuid, metaAlertSensor);
    if (MetaAlertStatus.ACTIVE.getStatusString()
        .equals(metaAlert.getDocument().get(MetaAlertConstants.STATUS_FIELD))) {
      Iterable<Document> alerts = retrieveLatestDao.getAllLatest(alertRequests);
      Map<Document, Optional<String>> updates = buildAddAlertToMetaAlertUpdates(metaAlert, alerts);
      update(updates);
      success = updates.size() != 0;
    } else {
      throw new IllegalStateException("Adding alerts to an INACTIVE meta alert is not allowed");
    }
    try {
      solrClient.commit(METAALERTS_COLLECTION);
    } catch (SolrServerException e) {
      throw new IOException("Unable to commit alerts to metaalert: " + metaAlertGuid, e);
    }
    return success;
  }

  @SuppressWarnings("unchecked")
  public boolean removeAlertsFromMetaAlert(String metaAlertGuid, List<GetRequest> alertRequests)
      throws IOException {
    Document metaAlert = retrieveLatestDao
        .getLatest(metaAlertGuid, MetaAlertConstants.METAALERT_TYPE);
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
      // If we have no alerts left, we might need to handle the deletes manually. Thanks Solr.
      if (alertsAfter.size() < alertsBefore.size() && alertsAfter.size() == 0) {
        deleteRemainingMetaAlerts(alertsBefore);
      }
      MetaScores.calculateMetaScores(metaAlert, threatTriageField, threatSort);
      updates.put(metaAlert, Optional.of(metaAlertIndex));
      for (Document alert : alerts) {
        if (removeMetaAlertFromAlert(metaAlert.getGuid(), alert)) {
          updates.put(alert, Optional.empty());
        }
      }
    }
    return updates;
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
  public void patch(PatchRequest request, Optional<Long> timestamp)
      throws OriginalNotFoundException, IOException {
    if (isPatchAllowed(request)) {
      updateDao.patch(request, timestamp);
    } else {
      throw new IllegalArgumentException(
          "Meta alert patches are not allowed for /alert or /status paths.  "
              + "Please use the add/remove alert or update status functions instead.");
    }
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
              (String) currentAlert.get(MetaAlertConstants.SOURCE_TYPE))));
      Iterable<Document> alerts = retrieveLatestDao.getAllLatest(getRequests);
      Map<Document, Optional<String>> updates = buildStatusChangeUpdates(metaAlert, alerts, status);
      update(updates);
    }
    return metaAlertUpdated;
  }

  protected void deleteRemainingMetaAlerts(List<Map<String, Object>> alertsBefore)
      throws IOException {
    List<String> guidsToDelete = new ArrayList<>();
    for (Map<String, Object> alert : alertsBefore) {
      guidsToDelete.add((String) alert.get(Constants.GUID));
    }
    try {
      solrClient.deleteById(metaAlertIndex, guidsToDelete);
    } catch (SolrServerException | IOException e) {
      throw new IOException("Unable to delete metaalert child alerts", e);
    }
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
    updates.put(metaAlert, Optional.of(metaAlertIndex));

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
      MetaScores.calculateMetaScores(metaAlert, threatTriageField, threatSort);
      updates.put(metaAlert, Optional.of(metaAlertIndex));
      for (Document alert : alerts) {
        if (addMetaAlertToAlert(metaAlert.getGuid(), alert)) {
          updates.put(alert, Optional.empty());
        }
      }
    }
    return updates;
  }
}
