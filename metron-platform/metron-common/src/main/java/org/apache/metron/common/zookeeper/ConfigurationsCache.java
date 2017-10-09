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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public enum ConfigurationsCache {
  INSTANCE;

  private static final Logger LOG =  LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  List<ConfigurationsUpdater< ? extends Configurations>> updaters;
  Map<Class<? extends Configurations>, Configurations> configs;
  ZKCache cache;
  ReadWriteLock lock = new ReentrantReadWriteLock();

  ConfigurationsCache() {
    Reloadable noopReloadable = (name, type) -> { };
    updaters = new ArrayList<ConfigurationsUpdater< ? extends Configurations>>()
    {{
      add(new EnrichmentUpdater( noopReloadable, createSupplier(EnrichmentConfigurations.class)));
      add(new ParserUpdater( noopReloadable, createSupplier(ParserConfigurations.class)));
      add(new ProfilerUpdater( noopReloadable, createSupplier(ProfilerConfigurations.class)));
      add(new IndexingUpdater( noopReloadable, createSupplier(IndexingConfigurations.class)));
    }};
    configs = new HashMap<>();
    for(ConfigurationsUpdater<? extends Configurations> updater : updaters) {
      configs.put(updater.getConfigurationClass(), updater.defaultConfigurations());
    }
  }


  private <T extends Configurations> Supplier<T> createSupplier(Class<T> clazz) {
    return () -> clazz.cast(configs.get(clazz));
  }

  public <T extends Configurations> T get(CuratorFramework client, Class<T> configClass){
    Lock writeLock = lock.writeLock();
    try {
      writeLock.lock();
      if (cache == null) {
        for (ConfigurationsUpdater<? extends Configurations> updater : updaters) {
          updater.forceUpdate(client);
        }

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

        cache.start();
      }
    }
    catch(Exception ex) {
      LOG.error("Unable to set up zookeeper configuration cache: " + ex.getMessage(), ex);
      throw new IllegalStateException("Unable to set up zookeeper configuration cache: " + ex.getMessage(), ex);
    }
    finally {
      writeLock.unlock();
    }

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
