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
package org.apache.metron.maas.service.runner;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.metron.maas.discovery.ServiceDiscoverer;
import org.apache.metron.maas.config.MaaSConfig;
import org.apache.metron.maas.util.ConfigUtil;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MaaSHandler {

  protected CuratorFramework client;
  protected NodeCache cache;
  protected MaaSConfig config;
  private String zkQuorum;
  private String root;
  private ObjectMapper _mapper = new ObjectMapper();
  protected ReadWriteLock lock = new ReentrantReadWriteLock();
  private ServiceDiscoverer discoverer;
  public MaaSHandler(String zkQuorum, String root) {
    this.zkQuorum = zkQuorum;
    this.root = root;
  }

  public MaaSConfig getConfig() {
    return config;
  }

  public CuratorFramework getClient() {
    return client;
  }



  public void start() throws Exception {
    if (client == null) {
      RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
      client = CuratorFrameworkFactory.newClient(zkQuorum, retryPolicy);
      client.start();
    }
    config = ConfigUtil.INSTANCE.read(client, root, new MaaSConfig(), MaaSConfig.class);
    cache = new NodeCache(client, root);
    cache.getListenable().addListener(() -> {
              byte[] data = cache.getCurrentData().getData();
              Lock wLock= lock.writeLock();
              wLock.lock();
              try {
                config = _mapper.readValue(data, MaaSConfig.class);
              }
              finally {
                wLock.unlock();
              }
            }
    );
    discoverer = new ServiceDiscoverer(client, config.getServiceRoot());
    discoverer.start();
  }
  public ServiceDiscoverer getDiscoverer() {
    return discoverer;
  }
}
