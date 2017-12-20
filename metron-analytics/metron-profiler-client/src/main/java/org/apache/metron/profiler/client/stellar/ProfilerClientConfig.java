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

package org.apache.metron.profiler.client.stellar;

import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.hbase.HTableProvider;

import java.util.Map;

public enum ProfilerClientConfig {
  /**
   * A global property that defines the name of the HBase table used to store profile data.
   */
  PROFILER_HBASE_TABLE("profiler.client.hbase.table", "profiler", String.class),

  /**
   * A global property that defines the name of the column family used to store profile data.
   */
  PROFILER_COLUMN_FAMILY("profiler.client.hbase.column.family", "P", String.class),

  /**
   * A global property that defines the name of the HBaseTableProvider implementation class.
   */
  PROFILER_HBASE_TABLE_PROVIDER("hbase.provider.impl", HTableProvider.class.getName(), String.class),

  /**
   * A global property that defines the duration of each profile period.  This value
   * should be defined along with 'profiler.client.period.duration.units'.
   */
  PROFILER_PERIOD("profiler.client.period.duration", 15L, Long.class),

  /**
   * A global property that defines the units of the profile period duration.  This value
   * should be defined along with 'profiler.client.period.duration'.
   */
  PROFILER_PERIOD_UNITS("profiler.client.period.duration.units", "MINUTES", String.class),

  /**
   * A global property that defines the salt divisor used to store profile data.
   */
  PROFILER_SALT_DIVISOR("profiler.client.salt.divisor", 1000L, Long.class),

  /**
   * The default value to be returned if a profile is not written for a given period for a profile and entity.
   */
  PROFILER_DEFAULT_VALUE("profiler.default.value", null, Object.class);
  String key;
  Object defaultValue;
  Class<?> valueType;

  ProfilerClientConfig(String key, Object defaultValue, Class<?> valueType) {
    this.key = key;
    this.defaultValue = defaultValue;
    this.valueType = valueType;
  }

  public String getKey() {
    return key;
  }

  public Object getDefault() {
    return getDefault(valueType);
  }

  public <T> T getDefault(Class<T> clazz) {
    return defaultValue == null?null:ConversionUtils.convert(defaultValue, clazz);
  }

  public Object get(Map<String, Object> profilerConfig) {
    return getOrDefault(profilerConfig, defaultValue);
  }

  public Object getOrDefault(Map<String, Object> profilerConfig, Object defaultValue) {
    return getOrDefault(profilerConfig, defaultValue, valueType);
  }

  public <T> T get(Map<String, Object> profilerConfig, Class<T> clazz) {
    return getOrDefault(profilerConfig, defaultValue, clazz);
  }

  public <T> T getOrDefault(Map<String, Object> profilerConfig, Object defaultValue, Class<T> clazz) {
    Object o = profilerConfig.getOrDefault(key, defaultValue);
    return o == null?null:ConversionUtils.convert(o, clazz);
  }

  @Override
  public String toString() {
    return key;
  }
}
