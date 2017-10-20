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
package org.apache.metron.rest.service.impl;

import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.apache.metron.common.zookeeper.ConfigurationsCache;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.GlobalConfigService;
import org.apache.metron.common.zookeeper.ZKConfigurationsCache;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GlobalConfigServiceImpl implements GlobalConfigService {
    private CuratorFramework client;

    private ConfigurationsCache cache;

    @Autowired
    public GlobalConfigServiceImpl(CuratorFramework client, ConfigurationsCache cache) {
      this.client = client;
      this.cache = cache;
    }

    public void setCache(ConfigurationsCache cache) {
      this.cache = cache;
    }

    @Override
    public Map<String, Object> save(Map<String, Object> globalConfig) throws RestException {
      try {
        ConfigurationsUtils.writeGlobalConfigToZookeeper(globalConfig, client);
      } catch (Exception e) {
        throw new RestException(e);
      }
      return globalConfig;
    }

    @Override
    public Map<String, Object> get() throws RestException {
      Map<String, Object> globalConfig;
      try {
        EnrichmentConfigurations configs = cache.get( EnrichmentConfigurations.class);
        globalConfig = configs.getGlobalConfig(false);
      } catch (Exception e) {
        throw new RestException(e.getMessage(), e);
      }
      return globalConfig;
    }

    @Override
    public boolean delete() throws RestException {
        try {
            client.delete().forPath(ConfigurationType.GLOBAL.getZookeeperRoot());
        } catch (KeeperException.NoNodeException e) {
            return false;
        } catch (Exception e) {
          throw new RestException(e);
        }
        return true;
    }
}
