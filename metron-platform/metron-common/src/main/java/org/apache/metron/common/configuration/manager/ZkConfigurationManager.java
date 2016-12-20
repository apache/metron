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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang.ArrayUtils.isNotEmpty;
import static org.apache.metron.common.utils.ConversionUtils.convert;

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
   * Maps a zookeeper path to the Curator cache used to retrieve the raw data from Zookeeper.
   */
  private Map<String, NodeCache> zookeeperCache;

  /**
   * A cache of the deserialized objects stored in Zookeeper.  This cache helps limit how
   * frequently we need to deserialize the raw data stored in Zookeeper.
   */
  private Cache<String, Object> configurationCache;

  /**
   * @param zookeeperClient The client used to communicate with Zookeeper.  The client is not
   *                        closed.  It must be managed externally.
   */
  public ZkConfigurationManager(CuratorFramework zookeeperClient) {
    this(zookeeperClient, 15, TimeUnit.MINUTES);
  }

  /**
   * @param zookeeperClient The client used to communicate with Zookeeper.  The client is not
   *                        closed.  It must be managed externally.
   */
  public ZkConfigurationManager(CuratorFramework zookeeperClient, long cacheExpiration, TimeUnit cacheExpirationUnits) {
    this.zookeeperClient = zookeeperClient;
    this.zookeeperCache = Collections.synchronizedMap(new HashMap<>());
    this.configurationCache = CacheBuilder.newBuilder()
            .expireAfterAccess(cacheExpiration, cacheExpirationUnits)
            .build();
  }

  /**
   * Define the paths within Zookeeper that contains configuration values that need managed.
   * @param zkPath The Zookeeper path.
   */
  @Override
  public <T> ZkConfigurationManager with(String zkPath) {

    NodeCache cache = new NodeCache(zookeeperClient, zkPath);
    cache.getListenable().addListener(() -> configurationCache.invalidate(zkPath));
    zookeeperCache.put(zkPath, cache);

    return this;
  }

  /**
   * Open a connection to Zookeeper.
   */
  @Override
  public synchronized ZkConfigurationManager open() throws IOException {
    try {
      synchronized (zookeeperCache) {
        for (NodeCache cache : zookeeperCache.values()) {
          cache.start(true);
        }
      }
    } catch(Exception e) {
      throw new IOException(e);
    }

    return this;
  }

  /**
   * Retrieve the configuration object.
   * @param zkPath A key to identify which configuration value.
   * @param clazz The expected type of the configuration value.
   * @param <T> The expected type of the configuration value.
   */
  @Override
  public <T> Optional<T> get(String zkPath, Class<T> clazz) throws IOException {
    try {
      Object value = configurationCache.get(zkPath, () -> doGet(zkPath, clazz));
      return Optional.ofNullable(convert(value, clazz));

    } catch(ExecutionException e) {

      // exception used to signal no value exists for this key; cannot use null with a guava cache
      if(e.getCause() instanceof ZkPathNotFoundException) {
        return Optional.empty();

      } else {
        throw new IOException(e);
      }
    }
  }

  /**
   * Retrieves the serialized data stored in Zookeeper and deserializes it.
   * @param zkPath The key of the configuration value to get.
   * @param clazz The type of configuration value expected.
   * @param <T> The type of configuration value expected.
   * @return The configuration value.  If value does not exist, an exception is thrown.
   * @throws ZkPathNotFoundException If no such path exists in Zookeeper.
   * @throws IOException
   */
  public synchronized <T> T doGet(String zkPath, Class<T> clazz) throws ZkPathNotFoundException, IOException {
    NodeCache cache = zookeeperCache.get(zkPath);
    if(cache != null && cache.getCurrentData() != null && isNotEmpty(cache.getCurrentData().getData())) {
      return deserialize(cache.getCurrentData().getData(), clazz);

    } else {
      throw new ZkPathNotFoundException(zkPath);
    }
  }

  /**
   * Close the configuration manager.
   *
   * Does not close the zookeeperClient that was passed in to the constructor.
   */
  @Override
  public synchronized void close() {
    synchronized (zookeeperCache) {
      for (NodeCache cache : zookeeperCache.values()) {
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

  /**
   * Indicates that a zookeeper path does not exist.
   */
  public static class ZkPathNotFoundException extends Exception {
    public ZkPathNotFoundException(String zkPath) {
      super("zookeeper path does not exist: " + zkPath);
    }
  }
}
