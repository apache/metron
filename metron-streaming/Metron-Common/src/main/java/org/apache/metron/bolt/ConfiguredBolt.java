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
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.log4j.Logger;
import org.apache.metron.Constants;
import org.apache.metron.domain.SourceConfig;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class ConfiguredBolt extends BaseRichBolt {

  private static final Logger LOG = Logger.getLogger(ConfiguredBolt.class);

  private String zookeeperUrl;

  protected Map<String, SourceConfig> configurations = Collections.synchronizedMap(new HashMap<String, SourceConfig>());
  private CuratorFramework client;
  private PathChildrenCache cache;

  public ConfiguredBolt(String zookeeperUrl) {
    this.zookeeperUrl = zookeeperUrl;
  }

  @Override
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    client = CuratorFrameworkFactory.newClient(zookeeperUrl, retryPolicy);
    client.start();
    cache = new PathChildrenCache(client, Constants.ZOOKEEPER_TOPOLOGY_ROOT, true);
    PathChildrenCacheListener listener = new PathChildrenCacheListener() {
      @Override
      public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_ADDED) || event.getType().equals(PathChildrenCacheEvent.Type.CHILD_UPDATED)) {
          byte[] data = event.getData().getData();
          if (data != null) {
            SourceConfig temp = SourceConfig.load(data);
            if (temp != null) {
              String[] path = event.getData().getPath().split("/");
              configurations.put(path[path.length - 1], temp);
            }
          }
        }
      }
    };
    cache.getListenable().addListener(listener);
    try {
      cache.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void cleanup() {
    try {
      cache.close();
      client.close();
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
    }
  }
}
