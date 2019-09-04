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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.Group;
import org.apache.metron.indexing.dao.search.GroupOrder;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.GroupResult;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.metron.indexing.dao.search.SortField;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.solr.matcher.ModifiableSolrParamsMatcher;
import org.apache.metron.solr.matcher.SolrQueryMatcher;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.PivotField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CollectionAdminRequest.class})
public class SolrSearchDaoTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  private SolrClient client;
  private AccessConfig accessConfig;
  private SolrSearchDao solrSearchDao;
  private SolrRetrieveLatestDao solrRetrieveLatestDao;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    client = mock(SolrClient.class);
    accessConfig = mock(AccessConfig.class);
    when(accessConfig.getIndexSupplier()).thenReturn(sensorType -> sensorType);
    solrSearchDao = new SolrSearchDao(client, accessConfig);
    solrRetrieveLatestDao = new SolrRetrieveLatestDao(client, accessConfig);
    mockStatic(CollectionAdminRequest.class);
    when(CollectionAdminRequest.listCollections(client)).thenReturn(Arrays.asList("bro", "snort"));
  }

  @Test
  public void searchShouldProperlyReturnSearchResponse() throws Exception {
    SearchRequest searchRequest = mock(SearchRequest.class);
    SearchResponse searchResponse = mock(SearchResponse.class);
    SolrQuery solrQuery = mock(SolrQuery.class);
    QueryResponse queryResponse = mock(QueryResponse.class);

    solrSearchDao = spy(new SolrSearchDao(client, accessConfig));
    when(searchRequest.getQuery()).thenReturn("query");
    doReturn(solrQuery).when(solrSearchDao).buildSearchRequest(searchRequest, "*");
    when(client.query(solrQuery)).thenReturn(queryResponse);
    doReturn(searchResponse).when(solrSearchDao).buildSearchResponse(searchRequest, queryResponse);

    assertEquals(searchResponse, solrSearchDao.search(searchRequest, "*"));
    verify(solrSearchDao).buildSearchRequest(searchRequest, "*");
    verify(client).query(solrQuery);
    verify(solrSearchDao).buildSearchResponse(searchRequest, queryResponse);
    verifyNoMoreInteractions(client);
  }

  @Test
  public void searchShouldThrowInvalidSearchExceptionOnEmptyQuery() throws Exception {
    exception.expect(InvalidSearchException.class);
    exception.expectMessage("Search query is invalid: null");

    solrSearchDao.search(new SearchRequest());
  }

  @Test
  public void searchShouldThrowInvalidSearchExceptionOnEmptyClient() throws Exception {
    exception.expect(InvalidSearchException.class);
    exception.expectMessage("Uninitialized Dao!  You must call init() prior to use.");

    SearchRequest searchRequest = new SearchRequest();
    searchRequest.setQuery("query");
    new SolrSearchDao(null, accessConfig).search(searchRequest);
  }

  @Test
  public void searchShouldThrowInvalidSearchExceptionOnNullGroup() throws Exception {
    exception.expect(InvalidSearchException.class);
    exception.expectMessage("At least 1 group must be provided.");

    GroupRequest groupRequest = mock(GroupRequest.class);
    GroupResponse groupResponse = mock(GroupResponse.class);

    solrSearchDao = spy(new SolrSearchDao(client, accessConfig));
    when(groupRequest.getQuery()).thenReturn("query");
    when(groupRequest.getGroups()).thenReturn(null);
    when(groupRequest.getScoreField()).thenReturn(Optional.of("scoreField"));
    when(groupRequest.getIndices()).thenReturn(Arrays.asList("bro", "snort"));

    assertEquals(groupResponse, solrSearchDao.group(groupRequest));
    verifyNoMoreInteractions(client);
  }

  @Test
  public void searchShouldThrowInvalidSearchExceptionOnEmptyGroup() throws Exception {
    exception.expect(InvalidSearchException.class);
    exception.expectMessage("At least 1 group must be provided.");

    GroupRequest groupRequest = mock(GroupRequest.class);
    GroupResponse groupResponse = mock(GroupResponse.class);

    solrSearchDao = spy(new SolrSearchDao(client, accessConfig));
    when(groupRequest.getQuery()).thenReturn("query");
    when(groupRequest.getGroups()).thenReturn(Collections.EMPTY_LIST);
    when(groupRequest.getScoreField()).thenReturn(Optional.of("scoreField"));
    when(groupRequest.getIndices()).thenReturn(Arrays.asList("bro", "snort"));

    assertEquals(groupResponse, solrSearchDao.group(groupRequest));
    verifyNoMoreInteractions(client);
  }

  @Test
  public void searchShouldThrowSearchResultSizeException() throws Exception {
    exception.expect(InvalidSearchException.class);
    exception.expectMessage("Search result size must be less than 100");

    when(accessConfig.getMaxSearchResults()).thenReturn(100);
    SearchRequest searchRequest = new SearchRequest();
    searchRequest.setQuery("query");
    searchRequest.setSize(200);
    solrSearchDao.search(searchRequest);
  }

  @Test
  public void groupShouldProperlyReturnGroupResponse() throws Exception {
    GroupRequest groupRequest = mock(GroupRequest.class);
    QueryResponse queryResponse = mock(QueryResponse.class);
    GroupResponse groupResponse = mock(GroupResponse.class);

    solrSearchDao = spy(new SolrSearchDao(client, accessConfig));
    Group group1 = new Group();
    group1.setField("field1");
    Group group2 = new Group();
    group2.setField("field2");
    when(groupRequest.getQuery()).thenReturn("query");
    when(groupRequest.getGroups()).thenReturn(Arrays.asList(group1, group2));
    when(groupRequest.getScoreField()).thenReturn(Optional.of("scoreField"));
    when(groupRequest.getIndices()).thenReturn(Arrays.asList("bro", "snort"));
    when(client.query(any())).thenReturn(queryResponse);
    doReturn(groupResponse).when(solrSearchDao).buildGroupResponse(groupRequest, queryResponse);
    SolrQuery expectedSolrQuery = new SolrQuery()
        .setStart(0)
        .setRows(0)
        .setQuery("query");
    expectedSolrQuery.set("collection", "bro,snort");
    expectedSolrQuery.set("stats", true);
    expectedSolrQuery.set("stats.field", "{!tag=piv1 sum=true}scoreField");
    expectedSolrQuery.set("facet", true);
    expectedSolrQuery.set("facet.pivot", "{!stats=piv1}field1,field2");

    assertEquals(groupResponse, solrSearchDao.group(groupRequest));
    verify(client).query(argThat(new SolrQueryMatcher(expectedSolrQuery)));
    verify(solrSearchDao).buildGroupResponse(groupRequest, queryResponse);

    verifyNoMoreInteractions(client);
  }

  @Test
  public void getLatestShouldProperlyReturnDocument() throws Exception {
    SolrDocument solrDocument = mock(SolrDocument.class);

    solrSearchDao = spy(new SolrSearchDao(client, accessConfig));
    when(client.getById("collection", "guid")).thenReturn(solrDocument);
    Document document = SolrUtilities.toDocument(solrDocument);

    assertEquals(document, solrRetrieveLatestDao.getLatest("guid", "collection"));

    verify(client).getById("collection", "guid");
    verifyNoMoreInteractions(client);
  }

  @Test
  public void getAllLatestShouldProperlyReturnDocuments() throws Exception {
    GetRequest broRequest1 = new GetRequest("bro-1", "bro");
    GetRequest broRequest2 = new GetRequest("bro-2", "bro");
    GetRequest snortRequest1 = new GetRequest("snort-1", "snort");
    GetRequest snortRequest2 = new GetRequest("snort-2", "snort");
    SolrDocument broSolrDoc1 = mock(SolrDocument.class);
    SolrDocument broSolrDoc2 = mock(SolrDocument.class);
    SolrDocument snortSolrDoc1 = mock(SolrDocument.class);
    SolrDocument snortSolrDoc2 = mock(SolrDocument.class);
    Document broDoc1 = SolrUtilities.toDocument(broSolrDoc1);
    Document broDoc2 = SolrUtilities.toDocument(broSolrDoc2);
    Document snortDoc1 = SolrUtilities.toDocument(snortSolrDoc1);
    Document snortDoc2 = SolrUtilities.toDocument(snortSolrDoc2);

    solrSearchDao = spy(new SolrSearchDao(client, accessConfig));
    SolrDocumentList broList = new SolrDocumentList();
    broList.add(broSolrDoc1);
    broList.add(broSolrDoc2);
    SolrDocumentList snortList = new SolrDocumentList();
    snortList.add(snortSolrDoc1);
    snortList.add(snortSolrDoc2);
    when(client.getById((Collection<String>) argThat(hasItems("bro-1", "bro-2")),
        argThat(
            new ModifiableSolrParamsMatcher(new ModifiableSolrParams().set("collection", "bro")))))
        .thenReturn(broList);
    when(client.getById((Collection<String>) argThat(hasItems("snort-1", "snort-2")),
        argThat(new ModifiableSolrParamsMatcher(
            new ModifiableSolrParams().set("collection", "snort"))))).thenReturn(snortList);
    assertEquals(Arrays.asList(broDoc1, broDoc2, snortDoc1, snortDoc2), solrRetrieveLatestDao
        .getAllLatest(Arrays.asList(broRequest1, broRequest2, snortRequest1, snortRequest2)));
  }

  @Test
  public void buildSearchRequestShouldReturnSolrQuery() throws Exception {
    SearchRequest searchRequest = new SearchRequest();
    searchRequest.setIndices(Arrays.asList("bro", "snort"));
    searchRequest.setSize(5);
    searchRequest.setFrom(10);
    searchRequest.setQuery("query");
    SortField sortField = new SortField();
    sortField.setField("sortField");
    sortField.setSortOrder("ASC");
    searchRequest.setSort(Collections.singletonList(sortField));
    searchRequest.setFields(Arrays.asList("field1", "field2"));
    searchRequest.setFacetFields(Arrays.asList("facetField1", "facetField2"));

    SolrQuery exceptedSolrQuery = new SolrQuery()
        .setStart(10)
        .setRows(5)
        .setQuery("query")
        .addSort("sortField", SolrQuery.ORDER.asc)
        .addField("field1").addField("field2")
        .addFacetField("facetField1", "facetField2");
    exceptedSolrQuery.set("collection", "bro,snort");

    SolrQuery solrQuery = solrSearchDao.buildSearchRequest(searchRequest, "field1,field2");
    assertThat(solrQuery, new SolrQueryMatcher(exceptedSolrQuery));
  }

  @Test
  public void buildSearchResponseShouldReturnSearchResponse() {
    SearchRequest searchRequest = new SearchRequest();
    searchRequest.setFields(Collections.singletonList("id"));
    searchRequest.setFacetFields(Collections.singletonList("facetField"));
    QueryResponse queryResponse = mock(QueryResponse.class);
    SolrDocument solrDocument1 = new SolrDocument();
    solrDocument1.setField(Constants.GUID, "id1");
    solrDocument1.setField("id", "id1");
    SolrDocument solrDocument2 = new SolrDocument();
    solrDocument2.setField(Constants.GUID, "id2");
    solrDocument2.setField("id", "id2");

    solrSearchDao = spy(new SolrSearchDao(client, accessConfig));
    SolrDocumentList solrDocumentList = new SolrDocumentList();
    solrDocumentList.add(solrDocument1);
    solrDocumentList.add(solrDocument2);
    solrDocumentList.setNumFound(100);
    when(queryResponse.getResults()).thenReturn(solrDocumentList);
    SearchResult searchResult1 = new SearchResult();
    searchResult1.setId("id1");
    HashMap<String, Object> source1 = new HashMap<>();
    source1.put("id", "id1");
    searchResult1.setSource(source1);
    SearchResult searchResult2 = new SearchResult();
    searchResult2.setId("id2");
    HashMap<String, Object> source2 = new HashMap<>();
    source2.put("id", "id2");
    searchResult2.setSource(source2);
    Map<String, Map<String, Long>> facetCounts = new HashMap<String, Map<String, Long>>() {{
      put("id", new HashMap<String, Long>() {{
        put("id1", 1L);
        put("id2", 1L);
      }});
    }};
    doReturn(facetCounts).when(solrSearchDao)
        .getFacetCounts(Collections.singletonList("facetField"), queryResponse);
    SearchResponse expectedSearchResponse = new SearchResponse();
    SearchResult expectedSearchResult1 = new SearchResult();
    expectedSearchResult1.setId("id1");
    expectedSearchResult1.setSource(source1);
    SearchResult expectedSearchResult2 = new SearchResult();
    expectedSearchResult2.setId("id2");
    expectedSearchResult2.setSource(source2);

    expectedSearchResponse.setResults(Arrays.asList(expectedSearchResult1, expectedSearchResult2));
    expectedSearchResponse.setTotal(100);
    expectedSearchResponse.setFacetCounts(facetCounts);

    assertEquals(expectedSearchResponse,
        solrSearchDao.buildSearchResponse(searchRequest, queryResponse));
  }

  @Test
  public void getSearchResultShouldProperlyReturnResults() {
    SolrDocument solrDocument = mock(SolrDocument.class);

    when(solrDocument.getFieldValue(Constants.GUID)).thenReturn("guid");
    when(solrDocument.getFieldValue(Constants.SENSOR_TYPE)).thenReturn("sensorType");
    when(solrDocument.getFieldValue("field1")).thenReturn("value1");
    when(solrDocument.getFieldValue("field2")).thenReturn("value2");
    when(solrDocument.getFieldNames()).thenReturn(Arrays.asList("field1", "field2"));

    SearchResult expectedSearchResult = new SearchResult();
    expectedSearchResult.setId("guid");
    expectedSearchResult.setIndex("sensorType");
    expectedSearchResult.setSource(new HashMap<String, Object>() {{
      put("field1", "value1");
    }});

    assertEquals(expectedSearchResult, SolrUtilities.getSearchResult(solrDocument,
        Collections.singletonList("field1"), solrSearchDao.getAccessConfig().getIndexSupplier()));

    SearchResult expectedSearchResultAllFields = new SearchResult();
    expectedSearchResultAllFields.setId("guid");
    expectedSearchResultAllFields.setIndex("sensorType");
    expectedSearchResultAllFields.setSource(new HashMap<String, Object>() {{
      put("field1", "value1");
      put("field2", "value2");
    }});

    assertEquals(expectedSearchResultAllFields,
        SolrUtilities.getSearchResult(solrDocument, null, solrSearchDao.getAccessConfig().getIndexSupplier()));
  }

  @Test
  public void getFacetCountsShouldProperlyReturnFacetCounts() {
    QueryResponse queryResponse = mock(QueryResponse.class);

    FacetField facetField1 = new FacetField("field1");
    facetField1.add("value1", 1);
    facetField1.add("value2", 2);
    FacetField facetField2 = new FacetField("field2");
    facetField2.add("value3", 3);
    facetField2.add("value4", 4);
    when(queryResponse.getFacetField("field1")).thenReturn(facetField1);
    when(queryResponse.getFacetField("field2")).thenReturn(facetField2);

    Map<String, Map<String, Long>> expectedFacetCounts = new HashMap<String, Map<String, Long>>() {{
      put("field1", new HashMap<String, Long>() {{
        put("value1", 1L);
        put("value2", 2L);
      }});
      put("field2", new HashMap<String, Long>() {{
        put("value3", 3L);
        put("value4", 4L);
      }});
    }};

    assertEquals(expectedFacetCounts,
        solrSearchDao.getFacetCounts(Arrays.asList("field1", "field2"), queryResponse));
  }

  @Test
  public void buildGroupResponseShouldProperlyReturnGroupReponse() {
    GroupRequest groupRequest = mock(GroupRequest.class);
    QueryResponse queryResponse = mock(QueryResponse.class);
    NamedList namedList = mock(NamedList.class);
    List pivotFields = mock(List.class);
    List groupResults = mock(List.class);

    solrSearchDao = spy(new SolrSearchDao(client, accessConfig));
    Group group1 = new Group();
    group1.setField("field1");
    Group group2 = new Group();
    group2.setField("field2");
    when(groupRequest.getGroups()).thenReturn(Arrays.asList(group1, group2));
    when(queryResponse.getFacetPivot()).thenReturn(namedList);
    when(namedList.get("field1,field2")).thenReturn(pivotFields);
    doReturn(groupResults).when(solrSearchDao).getGroupResults(groupRequest, 0, pivotFields);

    GroupResponse groupResponse = solrSearchDao.buildGroupResponse(groupRequest, queryResponse);
    assertEquals("field1", groupResponse.getGroupedBy());
    verify(namedList).get("field1,field2");
    verify(solrSearchDao).getGroupResults(groupRequest, 0, pivotFields);

  }

  @Test
  public void getGroupResultsShouldProperlyReturnGroupResults() {
    GroupRequest groupRequest = new GroupRequest();
    Group group1 = new Group();
    group1.setField("field1");
    GroupOrder groupOrder1 = new GroupOrder();
    groupOrder1.setSortOrder("ASC");
    groupOrder1.setGroupOrderType("TERM");
    group1.setOrder(groupOrder1);
    Group group2 = new Group();
    group2.setField("field2");
    GroupOrder groupOrder2 = new GroupOrder();
    groupOrder2.setSortOrder("DESC");
    groupOrder2.setGroupOrderType("COUNT");
    group2.setOrder(groupOrder2);
    groupRequest.setGroups(Arrays.asList(group1, group2));
    groupRequest.setScoreField("score");

    PivotField level1Pivot1 = mock(PivotField.class);
    PivotField level1Pivot2 = mock(PivotField.class);
    PivotField level2Pivot1 = mock(PivotField.class);
    PivotField level2Pivot2 = mock(PivotField.class);
    FieldStatsInfo level1Pivot1FieldStatsInfo = mock(FieldStatsInfo.class);
    FieldStatsInfo level1Pivot2FieldStatsInfo = mock(FieldStatsInfo.class);
    FieldStatsInfo level2Pivot1FieldStatsInfo = mock(FieldStatsInfo.class);
    FieldStatsInfo level2Pivot2FieldStatsInfo = mock(FieldStatsInfo.class);
    List<PivotField> level1Pivots = Arrays.asList(level1Pivot1, level1Pivot2);
    List<PivotField> level2Pivots = Arrays.asList(level2Pivot1, level2Pivot2);

    when(level1Pivot1.getValue()).thenReturn("field1value1");
    when(level1Pivot1.getCount()).thenReturn(1);
    when(level1Pivot1FieldStatsInfo.getSum()).thenReturn(1.0);
    when(level1Pivot1.getFieldStatsInfo()).thenReturn(new HashMap<String, FieldStatsInfo>() {{
      put("score", level1Pivot1FieldStatsInfo);
    }});
    when(level1Pivot2.getValue()).thenReturn("field1value2");
    when(level1Pivot2.getCount()).thenReturn(2);
    when(level1Pivot2FieldStatsInfo.getSum()).thenReturn(2.0);
    when(level1Pivot2.getFieldStatsInfo()).thenReturn(new HashMap<String, FieldStatsInfo>() {{
      put("score", level1Pivot2FieldStatsInfo);
    }});
    when(level2Pivot1.getValue()).thenReturn("field2value1");
    when(level2Pivot1.getCount()).thenReturn(3);
    when(level2Pivot1FieldStatsInfo.getSum()).thenReturn(3.0);
    when(level2Pivot1.getFieldStatsInfo()).thenReturn(new HashMap<String, FieldStatsInfo>() {{
      put("score", level2Pivot1FieldStatsInfo);
    }});
    when(level2Pivot2.getValue()).thenReturn("field2value2");
    when(level2Pivot2.getCount()).thenReturn(4);
    when(level2Pivot2FieldStatsInfo.getSum()).thenReturn(4.0);
    when(level2Pivot2.getFieldStatsInfo()).thenReturn(new HashMap<String, FieldStatsInfo>() {{
      put("score", level2Pivot2FieldStatsInfo);
    }});
    when(level1Pivot1.getPivot()).thenReturn(level2Pivots);

    List<GroupResult> level1GroupResults = solrSearchDao
        .getGroupResults(groupRequest, 0, level1Pivots);

    assertEquals("field1value1", level1GroupResults.get(0).getKey());
    assertEquals(1, level1GroupResults.get(0).getTotal());
    assertEquals(1.0, level1GroupResults.get(0).getScore(), 0.00001);
    assertEquals("field2", level1GroupResults.get(0).getGroupedBy());
    assertEquals("field1value2", level1GroupResults.get(1).getKey());
    assertEquals(2, level1GroupResults.get(1).getTotal());
    assertEquals(2.0, level1GroupResults.get(1).getScore(), 0.00001);
    assertEquals("field2", level1GroupResults.get(1).getGroupedBy());
    assertEquals(0, level1GroupResults.get(1).getGroupResults().size());

    List<GroupResult> level2GroupResults = level1GroupResults.get(0).getGroupResults();
    assertEquals("field2value2", level2GroupResults.get(0).getKey());
    assertEquals(4, level2GroupResults.get(0).getTotal());
    assertEquals(4.0, level2GroupResults.get(0).getScore(), 0.00001);
    assertNull(level2GroupResults.get(0).getGroupedBy());
    assertNull(level2GroupResults.get(0).getGroupResults());
    assertEquals("field2value1", level2GroupResults.get(1).getKey());
    assertEquals(3, level2GroupResults.get(1).getTotal());
    assertEquals(3.0, level2GroupResults.get(1).getScore(), 0.00001);
    assertNull(level2GroupResults.get(1).getGroupedBy());
    assertNull(level2GroupResults.get(1).getGroupResults());
  }


}
