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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.AbstractMetaAlertDao;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MetaAlertDao;
import org.apache.metron.indexing.dao.MultiIndexDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
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
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CursorMarkParams;

public class SolrMetaAlertDao extends AbstractMetaAlertDao {
  private SolrDao solrDao;

  /**
   * Wraps an {@link org.apache.metron.indexing.dao.IndexDao} to handle meta alerts.
   * @param indexDao The Dao to wrap
   */
  public SolrMetaAlertDao(IndexDao indexDao) {
    this(indexDao, METAALERTS_INDEX, THREAT_FIELD_DEFAULT, THREAT_SORT_DEFAULT);
  }

  /**
   * Wraps an {@link org.apache.metron.indexing.dao.IndexDao} to handle meta alerts.
   * @param indexDao The Dao to wrap
   * @param triageLevelField The field name to use as the threat scoring field
   * @param threatSort The summary aggregation of all child threat triage scores used
   *                   as the overall threat triage score for the metaalert. This
   *                   can be either max, min, average, count, median, or sum.
   */
  public SolrMetaAlertDao(IndexDao indexDao, String index, String triageLevelField,
      String threatSort) {
    init(indexDao, Optional.of(threatSort));
    this.index = index;
    this.threatTriageField = triageLevelField;
  }

  public SolrMetaAlertDao() {
    //uninitialized.
  }

  // TODO Update this comment if necessary

  /**
   * Initializes this implementation by setting the supplied IndexDao and also setting a separate SolrDao.
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
        if (childDao instanceof SolrDao) {
          this.solrDao = (SolrDao) childDao;
        }
      }
    } else if (indexDao instanceof SolrDao) {
      this.indexDao = indexDao;
      this.solrDao = (SolrDao) indexDao;
    } else {
      throw new IllegalArgumentException(
          "Need a SolrDao when using SolrMetaAlertDao"
      );
    }

    if (threatSort.isPresent()) {
      this.threatSort = threatSort.get();
    }
  }

  @Override
  public int getPageSize() {
    return pageSize;
  }

  @Override
  public void setPageSize(int pageSize) {
    this.pageSize=pageSize;
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
    // TODO determine if we can split the sibling clauses into two .addFilterQuery calls
    SolrQuery solrQuery = new SolrQuery()
        .setQuery("*:*")
        .addFilterQuery("+" + STATUS_FIELD + ":" + MetaAlertStatus.ACTIVE.getStatusString()
            + "+{!parent which\"content_type:metaalert\"}"
            + "(alert:" + guid + ")")
        .addSort("id", SolrQuery.ORDER.asc); // Just do basic sorting to track where we are

    List<SearchResult> allResults = new ArrayList<>();
    try {
      String cursorMark = CursorMarkParams.CURSOR_MARK_START;
      boolean done = false;
      while (!done) {
        solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
        QueryResponse rsp = solrDao.getClient().query(solrQuery);
        String nextCursorMark = rsp.getNextCursorMark();
        rsp.getResults().stream()
            .map(solrDocument -> getSearchResult(solrDocument, Optional.empty()))
            .forEachOrdered(allResults::add);
        if (cursorMark.equals(nextCursorMark)) {
          done = true;
        }
        cursorMark = nextCursorMark;
      }
    } catch (IOException ioe) {
      // TODO fill this in with something reasonable
    } catch (SolrServerException e) {
      // TODO fill this in with something reasonable
    }

    SearchResponse searchResponse = new SearchResponse();
    searchResponse.setResults(allResults);
    searchResponse.setTotal(allResults.size());
    return searchResponse;
  }

  // TODO this is largely identical to ES.  Should refactor to share
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
    Iterable<Document> alerts = indexDao.getAllLatest(alertRequests);

    Document metaAlert = buildCreateDocument(alerts, request.getGroups());
    calculateMetaScores(metaAlert);

    // Add source type to be consistent with other sources and allow filtering
    metaAlert.getDocument().put(Constants.SENSOR_TYPE, MetaAlertDao.METAALERT_TYPE);

    // Start a list of updates / inserts we need to run
    Map<Document, Optional<String>> updates = new HashMap<>();
    updates.put(metaAlert, Optional.of(MetaAlertDao.METAALERTS_INDEX));

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
            // TODO Figure out what to do for Solr here? Do we need to create a collection equivalent?
            // Look up the index from Elasticsearch if one is not supplied in the request
//            index = elasticsearchDao.getIndexName(alert.getGuid(), guidToSensorTypes.get(alert.getGuid()));
//            if (!index.isPresent()) {
//              throw new IllegalArgumentException("Could not find index for " + alert.getGuid());
//            }
          }
          updates.put(alert, index);
        }
      }

      // Kick off any updates.
      indexDaoUpdate(updates);

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
    // Need to wrap such that two things are true
    // 1. The provided query is true OR nested query on the alert field is true
    // 2. Metaalert is active OR it's not a metaalert

//    String baseQuery = searchRequest.getQuery();
//    String potentialMatch = baseQuery +

    SolrQuery query = new SolrQuery()
        .setQuery(searchRequest.getQuery() + "OR ");
    return null;




//    // Wrap the query to also get any meta-alerts.
//    QueryBuilder qb = constantScoreQuery(boolQuery()
//        .must(boolQuery()
//            .should(new QueryStringQueryBuilder(searchRequest.getQuery()))
//            .should(nestedQuery(
//                ALERT_FIELD,
//                new QueryStringQueryBuilder(searchRequest.getQuery()),
//                ScoreMode.None
//                )
//            )
//        )
//        // Ensures that it's a meta alert with active status or that it's an alert (signified by
//        // having no status field)
//        .must(boolQuery()
//            .should(termQuery(MetaAlertDao.STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString()))
//            .should(boolQuery().mustNot(existsQuery(MetaAlertDao.STATUS_FIELD)))
//        )
//        .mustNot(existsQuery(MetaAlertDao.METAALERT_FIELD))
//    );
//    return elasticsearchDao.search(searchRequest, qb);
  }

  @Override
  public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
    return null;
  }

  @Override
  public void update(Document update, Optional<String> index) throws IOException {

  }

  // TODO refactor to remove this duplication from SolrSearchDao
  private SearchResult getSearchResult(SolrDocument solrDocument, Optional<List<String>> fields) {
    SearchResult searchResult = new SearchResult();
    searchResult.setId((String) solrDocument.getFieldValue(Constants.GUID));
    Map<String, Object> source;
    if (fields.isPresent()) {
      source = new HashMap<>();
      fields.get().forEach(field -> source.put(field, solrDocument.getFieldValue(field)));
    } else {
      source = solrDocument.getFieldValueMap();
    }
    searchResult.setSource(source);
    return searchResult;
  }
}
