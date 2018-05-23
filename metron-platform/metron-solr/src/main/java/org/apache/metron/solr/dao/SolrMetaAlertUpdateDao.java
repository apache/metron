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

import static org.apache.metron.solr.dao.SolrMetaAlertDao.METAALERTS_COLLECTION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConfig;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConstants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.metaalert.MetaAlertUpdateDao;
import org.apache.metron.indexing.dao.metaalert.MetaScores;
import org.apache.metron.indexing.dao.metaalert.lucene.AbstractLuceneMetaAlertUpdateDao;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.InvalidCreateException;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.UpdateDao;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;

public class SolrMetaAlertUpdateDao extends AbstractLuceneMetaAlertUpdateDao implements
    MetaAlertUpdateDao, UpdateDao {

  private SolrClient solrClient;
  private SolrMetaAlertSearchDao metaAlertSearchDao;

  /**
   * Constructor a SolrMetaAlertUpdateDao
   * @param solrDao An SolrDao to defer queries to.
   * @param metaAlertSearchDao A MetaAlert aware search DAO used in retrieving items being mutated.
   * @param retrieveLatestDao A RetrieveLatestDao for getting the current state of items being
   *     mutated.
   */
  public SolrMetaAlertUpdateDao(SolrDao solrDao,
      SolrMetaAlertSearchDao metaAlertSearchDao,
      SolrMetaAlertRetrieveLatestDao retrieveLatestDao,
      MetaAlertConfig config) {
    super(solrDao, retrieveLatestDao, config);
    this.solrClient = solrDao.getSolrClient(solrDao.getZkHost());
    this.metaAlertSearchDao = metaAlertSearchDao;
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
    Iterable<Document> alerts = getRetrieveLatestDao().getAllLatest(alertRequests);

    Document metaAlert = buildCreateDocument(alerts, request.getGroups(),
        MetaAlertConstants.ALERT_FIELD);
    MetaScores.calculateMetaScores(metaAlert, getConfig().getThreatTriageField(),
        getConfig().getThreatSort());

    // Add source type to be consistent with other sources and allow filtering
    metaAlert.getDocument().put(Constants.SENSOR_TYPE, MetaAlertConstants.METAALERT_TYPE);

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
    getUpdateDao().batchUpdate(updates);

    try {
      solrClient.commit(METAALERTS_COLLECTION);
      if (collection.isPresent()) {
        solrClient.commit(collection.get());
      }
    } catch (SolrServerException e) {
      throw new IOException("Unable to update document", e);
    }
  }

  protected boolean replaceAlertInMetaAlert(Document metaAlert, Document alert) {
    boolean metaAlertUpdated = removeAlertsFromMetaAlert(metaAlert,
        Collections.singleton(alert.getGuid()));
    if (metaAlertUpdated) {
      addAlertsToMetaAlert(metaAlert, Collections.singleton(alert));
    }
    return metaAlertUpdated;
  }

  @Override
  public boolean addAlertsToMetaAlert(String metaAlertGuid, List<GetRequest> alertRequests)
      throws IOException {
    boolean success;
    Document metaAlert = getRetrieveLatestDao()
        .getLatest(metaAlertGuid, MetaAlertConstants.METAALERT_TYPE);
    if (MetaAlertStatus.ACTIVE.getStatusString()
        .equals(metaAlert.getDocument().get(MetaAlertConstants.STATUS_FIELD))) {
      Iterable<Document> alerts = getRetrieveLatestDao().getAllLatest(alertRequests);
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
}
