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

package org.apache.metron.common.configuration.manager;

import org.apache.metron.common.utils.ConversionUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A ConfigurationManager that maintains all values in memory.
 *
 * The implementation ensures that the same semantics are followed as the ZkConfigurationManager.
 * First the configuration manager must be opened, then the configuration keys must be defined,
 * then values can be loaded, and finally it can be closed.
 */
public class InMemoryConfigurationManager implements ConfigurationManager {

  public Map<String, Object> values;

  public InMemoryConfigurationManager() {
    values = new HashMap<>();
  }

  @Override
  public ConfigurationManager open() throws IOException {
    // nothing to do
    return this;
  }

  @Override
  public <T> ConfigurationManager with(String key) {
    // nothing to do
    return this;
  }

  public ConfigurationManager setValue(String key, Object value) {
    values.put(key, value);
    return this;
  }

  @Override
  public <T> Optional<T> get(String key, Class<T> clazz) throws IOException {
    T result = ConversionUtils.convert(values.get(key), clazz);
    return Optional.ofNullable(result);
  }

  @Override
  public void close() throws IOException {
    // nothing to do
  }
}
