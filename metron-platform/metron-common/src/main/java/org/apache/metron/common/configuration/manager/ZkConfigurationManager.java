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

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.utils.CloseableUtils;
import org.apache.metron.common.utils.JSONUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang.ArrayUtils.isNotEmpty;

/**
 * Responsible for managing configuration values that are created, persisted, and updated
 * in Zookeeper.
 */
public class ZkConfigurationManager implements ConfigurationManager {

  /**
   * A Zookeeper client.
   */
  private CuratorFramework zookeeperClient;

  /**
   * The configuration values under management.  Maps the path to the configuration values
   * in Zookeeper to the cache of its values.
   */
  private Map<String, NodeCache> valuesCache;

  /**
   * @param zookeeperClient The client used to communicate with Zookeeper.  The client is not
   *                        closed.  It must be managed externally.
   */
  public ZkConfigurationManager(CuratorFramework zookeeperClient) {
    this.zookeeperClient = zookeeperClient;
    this.valuesCache = Collections.synchronizedMap(new HashMap<>());
  }

  /**
   * Define the paths within Zookeeper that contains configuration values that need managed.
   * @param zookeeperPath The Zookeeper path.
   */
  @Override
  public ZkConfigurationManager with(String zookeeperPath) {
    NodeCache cache = new NodeCache(zookeeperClient, zookeeperPath);
    valuesCache.put(zookeeperPath, cache);
    return this;
  }

  /**
   * Open a connection to Zookeeper and retrieve the initial configuration value.
   */
  @Override
  public synchronized ZkConfigurationManager open() throws IOException {
    try {
      doOpen();
    } catch(Exception e) {
      throw new IOException(e);
    }

    return this;
  }

  private void doOpen() throws Exception {
    synchronized (valuesCache) {
      for (NodeCache cache : valuesCache.values()) {
        cache.start(true);
      }
    }
  }

  /**
   * Retrieve the configuration object.
   */
  @Override
  public synchronized <T> Optional<T> get(String key, Class<T> clazz) throws IOException {
    T result = null;

    NodeCache cache = valuesCache.get(key);
    if(cache != null && cache.getCurrentData() != null && isNotEmpty(cache.getCurrentData().getData())) {
      result = deserialize(cache.getCurrentData().getData(), clazz);
    }

    return Optional.ofNullable(result);
  }

  /**
   * Close the configuration manager.
   *
   * Does not close the zookeeperClient that was passed in to the constructor.
   */
  @Override
  public synchronized void close() {
    synchronized (valuesCache) {
      for (NodeCache cache : valuesCache.values()) {
        CloseableUtils.closeQuietly(cache);
      }
    }
  }

  /**
   * Deserializes the configuration value.
   * @param raw The serialized form of the configuration value.
   * @param clazz The expected type of the configuration value.
   */
  protected static <T> T deserialize(byte[] raw, Class<T> clazz) throws IOException {
    return JSONUtils.INSTANCE.load(new ByteArrayInputStream(raw), clazz);
  }
}
