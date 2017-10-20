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
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class EnrichmentUpdater extends ConfigurationsUpdater<EnrichmentConfigurations>{

  public EnrichmentUpdater(Reloadable reloadable, Supplier<EnrichmentConfigurations> configSupplier) {
    super(reloadable, configSupplier);
  }

  @Override
  public Class<EnrichmentConfigurations> getConfigurationClass() {
    return EnrichmentConfigurations.class;
  }

  @Override
  public void forceUpdate(CuratorFramework client) {
    try {
      ConfigurationsUtils.updateEnrichmentConfigsFromZookeeper(getConfigurations(), client);
    }
    catch (KeeperException.NoNodeException nne) {
      LOG.warn("No current enrichment configs in zookeeper, but the cache should load lazily...");
    }
    catch (Exception e) {
      LOG.warn("Unable to load configs from zookeeper, but the cache should load lazily...", e);
    }
  }

  @Override
  public EnrichmentConfigurations defaultConfigurations() {
    return new EnrichmentConfigurations();
  }

  @Override
  public ConfigurationType getType() {
    return ConfigurationType.ENRICHMENT;
  }

  @Override
  public void delete(String name) {
    getConfigurations().delete(name);
  }

  @Override
  public void update(String name, byte[] data) throws IOException {
    getConfigurations().updateSensorEnrichmentConfig(name, data);
  }

}
