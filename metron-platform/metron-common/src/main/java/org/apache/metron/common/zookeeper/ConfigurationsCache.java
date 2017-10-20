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
package org.apache.metron.common.zookeeper;

import org.apache.metron.common.configuration.Configurations;

/**
 * A cache for multiple Configurations object.  This cache is generally kept in
 * sync with zookeeper changes.
 */
public interface ConfigurationsCache extends AutoCloseable{
  /**
   * Return the Configurations object given the specific type of Configurations object.
   * @param configClass The Configurations class to return
   * @param <T> The type of Configurations class.
   * @return The Configurations object
   */
  <T extends Configurations> T get(Class<T> configClass);

  /**
   * Reset the cache and reload from zookeeper.
   */
  void reset();

  /**
   * Start the cache.
   */
  void start();
}
