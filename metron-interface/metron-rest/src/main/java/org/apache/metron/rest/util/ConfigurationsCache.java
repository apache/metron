package org.apache.metron.rest.util;

import com.google.common.collect.Iterables;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.*;
import org.apache.metron.common.configuration.profiler.ProfilerConfigurations;
import org.apache.metron.common.zookeeper.SimpleEventListener;
import org.apache.metron.common.zookeeper.ZKCache;
import org.apache.metron.common.zookeeper.configurations.*;

import java.io.IOException;
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

  public <T extends Configurations> T get(CuratorFramework client, Class<T> configClass) {
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
