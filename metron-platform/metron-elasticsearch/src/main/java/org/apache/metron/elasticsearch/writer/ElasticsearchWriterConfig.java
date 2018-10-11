/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.metron.elasticsearch.writer;

import org.apache.metron.stellar.common.utils.ConversionUtils;

import java.util.Map;

/**
 * Configuration settings that customize the behavior of the {@link ElasticsearchWriter}.
 */
public enum ElasticsearchWriterConfig {

  /**
   * The name of the Elasticsearch cluster.
   *
   * <p>This is optional and defaults to 'metron'.
   */
  ELASTICSEARCH_CLUSTER("es.clustername", "metron", String.class, false),

  /**
   * Defines the nodes in the Elasticsearch cluster.
   *
   * <p>This is a required configuration.
   */
  ELASTICSEARCH_IP("es.ip", "", Object.class, true),

  /**
   * Defines the port to use when connecting with the Elasticsearch cluster.
   *
   * <p>This is optional and defaults to '9300'.
   */
  ELASTICSEARCH_PORT("es.port", "9300", String.class, false),

  /**
   * The date format to use when constructing the indices.
   *
   * <p>This is optional and defaults to 'yyyy.MM.dd.HH' which rolls the indices hourly.
   */
  ELASTICSEARCH_DATE_FORMAT("es.date.format", "yyyy.MM.dd.HH", String.class, false),

  /**
   * Defines which message field, the document identifier is set to.
   *
   * <p>This is optional and defaults to not setting the document ID.
   */
  ELASTICSEARCH_DOC_ID("es.document.id", "", String.class, false),

  /**
   * The class used for the Elasticsearch client.
   *
   * <p>This is an optional configuration.
   */
  ELASTICSEARCH_CLIENT_CLASS("es.client.class", "org.elasticsearch.transport.client.PreBuiltTransportClient", String.class, false),

  /**
   * Defines the X-Pack username.
   *
   * <p>This is a required configuration.
   */
  ELASTICSEARCH_XPACK_USERNAME("es.xpack.username", "", String.class, true),

  /**
   * Defines the path in HDFS to a file containing the X-Pack password.
   *
   * <p>This is a required configuration.
   */
  ELASTICSEARCH_XPACK_PASSWORD_FILE("es.xpack.password.file", "", String.class, true);

  /**
   * The key for the configuration value.
   */
  private String key;

  /**
   * The default value of the configuration, if none other is specified.
   */
  private Object defaultValue;

  /**
   * The type of the configuration value.
   */
  private Class<?> valueType;

  /**
   * If the property is required.  False indicates that the property is optional.
   */
  private boolean isRequired;

  ElasticsearchWriterConfig(String key, Object defaultValue, Class<?> valueType, boolean isRequired) {
    this.key = key;
    this.defaultValue = defaultValue;
    this.valueType = valueType;
  }

  /**
   * Returns the key of the configuration value.
   */
  public String getKey() {
    return key;
  }

  /**
   * Returns the default value of the configuration.
   */
  public Object getDefault() {
    return getDefault(valueType);
  }

  /**
   * Returns the default value of the configuration, cast to the expected type.
   *
   * @param clazz The class of the expected type of the configuration value.
   * @param <T> The expected type of the configuration value.
   */
  public <T> T getDefault(Class<T> clazz) {
    return defaultValue == null ? null: ConversionUtils.convert(defaultValue, clazz);
  }

  /**
   * Returns the configuration value from a map of configuration values.
   *
   * @param config A map containing configuration values.
   */
  public Object get(Map<String, Object> config) {
    return getOrDefault(config, defaultValue);
  }

  /**
   * Returns true if the configuration is defined.
   *
   * @param config A map containing configuration values.
   * @return True, if the configuration is defined. Otherwise, false.
   */
  public boolean isDefined(Map<String, Object> config) {
    return config != null && config.containsKey(key);
  }

  /**
   * Returns true if the configuration is defined.
   *
   * @param config A map containing configuration values.
   * @return True, if the configuration is defined. Otherwise, false.
   */
  public boolean isStringDefined(Map<String, String> config) {
    return config != null && config.containsKey(key);
  }

  /**
   * Returns the configuration value from a map of configuration values, cast to the expected type.
   *
   * @param config A map containing configuration values.
   */
  public <T> T get(Map<String, Object> config, Class<T> clazz) {
    return getOrDefault(config, defaultValue, clazz);
  }

  /**
   * Returns the configuration value from a map of configuration values, cast to the expected type.
   *
   * @param config A map containing configuration values.
   */
  public String getString(Map<String, String> config) {
    return getStringOrDefault(config, defaultValue);
  }

  /**
   * Returns the configuration value from a map of configuration values.  If the value is not specified,
   * the default value is returned.
   *
   * @param config A map containing configuration values.
   * @param defaultValue The default value to return, if one is not defined.
   * @return The configuration value or the specified default, if one is not defined.
   */
  private Object getOrDefault(Map<String, Object> config, Object defaultValue) {
    return getOrDefault(config, defaultValue, valueType);
  }

  /**
   * Returns the configuration value, cast to the expected type, from a map of configuration values.
   * If the value is not specified, the default value is returned.
   *
   * @param config A map containing configuration values.
   * @param defaultValue The default value to return, if one is not defined.
   * @param clazz The class of the expected type of the configuration value.
   * @param <T> The expected type of the configuration value.
   * @return The configuration value or the specified default, if one is not defined.
   */
  private <T> T getOrDefault(Map<String, Object> config, Object defaultValue, Class<T> clazz) {
    if(isRequired && !config.containsKey(key)) {
      throw new IllegalArgumentException("Missing required configuration; " + key);
    }
    Object value = config.getOrDefault(key, defaultValue);
    return value == null ? null : ConversionUtils.convert(value, clazz);
  }

  /**
   * Returns the configuration value, cast to the expected type, from a map of configuration values.
   * If the value is not specified, the default value is returned.
   *
   * @param config A map containing configuration values.
   * @param defaultValue The default value to return, if one is not defined.
   * @return The configuration value or the specified default, if one is not defined.
   */
  private String getStringOrDefault(Map<String, String> config, Object defaultValue) {
    if(isRequired && !config.containsKey(key)) {
      throw new IllegalArgumentException("Missing required configuration; " + key);
    }
    Object value = config.getOrDefault(key, defaultValue.toString());
    return value == null ? null : ConversionUtils.convert(value, String.class);
  }

  @Override
  public String toString() {
    return key;
  }
}
