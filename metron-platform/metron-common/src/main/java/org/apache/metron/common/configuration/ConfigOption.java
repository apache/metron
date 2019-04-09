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

package org.apache.metron.common.configuration;

import java.util.Map;
import java.util.function.BiFunction;
import org.apache.metron.stellar.common.utils.ConversionUtils;

/**
 * Interface for defining Key -> Value options for programs, along with some basic utilities for
 * handling casting to specified types.
 */
public interface ConfigOption {

  String getKey();

  /**
   * A default identity transformation.
   *
   * @return The transformed object
   */
  default BiFunction<String, Object, Object> transform() {
    return (s, o) -> o;
  }

  /**
   * Returns true if the map contains the key for the defined config option.
   */
  default boolean containsOption(Map<String, Object> map) {
    return map.containsKey(getKey());
  }

  default void put(Map<String, Object> map, Object value) {
    map.put(getKey(), value);
  }

  /**
   * Retrieves a value from a {@link Map}. It will cast to a provided class. If nothing is found,
   * a default will be returned.
   *
   * @param map The map to look for the key in
   * @param clazz The class to cast the value to
   * @param defaultValue The default value if no key is found
   * @param <T> The class type parameter for the class being casted to
   * @return The value, or if not found, the default value
   */
  default <T> T getOrDefault(Map<String, Object> map, Class<T> clazz, T defaultValue) {
    T val;
    return ((val = get(map, clazz)) == null ? defaultValue : val);
  }

  /**
   * Retrieves a value from a {@link Map}. It will cast to a provided class.
   *
   * @param map The map to look for the key in
   * @param clazz The class to cast the value to
   * @param <T> The class type parameter for the class being casted to
   * @return The value if found, else null
   */
  default <T> T get(Map<String, Object> map, Class<T> clazz) {
    Object obj = map.get(getKey());
    if (clazz.isInstance(obj)) {
      return clazz.cast(obj);
    } else {
      return ConversionUtils.convert(obj, clazz);
    }
  }

  /**
   * Retrieves a transformed result from a {@link Map}. It will cast to a provided class. If
   * the transformation result is null, a default will be returned.
   *
   * @param map The map to look for the key in
   * @param transform The transform to run on key-value
   * @param clazz The class to cast the value to
   * @param defaultValue The default value if no key is found
   * @param <T> The class type parameter for the class being casted to
   * @return The transformed result of the lookup, or the default value if the result is null
   */
  default <T> T getOrDefault(Map<String, Object> map, BiFunction<String, Object, T> transform,
      Class<T> clazz, T defaultValue) {
    T val;
    return ((val = get(map, transform, clazz)) == null ? defaultValue : val);
  }

  /**
   * Retrieves a transformed result from a {@link Map}. It will cast to a provided class.
   *
   * @param map The map to look for the key in
   * @param transform The transform to run on key-value
   * @param clazz The class to cast the value to
   * @param <T> The class type parameter for the class being casted to
   * @return The transformed result of the lookup
   */
  default <T> T get(Map<String, Object> map, BiFunction<String, Object, T> transform,
      Class<T> clazz) {
    return clazz.cast(transform.apply(getKey(), map.get(getKey())));
  }

  /**
   * Retrieves a transformed result from a {@link Map}. It will cast to a provided class. The
   * transform is the one provided by the implementation. If the transformation result is null,
   * a default will be returned.
   *
   * @param map The map to look for the key in
   * @param clazz The class to cast the value to
   * @param defaultValue The default value if no key is found
   * @param <T> The class type parameter for the class being casted to
   * @return The transformed result of the lookup, or the default value if the result is null
   */
  default <T> T getTransformedOrDefault(Map<String, Object> map, Class<T> clazz, T defaultValue) {
    T val;
    return ((val = getTransformed(map, clazz)) == null ? defaultValue : val);
  }

  /**
   * Retrieves a transformed result from a {@link Map}. It will cast to a provided class. The
   * transform is the one provided by the implementation.
   *
   * @param map The map to look for the key in
   * @param clazz The class to cast the value to
   * @param <T> The class type parameter for the class being casted to
   * @return The transformed result of the lookup, or the default value if the result is null
   */
  default <T> T getTransformed(Map<String, Object> map, Class<T> clazz) {
    return clazz.cast(transform().apply(getKey(), map.get(getKey())));
  }

}
