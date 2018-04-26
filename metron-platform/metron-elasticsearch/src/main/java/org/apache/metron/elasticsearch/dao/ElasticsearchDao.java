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
import org.apache.metron.elasticsearch.utils.ElasticsearchUtils;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.ColumnMetadataDao;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.update.Document;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticsearchDao implements IndexDao {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private transient TransportClient client;
  private ElasticsearchSearchDao searchDao;
  private ElasticsearchUpdateDao updateDao;

  /**
   * Retrieves column metadata about search indices.
   */
  private ElasticsearchColumnMetadataDao columnMetadataDao;

  /**
   * Handles the submission of search requests to Elasticsearch.
   */
  private ElasticsearchRequestSubmitter requestSubmitter;

  private AccessConfig accessConfig;

  protected ElasticsearchDao(TransportClient client,
      AccessConfig config,
      ElasticsearchSearchDao searchDao,
      ElasticsearchUpdateDao updateDao,
      ElasticsearchColumnMetadataDao columnMetadataDao,
      ElasticsearchRequestSubmitter requestSubmitter
                             ) {
    this.client = client;
    this.searchDao = searchDao;
    this.updateDao = updateDao;
    this.columnMetadataDao = columnMetadataDao;
    this.requestSubmitter = requestSubmitter;
    this.accessConfig = config;
  }

  public ElasticsearchDao() {
    //uninitialized.
  }

  public ElasticsearchDao columnMetadataDao(ElasticsearchColumnMetadataDao columnMetadataDao) {
    this.columnMetadataDao = columnMetadataDao;
    return this;
  }

  public ElasticsearchDao accessConfig(AccessConfig accessConfig) {
    this.accessConfig = accessConfig;
    return this;
  }

  @Override
  public synchronized void init(AccessConfig config) {
    if(this.client == null) {
      this.client = ElasticsearchUtils.getClient(config.getGlobalConfigSupplier().get());
      this.accessConfig = config;
      this.columnMetadataDao = new ElasticsearchColumnMetadataDao(this.client.admin());
      this.requestSubmitter = new ElasticsearchRequestSubmitter(this.client);
      this.searchDao = new ElasticsearchSearchDao(client, accessConfig, columnMetadataDao, requestSubmitter);
      this.updateDao = new ElasticsearchUpdateDao(client, accessConfig, searchDao);
    }

    if(columnMetadataDao == null) {
      throw new IllegalArgumentException("No ColumnMetadataDao available");
    }

    if(requestSubmitter == null) {
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
    return searchDao.getLatest(guid, sensorType);
  }

  @Override
  public Iterable<Document> getAllLatest(
      final List<GetRequest> getRequests) throws IOException {
    return searchDao.getAllLatest(getRequests);
  }

  @Override
  public void update(Document update, Optional<String> index) throws IOException {
    updateDao.update(update, index);
  }

  @Override
  public void batchUpdate(Map<Document, Optional<String>> updates) throws IOException {
    updateDao.batchUpdate(updates);
  }

  @Override
  public Map<String, FieldType> getColumnMetadata(List<String> indices) throws IOException {
    return this.columnMetadataDao.getColumnMetadata(indices);
  }

  protected Optional<String> getIndexName(String guid, String sensorType) {
    return updateDao.getIndexName(guid, sensorType);
  }

  protected SearchResponse search(SearchRequest request, QueryBuilder queryBuilder) throws InvalidSearchException {
    return searchDao.search(request, queryBuilder);
  }

  protected GroupResponse group(GroupRequest groupRequest, QueryBuilder queryBuilder) throws InvalidSearchException {
    return searchDao.group(groupRequest, queryBuilder);
  }

  public TransportClient getClient() {
    return this.client;
  }
}
