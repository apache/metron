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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
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
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
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
import static org.apache.metron.stellar.dsl.functions.RestConfig.POOLING_DEFAULT_MAX_PER_RUOTE;
import static org.apache.metron.stellar.dsl.functions.RestConfig.POOLING_MAX_TOTAL;
import static org.apache.metron.stellar.dsl.functions.RestConfig.STELLAR_REST_SETTINGS;

/**
 * Defines functions that enable REST requests with proper result and error handling.  Depends on an
 * Apache HttpComponents client being supplied as a Stellar HTTP_CLIENT capability.  Exposes various Http settings
 * including authentication, proxy and timeouts through the global config with the option to override any settings
 * through a config object supplied in the expression.
 */
public class RestFunctions {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

  @Stellar(
          namespace = "REST",
          name = "GET",
          description = "Performs a REST GET request and parses the JSON results into a map.",
          params = {
                  "url - URI to the REST service",
                  "rest_config - Optional - Map (in curly braces) of name:value pairs, each overriding the global config parameter " +
                          "of the same name. Default is the empty Map, meaning no overrides."
          },
          returns = "JSON results as a Map")
  public static class RestGet implements StellarFunction {

    /**
     * Whether the function has been initialized.
     */
    private boolean initialized = false;

    /**
     * The CloseableHttpClient.
     */
    private CloseableHttpClient httpClient;

    /**
     * Executor used to impose a hard request timeout.
     */
    private ScheduledExecutorService scheduledExecutorService;

    /**
     * Initialize the function by creating a ScheduledExecutorService and looking up the CloseableHttpClient from the
     * Stellar context.
     * @param context
     */
    @Override
    public void initialize(Context context) {
      scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
      httpClient = getHttpClient(context);
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
      RestConfig restConfig = new RestConfig();
      try {
        URI uri = new URI(getArg(0, String.class, args));
        restConfig = getRestConfig(args, getGlobalConfig(context));

        HttpHost target = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
        Optional<HttpHost> proxy = getProxy(restConfig);
        HttpClientContext httpClientContext = getHttpClientContext(restConfig, target, proxy);

        HttpGet httpGet = new HttpGet(uri);
        httpGet.addHeader("Accept", "application/json");
        httpGet.setConfig(getRequestConfig(restConfig, proxy));

        return doGet(restConfig, httpGet, httpClientContext);
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException(e.getMessage(), e);
      } catch (IOException e) {
        LOG.error(e.getMessage(), e);
        return restConfig.getErrorValueOverride();
      }
    }

    @Override
    public void close() throws IOException {
      if (httpClient != null) {
        httpClient.close();
      }
      if (scheduledExecutorService != null) {
        scheduledExecutorService.shutdown();
      }
    }

    /**
     * Retrieves the ClosableHttpClient from a pooling connection manager.
     *
     * @param context The execution context.
     * @return A ClosableHttpClient.
     */
    protected CloseableHttpClient getHttpClient(Context context) {
      RestConfig restConfig = getRestConfig(Collections.emptyList(), getGlobalConfig(context));

      PoolingHttpClientConnectionManager cm = getConnectionManager(restConfig);

      return HttpClients.custom()
              .setConnectionManager(cm)
              .build();
    }

    protected PoolingHttpClientConnectionManager getConnectionManager(RestConfig restConfig) {
      PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
      if (restConfig.containsKey(POOLING_MAX_TOTAL)) {
        cm.setMaxTotal(restConfig.getPoolingMaxTotal());
      }
      if (restConfig.containsKey(POOLING_DEFAULT_MAX_PER_RUOTE)) {
        cm.setDefaultMaxPerRoute(restConfig.getPoolingDefaultMaxPerRoute());
      }
      return cm;
    }

    /**
     * Only used for testing.
     * @param httpClient
     */
    protected void setHttpClient(CloseableHttpClient httpClient) {
      this.httpClient = httpClient;
    }

    /**
     * Perform the HttpClient get and handle the results.  A configurable list of status codes are accepted and the
     * response content (expected to be json) is parsed into a Map.  Values returned on errors and when response content
     * is also configurable.  The rest config "timeout" setting is imposed in this method and will abort the get request
     * if exceeded.
     *
     * @param restConfig
     * @param httpGet
     * @param httpClientContext
     * @return
     * @throws IOException
     */
    protected Object doGet(RestConfig restConfig, HttpGet httpGet, HttpClientContext httpClientContext) throws IOException {

      // Schedule a command to abort the httpGet request if the timeout is exceeded
      ScheduledFuture scheduledFuture = scheduledExecutorService.schedule(httpGet::abort, restConfig.getTimeout(), TimeUnit.MILLISECONDS);
      CloseableHttpResponse response;
      try {
        response = httpClient.execute(httpGet, httpClientContext);
      } catch(Exception e) {
        // Report a timeout if the httpGet request was aborted.  Otherwise rethrow exception.
        if (httpGet.isAborted()) {
          throw new IOException(String.format("Total Stellar REST request time to %s exceeded the configured timeout of %d ms.", httpGet.getURI().toString(), restConfig.getTimeout()));
        } else {
          throw e;
        }
      }

      // Cancel the future if the request finished within the timeout
      if (!scheduledFuture.isDone()) {
        scheduledFuture.cancel(true);
      }
      int statusCode = response.getStatusLine().getStatusCode();
      LOG.debug("request = {}; response = {}", httpGet, response);
      if (restConfig.getResponseCodesAllowed().contains(statusCode)) {
        HttpEntity httpEntity = response.getEntity();

        // Parse the response if present, return the empty value override if not
        Optional<Object> parsedResponse = parseResponse(restConfig, httpGet, httpEntity);
        return parsedResponse.orElseGet(restConfig::getEmptyContentOverride);
      } else {
        throw new IOException(String.format("Stellar REST request to %s expected status code to be one of %s but " +
                "failed with http status code %d: %s",
                httpGet.getURI().toString(),
                restConfig.getResponseCodesAllowed().toString(),
                statusCode,
                EntityUtils.toString(response.getEntity())));
      }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getGlobalConfig(Context context) {
      Optional<Object> globalCapability = context.getCapability(GLOBAL_CONFIG, false);
      return globalCapability.map(o -> (Map<String, Object>) o).orElseGet(HashMap::new);
    }

    /**
     * Build the RestConfig object using the following order of precedence:
     * <ul>
     *   <li>rest config supplied as an expression parameter</li>
     *   <li>rest config stored in the global config</li>
     *   <li>default rest config</li>
     * </ul>
     * Only settings specified in the rest config will override lower priority config settings.
     * @param args
     * @param globalConfig
     * @return
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    protected RestConfig getRestConfig(List<Object> args, Map<String, Object> globalConfig) {
      Map<String, Object> globalRestConfig = (Map<String, Object>) globalConfig.get(STELLAR_REST_SETTINGS);
      Map<String, Object> functionRestConfig = null;
      if (args.size() > 1) {
        functionRestConfig = getArg(1, Map.class, args);
      }

      // Add settings in order of precedence
      RestConfig restConfig = new RestConfig();
      if (globalRestConfig != null) {
        restConfig.putAll(globalRestConfig);
      }
      if (functionRestConfig != null) {
        restConfig.putAll(functionRestConfig);
      }
      return restConfig;
    }

    /**
     * Returns the proxy HttpHost object if the proxy rest config settings are set.
     * @param restConfig
     * @return
     */
    protected Optional<HttpHost> getProxy(RestConfig restConfig) {
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
    protected RequestConfig getRequestConfig(RestConfig restConfig, Optional<HttpHost> proxy) {
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
    protected HttpClientContext getHttpClientContext(RestConfig restConfig, HttpHost target, Optional<HttpHost> proxy) throws IOException {
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

    protected Optional<Object> parseResponse(RestConfig restConfig, HttpGet httpGet, HttpEntity httpEntity) throws IOException {
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
                  httpGet.getURI().toString(),
                  httpEntity.getContentLength(),
                  actualContentLength));
        }
      }
      return parsedResponse;
    }

    /**
     * Read bytes from a HDFS path.
     * @param inPath
     * @return
     * @throws IOException
     */
    private byte[] readBytes(Path inPath) throws IOException {
      FileSystem fs = FileSystem.get(inPath.toUri(), new Configuration());
      try (FSDataInputStream inputStream = fs.open(inPath)) {
        return IOUtils.toByteArray(inputStream);
      }
    }
  }
}
