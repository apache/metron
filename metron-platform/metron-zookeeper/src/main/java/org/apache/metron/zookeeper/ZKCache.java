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
package org.apache.metron.zookeeper;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A zookeeper cache that composes the Curator TreeCache.  This is the basic point of
 * abstraction to interact with metron configuration in Zookeeper.
 */
public class ZKCache implements AutoCloseable{
  private static final Logger LOG =  LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final int DEFAULT_CLIENT_SLEEP_MS = 1000;
  public static final int DEFAULT_MAX_RETRIES = 3;


  /**
   * Build a ZKCache instance.
   */
  public static class Builder {
    private Optional<CuratorFramework> client = Optional.empty();
    private boolean ownClient = false;
    private List<TreeCacheListener> listener = new ArrayList<>();
    private String zkRoot;

    public Builder() { }

    /**
     * Specify your own client.  If you specify this, closing will not close your Client.
     * If a client is not passed in, then one is created and will be closed when the ZKCache
     * closes.
     * @param client The CuratorFramework client.
     * @return The Builder
     */
    public Builder withClient(CuratorFramework client) {
      this.client = Optional.ofNullable(client);
      ownClient = false;
      return this;
    }

    /**
     * Specify your own zookeeper URL.  If you pass this in, the ZKCache will own the client
     * and it will be closed when the ZKCache is closed.
     *
     * @param zookeeperUrl The zookeeper quorum
     * @return The Builder
     */
    public Builder withClient(String zookeeperUrl) {
      this.client = Optional.ofNullable(createClient(zookeeperUrl, Optional.empty()));
      ownClient = true;
      return this;
    }

    /**
     * Specify your own zookeeper URL.  If you pass this in, the ZKCache will own the client
     * and it will be closed when the ZKCache is closed.
     *
     * @param zookeeperUrl The zookeeper quorum
     * @param retryPolicy The RetryPolicy to use
     * @return The Builder
     */
    public Builder withClient(String zookeeperUrl, RetryPolicy retryPolicy) {
      this.client = Optional.ofNullable(createClient(zookeeperUrl, Optional.ofNullable(retryPolicy)));
      ownClient = true;
      return this;
    }

    /**
     * Specify the treecache listener, which will be called when changes happen to the zookeeper root.
     *
     * @param listener The callback which is called when changes happen in zookeeper.
     * @return The Builder
     */
    public Builder withListener(TreeCacheListener listener) {
      this.listener.add(listener);
      return this;
    }

    /**
     * Specify the root in zookeeper to monitor.
     * @param zkRoot The root path in zookeeper
     * @return The Builder
     */
    public Builder withRoot(String zkRoot) {
      this.zkRoot = zkRoot;
      return this;
    }

    /**
     * Create the ZKCache object based on the config passed in the Builder.
     * @return The ZKCache
     */
    public ZKCache build() {
      if(!client.isPresent()) {
        throw new IllegalArgumentException("Zookeeper client must be specified.");
      }
      if(listener.isEmpty()) {
        LOG.warn("Zookeeper listener is null or empty, which is very likely an error.");
      }
      if(zkRoot == null) {
        throw new IllegalArgumentException("Zookeeper root must not be null.");
      }
      return new ZKCache(client.get(), listener, zkRoot, ownClient);
    }

  }

  private CuratorFramework client;
  private List<TreeCacheListener> listeners;
  private TreeCache cache;
  private String zkRoot;
  private boolean ownClient = false;

  private ZKCache(CuratorFramework client, List<TreeCacheListener> listeners, String zkRoot, boolean ownClient) {
    this.client = client;
    this.listeners = listeners;
    this.ownClient = ownClient;
    if(zkRoot == null) {
      throw new IllegalArgumentException("Zookeeper root must not be null.");
    }
    this.zkRoot = zkRoot;
  }

  /**
   * Return the client used.
   * NOTE: DO NOT CLOSE THIS CLIENT OUT OF BAND.
   * @return The Curator Client
   */
  public CuratorFramework getClient() {
    return client;
  }


  /**
   * Start the cache.
   * @throws Exception If unable to be started.
   */
  public void start() throws Exception {
    if(cache == null) {
      if(ownClient) {
        client.start();
      }
      TreeCache.Builder builder = TreeCache.newBuilder(client, zkRoot);
      builder.setCacheData(true);
      cache = builder.build();
      for(TreeCacheListener l : listeners) {
        cache.getListenable().addListener(l);
      }
      cache.start();
    }
  }

  /**
   * Close the cache, which closes the client if it's owned by the ZKCache.
   */
  @Override
  public void close() {
    cache.close();
    if(ownClient) {
      client.close();
    }
  }

  public static CuratorFramework createClient(String zookeeperUrl, Optional<RetryPolicy> retryPolicy) {
    return CuratorFrameworkFactory.newClient(zookeeperUrl, retryPolicy.orElse(new ExponentialBackoffRetry(DEFAULT_CLIENT_SLEEP_MS, DEFAULT_MAX_RETRIES)));
  }


}
