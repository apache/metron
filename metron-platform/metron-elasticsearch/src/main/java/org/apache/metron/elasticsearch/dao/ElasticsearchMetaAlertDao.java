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

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.constantScoreQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.collections4.SetUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MetaAlertDao;
import org.apache.metron.indexing.dao.MultiIndexDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.metaalert.MetaScores;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidCreateException;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.metron.indexing.dao.update.Document;
import org.elasticsearch.action.ActionWriteResponse.ShardInfo;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequest.Item;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.support.QueryInnerHitBuilder;
import org.elasticsearch.search.SearchHit;

public class ElasticsearchMetaAlertDao implements MetaAlertDao {

  private static final String SOURCE_TYPE = Constants.SENSOR_TYPE.replace('.', ':');
  private IndexDao indexDao;
  private ElasticsearchDao elasticsearchDao;
  private String index = METAALERTS_INDEX;
  private String threatTriageField = THREAT_FIELD_DEFAULT;
  private String threatSort = THREAT_SORT_DEFAULT;

  /**
   * Wraps an {@link org.apache.metron.indexing.dao.IndexDao} to handle meta alerts.
   * @param indexDao The Dao to wrap
   */
  public ElasticsearchMetaAlertDao(IndexDao indexDao) {
    this(indexDao, METAALERTS_INDEX, THREAT_FIELD_DEFAULT, THREAT_SORT_DEFAULT);
  }

  /**
   * Wraps an {@link org.apache.metron.indexing.dao.IndexDao} to handle meta alerts.
   * @param indexDao The Dao to wrap
   * @param triageLevelField The field name to use as the threat scoring field
   */
  public ElasticsearchMetaAlertDao(IndexDao indexDao, String index, String triageLevelField,
      String threatSort) {
    init(indexDao, threatSort);
    this.index = index;
    this.threatTriageField = triageLevelField;
  }

  public ElasticsearchMetaAlertDao() {
    //uninitialized.
  }

  @Override
  public void init(IndexDao indexDao, String threatSort) {
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

    if (threatSort != null) {
      this.threatSort = threatSort;
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
    org.elasticsearch.action.search.SearchResponse esResponse = getMetaAlertsForAlert(guid.trim());
    SearchResponse searchResponse = new SearchResponse();
    searchResponse.setTotal(esResponse.getHits().getTotalHits());
    searchResponse.setResults(
        Arrays.stream(esResponse.getHits().getHits()).map(searchHit -> {
              SearchResult searchResult = new SearchResult();
              searchResult.setId(searchHit.getId());
              searchResult.setSource(searchHit.getSource());
              searchResult.setScore(searchHit.getScore());
              searchResult.setIndex(searchHit.getIndex());
              return searchResult;
            }
        ).collect(Collectors.toList()));
    return searchResponse;
  }

  @Override
  @SuppressWarnings("unchecked")
  public MetaAlertCreateResponse createMetaAlert(MetaAlertCreateRequest request)
      throws InvalidCreateException, IOException {
    if (request.getGuidToIndices().isEmpty()) {
      throw new InvalidCreateException("MetaAlertCreateRequest must contain alert GUIDs");
    }
    if (request.getGroups().isEmpty()) {
      throw new InvalidCreateException("MetaAlertCreateRequest must contain UI groups");
    }

    // Retrieve the documents going into the meta alert and build it
    MultiGetResponse multiGetResponse = getDocumentsByGuid(request);
    Document createDoc = buildCreateDocument(multiGetResponse, request.getGroups());
    MetaScores metaScores = calculateMetaScores(createDoc);
    createDoc.getDocument().putAll(metaScores.getMetaScores());
    createDoc.getDocument().put(threatTriageField, metaScores.getMetaScores().get(threatSort));
    // Add source type to be consistent with other sources and allow filtering
    createDoc.getDocument().put("source:type", MetaAlertDao.METAALERT_TYPE);

    // Start a list of updates / inserts we need to run
    Map<Document, Optional<String>> updates = new HashMap<>();
    updates.put(createDoc, Optional.of(MetaAlertDao.METAALERTS_INDEX));

    try {
      // We need to update the associated alerts with the new meta alerts, making sure existing
      // links are maintained.
      List<String> metaAlertField;
      for (MultiGetItemResponse itemResponse : multiGetResponse) {
        metaAlertField = new ArrayList<>();
        GetResponse response = itemResponse.getResponse();
        if (response.isExists()) {
          List<String> alertField = (List<String>) response.getSourceAsMap()
              .get(MetaAlertDao.METAALERT_FIELD);
          if (alertField != null) {
            metaAlertField.addAll(alertField);
          }
        }
        metaAlertField.add(createDoc.getGuid());

        Document alertUpdate = buildAlertUpdate(response.getId(),
            (String) response.getSource().get(SOURCE_TYPE), metaAlertField,
            (Long) response.getSourceAsMap().get("_timestamp"));
        updates.put(alertUpdate, Optional.of(itemResponse.getIndex()));
      }

      // Kick off any updates.
      indexDaoUpdate(updates);

      MetaAlertCreateResponse createResponse = new MetaAlertCreateResponse();
      createResponse.setCreated(true);
      createResponse.setGuid(createDoc.getGuid());
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
                ALERT_FIELD,
                new QueryStringQueryBuilder(searchRequest.getQuery())
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
  public void update(Document update, Optional<String> index) throws IOException {
    if (METAALERT_TYPE.equals(update.getSensorType())) {
      // We've been passed an update to the meta alert.
      handleMetaUpdate(update);
    } else {
      // We need to update an alert itself.  Only that portion of the update can be delegated.
      // We still need to get meta alerts potentially associated with it and update.
      org.elasticsearch.action.search.SearchResponse response = getMetaAlertsForAlert(
          update.getGuid()
      );

      // Each hit, if any, is a metaalert that needs to be updated
      for (SearchHit hit : response.getHits()) {
        handleAlertUpdate(update, hit);
      }

      // Run the alert's update
      indexDao.update(update, index);
    }
  }

  @Override
  public void batchUpdate(Map<Document, Optional<String>> updates) throws IOException {
    throw new UnsupportedOperationException("Meta alerts do not allow for bulk updates");
  }

  /**
   * Given an alert GUID, retrieve all associated meta alerts.
   * @param guid The GUID of the child alert
   * @return The Elasticsearch response containing the meta alerts
   */
  protected org.elasticsearch.action.search.SearchResponse getMetaAlertsForAlert(String guid) {
    QueryBuilder qb = boolQuery()
        .must(
            nestedQuery(
                ALERT_FIELD,
                boolQuery()
                    .must(termQuery(ALERT_FIELD + "." + Constants.GUID, guid))
            ).innerHit(new QueryInnerHitBuilder())
        )
        .must(termQuery(STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString()));
    SearchRequest sr = new SearchRequest();
    ArrayList<String> indices = new ArrayList<>();
    indices.add(index);
    sr.setIndices(indices);
    return elasticsearchDao
        .getClient()
        .prepareSearch(index)
        .addFields("*")
        .setFetchSource(true)
        .setQuery(qb)
        .execute()
        .actionGet();
  }

  /**
   * Return child documents after retrieving them from Elasticsearch.
   * @param request The request detailing which child alerts we need
   * @return The Elasticsearch response to our request for alerts
   */
  protected MultiGetResponse getDocumentsByGuid(MetaAlertCreateRequest request) {
    MultiGetRequestBuilder multiGet = elasticsearchDao.getClient().prepareMultiGet();
    for (Entry<String, String> entry : request.getGuidToIndices().entrySet()) {
      multiGet.add(new Item(entry.getValue(), null, entry.getKey()));
    }
    return multiGet.get();
  }

  /**
   * Build the Document representing a meta alert to be created.
   * @param multiGetResponse The Elasticsearch results for the meta alerts child documents
   * @param groups The groups used to create this meta alert
   * @return A Document representing the new meta alert
   */
  protected Document buildCreateDocument(MultiGetResponse multiGetResponse, List<String> groups) {
    // Need to create a Document from the multiget. Scores will be calculated later
    Map<String, Object> metaSource = new HashMap<>();
    List<Map<String, Object>> alertList = new ArrayList<>();
    for (MultiGetItemResponse itemResponse : multiGetResponse) {
      GetResponse response = itemResponse.getResponse();
      if (response.isExists()) {
        alertList.add(response.getSource());
      }
    }
    metaSource.put(ALERT_FIELD, alertList);

    // Add any meta fields
    String guid = UUID.randomUUID().toString();
    metaSource.put(Constants.GUID, guid);
    metaSource.put(Constants.Fields.TIMESTAMP.getName(), System.currentTimeMillis());
    metaSource.put(GROUPS_FIELD, groups);
    metaSource.put(STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString());

    return new Document(metaSource, guid, METAALERT_TYPE, System.currentTimeMillis());
  }

  /**
   * Process an update to a meta alert itself.
   * @param update The update Document to be applied
   * @throws IOException If there's a problem running the update
   */
  protected void handleMetaUpdate(Document update) throws IOException {
    Map<Document, Optional<String>> updates = new HashMap<>();

    if (update.getDocument().containsKey(MetaAlertDao.STATUS_FIELD)) {
      // Update all associated alerts to maintain the meta alert link properly
      updates.putAll(buildStatusAlertUpdates(update));
    }
    if (update.getDocument().containsKey(MetaAlertDao.ALERT_FIELD)) {
      // If the alerts field changes (i.e. add/remove alert), update all affected alerts to
      // maintain the meta alert link properly.
      updates.putAll(buildAlertFieldUpdates(update));
    }

    // Run meta alert update.
    updates.put(update, Optional.of(index));
    indexDaoUpdate(updates);
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

  protected Map<Document, Optional<String>> buildStatusAlertUpdates(Document update)
      throws IOException {
    Map<Document, Optional<String>> updates = new HashMap<>();
    List<Map<String, Object>> alerts = getAllAlertsForMetaAlert(update);
    for (Map<String, Object> alert : alerts) {
      // Retrieve the associated alert, so we can update the array
      List<String> metaAlertField = new ArrayList<>();
      @SuppressWarnings("unchecked")
      List<String> alertField = (List<String>) alert.get(MetaAlertDao.METAALERT_FIELD);
      if (alertField != null) {
        metaAlertField.addAll(alertField);
      }
      String status = (String) update.getDocument().get(MetaAlertDao.STATUS_FIELD);

      Document alertUpdate = null;
      String alertGuid = (String) alert.get(Constants.GUID);
      // If we're making it active add add the meta alert guid for every alert.
      if (MetaAlertStatus.ACTIVE.getStatusString().equals(status)
          && !metaAlertField.contains(update.getGuid())) {
        metaAlertField.add(update.getGuid());
        alertUpdate = buildAlertUpdate(
            alertGuid,
            (String) alert.get(SOURCE_TYPE),
            metaAlertField,
            (Long) alert.get("_timestamp")
        );
      }

      // If we're making it inactive, remove the meta alert guid from every alert.
      if (MetaAlertStatus.INACTIVE.getStatusString().equals(status)
          && metaAlertField.remove(update.getGuid())) {
        alertUpdate = buildAlertUpdate(
            alertGuid,
            (String) alert.get(SOURCE_TYPE),
            metaAlertField,
            (Long) alert.get("_timestamp")
        );
      }

      // Only run an alert update if we have an actual update.
      if (alertUpdate != null) {
        updates.put(alertUpdate, Optional.empty());
      }
    }
    return updates;
  }

  protected Map<Document, Optional<String>> buildAlertFieldUpdates(Document update)
      throws IOException {
    Map<Document, Optional<String>> updates = new HashMap<>();
    // If we've updated the alerts field (i.e add/remove), recalculate meta alert scores and
    // the metaalerts fields for updating the children alerts.
    MetaScores metaScores = calculateMetaScores(update);
    update.getDocument().putAll(metaScores.getMetaScores());
    update.getDocument().put(threatTriageField, metaScores.getMetaScores().get(threatSort));

    // Get the set of GUIDs that are in the new version.
    Set<String> updateGuids = new HashSet<>();
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> updateAlerts = (List<Map<String, Object>>) update.getDocument()
        .get(MetaAlertDao.ALERT_FIELD);
    for (Map<String, Object> alert : updateAlerts) {
      updateGuids.add((String) alert.get(Constants.GUID));
    }

    // Get the set of GUIDs from the old version
    List<Map<String, Object>> alerts = getAllAlertsForMetaAlert(update);
    Set<String> currentGuids = new HashSet<>();
    for (Map<String, Object> alert : alerts) {
      currentGuids.add((String) alert.get(Constants.GUID));
    }

    // Get both set differences, so we know what's been added and removed.
    Set<String> removedGuids = SetUtils.difference(currentGuids, updateGuids);
    Set<String> addedGuids = SetUtils.difference(updateGuids, currentGuids);

    Document alertUpdate;

    // Handle any removed GUIDs
    for (String guid : removedGuids) {
      // Retrieve the associated alert, so we can update the array
      Document alert = elasticsearchDao.getLatest(guid, null);
      List<String> metaAlertField = new ArrayList<>();
      @SuppressWarnings("unchecked")
      List<String> alertField = (List<String>) alert.getDocument()
          .get(MetaAlertDao.METAALERT_FIELD);
      if (alertField != null) {
        metaAlertField.addAll(alertField);
      }
      if (metaAlertField.remove(update.getGuid())) {
        alertUpdate = buildAlertUpdate(guid, alert.getSensorType(), metaAlertField,
            alert.getTimestamp());
        updates.put(alertUpdate, Optional.empty());
      }
    }

    // Handle any added GUIDs
    for (String guid : addedGuids) {
      // Retrieve the associated alert, so we can update the array
      Document alert = elasticsearchDao.getLatest(guid, null);
      List<String> metaAlertField = new ArrayList<>();
      @SuppressWarnings("unchecked")
      List<String> alertField = (List<String>) alert.getDocument()
          .get(MetaAlertDao.METAALERT_FIELD);
      if (alertField != null) {
        metaAlertField.addAll(alertField);
      }
      metaAlertField.add(update.getGuid());
      alertUpdate = buildAlertUpdate(guid, alert.getSensorType(), metaAlertField,
          alert.getTimestamp());
      updates.put(alertUpdate, Optional.empty());
    }

    return updates;
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
    QueryBuilder query = QueryBuilders.idsQuery().ids(guids);
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

  /**
   * Takes care of upserting a child alert to a meta alert.
   * @param update The update Document to be applied
   * @param hit The meta alert to be updated
   * @throws IOException If there's an issue running the upsert
   */
  protected void handleAlertUpdate(Document update, SearchHit hit) throws IOException {
    XContentBuilder builder = buildUpdatedMetaAlert(update, hit);

    // Run the meta alert's update
    IndexRequest indexRequest = new IndexRequest(
        METAALERTS_INDEX,
        METAALERT_DOC,
        hit.getId()
    ).source(builder);
    UpdateRequest updateRequest = new UpdateRequest(
        METAALERTS_INDEX,
        METAALERT_DOC,
        hit.getId()
    ).doc(builder).upsert(indexRequest);
    try {
      UpdateResponse updateResponse = elasticsearchDao.getClient().update(updateRequest).get();

      ShardInfo shardInfo = updateResponse.getShardInfo();
      int failed = shardInfo.getFailed();
      if (failed > 0) {
        throw new IOException(
            "ElasticsearchMetaAlertDao upsert failed: "
                + Arrays.toString(shardInfo.getFailures())
        );
      }
    } catch (Exception e) {
      throw new IOException(e.getMessage(), e);
    }
  }

  @Override
  public Map<String, FieldType> getColumnMetadata(List<String> indices)
      throws IOException {
    return indexDao.getColumnMetadata(indices);
  }

  @Override
  public Map<String, FieldType> getCommonColumnMetadata(List<String> indices) throws
      IOException {
    return indexDao.getCommonColumnMetadata(indices);
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
   * @param document The Document containing scores
   * @return Set of score statistics
   */
  @SuppressWarnings("unchecked")
  protected MetaScores calculateMetaScores(Document document) {
    List<Object> alertsRaw = ((List<Object>) document.getDocument().get(ALERT_FIELD));
    if (alertsRaw == null || alertsRaw.isEmpty()) {
      throw new IllegalArgumentException("No alerts to use in calculation for doc GUID: "
          + document.getDocument().get(Constants.GUID));
    }

    ArrayList<Double> scores = new ArrayList<>();
    for (Object alertRaw : alertsRaw) {
      Map<String, Object> alert = (Map<String, Object>) alertRaw;
      Double scoreNum = parseThreatField(alert.get(threatTriageField));
      if (scoreNum != null) {
        scores.add(scoreNum);
      }
    }

    return new MetaScores(scores);
  }

  /**
   * Builds the updated meta alert based on the update.
   * @param update The update Document for the meta alert
   * @param hit The meta alert to be updated
   * @return A builder for Elasticsearch to use
   * @throws IOException If we have an issue building the result
   */
  protected XContentBuilder buildUpdatedMetaAlert(Document update, SearchHit hit)
      throws IOException {
    // Make sure to get all the threat scores while we're going through the docs
    List<Double> scores = new ArrayList<>();
    // Start building the new version of the metaalert
    XContentBuilder builder = jsonBuilder().startObject();

    // Run through the nested alerts of the meta alert and either use the new or old versions
    builder.startArray(ALERT_FIELD);
    Map<String, Object> hitAlerts = hit.sourceAsMap();

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> alertHits = (List<Map<String, Object>>) hitAlerts.get(ALERT_FIELD);
    for (Map<String, Object> alertHit : alertHits) {
      Map<String, Object> docMap = alertHit;
      // If we're at the update use it instead of the original
      if (alertHit.get(Constants.GUID).equals(update.getGuid())) {
        docMap = update.getDocument();
      }
      builder.map(docMap);

      // Handle either String or Number values in the threatTriageField
      Object threatRaw = docMap.get(threatTriageField);
      Double threat = parseThreatField(threatRaw);

      if (threat != null) {
        scores.add(threat);
      }
    }
    builder.endArray();

    // Add all the meta alert fields, and score calculation
    Map<String, Object> updatedMeta = new HashMap<>();
    updatedMeta.putAll(hit.getSource());
    updatedMeta.putAll(new MetaScores(scores).getMetaScores());
    for (Entry<String, Object> entry : updatedMeta.entrySet()) {
      // The alerts field is being added separately, so ignore the original
      if (!(entry.getKey().equals(ALERT_FIELD))) {
        builder.field(entry.getKey(), entry.getValue());
      }
    }
    builder.endObject();

    return builder;
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
}
