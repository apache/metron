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

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.dao.AccessConfig;
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
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.PivotField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrSearchDao implements SearchDao {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private transient SolrClient client;
  private AccessConfig accessConfig;

  public SolrSearchDao(SolrClient client, AccessConfig accessConfig) {
    this.client = client;
    this.accessConfig = accessConfig;
  }

  protected AccessConfig getAccessConfig() {
    return accessConfig;
  }

  @Override
  public SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException {
    return search(searchRequest, null);
  }

  // Allow for the fieldList to be explicitly specified, letting things like metaalerts expand on them.
  // If null, use whatever the searchRequest defines.
  public SearchResponse search(SearchRequest searchRequest, String fieldList)
      throws InvalidSearchException {
    if (searchRequest.getQuery() == null) {
      throw new InvalidSearchException("Search query is invalid: null");
    }
    if (client == null) {
      throw new InvalidSearchException("Uninitialized Dao!  You must call init() prior to use.");
    }
    if (searchRequest.getSize() > accessConfig.getMaxSearchResults()) {
      throw new InvalidSearchException(
          "Search result size must be less than " + accessConfig.getMaxSearchResults());
    }
    try {
      SolrQuery query = buildSearchRequest(searchRequest, fieldList);
      QueryResponse response = client.query(query);
      return buildSearchResponse(searchRequest, response);
    } catch (SolrException | IOException | SolrServerException e) {
      String msg = e.getMessage();
      LOG.error(msg, e);
      throw new InvalidSearchException(msg, e);
    }
  }

  @Override
  public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
    try {
      if (groupRequest.getGroups() == null || groupRequest.getGroups().size() == 0) {
        throw new InvalidSearchException("At least 1 group must be provided.");
      }
      String groupNames = groupRequest.getGroups().stream().map(Group::getField).collect(
          Collectors.joining(","));
      SolrQuery query = new SolrQuery()
          .setStart(0)
          .setRows(0)
          .setQuery(groupRequest.getQuery());

      query.set("collection", getCollections(groupRequest.getIndices()));
      Optional<String> scoreField = groupRequest.getScoreField();
      if (scoreField.isPresent()) {
        query.set("stats", true);
        query.set("stats.field", String.format("{!tag=piv1 sum=true}%s", scoreField.get()));
      }
      query.set("facet", true);
      query.set("facet.pivot", String.format("{!stats=piv1}%s", groupNames));
      QueryResponse response = client.query(query);
      return buildGroupResponse(groupRequest, response);
    } catch (IOException | SolrServerException e) {
      String msg = e.getMessage();
      LOG.error(msg, e);
      throw new InvalidSearchException(msg, e);
    }
  }

  // An explicit, overriding fieldList can be provided.  This is useful for things like metaalerts,
  // which may need to modify that parameter.
  protected SolrQuery buildSearchRequest(
      SearchRequest searchRequest, String fieldList) throws IOException, SolrServerException {
    SolrQuery query = new SolrQuery()
        .setStart(searchRequest.getFrom())
        .setRows(searchRequest.getSize())
        .setQuery(searchRequest.getQuery());

    // handle sort fields
    for (SortField sortField : searchRequest.getSort()) {
      query.addSort(sortField.getField(), getSolrSortOrder(sortField.getSortOrder()));
    }

    // handle search fields
    List<String> fields = searchRequest.getFields();
    if (fieldList == null) {
      fieldList = "*";
      if (fields != null) {
        fieldList = StringUtils.join(fields, ",");
      }
    }
    query.set("fl", fieldList);

    //handle facet fields
    List<String> facetFields = searchRequest.getFacetFields();
    if (facetFields != null) {
      facetFields.forEach(query::addFacetField);
    }

    query.set("collection", getCollections(searchRequest.getIndices()));

    return query;
  }

  private String getCollections(List<String> indices) throws IOException, SolrServerException {
    List<String> existingCollections = CollectionAdminRequest.listCollections(client);
    return indices.stream().filter(existingCollections::contains).collect(Collectors.joining(","));
  }

  private SolrQuery.ORDER getSolrSortOrder(
      SortOrder sortOrder) {
    return sortOrder == SortOrder.DESC
        ? ORDER.desc : ORDER.asc;
  }

  protected SearchResponse buildSearchResponse(
      SearchRequest searchRequest,
      QueryResponse solrResponse) {

    SearchResponse searchResponse = new SearchResponse();
    SolrDocumentList solrDocumentList = solrResponse.getResults();
    searchResponse.setTotal(solrDocumentList.getNumFound());

    // search hits --> search results
    List<SearchResult> results = solrDocumentList.stream()
        .map(solrDocument -> SolrUtilities.getSearchResult(solrDocument, searchRequest.getFields(),
                accessConfig.getIndexSupplier()))
        .collect(Collectors.toList());
    searchResponse.setResults(results);

    // handle facet fields
    List<String> facetFields = searchRequest.getFacetFields();
    if (facetFields != null) {
      searchResponse.setFacetCounts(getFacetCounts(facetFields, solrResponse));
    }

    if (LOG.isDebugEnabled()) {
      String response;
      try {
        response = JSONUtils.INSTANCE.toJSON(searchResponse, false);
      } catch (JsonProcessingException e) {
        response = e.getMessage();
      }
      LOG.debug("Built search response; response={}", response);
    }
    return searchResponse;
  }

  protected Map<String, Map<String, Long>> getFacetCounts(List<String> fields,
      QueryResponse solrResponse) {
    Map<String, Map<String, Long>> fieldCounts = new HashMap<>();
    for (String field : fields) {
      Map<String, Long> valueCounts = new HashMap<>();
      FacetField facetField = solrResponse.getFacetField(field);
      for (Count facetCount : facetField.getValues()) {
        valueCounts.put(facetCount.getName(), facetCount.getCount());
      }
      fieldCounts.put(field, valueCounts);
    }
    return fieldCounts;
  }

  /**
   * Build a group response.
   * @param groupRequest The original group request.
   * @param response The search response.
   * @return A group response.
   */
  protected GroupResponse buildGroupResponse(
      GroupRequest groupRequest,
      QueryResponse response) {
    String groupNames = groupRequest.getGroups().stream().map(Group::getField).collect(
        Collectors.joining(","));
    List<PivotField> pivotFields = response.getFacetPivot().get(groupNames);
    GroupResponse groupResponse = new GroupResponse();
    groupResponse.setGroupedBy(groupRequest.getGroups().get(0).getField());
    groupResponse.setGroupResults(getGroupResults(groupRequest, 0, pivotFields));
    return groupResponse;
  }

  protected List<GroupResult> getGroupResults(GroupRequest groupRequest, int index,
      List<PivotField> pivotFields) {
    List<Group> groups = groupRequest.getGroups();
    List<GroupResult> searchResultGroups = new ArrayList<>();
    final GroupOrder groupOrder = groups.get(index).getOrder();
    pivotFields.sort((o1, o2) -> {
      String s1 = groupOrder.getGroupOrderType() == GroupOrderType.TERM
          ? o1.getValue().toString() : Integer.toString(o1.getCount());
      String s2 = groupOrder.getGroupOrderType() == GroupOrderType.TERM
          ? o2.getValue().toString() : Integer.toString(o2.getCount());
      if (groupOrder.getSortOrder() == SortOrder.ASC) {
        return s1.compareTo(s2);
      } else {
        return s2.compareTo(s1);
      }
    });

    for (PivotField pivotField : pivotFields) {
      GroupResult groupResult = new GroupResult();
      groupResult.setKey(pivotField.getValue().toString());
      groupResult.setTotal(pivotField.getCount());
      Optional<String> scoreField = groupRequest.getScoreField();
      if (scoreField.isPresent()) {
        groupResult
            .setScore((Double) pivotField.getFieldStatsInfo().get(scoreField.get()).getSum());
      }
      if (index < groups.size() - 1) {
        groupResult.setGroupedBy(groups.get(index + 1).getField());
        groupResult
            .setGroupResults(getGroupResults(groupRequest, index + 1, pivotField.getPivot()));
      }
      searchResultGroups.add(groupResult);
    }
    return searchResultGroups;
  }
}
