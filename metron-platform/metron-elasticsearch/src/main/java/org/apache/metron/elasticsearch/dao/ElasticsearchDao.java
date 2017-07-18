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

import org.apache.metron.elasticsearch.utils.ElasticsearchUtils;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.search.*;
import org.apache.metron.indexing.dao.search.SortOrder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.*;

import java.util.Arrays;
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
  public void init(Map<String, Object> globalConfig, AccessConfig config) {
    this.client = ElasticsearchUtils.getClient(globalConfig, config.getOptionalSettings());
    this.accessConfig = config;
  }
}
