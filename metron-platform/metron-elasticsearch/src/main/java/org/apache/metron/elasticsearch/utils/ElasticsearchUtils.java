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

import static java.lang.String.format;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.elasticsearch.client.ElasticsearchClient;
import org.apache.metron.elasticsearch.config.ElasticsearchClientConfig;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
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

  public static SimpleDateFormat getIndexFormat(WriterConfiguration configurations) {
    return getIndexFormat(configurations.getGlobalConfig());
  }

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

  /**
   * Extracts the base index name from a full index name.
   *
   * For example, given an index named 'bro_index_2017.01.01.01', the base
   * index name is 'bro'.
   *
   * @param indexName The full index name including delimiter and date postfix.
   * @return The base index name.
   */
  public static String getBaseIndexName(String indexName) {

    String[] parts = indexName.split(INDEX_NAME_DELIMITER);
    if(parts.length < 1 || StringUtils.isEmpty(parts[0])) {
      String msg = format("Unexpected index name; index=%s, delimiter=%s", indexName, INDEX_NAME_DELIMITER);
      throw new IllegalStateException(msg);
    }

    return parts[0];
  }

  /**
   * Instantiates an Elasticsearch client
   *
   * @param globalConfiguration Metron global config
   * @return new es client
   */
  public static ElasticsearchClient getClient(Map<String, Object> globalConfiguration) {
    ElasticsearchClientConfig esClientConfig = new ElasticsearchClientConfig(getEsSettings(globalConfiguration));

    String scheme = esClientConfig.isSSLEnabled() ? "https" : "http";
    RestClientBuilder builder = getRestClientBuilder(globalConfiguration, scheme);

    RestClientBuilder.RequestConfigCallback reqCallback = reqConfigBuilder -> {
      setupConnectionTimeouts(reqConfigBuilder, esClientConfig);
      return reqConfigBuilder;
    };
    builder.setRequestConfigCallback(reqCallback);
    builder.setMaxRetryTimeoutMillis(esClientConfig.getMaxRetryTimeoutMillis());

    RestClientBuilder.HttpClientConfigCallback httpClientConfigCallback = clientBuilder -> {
      setupNumConnectionThreads(clientBuilder, esClientConfig);
      setupAuthentication(clientBuilder, esClientConfig);
      setupConnectionEncryption(clientBuilder, esClientConfig);
      return clientBuilder;
    };
    builder.setHttpClientConfigCallback(httpClientConfigCallback);

    RestClient lowLevelClient = builder.build();
    RestHighLevelClient client = new RestHighLevelClient(lowLevelClient);
    return new ElasticsearchClient(lowLevelClient, client);
  }

  private static Map<String, Object> getEsSettings(Map<String, Object> globalConfig) {
    return (Map<String, Object>) globalConfig.getOrDefault("es.client.settings", new HashMap<String, Object>());
  }

  private static RestClientBuilder getRestClientBuilder(Map<String, Object> globalConfiguration,
      String scheme) {
    List<HostnamePort> hps = getIps(globalConfiguration);
    HttpHost[] posts = new HttpHost[hps.size()];
    int i = 0;
    for (HostnamePort hp : hps) {
      posts[i++] = new HttpHost(hp.hostname, hp.port, scheme);
    }
    return RestClient.builder(posts);
  }

  /**
   * Modifies request config builder with connection and socket timeouts.
   * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/5.6/_timeouts.html
   *
   * @param reqConfigBuilder builder to modify
   * @param esClientConfig pull timeout settings from this config
   */
  private static void setupConnectionTimeouts(Builder reqConfigBuilder,
      ElasticsearchClientConfig esClientConfig) {
    reqConfigBuilder.setConnectTimeout(esClientConfig.getConnectTimeoutMillis());
    reqConfigBuilder.setSocketTimeout(esClientConfig.getSocketTimeoutMillis());
  }

  /**
   * Modifies client builder with setting for num connection threads. Default is ES client default,
   * which is 1 to num processors per the documentation.
   * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/5.6/_number_of_threads.html
   *
   * @param clientBuilder builder to modify
   * @param esClientConfig pull num threads property from config
   */
  private static void setupNumConnectionThreads(HttpAsyncClientBuilder clientBuilder,
      ElasticsearchClientConfig esClientConfig) {
    if (esClientConfig.getNumClientConnectionThreads().isPresent()) {
      Integer numThreads = esClientConfig.getNumClientConnectionThreads().get();
      LOG.info("Setting number of client connection threads: {}", numThreads);
      clientBuilder.setDefaultIOReactorConfig(IOReactorConfig.custom()
          .setIoThreadCount(numThreads).build());
    }
  }

  /**
   * Modifies client builder with settings for authentication with X-Pack.
   * Note, we do not expose the ability to disable preemptive authentication.
   * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/5.6/_basic_authentication.html
   *
   * @param clientBuilder builder to modify
   * @param esClientConfig pull credentials property from config
   */
  private static void setupAuthentication(HttpAsyncClientBuilder clientBuilder, ElasticsearchClientConfig esClientConfig) {
    Optional<Entry<String, String>> credentials = esClientConfig.getCredentials();
    if (credentials.isPresent()) {
      LOG.info(
          "Found auth credentials - setting up user/pass authenticated client connection for ES.");
      final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      UsernamePasswordCredentials upcredentials = new UsernamePasswordCredentials(
          credentials.get().getKey(), credentials.get().getValue());
      credentialsProvider.setCredentials(AuthScope.ANY, upcredentials);
      clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
    } else {
      LOG.info(
          "Elasticsearch client credentials not provided. Defaulting to non-authenticated client connection.");
    }
  }

  /**
   * Modify client builder with connection encryption details (SSL) if applicable.
   * If ssl.enabled=true, sets up SSL connection. If enabled, keystore.path is required. User can
   * also optionally set keystore.password and keystore.type.
   * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/5.6/_encrypted_communication.html
   *
   * @param clientBuilder builder to modify
   * @param esClientConfig pull connection encryption details from config
   */
  private static void setupConnectionEncryption(HttpAsyncClientBuilder clientBuilder,
      ElasticsearchClientConfig esClientConfig) {
    if (esClientConfig.isSSLEnabled()) {
      LOG.info("Configuring client for SSL connection.");
      if (!esClientConfig.getKeyStorePath().isPresent()) {
        throw new IllegalStateException("KeyStore path must be provided for SSL connection.");
      }
      KeyStore truststore;
      try {
        truststore = KeyStore.getInstance(esClientConfig.getKeyStoreType());
      } catch (KeyStoreException e) {
        throw new IllegalStateException(
            "Unable to get keystore type '" + esClientConfig.getKeyStoreType() + "'", e);
      }
      Optional<String> optKeyStorePass = esClientConfig.getKeyStorePassword();
      char[] keyStorePass = optKeyStorePass.map(String::toCharArray).orElse(null);
      try (InputStream is = Files.newInputStream(esClientConfig.getKeyStorePath().get())) {
        truststore.load(is, keyStorePass);
      } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
        throw new IllegalStateException(
            "Unable to load keystore from path '" + esClientConfig.getKeyStorePath().get() + "'",
            e);
      }
      try {
        SSLContextBuilder sslBuilder = SSLContexts.custom().loadTrustMaterial(truststore, null);
        clientBuilder.setSSLContext(sslBuilder.build());
      } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
        throw new IllegalStateException("Unable to load truststore.", e);
      }
    }
  }

  public static class HostnamePort {
    String hostname;
    Integer port;
    public HostnamePort(String hostname, Integer port) {
      this.hostname = hostname;
      this.port = port;
    }
  }

  protected static List<HostnamePort> getIps(Map<String, Object> globalConfiguration) {
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
