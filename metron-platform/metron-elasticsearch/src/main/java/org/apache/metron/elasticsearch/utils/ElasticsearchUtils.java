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
package org.apache.metron.elasticsearch.utils;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticsearchUtils {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static ThreadLocal<Map<String, SimpleDateFormat>> DATE_FORMAT_CACHE
          = ThreadLocal.withInitial(() -> new HashMap<>());

  /**
   * A delimiter that is appended to the user-defined index name to separate
   * the index's date postfix.
   *
   * For example, if the user-defined index name is 'bro', the delimiter is
   * '_index', and the index's date postfix is '2017.10.03.19', then the actual
   * index name should be 'bro_index_2017.10.03.19'.
   */
  public static final String INDEX_NAME_DELIMITER = "_index";

  public static SimpleDateFormat getIndexFormat(Map<String, Object> globalConfig) {
    String format = (String) globalConfig.get("es.date.format");
    return DATE_FORMAT_CACHE.get().computeIfAbsent(format, SimpleDateFormat::new);
  }

  /**
   * Builds the name of an Elasticsearch index.
   * @param sensorType The sensor type; bro, yaf, snort, ...
   * @param indexPostfix The index postfix; most often a formatted date.
   * @param configurations User-defined configuration for the writers.
   */
  public static String getIndexName(String sensorType, String indexPostfix, WriterConfiguration configurations) {
    String indexName = sensorType;
    if (configurations != null) {
      indexName = configurations.getIndex(sensorType);
    }
    indexName = indexName + INDEX_NAME_DELIMITER + "_" + indexPostfix;
    return indexName;
  }

  public static class HostnamePort {
    public String hostname;
    public Integer port;
    public HostnamePort(String hostname, Integer port) {
      this.hostname = hostname;
      this.port = port;
    }
  }

  public static List<HostnamePort> getIps(Map<String, Object> globalConfiguration) {
    Object ipObj = globalConfiguration.get("es.ip");
    Object portObj = globalConfiguration.get("es.port");
    if(ipObj == null) {
      return Collections.emptyList();
    }
    if(ipObj instanceof String
            && ipObj.toString().contains(",") && ipObj.toString().contains(":")){
      List<String> ips = Arrays.asList(((String)ipObj).split(","));
      List<HostnamePort> ret = new ArrayList<>();
      for(String ip : ips) {
        Iterable<String> tokens = Splitter.on(":").split(ip);
        String host = Iterables.getFirst(tokens, null);
        String portStr = Iterables.getLast(tokens, null);
        ret.add(new HostnamePort(host, Integer.parseInt(portStr)));
      }
      return ret;
    }else if(ipObj instanceof String
            && ipObj.toString().contains(",")){
      List<String> ips = Arrays.asList(((String)ipObj).split(","));
      List<HostnamePort> ret = new ArrayList<>();
      for(String ip : ips) {
        ret.add(new HostnamePort(ip, Integer.parseInt(portObj + "")));
      }
      return ret;
    }else if(ipObj instanceof String
            && !ipObj.toString().contains(":")
            ) {
      return ImmutableList.of(new HostnamePort(ipObj.toString(), Integer.parseInt(portObj + "")));
    }
    else if(ipObj instanceof String
            && ipObj.toString().contains(":")
            ) {
      Iterable<String> tokens = Splitter.on(":").split(ipObj.toString());
      String host = Iterables.getFirst(tokens, null);
      String portStr = Iterables.getLast(tokens, null);
      return ImmutableList.of(new HostnamePort(host, Integer.parseInt(portStr)));
    }
    else if(ipObj instanceof List) {
      List<String> ips = (List)ipObj;
      List<HostnamePort> ret = new ArrayList<>();
      for(String ip : ips) {
        Iterable<String> tokens = Splitter.on(":").split(ip);
        String host = Iterables.getFirst(tokens, null);
        String portStr = Iterables.getLast(tokens, null);
        ret.add(new HostnamePort(host, Integer.parseInt(portStr)));
      }
      return ret;
    }
    throw new IllegalStateException("Unable to read the elasticsearch ips, expected es.ip to be either a list of strings, a string hostname or a host:port string");
  }

  /**
   * Converts an Elasticsearch SearchRequest to JSON.
   * @param esRequest The search request.
   * @return The JSON representation of the SearchRequest.
   */
  public static Optional<String> toJSON(org.elasticsearch.action.search.SearchRequest esRequest) {
    Optional<String> json = Optional.empty();

    if(esRequest != null && esRequest.source() != null) {
      try {
        BytesReference requestBytes = esRequest.source().buildAsBytes();
        json = Optional.of(XContentHelper.convertToJson(requestBytes, true));

      } catch (Throwable t) {
        LOG.error("Failed to convert search request to JSON", t);
      }
    }

    return json;
  }

  /**
   * Convert a SearchRequest to JSON.
   * @param request The search request.
   * @return The JSON representation of the SearchRequest.
   */
  public static Optional<String> toJSON(Object request) {
    Optional<String> json = Optional.empty();

    if(request != null) {
      try {
        json = Optional.of(
                new ObjectMapper()
                        .writer()
                        .withDefaultPrettyPrinter()
                        .writeValueAsString(request));

      } catch (Throwable t) {
        LOG.error("Failed to convert request to JSON", t);
      }
    }

    return json;
  }

  /**
   * Elasticsearch queries default to 10 records returned.  Some internal queries require that all
   * results are returned.  Rather than setting an arbitrarily high size, this method pages through results
   * and returns them all in a single SearchResponse.
   * @param qb A QueryBuilder that provides the query to be run.
   * @return A SearchResponse containing the appropriate results.
   */
  public static  SearchResponse queryAllResults(RestHighLevelClient transportClient,
      QueryBuilder qb,
      String index,
      int pageSize
  ) throws IOException {
    org.elasticsearch.action.search.SearchRequest request = new org.elasticsearch.action.search.SearchRequest();
    SearchSourceBuilder builder = new SearchSourceBuilder();
    builder.query(qb);
    builder.size(pageSize);
    builder.fetchSource(true);
    builder.storedField("*");
    request.source(builder);
    request.indices(index);

    org.elasticsearch.action.search.SearchResponse esResponse = transportClient.search(request);
    List<SearchResult> allResults = getSearchResults(esResponse);
    long total = esResponse.getHits().getTotalHits();
    if (total > pageSize) {
      int pages = (int) (total / pageSize) + 1;
      for (int i = 1; i < pages; i++) {
        int from = i * pageSize;
        builder.from(from);
        esResponse = transportClient.search(request);
        allResults.addAll(getSearchResults(esResponse));
      }
    }
    SearchResponse searchResponse = new SearchResponse();
    searchResponse.setTotal(total);
    searchResponse.setResults(allResults);
    return searchResponse;
  }

  /**
   * Transforms a list of Elasticsearch SearchHits to a list of SearchResults
   * @param searchResponse An Elasticsearch SearchHit to be converted.
   * @return The list of SearchResults for the SearchHit
   */
  protected static List<SearchResult> getSearchResults(
      org.elasticsearch.action.search.SearchResponse searchResponse) {
    return Arrays.stream(searchResponse.getHits().getHits()).map(searchHit -> {
          SearchResult searchResult = new SearchResult();
          searchResult.setId(searchHit.getId());
          searchResult.setSource(searchHit.getSource());
          searchResult.setScore(searchHit.getScore());
          searchResult.setIndex(searchHit.getIndex());
          return searchResult;
        }
    ).collect(Collectors.toList());
  }
}
