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

import java.lang.invoke.MethodHandles;
import java.util.Map;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.Configurations;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.writer.ConfigurationStrategy;
import org.apache.metron.common.configuration.writer.ConfigurationsStrategies;
import org.apache.metron.zookeeper.SimpleEventListener;
import org.apache.metron.common.zookeeper.configurations.ConfigurationsUpdater;
import org.apache.metron.common.zookeeper.configurations.Reloadable;
import org.apache.metron.zookeeper.ZKCache;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.base.BaseRichBolt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Storm bolt that will manage configuration via ZooKeeper. A cache is maintained using an
 * {@link ZKCache}
 *
 * @param <CONFIG_T> The config type being used, e.g. {@link ParserConfigurations}
 */
public abstract class ConfiguredBolt<CONFIG_T extends Configurations> extends BaseRichBolt implements Reloadable {

  private static final Logger LOG =  LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private String zookeeperUrl;
  private String configurationStrategy;

  protected CuratorFramework client;
  protected ZKCache cache;
  private final CONFIG_T configurations;

  /**
   * Builds the bolt that knows where to communicate with ZooKeeper and which configuration this
   * bolt will be responsible for.
   *
   * @param zookeeperUrl A URL for ZooKeeper in the form host:port
   * @param configurationStrategy The configuration strategy to use, e.g. INDEXING or PROFILER
   */
  public ConfiguredBolt(String zookeeperUrl, String configurationStrategy) {
    this.zookeeperUrl = zookeeperUrl;
    this.configurationStrategy = configurationStrategy;
    this.configurations = createUpdater().defaultConfigurations();
  }

  public void setCuratorFramework(CuratorFramework client) {
    this.client = client;
  }

  public void setZKCache(ZKCache cache) {
    this.cache = cache;
  }

  @Override
  public void reloadCallback(String name, ConfigurationType type) {
  }

  public CONFIG_T getConfigurations() {
    return configurations;
  }

  protected ConfigurationStrategy<CONFIG_T> getConfigurationStrategy() {
    return ConfigurationsStrategies.valueOf(configurationStrategy);
  }

  protected ConfigurationsUpdater<CONFIG_T> createUpdater() {
    return getConfigurationStrategy().createUpdater(this, this::getConfigurations);
  }


  @Override
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    prepCache();
  }

  /**
   * Prepares the cache that will be used during Metron's interaction with ZooKeeper.
   */
  protected void prepCache() {
    try {
      if (client == null) {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        client = CuratorFrameworkFactory.newClient(zookeeperUrl, retryPolicy);
      }
      client.start();

      //this is temporary to ensure that any validation passes.
      //The individual bolt will reinitialize stellar to dynamically pull from
      //zookeeper.
      ConfigurationsUtils.setupStellarStatically(client);
      if (cache == null) {
        ConfigurationsUpdater<CONFIG_T> updater = createUpdater();
        SimpleEventListener listener = new SimpleEventListener.Builder()
                                                              .with( updater::update
                                                                   , TreeCacheEvent.Type.NODE_ADDED
                                                                   , TreeCacheEvent.Type.NODE_UPDATED
                                                                   )
                                                              .with( updater::delete
                                                                   , TreeCacheEvent.Type.NODE_REMOVED
                                                                   )
                                                              .build();
        cache = new ZKCache.Builder()
                           .withClient(client)
                           .withListener(listener)
                           .withRoot(Constants.ZOOKEEPER_TOPOLOGY_ROOT)
                           .build();
        updater.forceUpdate(client);
        cache.start();
      }
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void cleanup() {
    cache.close();
    client.close();
  }
}
