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
package org.apache.metron.bolt;

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
import org.apache.metron.Constants;
import org.apache.metron.domain.Configurations;
import org.apache.metron.utils.ConfigurationsUtils;

import java.io.IOException;
import java.util.Map;

public abstract class ConfiguredBolt extends BaseRichBolt {

  private static final Logger LOG = Logger.getLogger(ConfiguredBolt.class);

  private String zookeeperUrl;
  private long timeout = Constants.DEFAULT_CONFIGURED_BOLT_TIMEOUT;

  protected final Configurations configurations = new Configurations();
  private CuratorFramework client;
  private TreeCache cache;

  public ConfiguredBolt(String zookeeperUrl) {
    this.zookeeperUrl = zookeeperUrl;
  }

  protected void reloadCallback() {
  }

  public ConfiguredBolt withTimeout(long timeout) {
    this.timeout = timeout;
    return this;
  }

  @Override
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    client = CuratorFrameworkFactory.newClient(zookeeperUrl, retryPolicy);
    client.start();
    try {
      ConfigurationsUtils.updateConfigsFromZookeeper(configurations, client);
      cache = new TreeCache(client, Constants.ZOOKEEPER_TOPOLOGY_ROOT);
      TreeCacheListener listener = new TreeCacheListener() {
        @Override
        public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
          if (event.getType().equals(TreeCacheEvent.Type.NODE_ADDED) || event.getType().equals(TreeCacheEvent.Type.NODE_UPDATED)) {
            String path = event.getData().getPath();
            byte[] data = event.getData().getData();
            updateConfig(path, data);
            reloadCallback();
          }
        }
      };
      cache.getListenable().addListener(listener);
      try {
        ConfigurationsUtils.updateConfigsFromZookeeper(configurations, client);
      }
      catch(Exception e) {
        LOG.warn("Unable to load configs from zookeeper, but the cache should load lazily...");
      }

      cache.start();
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  public void updateConfig(String path, byte[] data) throws IOException {
    if (data.length != 0 && path != null) {
      String name = path.substring(path.lastIndexOf("/") + 1);
      if (path.startsWith(Constants.ZOOKEEPER_SENSOR_ROOT)) {
        configurations.updateSensorEnrichmentConfig(name, data);
      } else if (Constants.ZOOKEEPER_GLOBAL_ROOT.equals(path)) {
        configurations.updateGlobalConfig(data);
      } else {
        configurations.updateConfig(name, data);
      }
    }
  }

  @Override
  public void cleanup() {
    cache.close();
    client.close();
  }
}
