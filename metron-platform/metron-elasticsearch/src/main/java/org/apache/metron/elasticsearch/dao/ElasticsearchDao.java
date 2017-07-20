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

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.elasticsearch.utils.ElasticsearchUtils;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.Document;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.search.*;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SortOrder;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.*;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

public class ElasticsearchDao implements IndexDao {
  private transient TransportClient client;
  private AccessConfig accessConfig;

  protected ElasticsearchDao(TransportClient client, AccessConfig config) {
    this.client = client;
    this.accessConfig = config;
  }

  public ElasticsearchDao() {
    //uninitialized.
  }

  @Override
  public SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException {
    if(client == null) {
      throw new InvalidSearchException("Uninitialized Dao!  You must call init() prior to use.");
    }
    if (searchRequest.getSize() > accessConfig.getMaxSearchResults()) {
      throw new InvalidSearchException("Search result size must be less than " + accessConfig.getMaxSearchResults());
    }
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
            .size(searchRequest.getSize())
            .from(searchRequest.getFrom())
            .query(new QueryStringQueryBuilder(searchRequest.getQuery()))
            .fetchSource(true)
            .trackScores(true);
    for (SortField sortField : searchRequest.getSort()) {
      FieldSortBuilder fieldSortBuilder = new FieldSortBuilder(sortField.getField());
      if (sortField.getSortOrder() == org.apache.metron.indexing.dao.search.SortOrder.DESC) {
        fieldSortBuilder.order(org.elasticsearch.search.sort.SortOrder.DESC);
      } else {
        fieldSortBuilder.order(org.elasticsearch.search.sort.SortOrder.ASC);
      }
      searchSourceBuilder = searchSourceBuilder.sort(fieldSortBuilder);
    }
    String[] wildcardIndices = searchRequest.getIndices().stream().map(index -> String.format("%s*", index)).toArray(value -> new String[searchRequest.getIndices().size()]);
    org.elasticsearch.action.search.SearchResponse elasticsearchResponse = client.search(new org.elasticsearch.action.search.SearchRequest(wildcardIndices)
            .source(searchSourceBuilder)).actionGet();
    SearchResponse searchResponse = new SearchResponse();
    searchResponse.setTotal(elasticsearchResponse.getHits().getTotalHits());
    searchResponse.setResults(Arrays.stream(elasticsearchResponse.getHits().getHits()).map(searchHit -> {
      SearchResult searchResult = new SearchResult();
      searchResult.setId(searchHit.getId());
      searchResult.setSource(searchHit.getSource());
      searchResult.setScore(searchHit.getScore());
      return searchResult;
    }).collect(Collectors.toList()));
    return searchResponse;
  }

  @Override
  public synchronized void init(Map<String, Object> globalConfig, AccessConfig config) {
    if(this.client == null) {
      this.client = ElasticsearchUtils.getClient(globalConfig, config.getOptionalSettings());
      this.accessConfig = config;
    }
  }

  @Override
  public Document getLatest(String uuid, String sensorType) throws IOException {
    QueryBuilder query =  QueryBuilders.matchQuery(Constants.GUID, uuid);
    SearchRequestBuilder request = client.prepareSearch()
                                         .setTypes(sensorType + "_doc")
                                         .setQuery(query)
                                         .setSource("message")
                                         ;
    MultiSearchResponse response = client.prepareMultiSearch()
                                         .add(request)
                                         .get();
    //TODO: Fix this to
    //      * handle multiple responses
    //      * be more resilient to error
    for(MultiSearchResponse.Item i : response) {
      org.elasticsearch.action.search.SearchResponse resp = i.getResponse();
      SearchHits hits = resp.getHits();
      for(SearchHit hit : hits) {
        Long ts = 0L;
        String doc = hit.getSourceAsString();
        String sourceType = Iterables.getFirst(Splitter.on("_doc").split(hit.getType()), null);
        Document d = new Document(doc, uuid, sourceType, ts);
        return d;
      }
    }
    return null;
  }

  @Override
  public void update(Document update, WriterConfiguration configurations) throws IOException {
    String indexPostfix = ElasticsearchUtils.getIndexFormat(configurations).format(new Date());
    String sensorType = update.getSensorType();
    String indexName = ElasticsearchUtils.getIndexName(sensorType, indexPostfix, configurations);
    IndexRequestBuilder indexRequestBuilder = client.prepareIndex(indexName,
            sensorType + "_doc");

    indexRequestBuilder = indexRequestBuilder.setSource(update.getDocument());
    Object ts = update.getTimestamp();
    if(ts != null) {
      indexRequestBuilder = indexRequestBuilder.setTimestamp(ts.toString());
    }

    BulkResponse bulkResponse = client.prepareBulk().add(indexRequestBuilder).execute().actionGet();
    if(bulkResponse.hasFailures()) {
      throw new IOException(bulkResponse.buildFailureMessage());
    }
  }

}
