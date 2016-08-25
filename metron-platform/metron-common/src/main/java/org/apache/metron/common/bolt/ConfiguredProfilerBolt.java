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

import org.apache.log4j.Logger;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.common.configuration.profiler.ProfilerConfigurations;

import java.io.IOException;

/**
 * A bolt used in the Profiler topology that is configured with values stored in Zookeeper.
 */
public abstract class ConfiguredProfilerBolt extends ConfiguredBolt<ProfilerConfigurations> {

  private static final Logger LOG = Logger.getLogger(ConfiguredProfilerBolt.class);
  protected final ProfilerConfigurations configurations = new ProfilerConfigurations();

  public ConfiguredProfilerBolt(String zookeeperUrl) {
    super(zookeeperUrl);
  }

  protected ProfilerConfig getProfilerConfig() {
    return getConfigurations().getProfilerConfig();
  }

  @Override
  protected ProfilerConfigurations defaultConfigurations() {
    return new ProfilerConfigurations();
  }

  @Override
  public void loadConfig() {
    try {
      ConfigurationsUtils.updateProfilerConfigsFromZookeeper(getConfigurations(), client);
    } catch (Exception e) {
      LOG.warn("Unable to load configs from zookeeper, but the cache should load lazily...");
    }
  }

  @Override
  public void updateConfig(String path, byte[] data) throws IOException {
    if (data.length != 0) {
      String name = path.substring(path.lastIndexOf("/") + 1);

      // update the profiler configuration from zookeeper
      if (path.startsWith(ConfigurationType.PROFILER.getZookeeperRoot())) {
        getConfigurations().updateProfilerConfig(data);
        reloadCallback(name, ConfigurationType.PROFILER);

      // update the global configuration from zookeeper
      } else if (ConfigurationType.GLOBAL.getZookeeperRoot().equals(path)) {
        getConfigurations().updateGlobalConfig(data);
        reloadCallback(name, ConfigurationType.GLOBAL);
      }
    }
  }
}
