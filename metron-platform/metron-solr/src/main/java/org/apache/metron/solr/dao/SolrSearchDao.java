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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.dao.AccessConfig;
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
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.PivotField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
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

  @Override
  public SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException {
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
    SolrQuery query = buildSearchRequest(searchRequest);
    try {
      QueryResponse response = client.query(query);
      return buildSearchResponse(searchRequest, response);
    } catch (IOException | SolrServerException e) {
      String msg = e.getMessage();
      LOG.error(msg, e);
      throw new InvalidSearchException(msg, e);
    }
  }

  @Override
  public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
    String groupNames = groupRequest.getGroups().stream().map(Group::getField).collect(
        Collectors.joining(","));
    SolrQuery query = new SolrQuery()
        .setStart(0)
        .setRows(0)
        .setQuery(groupRequest.getQuery());
    query.set("collection", "bro,snort");
    Optional<String> scoreField = groupRequest.getScoreField();
    if (scoreField.isPresent()) {
      query.set("stats", true);
      query.set("stats.field", String.format("{!tag=piv1 sum=true}%s", scoreField.get()));
    }
    query.set("facet", true);
    query.set("facet.pivot", String.format("{!stats=piv1}%s", groupNames));
    try {
      QueryResponse response = client.query(query);
      return buildGroupResponse(groupRequest, response);
    } catch (IOException | SolrServerException e) {
      String msg = e.getMessage();
      LOG.error(msg, e);
      throw new InvalidSearchException(msg, e);
    }
  }

  @Override
  public Document getLatest(String guid, String collection) throws IOException {
    try {
      SolrDocument solrDocument = client.getById(collection, guid);
      return toDocument(solrDocument);
    } catch (SolrServerException e) {
      throw new IOException(e);
    }
  }

  @Override
  public Iterable<Document> getAllLatest(List<GetRequest> getRequests) throws IOException {
    Map<String, Collection<String>> collectionIdMap = new HashMap<>();
    for (GetRequest getRequest: getRequests) {
      Collection<String> ids = collectionIdMap.getOrDefault(getRequest.getSensorType(), new HashSet<>());
      ids.add(getRequest.getGuid());
      collectionIdMap.put(getRequest.getSensorType(), ids);
    }
    try {
      List<Document> documents = new ArrayList<>();
      for (String collection: collectionIdMap.keySet()) {
        SolrDocumentList solrDocumentList = client.getById(collectionIdMap.get(collection),
            new SolrQuery().set("collection", collection));
        documents.addAll(solrDocumentList.stream().map(this::toDocument).collect(Collectors.toList()));
      }
      return documents;
    } catch (SolrServerException e) {
      throw new IOException(e);
    }
  }

  private SolrQuery buildSearchRequest(
      SearchRequest searchRequest) {
    SolrQuery query = new SolrQuery()
        .setStart(searchRequest.getFrom())
        .setRows(searchRequest.getSize())
        .setQuery(searchRequest.getQuery());

    // handle sort fields
    for (SortField sortField : searchRequest.getSort()) {
      query.addSort(sortField.getField(), getSolrSortOrder(sortField.getSortOrder()));
    }

    // handle search fields
    Optional<List<String>> fields = searchRequest.getFields();
    if (fields.isPresent()) {
      fields.get().forEach(query::addField);
    }

    //handle facet fields
    Optional<List<String>> facetFields = searchRequest.getFacetFields();
    if (facetFields.isPresent()) {
      facetFields.get().forEach(query::addFacetField);
    }

    String collections = searchRequest.getIndices().stream().collect(Collectors.joining(","));
    query.set("collection", collections);

    return query;
  }

  private SolrQuery.ORDER getSolrSortOrder(
      SortOrder sortOrder) {
    return sortOrder == SortOrder.DESC ?
        ORDER.desc : ORDER.asc;
  }

  private SearchResponse buildSearchResponse(
      SearchRequest searchRequest,
      QueryResponse solrResponse) {

    SearchResponse searchResponse = new SearchResponse();
    SolrDocumentList solrDocumentList = solrResponse.getResults();
    searchResponse.setTotal(solrDocumentList.getNumFound());

    // search hits --> search results
    List<SearchResult> results = solrDocumentList.stream()
        .map(solrDocument -> getSearchResult(solrDocument, searchRequest.getFields()))
        .collect(Collectors.toList());
    searchResponse.setResults(results);

    // handle facet fields
    Optional<List<String>> facetFields = searchRequest.getFacetFields();
    if (facetFields.isPresent()) {
      searchResponse.setFacetCounts(getFacetCounts(facetFields.get(), solrResponse));
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

  private SearchResult getSearchResult(SolrDocument solrDocument, Optional<List<String>> fields) {
    SearchResult searchResult = new SearchResult();
    searchResult.setId((String) solrDocument.getFieldValue(Constants.GUID));
    final Map<String, Object> source = new HashMap<>();
    if (fields.isPresent()) {
      fields.get().forEach(field -> source.put(field, solrDocument.getFieldValue(field)));
    } else {
      solrDocument.getFieldNames().forEach(field -> source.put(field, solrDocument.getFieldValue(field)));
    }
    searchResult.setSource(source);
    return searchResult;
  }

  private Map<String, Map<String, Long>> getFacetCounts(List<String> fields,
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
  private GroupResponse buildGroupResponse(
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

  private List<GroupResult> getGroupResults(GroupRequest groupRequest, int index, List<PivotField> pivotFields) {
    List<Group> groups = groupRequest.getGroups();
    List<GroupResult> searchResultGroups = new ArrayList<>();
    final GroupOrder groupOrder = groups.get(index).getOrder();
    pivotFields.sort((o1, o2) -> {
      String s1 = groupOrder.getGroupOrderType() == GroupOrderType.TERM ?
          o1.getValue().toString() : Integer.toString(o1.getCount());
      String s2 = groupOrder.getGroupOrderType() == GroupOrderType.TERM ?
          o2.getValue().toString() : Integer.toString(o2.getCount());
      if (groupOrder.getSortOrder() == SortOrder.ASC) {
        return s1.compareTo(s2);
      } else {
        return s2.compareTo(s1);
      }
    });

    for(PivotField pivotField: pivotFields) {
      GroupResult groupResult = new GroupResult();
      groupResult.setKey(pivotField.getValue().toString());
      groupResult.setTotal(pivotField.getCount());
      Optional<String> scoreField = groupRequest.getScoreField();
      if (scoreField.isPresent()) {
        groupResult.setScore((Double) pivotField.getFieldStatsInfo().get("score").getSum());
      }
      if (index < groups.size() - 1) {
        groupResult.setGroupedBy(groups.get(index + 1).getField());
        groupResult.setGroupResults(getGroupResults(groupRequest, index + 1, pivotField.getPivot()));
      }
      searchResultGroups.add(groupResult);
    }
    return searchResultGroups;
  }

  private Document toDocument(SolrDocument solrDocument) {
    Map<String, Object> document = new HashMap<>();
    solrDocument.getFieldNames().stream()
        .filter(name -> !name.equals(SolrDao.VERSION_FIELD))
        .forEach(name -> document.put(name, solrDocument.getFieldValue(name)));
    return new Document(document,
        (String) solrDocument.getFieldValue(Constants.GUID),
        (String) solrDocument.getFieldValue("source:type"), 0L);
  }
}
