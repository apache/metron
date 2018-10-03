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

import org.apache.metron.common.Constants;
import org.apache.metron.stellar.common.utils.ConversionUtils;

import java.util.Map;

/**
 * Configuration settings that customize the behavior of the {@link ElasticsearchWriter}.
 */
public enum ElasticsearchWriterConfig {

  /**
   * Defines which message field, the document identifier is set to.
   *
   * <p>If defined, the value of the specified message field is set as the Elasticsearch doc ID. If
   * this field is undefined or blank, then the document identifier is not set.
   */
  DOC_ID_SOURCE_FIELD("es.document.id", "", String.class);

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

  ElasticsearchWriterConfig(String key, Object defaultValue, Class<?> valueType) {
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
   * Returns the configuration value from a map of configuration values, cast to the expected type.
   *
   * @param config A map containing configuration values.
   */
  public <T> T get(Map<String, Object> config, Class<T> clazz) {
    return getOrDefault(config, defaultValue, clazz);
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
    Object value = config.getOrDefault(key, defaultValue.toString());
    return value == null ? null : ConversionUtils.convert(value, clazz);
  }

  @Override
  public String toString() {
    return key;
  }
}
