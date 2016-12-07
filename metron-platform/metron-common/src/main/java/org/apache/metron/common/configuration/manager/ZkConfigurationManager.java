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
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.utils.CloseableUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
   * A cache of the values stored in Zookeeper.
   */
  private TreeCache zookeeperCache;

  /**
   * The configuration values under management
   */
  private Map<String, byte[]> values;

  /**
   * The paths within Zookeeper that we care about.
   */
  private List<String> paths;

  /**
   * All configuration values must live under this root path.
   */
  private String rootPath;

  /**
   * @param zookeeperClient The client used to communicate with Zookeeper.  The client is not
   *                        closed.  It must be managed externally.
   */
  public ZkConfigurationManager(CuratorFramework zookeeperClient) {
    this(zookeeperClient, Constants.ZOOKEEPER_TOPOLOGY_ROOT);
  }

  /**
   * @param zookeeperClient The client used to communicate with Zookeeper.  The client is not
   *                        closed.  It must be managed externally.
   * @param rootPath        The root of all configuration paths in Zookeeper that should be
   *                        monitored for configuration values.
   */
  public ZkConfigurationManager(CuratorFramework zookeeperClient, String rootPath) {
    this.zookeeperClient = zookeeperClient;
    this.paths = new ArrayList<>();
    this.values = new ConcurrentHashMap<>();
    this.rootPath = rootPath;
  }

  /**
   * Define the paths within Zookeeper that contains configuration values that need managed.
   * @param zookeeperPath The Zookeeper path.
   */
  @Override
  public ZkConfigurationManager with(String zookeeperPath) {
    paths.add(zookeeperPath);
    return this;
  }

  /**
   * Open a connection to Zookeeper and retrieve the initial configuration value.
   */
  @Override
  public ZkConfigurationManager open() throws IOException {
    try {
      doOpen();
    } catch(Exception e) {
      throw new IOException(e);
    }

    return this;
  }

  private void doOpen() throws Exception {

    // the cache which will remain synced with zookeeper
    zookeeperCache = new TreeCache(zookeeperClient, rootPath);
    zookeeperCache.getListenable().addListener((client, event) -> {

      // is there data that was added or changed?
      if(event != null && event.getData() != null) {
        String pathAffected = event.getData().getPath();

        switch(event.getType()) {
          case NODE_ADDED:
          case NODE_UPDATED:
            paths.stream()
                    .filter(path -> path.equals(pathAffected))
                    .forEach(path -> values.put(path, event.getData().getData()));
            break;

          case NODE_REMOVED:
            paths.stream()
                    .filter(path -> path.equals(pathAffected))
                    .forEach(path -> values.remove(path));
            break;
        }
      }
    });

    // initialize the values
    for (String path : paths) {
      fetch(path).ifPresent(val -> values.put(path, val));
    }

    // start the cache
    zookeeperCache.start();
  }

  /**
   * Retrieve the configuration object.
   */
  @Override
  public <T> Optional<T> get(String key, Class<T> clazz) throws IOException {
    T result = null;

    byte[] val = values.get(key);
    if(isNotEmpty(val)) {
      result = deserialize(val, clazz);
    }

    return Optional.ofNullable(result);
  }

  /**
   * Close the configuration manager.
   *
   * Does not close the zookeeperClient that was passed in to the constructor.
   */
  @Override
  public void close() {
    CloseableUtils.closeQuietly(zookeeperCache);
  }

  /**
   * Retrieves the initial configuration value from Zookeeper
   * @param zookeeperPath The path in Zookeeper where the value is stored.
   */
  private Optional<byte[]> fetch(String zookeeperPath) throws Exception {
    byte[] result = null;

    if(zookeeperClient.checkExists().forPath(zookeeperPath) != null) {
      result = zookeeperClient.getData().forPath(zookeeperPath);
    }

    return Optional.ofNullable(result);
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
