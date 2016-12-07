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

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;

/**
 * Responsible for managing configuration values that are created,
 * persisted, and updated in an external data store.
 *
 * The semantics of using a ConfigurationManager are as follows.
 * The configuration keys of interest must be defined, then it must
 * be opened, and next the configuration values can be read.
 * It should be closed when no longer needed.
 */
public interface ConfigurationManager extends Closeable {

  /**
   * Defines the configuration values that will be managed.
   * @param key A key that identifies the configuration value.
   */
  ConfigurationManager with(String key);

  /**
   * Open the configuration manager.
   *
   * Expects to be called before retrieving any configuration values.
   */
  ConfigurationManager open() throws IOException;

  /**
   * Retrieve a configuration value.
   * @param key A key to identify which configuration value.
   * @param clazz The expected type of the configuration value.
   * @param <T> The expected type of the configuration value.
   */
  <T> Optional<T> get(String key, Class<T> clazz) throws IOException;
}
