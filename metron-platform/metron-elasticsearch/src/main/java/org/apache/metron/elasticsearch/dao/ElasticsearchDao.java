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
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ElasticsearchDao implements IndexDao {
  private transient TransportClient client;
  private AccessConfig accessConfig;
  private List<String> ignoredIndices = new ArrayList<>();

  protected ElasticsearchDao(TransportClient client, AccessConfig config) {
    this.client = client;
    this.accessConfig = config;
    this.ignoredIndices.add(".kibana");
  }

  public ElasticsearchDao() {
    //uninitialized.
  }

  private static Map<String, FieldType> elasticsearchSearchTypeMap;

  static {
    Map<String, FieldType> fieldTypeMap = new HashMap<>();
    fieldTypeMap.put("string", FieldType.STRING);
    fieldTypeMap.put("ip", FieldType.IP);
    fieldTypeMap.put("integer", FieldType.INTEGER);
    fieldTypeMap.put("long", FieldType.LONG);
    fieldTypeMap.put("date", FieldType.DATE);
    fieldTypeMap.put("float", FieldType.FLOAT);
    fieldTypeMap.put("double", FieldType.DOUBLE);
    fieldTypeMap.put("boolean", FieldType.BOOLEAN);
    elasticsearchSearchTypeMap = Collections.unmodifiableMap(fieldTypeMap);
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

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, Map<String, FieldType>> getColumnMetadata(List<String> indices) throws IOException {
    Map<String, Map<String, FieldType>> allColumnMetadata = new HashMap<>();
    ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings =
            client.admin().indices().getMappings(new GetMappingsRequest().indices(getLatestIndices(indices))).actionGet().getMappings();
    for(Object index: mappings.keys().toArray()) {
      Map<String, FieldType> indexColumnMetadata = new HashMap<>();
      ImmutableOpenMap<String, MappingMetaData> mapping = mappings.get(index.toString());
      Iterator<String> mappingIterator = mapping.keysIt();
      while(mappingIterator.hasNext()) {
        MappingMetaData mappingMetaData = mapping.get(mappingIterator.next());
        Map<String, Map<String, String>> map = (Map<String, Map<String, String>>) mappingMetaData.getSourceAsMap().get("properties");
        for(String field: map.keySet()) {
          indexColumnMetadata.put(field, elasticsearchSearchTypeMap.getOrDefault(map.get(field).get("type"), FieldType.OTHER));
        }
      }
      allColumnMetadata.put(index.toString().split("_index_")[0], indexColumnMetadata);
    }
    return allColumnMetadata;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, FieldType> getCommonColumnMetadata(List<String> indices) throws IOException {
    Map<String, FieldType> commonColumnMetadata = null;
    ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings =
            client.admin().indices().getMappings(new GetMappingsRequest().indices(getLatestIndices(indices))).actionGet().getMappings();
    for(Object index: mappings.keys().toArray()) {
      ImmutableOpenMap<String, MappingMetaData> mapping = mappings.get(index.toString());
      Iterator<String> mappingIterator = mapping.keysIt();
      while(mappingIterator.hasNext()) {
        MappingMetaData mappingMetaData = mapping.get(mappingIterator.next());
        Map<String, Map<String, String>> map = (Map<String, Map<String, String>>) mappingMetaData.getSourceAsMap().get("properties");
        Map<String, FieldType> mappingsWithTypes = map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e-> elasticsearchSearchTypeMap.getOrDefault(e.getValue().get("type"), FieldType.OTHER)));
        if (commonColumnMetadata == null) {
          commonColumnMetadata = mappingsWithTypes;
        } else {
          commonColumnMetadata.entrySet().retainAll(mappingsWithTypes.entrySet());
        }
      }
    }
    return commonColumnMetadata;
  }

  protected String[] getLatestIndices(List<String> includeIndices) {
    Map<String, String> latestIndices = new HashMap<>();
    String[] indices = client.admin().indices().prepareGetIndex().setFeatures().get().getIndices();
    for (String index : indices) {
      if (!ignoredIndices.contains(index)) {
        int prefixEnd = index.indexOf("_index_");
        if (prefixEnd != -1) {
          String prefix = index.substring(0, prefixEnd);
          if (includeIndices.contains(prefix)) {
            String latestIndex = latestIndices.get(prefix);
            if (latestIndex == null || index.compareTo(latestIndex) > 0) {
              latestIndices.put(prefix, index);
            }
          }
        }
      }
    }
    return latestIndices.values().toArray(new String[latestIndices.size()]);
  }

}
