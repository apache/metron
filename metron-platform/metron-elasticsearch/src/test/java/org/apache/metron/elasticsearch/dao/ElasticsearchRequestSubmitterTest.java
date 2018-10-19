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

import org.apache.metron.elasticsearch.client.ElasticsearchClient;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.Index;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchShardTarget;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElasticsearchRequestSubmitterTest {

  private ElasticsearchRequestSubmitter submitter;

  public ElasticsearchRequestSubmitter setup(SearchResponse response) throws IOException {

    // mocks
    RestHighLevelClient highLevelClient = mock(RestHighLevelClient.class);
    ElasticsearchClient client = new ElasticsearchClient(mock(RestClient.class), highLevelClient);

    // the client should return the given search response
    when(highLevelClient.search(any())).thenReturn(response);

    return new ElasticsearchRequestSubmitter(client);
  }

  @Test
  public void searchShouldSucceedWhenOK() throws InvalidSearchException, IOException {

    // mocks
    SearchResponse response = mock(SearchResponse.class);
    SearchRequest request = new SearchRequest();

    // response will have status of OK and no failed shards
    when(response.status()).thenReturn(RestStatus.OK);
    when(response.getFailedShards()).thenReturn(0);
    when(response.getTotalShards()).thenReturn(2);

    // search should succeed
    ElasticsearchRequestSubmitter submitter = setup(response);
    SearchResponse actual = submitter.submitSearch(request);
    assertNotNull(actual);
  }

  @Test(expected = InvalidSearchException.class)
  public void searchShouldFailWhenNotOK() throws InvalidSearchException, IOException {

    // mocks
    SearchResponse response = mock(SearchResponse.class);
    SearchRequest request = new SearchRequest();

    // response will have status of OK
    when(response.status()).thenReturn(RestStatus.PARTIAL_CONTENT);
    when(response.getFailedShards()).thenReturn(0);
    when(response.getTotalShards()).thenReturn(2);

    // search should succeed
    ElasticsearchRequestSubmitter submitter = setup(response);
    submitter.submitSearch(request);
  }

  @Test
  public void searchShouldHandleShardFailure() throws InvalidSearchException, IOException {
    // mocks
    SearchResponse response = mock(SearchResponse.class);
    SearchRequest request = new SearchRequest();
    ShardSearchFailure fail = mock(ShardSearchFailure.class);
    SearchShardTarget target = new SearchShardTarget("node1", mock(Index.class), 1, "metron");

    // response will have status of OK
    when(response.status()).thenReturn(RestStatus.OK);

    // the response will report shard failures
    when(response.getFailedShards()).thenReturn(1);
    when(response.getTotalShards()).thenReturn(2);

    // the response will return the failures
    ShardSearchFailure[] failures = { fail };
    when(response.getShardFailures()).thenReturn(failures);

    // shard failure needs to report the node
    when(fail.shard()).thenReturn(target);

    // shard failure needs to report details of failure
    when(fail.index()).thenReturn("bro_index_2017-10-11");
    when(fail.shardId()).thenReturn(1);

    // search should succeed, even with failed shards
    ElasticsearchRequestSubmitter submitter = setup(response);
    SearchResponse actual = submitter.submitSearch(request);
    assertNotNull(actual);
  }
}
