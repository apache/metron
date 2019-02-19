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
package org.apache.metron.common.zookeeper.configurations;

import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.Configurations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

/**
 * Handles update for an underlying Configurations object.  This is the base abstract implementation.
 * You will find system-specific implementations (e.g. IndexingUpdater, ParserUpdater, etc.) which
 * correspond to the various components of our system which accept configuration from zookeeper.
 *
 * @param <T> the Type of Configuration
 */
public abstract class ConfigurationsUpdater<T extends Configurations> implements Serializable {
  protected static final Logger LOG =  LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private Reloadable reloadable;
  private Supplier<T> configSupplier;

  /**
   * Construct a ConfigurationsUpdater.
   * @param reloadable A callback which gets called whenever a reload happens
   * @param configSupplier A Supplier which creates the Configurations object.
   */
  public ConfigurationsUpdater(Reloadable reloadable
                              , Supplier<T> configSupplier
  )
  {
    this.reloadable = reloadable;
    this.configSupplier = configSupplier;
  }

  /**
   * Update callback, this is called whenever a path is updated in zookeeper which we are monitoring.
   *
   * @param client The CuratorFramework
   * @param path The zookeeper path
   * @param data The change.
   * @throws IOException When update is impossible.
   */
  public void update(CuratorFramework client, String path, byte[] data) throws IOException {
    if (data.length != 0) {
      String name = path.substring(path.lastIndexOf("/") + 1);
      if (path.startsWith(getType().getZookeeperRoot())) {
        LOG.debug("Updating the {} config: {} -> {}", getType().name(), name, new String(data == null?"".getBytes():data));
        update(name, data);
        reloadCallback(name, getType());
      } else if (ConfigurationType.GLOBAL.getZookeeperRoot().equals(path)) {
        LOG.debug("Updating the global config: {}", new String(data == null?"".getBytes():data));
        getConfigurations().updateGlobalConfig(data);
        reloadCallback(name, ConfigurationType.GLOBAL);
      }
    }
  }

  /**
   * Delete callback, this is called whenever a path is deleted in zookeeper which we are monitoring.
   *
   * @param client The CuratorFramework
   * @param path The zookeeper path
   * @param data The change.
   * @throws IOException When update is impossible.
   */
  public void delete(CuratorFramework client, String path, byte[] data) throws IOException {
    String name = path.substring(path.lastIndexOf("/") + 1);
    if (path.startsWith(getType().getZookeeperRoot())) {
      LOG.debug("Deleting {} {} config from internal cache", getType().name(), name);
      delete(name);
      reloadCallback(name, getType());
    } else if (ConfigurationType.GLOBAL.getZookeeperRoot().equals(path)) {
      LOG.debug("Deleting global config from internal cache");
      getConfigurations().deleteGlobalConfig();
      reloadCallback(name, ConfigurationType.GLOBAL);
    }
  }

  /**
   * The ConfigurationsType that we're monitoring.
   * @return The ConfigurationsType enum
   */
  public abstract ConfigurationType getType();

  /**
   * The simple update.  This differs from the full update elsewhere in that
   * this is ONLY called on updates to path to the zookeeper nodes which correspond
   * to your configurations type (rather than all configurations type).
   * @param name The path
   * @param data The data updated
   * @throws IOException when update is unable to happen
   */
  public abstract void update(String name, byte[] data) throws IOException;

  /**
   * The simple delete.  This differs from the full delete elsewhere in that
   * this is ONLY called on deletes to path to the zookeeper nodes which correspond
   * to your configurations type (rather than all configurations type).
   * @param name the path
   */
  public abstract void delete(String name);

  /**
   * Gets the class for the {@link Configurations} type.
   *
   * @return The class
   */
  public abstract Class<T> getConfigurationClass();

  /**
   * This pulls the configuration from zookeeper and updates the cache.  It represents the initial state.
   * Force update is called when the zookeeper cache is initialized to ensure that the caches are updated.
   * @param client The ZK client interacting with configuration
   */
  public abstract void forceUpdate(CuratorFramework client);

  /**
   * Create an empty {@link Configurations} object of type T.
   * @return configurations
   */
  public abstract T defaultConfigurations();

  protected void reloadCallback(String name, ConfigurationType type) {
    reloadable.reloadCallback(name, type);
  }

  protected T getConfigurations() {
    return configSupplier.get();
  }

}
