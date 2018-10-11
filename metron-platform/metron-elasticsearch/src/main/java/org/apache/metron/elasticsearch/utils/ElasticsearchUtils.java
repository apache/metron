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
import org.apache.commons.lang.StringUtils;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.utils.HDFSUtils;
import org.apache.metron.common.utils.ReflectionUtils;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.metron.netty.utils.NettyRuntimeWrapper;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.metron.common.Constants.GUID;

public class ElasticsearchUtils {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String ES_CLIENT_CLASS_DEFAULT = "org.elasticsearch.transport.client.PreBuiltTransportClient";
  private static final String PWD_FILE_CONFIG_KEY = "es.xpack.password.file";
  private static final String USERNAME_CONFIG_KEY = "es.xpack.username";
  private static final String TRANSPORT_CLIENT_USER_KEY = "xpack.security.user";

  /**
   * Defines which message field, the document identifier is set to.
   *
   * <p>If defined, the value of the specified message field is set as the Elasticsearch doc ID. If
   * this field is undefined or blank, then the document identifier is not set.
   */
  public static final String DOC_ID_SOURCE_FIELD = "es.document.id";
  public static final String DOC_ID_SOURCE_FIELD_DEFAULT = "";

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
   * Instantiates an Elasticsearch client based on es.client.class, if set. Defaults to
   * org.elasticsearch.transport.client.PreBuiltTransportClient.
   *
   * @param globalConfiguration Metron global config
   * @return
   */
  public static TransportClient getClient(Map<String, Object> globalConfiguration) {
    Set<String> customESSettings = new HashSet<>();
    customESSettings.addAll(Arrays.asList("es.client.class", USERNAME_CONFIG_KEY, PWD_FILE_CONFIG_KEY));
    Settings.Builder settingsBuilder = Settings.builder();
    Map<String, String> esSettings = getEsSettings(globalConfiguration);
    for (Map.Entry<String, String> entry : esSettings.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (!customESSettings.contains(key)) {
        settingsBuilder.put(key, value);
      }
    }
    settingsBuilder.put("cluster.name", globalConfiguration.get("es.clustername"));
    settingsBuilder.put("client.transport.ping_timeout", esSettings.getOrDefault("client.transport.ping_timeout","500s"));
    setXPackSecurityOrNone(settingsBuilder, esSettings);

    try {
      LOG.info("Number of available processors in Netty: {}", NettyRuntimeWrapper.availableProcessors());
      // Netty sets available processors statically and if an attempt is made to set it more than
      // once an IllegalStateException is thrown by NettyRuntime.setAvailableProcessors(NettyRuntime.java:87)
      // https://discuss.elastic.co/t/getting-availableprocessors-is-already-set-to-1-rejecting-1-illegalstateexception-exception/103082
      // https://discuss.elastic.co/t/elasticsearch-5-4-1-availableprocessors-is-already-set/88036
      System.setProperty("es.set.netty.runtime.available.processors", "false");
      TransportClient client = createTransportClient(settingsBuilder.build(), esSettings);
      for (HostnamePort hp : getIps(globalConfiguration)) {
        client.addTransportAddress(
                new InetSocketTransportAddress(InetAddress.getByName(hp.hostname), hp.port)
        );
      }
      return client;
    } catch (UnknownHostException exception) {
      throw new RuntimeException(exception);
    }
  }

  private static Map<String, String> getEsSettings(Map<String, Object> config) {
    return ConversionUtils
        .convertMap((Map<String, Object>) config.getOrDefault("es.client.settings", new HashMap<String, Object>()),
            String.class);
  }

  /*
   * Append Xpack security settings (if any)
   */
  private static void setXPackSecurityOrNone(Settings.Builder settingsBuilder, Map<String, String> esSettings) {

    if (esSettings.containsKey(PWD_FILE_CONFIG_KEY)) {

      if (!esSettings.containsKey(USERNAME_CONFIG_KEY) || StringUtils.isEmpty(esSettings.get(USERNAME_CONFIG_KEY))) {
        throw new IllegalArgumentException("X-pack username is required and cannot be empty");
      }

      settingsBuilder.put(
         TRANSPORT_CLIENT_USER_KEY,
         esSettings.get(USERNAME_CONFIG_KEY) + ":" + getPasswordFromFile(esSettings.get(PWD_FILE_CONFIG_KEY))
      );
    }
  }

  /*
   * Single password on first line
   */
  private static String getPasswordFromFile(String hdfsPath) {
    List<String> lines = null;
    try {
      lines = HDFSUtils.readFile(hdfsPath);
    } catch (IOException e) {
      throw new IllegalArgumentException(
          format("Unable to read XPack password file from HDFS location '%s'", hdfsPath), e);
    }
    if (lines.size() == 0) {
      throw new IllegalArgumentException(format("No password found in file '%s'", hdfsPath));
    }
    return lines.get(0);
  }

  /**
   * Constructs ES transport client from the provided ES settings additional es config
   *
   * @param settings client settings
   * @param esSettings client type to instantiate
   * @return client with provided settings
   */
  private static TransportClient createTransportClient(Settings settings,
      Map<String, String> esSettings) {
    String esClientClassName = (String) esSettings
        .getOrDefault("es.client.class", ES_CLIENT_CLASS_DEFAULT);
    return ReflectionUtils
        .createInstance(esClientClassName, new Class[]{Settings.class, Class[].class},
            new Object[]{settings, new Class[0]});
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
  public static  SearchResponse queryAllResults(TransportClient transportClient,
      QueryBuilder qb,
      String index,
      int pageSize
  ) {
    SearchRequestBuilder searchRequestBuilder = transportClient
        .prepareSearch(index)
        .addStoredField("*")
        .setFetchSource(true)
        .setQuery(qb)
        .setSize(pageSize);
    org.elasticsearch.action.search.SearchResponse esResponse = searchRequestBuilder
        .execute()
        .actionGet();
    List<SearchResult> allResults = getSearchResults(esResponse);
    long total = esResponse.getHits().getTotalHits();
    if (total > pageSize) {
      int pages = (int) (total / pageSize) + 1;
      for (int i = 1; i < pages; i++) {
        int from = i * pageSize;
        searchRequestBuilder.setFrom(from);
        esResponse = searchRequestBuilder
            .execute()
            .actionGet();
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
  protected static List<SearchResult> getSearchResults(org.elasticsearch.action.search.SearchResponse searchResponse) {
    SearchHit[] searchHits = searchResponse.getHits().getHits();
    return Arrays.stream(searchHits)
            .map(hit -> toSearchResult(hit))
            .collect(Collectors.toList());
  }

  /**
   * Transforms a {@link SearchHit} to a {@link SearchResult}.
   *
   * @param searchHit The search hit to transform.
   * @return A {@link SearchResult} representing the {@link SearchHit}.
   */
  protected static SearchResult toSearchResult(SearchHit searchHit) {
    SearchResult searchResult = new SearchResult();
    searchResult.setId(getGUID(searchHit));
    searchResult.setSource(searchHit.getSource());
    searchResult.setScore(searchHit.getScore());
    searchResult.setIndex(searchHit.getIndex());
    return searchResult;
  }

  /**
   * Retrieves the Metron GUID from a {@link SearchHit}.
   *
   * @param searchHit The search hit containing a Metron GUID.
   * @return The Metron GUID.
   */
  public static String getGUID(SearchHit searchHit) {
    String guid;
    if(searchHit.hasSource() && searchHit.getSource().containsKey(GUID)) {
      guid = (String) searchHit.getSource().get(GUID);

    } else if(!searchHit.hasSource()) {
      String template = "No source found, has it been disabled in the mapping? index=%s, docId=%s";
      throw new IllegalStateException(String.format(template, searchHit.getIndex(), searchHit.getId()));

    } else {
      String template = "Missing expected field; field=%s, index=%s, docId=%s";
      throw new IllegalStateException(String.format(template, GUID, searchHit.getIndex(), searchHit.getId()));
    }

    return guid;
  }
}
