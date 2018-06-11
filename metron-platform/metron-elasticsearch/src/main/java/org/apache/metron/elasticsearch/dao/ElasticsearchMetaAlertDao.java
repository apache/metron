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

import org.apache.lucene.search.join.ScoreMode;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MetaAlertDao;
import org.apache.metron.indexing.dao.MultiIndexDao;
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
import org.apache.metron.indexing.dao.update.OriginalNotFoundException;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.InnerHitBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.apache.metron.common.Constants.GUID;
import static org.apache.metron.common.Constants.SENSOR_TYPE_FIELD_PROPERTY;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.constantScoreQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

public class ElasticsearchMetaAlertDao implements MetaAlertDao {

  public static final String SOURCE_TYPE = Constants.SENSOR_TYPE.replace('.', ':');
  public static final String THREAT_TRIAGE_FIELD = THREAT_FIELD_DEFAULT.replace('.', ':');
  private static final String STATUS_PATH = "/status";
  private static final String ALERT_PATH = "/alert";

  private IndexDao indexDao;
  private ElasticsearchDao elasticsearchDao;
  private String index = METAALERTS_INDEX;

  /**
   * Defines which summary aggregation is used to represent the overall threat triage score for
   * the metaalert. The summary aggregation is applied to the threat triage score of all child alerts.
   *
   * This overall score is primarily used for sorting; hence it is called the 'threatSort'.  This
   * can be either max, min, average, count, median, or sum.
   */
  private String threatSort = THREAT_SORT_DEFAULT;
  private int pageSize = 500;

  /**
   * Wraps an {@link org.apache.metron.indexing.dao.IndexDao} to handle meta alerts.
   * @param indexDao The Dao to wrap
   */
  public ElasticsearchMetaAlertDao(IndexDao indexDao) {
    this(indexDao, METAALERTS_INDEX, THREAT_SORT_DEFAULT);
  }

  /**
   * Wraps an {@link org.apache.metron.indexing.dao.IndexDao} to handle meta alerts.
   * @param indexDao The Dao to wrap
   * @param threatSort The summary aggregation of all child threat triage scores used
   *                   as the overall threat triage score for the metaalert. This
   *                   can be either max, min, average, count, median, or sum.
   */
  public ElasticsearchMetaAlertDao(IndexDao indexDao, String index, String threatSort) {
    init(indexDao, Optional.of(threatSort));
    this.index = index;
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
                ALERT_FIELD,
                boolQuery()
                    .must(termQuery(ALERT_FIELD + "." + GUID, guid)),
                    ScoreMode.None
            ).innerHit(new InnerHitBuilder())
        )
        .must(termQuery(STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString()));
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

    Document metaAlert = buildCreateDocument(alerts, request.getGroups());
    calculateMetaScores(metaAlert);
    // Add source type to be consistent with other sources and allow filtering
    metaAlert.getDocument().put(getFieldName(SENSOR_TYPE_FIELD_PROPERTY, SOURCE_TYPE), MetaAlertDao.METAALERT_TYPE);

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
      for (Document alert: alerts) {
        if (addMetaAlertToAlert(metaAlert.getGuid(), alert)) {
          // Use the index in the request if it exists
          Optional<String> index = guidToIndices.get(alert.getGuid());
          if (!index.isPresent()) {
            // Look up the index from Elasticsearch if one is not supplied in the request
            index = elasticsearchDao.getIndexName(alert.getGuid(), guidToSensorTypes.get(alert.getGuid()));
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
      return createResponse;
    } catch (IOException ioe) {
      throw new InvalidCreateException("Unable to create meta alert", ioe);
    }
  }

  @Override
  public boolean addAlertsToMetaAlert(String metaAlertGuid, List<GetRequest> alertRequests)
      throws IOException {
    Map<Document, Optional<String>> updates = new HashMap<>();
    Document metaAlert = indexDao.getLatest(metaAlertGuid, METAALERT_TYPE);
    if (MetaAlertStatus.ACTIVE.getStatusString().equals(metaAlert.getDocument().get(STATUS_FIELD))) {
      Iterable<Document> alerts = indexDao.getAllLatest(alertRequests);
      boolean metaAlertUpdated = addAlertsToMetaAlert(metaAlert, alerts);
      if (metaAlertUpdated) {
        calculateMetaScores(metaAlert);
        updates.put(metaAlert, Optional.of(index));
        for(Document alert: alerts) {
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
    List<Map<String,Object>> currentAlerts = (List<Map<String, Object>>) metaAlert.getDocument().get(ALERT_FIELD);
    Set<String> currentAlertGuids = currentAlerts.stream().map(currentAlert ->
        (String) currentAlert.get(GUID)).collect(Collectors.toSet());
    for (Document alert: alerts) {
      String alertGuid = alert.getGuid();
      // Only add an alert if it isn't already in the meta alert
      if (!currentAlertGuids.contains(alertGuid)) {
        currentAlerts.add(alert.getDocument());
        alertAdded = true;
      }
    }
    return alertAdded;
  }

  protected boolean addMetaAlertToAlert(String metaAlertGuid, Document alert) {
    List<String> metaAlertField = new ArrayList<>();
    List<String> alertField = (List<String>) alert.getDocument()
        .get(MetaAlertDao.METAALERT_FIELD);
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

  @Override
  public boolean removeAlertsFromMetaAlert(String metaAlertGuid, List<GetRequest> alertRequests)
      throws IOException {
    Map<Document, Optional<String>> updates = new HashMap<>();
    Document metaAlert = indexDao.getLatest(metaAlertGuid, METAALERT_TYPE);
    if (MetaAlertStatus.ACTIVE.getStatusString().equals(metaAlert.getDocument().get(STATUS_FIELD))) {
      Iterable<Document> alerts = indexDao.getAllLatest(alertRequests);
      Collection<String> alertGuids = alertRequests.stream().map(GetRequest::getGuid).collect(
          Collectors.toList());
      boolean metaAlertUpdated = removeAlertsFromMetaAlert(metaAlert, alertGuids);
      if (metaAlertUpdated) {
        calculateMetaScores(metaAlert);
        updates.put(metaAlert, Optional.of(index));
        for(Document alert: alerts) {
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

  protected boolean removeAlertsFromMetaAlert(Document metaAlert, Collection<String> alertGuids) {
    List<Map<String,Object>> currentAlerts = (List<Map<String, Object>>) metaAlert.getDocument().get(ALERT_FIELD);
    int previousSize = currentAlerts.size();
    // Only remove an alert if it is in the meta alert
    currentAlerts.removeIf(currentAlert -> alertGuids.contains((String) currentAlert.get(GUID)));
    return currentAlerts.size() != previousSize;
  }

  protected boolean removeMetaAlertFromAlert(String metaAlertGuid, Document alert) {
    List<String> metaAlertField = new ArrayList<>();
    List<String> alertField = (List<String>) alert.getDocument()
        .get(MetaAlertDao.METAALERT_FIELD);
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
    Document metaAlert = indexDao.getLatest(metaAlertGuid, METAALERT_TYPE);
    String currentStatus = (String) metaAlert.getDocument().get(MetaAlertDao.STATUS_FIELD);
    boolean metaAlertUpdated = !status.getStatusString().equals(currentStatus);
    if (metaAlertUpdated) {
      metaAlert.getDocument().put(MetaAlertDao.STATUS_FIELD, status.getStatusString());
      List<GetRequest> getRequests = new ArrayList<>();
      List<Map<String, Object>> currentAlerts = (List<Map<String, Object>>) metaAlert.getDocument()
          .get(MetaAlertDao.ALERT_FIELD);
      currentAlerts.stream().forEach(currentAlert -> {
        getRequests.add(new GetRequest((String) currentAlert.get(GUID), (String) currentAlert.get(getFieldName(SENSOR_TYPE_FIELD_PROPERTY, SOURCE_TYPE))));
      });
      Iterable<Document> alerts = indexDao.getAllLatest(getRequests);
      List<Map<String, Object>> updatedAlerts = new ArrayList<>();
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
        updatedAlerts.add(alert.getDocument());
      }
      if (MetaAlertStatus.ACTIVE.equals(status)) {
        metaAlert.getDocument().put(MetaAlertDao.ALERT_FIELD, updatedAlerts);
      }
      updates.put(metaAlert, Optional.of(index));
    }
    if (metaAlertUpdated) {
      indexDaoUpdate(updates);
    }
    return metaAlertUpdated;
  }

  @Override
  public SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException {
    // Wrap the query to also get any meta-alerts.
    QueryBuilder qb = constantScoreQuery(boolQuery()
        .must(boolQuery()
            .should(new QueryStringQueryBuilder(searchRequest.getQuery()))
            .should(nestedQuery(
                ALERT_FIELD,
                new QueryStringQueryBuilder(searchRequest.getQuery()),
                ScoreMode.None
                )
            )
        )
        // Ensures that it's a meta alert with active status or that it's an alert (signified by
        // having no status field)
        .must(boolQuery()
            .should(termQuery(MetaAlertDao.STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString()))
            .should(boolQuery().mustNot(existsQuery(MetaAlertDao.STATUS_FIELD)))
        )
        .mustNot(existsQuery(MetaAlertDao.METAALERT_FIELD))
    );
    return elasticsearchDao.search(searchRequest, qb);
  }

  @Override
  public Document getLatest(String guid, String sensorType) throws IOException {
    return indexDao.getLatest(guid, sensorType);
  }

  @Override
  public Iterable<Document> getAllLatest(
      List<GetRequest> getRequests) throws IOException {
    return indexDao.getAllLatest(getRequests);
  }

  @Override
  public void update(Document update, Optional<String> index) throws IOException {
    if (METAALERT_TYPE.equals(update.getSensorType())) {
      // We've been passed an update to the meta alert.
      throw new UnsupportedOperationException("Meta alerts cannot be directly updated");
    } else {
      Map<Document, Optional<String>> updates = new HashMap<>();
      updates.put(update, index);
      // We need to update an alert itself.  Only that portion of the update can be delegated.
      // We still need to get meta alerts potentially associated with it and update.
      Collection<Document> metaAlerts = getMetaAlertsForAlert(update.getGuid()).getResults().stream()
          .map(searchResult -> new Document(searchResult.getSource(), searchResult.getId(), METAALERT_TYPE, update.getTimestamp()))
          .collect(Collectors.toList());
      // Each meta alert needs to be updated with the new alert
      for (Document metaAlert : metaAlerts) {
        replaceAlertInMetaAlert(metaAlert, update);
        updates.put(metaAlert, Optional.of(METAALERTS_INDEX));
      }

      // Run the alert's update
      indexDao.batchUpdate(updates);
    }
  }

  protected boolean replaceAlertInMetaAlert(Document metaAlert, Document alert) {
    boolean metaAlertUpdated = removeAlertsFromMetaAlert(metaAlert, Collections.singleton(alert.getGuid()));
    if (metaAlertUpdated) {
      addAlertsToMetaAlert(metaAlert, Collections.singleton(alert));
    }
    return metaAlertUpdated;
  }

  @Override
  public void batchUpdate(Map<Document, Optional<String>> updates) throws IOException {
    throw new UnsupportedOperationException("Meta alerts do not allow for bulk updates");
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
    if (METAALERT_TYPE.equals(request.getSensorType())) {
      if (isPatchAllowed(request)) {
        Document d = getPatchedDocument(request, timestamp);
        indexDao.update(d, Optional.ofNullable(request.getIndex()));
      } else {
        throw new IllegalArgumentException("Meta alert patches are not allowed for /alert or /status paths.  "
                + "Please use the add/remove alert or update status functions instead.");
      }
    } else {
      Document d = getPatchedDocument(request, timestamp);
      update(d, Optional.ofNullable(request.getIndex()));
    }
  }

  protected boolean isPatchAllowed(PatchRequest request) {
    if(request.getPatch() != null && !request.getPatch().isEmpty()) {
      for(Map<String, Object> patch : request.getPatch()) {
        Object pathObj = patch.get("path");
        if(pathObj != null && pathObj instanceof String) {
          String path = (String)pathObj;
          if (STATUS_PATH.equals(path) || ALERT_PATH.equals(path)) {
            return false;
          }
        }
      }
    }
    return true;
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
                ALERT_FIELD,
                boolQuery()
                    .must(termQuery(ALERT_FIELD + "." + Constants.GUID, alertGuid)),
                ScoreMode.None
            ).innerHit(new InnerHitBuilder())
        )
        .must(termQuery(STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString()));
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
        .prepareSearch(index)
        .addStoredField("*")
        .setFetchSource(true)
        .setQuery(qb)
        .setSize(pageSize);
    org.elasticsearch.action.search.SearchResponse esResponse = searchRequestBuilder
        .execute()
        .actionGet();
    List<SearchResult> allResults = getSearchResults(esResponse);
    long total = esResponse.getHits().getTotalHits();
    if (total > pageSize) {
      int pages = (int) (total / pageSize) + 1;
      for (int i = 1; i < pages; i++) {
        int from = i * pageSize;
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
  protected List<SearchResult> getSearchResults(org.elasticsearch.action.search.SearchResponse searchResponse) {
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

  /**
   * Build the Document representing a meta alert to be created.
   * @param alerts The Elasticsearch results for the meta alerts child documents
   * @param groups The groups used to create this meta alert
   * @return A Document representing the new meta alert
   */
  protected Document buildCreateDocument(Iterable<Document> alerts, List<String> groups) {
    // Need to create a Document from the multiget. Scores will be calculated later
    Map<String, Object> metaSource = new HashMap<>();
    List<Map<String, Object>> alertList = new ArrayList<>();
    for (Document alert: alerts) {
      alertList.add(alert.getDocument());
    }
    metaSource.put(ALERT_FIELD, alertList);

    // Add any meta fields
    String guid = UUID.randomUUID().toString();
    metaSource.put(GUID, guid);
    metaSource.put(Constants.Fields.TIMESTAMP.getName(), System.currentTimeMillis());
    metaSource.put(GROUPS_FIELD, groups);
    metaSource.put(STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString());

    return new Document(metaSource, guid, METAALERT_TYPE, System.currentTimeMillis());
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



  @SuppressWarnings("unchecked")
  protected List<Map<String, Object>> getAllAlertsForMetaAlert(Document update) throws IOException {
    Document latest = indexDao.getLatest(update.getGuid(), MetaAlertDao.METAALERT_TYPE);
    if (latest == null) {
      return new ArrayList<>();
    }
    List<String> guids = new ArrayList<>();
    List<Map<String, Object>> latestAlerts = (List<Map<String, Object>>) latest.getDocument()
        .get(MetaAlertDao.ALERT_FIELD);
    for (Map<String, Object> alert : latestAlerts) {
      guids.add((String) alert.get(Constants.GUID));
    }

    List<Map<String, Object>> alerts = new ArrayList<>();
    QueryBuilder query = QueryBuilders.idsQuery().addIds(guids.toArray(new String[0]));
    SearchRequestBuilder request = elasticsearchDao.getClient().prepareSearch()
        .setQuery(query);
    org.elasticsearch.action.search.SearchResponse response = request.get();
    for (SearchHit hit : response.getHits().getHits()) {
      alerts.add(hit.sourceAsMap());
    }
    return alerts;
  }

  /**
   * Builds an update Document for updating the meta alerts list.
   * @param alertGuid The GUID of the alert to update
   * @param sensorType The sensor type to update
   * @param metaAlertField The new metaAlertList to use
   * @return The update Document
   */
  protected Document buildAlertUpdate(String alertGuid, String sensorType,
      List<String> metaAlertField, Long timestamp) {
    Document alertUpdate;
    Map<String, Object> document = new HashMap<>();
    document.put(MetaAlertDao.METAALERT_FIELD, metaAlertField);
    alertUpdate = new Document(
        document,
        alertGuid,
        sensorType,
        timestamp
    );
    return alertUpdate;
  }


  @Override
  public Map<String, FieldType> getColumnMetadata(List<String> indices)
      throws IOException {
    return indexDao.getColumnMetadata(indices);
  }

  @Override
  public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
    // Wrap the query to hide any alerts already contained in meta alerts
    QueryBuilder qb = QueryBuilders.boolQuery()
        .must(new QueryStringQueryBuilder(groupRequest.getQuery()))
        .mustNot(existsQuery(MetaAlertDao.METAALERT_FIELD));
    return elasticsearchDao.group(groupRequest, qb);
  }

  /**
   * Calculate the meta alert scores for a Document.
   * @param metaAlert The Document containing scores
   * @return Set of score statistics
   */
  @SuppressWarnings("unchecked")
  protected void calculateMetaScores(Document metaAlert) {
    MetaScores metaScores = new MetaScores(new ArrayList<>());
    List<Object> alertsRaw = ((List<Object>) metaAlert.getDocument().get(ALERT_FIELD));
    if (alertsRaw != null && !alertsRaw.isEmpty()) {
      ArrayList<Double> scores = new ArrayList<>();
      for (Object alertRaw : alertsRaw) {
        Map<String, Object> alert = (Map<String, Object>) alertRaw;
        Double scoreNum = parseThreatField(alert.get(getFieldName(THREAT_FIELD_PROPERTY, THREAT_TRIAGE_FIELD)));
        if (scoreNum != null) {
          scores.add(scoreNum);
        }
      }
      metaScores = new MetaScores(scores);
    }

    // add a summary (max, min, avg, ...) of all the threat scores from the child alerts
    metaAlert.getDocument().putAll(metaScores.getMetaScores());

    // add the overall threat score for the metaalert; one of the summary aggregations as defined by `threatSort`
    Object threatScore = metaScores.getMetaScores().get(threatSort);

    // add the threat score as a float; type needs to match the threat score field from each of the sensor indices
    metaAlert.getDocument().put(getFieldName(THREAT_FIELD_PROPERTY, THREAT_TRIAGE_FIELD), ConversionUtils.convert(threatScore, Float.class));
  }

  private Double parseThreatField(Object threatRaw) {
    Double threat = null;
    if (threatRaw instanceof Number) {
      threat = ((Number) threatRaw).doubleValue();
    } else if (threatRaw instanceof String) {
      threat = Double.parseDouble((String) threatRaw);
    }
    return threat;
  }

  public int getPageSize() {
    return pageSize;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  private String getFieldName(String globalConfigKey, String defaultFieldName) {
    if (this.elasticsearchDao == null || this.elasticsearchDao.getAccessConfig() == null) {
      return defaultFieldName;
    }
    Map<String, Object> globalConfig = this.elasticsearchDao.getAccessConfig().getGlobalConfigSupplier().get();
    return ConfigurationsUtils.getFieldName(globalConfig, globalConfigKey, defaultFieldName);
  }
}
