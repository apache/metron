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
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.index.mapper.ip.IpFieldMapper;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.DoubleTerms;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
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
    Optional<List<String>> facetFields = searchRequest.getFacetFields();
    if (facetFields.isPresent()) {
      addFacetFields(searchSourceBuilder, facetFields.get());
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
    searchResponse.setResults(Arrays.stream(elasticsearchResponse.getHits().getHits()).map(searchHit -> {
      SearchResult searchResult = new SearchResult();
      searchResult.setId(searchHit.getId());
      searchResult.setSource(searchHit.getSource());
      searchResult.setScore(searchHit.getScore());
      searchResult.setIndex(searchHit.getIndex());
      return searchResult;
    }).collect(Collectors.toList()));
    if (facetFields.isPresent()) {
      Map<String, FieldType> commonColumnMetadata;
      try {
        commonColumnMetadata = getCommonColumnMetadata(searchRequest.getIndices());
      } catch (IOException e) {
        throw new InvalidSearchException(String.format("Could not get common column metadata for indices %s", Arrays.toString(searchRequest.getIndices().toArray())));
      }
      searchResponse.setFacetCounts(getFacetCounts(facetFields.get(), elasticsearchResponse.getAggregations(), commonColumnMetadata ));
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

  public void addFacetFields(SearchSourceBuilder searchSourceBuilder, List<String> fields) {
    for(String field: fields) {
      searchSourceBuilder = searchSourceBuilder.aggregation(new TermsBuilder(getAggregationName(field)).field(field));
    }
  }

  public Map<String, Map<String, Long>> getFacetCounts(List<String> fields, Aggregations aggregations, Map<String, FieldType> commonColumnMetadata) {
    Map<String, Map<String, Long>> fieldCounts = new HashMap<>();
    for (String field: fields) {
      Map<String, Long> valueCounts = new HashMap<>();
      Aggregation aggregation = aggregations.get(getAggregationName(field));
      if (aggregation instanceof LongTerms) {
        LongTerms longTerms = (LongTerms) aggregation;
        FieldType type = commonColumnMetadata.get(field);
        if (FieldType.IP.equals(type)) {
          longTerms.getBuckets().stream().forEach(bucket -> valueCounts.put(IpFieldMapper.longToIp((Long) bucket.getKey()), bucket.getDocCount()));
        } else if (FieldType.BOOLEAN.equals(type)) {
          longTerms.getBuckets().stream().forEach(bucket -> {
            String key = (Long) bucket.getKey() == 1 ? "true" : "false";
            valueCounts.put(key, bucket.getDocCount());
          });
        } else {
          longTerms.getBuckets().stream().forEach(bucket -> valueCounts.put(bucket.getKeyAsString(), bucket.getDocCount()));
        }
      } else if (aggregation instanceof DoubleTerms) {
        DoubleTerms doubleTerms = (DoubleTerms) aggregation;
        doubleTerms.getBuckets().stream().forEach(bucket -> valueCounts.put(bucket.getKeyAsString(), bucket.getDocCount()));
      } else if (aggregation instanceof StringTerms) {
        StringTerms stringTerms = (StringTerms) aggregation;
        stringTerms.getBuckets().stream().forEach(bucket -> valueCounts.put(bucket.getKeyAsString(), bucket.getDocCount()));
      }
      fieldCounts.put(field, valueCounts);
    }
    return fieldCounts;
  }

  private String getAggregationName(String field) {
    return String.format("%s_count", field);
  }
}
