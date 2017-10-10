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

import static org.apache.metron.common.configuration.ConfigurationType.PROFILER;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.common.configuration.profiler.ProfilerConfigurations;
import org.apache.metron.common.utils.JSONUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ProfilerUpdater extends ConfigurationsUpdater<ProfilerConfigurations> {
  public ProfilerUpdater(Reloadable reloadable, Supplier<ProfilerConfigurations> configSupplier) {
    super(reloadable, configSupplier);
  }

  @Override
  public Class<ProfilerConfigurations> getConfigurationClass() {
    return ProfilerConfigurations.class;
  }

  private ProfilerConfig readFromZookeeper(CuratorFramework client) throws Exception {
    byte[] raw = client.getData().forPath(PROFILER.getZookeeperRoot());
    return JSONUtils.INSTANCE.load(new ByteArrayInputStream(raw), ProfilerConfig.class);
  }

  @Override
  public void forceUpdate(CuratorFramework client) {
    try {
      ConfigurationsUtils.updateConfigsFromZookeeper(getConfigurations(), client);
    }
    catch(Exception e) {
      LOG.warn("Unable to load global configs from zookeeper, but the cache should load lazily...", e);
    }
    try {
      ProfilerConfig config = readFromZookeeper(client);
      if(config != null) {
        getConfigurations().updateProfilerConfig(config);
      }

    } catch (Exception e) {
      LOG.warn("Unable to load profiler configs from zookeeper, but the cache should load lazily...", e);
    }
  }

  @Override
  public ProfilerConfigurations defaultConfigurations() {
    return new ProfilerConfigurations();
  }

  @Override
  public void delete(CuratorFramework client, String path, byte[] data) throws IOException {
    String name = path.substring(path.lastIndexOf("/") + 1);
    if (path.startsWith(ConfigurationType.PROFILER.getZookeeperRoot())) {
      LOG.debug("Deleting profiler config from internal cache");
      getConfigurations().delete();
      reloadCallback(name, ConfigurationType.PROFILER);
    } else if (ConfigurationType.GLOBAL.getZookeeperRoot().equals(path)) {
      LOG.debug("Deleting global config from internal cache");
      getConfigurations().deleteGlobalConfig();
      reloadCallback(name, ConfigurationType.GLOBAL);
    }
  }

  @Override
  public void update(CuratorFramework client, String path, byte[] data) throws IOException {
    if (data.length != 0) {
      String name = path.substring(path.lastIndexOf("/") + 1);

      // update the profiler configuration from zookeeper
      if (path.startsWith(ConfigurationType.PROFILER.getZookeeperRoot())) {
        LOG.debug("Updating the profiler config: {}", new String(data == null?"".getBytes():data));
        getConfigurations().updateProfilerConfig(data);
        reloadCallback(name, ConfigurationType.PROFILER);

      // update the global configuration from zookeeper
      } else if (ConfigurationType.GLOBAL.getZookeeperRoot().equals(path)) {
        LOG.debug("Updating the global config: {}", new String(data == null?"".getBytes():data));
        getConfigurations().updateGlobalConfig(data);
        reloadCallback(name, ConfigurationType.GLOBAL);
      }
    }
  }
}
