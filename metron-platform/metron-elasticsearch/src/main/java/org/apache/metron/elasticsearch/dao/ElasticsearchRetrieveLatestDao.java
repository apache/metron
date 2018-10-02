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
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.elasticsearch.utils.ElasticsearchUtils;
import org.apache.metron.indexing.dao.RetrieveLatestDao;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

public class ElasticsearchRetrieveLatestDao implements RetrieveLatestDao {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private TransportClient transportClient;

  public ElasticsearchRetrieveLatestDao(TransportClient transportClient) {
    this.transportClient = transportClient;
  }

  @Override
  public Document getLatest(String guid, String sensorType) {
    Optional<Document> doc = searchByGuid(guid, sensorType, hit -> toDocument(hit));
    return doc.orElse(null);
  }

  @Override
  public Iterable<Document> getAllLatest(List<GetRequest> getRequests) {
    Collection<String> guids = new HashSet<>();
    Collection<String> sensorTypes = new HashSet<>();
    for (GetRequest getRequest : getRequests) {
      guids.add(getRequest.getGuid());
      sensorTypes.add(getRequest.getSensorType());
    }
    return searchByGuids(guids, sensorTypes, hit -> toDocument(hit));
  }

  /**
   * Search for a Metron GUID.
   *
   * @param guid The Metron GUID to search for.
   * @param sensorType The sensor type to include in the search.
   * @param callback A callback that transforms a search hit to a search result.
   * @param <T> The type of search results expected.
   * @return All documents with the given Metron GUID and sensor type.
   */
  <T> Optional<T> searchByGuid(String guid, String sensorType, Function<SearchHit, Optional<T>> callback) {
    Collection<String> sensorTypes = sensorType != null ? Collections.singleton(sensorType) : null;
    List<T> results = searchByGuids(Collections.singleton(guid), sensorTypes, callback);
    if (results.size() > 0) {
      return Optional.of(results.get(0));
    } else {
      return Optional.empty();
    }
  }

  /**
   * Search for one or more Metron GUIDs.
   *
   * @param guids The Metron GUIDs to search by.
   * @param sensorTypes The sensor types to include in the search.
   * @param callback A callback that transforms a search hit to a search result.
   * @param <T> The type of expected results.
   * @return All documents with the given Metron GUID and sensor type.
   */
  <T> List<T> searchByGuids(Collection<String> guids,
                            Collection<String> sensorTypes,
                            Function<SearchHit, Optional<T>> callback) {
    if (guids == null || guids.isEmpty()) {
      return Collections.emptyList();
    }

    // search by metron's GUID field
    QueryBuilder query = boolQuery().must(termsQuery(Constants.GUID, guids));
    SearchRequestBuilder request = transportClient
            .prepareSearch()
            .setQuery(query)
            .setSize(guids.size());

    if(sensorTypes != null) {
      request.setTypes(toDocTypes(sensorTypes));
    }

    // transform the search hits to results
    SearchHits hits = request.get().getHits();
    return getResults(hits, callback);
  }

  /**
   * Retrieve search results.
   *
   * @param hits The search hit.
   * @param callback A callback function that transforms search hits to search results.
   * @param <T> The execpted type of search result.
   * @return The search results.
   */
  private <T> List<T> getResults(SearchHits hits, Function<SearchHit, Optional<T>> callback) {
    List<T> results = new ArrayList<>();

    for (SearchHit hit : hits) {
      Optional<T> result = callback.apply(hit);
      if (result.isPresent()) {
        results.add(result.get());
      }
    }

    return results;
  }

  /**
   * Transforms a {@link SearchHit} to a {@link Document}.
   *
   * @param hit The result of a search.
   * @return An optional {@link Document}.
   */
  private Optional<Document> toDocument(SearchHit hit) {
    Long ts = 0L;
    String doc = hit.getSourceAsString();
    String sourceType = toSourceType(hit.getType());
    String guid = ElasticsearchUtils.getGUID(hit);
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

  /**
   * Returns the doc types for a given collection of sensor types.
   *
   * @param sensorTypes The sensor types.
   * @return The doc types associated with each sensor.
   */
  private String[] toDocTypes(Collection<String> sensorTypes) {
    String[] result;

    if(sensorTypes != null && sensorTypes.size() > 0) {
      result = sensorTypes
              .stream()
              .map(sensorType -> sensorType + "_doc")
              .toArray(String[]::new);

    } else {
      result = new String[0];
    }

    return result;
  }
}
