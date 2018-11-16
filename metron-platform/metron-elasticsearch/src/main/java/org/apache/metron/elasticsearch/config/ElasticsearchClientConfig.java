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

package org.apache.metron.elasticsearch.config;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.collections4.map.AbstractMapDecorator;
import org.apache.commons.lang.StringUtils;
import org.apache.metron.common.utils.HDFSUtils;

/**
 * Access configuration options for the ES client.
 */
public class ElasticsearchClientConfig extends AbstractMapDecorator<String, Object> {

  private static final Integer THIRTY_SECONDS_IN_MILLIS = 30_000;
  private static final Integer ONE_SECONDS_IN_MILLIS = 1_000;
  private static final String DEFAULT_KEYSTORE_TYPE = "JKS";

  /**
   * Initialize config from provided settings Map.
   *
   * @param settings Map of config options from which to initialize.
   */
  public ElasticsearchClientConfig(Map<String, Object> settings) {
    super(settings);
  }

  /**
   * @return Connection timeout as specified by user, or default 1s as defined by the ES client.
   */
  public Integer getConnectTimeoutMillis() {
    return ElasticsearchClientOptions.CONNECTION_TIMEOUT_MILLIS
        .getOrDefault(this, Integer.class, ONE_SECONDS_IN_MILLIS);
  }

  /**
   * @return socket timeout specified by user, or default 30s as defined by the ES client.
   */
  public Integer getSocketTimeoutMillis() {
    return ElasticsearchClientOptions.SOCKET_TIMEOUT_MILLIS
        .getOrDefault(this, Integer.class, THIRTY_SECONDS_IN_MILLIS);
  }

  /**
   * @return max retry timeout specified by user, or default 30s as defined by the ES client.
   */
  public Integer getMaxRetryTimeoutMillis() {
    return ElasticsearchClientOptions.MAX_RETRY_TIMEOUT_MILLIS
        .getOrDefault(this, Integer.class, THIRTY_SECONDS_IN_MILLIS);
  }

  /**
   * Elasticsearch X-Pack credentials.
   *
   * @return Username, password
   */
  public Optional<Map.Entry<String, String>> getCredentials() {
    if (ElasticsearchClientOptions.XPACK_PASSWORD_FILE.containsOption(this)) {
      if (!ElasticsearchClientOptions.XPACK_USERNAME.containsOption(this) ||
          StringUtils.isEmpty(ElasticsearchClientOptions.XPACK_USERNAME.get(this, String.class))) {
        throw new IllegalArgumentException(
            "X-pack username is required when password supplied and cannot be empty");
      }
      String user = ElasticsearchClientOptions.XPACK_USERNAME.get(this, String.class);
      String password = getPasswordFromFile(
          ElasticsearchClientOptions.XPACK_PASSWORD_FILE.get(this, String.class));
      if (user != null && password != null) {
        return Optional.of(new AbstractMap.SimpleImmutableEntry<String, String>(user, password));
      }
    }
    return Optional.empty();
  }

  /**
   * Expects single password on first line.
   */
  private static String getPasswordFromFile(String hdfsPath) {
    List<String> lines = readLines(hdfsPath);
    if (lines.size() == 0) {
      throw new IllegalArgumentException(format("No password found in file '%s'", hdfsPath));
    }
    return lines.get(0);
  }

  /**
   * Read all lines from HDFS file.
   *
   * @param hdfsPath path to file
   * @return lines
   */
  private static List<String> readLines(String hdfsPath) {
    try {
      return HDFSUtils.readFile(hdfsPath);
    } catch (IOException e) {
      throw new IllegalStateException(
          format("Unable to read XPack password file from HDFS location '%s'", hdfsPath), e);
    }
  }

  /**
   * Determines if SSL is enabled from user-supplied config ssl.enabled.
   */
  public boolean isSSLEnabled() {
    return ElasticsearchClientOptions.SSL_ENABLED.getOrDefault(this, Boolean.class, false);
  }

  /**
   * http by default, https if ssl is enabled.
   */
  public String getConnectionScheme() {
    return isSSLEnabled() ? "https" : "http";
  }

  /**
   * @return Number of threads to use for client connection.
   */
  public Optional<Integer> getNumClientConnectionThreads() {
    if (ElasticsearchClientOptions.NUM_CLIENT_CONNECTION_THREADS.containsOption(this)) {
      return Optional
          .of(ElasticsearchClientOptions.NUM_CLIENT_CONNECTION_THREADS.get(this, Integer.class));
    }
    return Optional.empty();
  }

  /**
   * @return User-defined keystore type. Defaults to "JKS" if not defined.
   */
  public String getKeyStoreType() {
    if (ElasticsearchClientOptions.KEYSTORE_TYPE.containsOption(this)
        && StringUtils
        .isNotEmpty(ElasticsearchClientOptions.KEYSTORE_TYPE.get(this, String.class))) {
      return ElasticsearchClientOptions.KEYSTORE_TYPE.get(this, String.class);
    }
    return DEFAULT_KEYSTORE_TYPE;
  }

  /**
   * Reads keystore password from the HDFS file defined by setting "keystore.password.file", if it
   * exists.
   *
   * @return password if it exists, empty optional otherwise.
   */
  public Optional<String> getKeyStorePassword() {
    if (ElasticsearchClientOptions.KEYSTORE_PASSWORD_FILE.containsOption(this)) {
      String password = getPasswordFromFile(
          ElasticsearchClientOptions.KEYSTORE_PASSWORD_FILE.get(this, String.class));
      if (StringUtils.isNotEmpty(password)) {
        return Optional.of(password);
      }
    }
    return Optional.empty();
  }

  /**
   * @return keystore path.
   */
  public Optional<Path> getKeyStorePath() {
    if (ElasticsearchClientOptions.KEYSTORE_PATH.containsOption(this)) {
      return Optional.of(Paths.get(ElasticsearchClientOptions.KEYSTORE_PATH.get(this, String.class)));
    }
    return Optional.empty();
  }
}
