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

import org.apache.metron.common.configuration.ConfigOption;

public enum ElasticsearchClientOptions implements ConfigOption {
  CONNECTION_TIMEOUT_MILLIS("connection.timeout.millis"),
  SOCKET_TIMEOUT_MILLIS("socket.timeout.millis"),
  MAX_RETRY_TIMEOUT_MILLIS("max.retry.timeout.millis"),
  NUM_CLIENT_CONNECTION_THREADS("num.client.connection.threads"),
  // authentication
  XPACK_USERNAME("xpack.username"),
  XPACK_PASSWORD_FILE("xpack.password.file"),
  // security/encryption
  SSL_ENABLED("ssl.enabled"),
  KEYSTORE_TYPE("keystore.type"),
  KEYSTORE_PATH("keystore.path"),
  KEYSTORE_PASSWORD_FILE("keystore.password.file");

  private final String key;

  ElasticsearchClientOptions(String key) {
    this.key = key;
  }

  @Override
  public String getKey() {
    return key;
  }

  /**
   * Convenience method for printing all options as their key representation.
   */
  public static void printOptions() {
    String newLine = "";
    for (ElasticsearchClientOptions opt : ElasticsearchClientOptions.values()) {
      System.out.print(newLine);
      System.out.print(opt.getKey());
      newLine = System.lineSeparator();
    }
  }

}
