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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * A Map containing the Stellar REST settings.
 */
public class RestConfig extends HashMap<String, Object> {

  /**
   * A global config prefix used for storing Stellar REST settings.
   */
  public final static String STELLAR_REST_SETTINGS = "stellar.rest.settings";

  /**
   * User name for basic authentication.
   */
  public final static String BASIC_AUTH_USER = "basic.auth.user";

  /**
   * Path to the basic authentication password file stored in HDFS.
   */
  public final static String BASIC_AUTH_PASSWORD_PATH = "basic.auth.password.path";

  /**
   * Proxy host.
   */
  public final static String PROXY_HOST = "proxy.host";

  /**
   * Proxy port.
   */
  public final static String PROXY_PORT = "proxy.port";

  /**
   * User name for proxy basic authentication.
   */
  public final static String PROXY_BASIC_AUTH_USER = "proxy.basic.auth.user";

  /**
   * Path to the proxy basic authentication password file stored in HDFS.
   */
  public final static String PROXY_BASIC_AUTH_PASSWORD_PATH = "proxy.basic.auth.password.path";

  /**
   * Hard timeout for the total request time.
   */
  public final static String TIMEOUT = "timeout";

  /**
   * Timeouts exposed by the HttpClient object.
   */
  public final static String CONNECT_TIMEOUT = "connect.timeout";
  public final static String CONNECTION_REQUEST_TIMEOUT = "connection.request.timeout";
  public final static String SOCKET_TIMEOUT = "socket.timeout";

  /**
   * A list of response codes that are allowed.  All others will be treated as errors.
   */
  public final static String RESPONSE_CODES_ALLOWED = "response.codes.allowed";

  /**
   * The default value that will be returned on a successful request with empty content.  Default is null.
   */
  public final static String EMPTY_CONTENT_OVERRIDE = "empty.content.override";

  /**
   * The default value that will be returned on an error.  Default is null.
   */
  public final static String ERROR_VALUE_OVERRIDE = "error.value.override";

  /**
   * The maximum number of connections in the connection pool.
   */
  public final static String POOLING_MAX_TOTAL = "pooling.max.total";

  /**
   * The default maximum number of connections per route in the connection pool.
   */
  public final static String POOLING_DEFAULT_MAX_PER_RUOTE = "pooling.default.max.per.route";

  /**
   * Setting this to true will verify the actual body content length equals the content length header
   */
  public final static String VERIFY_CONTENT_LENGTH = "verify.content.length";

  public RestConfig() {
    put(TIMEOUT, 1000);
    put(RESPONSE_CODES_ALLOWED, Collections.singletonList(200));
    put(VERIFY_CONTENT_LENGTH, false);
  }

  public String getBasicAuthUser() {
    return (String) get(BASIC_AUTH_USER);
  }

  public String getBasicAuthPasswordPath() {
    return (String) get(BASIC_AUTH_PASSWORD_PATH);
  }

  public String getProxyHost() {
    return (String) get(PROXY_HOST);
  }

  public Integer getProxyPort() {
    return (Integer) get(PROXY_PORT);
  }

  public String getProxyBasicAuthUser() {
    return (String) get(PROXY_BASIC_AUTH_USER);
  }

  public String getProxyBasicAuthPasswordPath() {
    return (String) get(PROXY_BASIC_AUTH_PASSWORD_PATH);
  }

  public Integer getTimeout() {
    return (Integer) get(TIMEOUT);
  }

  public Integer getConnectTimeout() {
    return (Integer) get(CONNECT_TIMEOUT);
  }

  public Integer getConnectionRequestTimeout() {
    return (Integer) get(CONNECTION_REQUEST_TIMEOUT);
  }

  public Integer getSocketTimeout() {
    return (Integer) get(SOCKET_TIMEOUT);
  }

  @SuppressWarnings("unchecked")
  public List<Integer> getResponseCodesAllowed() {
    return (List<Integer>) get(RESPONSE_CODES_ALLOWED);
  }

  public Object getEmptyContentOverride() {
    return get(EMPTY_CONTENT_OVERRIDE);
  }

  public Object getErrorValueOverride() {
    return get(ERROR_VALUE_OVERRIDE);
  }

  public Integer getPoolingMaxTotal() {
    return (Integer) get(POOLING_MAX_TOTAL);
  }

  public Integer getPoolingDefaultMaxPerRoute() {
    return (Integer) get(POOLING_DEFAULT_MAX_PER_RUOTE);
  }

  public Boolean verifyContentLength() {
    return (Boolean) get(VERIFY_CONTENT_LENGTH);
  }
}
