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

import static org.apache.metron.common.Constants.GUID;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.constantScoreQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertDao;
import org.apache.metron.indexing.dao.MultiIndexDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConstants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.metaalert.MetaScores;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidCreateException;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.metron.indexing.dao.update.Document;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.InnerHitBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;

public class ElasticsearchMetaAlertDao implements MetaAlertDao {

  public static final String THREAT_TRIAGE_FIELD = MetaAlertConstants.THREAT_FIELD_DEFAULT
      .replace('.', ':');
  private static final String METAALERTS_INDEX = "metaalert_index";

  protected IndexDao indexDao;
  protected String metaAlertsIndex = METAALERTS_INDEX;
  protected String threatTriageField = THREAT_TRIAGE_FIELD;
  protected String threatSort = MetaAlertConstants.THREAT_SORT_DEFAULT;

  private ElasticsearchDao elasticsearchDao;

  protected int pageSize = 500;

  /**
   * Wraps an {@link org.apache.metron.indexing.dao.IndexDao} to handle meta alerts.
   * @param indexDao The Dao to wrap
   */
  public ElasticsearchMetaAlertDao(IndexDao indexDao) {
    this(indexDao, METAALERTS_INDEX, MetaAlertConstants.THREAT_FIELD_DEFAULT,
        MetaAlertConstants.THREAT_SORT_DEFAULT);
  }

  /**
   * Wraps an {@link org.apache.metron.indexing.dao.IndexDao} to handle meta alerts.
   * @param indexDao The Dao to wrap
   * @param triageLevelField The field name to use as the threat scoring field
   * @param threatSort The summary aggregation of all child threat triage scores used
   *                   as the overall threat triage score for the metaalert. This
   *                   can be either max, min, average, count, median, or sum.
   */
  public ElasticsearchMetaAlertDao(IndexDao indexDao, String metaAlertsIndex, String triageLevelField,
      String threatSort) {
    init(indexDao, Optional.of(threatSort));
    this.threatTriageField = triageLevelField;
    this.threatSort = threatSort;
    this.metaAlertsIndex = metaAlertsIndex;
  }

  public ElasticsearchMetaAlertDao() {
    //uninitialized.
  }

  /**
   * Initializes this implementation by setting the supplied IndexDao and also setting a separate ElasticsearchDao.
   * This is needed for some specific Elasticsearch functions (looking up an index from a GUID for example).
   * @param indexDao The DAO to wrap for our queries
   * @param threatSort The summary aggregation of the child threat triage scores used
   *                   as the overall threat triage score for the metaalert. This
   *                   can be either max, min, average, count, median, or sum.
   */
  @Override
  public void init(IndexDao indexDao, Optional<String> threatSort) {
    if (indexDao instanceof MultiIndexDao) {
      this.indexDao = indexDao;
      MultiIndexDao multiIndexDao = (MultiIndexDao) indexDao;
      for (IndexDao childDao : multiIndexDao.getIndices()) {
        if (childDao instanceof ElasticsearchDao) {
          this.elasticsearchDao = (ElasticsearchDao) childDao;
        }
      }
    } else if (indexDao instanceof ElasticsearchDao) {
      this.indexDao = indexDao;
      this.elasticsearchDao = (ElasticsearchDao) indexDao;
    } else {
      throw new IllegalArgumentException(
          "Need an ElasticsearchDao when using ElasticsearchMetaAlertDao"
      );
    }

    if (threatSort.isPresent()) {
      this.threatSort = threatSort.get();
    }
  }

  @Override
  public void init(AccessConfig config) {
    // Do nothing. We're just wrapping a child dao
  }

  @Override
  public SearchResponse getAllMetaAlertsForAlert(String guid) throws InvalidSearchException {
    if (guid == null || guid.trim().isEmpty()) {
      throw new InvalidSearchException("Guid cannot be empty");
    }
    // Searches for all alerts containing the meta alert guid in it's "metalerts" array
    QueryBuilder qb = boolQuery()
        .must(
            nestedQuery(
                MetaAlertConstants.ALERT_FIELD,
                boolQuery()
                    .must(termQuery(MetaAlertConstants.ALERT_FIELD + "." + GUID, guid)),
                ScoreMode.None
            ).innerHit(new InnerHitBuilder())
        )
        .must(termQuery(MetaAlertConstants.STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString()));
    return queryAllResults(qb);
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
    Iterable<Document> alerts = indexDao.getAllLatest(alertRequests);

    Document metaAlert = buildCreateDocument(alerts, request.getGroups(),
        MetaAlertConstants.ALERT_FIELD);
    MetaScores.calculateMetaScores(metaAlert, getThreatTriageField(), getThreatSort());
    // Add source type to be consistent with other sources and allow filtering
    metaAlert.getDocument().put(MetaAlertConstants.SOURCE_TYPE, MetaAlertConstants.METAALERT_TYPE);

    // Start a list of updates / inserts we need to run
    Map<Document, Optional<String>> updates = new HashMap<>();
    updates.put(metaAlert, Optional.of(getMetaAlertIndex()));

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


  @Override
  public SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException {
    // Wrap the query to also get any meta-alerts.
    QueryBuilder qb = constantScoreQuery(boolQuery()
        .must(boolQuery()
            .should(new QueryStringQueryBuilder(searchRequest.getQuery()))
            .should(nestedQuery(
                MetaAlertConstants.ALERT_FIELD,
                new QueryStringQueryBuilder(searchRequest.getQuery()),
                ScoreMode.None
                )
            )
        )
        // Ensures that it's a meta alert with active status or that it's an alert (signified by
        // having no status field)
        .must(boolQuery()
            .should(termQuery(MetaAlertConstants.STATUS_FIELD,
                MetaAlertStatus.ACTIVE.getStatusString()))
            .should(boolQuery().mustNot(existsQuery(MetaAlertConstants.STATUS_FIELD)))
        )
        .mustNot(existsQuery(MetaAlertConstants.METAALERT_FIELD))
    );
    return elasticsearchDao.search(searchRequest, qb);
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
          updates.put(metaAlert, Optional.of(METAALERTS_INDEX));
        }
      }

      // Run the alert's update
      indexDao.batchUpdate(updates);
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
    return queryAllResults(qb);
  }

  /**
   * Elasticsearch queries default to 10 records returned.  Some internal queries require that all
   * results are returned.  Rather than setting an arbitrarily high size, this method pages through results
   * and returns them all in a single SearchResponse.
   * @param qb
   * @return
   */
  protected SearchResponse queryAllResults(QueryBuilder qb) {
    SearchRequestBuilder searchRequestBuilder = elasticsearchDao
        .getClient()
        .prepareSearch(getMetaAlertIndex())
        .addStoredField("*")
        .setFetchSource(true)
        .setQuery(qb)
        .setSize(getPageSize());
    org.elasticsearch.action.search.SearchResponse esResponse = searchRequestBuilder
        .execute()
        .actionGet();
    List<SearchResult> allResults = getSearchResults(esResponse);
    long total = esResponse.getHits().getTotalHits();
    if (total > getPageSize()) {
      int pages = (int) (total / getPageSize()) + 1;
      for (int i = 1; i < pages; i++) {
        int from = i * getPageSize();
        searchRequestBuilder.setFrom(from);
        esResponse = searchRequestBuilder
            .execute()
            .actionGet();
        allResults.addAll(getSearchResults(esResponse));
      }
    }
    SearchResponse searchResponse = new SearchResponse();
    searchResponse.setTotal(total);
    searchResponse.setResults(allResults);
    return searchResponse;
  }

  /**
   * Transforms a list of Elasticsearch SearchHits to a list of SearchResults
   * @param searchResponse
   * @return
   */
  protected List<SearchResult> getSearchResults(
      org.elasticsearch.action.search.SearchResponse searchResponse) {
    return Arrays.stream(searchResponse.getHits().getHits()).map(searchHit -> {
          SearchResult searchResult = new SearchResult();
          searchResult.setId(searchHit.getId());
          searchResult.setSource(searchHit.getSource());
          searchResult.setScore(searchHit.getScore());
          searchResult.setIndex(searchHit.getIndex());
          return searchResult;
        }
    ).collect(Collectors.toList());
  }

  @Override
  public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
    // Wrap the query to hide any alerts already contained in meta alerts
    QueryBuilder qb = QueryBuilders.boolQuery()
        .must(new QueryStringQueryBuilder(groupRequest.getQuery()))
        .mustNot(existsQuery(MetaAlertConstants.METAALERT_FIELD));
    return elasticsearchDao.group(groupRequest, qb);
  }

  @Override
  public Map<String, FieldType> getColumnMetadata(List<String> indices) throws IOException {
    return getIndexDao().getColumnMetadata(indices);
  }

  @Override
  public int getPageSize() {
    return pageSize;
  }

  @Override
  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  @Override
  public IndexDao getIndexDao() {
    return indexDao;
  }

  @Override
  public String getThreatTriageField() {
    return threatTriageField;
  }

  @Override
  public String getThreatSort() {
    return threatSort;
  }

  @Override
  public String getMetAlertSensorName() {
    return MetaAlertConstants.METAALERT_TYPE;
  }

  @Override
  public String getMetaAlertIndex() {
    return metaAlertsIndex;
  }

  @Override
  public Document getLatest(String guid, String sensorType) throws IOException {
    return indexDao.getLatest(guid, sensorType);
  }

  @Override
  public Iterable<Document> getAllLatest(List<GetRequest> getRequests) throws IOException {
    return indexDao.getAllLatest(getRequests);
  }
}
