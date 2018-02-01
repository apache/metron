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
package org.apache.metron.solr.dao;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrDao implements IndexDao {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String ROOT_FIELD = "_root_";
  public static final String VERSION_FIELD = "_version_";

  private transient SolrClient client;
  private SolrSearchDao solrSearchDao;
  private SolrUpdateDao solrUpdateDao;
  private ColumnMetadataDao solrColumnMetadataDao;

  private AccessConfig accessConfig;

  protected SolrDao(SolrClient client,
      AccessConfig config,
      SolrSearchDao solrSearchDao,
      SolrUpdateDao solrUpdateDao,
      SolrColumnMetadataDao solrColumnMetadataDao) {
    this.client = client;
    this.accessConfig = config;
    this.solrSearchDao = solrSearchDao;
    this.solrUpdateDao = solrUpdateDao;
    this.solrColumnMetadataDao = solrColumnMetadataDao;
  }

  public SolrDao() {
    //uninitialized.
  }

  @Override
  public void init(AccessConfig config) {
    if (this.client == null) {
      Map<String, Object> globalConfig = config.getGlobalConfigSupplier().get();
      String zkHost = (String) globalConfig.get("solr.zookeeper");
      this.client = new CloudSolrClient.Builder().withZkHost((String) globalConfig.get("solr.zookeeper")).build();
      this.accessConfig = config;
      this.solrSearchDao = new SolrSearchDao(this.client, this.accessConfig);
      this.solrUpdateDao = new SolrUpdateDao(this.client);
      this.solrColumnMetadataDao = new SolrColumnMetadataDao(zkHost);
    }
  }

  @Override
  public SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException {
    return this.solrSearchDao.search(searchRequest);
  }

  @Override
  public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
    return this.solrSearchDao.group(groupRequest);
  }

  @Override
  public Document getLatest(String guid, String collection) throws IOException {
    return this.solrSearchDao.getLatest(guid, collection);
  }

  @Override
  public Iterable<Document> getAllLatest(List<GetRequest> getRequests) throws IOException {
    return this.solrSearchDao.getAllLatest(getRequests);
  }

  @Override
  public void update(Document update, Optional<String> index) throws IOException {
    this.solrUpdateDao.update(update, index);
  }

  @Override
  public void batchUpdate(Map<Document, Optional<String>> updates) throws IOException {
    this.solrUpdateDao.batchUpdate(updates);
  }

  @Override
  public Map<String, FieldType> getColumnMetadata(List<String> indices) throws IOException {
    return this.solrColumnMetadataDao.getColumnMetadata(indices);
  }
}
