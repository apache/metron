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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.metron.elasticsearch.utils.ElasticsearchClient;
import org.apache.metron.elasticsearch.utils.ElasticsearchUtils;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.rest.RestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Responsible for submitting requests to Elasticsearch.
 */
public class ElasticsearchRequestSubmitter {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * The Elasticsearch client.
   */
  private ElasticsearchClient client;

  public ElasticsearchRequestSubmitter(ElasticsearchClient client) {
    this.client = client;
  }

  /**
   * Submit a search to Elasticsearch.
   * @param request A search request.
   * @return The search response.
   */
  public SearchResponse submitSearch(SearchRequest request) throws InvalidSearchException {
    LOG.debug("About to submit a search; request={}", ElasticsearchUtils.toJSON(request).orElse("???"));

    // submit the search request
    org.elasticsearch.action.search.SearchResponse esResponse;
    try {
      esResponse = client.getHighLevelClient().search(request);
      LOG.debug("Got Elasticsearch response; response={}", esResponse.toString());

    } catch (Exception e) {
      String msg = String.format(
              "Failed to execute search; error='%s', search='%s'",
              ExceptionUtils.getRootCauseMessage(e),
              ElasticsearchUtils.toJSON(request).orElse("???"));
      LOG.error(msg, e);
      throw new InvalidSearchException(msg, e);
    }

    // check for shard failures
    if(esResponse.getFailedShards() > 0) {
      handleShardFailures(request, esResponse);
    }

    // validate the response status
    if(RestStatus.OK == esResponse.status()) {
      return esResponse;

    } else {
      // the search was not successful
      String msg = String.format(
              "Bad search response; status=%s, timeout=%s, terminatedEarly=%s",
              esResponse.status(), esResponse.isTimedOut(), esResponse.isTerminatedEarly());
      LOG.error(msg);
      throw new InvalidSearchException(msg);
    }
  }

  /**
   * Handle individual shard failures that can occur even when the response is OK.  These
   * can indicate misconfiguration of the search indices.
   * @param request The search request.
   * @param response  The search response.
   */
  private void handleShardFailures(
          org.elasticsearch.action.search.SearchRequest request,
          org.elasticsearch.action.search.SearchResponse response) {
    /*
     * shard failures are only logged.  the search itself is not failed.  this approach
     * assumes that a user is interested in partial search results, even if the
     * entire search result set cannot be produced.
     *
     * for example, assume the user adds an additional sensor and the telemetry
     * is indexed into a new search index.  if that search index is misconfigured,
     * it can result in partial shard failures.  rather than failing the entire search,
     * we log the error and allow the results to be returned from shards that
     * are correctly configured.
     */
    int errors = ArrayUtils.getLength(response.getShardFailures());
    LOG.error("Search resulted in {}/{} shards failing; errors={}, search={}",
            response.getFailedShards(),
            response.getTotalShards(),
            errors,
            ElasticsearchUtils.toJSON(request).orElse("???"));

    // log each reported failure
    int failureCount=1;
    for(ShardSearchFailure fail: response.getShardFailures()) {
      String msg = String.format(
              "Shard search failure [%s/%s]; reason=%s, index=%s, shard=%s, status=%s, nodeId=%s",
              failureCount,
              errors,
              ExceptionUtils.getRootCauseMessage(fail.getCause()),
              fail.index(),
              fail.shardId(),
              fail.status(),
              fail.shard().getNodeId());
      LOG.error(msg, fail.getCause());
    }
  }
}
