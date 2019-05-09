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

import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.junit.ProxyRule;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.run;
import static org.apache.metron.stellar.dsl.functions.RestConfig.BASIC_AUTH_PASSWORD_PATH;
import static org.apache.metron.stellar.dsl.functions.RestConfig.BASIC_AUTH_USER;
import static org.apache.metron.stellar.dsl.functions.RestConfig.CONNECTION_REQUEST_TIMEOUT;
import static org.apache.metron.stellar.dsl.functions.RestConfig.CONNECT_TIMEOUT;
import static org.apache.metron.stellar.dsl.functions.RestConfig.POOLING_DEFAULT_MAX_PER_RUOTE;
import static org.apache.metron.stellar.dsl.functions.RestConfig.POOLING_MAX_TOTAL;
import static org.apache.metron.stellar.dsl.functions.RestConfig.PROXY_BASIC_AUTH_PASSWORD_PATH;
import static org.apache.metron.stellar.dsl.functions.RestConfig.PROXY_BASIC_AUTH_USER;
import static org.apache.metron.stellar.dsl.functions.RestConfig.PROXY_HOST;
import static org.apache.metron.stellar.dsl.functions.RestConfig.PROXY_PORT;
import static org.apache.metron.stellar.dsl.functions.RestConfig.SOCKET_TIMEOUT;
import static org.apache.metron.stellar.dsl.functions.RestConfig.STELLAR_REST_SETTINGS;
import static org.apache.metron.stellar.dsl.functions.RestConfig.TIMEOUT;
import static org.apache.metron.stellar.dsl.functions.RestConfig.VERIFY_CONTENT_LENGTH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * Tests the RestFunctions class.
 */
public class RestFunctionsTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public MockServerRule mockServerRule = new MockServerRule(this);

  @Rule
  public ProxyRule proxyRule = new ProxyRule(1080, this);

  private MockServerClient mockServerClient;
  private String getUri;
  private Context context;

  private String basicAuthPasswordPath = "./target/basicAuth.txt";
  private String basicAuthPassword = "password";
  private String proxyBasicAuthPasswordPath = "./target/proxyBasicAuth.txt";
  private String proxyAuthPassword = "proxyPassword";

  @Before
  public void setup() throws Exception {
    context = new Context.Builder()
            .with(Context.Capabilities.GLOBAL_CONFIG, HashMap::new)
            .build();

    // Store the passwords in the local file system
    FileUtils.writeStringToFile(new File(basicAuthPasswordPath), basicAuthPassword, StandardCharsets.UTF_8);
    FileUtils.writeStringToFile(new File(proxyBasicAuthPasswordPath), proxyAuthPassword, StandardCharsets.UTF_8);

    // By default, the mock server expects a GET request with the path set to /get
    getUri = String.format("http://localhost:%d/get", mockServerRule.getPort());
    mockServerClient.when(
            request()
                    .withMethod("GET")
                    .withPath("/get"))
            .respond(response()
                    .withBody("{\"get\":\"success\"}"));
  }

  /**
   * The REST_GET function should perform a get request and parse the results.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void restGetShouldSucceed() throws Exception {
    Map<String, Object> actual = (Map<String, Object>) run(String.format("REST_GET('%s')", getUri), context);

    assertEquals(1, actual.size());
    assertEquals("success", actual.get("get"));
  }

  /**
   * The REST_GET function should perform a get request using a proxy and parse the results.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void restGetShouldSucceedWithProxy() {
    mockServerClient.when(
            request()
                    .withMethod("GET")
                    .withPath("/get"))
            .respond(response()
                    .withBody("{\"proxyGet\":\"success\"}"));

    context.addCapability(Context.Capabilities.GLOBAL_CONFIG, () -> new HashMap<String, Object>() {{
      put(PROXY_HOST, "localhost");
      put(PROXY_PORT, proxyRule.getHttpPort());
    }});

    Map<String, Object> actual = (Map<String, Object>) run(String.format("REST_GET('%s')", getUri), context);

    assertEquals(1, actual.size());
    assertEquals("success", actual.get("proxyGet"));
  }

  /**
   * The REST_GET function should handle an error status code and return null by default.
   */
  @Test
  public void restGetShouldHandleErrorStatusCode() {
      mockServerClient.when(
              request()
                      .withMethod("GET")
                      .withPath("/get"))
              .respond(response()
                      .withStatusCode(403));

      assertNull(run(String.format("REST_GET('%s')", getUri), context));
  }

  /**
   * {
   *   "response.codes.allowed": [200,404],
   *   "empty.content.override": {}
   * }
   */
  @Multiline
  private String emptyContentOverride;

  /**
   * The REST_GET function should return the empty content override setting when status is allowed and content is empty.
   */
  @Test
  public void restGetShouldReturnEmptyContentOverride() {
      mockServerClient.when(
              request()
                      .withMethod("GET")
                      .withPath("/get"))
              .respond(response()
                      .withStatusCode(404));

    assertEquals(new HashMap<>(), run(String.format("REST_GET('%s', %s)", getUri, emptyContentOverride), context));
  }

  /**
   * {
   *   "error.value.override": "error message"
   * }
   */
  @Multiline
  private String errorValueOverride;

  /**
   * The REST_GET function should return the error value override setting on error.
   */
  @Test
  public void restGetShouldReturnErrorValueOverride() {
    mockServerClient.when(
            request()
                    .withMethod("GET")
                    .withPath("/get"))
            .respond(response()
                    .withStatusCode(500));

    Object result = run(String.format("REST_GET('%s', %s)", getUri, errorValueOverride), context);
    assertEquals("error message" , result);
  }

  /**
   * The REST_GET function should return a proxy HttpHost if the correct settings are present.
   */
  @Test
  public void restGetShouldGetProxy() {
    RestFunctions.RestGet restGet = new RestFunctions.RestGet();

    {
      RestConfig restConfig = new RestConfig();
      Optional<HttpHost> actual = restGet.getProxy(restConfig);

      assertEquals(Optional.empty(), actual);
    }

    {
      RestConfig restConfig = new RestConfig();
      restConfig.put(PROXY_HOST, "localhost");
      Optional<HttpHost> actual = restGet.getProxy(restConfig);

      assertEquals(Optional.empty(), actual);
    }

    {
      RestConfig restConfig = new RestConfig();
      restConfig.put(PROXY_PORT, proxyRule.getHttpPort());
      Optional<HttpHost> actual = restGet.getProxy(restConfig);

      assertEquals(Optional.empty(), actual);
    }

    {
      RestConfig restConfig = new RestConfig();
      restConfig.put(PROXY_HOST, "localhost");
      restConfig.put(PROXY_PORT, proxyRule.getHttpPort());
      Optional<HttpHost> actual = restGet.getProxy(restConfig);

      assertEquals(new HttpHost("localhost", proxyRule.getHttpPort()), actual.get());
    }
  }

  /**
   * The REST_GET function should return settings in the correct order of precedence.
   * @throws Exception
   */
  @Test
  public void restGetShouldGetRestConfig() throws Exception {
    RestFunctions.RestGet restGet = new RestFunctions.RestGet();

    {
      // Test for default timeout
      RestConfig restConfig = restGet.getRestConfig(Collections.singletonList("uri"), new HashMap<>());

      assertEquals(3, restConfig.size());
      assertEquals(1000, restConfig.getTimeout().intValue());
      assertEquals(Collections.singletonList(200), restConfig.getResponseCodesAllowed());
      assertNull(restConfig.getBasicAuthUser());
    }

    Map<String, Object> globalRestConfig = new HashMap<String, Object>() {{
      put(STELLAR_REST_SETTINGS, new HashMap<String, Object>() {{
        put(SOCKET_TIMEOUT, 2000);
        put(BASIC_AUTH_USER, "globalUser");
        put(PROXY_HOST, "globalHost");
      }});
    }};

    // Global config settings should take effect
    {
      RestConfig restConfig = restGet.getRestConfig(Collections.singletonList("uri"), globalRestConfig);

      assertEquals(6, restConfig.size());
      assertEquals(1000, restConfig.getTimeout().intValue());
      assertEquals(Collections.singletonList(200), restConfig.getResponseCodesAllowed());
      assertEquals(2000, restConfig.getSocketTimeout().intValue());
      assertEquals("globalUser", restConfig.getBasicAuthUser());
      assertEquals("globalHost", restConfig.getProxyHost());
    }

    Map<String, Object> functionRestConfig = new HashMap<String, Object>() {{
      put(SOCKET_TIMEOUT, 1);
      put(BASIC_AUTH_USER, "functionUser");
      put(TIMEOUT, 100);
    }};


    // Function call settings should override global settings
    {
      RestConfig restConfig = restGet.getRestConfig(Arrays.asList("uri", functionRestConfig), globalRestConfig);

      assertEquals(6, restConfig.size());
      assertEquals(Collections.singletonList(200), restConfig.getResponseCodesAllowed());
      assertEquals(100, restConfig.getTimeout().intValue());
      assertEquals(1, restConfig.getSocketTimeout().intValue());
      assertEquals("functionUser", restConfig.getBasicAuthUser());
      assertEquals("globalHost", restConfig.getProxyHost());
    }

    functionRestConfig = new HashMap<String, Object>() {{
      put(BASIC_AUTH_USER, "functionUser");
      put(TIMEOUT, 100);
    }};

    // New function call settings should take effect with global settings staying the same
    {
      RestConfig restConfig = restGet.getRestConfig(Arrays.asList("uri", functionRestConfig), globalRestConfig);

      assertEquals(6, restConfig.size());
      assertEquals(Collections.singletonList(200), restConfig.getResponseCodesAllowed());
      assertEquals(100, restConfig.getTimeout().intValue());
      assertEquals(2000, restConfig.getSocketTimeout().intValue());
      assertEquals("functionUser", restConfig.getBasicAuthUser());
      assertEquals("globalHost", restConfig.getProxyHost());
    }

    globalRestConfig = new HashMap<String, Object>() {{
      put(STELLAR_REST_SETTINGS, new HashMap<String, Object>() {{
        put(SOCKET_TIMEOUT, 2000);
        put(BASIC_AUTH_USER, "globalUser");
      }});
    }};

    // New global settings should take effect with function call settings staying the same
    {
      RestConfig restConfig = restGet.getRestConfig(Arrays.asList("uri", functionRestConfig), globalRestConfig);

      assertEquals(5, restConfig.size());
      assertEquals(Collections.singletonList(200), restConfig.getResponseCodesAllowed());
      assertEquals(100, restConfig.getTimeout().intValue());
      assertEquals(2000, restConfig.getSocketTimeout().intValue());
      assertEquals("functionUser", restConfig.getBasicAuthUser());
    }

    // Should fall back to global settings on missing function call config
    {
      RestConfig restConfig = restGet.getRestConfig(Collections.singletonList("uri"), globalRestConfig);

      assertEquals(5, restConfig.size());
      assertEquals(Collections.singletonList(200), restConfig.getResponseCodesAllowed());
      assertEquals(1000, restConfig.getTimeout().intValue());
      assertEquals(2000, restConfig.getSocketTimeout().intValue());
      assertEquals("globalUser", restConfig.getBasicAuthUser());
    }

    // Should fall back to default settings on missing global settings
    {
      RestConfig restConfig = restGet.getRestConfig(Collections.singletonList("uri"), new HashMap<>());

      assertEquals(3, restConfig.size());
      assertEquals(Collections.singletonList(200), restConfig.getResponseCodesAllowed());
      assertEquals(1000, restConfig.getTimeout().intValue());
    }

  }

  /**
   * The REST_GET function should properly set the HttpClient timeout settings and proxy
   */
  @Test
  public void restGetShouldGetRequestConfig() {
    RestFunctions.RestGet restGet = new RestFunctions.RestGet();

    {
      RequestConfig actual = restGet.getRequestConfig(new RestConfig(), Optional.empty());
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
      HttpHost proxy = new HttpHost("localhost", proxyRule.getHttpPort());
      Optional<HttpHost> proxyOptional = Optional.of(proxy);

      RequestConfig actual = restGet.getRequestConfig(restConfig, proxyOptional);
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
    RestFunctions.RestGet restGet = new RestFunctions.RestGet();
    HttpHost target = new HttpHost("localhost", mockServerRule.getPort());
    HttpHost proxy = new HttpHost("localhost", proxyRule.getHttpPort());

    {
      RestConfig restConfig = new RestConfig();
      HttpClientContext actual = restGet.getHttpClientContext(restConfig, target, Optional.empty());

      assertNull(actual.getCredentialsProvider());
    }

    {
      RestConfig restConfig = new RestConfig();
      restConfig.put(BASIC_AUTH_USER, "user");
      restConfig.put(BASIC_AUTH_PASSWORD_PATH, basicAuthPasswordPath);

      HttpClientContext actual = restGet.getHttpClientContext(restConfig, target, Optional.empty());
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
      restConfig.put(PROXY_BASIC_AUTH_PASSWORD_PATH, proxyBasicAuthPasswordPath);

      HttpClientContext actual = restGet.getHttpClientContext(restConfig, target, Optional.of(proxy));
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
      restConfig.put(BASIC_AUTH_PASSWORD_PATH, basicAuthPasswordPath);
      restConfig.put(PROXY_BASIC_AUTH_USER, "proxyUser");
      restConfig.put(PROXY_BASIC_AUTH_PASSWORD_PATH, proxyBasicAuthPasswordPath);

      HttpClientContext actual = restGet.getHttpClientContext(restConfig, target, Optional.of(proxy));
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
   * The REST_GET function should timeout and return null.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void restGetShouldTimeout() {
    String uri = String.format("http://localhost:%d/get", mockServerRule.getPort());

    mockServerClient.when(
            request()
                    .withMethod("GET")
                    .withPath("/get"))
            .respond(response()
                    .withBody("{\"get\":\"success\"}"));

    Map<String, Object> globalConfig = new HashMap<String, Object>() {{
      put(STELLAR_REST_SETTINGS, new HashMap<String, Object>() {{
        put(TIMEOUT, 1);
      }});
    }};

    context.addCapability(Context.Capabilities.GLOBAL_CONFIG, () -> globalConfig);

    Map<String, Object> actual = (Map<String, Object>) run(String.format("REST_GET('%s')", uri), context);
    assertNull(actual);
  }

  /**
   * {
   * "timeout": 1
   * }
   */
  @Multiline
  private String timeoutConfig;

  /**
   * The REST_GET function should honor the function supplied timeout setting.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void restGetShouldTimeoutWithSuppliedTimeout() {
    String expression = String.format("REST_GET('%s', %s)", getUri, timeoutConfig);
    Map<String, Object> actual = (Map<String, Object>) run(expression, context);
    assertNull(actual);
  }

  /**
   * The REST_GET function should throw an exception on a malformed uri.
   * @throws IllegalArgumentException
   * @throws IOException
   */
  @Test
  public void restGetShouldHandleURISyntaxException() throws IllegalArgumentException, IOException {
    thrown.expect(ParseException.class);
    thrown.expectMessage("Unable to parse REST_GET('some invalid uri'): Unable to parse: REST_GET('some invalid uri') due to: Illegal character in path at index 4: some invalid uri");

    run("REST_GET('some invalid uri')", context);
  }

  /**
   * The REST_GET function should handle IOExceptions and return null.
   * @throws IllegalArgumentException
   * @throws IOException
   */
  @Test
  public void restGetShouldHandleIOException() throws IllegalArgumentException, IOException {
    RestFunctions.RestGet restGet = spy(RestFunctions.RestGet.class);
    doThrow(new IOException("io exception")).when(restGet).doGet(any(RestConfig.class), any(HttpGet.class), any(HttpClientContext.class));

    Object result = restGet.apply(Collections.singletonList(getUri), context);
    Assert.assertNull(result);
  }

  /**
   * The REST_GET function should throw an exception when the required uri parameter is missing.
   */
  @Test
  public void restGetShouldThrownExceptionOnMissingParameter() {
    thrown.expect(ParseException.class);
    thrown.expectMessage("Unable to parse REST_GET(): Unable to parse: REST_GET() due to: Expected at least 1 argument(s), found 0");

    run("REST_GET()", context);
  }

  @Test
  public void restGetShouldGetPoolingConnectionManager() {
    RestFunctions.RestGet restGet = new RestFunctions.RestGet();

    RestConfig restConfig = new RestConfig();
    restConfig.put(POOLING_MAX_TOTAL, 5);
    restConfig.put(POOLING_DEFAULT_MAX_PER_RUOTE, 2);

    PoolingHttpClientConnectionManager cm = restGet.getConnectionManager(restConfig);

    assertEquals(5, cm.getMaxTotal());
    assertEquals(2, cm.getDefaultMaxPerRoute());
  }

  @Test
  public void restGetShouldCloseHttpClient() throws Exception {
    RestFunctions.RestGet restGet = new RestFunctions.RestGet();
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);

    restGet.setHttpClient(httpClient);
    restGet.close();

    verify(httpClient, times(1)).close();
    verifyNoMoreInteractions(httpClient);
  }

  @Test
  public void restGetShouldParseResponse() throws Exception {
    RestFunctions.RestGet restGet = new RestFunctions.RestGet();
    RestConfig restConfig = new RestConfig();
    HttpGet httpGet = mock(HttpGet.class);
    HttpEntity httpEntity = mock(HttpEntity.class);

    // return successfully parsed response
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("{\"get\":\"success\"}".getBytes()));
    Optional<Object> actual = restGet.parseResponse(restConfig, httpGet, httpEntity);
    assertTrue(actual.isPresent());
    assertEquals("success", ((Map<String, Object>) actual.get()).get("get"));
  }

  @Test
  public void restGetShouldParseResponseOnNullHttpEntity() throws Exception {
    RestFunctions.RestGet restGet = new RestFunctions.RestGet();
    RestConfig restConfig = new RestConfig();
    HttpGet httpGet = mock(HttpGet.class);

    // return empty on null httpEntity
    assertEquals(Optional.empty(), restGet.parseResponse(restConfig, httpGet, null));
  }

  @Test
  public void restGetShouldParseResponseOnNullContent() throws Exception {
    RestFunctions.RestGet restGet = new RestFunctions.RestGet();
    RestConfig restConfig = new RestConfig();
    HttpGet httpGet = mock(HttpGet.class);
    HttpEntity httpEntity = mock(HttpEntity.class);

    // return empty on null content
    when(httpEntity.getContent()).thenReturn(null);
    assertEquals(Optional.empty(), restGet.parseResponse(restConfig, httpGet, httpEntity));
  }

  @Test
  public void restGetShouldParseResponseOnEmptyInputStream() throws Exception {
    RestFunctions.RestGet restGet = new RestFunctions.RestGet();
    RestConfig restConfig = new RestConfig();
    HttpGet httpGet = mock(HttpGet.class);
    HttpEntity httpEntity = mock(HttpEntity.class);

    // return empty on empty input stream
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("".getBytes()));
    assertEquals(Optional.empty(), restGet.parseResponse(restConfig, httpGet, httpEntity));
  }

  @Test
  public void restGetShouldThrowExceptionOnContentLengthMismatch() throws Exception {
    thrown.expect(IOException.class);
    thrown.expectMessage("Stellar REST request to uri returned incorrect or missing content length. Content length in the response was -1 but the actual body content length was 17.");

    RestFunctions.RestGet restGet = new RestFunctions.RestGet();
    RestConfig restConfig = new RestConfig();
    HttpGet httpGet = mock(HttpGet.class);
    HttpEntity httpEntity = mock(HttpEntity.class);

    restConfig.put(VERIFY_CONTENT_LENGTH, true);
    when(httpGet.getURI()).thenReturn(new URI("uri"));
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("{\"get\":\"success\"}".getBytes()));
    when(httpEntity.getContentLength()).thenReturn(-1L);
    restGet.parseResponse(restConfig, httpGet, httpEntity);
  }

}
