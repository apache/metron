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
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.elasticsearch.utils.ElasticsearchUtils;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.search.*;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.*;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
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
      searchResult.setId(searchHit.getIndex());
      return searchResult;
    }).collect(Collectors.toList()));
    return searchResponse;
  }

  @Override
  public synchronized void init(AccessConfig config) {
    if(this.client == null) {
      this.client = ElasticsearchUtils.getClient(config.getGlobalConfigSupplier().get(), config.getOptionalSettings());
      this.accessConfig = config;
    }
  }

  @Override
  public Document getLatest(final String uuid, final String sensorType) throws IOException {
    Optional<Document> ret = searchByUuid(
            uuid
            , sensorType
            , hit -> {
              Long ts = 0L;
              String doc = hit.getSourceAsString();
              String sourceType = Iterables.getFirst(Splitter.on("_doc").split(hit.getType()), null);
              try {
                return Optional.of(new Document(doc, uuid, sourceType, ts));
              } catch (IOException e) {
                throw new IllegalStateException("Unable to retrieve latest: " + e.getMessage(), e);
              }
            }
            );
    return ret.orElse(null);
  }

  <T> Optional<T> searchByUuid(String uuid, String sensorType, Function<SearchHit, Optional<T>> callback) throws IOException{
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
        Optional<T> ret = callback.apply(hit);
        if(ret.isPresent()) {
          return ret;
        }
      }
    }
    return Optional.empty();

  }

  @Override
  public void update(Document update, Optional<String> index) throws IOException {
    String indexPostfix = ElasticsearchUtils.getIndexFormat(accessConfig.getGlobalConfigSupplier().get()).format(new Date());
    String sensorType = update.getSensorType();
    String indexName = ElasticsearchUtils.getIndexName(sensorType, indexPostfix, null);

    String type = sensorType + "_doc";
    byte[] source = JSONUtils.INSTANCE.toJSON(update.getDocument());
    Object ts = update.getTimestamp();
    IndexRequest indexRequest = new IndexRequest(indexName, type, update.getUuid())
            .source(source)
            ;
    if(ts != null) {
      indexRequest = indexRequest.timestamp(ts.toString());
    }
    String existingIndex = index.orElse(
            searchByUuid(update.getUuid()
                        , sensorType
                        , hit -> Optional.ofNullable(hit.getIndex())
                        ).orElse(indexName)
                                       );
    UpdateRequest updateRequest = new UpdateRequest(existingIndex, type, update.getUuid())
            .doc(source)
            .upsert(indexRequest)
            ;

    try {
      client.update(updateRequest).get();
    } catch (Exception e) {
      throw new IOException(e.getMessage(), e);
    }
  }

}
