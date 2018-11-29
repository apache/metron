/**
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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.metron.elasticsearch.client.ElasticsearchClient;
import org.apache.metron.elasticsearch.client.ElasticsearchClientFactory;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.RetrieveLatestDao;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.update.CommentAddRemoveRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.OriginalNotFoundException;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.apache.metron.indexing.dao.update.ReplaceRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.index.query.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticsearchDao implements IndexDao {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private transient ElasticsearchClient client;
  private ElasticsearchSearchDao searchDao;
  private ElasticsearchUpdateDao updateDao;
  private ElasticsearchRetrieveLatestDao retrieveLatestDao;

  /**
   * Retrieves column metadata about search indices.
   */
  private ElasticsearchColumnMetadataDao columnMetadataDao;

  /**
   * Handles the submission of search requests to Elasticsearch.
   */
  private ElasticsearchRequestSubmitter requestSubmitter;

  private AccessConfig accessConfig;
  private WriteRequest.RefreshPolicy refreshPolicy;

  public ElasticsearchDao(ElasticsearchClient client,
      AccessConfig config,
      ElasticsearchSearchDao searchDao,
      ElasticsearchUpdateDao updateDao,
      ElasticsearchRetrieveLatestDao retrieveLatestDao,
      ElasticsearchColumnMetadataDao columnMetadataDao,
      ElasticsearchRequestSubmitter requestSubmitter
  ) {
    this.client = client;
    this.searchDao = searchDao;
    this.updateDao = updateDao;
    this.retrieveLatestDao = retrieveLatestDao;
    this.columnMetadataDao = columnMetadataDao;
    this.requestSubmitter = requestSubmitter;
    this.accessConfig = config;
  }

  public ElasticsearchDao() {
    //uninitialized.
    refreshPolicy = WriteRequest.RefreshPolicy.NONE;
  }

  public AccessConfig getAccessConfig() {
    return accessConfig;
  }

  public void setAccessConfig(AccessConfig accessConfig) {
    this.accessConfig = accessConfig;
  }

  @Override
  public synchronized void init(AccessConfig config) {
    if (this.client == null) {
      this.client = ElasticsearchClientFactory.create(config.getGlobalConfigSupplier().get());
      this.accessConfig = config;
      this.columnMetadataDao = new ElasticsearchColumnMetadataDao(this.client);
      this.requestSubmitter = new ElasticsearchRequestSubmitter(this.client);
      this.searchDao = new ElasticsearchSearchDao(client, accessConfig, columnMetadataDao, requestSubmitter);
      this.retrieveLatestDao = new ElasticsearchRetrieveLatestDao(client);
      this.updateDao = new ElasticsearchUpdateDao(client, accessConfig, retrieveLatestDao)
              .withRefreshPolicy(refreshPolicy);
    }

    if (columnMetadataDao == null) {
      throw new IllegalArgumentException("No ColumnMetadataDao available");
    }

    if (requestSubmitter == null) {
      throw new IllegalArgumentException("No ElasticsearchRequestSubmitter available");
    }
  }

  @Override
  public SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException {
    return this.searchDao.search(searchRequest);
  }

  @Override
  public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
    return this.searchDao.group(groupRequest);
  }

  @Override
  public Document getLatest(final String guid, final String sensorType) throws IOException {
    return retrieveLatestDao.getLatest(guid, sensorType);
  }

  @Override
  public Iterable<Document> getAllLatest(
      final List<GetRequest> getRequests) throws IOException {
    return retrieveLatestDao.getAllLatest(getRequests);
  }

  @Override
  public Document update(Document update, Optional<String> index) throws IOException {
    return updateDao.update(update, index);
  }

  @Override
  public Map<Document, Optional<String>> batchUpdate(Map<Document, Optional<String>> updates) throws IOException {
    return updateDao.batchUpdate(updates);
  }

  @Override
  public Document patch(RetrieveLatestDao retrieveLatestDao, PatchRequest request, Optional<Long> timestamp)
      throws OriginalNotFoundException, IOException {
    return updateDao.patch(retrieveLatestDao, request, timestamp);
  }

  @Override
  public Document replace(ReplaceRequest request, Optional<Long> timestamp) throws IOException {
    return updateDao.replace(request, timestamp);
  }

  @Override
  public Document addCommentToAlert(CommentAddRemoveRequest request) throws IOException {
    return updateDao.addCommentToAlert(request);
  }

  @Override
  public Document removeCommentFromAlert(CommentAddRemoveRequest request) throws IOException {
    return updateDao.removeCommentFromAlert(request);
  }

  @Override
  public Map<String, FieldType> getColumnMetadata(List<String> indices) throws IOException {
    return this.columnMetadataDao.getColumnMetadata(indices);
  }

  @Override
  public Optional<Map<String, Object>> getLatestResult(GetRequest request) throws IOException {
    return retrieveLatestDao.getLatestResult(request);
  }

  @Override
  public Document addCommentToAlert(CommentAddRemoveRequest request, Document latest) throws IOException {
    return this.updateDao.addCommentToAlert(request, latest);
  }

  @Override
  public Document removeCommentFromAlert(CommentAddRemoveRequest request, Document latest) throws IOException {
    return this.updateDao.removeCommentFromAlert(request, latest);
  }

  public ElasticsearchDao withRefreshPolicy(WriteRequest.RefreshPolicy refreshPolicy) {
    this.refreshPolicy = refreshPolicy;
    return this;
  }

  protected Optional<String> getIndexName(String guid, String sensorType) throws IOException {
    return updateDao.getIndexName(guid, sensorType);
  }

  protected SearchResponse search(SearchRequest request, QueryBuilder queryBuilder)
      throws InvalidSearchException {
    return searchDao.search(request, queryBuilder);
  }

  protected GroupResponse group(GroupRequest groupRequest, QueryBuilder queryBuilder)
      throws InvalidSearchException {
    return searchDao.group(groupRequest, queryBuilder);
  }

  public ElasticsearchClient getClient() {
    return this.client;
  }
}
