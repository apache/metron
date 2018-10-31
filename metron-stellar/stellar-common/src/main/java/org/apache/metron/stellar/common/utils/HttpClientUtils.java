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
package org.apache.metron.stellar.common.utils;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.util.Map;

/**
 * Utility class for building HttpClient objects.
 */
public class HttpClientUtils {

  public static final String HTTPCLIENT_POOL_MAX_TOTAL_PROPERTY = "stellar.httpclient.pool.max.total";
  public static final String HTTPCLIENT_POOL_MAX_PER_ROUTE_PROPERTY = "stellar.httpclient.pool.max.per.route";

  /**
   * Returns a pooling CloseableHttpClient and configures it based on settings exposed in the global config.
   * @param globalConfig
   * @return
   */
  public static CloseableHttpClient getPoolingClient(Map<String, Object> globalConfig) {
    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    if (globalConfig.containsKey(HTTPCLIENT_POOL_MAX_TOTAL_PROPERTY)) {
      cm.setMaxTotal((int) globalConfig.get(HTTPCLIENT_POOL_MAX_TOTAL_PROPERTY));
    }
    if (globalConfig.containsKey(HTTPCLIENT_POOL_MAX_PER_ROUTE_PROPERTY)) {
      cm.setDefaultMaxPerRoute((int) globalConfig.get(HTTPCLIENT_POOL_MAX_PER_ROUTE_PROPERTY));
    }

   return HttpClients.custom()
            .setConnectionManager(cm)
            .build();
  }
}
