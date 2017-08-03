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
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MetaAlertDao;
import org.apache.metron.indexing.dao.MultiIndexDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.metaalert.MetaScores;
import org.apache.metron.indexing.dao.search.FieldType;
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
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.support.QueryInnerHitBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

public class ElasticsearchMetaAlertDao implements MetaAlertDao {
  private ElasticsearchDao esDao;
  private String index;
  private String threatTriageField = THREAT_FIELD_DEFAULT;

  /**
   * Wraps an {@link org.apache.metron.indexing.dao.IndexDao} to handle meta alerts.
   * @param indexDao The Dao to wrap
   */
  public ElasticsearchMetaAlertDao(IndexDao indexDao) {
    this(indexDao, METAALERTS_INDEX, THREAT_FIELD_DEFAULT);
  }

  /**
   * Wraps an {@link org.apache.metron.indexing.dao.IndexDao} to handle meta alerts.
   * @param indexDao The Dao to wrap
   */
  public ElasticsearchMetaAlertDao(IndexDao indexDao, String index) {
    this(indexDao, index, THREAT_FIELD_DEFAULT);
  }

  /**
   * Wraps an {@link org.apache.metron.indexing.dao.IndexDao} to handle meta alerts.
   * @param indexDao The Dao to wrap
   * @param triageLevelField The field name to use as the threat scoring field
   */
  public ElasticsearchMetaAlertDao(IndexDao indexDao, String index, String triageLevelField) {
    init(indexDao);
    this.index = index;
    this.threatTriageField = triageLevelField;
  }

  public ElasticsearchMetaAlertDao() {
    //uninitialized.
  }

  @Override
  public void init(IndexDao indexDao) {
    if (indexDao instanceof MultiIndexDao) {
      MultiIndexDao miDao = (MultiIndexDao) indexDao;
      for (IndexDao childDao : miDao.getIndices()) {
        if (childDao instanceof ElasticsearchDao) {
          esDao = (ElasticsearchDao) childDao;
        }
      }
    } else if (indexDao instanceof ElasticsearchDao) {
      esDao = (ElasticsearchDao) indexDao;
    } else {
      throw new IllegalArgumentException(
          "Need an ElasticsearchDao when using ElasticsearchMetaAlertDao"
      );
    }
  }

  @Override
  public SearchResponse getAllMetaAlertsForAlert(String guid) throws InvalidSearchException {
    if (guid == null || guid.isEmpty()) {
      throw new InvalidSearchException("Guid cannot be empty");
    }
    org.elasticsearch.action.search.SearchResponse esResponse = getMetaAlertsForAlert(guid);
    SearchResponse searchResponse = new SearchResponse();
    searchResponse.setTotal(esResponse.getHits().getTotalHits());
    searchResponse.setResults(Arrays.stream(esResponse.getHits().getHits()).map(searchHit -> {
      SearchResult searchResult = new SearchResult();
      searchResult.setId(searchHit.getId());
      searchResult.setSource(searchHit.getSource());
      searchResult.setScore(searchHit.getScore());
      searchResult.setIndex(searchHit.getIndex());
      return searchResult;
    }).collect(Collectors.toList()));
    return null;
  }

  protected org.elasticsearch.action.search.SearchResponse getMetaAlertsForAlert(String guid) {
    QueryBuilder qb = constantScoreQuery(
        boolQuery()
            .must(
                nestedQuery(
                    ALERT_FIELD,
                    boolQuery()
                        .must(termQuery(ALERT_FIELD + "." + Constants.GUID, guid))
                        .must(termQuery(ALERT_FIELD + "." + STATUS_FIELD, "active"))
                ).innerHit(new QueryInnerHitBuilder())
            )
    );
    SearchRequest sr = new SearchRequest();
    ArrayList<String> indices = new ArrayList<>();
    indices.add(index);
    sr.setIndices(indices);
    return esDao
        .getClient()
        .prepareSearch(index)
        .addFields("*")
        .setFetchSource(true)
        .setQuery(qb)
        .execute()
        .actionGet();
  }

  @Override
  public MetaAlertCreateResponse createMetaAlert(MetaAlertCreateRequest request)
      throws InvalidCreateException, IOException {
    if (request.getGuidToIndices().isEmpty()) {
      throw new InvalidCreateException("MetaAlertCreateRequest must contain alert GUIDs");
    }
    if (request.getGroups().isEmpty()) {
      throw new InvalidCreateException("MetaAlertCreateRequest must contain UI groups");
    }

    // Retrieve the documents going into the meta alert
    MultiGetResponse multiGetResponse = getDocumentsByGuid(request);
    Document createDoc = buildCreateDocument(multiGetResponse, request.getGroups());

    try {
      handleMetaUpdate(createDoc, Optional.of(METAALERTS_INDEX));
      MetaAlertCreateResponse createResponse = new MetaAlertCreateResponse();
      createResponse.setCreated(true);
      return createResponse;
    } catch (IOException ioe) {
      throw new InvalidCreateException("Unable to create meta alert", ioe);
    }
  }

  protected MultiGetResponse getDocumentsByGuid(MetaAlertCreateRequest request) {
    MultiGetRequestBuilder multiGet = esDao.getClient().prepareMultiGet();
    for (Entry<String, String> entry : request.getGuidToIndices().entrySet()) {
      multiGet.add(new Item(entry.getValue(), null, entry.getKey()));
    }
    return multiGet.get();
  }

  protected Document buildCreateDocument(MultiGetResponse multiGetResponse, List<String> groups) {
    // Need to create a Document from the multiget and
    // Make sure to track the scores as we go through.
    List<Double> scores = new ArrayList<>();
    Map<String, Object> metaSource = new HashMap<>();
    // Run through the alerts and add them and their scores
    List<Map<String, Object>> alertList = new ArrayList<>();
    for (MultiGetItemResponse itemResponse : multiGetResponse) {
      GetResponse response = itemResponse.getResponse();
      if (response.isExists()) {
        Map<String, Object> source = response.getSource();
        alertList.add(source);
        Number value = (Number) source.get(threatTriageField);
        if (value != null) {
          scores.add(value.doubleValue());
        }
      }
    }
    metaSource.put(ALERT_FIELD, alertList.toArray());

    // Add any meta fields and score calculation.
    Map<String, Object> metaFields = new HashMap<>();
    String guid = UUID.randomUUID().toString();
    metaFields.put(Constants.GUID, guid);
    metaFields.put(GROUPS_FIELD, groups.toArray());
    metaFields.putAll(new MetaScores(scores).getMetaScores());
    for (Entry<String, Object> entry : metaFields.entrySet()) {
      metaSource.put(entry.getKey(), entry.getValue());
    }

    return new Document(metaSource, guid, METAALERT_TYPE, System.currentTimeMillis());
  }

  @Override
  public SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException {
    // Wrap the query to also get any meta-alerts.
    QueryBuilder qb = constantScoreQuery(
        boolQuery().should(new QueryStringQueryBuilder(searchRequest.getQuery()))
            .should(nestedQuery(ALERT_FIELD,
                boolQuery().must(new QueryStringQueryBuilder(searchRequest.getQuery())))
            )
    );
    return esDao.search(searchRequest, qb);
  }

  @Override
  public void init(AccessConfig config) {
    // Do nothing. We're just wrapping a child dao
  }

  @Override
  public Document getLatest(String guid, String sensorType) throws IOException {
    if (METAALERT_TYPE.equals(sensorType)) {
      throw new UnsupportedOperationException("MetaAlerts don't support getting latest");
    }

    return esDao.getLatest(guid, sensorType);
  }

  @Override
  public void update(Document update, Optional<String> index) throws IOException {
    if (METAALERT_TYPE.equals(update.getSensorType())) {
      // We've been passed an update to the meta alert.
      handleMetaUpdate(update, index);
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
      esDao.update(update, index);
    }
  }

  protected void handleMetaUpdate(Document update, Optional<String> index) throws IOException {
    // We have an update to a meta alert itself (e.g. adding a document, etc.)  Recalculate scores
    // and defer the final result to the Elasticsearch DAO.
    MetaScores metaScores = calculateMetaScores(update);
    update.getDocument().putAll(metaScores.getMetaScores());
    esDao.update(update, index);
  }

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
      UpdateResponse updateResponse = esDao.getClient().update(updateRequest).get();

      // If we have no successes or
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
  public Map<String, Map<String, FieldType>> getColumnMetadata(List<String> indices)
      throws IOException {
    return esDao.getColumnMetadata(indices);
  }

  @Override
  public Map<String, FieldType> getCommonColumnMetadata(List<String> indices) throws
      IOException {
    return esDao.getCommonColumnMetadata(indices);
  }

  @SuppressWarnings("unchecked")
  protected MetaScores calculateMetaScores(Document update) {
    Map<String, Object>[] alerts = (Map<String, Object>[]) update.getDocument().get(ALERT_FIELD);
    if (alerts == null) {
      throw new IllegalArgumentException("No alerts to use in calculation for doc GUID: "
          + update.getDocument().get(Constants.GUID));
    }

    ArrayList<Double> scores = new ArrayList<>();
    for (Map<String, Object> alert : alerts) {
      Number scoreNum = (Number) alert.get(THREAT_FIELD_DEFAULT);
      if (scoreNum != null) {
        scores.add(scoreNum.doubleValue());
      }
    }

    return new MetaScores(scores);
  }

  protected XContentBuilder buildUpdatedMetaAlert(Document update, SearchHit hit)
      throws IOException {
    // Make sure to get all the threat scores while we're going through the docs
    List<Double> scores = new ArrayList<>();
    // Start building the new version of the metaalert
    XContentBuilder builder = jsonBuilder().startObject();

    // Run through the nested alerts of the meta alert and either use the new or old versions
    builder.startArray(ALERT_FIELD);
    Map<String, SearchHits> innerHits = hit.getInnerHits();

    SearchHits alertHits = innerHits.get(ALERT_FIELD);
    for (SearchHit alertHit : alertHits.getHits()) {
      Map<String, Object> docMap;
      // If we're at the update use it, otherwise use the original
      if (alertHit.getId().equals(update.getGuid())) {
        docMap = update.getDocument();
      } else {
        docMap = alertHit.getSource();
      }
      builder.map(docMap);
      Number value = (Number) docMap.get(threatTriageField);
      if (value != null) {
        scores.add(value.doubleValue());
      }
    }
    builder.endArray();

    // Add all the meta alert fields, and score calculation
    Map<String, Object> updatedMeta = new HashMap<>();
    updatedMeta.putAll(hit.getSource());
    updatedMeta.putAll(new MetaScores(scores).getMetaScores());
    for (Entry<String, Object> entry : updatedMeta.entrySet()) {
      builder.field(entry.getKey(), entry.getValue());
    }
    builder.endObject();
    return builder;
  }
}
