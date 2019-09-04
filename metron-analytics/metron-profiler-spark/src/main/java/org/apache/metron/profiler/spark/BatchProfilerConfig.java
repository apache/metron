/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.metron.profiler.spark;

import static org.apache.metron.profiler.spark.reader.TelemetryReaders.JSON;

import java.util.Map;
import java.util.Properties;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.metron.hbase.HTableProvider;
import org.apache.metron.stellar.common.utils.ConversionUtils;

/**
 * Defines the configuration values recognized by the Batch Profiler.
 */
public enum BatchProfilerConfig {

  PERIOD_DURATION_UNITS("profiler.period.duration.units", "MINUTES", String.class),

  PERIOD_DURATION("profiler.period.duration", 15, Integer.class),

  HBASE_SALT_DIVISOR("profiler.hbase.salt.divisor", 1000, Integer.class),

  HBASE_TABLE_PROVIDER("profiler.hbase.table.provider", HTableProvider.class.getName(), String.class),

  HBASE_TABLE_NAME("profiler.hbase.table", "profiler", String.class),

  HBASE_COLUMN_FAMILY("profiler.hbase.column.family", "P", String.class),

  HBASE_WRITE_DURABILITY("profiler.hbase.durability", Durability.USE_DEFAULT, Durability.class),

  TELEMETRY_INPUT_READER("profiler.batch.input.reader", JSON.toString(), String.class),

  TELEMETRY_INPUT_FORMAT("profiler.batch.input.format", "", String.class),

  TELEMETRY_INPUT_PATH("profiler.batch.input.path", "hdfs://localhost:8020/apps/metron/indexing/indexed/*/*", String.class),

  TELEMETRY_INPUT_BEGIN("profiler.batch.input.begin", "", String.class),

  TELEMETRY_INPUT_END("profiler.batch.input.end", "", String.class);

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

  BatchProfilerConfig(String key, Object defaultValue, Class<?> valueType) {
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
  public Object get(Map<String, String> config) {
    return getOrDefault(config, defaultValue);
  }

  /**
   * Returns the configuration value from a map of configuration values.
   *
   * @param properties Configuration properties.
   */
  public Object get(Properties properties) {
    return getOrDefault(properties, defaultValue);
  }

  /**
   * Returns the configuration value from a map of configuration values, cast to the expected type.
   *
   * @param config A map containing configuration values.
   */
  public <T> T get(Map<String, String> config, Class<T> clazz) {
    return getOrDefault(config, defaultValue, clazz);
  }

  /**
   * Returns the configuration value from a map of configuration values, cast to the expected type.
   *
   * @param properties Configuration properties.
   */
  public <T> T get(Properties properties, Class<T> clazz) {
    return getOrDefault(properties, defaultValue, clazz);
  }

  /**
   * Returns the configuration value from a map of configuration values.  If the value is not specified,
   * the default value is returned.
   *
   * @param config A map containing configuration values.
   * @param defaultValue The default value to return, if one is not defined.
   * @return The configuration value or the specified default, if one is not defined.
   */
  private Object getOrDefault(Map<String, String> config, Object defaultValue) {
    return getOrDefault(config, defaultValue, valueType);
  }

  /**
   * Returns the configuration value from a map of configuration values.  If the value is not specified,
   * the default value is returned.
   *
   * @param properties A map containing configuration values.
   * @param defaultValue The default value to return, if one is not defined.
   * @return The configuration value or the specified default, if one is not defined.
   */
  private Object getOrDefault(Properties properties, Object defaultValue) {
    return getOrDefault(properties, defaultValue, valueType);
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
  private <T> T getOrDefault(Map<String, String> config, Object defaultValue, Class<T> clazz) {
    Object value = config.getOrDefault(key, defaultValue.toString());
    return value == null ? null : ConversionUtils.convert(value, clazz);
  }

  /**
   * Returns the configuration value, cast to the expected type, from a map of configuration values.
   * If the value is not specified, the default value is returned.
   *
   * @param properties Configuration properties.
   * @param defaultValue The default value to return, if one is not defined.
   * @param clazz The class of the expected type of the configuration value.
   * @param <T> The expected type of the configuration value.
   * @return The configuration value or the specified default, if one is not defined.
   */
  private <T> T getOrDefault(Properties properties, Object defaultValue, Class<T> clazz) {
    Object value = properties.getOrDefault(key, defaultValue);
    return value == null ? null : ConversionUtils.convert(value, clazz);
  }

  @Override
  public String toString() {
    return key;
  }
}
