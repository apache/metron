/*
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

import org.apache.metron.common.Constants;
import org.apache.metron.elasticsearch.client.ElasticsearchClient;
import org.apache.metron.indexing.dao.RetrieveLatestDao;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.update.Document;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.elasticsearch.index.query.QueryBuilders.typeQuery;

public class ElasticsearchRetrieveLatestDao implements RetrieveLatestDao {

  private ElasticsearchClient client;
  private ElasticsearchRequestSubmitter submitter;

  public ElasticsearchRetrieveLatestDao(ElasticsearchClient client) {
    this.client = client;
    this.submitter = new ElasticsearchRequestSubmitter(client);
  }

  @Override
  public Document getLatest(String guid, String sensorType) throws IOException {
    Optional<Document> doc = searchByGuid(guid, sensorType, hit -> toDocument(hit));
    return doc.orElse(null);
  }

  @Override
  public Iterable<Document> getAllLatest(List<GetRequest> getRequests) throws IOException {
    Collection<String> guids = new HashSet<>();
    Collection<String> sensorTypes = new HashSet<>();
    for (GetRequest getRequest : getRequests) {
      guids.add(getRequest.getGuid());
      sensorTypes.add(getRequest.getSensorType());
    }
    List<Document> documents = searchByGuids(guids, sensorTypes, hit -> toDocument(hit));
    return documents;
  }

  <T> Optional<T> searchByGuid(String guid, String sensorType,
      Function<SearchHit, Optional<T>> callback) throws IOException {
    Collection<String> sensorTypes = sensorType != null ? Collections.singleton(sensorType) : null;
    List<T> results = searchByGuids(Collections.singleton(guid), sensorTypes, callback);
    if (results.size() > 0) {
      return Optional.of(results.get(0));
    } else {
      return Optional.empty();
    }
  }

  /**
   * Return the search hit based on the UUID and sensor type.
   * A callback can be specified to transform the hit into a type T.
   * If more than one hit happens, the first one will be returned.
   */
  <T> List<T> searchByGuids(Collection<String> guids, Collection<String> sensorTypes,
      Function<SearchHit, Optional<T>> callback) throws IOException {
    if (guids == null || guids.isEmpty()) {
      return Collections.emptyList();
    }

    // should match any of the guids
    // the 'guid' field must be of type 'keyword' or this term query will not match
    BoolQueryBuilder guidQuery = boolQuery().must(termsQuery(Constants.GUID, guids));

    // should match any of the sensor types
    BoolQueryBuilder sensorQuery = boolQuery();
    sensorTypes.forEach(sensorType -> sensorQuery.should(typeQuery(sensorType + "_doc")));

    // must have a match for both guid and sensor
    BoolQueryBuilder query = boolQuery()
            .must(guidQuery)
            .must(sensorQuery);

    // submit the search
    SearchResponse response;
    try {
      SearchSourceBuilder source = new SearchSourceBuilder()
              .query(query)
              .size(guids.size());
      SearchRequest request = new SearchRequest().source(source);
      response = submitter.submitSearch(request);

    } catch(InvalidSearchException e) {
      throw new IOException(e);
    }

    // transform the search hits to results using the callback
    List<T> results = new ArrayList<>();
    for(SearchHit hit: response.getHits()) {
      Optional<T> result = callback.apply(hit);
      result.ifPresent(r -> results.add(r));
    }

    return results;
  }

  private Optional<Document> toDocument(SearchHit hit) {
    Document document = Document.fromJSON(hit.getSource());
    document.setDocumentID(hit.getId());

    return Optional.of(document);
  }
}
