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
package org.apache.metron.common.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.base.BaseRichBolt;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.log4j.Logger;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.Configurations;

import java.io.IOException;
import java.util.Map;

public abstract class ConfiguredBolt<CONFIG_T extends Configurations> extends BaseRichBolt {

  private static final Logger LOG = Logger.getLogger(ConfiguredBolt.class);

  private String zookeeperUrl;

  protected CuratorFramework client;
  protected TreeCache cache;
  private final CONFIG_T configurations = defaultConfigurations();
  public ConfiguredBolt(String zookeeperUrl) {
    this.zookeeperUrl = zookeeperUrl;
  }

  public void setCuratorFramework(CuratorFramework client) {
    this.client = client;
  }

  public void setTreeCache(TreeCache cache) {
    this.cache = cache;
  }

  public void reloadCallback(String name, ConfigurationType type) {
  }
  public CONFIG_T getConfigurations() {
    return configurations;
  }
  protected abstract CONFIG_T defaultConfigurations();

  @Override
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    try {
      if (client == null) {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        client = CuratorFrameworkFactory.newClient(zookeeperUrl, retryPolicy);
      }
      client.start();
      if (cache == null) {
        cache = new TreeCache(client, Constants.ZOOKEEPER_TOPOLOGY_ROOT);
        TreeCacheListener listener = new TreeCacheListener() {
          @Override
          public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
            if (event.getType().equals(TreeCacheEvent.Type.NODE_ADDED) || event.getType().equals(TreeCacheEvent.Type.NODE_UPDATED)) {
              String path = event.getData().getPath();
              byte[] data = event.getData().getData();
              updateConfig(path, data);
            }
          }
        };
        cache.getListenable().addListener(listener);
        loadConfig();
      }
      cache.start();
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  abstract public void loadConfig();
  abstract public void updateConfig(String path, byte[] data) throws IOException;

  @Override
  public void cleanup() {
    cache.close();
    client.close();
  }
}
