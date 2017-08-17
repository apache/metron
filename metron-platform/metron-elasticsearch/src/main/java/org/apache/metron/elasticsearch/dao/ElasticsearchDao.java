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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.metron.common.Constants;
import org.apache.metron.elasticsearch.utils.ElasticsearchUtils;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.metron.indexing.dao.search.SearchResultGroup;
import org.apache.metron.indexing.dao.update.Document;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.index.mapper.ip.IpFieldMapper;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

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
    final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.query(new QueryStringQueryBuilder(searchRequest.getQuery()));
    Optional<List<String>> groupByFields = searchRequest.getGroupByFields();
    if (groupByFields.isPresent()) {
      searchSourceBuilder.aggregation(getGroupsTermBuilder(searchRequest, 0));
    } else {
      searchSourceBuilder.size(searchRequest.getSize())
          .from(searchRequest.getFrom())
          .fetchSource(true)
          .trackScores(true);
      searchRequest.getSort().forEach(sortField -> searchSourceBuilder.sort(sortField.getField(), getElasticsearchSortOrder(sortField.getSortOrder())));
    }
    Optional<List<String>> facetFields = searchRequest.getFacetFields();
    if (facetFields.isPresent()) {
      facetFields.get().forEach(field -> searchSourceBuilder.aggregation(new TermsBuilder(getFacentAggregationName(field)).field(field)));
    }
    String[] wildcardIndices = searchRequest.getIndices().stream().map(index -> String.format("%s*", index)).toArray(value -> new String[searchRequest.getIndices().size()]);
    org.elasticsearch.action.search.SearchResponse elasticsearchResponse;
    try {
      elasticsearchResponse = client.search(new org.elasticsearch.action.search.SearchRequest(wildcardIndices)
              .source(searchSourceBuilder)).actionGet();
    } catch (SearchPhaseExecutionException e) {
      throw new InvalidSearchException("Could not execute search", e);
    }
    SearchResponse searchResponse = new SearchResponse();
    searchResponse.setTotal(elasticsearchResponse.getHits().getTotalHits());
    if (!groupByFields.isPresent()) {
      searchResponse.setResults(Arrays.stream(elasticsearchResponse.getHits().getHits()).map(this::getSearchResult).collect(Collectors.toList()));
    }
    if (groupByFields.isPresent() || facetFields.isPresent()) {
      Map<String, FieldType> commonColumnMetadata;
      try {
        commonColumnMetadata = getCommonColumnMetadata(searchRequest.getIndices());
      } catch (IOException e) {
        throw new InvalidSearchException(String.format("Could not get common column metadata for indices %s", Arrays.toString(searchRequest.getIndices().toArray())));
      }
      if (groupByFields.isPresent()) {
        searchResponse.setGroupedBy(groupByFields.get().get(0));
        searchResponse.setGroups(getGroups(groupByFields.get(), 0, elasticsearchResponse.getAggregations(), commonColumnMetadata));
      }
      if (facetFields.isPresent()) {
        searchResponse.setFacetCounts(getFacetCounts(facetFields.get(), elasticsearchResponse.getAggregations(), commonColumnMetadata ));
      }
    }

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
  public Document getLatest(final String guid, final String sensorType) throws IOException {
    Optional<Document> ret = searchByGuid(
            guid
            , sensorType
            , hit -> {
              Long ts = 0L;
              String doc = hit.getSourceAsString();
              String sourceType = Iterables.getFirst(Splitter.on("_doc").split(hit.getType()), null);
              try {
                return Optional.of(new Document(doc, guid, sourceType, ts));
              } catch (IOException e) {
                throw new IllegalStateException("Unable to retrieve latest: " + e.getMessage(), e);
              }
            }
            );
    return ret.orElse(null);
  }

  /**
   * Return the search hit based on the UUID and sensor type.
   * A callback can be specified to transform the hit into a type T.
   * If more than one hit happens, the first one will be returned.
   * @throws IOException
   */
  <T> Optional<T> searchByGuid(String guid, String sensorType, Function<SearchHit, Optional<T>> callback) throws IOException{
    QueryBuilder query =  QueryBuilders.matchQuery(Constants.GUID, guid);
    SearchRequestBuilder request = client.prepareSearch()
                                         .setTypes(sensorType + "_doc")
                                         .setQuery(query)
                                         .setSource("message")
                                         ;
    MultiSearchResponse response = client.prepareMultiSearch()
                                         .add(request)
                                         .get();
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
    Object ts = update.getTimestamp();
    IndexRequest indexRequest = new IndexRequest(indexName, type, update.getGuid())
            .source(update.getDocument())
            ;
    if(ts != null) {
      indexRequest = indexRequest.timestamp(ts.toString());
    }
    String existingIndex = index.orElse(
            searchByGuid(update.getGuid()
                        , sensorType
                        , hit -> Optional.ofNullable(hit.getIndex())
                        ).orElse(indexName)
                                       );
    UpdateRequest updateRequest = new UpdateRequest(existingIndex, type, update.getGuid())
            .doc(update.getDocument())
            .upsert(indexRequest)
            ;

    try {
      client.update(updateRequest).get();
    } catch (Exception e) {
      throw new IOException(e.getMessage(), e);
    }
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

  private org.elasticsearch.search.sort.SortOrder getElasticsearchSortOrder(
      org.apache.metron.indexing.dao.search.SortOrder sortOrder) {
    return sortOrder == org.apache.metron.indexing.dao.search.SortOrder.DESC ?
        org.elasticsearch.search.sort.SortOrder.DESC : org.elasticsearch.search.sort.SortOrder.ASC;
  }

  public Map<String, Map<String, Long>> getFacetCounts(List<String> fields, Aggregations aggregations, Map<String, FieldType> commonColumnMetadata) {
    Map<String, Map<String, Long>> fieldCounts = new HashMap<>();
    for (String field: fields) {
      Map<String, Long> valueCounts = new HashMap<>();
      Aggregation aggregation = aggregations.get(getFacentAggregationName(field));
      if (aggregation instanceof Terms) {
        Terms terms = (Terms) aggregation;
        terms.getBuckets().stream().forEach(bucket -> valueCounts.put(formatKey(bucket.getKey(), commonColumnMetadata.get(field)), bucket.getDocCount()));
      }
      fieldCounts.put(field, valueCounts);
    }
    return fieldCounts;
  }

  private String formatKey(Object key, FieldType type) {
    if (FieldType.IP.equals(type)) {
      return IpFieldMapper.longToIp((Long) key);
    } else if (FieldType.BOOLEAN.equals(type)) {
      return (Long) key == 1 ? "true" : "false";
    } else {
      return key.toString();
    }
  }

  private TermsBuilder getGroupsTermBuilder(SearchRequest searchRequest, int index) {
    List<String> fields = searchRequest.getGroupByFields().get();
    String field = fields.get(index);
    String aggregationName = getGroupByAggregationName(field);
    if (index < fields.size() - 1) {
      return new TermsBuilder(aggregationName).field(field)
          .subAggregation(getGroupsTermBuilder(searchRequest, index + 1));
    } else {
      TopHitsBuilder topHitsBuilder = new TopHitsBuilder("top_hits").setSize(searchRequest.getSize())
          .setFetchSource(true);
      searchRequest.getSort().forEach(sortField ->
          topHitsBuilder.addSort(sortField.getField(), getElasticsearchSortOrder(sortField.getSortOrder())));
      return new TermsBuilder(aggregationName).field(field).subAggregation(topHitsBuilder);
    }
  }

  private List<SearchResultGroup> getGroups(List<String> fields, int index, Aggregations aggregations, Map<String, FieldType> commonColumnMetadata) {
    String field = fields.get(index);
    Terms terms = aggregations.get(getGroupByAggregationName(field));
    List<SearchResultGroup> searchResultGroups = new ArrayList<>();
    if (index < fields.size() - 1) {
      String childField = fields.get(index + 1);
      for(Bucket bucket: terms.getBuckets()) {
        SearchResultGroup searchResultGroup = new SearchResultGroup();
        searchResultGroup.setKey(formatKey(bucket.getKey(), commonColumnMetadata.get(field)));
        searchResultGroup.setTotal(bucket.getDocCount());
        searchResultGroup.setGroupedBy(childField);
        searchResultGroup.setGroups(getGroups(fields, index + 1, bucket.getAggregations(), commonColumnMetadata));
        searchResultGroups.add(searchResultGroup);
      }
    } else {
      for(Bucket bucket: terms.getBuckets()) {
        SearchResultGroup searchResultGroup = new SearchResultGroup();
        searchResultGroup.setKey(formatKey(bucket.getKey(), commonColumnMetadata.get(field)));
        searchResultGroup.setTotal(bucket.getDocCount());
        TopHits topHits = bucket.getAggregations().get("top_hits");
        searchResultGroup.setResults(Arrays.stream(topHits.getHits().getHits()).map(this::getSearchResult).collect(Collectors.toList()));
        searchResultGroups.add(searchResultGroup);
      }
    }
    return searchResultGroups;
  }

  private SearchResult getSearchResult(SearchHit searchHit) {
    SearchResult searchResult = new SearchResult();
    searchResult.setId(searchHit.getId());
    searchResult.setSource(searchHit.getSource());
    searchResult.setScore(searchHit.getScore());
    searchResult.setIndex(searchHit.getIndex());
    return searchResult;
  }

  private String getFacentAggregationName(String field) {
    return String.format("%s_count", field);
  }

  private String getGroupByAggregationName(String field) {
    return String.format("%s_group", field);
  }
}
