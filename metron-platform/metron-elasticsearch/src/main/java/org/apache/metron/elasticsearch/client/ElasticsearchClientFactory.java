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
import java.nio.file.Path;
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
import javax.net.ssl.SSLContext;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
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
  private static final String ES_SETTINGS_KEY = "es.client.settings"; // es config key in global config

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

    builder.setRequestConfigCallback(reqConfigBuilder -> {
      // Modifies request config builder with connection and socket timeouts.
      // https://www.elastic.co/guide/en/elasticsearch/client/java-rest/5.6/_timeouts.html
      reqConfigBuilder.setConnectTimeout(esClientConfig.getConnectTimeoutMillis());
      reqConfigBuilder.setSocketTimeout(esClientConfig.getSocketTimeoutMillis());
      return reqConfigBuilder;
    });
    builder.setMaxRetryTimeoutMillis(esClientConfig.getMaxRetryTimeoutMillis());

    builder.setHttpClientConfigCallback(clientBuilder -> {
      clientBuilder.setDefaultIOReactorConfig(getIOReactorConfig(esClientConfig));
      clientBuilder.setDefaultCredentialsProvider(getCredentialsProvider(esClientConfig));
      clientBuilder.setSSLContext(getSSLContext(esClientConfig));
      return clientBuilder;
    });

    RestClient lowLevelClient = builder.build();
    RestHighLevelClient client = new RestHighLevelClient(lowLevelClient);
    return new ElasticsearchClient(lowLevelClient, client);
  }

  private static Map<String, Object> getEsSettings(Map<String, Object> globalConfig) {
    return (Map<String, Object>) globalConfig.getOrDefault(ES_SETTINGS_KEY, new HashMap<>());
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
   * Creates config with setting for num connection threads. Default is ES client default,
   * which is 1 to num processors per the documentation.
   * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/5.6/_number_of_threads.html
   */
  private static IOReactorConfig getIOReactorConfig(ElasticsearchClientConfig esClientConfig) {
    if (esClientConfig.getNumClientConnectionThreads().isPresent()) {
      Integer numThreads = esClientConfig.getNumClientConnectionThreads().get();
      LOG.info("Setting number of client connection threads: {}", numThreads);
      return IOReactorConfig.custom().setIoThreadCount(numThreads).build();
    } else {
      return IOReactorConfig.DEFAULT;
    }
  }

  private static CredentialsProvider getCredentialsProvider(
      ElasticsearchClientConfig esClientConfig) {
    Optional<Entry<String, String>> credentials = esClientConfig.getCredentials();
    if (credentials.isPresent()) {
      LOG.info(
          "Found auth credentials - setting up user/pass authenticated client connection for ES.");
      final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      UsernamePasswordCredentials upcredentials = new UsernamePasswordCredentials(
          credentials.get().getKey(), credentials.get().getValue());
      credentialsProvider.setCredentials(AuthScope.ANY, upcredentials);
      return credentialsProvider;
    } else {
      LOG.info(
          "Elasticsearch client credentials not provided. Defaulting to non-authenticated client connection.");
      return null;
    }
  }

  /**
   * <p>Setup connection encryption details (SSL) if applicable.
   * If ssl.enabled=true, sets up SSL connection. If enabled, keystore.path is required. User can
   * also optionally set keystore.password and keystore.type.
   * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/5.6/_encrypted_communication.html
   * <p>
   * <p>Other guidance on the HTTP Component library and configuring SSL connections.
   * http://www.robinhowlett.com/blog/2016/01/05/everything-you-ever-wanted-to-know-about-ssl-but-were-afraid-to-ask.
   * <p>
   * <p>JSSE docs - https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html
   * <p>
   * <p>Additional guidance for configuring Elasticsearch for SSL can be found here - https://www.elastic.co/guide/en/x-pack/5.6/ssl-tls.html
   */
  private static SSLContext getSSLContext(ElasticsearchClientConfig esClientConfig) {
    if (esClientConfig.isSSLEnabled()) {
      LOG.info("Configuring client for SSL connection.");
      if (!esClientConfig.getKeyStorePath().isPresent()) {
        throw new IllegalStateException("KeyStore path must be provided for SSL connection.");
      }
      Optional<String> optKeyStorePass = esClientConfig.getKeyStorePassword();
      char[] keyStorePass = optKeyStorePass.map(String::toCharArray).orElse(null);
      KeyStore trustStore = getStore(esClientConfig.getKeyStoreType(),
          esClientConfig.getKeyStorePath().get(), keyStorePass);
      try {
        SSLContextBuilder sslBuilder = SSLContexts.custom().loadTrustMaterial(trustStore, null);
        return sslBuilder.build();
      } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
        throw new IllegalStateException("Unable to load truststore.", e);
      }
    }
    return null;
  }

  private static KeyStore getStore(String type, Path path, char[] pass) {
    KeyStore store;
    try {
      store = KeyStore.getInstance(type);
    } catch (KeyStoreException e) {
      throw new IllegalStateException("Unable to get keystore type '" + type + "'", e);
    }
    try (InputStream is = Files.newInputStream(path)) {
      store.load(is, pass);
    } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
      throw new IllegalStateException("Unable to load keystore from path '" + path + "'", e);
    }
    return store;
  }

}
