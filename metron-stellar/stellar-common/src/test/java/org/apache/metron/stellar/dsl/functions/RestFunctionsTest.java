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
package org.apache.metron.stellar.dsl.functions;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.metron.stellar.dsl.Context;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import static org.apache.metron.stellar.dsl.functions.RestConfig.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the RestFunctions class.
 */
@EnableRuleMigrationSupport
public class RestFunctionsTest {
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private Context context;

  private File basicAuthPasswordFile;
  private String basicAuthPassword = "password";
  private File proxyBasicAuthPasswordFile;
  private String proxyAuthPassword = "proxyPassword";

  @BeforeEach
  public void setup() throws Exception {
    context = new Context.Builder()
            .with(Context.Capabilities.GLOBAL_CONFIG, HashMap::new)
            .build();

    // Store the passwords in the local file system
    basicAuthPasswordFile = tempDir.newFile("basicAuth.txt");
    FileUtils.writeStringToFile(basicAuthPasswordFile, basicAuthPassword, StandardCharsets.UTF_8);
    proxyBasicAuthPasswordFile = tempDir.newFile("proxyBasicAuth.txt");
    FileUtils.writeStringToFile(proxyBasicAuthPasswordFile, proxyAuthPassword, StandardCharsets.UTF_8);
  }

  /**
   * The REST_GET function should return a proxy HttpHost if the correct settings are present.
   */
  @Test
  public void restGetShouldGetProxy() {
    {
      RestConfig restConfig = new RestConfig();
      Optional<HttpHost> actual = RestFunctions.getProxy(restConfig);

      assertEquals(Optional.empty(), actual);
    }

    {
      RestConfig restConfig = new RestConfig();
      restConfig.put(PROXY_HOST, "localhost");
      Optional<HttpHost> actual = RestFunctions.getProxy(restConfig);

      assertEquals(Optional.empty(), actual);
    }

    {
      RestConfig restConfig = new RestConfig();
      restConfig.put(PROXY_PORT, 3128);
      Optional<HttpHost> actual = RestFunctions.getProxy(restConfig);

      assertEquals(Optional.empty(), actual);
    }

    {
      RestConfig restConfig = new RestConfig();
      restConfig.put(PROXY_HOST, "localhost");
      restConfig.put(PROXY_PORT, 3128);
      Optional<HttpHost> actual = RestFunctions.getProxy(restConfig);

      assertEquals(new HttpHost("localhost", 3128), actual.get());
    }
  }

  /**
   * RestConfig should be built with settings in the correct order of precedence.
   */
  @Test
  public void restShouldBuildRestConfig() {
    Map<String, Object> config = new HashMap<String, Object>() {{
      put(BASIC_AUTH_USER, "user");
      put(PROXY_BASIC_AUTH_USER, "proxyUser");
    }};

    Map<String, Object> priorityConfig = new HashMap<String, Object>() {{
      put(BASIC_AUTH_USER, "priorityUser");
    }};

    RestConfig restConfig = RestFunctions.buildRestConfig(config, priorityConfig);
    assertEquals(6, restConfig.size());
    assertEquals(Collections.singletonList(200), restConfig.getResponseCodesAllowed());
    assertEquals("priorityUser", restConfig.getBasicAuthUser());
    assertEquals("proxyUser", restConfig.getProxyBasicAuthUser());
    assertTrue(restConfig.enforceJson());
    assertEquals(1000, restConfig.getTimeout().intValue());
    assertFalse(restConfig.verifyContentLength());
  }

  /**
   * The REST_GET function should properly set the HttpClient timeout settings and proxy
   */
  @Test
  public void restGetShouldGetRequestConfig() {
    {
      RequestConfig actual = RestFunctions.getRequestConfig(new RestConfig(), Optional.empty());
      RequestConfig expected = RequestConfig.custom().build();

      assertEquals(expected.getConnectTimeout(), actual.getConnectTimeout());
      assertEquals(expected.getConnectionRequestTimeout(), actual.getConnectionRequestTimeout());
      assertEquals(expected.getSocketTimeout(), actual.getSocketTimeout());
      assertEquals(expected.getProxy(), actual.getProxy());
    }

    {
      RestConfig restConfig = new RestConfig();
      restConfig.put(CONNECT_TIMEOUT, 1);
      restConfig.put(CONNECTION_REQUEST_TIMEOUT, 2);
      restConfig.put(SOCKET_TIMEOUT, 3);
      HttpHost proxy = new HttpHost("localhost", 3128);
      Optional<HttpHost> proxyOptional = Optional.of(proxy);

      RequestConfig actual = RestFunctions.getRequestConfig(restConfig, proxyOptional);
      RequestConfig expected = RequestConfig.custom()
              .setConnectTimeout(1)
              .setConnectionRequestTimeout(2)
              .setSocketTimeout(3)
              .setProxy(proxy)
              .build();

      assertEquals(expected.getConnectTimeout(), actual.getConnectTimeout());
      assertEquals(expected.getConnectionRequestTimeout(), actual.getConnectionRequestTimeout());
      assertEquals(expected.getSocketTimeout(), actual.getSocketTimeout());
      assertEquals(expected.getProxy(), actual.getProxy());
    }

  }

  /**
   * The REST_GET function should set the proper credentials in the HttpClientContext.
   * @throws Exception
   */
  @Test
  public void restGetShouldGetHttpClientContext() throws Exception {
    HttpHost target = new HttpHost("localhost", 8080);
    HttpHost proxy = new HttpHost("localhost", 3128);

    {
      RestConfig restConfig = new RestConfig();
      HttpClientContext actual = RestFunctions.getHttpClientContext(restConfig, target, Optional.empty());

      assertNull(actual.getCredentialsProvider());
    }

    {
      RestConfig restConfig = new RestConfig();
      restConfig.put(BASIC_AUTH_USER, "user");
      restConfig.put(BASIC_AUTH_PASSWORD_PATH, basicAuthPasswordFile.getAbsolutePath());

      HttpClientContext actual = RestFunctions.getHttpClientContext(restConfig, target, Optional.empty());
      HttpClientContext expected = HttpClientContext.create();
      CredentialsProvider expectedCredentialsProvider = new BasicCredentialsProvider();
      expectedCredentialsProvider.setCredentials(
              new AuthScope(target),
              new UsernamePasswordCredentials(restConfig.getBasicAuthUser(), basicAuthPassword));
      expected.setCredentialsProvider(expectedCredentialsProvider);

      assertEquals(expected.getCredentialsProvider().getCredentials(new AuthScope(target)),
              actual.getCredentialsProvider().getCredentials(new AuthScope(target)));
      assertEquals(expected.getCredentialsProvider().getCredentials(new AuthScope(proxy)),
              actual.getCredentialsProvider().getCredentials(new AuthScope(proxy)));
    }

    {
      RestConfig restConfig = new RestConfig();
      restConfig.put(PROXY_BASIC_AUTH_USER, "proxyUser");
      restConfig.put(PROXY_BASIC_AUTH_PASSWORD_PATH, proxyBasicAuthPasswordFile.getAbsolutePath());

      HttpClientContext actual = RestFunctions.getHttpClientContext(restConfig, target, Optional.of(proxy));
      HttpClientContext expected = HttpClientContext.create();
      CredentialsProvider expectedCredentialsProvider = new BasicCredentialsProvider();
      expectedCredentialsProvider.setCredentials(
              new AuthScope(proxy),
              new UsernamePasswordCredentials(restConfig.getProxyBasicAuthUser(), proxyAuthPassword));
      expected.setCredentialsProvider(expectedCredentialsProvider);

      assertEquals(expected.getCredentialsProvider().getCredentials(new AuthScope(target)),
              actual.getCredentialsProvider().getCredentials(new AuthScope(target)));
      assertEquals(expected.getCredentialsProvider().getCredentials(new AuthScope(proxy)),
              actual.getCredentialsProvider().getCredentials(new AuthScope(proxy)));
    }

    {
      RestConfig restConfig = new RestConfig();
      restConfig.put(BASIC_AUTH_USER, "user");
      restConfig.put(BASIC_AUTH_PASSWORD_PATH, basicAuthPasswordFile.getAbsolutePath());
      restConfig.put(PROXY_BASIC_AUTH_USER, "proxyUser");
      restConfig.put(PROXY_BASIC_AUTH_PASSWORD_PATH, proxyBasicAuthPasswordFile.getAbsolutePath());

      HttpClientContext actual = RestFunctions.getHttpClientContext(restConfig, target, Optional.of(proxy));
      HttpClientContext expected = HttpClientContext.create();
      CredentialsProvider expectedCredentialsProvider = new BasicCredentialsProvider();
      expectedCredentialsProvider.setCredentials(
              new AuthScope(target),
              new UsernamePasswordCredentials(restConfig.getBasicAuthUser(), basicAuthPassword));
      expectedCredentialsProvider.setCredentials(
              new AuthScope(proxy),
              new UsernamePasswordCredentials(restConfig.getProxyBasicAuthUser(), proxyAuthPassword));
      expected.setCredentialsProvider(expectedCredentialsProvider);

      assertEquals(expected.getCredentialsProvider().getCredentials(new AuthScope(target)),
              actual.getCredentialsProvider().getCredentials(new AuthScope(target)));
      assertEquals(expected.getCredentialsProvider().getCredentials(new AuthScope(proxy)),
              actual.getCredentialsProvider().getCredentials(new AuthScope(proxy)));
    }
  }

  /**
   * The REST_GET function should handle IOExceptions and return null.
   * @throws IOException
   */
  @Test
  public void restGetShouldHandleIOException() throws IOException {
    RestFunctions.RestGet restGet = new RestFunctions.RestGet();
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    ScheduledExecutorService executorService = mock(ScheduledExecutorService.class);

    RestFunctions.setCloseableHttpClient(httpClient);
    RestFunctions.setScheduledExecutorService(executorService);

    when(httpClient.execute(any(HttpRequestBase.class), any(HttpClientContext.class))).thenThrow(new IOException("io exception"));

    Object result = restGet.apply(Collections.singletonList("http://www.host.com:8080/some/uri"), context);
    assertNull(result);
  }

  @Test
  public void restGetShouldGetPoolingConnectionManager() {
    RestConfig restConfig = new RestConfig();
    restConfig.put(POOLING_MAX_TOTAL, 5);
    restConfig.put(POOLING_DEFAULT_MAX_PER_RUOTE, 2);

    PoolingHttpClientConnectionManager cm = RestFunctions.getConnectionManager(restConfig);

    assertEquals(5, cm.getMaxTotal());
    assertEquals(2, cm.getDefaultMaxPerRoute());
  }

  @Test
  public void restGetShouldClose() throws Exception {
    RestFunctions.RestGet restGet = new RestFunctions.RestGet();
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    ScheduledExecutorService executorService = mock(ScheduledExecutorService.class);

    RestFunctions.setCloseableHttpClient(httpClient);
    RestFunctions.setScheduledExecutorService(executorService);
    restGet.close();

    verify(httpClient, times(1)).close();
    verify(executorService, times(1)).shutdown();
    verifyNoMoreInteractions(httpClient);
  }

  @Test
  public void restPostShouldClose() throws Exception {
    RestFunctions.RestPost restPost = new RestFunctions.RestPost();
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    ScheduledExecutorService executorService = mock(ScheduledExecutorService.class);

    RestFunctions.setCloseableHttpClient(httpClient);
    RestFunctions.setScheduledExecutorService(executorService);
    restPost.close();

    verify(httpClient, times(1)).close();
    verify(executorService, times(1)).shutdown();
    verifyNoMoreInteractions(httpClient);
  }

  @Test
  public void restGetShouldParseResponse() throws Exception {
    RestConfig restConfig = new RestConfig();
    HttpGet httpGet = mock(HttpGet.class);
    HttpEntity httpEntity = mock(HttpEntity.class);

    // return successfully parsed response
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("{\"get\":\"success\"}".getBytes(
            StandardCharsets.UTF_8
    )));
    Optional<Object> actual = RestFunctions.parseResponse(restConfig, httpGet, httpEntity);
    assertTrue(actual.isPresent());
    assertEquals("success", ((Map<String, Object>) actual.get()).get("get"));
  }

  @Test
  public void restGetShouldParseResponseOnNullHttpEntity() throws Exception {
    RestConfig restConfig = new RestConfig();
    HttpGet httpGet = mock(HttpGet.class);

    // return empty on null httpEntity
    assertEquals(Optional.empty(), RestFunctions.parseResponse(restConfig, httpGet, null));
  }

  @Test
  public void restGetShouldParseResponseOnNullContent() throws Exception {
    RestConfig restConfig = new RestConfig();
    HttpGet httpGet = mock(HttpGet.class);
    HttpEntity httpEntity = mock(HttpEntity.class);

    // return empty on null content
    when(httpEntity.getContent()).thenReturn(null);
    assertEquals(Optional.empty(), RestFunctions.parseResponse(restConfig, httpGet, httpEntity));
  }

  @Test
  public void restGetShouldParseResponseOnEmptyInputStream() throws Exception {
    RestConfig restConfig = new RestConfig();
    HttpGet httpGet = mock(HttpGet.class);
    HttpEntity httpEntity = mock(HttpEntity.class);

    // return empty on empty input stream
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
    assertEquals(Optional.empty(), RestFunctions.parseResponse(restConfig, httpGet, httpEntity));
  }

  @Test
  public void restGetShouldThrowExceptionOnContentLengthMismatch() throws Exception {
    RestFunctions.RestGet restGet = new RestFunctions.RestGet();
    RestConfig restConfig = new RestConfig();
    HttpGet httpGet = mock(HttpGet.class);
    HttpEntity httpEntity = mock(HttpEntity.class);

    restConfig.put(VERIFY_CONTENT_LENGTH, true);
    when(httpGet.getURI()).thenReturn(new URI("uri"));
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("{\"get\":\"success\"}".getBytes(
        StandardCharsets.UTF_8)));
    when(httpEntity.getContentLength()).thenReturn(-1L);
    IOException e = assertThrows(IOException.class, () -> RestFunctions.parseResponse(restConfig, httpGet, httpEntity));
    assertEquals(
        "Stellar REST request to uri returned incorrect or missing content length. Content length in the response was -1 but the actual body content length was 17.",
        e.getMessage());
  }
}
