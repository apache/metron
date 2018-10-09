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

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.metron.elasticsearch.utils.ElasticsearchClient;
import org.apache.metron.indexing.dao.RetrieveLatestDao;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

public class ElasticsearchRetrieveLatestDao implements RetrieveLatestDao {

  private ElasticsearchClient transportClient;

  public ElasticsearchRetrieveLatestDao(ElasticsearchClient transportClient) {
    this.transportClient = transportClient;
  }

  @Override
  public Document getLatest(String guid, String sensorType) throws IOException {
    Optional<Document> doc = searchByGuid(guid, sensorType, hit -> toDocument(guid, hit));
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
    List<Document> documents = searchByGuids(
        guids,
        sensorTypes,
        hit -> {
          Long ts = 0L;
          String doc = hit.getSourceAsString();
          String sourceType = Iterables.getFirst(Splitter.on("_doc").split(hit.getType()), null);
          try {
            return Optional.of(new Document(doc, hit.getId(), sourceType, ts));
          } catch (IOException e) {
            throw new IllegalStateException("Unable to retrieve latest: " + e.getMessage(), e);
          }
        }

    );
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
    QueryBuilder query = null;
    IdsQueryBuilder idsQuery;
    if (sensorTypes != null) {
      String[] types = sensorTypes.stream().map(sensorType -> sensorType + "_doc")
          .toArray(String[]::new);
      idsQuery = QueryBuilders.idsQuery(types);
    } else {
      idsQuery = QueryBuilders.idsQuery();
    }

    for (String guid : guids) {
      query = idsQuery.addIds(guid);
    }
    SearchRequest request = new SearchRequest();
    SearchSourceBuilder builder = new SearchSourceBuilder();
    builder.query(query);
    builder.size(guids.size());
    request.source(builder);

    org.elasticsearch.action.search.SearchResponse response = transportClient.getHighLevelClient().search(request);
    SearchHits hits = response.getHits();
    List<T> results = new ArrayList<>();
    for (SearchHit hit : hits) {
      Optional<T> result = callback.apply(hit);
      if (result.isPresent()) {
        results.add(result.get());
      }
    }
    return results;
  }

  private Optional<Document> toDocument(final String guid, SearchHit hit) {
    Long ts = 0L;
    String doc = hit.getSourceAsString();
    String sourceType = toSourceType(hit.getType());
    try {
      return Optional.of(new Document(doc, guid, sourceType, ts));
    } catch (IOException e) {
      throw new IllegalStateException("Unable to retrieve latest: " + e.getMessage(), e);
    }
  }

  /**
   * Returns the source type based on a given doc type.
   * @param docType The document type.
   * @return The source type.
   */
  private String toSourceType(String docType) {
    return Iterables.getFirst(Splitter.on("_doc").split(docType), null);
  }
}
