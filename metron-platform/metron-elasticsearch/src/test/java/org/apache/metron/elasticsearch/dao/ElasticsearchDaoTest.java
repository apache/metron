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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SortField;
import org.apache.metron.indexing.dao.search.SortOrder;
import org.apache.metron.elasticsearch.utils.ElasticsearchUtils;
import org.apache.metron.indexing.dao.search.FieldType;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

public class ElasticsearchDaoTest {

  private ElasticsearchDao dao;
  private ElasticsearchRequestSubmitter requestSubmitter;

  private void setup(RestStatus status, int maxSearchResults, Map<String, FieldType> metadata) throws Exception {

    // setup the mock search hits
    SearchHit hit1 = mock(SearchHit.class);
    when(hit1.getId()).thenReturn("id1");
    when(hit1.getSource()).thenReturn(new HashMap<String, Object>(){{ put("field", "value1"); }});
    when(hit1.getScore()).thenReturn(0.1f);

    SearchHit hit2 = mock(SearchHit.class);
    when(hit2.getId()).thenReturn("id2");
    when(hit2.getSource()).thenReturn(new HashMap<String, Object>(){{ put("field", "value2"); }});
    when(hit2.getScore()).thenReturn(0.2f);

    // search hits
    SearchHit[] hits = { hit1, hit2 };
    SearchHits searchHits = mock(SearchHits.class);
    when(searchHits.getHits()).thenReturn(hits);
    when(searchHits.getTotalHits()).thenReturn(Integer.toUnsignedLong(hits.length));

    // search response which returns the search hits
    org.elasticsearch.action.search.SearchResponse response = mock(org.elasticsearch.action.search.SearchResponse.class);
    when(response.status()).thenReturn(status);
    when(response.getHits()).thenReturn(searchHits);

    // provides column metadata
    ColumnMetadataDao columnMetadataDao = mock(ColumnMetadataDao.class);
    when(columnMetadataDao.getColumnMetadata(any())).thenReturn(metadata);

    // returns the search response
    requestSubmitter = mock(ElasticsearchRequestSubmitter.class);
    when(requestSubmitter.submitSearch(any())).thenReturn(response);

    TransportClient client = mock(TransportClient.class);

    // provides configuration
    AccessConfig config = mock(AccessConfig.class);
    when(config.getMaxSearchResults()).thenReturn(maxSearchResults);

    dao = new ElasticsearchDao(client, columnMetadataDao, requestSubmitter, config);
  }

  private void setup(RestStatus status, int maxSearchResults) throws Exception {
    setup(status, maxSearchResults, new HashMap<>());
  }

  @Test
  public void searchShouldSortByGivenFields() throws Exception {

    // setup the column metadata
    Map<String, FieldType> columnMetadata = new HashMap<>();
    columnMetadata.put("sortByStringDesc", FieldType.TEXT);
    columnMetadata.put("sortByIntAsc", FieldType.INTEGER);

    // setup the dao
    setup(RestStatus.OK, 25, columnMetadata);

    // "sort by" fields for the search request
    SortField[] expectedSortFields = {
            sortBy("sortByStringDesc", SortOrder.DESC),
            sortBy("sortByIntAsc", SortOrder.ASC),
            sortBy("sortByUndefinedDesc", SortOrder.DESC)
    };

    // create a metron search request
    final List<String> indices = Arrays.asList("bro", "snort");
    SearchRequest searchRequest = new SearchRequest();
    searchRequest.setSize(2);
    searchRequest.setIndices(indices);
    searchRequest.setFrom(5);
    searchRequest.setSort(Arrays.asList(expectedSortFields));
    searchRequest.setQuery("some query");

    // submit the metron search request
    SearchResponse searchResponse = dao.search(searchRequest);
    assertNotNull(searchResponse);

    // capture the elasticsearch search request that was created
    ArgumentCaptor<org.elasticsearch.action.search.SearchRequest> argument = ArgumentCaptor.forClass(org.elasticsearch.action.search.SearchRequest.class);
    verify(requestSubmitter).submitSearch(argument.capture());
    org.elasticsearch.action.search.SearchRequest request = argument.getValue();

    // transform the request to JSON for validation
    JSONParser parser = new JSONParser();
    JSONObject json = (JSONObject) parser.parse(ElasticsearchUtils.toJSON(request).orElse("???"));

    // validate the sort fields
    JSONArray sortFields = (JSONArray) json.get("sort");
    assertEquals(3, sortFields.size());

    {
      // sort by string descending
      JSONObject aSortField = (JSONObject) sortFields.get(0);
      JSONObject sortBy = (JSONObject) aSortField.get("sortByStringDesc");
      assertEquals("desc", sortBy.get("order"));
      assertEquals("_last", sortBy.get("missing"));
      assertEquals("text", sortBy.get("unmapped_type"));
    }
    {
      // sort by integer ascending
      JSONObject aSortField = (JSONObject) sortFields.get(1);
      JSONObject sortByIntAsc = (JSONObject) aSortField.get("sortByIntAsc");
      assertEquals("asc", sortByIntAsc.get("order"));
      assertEquals("_first", sortByIntAsc.get("missing"));
      assertEquals("integer", sortByIntAsc.get("unmapped_type"));
    }
    {
      // sort by unknown type
      JSONObject aSortField = (JSONObject) sortFields.get(2);
      JSONObject sortByUndefinedDesc = (JSONObject) aSortField.get("sortByUndefinedDesc");
      assertEquals("desc", sortByUndefinedDesc.get("order"));
      assertEquals("_last", sortByUndefinedDesc.get("missing"));
      assertEquals("other", sortByUndefinedDesc.get("unmapped_type"));
    }
  }

  @Test
  public void searchShouldWildcardIndices() throws Exception {

    // setup the dao
    setup(RestStatus.OK, 25);

    // "sort by" fields for the search request
    SortField[] expectedSortFields = {
            sortBy("sortByStringDesc", SortOrder.DESC),
            sortBy("sortByIntAsc", SortOrder.ASC),
            sortBy("sortByUndefinedDesc", SortOrder.DESC)
    };

    // create a metron search request
    final List<String> indices = Arrays.asList("bro", "snort");
    SearchRequest searchRequest = new SearchRequest();
    searchRequest.setSize(2);
    searchRequest.setIndices(indices);
    searchRequest.setFrom(5);
    searchRequest.setSort(Arrays.asList(expectedSortFields));
    searchRequest.setQuery("some query");

    // submit the metron search request
    SearchResponse searchResponse = dao.search(searchRequest);
    assertNotNull(searchResponse);

    // capture the elasticsearch search request that was created
    ArgumentCaptor<org.elasticsearch.action.search.SearchRequest> argument = ArgumentCaptor.forClass(org.elasticsearch.action.search.SearchRequest.class);
    verify(requestSubmitter).submitSearch(argument.capture());
    org.elasticsearch.action.search.SearchRequest request = argument.getValue();

    // transform the request to JSON for validation
    JSONParser parser = new JSONParser();
    JSONObject json = (JSONObject) parser.parse(ElasticsearchUtils.toJSON(request).orElse("???"));

    // ensure that the index names are 'wildcard-ed'
    String[] expected = { "bro_index*", "snort_index*" };
    assertArrayEquals(expected, request.indices());
  }


  @Test(expected = InvalidSearchException.class)
  public void searchShouldThrowExceptionWhenMaxResultsAreExceeded() throws Exception {

    int maxSearchResults = 20;
    setup(RestStatus.OK, maxSearchResults);

    SearchRequest searchRequest = new SearchRequest();
    searchRequest.setSize(maxSearchResults+1);
    searchRequest.setQuery("");
    dao.search(searchRequest);
    // exception expected - size > max
  }

  private SortField sortBy(String field, SortOrder order) {
    SortField sortField = new SortField();
    sortField.setField(field);
    sortField.setSortOrder(order.toString());
    return sortField;
  }

}
