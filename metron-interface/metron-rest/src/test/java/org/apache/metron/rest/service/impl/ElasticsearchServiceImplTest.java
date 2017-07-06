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
package org.apache.metron.rest.service.impl;

import org.apache.metron.rest.RestException;
import org.apache.metron.rest.matcher.SearchRequestMatcher;
import org.apache.metron.rest.model.SearchRequest;
import org.apache.metron.rest.model.SearchResponse;
import org.apache.metron.rest.model.SearchResult;
import org.apache.metron.rest.model.SortField;
import org.apache.metron.rest.model.SortOrder;
import org.apache.metron.rest.service.SearchService;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ElasticsearchServiceImplTest {

  private SearchService searchService;
  TransportClient client;

  @Before
  public void setUp() throws Exception {
    client = mock(TransportClient.class);
    searchService = new ElasticsearchServiceImpl(client);
  }

  @Test
  public void searchShouldProperlyBuildSearchRequest() throws RestException {
    SearchHit searchHit1 = mock(SearchHit.class);
    when(searchHit1.getId()).thenReturn("id1");
    when(searchHit1.getSource()).thenReturn(new HashMap<String, Object>(){{ put("field", "value1"); }});
    when(searchHit1.getScore()).thenReturn(0.1f);
    SearchHit searchHit2 = mock(SearchHit.class);
    when(searchHit2.getId()).thenReturn("id2");
    when(searchHit2.getSource()).thenReturn(new HashMap<String, Object>(){{ put("field", "value2"); }});
    when(searchHit2.getScore()).thenReturn(0.2f);
    SearchHits searchHits = mock(SearchHits.class);
    when(searchHits.getHits()).thenReturn(new SearchHit[]{searchHit1, searchHit2});
    when(searchHits.getTotalHits()).thenReturn(2L);
    org.elasticsearch.action.search.SearchResponse elasticsearchResponse = mock(org.elasticsearch.action.search.SearchResponse.class);
    when(elasticsearchResponse.getHits()).thenReturn(searchHits);
    ActionFuture actionFuture = mock(ActionFuture.class);
    when(actionFuture.actionGet()).thenReturn(elasticsearchResponse);
    when(client.search(any())).thenReturn(actionFuture);

    SearchRequest searchRequest = new SearchRequest();
    searchRequest.setSize(2);
    searchRequest.setIndices(Arrays.asList("bro", "snort"));
    searchRequest.setFrom(5);
    SortField sortField1 = new SortField();
    sortField1.setField("sortField1");
    sortField1.setSortOrder(SortOrder.DESC);
    SortField sortField2 = new SortField();
    sortField2.setField("sortField2");
    sortField2.setSortOrder(SortOrder.ASC);
    searchRequest.setSort(Arrays.asList(sortField1, sortField2));
    searchRequest.setQuery("some query");
    SearchResponse searchResponse = searchService.search(searchRequest);
    verify(client, times(1)).search(argThat(new SearchRequestMatcher(new String[]{"bro*", "snort*"}, "some query", 2, 5, new SortField[]{sortField1, sortField2})));
    assertEquals(2, searchResponse.getTotal());
    List<SearchResult> actualSearchResults = searchResponse.getResults();
    assertEquals(2, actualSearchResults.size());
    assertEquals("id1", actualSearchResults.get(0).getId());
    assertEquals("value1", actualSearchResults.get(0).getSource().get("field"));
    assertEquals(0.1f, actualSearchResults.get(0).getScore(), 0.0f);
    assertEquals("id2", actualSearchResults.get(1).getId());
    assertEquals("value2", actualSearchResults.get(1).getSource().get("field"));
    assertEquals(0.2f, actualSearchResults.get(1).getScore(), 0.0f);
    verifyNoMoreInteractions(client);
  }



}
