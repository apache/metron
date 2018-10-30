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

package org.apache.metron.elasticsearch.client;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
import org.apache.metron.elasticsearch.config.ElasticsearchClientConfig;
import org.apache.metron.elasticsearch.utils.ElasticsearchUtils;
import org.apache.metron.elasticsearch.utils.ElasticsearchUtils.HostnamePort;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point to create the ES client.
 */
public class ElasticsearchClientFactory {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Creates an Elasticsearch client from settings provided via the global config.
   *
   * @return new client
   */
  public static ElasticsearchClient create(Map<String, Object> globalConfig) {
    ElasticsearchClientConfig esClientConfig = new ElasticsearchClientConfig(
        getEsSettings(globalConfig));

    HttpHost[] httpHosts = getHttpHosts(globalConfig, esClientConfig.getConnectionScheme());
    RestClientBuilder builder = RestClient.builder(httpHosts);

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
    return (Map<String, Object>) globalConfig.getOrDefault("es.client.settings", new HashMap<>());
  }

  private static HttpHost[] getHttpHosts(Map<String, Object> globalConfiguration, String scheme) {
    List<HostnamePort> hps = ElasticsearchUtils.getIps(globalConfiguration);
    HttpHost[] httpHosts = new HttpHost[hps.size()];
    int i = 0;
    for (HostnamePort hp : hps) {
      httpHosts[i++] = new HttpHost(hp.hostname, hp.port, scheme);
    }
    return httpHosts;
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
  private static void setupAuthentication(HttpAsyncClientBuilder clientBuilder,
      ElasticsearchClientConfig esClientConfig) {
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

}
