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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertDao;
import org.apache.metron.indexing.dao.MultiIndexDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConstants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.metaalert.MetaAlertUpdateDao;
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
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CursorMarkParams;

public class SolrMetaAlertDao implements MetaAlertDao {

  private static final String METAALERTS_COLLECTION = "metaalert";
  private static final String CHILD_DOCUMENTS = "_childDocuments_";

  private IndexDao indexDao;
  private SolrDao solrDao;
  protected String metaAlertsCollection = METAALERTS_COLLECTION;
  protected String threatTriageField = MetaAlertConstants.THREAT_FIELD_DEFAULT;
  protected String threatSort = MetaAlertConstants.THREAT_SORT_DEFAULT;

  protected int pageSize = 500;

  /**
   * Wraps an {@link org.apache.metron.indexing.dao.IndexDao} to handle meta alerts.
   * @param indexDao The Dao to wrap
   */
  public SolrMetaAlertDao(IndexDao indexDao) {
    this(indexDao, METAALERTS_COLLECTION, MetaAlertConstants.THREAT_FIELD_DEFAULT,
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
  public SolrMetaAlertDao(IndexDao indexDao, String metaAlertsCollection, String triageLevelField,
      String threatSort) {
    init(indexDao, Optional.of(threatSort));
    this.metaAlertsCollection = metaAlertsCollection;
    this.threatTriageField = triageLevelField;
    this.threatSort = threatSort;
  }

  public SolrMetaAlertDao() {
    //uninitialized.
  }

  /**
   * Initializes this implementation by setting the supplied IndexDao and also setting a separate SolrDao.
   * This is needed for some specific Solr functions (looking up an index from a GUID for example).
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
    boolean result = MetaAlertDao.super.addAlertsToMetaAlert(metaAlertGuid, alertRequests);
    try {
      solrDao.getClient().commit(METAALERTS_COLLECTION);
    } catch (SolrServerException e) {
      throw new IOException("Unable to commit alerts to metaalert: " + metaAlertGuid, e);
    }
    return result;
  }

  @Override
  public void init(AccessConfig config) {
    // Do nothing. We're just wrapping a child dao
  }

  @Override
  public Map<String, FieldType> getColumnMetadata(List<String> indices) throws IOException {
    return getIndexDao().getColumnMetadata(indices);
  }

  @Override
  public Document getLatest(String guid, String sensorType) throws IOException {
    if (getMetAlertSensorName().equals(sensorType)) {
      // Unfortunately, we can't just defer to the indexDao for this. Child alerts in Solr end up
      // having to be dug out.
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
  public Iterable<Document> getAllLatest(List<GetRequest> getRequests) throws IOException {
    return indexDao.getAllLatest(getRequests);
  }

  @Override
  public SearchResponse getAllMetaAlertsForAlert(String guid) throws InvalidSearchException {
    if (guid == null || guid.trim().isEmpty()) {
      throw new InvalidSearchException("Guid cannot be empty");
    }

    // Searches for all alerts containing the meta alert guid in it's "metalerts" array
    // The query has to match the parentFilter to avoid errors.  Guid must also be explicitly
    // included.
    String activeClause =
        MetaAlertConstants.STATUS_FIELD + ":" + MetaAlertStatus.ACTIVE.getStatusString();
    String guidClause = Constants.GUID + ":" + guid;
    String fullClause = "{!parent which=" + activeClause + "}" + guidClause;
    SolrQuery solrQuery = new SolrQuery()
        .setQuery(fullClause)
        .setFields("*", "[child parentFilter=" + activeClause + " limit=999]")
        .addSort(Constants.GUID,
            SolrQuery.ORDER.asc); // Just do basic sorting to track where we are

    // Use Solr's Cursors to handle the paging, rather than doing it manually.
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
    } catch (IOException | SolrServerException e) {
      throw new InvalidSearchException("Unable to complete search", e);
    }

    SearchResponse searchResponse = new SearchResponse();
    searchResponse.setResults(allResults);
    searchResponse.setTotal(allResults.size());
    return searchResponse;
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
    Iterable<Document> alerts = indexDao.getAllLatest(alertRequests);

    Document metaAlert = buildCreateDocument(alerts, request.getGroups(), CHILD_DOCUMENTS);
    MetaScores.calculateMetaScores(metaAlert, getThreatTriageField(), getThreatSort());

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

    String activeStatusClause =
        MetaAlertConstants.STATUS_FIELD + ":" + MetaAlertStatus.ACTIVE.getStatusString();

    String sensorType = ClientUtils.escapeQueryChars(MetaAlertConstants.SOURCE_TYPE);
    String metaalertTypeClause = sensorType + ":" + MetaAlertConstants.METAALERT_TYPE;
    // Use the 'v=' form in order to ensure complex clauses are properly handled.
    // Per the docs, the 'which=' clause should be used to identify all metaalert parents, not to
    //   filter
    // Status is a filter on parents and must be done outside the '!parent' construct
    String parentChildQuery =
        "(" + activeStatusClause + " AND " + "{!parent which=" + metaalertTypeClause + " v='"
            + searchRequest.getQuery() + "'})";

    // Put everything together to get our full query
    // The '-metaalert:[* TO *]' construct is to ensure the field doesn't exist on or is empty for
    //   plain alerts.
    // Also make sure that it's not a metaalert
    String fullQuery =
        "(" + searchRequest.getQuery() + " AND -" + MetaAlertConstants.METAALERT_FIELD + ":[* TO *]"
            + " AND " + "-" + sensorType + ":" + MetaAlertConstants.METAALERT_TYPE + ")"
            + " OR " + parentChildQuery;

    searchRequest.setQuery(fullQuery);

    // Build the custom field list
    Optional<List<String>> fields = searchRequest.getFields();
    String fieldList = "*";
    if (fields.isPresent()) {
      fieldList = StringUtils.join(fields.get(), ",");
    }
    String childClause = fieldList + ",[child parentFilter=" + MetaAlertConstants.STATUS_FIELD + ":"
        + MetaAlertStatus.ACTIVE.getStatusString() + "]";

    return solrDao.getSolrSearchDao().search(searchRequest, childClause);
  }

  @Override
  public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
    // Make sure to escape any problematic characters here
    String sourceType = ClientUtils.escapeQueryChars(MetaAlertConstants.SOURCE_TYPE);
    String baseQuery = groupRequest.getQuery();
    String adjustedQuery = baseQuery + " -" + MetaAlertConstants.METAALERT_FIELD + ":[* TO *]"
        + " -" + sourceType + ":" + MetaAlertConstants.METAALERT_TYPE;
    groupRequest.setQuery(adjustedQuery);
    return solrDao.group(groupRequest);
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
      searchResponse = getAllMetaAlertsForAlert(update.getGuid());
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
    indexDao.batchUpdate(updates);

    try {
      solrDao.getClient().commit(METAALERTS_COLLECTION);
      if (collection.isPresent()) {
        solrDao.getClient().commit(collection.get());
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
  public void deleteRemainingMetaAlerts(List<Map<String, Object>> alertsBefore)
      throws IOException {
    List<String> guidsToDelete = new ArrayList<>();
    for (Map<String, Object> alert : alertsBefore) {
      guidsToDelete.add((String) alert.get(Constants.GUID));
    }
    try {
      solrDao.getClient().deleteById(getMetaAlertIndex(), guidsToDelete);
    } catch (SolrServerException | IOException e) {
      throw new IOException("Unable to delete metaalert child alerts", e);
    }
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
  public String getMetAlertSensorName() {
    return MetaAlertConstants.METAALERT_TYPE;
  }

  @Override
  public String getMetaAlertIndex() {
    return metaAlertsCollection;
  }

  @Override
  public String getThreatTriageField() {
    return threatTriageField;
  }

  @Override
  public String getThreatSort() {
    return threatSort;
  }
}
