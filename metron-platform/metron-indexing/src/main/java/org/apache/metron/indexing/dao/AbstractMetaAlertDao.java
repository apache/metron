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
import java.util.stream.Collectors;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.metaalert.MetaScores;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.OriginalNotFoundException;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.apache.metron.stellar.common.utils.ConversionUtils;

/**
 * Abstract class providing helper functions for {@link org.apache.metron.indexing.dao.MetaAlertDao}
 * implementations.
 * </p>
 * This should provide default implementations for anything likely to be implementation
 * independent.
 */
public abstract class AbstractMetaAlertDao implements MetaAlertDao {

  protected IndexDao indexDao;
  protected String threatTriageField = THREAT_FIELD_DEFAULT;
  private static final String STATUS_PATH = "/status";
  private static final String ALERT_PATH = "/alert";

  /**
   * Defines which summary aggregation is used to represent the overall threat triage score for
   * the metaalert. The summary aggregation is applied to the threat triage score of all child
   * alert.
   * This overall score is primarily used for sorting; hence it is called the 'threatSort'.  This
   * can be either max, min, average, count, median, or sum.
   */
  protected String threatSort = THREAT_SORT_DEFAULT;
  protected int pageSize = 500;

  @Override
  public boolean addAlertsToMetaAlert(String metaAlertGuid, List<GetRequest> alertRequests)
      throws IOException {
    Map<Document, Optional<String>> updates = new HashMap<>();
    Document metaAlert = getLatest(metaAlertGuid, getMetAlertSensorName());
    if (MetaAlertStatus.ACTIVE.getStatusString()
        .equals(metaAlert.getDocument().get(STATUS_FIELD))) {
      Iterable<Document> alerts = getAllLatest(alertRequests);
      boolean metaAlertUpdated = addAlertsToMetaAlert(metaAlert, alerts);
      if (metaAlertUpdated) {
        calculateMetaScores(metaAlert);
        updates.put(metaAlert, Optional.of(getMetaAlertIndex()));
        for (Document alert : alerts) {
          if (addMetaAlertToAlert(metaAlert.getGuid(), alert)) {
            updates.put(alert, Optional.empty());
          }
        }
        indexDaoUpdate(updates);
      }
      return metaAlertUpdated;
    } else {
      throw new IllegalStateException("Adding alerts to an INACTIVE meta alert is not allowed");
    }
  }

  protected boolean addAlertsToMetaAlert(Document metaAlert, Iterable<Document> alerts) {
    boolean alertAdded = false;
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> currentAlerts = (List<Map<String, Object>>) metaAlert.getDocument()
        .get(ALERT_FIELD);
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

  @Override
  public boolean removeAlertsFromMetaAlert(String metaAlertGuid, List<GetRequest> alertRequests)
      throws IOException {
    Map<Document, Optional<String>> updates = new HashMap<>();
    Document metaAlert = getLatest(metaAlertGuid, METAALERT_TYPE);
    List<Map<String, Object>> alertsBefore = new ArrayList<>();
    alertsBefore
        .addAll((List<Map<String, Object>>) metaAlert.getDocument().get(MetaAlertDao.ALERT_FIELD));
    if (MetaAlertStatus.ACTIVE.getStatusString()
        .equals(metaAlert.getDocument().get(STATUS_FIELD))) {
      Iterable<Document> alerts = getAllLatest(alertRequests);
      Collection<String> alertGuids = alertRequests.stream().map(GetRequest::getGuid).collect(
          Collectors.toList());
      boolean metaAlertUpdated = removeAlertsFromMetaAlert(metaAlert, alertGuids);
      if (metaAlertUpdated) {
        List<Map<String, Object>> alertsAfter = (List<Map<String, Object>>) metaAlert.getDocument()
            .get(MetaAlertDao.ALERT_FIELD);
        // If we have no alerts left, we might need to handle the deletes manually. Thanks Solr.
        if (alertsAfter.size() < alertsBefore.size() && alertsAfter.size() == 0) {
          deleteRemainingMetaAlerts(alertsBefore);
        }
        calculateMetaScores(metaAlert);
        updates.put(metaAlert, Optional.of(getMetaAlertIndex()));
        for (Document alert : alerts) {
          if (removeMetaAlertFromAlert(metaAlert.getGuid(), alert)) {
            updates.put(alert, Optional.empty());
          }
        }
        indexDaoUpdate(updates);
      }
      return metaAlertUpdated;
    } else {
      throw new IllegalStateException("Removing alerts from an INACTIVE meta alert is not allowed");
    }
  }

  // Do nothing by default.  It's implementation weirdness can be handled.
  protected void deleteRemainingMetaAlerts(
      List<Map<String, Object>> alertsBefore) throws IOException {
  }

  protected boolean removeAlertsFromMetaAlert(Document metaAlert, Collection<String> alertGuids) {
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> currentAlerts = (List<Map<String, Object>>) metaAlert.getDocument()
        .get(ALERT_FIELD);
    int previousSize = currentAlerts.size();
    // Only remove an alert if it is in the meta alert
    currentAlerts.removeIf(currentAlert -> alertGuids.contains((String) currentAlert.get(GUID)));
    return currentAlerts.size() != previousSize;
  }

  protected boolean removeMetaAlertFromAlert(String metaAlertGuid, Document alert) {
    List<String> metaAlertField = new ArrayList<>();
    @SuppressWarnings("unchecked")
    List<String> alertField = (List<String>) alert.getDocument().get(MetaAlertDao.METAALERT_FIELD);
    if (alertField != null) {
      metaAlertField.addAll(alertField);
    }
    boolean metaAlertRemoved = metaAlertField.remove(metaAlertGuid);
    if (metaAlertRemoved) {
      alert.getDocument().put(MetaAlertDao.METAALERT_FIELD, metaAlertField);
    }
    return metaAlertRemoved;
  }

  @Override
  public boolean updateMetaAlertStatus(String metaAlertGuid, MetaAlertStatus status)
      throws IOException {
    Map<Document, Optional<String>> updates = new HashMap<>();
    Document metaAlert = getLatest(metaAlertGuid, METAALERT_TYPE);
    String currentStatus = (String) metaAlert.getDocument().get(MetaAlertDao.STATUS_FIELD);
    boolean metaAlertUpdated = !status.getStatusString().equals(currentStatus);
    if (metaAlertUpdated) {
      metaAlert.getDocument().put(MetaAlertDao.STATUS_FIELD, status.getStatusString());
      updates.put(metaAlert, Optional.of(getMetaAlertIndex()));
      List<GetRequest> getRequests = new ArrayList<>();
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> currentAlerts = (List<Map<String, Object>>) metaAlert.getDocument()
          .get(MetaAlertDao.ALERT_FIELD);
      currentAlerts.stream().forEach(currentAlert -> {
        getRequests.add(new GetRequest((String) currentAlert.get(GUID),
            (String) currentAlert.get(MetaAlertDao.SOURCE_TYPE)));
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
      indexDaoUpdate(updates);
    }
    return metaAlertUpdated;
  }

  @Override
  public Document getLatest(String guid, String sensorType) throws IOException {
    return indexDao.getLatest(guid, sensorType);
  }

  @Override
  public Iterable<Document> getAllLatest(List<GetRequest> getRequests) throws IOException {
    return indexDao.getAllLatest(getRequests);
  }

  @Override
  public abstract void update(Document update, Optional<String> index) throws IOException;

  @Override
  public void batchUpdate(Map<Document, Optional<String>> updates) {
    throw new UnsupportedOperationException("Meta alerts do not allow for bulk updates");
  }

  @Override
  public Map<String, FieldType> getColumnMetadata(List<String> indices)
      throws IOException {
    return indexDao.getColumnMetadata(indices);
  }

  /**
   * Does not allow patches on the "alerts" or "status" fields.  These fields must be updated with their
   * dedicated methods.
   *
   * @param request The patch request
   * @param timestamp Optionally a timestamp to set. If not specified then current time is used.
   * @throws OriginalNotFoundException
   * @throws IOException
   */
  @Override
  public void patch(PatchRequest request, Optional<Long> timestamp)
      throws OriginalNotFoundException, IOException {
    if (isPatchAllowed(request)) {
      Document d = getPatchedDocument(request, timestamp);
      indexDao.update(d, Optional.ofNullable(request.getIndex()));
    } else {
      throw new IllegalArgumentException(
          "Meta alert patches are not allowed for /alert or /status paths.  "
              + "Please use the add/remove alert or update status functions instead.");
    }
  }

  /**
   * Calls the single update variant if there's only one update, otherwise calls batch.
   * @param updates The list of updates to run
   * @throws IOException If there's an update error
   */
  protected void indexDaoUpdate(Map<Document, Optional<String>> updates) throws IOException {
    if (updates.size() == 1) {
      Entry<Document, Optional<String>> singleUpdate = updates.entrySet().iterator().next();
      indexDao.update(singleUpdate.getKey(), singleUpdate.getValue());
    } else if (updates.size() > 1) {
      indexDao.batchUpdate(updates);
    } // else we have no updates, so don't do anything
  }

  protected boolean addMetaAlertToAlert(String metaAlertGuid, Document alert) {
    List<String> metaAlertField = new ArrayList<>();
    @SuppressWarnings("unchecked")
    List<String> alertField = (List<String>) alert.getDocument().get(MetaAlertDao.METAALERT_FIELD);
    if (alertField != null) {
      metaAlertField.addAll(alertField);
    }

    boolean metaAlertAdded = !metaAlertField.contains(metaAlertGuid);
    if (metaAlertAdded) {
      metaAlertField.add(metaAlertGuid);
      alert.getDocument().put(MetaAlertDao.METAALERT_FIELD, metaAlertField);
    }
    return metaAlertAdded;
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
    metaSource.put(GROUPS_FIELD, groups);
    metaSource.put(STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString());

    return new Document(metaSource, guid, METAALERT_TYPE, System.currentTimeMillis());
  }

  protected Double parseThreatField(Object threatRaw) {
    Double threat = null;
    if (threatRaw instanceof Number) {
      threat = ((Number) threatRaw).doubleValue();
    } else if (threatRaw instanceof String) {
      threat = Double.parseDouble((String) threatRaw);
    }
    return threat;
  }

  /**
   * Calculate the meta alert scores for a Document. The scores are placed directly in the provided
   * document.
   * @param metaAlert The Document containing scores
   */
  @SuppressWarnings("unchecked")
  protected void calculateMetaScores(Document metaAlert) {
    MetaScores metaScores = new MetaScores(new ArrayList<>());
    List<Object> alertsRaw = ((List<Object>) metaAlert.getDocument().get(ALERT_FIELD));
    if (alertsRaw != null && !alertsRaw.isEmpty()) {
      ArrayList<Double> scores = new ArrayList<>();
      for (Object alertRaw : alertsRaw) {
        Map<String, Object> alert = (Map<String, Object>) alertRaw;
        Double scoreNum = parseThreatField(alert.get(threatTriageField));
        if (scoreNum != null) {
          scores.add(scoreNum);
        }
      }
      metaScores = new MetaScores(scores);
    }

    // add a summary (max, min, avg, ...) of all the threat scores from the child alerts
    metaAlert.getDocument().putAll(metaScores.getMetaScores());

    // add the overall threat score for the metaalert; one of the summary aggregations as defined
    // by `threatSort`
    Object threatScore = metaScores.getMetaScores().get(threatSort);

    // add the threat score as a float; type needs to match the threat score field from each of
    // the sensor indices
    metaAlert.getDocument()
        .put(threatTriageField, ConversionUtils.convert(threatScore, Float.class));
  }

  protected boolean isPatchAllowed(PatchRequest request) {
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
}
