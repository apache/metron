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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections.MapUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
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
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CursorMarkParams;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class SolrMetaAlertDao extends AbstractMetaAlertDao {

  private static final String METAALERTS_COLLECTION = "metaalert";

  private static final String CHILD_DOCUMENTS = "_childDocuments_";

  private SolrDao solrDao;

  /**
   * Wraps an {@link org.apache.metron.indexing.dao.IndexDao} to handle meta alerts.
   * @param indexDao The Dao to wrap
   */
  public SolrMetaAlertDao(IndexDao indexDao) {
    this(indexDao, METAALERTS_COLLECTION, THREAT_FIELD_DEFAULT, THREAT_SORT_DEFAULT);
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
  public boolean addAlertsToMetaAlert(String metaAlertGuid, List<GetRequest> alertRequests)
      throws IOException {
    boolean result = super.addAlertsToMetaAlert(metaAlertGuid, alertRequests);
    try {
      solrDao.getClient().commit(METAALERTS_COLLECTION);
    } catch (SolrServerException e) {
      throw new IOException("Unable to commit alerts to metaalert: " + metaAlertGuid, e);
    }
    return result;
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
  public String getMetAlertSensorName() {
    // TODO determine if this is right.
    // Pretty sure the actually correct answer is to kill this and make METAALERT_TYPE work for queries
    // Just return the collection name instead of a type (since Solr doesn't have one)
    return METAALERTS_COLLECTION;
  }

  @Override
  public String getMetaAlertIndex() {
    return METAALERTS_COLLECTION;
  }

  @Override
  public void init(AccessConfig config) {
    // Do nothing. We're just wrapping a child dao
  }

  @Override
  public Document getLatest(String guid, String sensorType) throws IOException {
    if (getMetAlertSensorName().equals(sensorType)) {
      // Unfortunately, we can't just defer to the indexDao for this. Child alerts in Solr end up having
      // to be dug out.
      String guidClause = Constants.GUID + ":" + guid;
      SolrQuery query = new SolrQuery();
      query.setQuery(guidClause)
          .setFields("*", "[child parentFilter=" + guidClause + " limit=999]");

      SolrClient client = solrDao.getClient();
      try {
        QueryResponse response = client.query(METAALERTS_COLLECTION, query);
        // GUID is unique, so it's definitely the first result
        if (response.getResults().size() == 1) {
          SolrDocument result = response.getResults().get(0);

          return SolrUtilities.toDocument(result);
        } else {
          return null;
        }
      } catch (SolrServerException e) {
        throw new IOException("Unable to retrieve metaalert", e);
      }
    } else {
      return indexDao.getLatest(guid, sensorType);
    }
  }

  @Override
  public SearchResponse getAllMetaAlertsForAlert(String guid) throws InvalidSearchException {
    if (guid == null || guid.trim().isEmpty()) {
      throw new InvalidSearchException("Guid cannot be empty");
    }

    // Searches for all alerts containing the meta alert guid in it's "metalerts" array
    // The query has to match the parentFilter to avoid errors.  Guid must also be explicitly
    // included.

//    {!parent which=status:active}guid:message_0
    String activeClause = STATUS_FIELD + ":" + MetaAlertStatus.ACTIVE.getStatusString();
    String guidClause = Constants.GUID + ":" + guid;
    String fullClause = "{!parent which=" + activeClause + "}" + guidClause;
    SolrQuery solrQuery = new SolrQuery()
        .setQuery(fullClause)
        .setFields("*", "[child parentFilter=" + activeClause + " limit=999]")
        .addSort(Constants.GUID,
            SolrQuery.ORDER.asc); // Just do basic sorting to track where we are

    List<SearchResult> allResults = new ArrayList<>();
    try {
      String cursorMark = CursorMarkParams.CURSOR_MARK_START;
      boolean done = false;
      while (!done) {
        solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
        QueryResponse rsp = solrDao.getClient().query(METAALERTS_COLLECTION, solrQuery);
        String nextCursorMark = rsp.getNextCursorMark();
        rsp.getResults().stream()
            .map(solrDocument -> SolrUtilities.getSearchResult(solrDocument, Optional.empty()))
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

    Document metaAlert = buildCreateDocument(alerts, request.getGroups(), CHILD_DOCUMENTS);
    calculateMetaScores(metaAlert);

    // Add source type to be consistent with other sources and allow filtering
    metaAlert.getDocument().put(Constants.SENSOR_TYPE, MetaAlertDao.METAALERT_TYPE);

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
            // TODO Figure out what to do for Solr here? Do we need to create a collection equivalent?
            // Look up the index from Elasticsearch if one is not supplied in the request
            index = Optional.ofNullable(guidToSensorTypes.get(alert.getGuid()));
//            index = elasticsearchDao.getIndexName(alert.getGuid(), guidToSensorTypes.get(alert.getGuid()));
            if (!index.isPresent()) {
              throw new IllegalArgumentException("Could not find index for " + alert.getGuid());
            }
          }
          updates.put(alert, index);
        }
      }

      // Kick off any updates.
      indexDaoUpdate(updates);

      MetaAlertCreateResponse createResponse = new MetaAlertCreateResponse();
      createResponse.setCreated(true);
      createResponse.setGuid(metaAlert.getGuid());
      solrDao.getClient().commit(METAALERTS_COLLECTION);
      return createResponse;
    } catch (IOException | SolrServerException e) {
      throw new InvalidCreateException("Unable to create meta alert", e);
    }
  }

  @Override
  public SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException {
    // Need to wrap such that two things are true
    // 1. The provided query is true OR nested query on the alert field is true
    // 2. Metaalert is active OR it's not a metaalert

    // TODO Do I need to worry about nested?
    String statusClause =
        MetaAlertDao.STATUS_FIELD + ":(" + MetaAlertStatus.ACTIVE.getStatusString()
            + " OR (*:* -[* TO *]))";
    String query = searchRequest.getQuery() + " +" + "(" + statusClause + ")";

    searchRequest.setQuery(query);
    return indexDao.search(searchRequest);
  }

  @Override
  public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
    return null;
  }

  @Override
  public void update(Document update, Optional<String> index) throws IOException {
    if (METAALERT_TYPE.equals(update.getSensorType())) {
      // We've been passed an update to the meta alert.
      throw new UnsupportedOperationException("Meta alerts cannot be directly updated");
    }
    // TODO I don't know of a way to avoid knowing the collection.  Which means that
    // index can't be optional, or it won't be committed

    Map<Document, Optional<String>> updates = new HashMap<>();
    updates.put(update, index);

    // We need to update an alert itself. It cannot be delegated in Solr; we need to retrieve all
    // metaalerts and update the entire document for each.
    SearchResponse searchResponse;
    try {
      searchResponse = getAllMetaAlertsForAlert(update.getGuid());
    } catch (InvalidSearchException e) {
      throw new IOException("Unable to retrieve metaalerts for alert", e);
    }

    ArrayList<Document> metaAlerts = new ArrayList<>();
    for (SearchResult searchResult : searchResponse.getResults()) {
      Document doc = new Document(searchResult.getSource(), searchResult.getId(),
          METAALERT_TYPE, 0L);
      metaAlerts.add(doc);
    }

    for (Document metaAlert : metaAlerts) {
      if (replaceAlertInMetaAlert(metaAlert, update)) {
        updates.put(metaAlert, Optional.of(METAALERTS_COLLECTION));
      }
    }

    // Run the alert's update
    indexDao.batchUpdate(updates);

    try {
      solrDao.getClient().commit(METAALERTS_COLLECTION);
      // TODO remove this
      solrDao.getClient().commit("test");
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
  public Iterable<Document> getAllLatest(List<GetRequest> getRequests) throws IOException {
    return indexDao.getAllLatest(getRequests);
  }

//  @Override
//  public boolean updateMetaAlertStatus(String metaAlertGuid, MetaAlertStatus status)
//      throws IOException {
//    boolean retVal = super.updateMetaAlertStatus(metaAlertGuid, status);
//    try {
//      solrDao.getClient().commit(METAALERTS_COLLECTION);
//      return retVal;
//    } catch (SolrServerException e) {
//      throw new IOException("Unable to commit", e);
//    }
//  }
}
