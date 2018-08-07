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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the default interface methods
 */
public class ConfigOptionTest {

  @Before
  public void setup() {
  }

  @Test
  public void gets_value_of_specified_type() {
    ConfigOption option = newOption("foo");
    Map<String, Object> config = new HashMap<>();
    option.put(config, 25L);
    assertThat(option.get(config, Long.class), equalTo(25L));
    assertThat(option.get(mapWith("foo", 25L), Long.class), equalTo(25L));
  }

  @Test
  public void gets_value_of_specified_type_with_transform() {
    ConfigOption option = newOption("foo");
    Map<String, Object> config = new HashMap<>();
    option.put(config, "25");
    BiFunction<String, Object, Long> transform = (s, o) -> o == null ? null
        : new Long(o.toString());
    assertThat(option.get(config, transform, Long.class), equalTo(25L));
    assertThat(option.get(mapWith("foo", "25"), transform, Long.class), equalTo(25L));
  }

  @Test
  public void gets_default_value_of_specified_type_with_transform() {
    ConfigOption option = newOption("foo");
    Map<String, Object> config = new HashMap<>();
    option.put(config, null);
    BiFunction<String, Object, Long> transform = (s, o) -> o == null ? null
        : new Long(o.toString());
    assertThat(option.getOrDefault(config, transform, Long.class, 25L), equalTo(25L));
    assertThat(option.getOrDefault(mapWith("foo", null), transform, Long.class, 25L), equalTo(25L));
  }

  @Test
  public void gets_default_when_null_value() {
    ConfigOption option = newOption("foo");
    Map<String, Object> config = new HashMap<>();
    option.put(config, null);
    assertThat(option.getOrDefault(config, Long.class, 0L), equalTo(0L));
    assertThat(option.getOrDefault(mapWith("foo", null), Long.class, 0L), equalTo(0L));
  }

  @Test
  public void gets_object_transformed_by_class_cast() {
    ConfigOption option = newOption("foo");
    Map<String, Object> config = new HashMap<>();
    option.put(config, (Object) 25L);
    assertThat(option.getTransformed(config, Long.class), equalTo(25L));
    assertThat(option.getTransformed(mapWith("foo", (Object) 25L), Long.class), equalTo(25L));
  }

  @Test
  public void gets_default_null_with_cast_when_null() {
    ConfigOption option = newOption("foo");
    Map<String, Object> config = new HashMap<>();
    option.put(config, null);
    assertThat(option.getTransformedOrDefault(config, Long.class, 25L), equalTo(25L));
    assertThat(option.getTransformedOrDefault(mapWith("foo", null), Long.class, 25L), equalTo(25L));
  }

  private <K, V> Map<K, V> mapWith(K key, V val) {
    Map<K, V> map = new HashMap<>();
    map.put(key, val);
    return map;
  }

  private ConfigOption newOption(final String key) {
    return new ConfigOption() {
      @Override
      public String getKey() {
        return key;
      }
    };
  }

}
