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

import static org.apache.metron.elasticsearch.utils.ElasticsearchUtils.INDEX_NAME_DELIMITER;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.apache.metron.elasticsearch.utils.ElasticsearchUtils;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.Group;
import org.apache.metron.indexing.dao.search.GroupOrder;
import org.apache.metron.indexing.dao.search.GroupOrderType;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.GroupResult;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchDao;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.metron.indexing.dao.search.SortField;
import org.apache.metron.indexing.dao.search.SortOrder;
import org.apache.metron.indexing.dao.update.Document;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.mapper.LegacyIpFieldMapper;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Order;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.elasticsearch.search.aggregations.metrics.sum.SumAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticsearchSearchDao implements SearchDao {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * The value required to ensure that Elasticsearch sorts missing values last.
   */
  private static final String SORT_MISSING_LAST = "_last";

  /**
   * The value required to ensure that Elasticsearch sorts missing values last.
   */
  private static final String SORT_MISSING_FIRST = "_first";

  private transient TransportClient client;
  private AccessConfig accessConfig;
  private ElasticsearchColumnMetadataDao columnMetadataDao;
  private ElasticsearchRequestSubmitter requestSubmitter;

  public ElasticsearchSearchDao(TransportClient client,
      AccessConfig accessConfig,
      ElasticsearchColumnMetadataDao columnMetadataDao,
      ElasticsearchRequestSubmitter requestSubmitter) {
    this.client = client;
    this.accessConfig = accessConfig;
    this.columnMetadataDao = columnMetadataDao;
    this.requestSubmitter = requestSubmitter;
  }

  @Override
  public SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException {
    if(searchRequest.getQuery() == null) {
      throw new InvalidSearchException("Search query is invalid: null");
    }
    return search(searchRequest, new QueryStringQueryBuilder(searchRequest.getQuery()));
  }

  @Override
  public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
    return group(groupRequest, new QueryStringQueryBuilder(groupRequest.getQuery()));
  }

  /**
   * Defers to a provided {@link org.elasticsearch.index.query.QueryBuilder} for the query.
   * @param request The request defining the parameters of the search
   * @param queryBuilder The actual query to be run. Intended for if the SearchRequest requires wrapping
   * @return The results of the query
   * @throws InvalidSearchException When the query is malformed or the current state doesn't allow search
   */
  protected SearchResponse search(SearchRequest request, QueryBuilder queryBuilder) throws InvalidSearchException {
    org.elasticsearch.action.search.SearchRequest esRequest;
    org.elasticsearch.action.search.SearchResponse esResponse;

    if(client == null) {
      throw new InvalidSearchException("Uninitialized Dao!  You must call init() prior to use.");
    }

    if (request.getSize() > accessConfig.getMaxSearchResults()) {
      throw new InvalidSearchException("Search result size must be less than " + accessConfig.getMaxSearchResults());
    }

    esRequest = buildSearchRequest(request, queryBuilder);
    esResponse = requestSubmitter.submitSearch(esRequest);
    return buildSearchResponse(request, esResponse);
  }

  /**
   * Builds an Elasticsearch search request.
   * @param searchRequest The Metron search request.
   * @param queryBuilder
   * @return An Elasticsearch search request.
   */
  private org.elasticsearch.action.search.SearchRequest buildSearchRequest(
      SearchRequest searchRequest,
      QueryBuilder queryBuilder) throws InvalidSearchException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Got search request; request={}", ElasticsearchUtils.toJSON(searchRequest).orElse("???"));
    }
    SearchSourceBuilder searchBuilder = new SearchSourceBuilder()
        .size(searchRequest.getSize())
        .from(searchRequest.getFrom())
        .query(queryBuilder)
        .trackScores(true);
    List<String> fields = searchRequest.getFields();
    // column metadata needed to understand the type of each sort field
    Map<String, FieldType> meta;
    try {
      meta = columnMetadataDao.getColumnMetadata(searchRequest.getIndices());
    } catch(IOException e) {
      throw new InvalidSearchException("Unable to get column metadata", e);
    }

    // handle sort fields
    for(SortField sortField : searchRequest.getSort()) {

      // what type is the sort field?
      FieldType sortFieldType = meta.getOrDefault(sortField.getField(), FieldType.OTHER);

      // sort order - if ascending missing values sorted last. otherwise, missing values sorted first
      org.elasticsearch.search.sort.SortOrder sortOrder = getElasticsearchSortOrder(sortField.getSortOrder());
      String missingSortOrder;
      if(sortOrder == org.elasticsearch.search.sort.SortOrder.DESC) {
        missingSortOrder = SORT_MISSING_LAST;
      } else {
        missingSortOrder = SORT_MISSING_FIRST;
      }

      // sort by the field - missing fields always last
      FieldSortBuilder sortBy = new FieldSortBuilder(sortField.getField())
          .order(sortOrder)
          .missing(missingSortOrder)
          .unmappedType(sortFieldType.getFieldType());
      searchBuilder.sort(sortBy);
    }

    // handle search fields
    if (fields != null) {
      searchBuilder.fetchSource("*", null);
    } else {
      searchBuilder.fetchSource(true);
    }

    List<String> facetFields = searchRequest.getFacetFields();

    // handle facet fields
    if (facetFields != null) {
      // https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/_bucket_aggregations.html
      for(String field : facetFields) {
        String name = getFacetAggregationName(field);
        TermsAggregationBuilder terms = AggregationBuilders.terms( name).field(field);
        // new TermsBuilder(name).field(field);
        searchBuilder.aggregation(terms);
      }
    }

    // return the search request
    String[] indices = wildcardIndices(searchRequest.getIndices());
    if (LOG.isDebugEnabled()) {
      LOG.debug("Built Elasticsearch request; indices={}, request={}", indices, searchBuilder.toString());
    }
    return new org.elasticsearch.action.search.SearchRequest()
        .indices(indices)
        .source(searchBuilder);
  }

  /**
   * Builds a search response.
   *
   * This effectively transforms an Elasticsearch search response into a Metron search response.
   *
   * @param searchRequest The Metron search request.
   * @param esResponse The Elasticsearch search response.
   * @return A Metron search response.
   * @throws InvalidSearchException
   */
  private SearchResponse buildSearchResponse(
      SearchRequest searchRequest,
      org.elasticsearch.action.search.SearchResponse esResponse) throws InvalidSearchException {

    SearchResponse searchResponse = new SearchResponse();

    searchResponse.setTotal(esResponse.getHits().getTotalHits());

    // search hits --> search results
    List<SearchResult> results = new ArrayList<>();
    for(SearchHit hit: esResponse.getHits().getHits()) {
      results.add(getSearchResult(hit, searchRequest.getFields()));
    }
    searchResponse.setResults(results);

    // handle facet fields
    if (searchRequest.getFacetFields() != null) {
      List<String> facetFields = searchRequest.getFacetFields();
      Map<String, FieldType> commonColumnMetadata;
      try {
        commonColumnMetadata = columnMetadataDao.getColumnMetadata(searchRequest.getIndices());
      } catch (IOException e) {
        throw new InvalidSearchException(String.format(
            "Could not get common column metadata for indices %s",
            Arrays.toString(searchRequest.getIndices().toArray())));
      }
      searchResponse.setFacetCounts(getFacetCounts(facetFields, esResponse.getAggregations(), commonColumnMetadata ));
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Built search response; response={}", ElasticsearchUtils.toJSON(searchResponse).orElse("???"));
    }
    return searchResponse;
  }

  private org.elasticsearch.search.sort.SortOrder getElasticsearchSortOrder(
      org.apache.metron.indexing.dao.search.SortOrder sortOrder) {
    return sortOrder == org.apache.metron.indexing.dao.search.SortOrder.DESC ?
        org.elasticsearch.search.sort.SortOrder.DESC : org.elasticsearch.search.sort.SortOrder.ASC;
  }

  private String getFacetAggregationName(String field) {
    return String.format("%s_count", field);
  }

  private String[] wildcardIndices(List<String> indices) {
    if(indices == null)
      return new String[] {};

    return indices
        .stream()
        .map(index -> String.format("%s%s*", index, INDEX_NAME_DELIMITER))
        .toArray(value -> new String[indices.size()]);
  }

  private SearchResult getSearchResult(SearchHit searchHit, List<String> fields) {
    SearchResult searchResult = new SearchResult();
    searchResult.setId(searchHit.getId());
    Map<String, Object> source;
    if (fields != null) {
      Map<String, Object> resultSourceAsMap = searchHit.getSourceAsMap();
      source = new HashMap<>();
      fields.forEach(field -> {
        source.put(field, resultSourceAsMap.get(field));
      });
    } else {
      source = searchHit.getSource();
    }
    searchResult.setSource(source);
    searchResult.setScore(searchHit.getScore());
    searchResult.setIndex(searchHit.getIndex());
    return searchResult;
  }

  private Map<String, Map<String, Long>> getFacetCounts(List<String> fields, Aggregations aggregations, Map<String, FieldType> commonColumnMetadata) {
    Map<String, Map<String, Long>> fieldCounts = new HashMap<>();
    for (String field: fields) {
      Map<String, Long> valueCounts = new HashMap<>();
      if(aggregations != null ) {
        Aggregation aggregation = aggregations.get(getFacetAggregationName(field));
        if (aggregation instanceof Terms) {
          Terms terms = (Terms) aggregation;
          terms.getBuckets().stream().forEach(bucket -> valueCounts.put(formatKey(bucket.getKey(), commonColumnMetadata.get(field)), bucket.getDocCount()));
        }
      }
      fieldCounts.put(field, valueCounts);
    }
    return fieldCounts;
  }

  private String formatKey(Object key, FieldType type) {
    if (FieldType.IP.equals(type) && key instanceof Long) {
      return LegacyIpFieldMapper.longToIp((Long) key);
    } else if (FieldType.BOOLEAN.equals(type)) {
      return (Long) key == 1 ? "true" : "false";
    } else {
      return key.toString();
    }
  }

  /**
   * Defers to a provided {@link org.elasticsearch.index.query.QueryBuilder} for the query.
   * @param groupRequest The request defining the parameters of the grouping
   * @param queryBuilder The actual query to be run. Intended for if the SearchRequest requires wrapping
   * @return The results of the query
   * @throws InvalidSearchException When the query is malformed or the current state doesn't allow search
   */
  protected GroupResponse group(GroupRequest groupRequest, QueryBuilder queryBuilder)
      throws InvalidSearchException {
    org.elasticsearch.action.search.SearchRequest esRequest;
    org.elasticsearch.action.search.SearchResponse esResponse;

    if (client == null) {
      throw new InvalidSearchException("Uninitialized Dao!  You must call init() prior to use.");
    }
    if (groupRequest.getGroups() == null || groupRequest.getGroups().size() == 0) {
      throw new InvalidSearchException("At least 1 group must be provided.");
    }

    esRequest = buildGroupRequest(groupRequest, queryBuilder);
    esResponse = requestSubmitter.submitSearch(esRequest);
    GroupResponse response = buildGroupResponse(groupRequest, esResponse);

    return response;
  }

  /**
   * Builds a group search request.
   * @param groupRequest The Metron group request.
   * @param queryBuilder The search query.
   * @return An Elasticsearch search request.
   */
  private org.elasticsearch.action.search.SearchRequest buildGroupRequest(
      GroupRequest groupRequest,
      QueryBuilder queryBuilder) {

    // handle groups
    TermsAggregationBuilder groups = getGroupsTermBuilder(groupRequest, 0);
    final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
        .query(queryBuilder)
        .aggregation(groups);

    // return the search request
    String[] indices = wildcardIndices(groupRequest.getIndices());
    return new org.elasticsearch.action.search.SearchRequest()
        .indices(indices)
        .source(searchSourceBuilder);
  }

  private TermsAggregationBuilder getGroupsTermBuilder(GroupRequest groupRequest, int index) {
    List<Group> groups = groupRequest.getGroups();
    Group group = groups.get(index);
    String aggregationName = getGroupByAggregationName(group.getField());
    TermsAggregationBuilder termsBuilder = AggregationBuilders.terms(aggregationName);
    termsBuilder
        .field(group.getField())
        .size(accessConfig.getMaxSearchGroups())
        .order(getElasticsearchGroupOrder(group.getOrder()));
    if (index < groups.size() - 1) {
      termsBuilder.subAggregation(getGroupsTermBuilder(groupRequest, index + 1));
    }
    Optional<String> scoreField = groupRequest.getScoreField();
    if (scoreField.isPresent()) {
      SumAggregationBuilder scoreSumAggregationBuilder = AggregationBuilders.sum(getSumAggregationName(scoreField.get())).field(scoreField.get()).missing(0);
      termsBuilder.subAggregation(scoreSumAggregationBuilder);
    }
    return termsBuilder;
  }

  private String getGroupByAggregationName(String field) {
    return String.format("%s_group", field);
  }

  private String getSumAggregationName(String field) {
    return String.format("%s_score", field);
  }

  private Order getElasticsearchGroupOrder(GroupOrder groupOrder) {
    if (groupOrder.getGroupOrderType() == GroupOrderType.TERM) {
      return groupOrder.getSortOrder() == SortOrder.ASC ? Order.term(true) : Order.term(false);
    } else {
      return groupOrder.getSortOrder() == SortOrder.ASC ? Order.count(true) : Order.count(false);
    }
  }

  /**
   * Build a group response.
   * @param groupRequest The original group request.
   * @param response The search response.
   * @return A group response.
   * @throws InvalidSearchException
   */
  private GroupResponse buildGroupResponse(
      GroupRequest groupRequest,
      org.elasticsearch.action.search.SearchResponse response) throws InvalidSearchException {

    // build the search response
    Map<String, FieldType> commonColumnMetadata;
    try {
      commonColumnMetadata = columnMetadataDao.getColumnMetadata(groupRequest.getIndices());
    } catch (IOException e) {
      throw new InvalidSearchException(String.format("Could not get common column metadata for indices %s",
          Arrays.toString(groupRequest.getIndices().toArray())));
    }

    GroupResponse groupResponse = new GroupResponse();
    groupResponse.setGroupedBy(groupRequest.getGroups().get(0).getField());
    groupResponse.setGroupResults(getGroupResults(groupRequest, 0, response.getAggregations(), commonColumnMetadata));
    return groupResponse;
  }

  private List<GroupResult> getGroupResults(GroupRequest groupRequest, int index, Aggregations aggregations, Map<String, FieldType> commonColumnMetadata) {
    List<Group> groups = groupRequest.getGroups();
    String field = groups.get(index).getField();
    Terms terms = aggregations.get(getGroupByAggregationName(field));
    List<GroupResult> searchResultGroups = new ArrayList<>();
    for(Bucket bucket: terms.getBuckets()) {
      GroupResult groupResult = new GroupResult();
      groupResult.setKey(formatKey(bucket.getKey(), commonColumnMetadata.get(field)));
      groupResult.setTotal(bucket.getDocCount());
      Optional<String> scoreField = groupRequest.getScoreField();
      if (scoreField.isPresent()) {
        Sum score = bucket.getAggregations().get(getSumAggregationName(scoreField.get()));
        groupResult.setScore(score.getValue());
      }
      if (index < groups.size() - 1) {
        groupResult.setGroupedBy(groups.get(index + 1).getField());
        groupResult.setGroupResults(getGroupResults(groupRequest, index + 1, bucket.getAggregations(), commonColumnMetadata));
      }
      searchResultGroups.add(groupResult);
    }
    return searchResultGroups;
  }
}
