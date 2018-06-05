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

package org.apache.metron.elasticsearch.dao;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.metron.common.Constants;
import org.apache.metron.elasticsearch.utils.ElasticsearchUtils;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConfig;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConstants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.metaalert.MetaAlertRetrieveLatestDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.metaalert.MetaScores;
import org.apache.metron.indexing.dao.metaalert.lucene.AbstractLuceneMetaAlertUpdateDao;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.InvalidCreateException;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.update.CommentAddRemoveRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.elasticsearch.index.query.InnerHitBuilder;
import org.elasticsearch.index.query.QueryBuilder;

public class ElasticsearchMetaAlertUpdateDao extends AbstractLuceneMetaAlertUpdateDao {

  private ElasticsearchDao elasticsearchDao;
  private MetaAlertRetrieveLatestDao retrieveLatestDao;
  private int pageSize;

  /**
   * Constructor an ElasticsearchMetaAlertUpdateDao
   * @param elasticsearchDao An UpdateDao to defer queries to.
   * @param retrieveLatestDao A RetrieveLatestDao for getting the current state of items being
   *     mutated.
   * @param config The meta alert config to use.
   */
  public ElasticsearchMetaAlertUpdateDao(
      ElasticsearchDao elasticsearchDao,
      MetaAlertRetrieveLatestDao retrieveLatestDao,
      MetaAlertConfig config,
      int pageSize
  ) {
    super(elasticsearchDao, retrieveLatestDao, config);
    this.elasticsearchDao = elasticsearchDao;
    this.retrieveLatestDao = retrieveLatestDao;
    this.pageSize = pageSize;
  }

  @Override
  @SuppressWarnings("unchecked")
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

    Document metaAlert = buildCreateDocument(alerts, request.getGroups(),
        MetaAlertConstants.ALERT_FIELD);
    MetaScores
        .calculateMetaScores(metaAlert, getConfig().getThreatTriageField(),
            getConfig().getThreatSort());
    // Add source type to be consistent with other sources and allow filtering
    metaAlert.getDocument()
        .put(ElasticsearchMetaAlertDao.SOURCE_TYPE_FIELD, MetaAlertConstants.METAALERT_TYPE);

    // Start a list of updates / inserts we need to run
    Map<Document, Optional<String>> updates = new HashMap<>();
    updates.put(metaAlert, Optional.of(getConfig().getMetaAlertIndex()));

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
            // Look up the index from Elasticsearch if one is not supplied in the request
            index = elasticsearchDao
                .getIndexName(alert.getGuid(), guidToSensorTypes.get(alert.getGuid()));
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
      return createResponse;
    } catch (IOException ioe) {
      throw new InvalidCreateException("Unable to create meta alert", ioe);
    }
  }

  /**
   * Adds alerts to a metaalert, based on a list of GetRequests provided for retrieval.
   * @param metaAlertGuid The GUID of the metaalert to be given new children.
   * @param alertRequests GetRequests for the appropriate alerts to add.
   * @return True if metaalert is modified, false otherwise.
   */
  public boolean addAlertsToMetaAlert(String metaAlertGuid, List<GetRequest> alertRequests)
      throws IOException {

    Document metaAlert = retrieveLatestDao
        .getLatest(metaAlertGuid, MetaAlertConstants.METAALERT_TYPE);
    if (MetaAlertStatus.ACTIVE.getStatusString()
        .equals(metaAlert.getDocument().get(MetaAlertConstants.STATUS_FIELD))) {
      Iterable<Document> alerts = retrieveLatestDao.getAllLatest(alertRequests);
      Map<Document, Optional<String>> updates = buildAddAlertToMetaAlertUpdates(metaAlert, alerts);
      update(updates);
      return updates.size() != 0;
    } else {
      throw new IllegalStateException("Adding alerts to an INACTIVE meta alert is not allowed");
    }
  }

  @Override
  public void update(Document update, Optional<String> index) throws IOException {
    if (MetaAlertConstants.METAALERT_TYPE.equals(update.getSensorType())) {
      // We've been passed an update to the meta alert.
      throw new UnsupportedOperationException("Meta alerts cannot be directly updated");
    } else {
      Map<Document, Optional<String>> updates = new HashMap<>();
      updates.put(update, index);
      // We need to update an alert itself.  Only that portion of the update can be delegated.
      // We still need to get meta alerts potentially associated with it and update.
      Collection<Document> metaAlerts = getMetaAlertsForAlert(update.getGuid()).getResults()
          .stream()
          .map(searchResult -> new Document(searchResult.getSource(), searchResult.getId(),
              MetaAlertConstants.METAALERT_TYPE, 0L))
          .collect(Collectors.toList());
      // Each meta alert needs to be updated with the new alert
      for (Document metaAlert : metaAlerts) {
        if (replaceAlertInMetaAlert(metaAlert, update)) {
          updates.put(metaAlert, Optional.of(getConfig().getMetaAlertIndex()));
        }
      }

      // Run the alert's update
      elasticsearchDao.batchUpdate(updates);
    }
  }

  @Override
  public void addCommentToAlert(CommentAddRemoveRequest request) throws IOException {
    getUpdateDao().addCommentToAlert(request);
  }

  @Override
  public void removeCommentFromAlert(CommentAddRemoveRequest request) throws IOException {
    getUpdateDao().removeCommentFromAlert(request);
  }

  @Override
  public void addCommentToAlert(CommentAddRemoveRequest request, Document latest)
      throws IOException {
    getUpdateDao().addCommentToAlert(request, latest);
  }

  @Override
  public void removeCommentFromAlert(CommentAddRemoveRequest request, Document latest)
      throws IOException {
    getUpdateDao().removeCommentFromAlert(request, latest);
  }

  /**
   * Given an alert GUID, retrieve all associated meta alerts.
   * @param alertGuid The GUID of the child alert
   * @return The Elasticsearch response containing the meta alerts
   */
  protected SearchResponse getMetaAlertsForAlert(String alertGuid) {
    QueryBuilder qb = boolQuery()
        .must(
            nestedQuery(
                MetaAlertConstants.ALERT_FIELD,
                boolQuery()
                    .must(termQuery(MetaAlertConstants.ALERT_FIELD + "." + Constants.GUID,
                        alertGuid)),
                ScoreMode.None
            ).innerHit(new InnerHitBuilder())
        )
        .must(termQuery(MetaAlertConstants.STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString()));
    return ElasticsearchUtils
        .queryAllResults(elasticsearchDao.getClient(), qb, getConfig().getMetaAlertIndex(),
            pageSize);
  }


  protected boolean replaceAlertInMetaAlert(Document metaAlert, Document alert) {
    boolean metaAlertUpdated = removeAlertsFromMetaAlert(metaAlert,
        Collections.singleton(alert.getGuid()));
    if (metaAlertUpdated) {
      addAlertsToMetaAlert(metaAlert, Collections.singleton(alert));
    }
    return metaAlertUpdated;
  }
}
