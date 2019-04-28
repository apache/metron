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

import com.google.common.collect.Iterables;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.*;
import org.apache.metron.common.configuration.profiler.ProfilerConfigurations;
import org.apache.metron.zookeeper.SimpleEventListener;
import org.apache.metron.zookeeper.ZKCache;
import org.apache.metron.common.zookeeper.configurations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class ZKConfigurationsCache implements ConfigurationsCache {
  private static final Logger LOG =  LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());



  private interface UpdaterSupplier {
    ConfigurationsUpdater<? extends Configurations> create(Map<Class<? extends Configurations>, Configurations> configs,Reloadable reloadCallback);
  }

  public enum ConfiguredTypes implements UpdaterSupplier {
    ENRICHMENT((c,r) -> new EnrichmentUpdater( r, createSupplier(EnrichmentConfigurations.class, c)))
    ,PARSER((c,r) -> new ParserUpdater( r, createSupplier(ParserConfigurations.class, c)))
    ,INDEXING((c,r) -> new IndexingUpdater( r, createSupplier(IndexingConfigurations.class, c)))
    ,PROFILER((c,r) -> new ProfilerUpdater( r, createSupplier(ProfilerConfigurations.class, c)))
    ;
    UpdaterSupplier updaterSupplier;
    ConfiguredTypes(UpdaterSupplier supplier) {
      this.updaterSupplier = supplier;
    }

    @Override
    public ConfigurationsUpdater<? extends Configurations>
    create(Map<Class<? extends Configurations>, Configurations> configs, Reloadable reloadCallback) {
      return updaterSupplier.create(configs, reloadCallback);
    }
  }

  private List<ConfigurationsUpdater< ? extends Configurations>> updaters;
  private final Map<Class<? extends Configurations>, Configurations> configs;
  private ZKCache cache;
  private CuratorFramework client;
  ReadWriteLock lock = new ReentrantReadWriteLock();

  /**
   * Constructor that sets up the cache.
   *
   * @param client The client to use for ZooKeeper communications
   * @param reloadable Callback for handling reloading
   * @param types The types to be handled by the cache
   */
  public ZKConfigurationsCache(CuratorFramework client, Reloadable reloadable, ConfiguredTypes... types) {
    updaters = new ArrayList<>();
    configs = new HashMap<>();
    this.client = client;
    for(ConfiguredTypes t : types) {
      ConfigurationsUpdater<? extends Configurations> updater = t.create(configs, reloadable);
      configs.put(updater.getConfigurationClass(), updater.defaultConfigurations());
      updaters.add(updater);
    }
  }

  public ZKConfigurationsCache(CuratorFramework client, Reloadable reloadable) {
    this(client, reloadable, ConfiguredTypes.values());
  }

  public ZKConfigurationsCache(CuratorFramework client, ConfiguredTypes... types) {
    this(client, (name, type) -> {}, types);
  }

  public ZKConfigurationsCache(CuratorFramework client) {
    this(client, ConfiguredTypes.values());
  }

  private static <T extends Configurations> Supplier<T> createSupplier(Class<T> clazz, Map<Class<? extends Configurations>, Configurations> configs) {
    return () -> clazz.cast(configs.get(clazz));
  }

  @Override
  public void start() {
    initializeCache(client);
  }

  @Override
  public void close() {
    Lock writeLock = lock.writeLock();
    try {
      writeLock.lock();
      if (cache != null) {
        cache.close();
      }
    }
    finally{
      writeLock.unlock();
    }
  }

  /**
   * Closes and reinitializes the instance.
   */
  public void reset() {
    Lock writeLock = lock.writeLock();
    try {
      writeLock.lock();
      close();
      initializeCache(client);
    }
    finally{
      writeLock.unlock();
    }
  }

  private void initializeCache(CuratorFramework client) {
    Lock writeLock = lock.writeLock();
    try {
      writeLock.lock();
      SimpleEventListener listener = new SimpleEventListener.Builder()
              .with(Iterables.transform(updaters, u -> u::update)
                      , TreeCacheEvent.Type.NODE_ADDED
                      , TreeCacheEvent.Type.NODE_UPDATED
              )
              .with(Iterables.transform(updaters, u -> u::delete)
                      , TreeCacheEvent.Type.NODE_REMOVED
              )
              .build();
      cache = new ZKCache.Builder()
              .withClient(client)
              .withListener(listener)
              .withRoot(Constants.ZOOKEEPER_TOPOLOGY_ROOT)
              .build();

      for (ConfigurationsUpdater<? extends Configurations> updater : updaters) {
        updater.forceUpdate(client);
      }
      cache.start();
    } catch (Exception e) {
      LOG.error("Unable to initialize zookeeper cache: " + e.getMessage(), e);
      throw new IllegalStateException("Unable to initialize zookeeper cache: " + e.getMessage(), e);
    }
    finally {
      writeLock.unlock();
    }
  }

  /**
   * Retrieves the {@link Configurations} for the provided config class.
   *
   * @param configClass The Configurations class to return
   * @param <T> The type parameter of the config class to return
   * @return The appropriate configurations class
   */
  public <T extends Configurations> T get(Class<T> configClass){
    Lock readLock = lock.readLock();
    try {
      readLock.lock();
      return configClass.cast(configs.get(configClass));
    }
    finally{
      readLock.unlock();
    }
  }


}
