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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.common.utils.JSONUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.apache.metron.stellar.dsl.Context.Capabilities.GLOBAL_CONFIG;
import static org.apache.metron.stellar.dsl.functions.RestConfig.*;

/**
 * Defines functions that enable REST requests with proper result and error handling.  Depends on an
 * Apache HttpComponents client being supplied as a Stellar HTTP_CLIENT capability.  Exposes various Http settings
 * including authentication, proxy and timeouts through the global config with the option to override any settings
 * through a config object supplied in the expression.
 */
public class RestFunctions {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * The CloseableHttpClient.
   */
  private static CloseableHttpClient closeableHttpClient;

  /**
   * Executor used to impose a hard request timeout.
   */
  private static ScheduledExecutorService scheduledExecutorService;

  /**
   * Initialize a single HttpClient to be shared by REST functions.
   * @param context
   */
  private static synchronized void initializeHttpClient(Context context) {
    if (closeableHttpClient == null) {
      closeableHttpClient = getHttpClient(context);
    }
  }

  /**
   * Close the shared HttpClient.
   */
  private static synchronized void closeHttpClient() throws IOException {
    if (closeableHttpClient != null) {
      closeableHttpClient.close();
      closeableHttpClient = null;
    }
  }

  /**
   * Initialize a single ExecutorService to be shared by REST functions.
   */
  private static synchronized void initializeExecutorService() {
    if (scheduledExecutorService == null) {
      scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }
  }

  /**
   * Shutdown the shared ExecutorService.
   */
  private static synchronized void closeExecutorService() {
    if (scheduledExecutorService != null) {
      scheduledExecutorService.shutdown();
      scheduledExecutorService = null;
    }
  }

  @Stellar(
          namespace = "REST",
          name = "GET",
          description = "Performs a REST GET request and parses the JSON results into a map.",
          params = {
                  "url - URI to the REST service",
                  "rest_config - Optional - Map (in curly braces) of name:value pairs, each overriding the global config parameter " +
                          "of the same name. Default is the empty Map, meaning no overrides.",
                  "query_parameters - Optional - Map (in curly braces) of name:value pairs that will be added to the request as query parameters"
          },
          returns = "JSON results as a Map")
  public static class RestGet implements StellarFunction {

    /**
     * Whether the function has been initialized.
     */
    private boolean initialized = false;

    /**
     * Initialize the function by creating a ScheduledExecutorService and looking up the CloseableHttpClient from the
     * Stellar context.
     * @param context
     */
    @Override
    public void initialize(Context context) {
      initializeExecutorService();
      initializeHttpClient(context);
      initialized = true;
    }

    @Override
    public boolean isInitialized() {
      return initialized;
    }

    /**
     * Apply the function.
     * @param args The function arguments including uri and rest config.
     * @param context Stellar context
     */
    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      String uriString = getArg(0, String.class, args);
      Map<String, Object> functionRestConfig = null;
      Map<String, Object> queryParameters = new HashMap<>();
      if (args.size() > 1) {
        functionRestConfig = getArg(1, Map.class, args);
        if (args.size() == 3) {
          queryParameters = getArg(2, Map.class, args);
        }
      }

      // Build the RestConfig by applying settins in order of precedence
      Map<String, Object> globalRestConfig = (Map<String, Object>) getGlobalConfig(context).get(STELLAR_REST_SETTINGS);
      Map<String, Object> getRestConfig = (Map<String, Object>) getGlobalConfig(context).get(STELLAR_REST_GET_SETTINGS);
      RestConfig restConfig = buildRestConfig(globalRestConfig, getRestConfig, functionRestConfig);

      try {
        HttpGet httpGet = buildGetRequest(uriString, queryParameters);
        return executeRequest(restConfig, httpGet);
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException(e.getMessage(), e);
      } catch (IOException e) {
        LOG.error(e.getMessage(), e);
        return restConfig.getErrorValueOverride();
      }
    }

    @Override
    public void close() throws IOException {
      closeHttpClient();
      closeExecutorService();
    }

    private HttpGet buildGetRequest(String uri, Map<String, Object> queryParameters) throws URISyntaxException {
      HttpGet httpGet = new HttpGet(getURI(uri, queryParameters));
      httpGet.addHeader("Accept", "application/json");

      return httpGet;
    }
  }

  @Stellar(
          namespace = "REST",
          name = "POST",
          description = "Performs a REST POST request and parses the JSON results into a map.",
          params = {
                  "url - URI to the REST service",
                  "post_data - POST data that will be sent in the POST request.  Must be well-formed JSON unless the 'enforce.json' property is set to false.",
                  "rest_config - Optional - Map (in curly braces) of name:value pairs, each overriding the global config parameter " +
                          "of the same name. Default is the empty Map, meaning no overrides.",
                  "query_parameters - Optional - Map (in curly braces) of name:value pairs that will be added to the request as query parameters"

          },
          returns = "JSON results as a Map")
  public static class RestPost implements StellarFunction {

    /**
     * Whether the function has been initialized.
     */
    private boolean initialized = false;

    /**
     * Initialize the function by creating a ScheduledExecutorService and looking up the CloseableHttpClient from the
     * Stellar context.
     * @param context
     */
    @Override
    public void initialize(Context context) {
      initializeExecutorService();
      initializeHttpClient(context);
      initialized = true;
    }

    @Override
    public boolean isInitialized() {
      return initialized;
    }

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      String uriString = getArg(0, String.class, args);
      Object dataObject = getArg(1, Object.class, args);
      Map<String, Object> functionRestConfig = null;
      Map<String, Object> queryParameters = new HashMap<>();
      if (args.size() > 2) {
        functionRestConfig = getArg(2, Map.class, args);
        if (args.size() == 4) {
          queryParameters = getArg(3, Map.class, args);
        }
      }

      // Build the RestConfig by applying settins in order of precedence
      Map<String, Object> globalRestConfig = (Map<String, Object>) getGlobalConfig(context).get(STELLAR_REST_SETTINGS);
      Map<String, Object> postRestConfig = (Map<String, Object>) getGlobalConfig(context).get(STELLAR_REST_POST_SETTINGS);
      RestConfig restConfig = buildRestConfig(globalRestConfig, postRestConfig, functionRestConfig);

      try {
        HttpPost httpPost = buildPostRequest(restConfig, uriString, dataObject, queryParameters);
        return executeRequest(restConfig, httpPost);
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException(e.getMessage(), e);
      } catch (IOException e) {
        LOG.error(e.getMessage(), e);
        return restConfig.getErrorValueOverride();
      }
    }

    @Override
    public void close() throws IOException {
      closeHttpClient();
      closeExecutorService();
    }

    private HttpPost buildPostRequest(RestConfig restConfig, String uriString, Object dataObject, Map<String, Object> queryParameters) throws JsonProcessingException, URISyntaxException, UnsupportedEncodingException {
      String body = getPostData(restConfig, dataObject);

      URI uri = getURI(uriString, queryParameters);
      HttpPost httpPost = new HttpPost(uri);
      httpPost.setEntity(new StringEntity(body));
      httpPost.addHeader("Accept", "application/json");
      httpPost.addHeader("Content-type", "application/json");

      return httpPost;
    }

    /**
     * Serializes the supplied POST data to be sent in the POST request.  Checks for well-formed JSON by default unless 'enforce.json' is set to false.
     * @param restConfig RestConfig
     * @param arg POST data
     * @return Serialized POST data
     * @throws JsonProcessingException
     */
    private String getPostData(RestConfig restConfig, Object arg) throws JsonProcessingException {
      String data = "";
      if (arg == null) {
        return data;
      }
      if (arg instanceof Map) {
        data = JSONUtils.INSTANCE.toJSON(arg, false);
      } else {
        data = arg.toString();
        if (restConfig.enforceJson()) {
          try {
            JSONUtils.INSTANCE.toJSONObject(data);
          } catch (org.json.simple.parser.ParseException e) {
            throw new IllegalArgumentException(String.format("POST data '%s' must be properly formatted JSON.  " +
                    "Set the '%s' property to false to disable this check.", data, RestConfig.ENFORCE_JSON));
          }
        }
      }
      return data;
    }
  }

  /**
   * Get an argument from a list of arguments.
   *
   * @param index The index within the list of arguments.
   * @param clazz The type expected.
   * @param args All of the arguments.
   * @param <T> The type of the argument expected.
   */
  public static <T> T getArg(int index, Class<T> clazz, List<Object> args) {

    if(index >= args.size()) {
      throw new IllegalArgumentException(format("Expected at least %d argument(s), found %d", index+1, args.size()));
    }

    return ConversionUtils.convert(args.get(index), clazz);
  }

  /**
   * Retrieves the ClosableHttpClient from a pooling connection manager.
   *
   * @param context The execution context.
   * @return A ClosableHttpClient.
   */
  protected static CloseableHttpClient getHttpClient(Context context) {
    RestConfig restConfig = buildRestConfig(getGlobalConfig(context));

    PoolingHttpClientConnectionManager cm = getConnectionManager(restConfig);

    return HttpClients.custom()
            .setConnectionManager(cm)
            .build();
  }

  protected static PoolingHttpClientConnectionManager getConnectionManager(RestConfig restConfig) {
    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    if (restConfig.containsKey(POOLING_MAX_TOTAL)) {
      cm.setMaxTotal(restConfig.getPoolingMaxTotal());
    }
    if (restConfig.containsKey(POOLING_DEFAULT_MAX_PER_RUOTE)) {
      cm.setDefaultMaxPerRoute(restConfig.getPoolingDefaultMaxPerRoute());
    }
    return cm;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> getGlobalConfig(Context context) {
    Optional<Object> globalCapability = context.getCapability(GLOBAL_CONFIG, false);
    return globalCapability.map(o -> (Map<String, Object>) o).orElseGet(HashMap::new);
  }

  /**
   * Build the RestConfig by applying settings in order of precedence (last item in the input list has highest priority).
   * Only settings specified in the rest config will override lower priority config settings.
   * @param configs
   * @return
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  protected static RestConfig buildRestConfig(Map<String, Object>... configs) {
    RestConfig restConfig = new RestConfig();

    // Add settings in order of precedence
    for(Map<String, Object> config: configs) {
      if (config != null) {
        restConfig.putAll(config);
      }
    }
    return restConfig;
  }

  /**
   * Builds a URI from the supplied URI string and adds query parameters.
   * @param uriString
   * @param queryParameters
   * @return
   * @throws URISyntaxException
   */
  private static URI getURI(String uriString, Map<String, Object> queryParameters) throws URISyntaxException {
    URIBuilder uriBuilder = new URIBuilder(uriString);
    if (queryParameters != null) {
      for(Map.Entry<String, Object> entry: queryParameters.entrySet()) {
        uriBuilder.setParameter(entry.getKey(), (String) entry.getValue());
      }
    }
    return uriBuilder.build();
  }

  /**
   * Returns the proxy HttpHost object if the proxy rest config settings are set.
   * @param restConfig
   * @return
   */
  protected static Optional<HttpHost> getProxy(RestConfig restConfig) {
    Optional<HttpHost> proxy = Optional.empty();
    if (restConfig.getProxyHost() != null && restConfig.getProxyPort() != null) {
      proxy = Optional.of(new HttpHost(restConfig.getProxyHost(), restConfig.getProxyPort(), "http"));
    }
    return proxy;
  }

  /**
   * Builds the RequestConfig object by setting HttpClient settings defined in the rest config.
   * @param restConfig
   * @param proxy
   * @return
   */
  protected static RequestConfig getRequestConfig(RestConfig restConfig, Optional<HttpHost> proxy) {
    RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
    if (restConfig.getConnectTimeout() != null) {
      requestConfigBuilder.setConnectTimeout(restConfig.getConnectTimeout());
    }
    if (restConfig.getConnectionRequestTimeout() != null) {
      requestConfigBuilder.setConnectionRequestTimeout(restConfig.getConnectionRequestTimeout());
    }
    if (restConfig.getSocketTimeout() != null) {
      requestConfigBuilder.setSocketTimeout(restConfig.getSocketTimeout());
    }

    proxy.ifPresent(requestConfigBuilder::setProxy);
    return requestConfigBuilder.build();
  }

  /**
   * Builds the HttpClientContext object by setting the basic auth and/or proxy basic auth credentials when the
   * necessary rest config settings are configured.  Passwords are stored in HDFS.
   * @param restConfig
   * @param target
   * @param proxy
   * @return
   * @throws IOException
   */
  protected static HttpClientContext getHttpClientContext(RestConfig restConfig, HttpHost target, Optional<HttpHost> proxy) throws IOException {
    HttpClientContext httpClientContext = HttpClientContext.create();
    boolean credentialsAdded = false;
    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

    // Add the basic auth credentials if the rest config settings are present
    if (restConfig.getBasicAuthUser() != null && restConfig.getBasicAuthPasswordPath() != null) {
      String password = new String(readBytes(new Path(restConfig.getBasicAuthPasswordPath())), StandardCharsets.UTF_8);
      credentialsProvider.setCredentials(
              new AuthScope(target),
              new UsernamePasswordCredentials(restConfig.getBasicAuthUser(), password));
      credentialsAdded = true;
    }

    // Add the proxy basic auth credentials if the rest config settings are present
    if (proxy.isPresent() && restConfig.getProxyBasicAuthUser() != null &&
            restConfig.getProxyBasicAuthPasswordPath() != null) {
      String password = new String(readBytes(new Path(restConfig.getProxyBasicAuthPasswordPath())), StandardCharsets.UTF_8);
      credentialsProvider.setCredentials(
              new AuthScope(proxy.get()),
              new UsernamePasswordCredentials(restConfig.getProxyBasicAuthUser(), password));
      credentialsAdded = true;
    }
    if (credentialsAdded) {
      httpClientContext.setCredentialsProvider(credentialsProvider);
    }
    return httpClientContext;
  }

  /**
   * Read bytes from a HDFS path.
   * @param inPath
   * @return
   * @throws IOException
   */
  private static byte[] readBytes(Path inPath) throws IOException {
    FileSystem fs = FileSystem.get(inPath.toUri(), new Configuration());
    try (FSDataInputStream inputStream = fs.open(inPath)) {
      return IOUtils.toByteArray(inputStream);
    }
  }

  /**
   * Perform the HttpClient request and handle the results.  A configurable list of status codes are accepted and the
   * response content (expected to be json) is parsed into a Map.  Values returned on errors and when response content
   * is also configurable.  The rest config "timeout" setting is imposed in this method and will abort the get request
   * if exceeded.
   *
   * @param restConfig
   * @param httpRequestBase
   * @return
   * @throws IOException
   */
  protected static Object executeRequest(RestConfig restConfig, HttpRequestBase httpRequestBase) throws IOException {
    URI uri = httpRequestBase.getURI();
    HttpHost target = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
    Optional<HttpHost> proxy = getProxy(restConfig);
    HttpClientContext httpClientContext = getHttpClientContext(restConfig, target, proxy);
    httpRequestBase.setConfig(getRequestConfig(restConfig, proxy));

    // Schedule a command to abort the request if the timeout is exceeded
    ScheduledFuture scheduledFuture = scheduledExecutorService.schedule(httpRequestBase::abort, restConfig.getTimeout(), TimeUnit.MILLISECONDS);
    CloseableHttpResponse response;
    try {
      response = closeableHttpClient.execute(httpRequestBase, httpClientContext);
    } catch(Exception e) {
      // Report a timeout if the httpGet request was aborted.  Otherwise rethrow exception.
      if (httpRequestBase.isAborted()) {
        throw new IOException(String.format("Total Stellar REST request time to %s exceeded the configured timeout of %d ms.", httpRequestBase.getURI().toString(), restConfig.getTimeout()));
      } else {
        throw e;
      }
    }

    // Cancel the future if the request finished within the timeout
    if (!scheduledFuture.isDone()) {
      scheduledFuture.cancel(true);
    }
    int statusCode = response.getStatusLine().getStatusCode();
    LOG.debug("request = {}; response = {}", httpRequestBase, response);
    if (restConfig.getResponseCodesAllowed().contains(statusCode)) {
      HttpEntity httpEntity = response.getEntity();

      // Parse the response if present, return the empty value override if not
      Optional<Object> parsedResponse = parseResponse(restConfig, httpRequestBase, httpEntity);
      return parsedResponse.orElseGet(restConfig::getEmptyContentOverride);
    } else {
      throw new IOException(String.format("Stellar REST request to %s expected status code to be one of %s but " +
                      "failed with http status code %d: %s",
              httpRequestBase.getURI().toString(),
              restConfig.getResponseCodesAllowed().toString(),
              statusCode,
              EntityUtils.toString(response.getEntity())));
    }
  }

  /**
   * Parses the Http response into a Map and checks for content length.
   * @param restConfig
   * @param httpUriRequest
   * @param httpEntity
   * @return
   * @throws IOException
   */
  protected static Optional<Object> parseResponse(RestConfig restConfig, HttpUriRequest httpUriRequest, HttpEntity httpEntity) throws IOException {
    Optional<Object> parsedResponse = Optional.empty();
    if (httpEntity != null) {
      int actualContentLength = 0;
      String json = EntityUtils.toString(httpEntity);
      if (json != null && !json.isEmpty()) {
        actualContentLength = json.length();
        parsedResponse = Optional.of(JSONUtils.INSTANCE.load(json, JSONUtils.MAP_SUPPLIER));
      }
      if (restConfig.verifyContentLength() && actualContentLength != httpEntity.getContentLength()) {
        throw new IOException(String.format("Stellar REST request to %s returned incorrect or missing content length. " +
                        "Content length in the response was %d but the actual body content length was %d.",
                httpUriRequest.getURI().toString(),
                httpEntity.getContentLength(),
                actualContentLength));
      }
    }
    return parsedResponse;
  }

  /**
   * Only used for testing.
   * @param httpClient
   */
  protected static void setCloseableHttpClient(CloseableHttpClient httpClient) {
    closeableHttpClient = httpClient;
  }

  /**
   * Only used for testing.
   * @param executorService
   */
  protected static void setScheduledExecutorService(ScheduledExecutorService executorService) {
    scheduledExecutorService = executorService;
  }
}
