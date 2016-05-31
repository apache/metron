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
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.EnrichmentConfigurations;

import java.io.IOException;

public abstract class ConfiguredEnrichmentBolt extends ConfiguredBolt {

  private static final Logger LOG = Logger.getLogger(ConfiguredEnrichmentBolt.class);

  protected final EnrichmentConfigurations configurations = new EnrichmentConfigurations();

  public ConfiguredEnrichmentBolt(String zookeeperUrl) {
    super(zookeeperUrl);
  }

  public EnrichmentConfigurations getConfigurations() {
    return configurations;
  }

  @Override
  public void loadConfig() {
    try {
      ConfigurationsUtils.updateEnrichmentConfigsFromZookeeper(configurations, client);
    } catch (Exception e) {
      LOG.warn("Unable to load configs from zookeeper, but the cache should load lazily...");
    }
  }

  @Override
  public void updateConfig(String path, byte[] data) throws IOException {
    if (data.length != 0) {
      String name = path.substring(path.lastIndexOf("/") + 1);
      if (path.startsWith(ConfigurationType.ENRICHMENT.getZookeeperRoot())) {
        configurations.updateSensorEnrichmentConfig(name, data);
        reloadCallback(name, ConfigurationType.ENRICHMENT);
      } else if (ConfigurationType.GLOBAL.getZookeeperRoot().equals(path)) {
        configurations.updateGlobalConfig(data);
        reloadCallback(name, ConfigurationType.GLOBAL);
      }
    }
  }
}
