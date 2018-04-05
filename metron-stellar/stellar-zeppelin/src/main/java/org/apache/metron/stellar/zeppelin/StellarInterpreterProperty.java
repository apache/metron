/*
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
package org.apache.metron.stellar.zeppelin;

import org.apache.metron.stellar.common.utils.ConversionUtils;

import java.util.Map;

/**
 * Defines the properties that a user can define when configuring
 * the Stellar Interpreter in Zeppelin.
 */
public enum StellarInterpreterProperty {

  /**
   * A property that defines the URL for connecting to Zookeeper.  By default this is empty.
   */
  ZOOKEEPER_URL("zookeeper.url", null);

  /**
   * The key or property name.
   */
  String key;

  /**
   * The default value of the property.
   */
  Object defaultValue;

  StellarInterpreterProperty(String key, Object defaultValue) {
    this.key = key;
    this.defaultValue = defaultValue;
  }

  /**
   * @return The key or name of the property.
   */
  public String getKey() {
    return key;
  }

  /**
   * @return The default value of the property.
   */
  public <T> T getDefault(Class<T> clazz) {
    return ConversionUtils.convert(defaultValue, clazz);
  }

  /**
   * Retrieves the property value from a map of properties.
   * @param properties A map of properties.
   * @return The value of the property within the map.
   */
  public <T> T get(Map<Object, Object> properties, Class<T> clazz) {
    Object o = properties.getOrDefault(key, defaultValue);
    return o == null ? null : ConversionUtils.convert(o, clazz);
  }

  @Override
  public String toString() {
    return key;
  }
}

