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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.SensorIndexingConfigService;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SensorIndexingConfigServiceImpl implements SensorIndexingConfigService {

  private ObjectMapper objectMapper;

  private CuratorFramework client;

  @Autowired
  public SensorIndexingConfigServiceImpl(ObjectMapper objectMapper, CuratorFramework client) {
    this.objectMapper = objectMapper;
    this.client = client;
  }

  @Override
  public Map<String, Object> save(String name, Map<String, Object> sensorIndexingConfig) throws RestException {
    try {
      ConfigurationsUtils.writeSensorIndexingConfigToZookeeper(name, objectMapper.writeValueAsString(sensorIndexingConfig).getBytes(), client);
    } catch (Exception e) {
      throw new RestException(e);
    }
    return sensorIndexingConfig;
  }

  @Override
  public Map<String, Object> findOne(String name) throws RestException {
    Map<String, Object> sensorIndexingConfig;
    try {
      byte[] sensorIndexingConfigBytes = ConfigurationsUtils.readSensorIndexingConfigBytesFromZookeeper(name, client);
      sensorIndexingConfig = JSONUtils.INSTANCE.load(new ByteArrayInputStream(sensorIndexingConfigBytes), new TypeReference<Map<String, Object>>(){});
    } catch (KeeperException.NoNodeException e) {
      return null;
    } catch (Exception e) {
      throw new RestException(e);
    }
    return sensorIndexingConfig;
  }

  @Override
  public Map<String, Map<String, Object>> getAll() throws RestException {
    Map<String, Map<String, Object>> sensorIndexingConfigs = new HashMap<>();
    List<String> sensorNames = getAllTypes();
    for (String name : sensorNames) {
      sensorIndexingConfigs.put(name, findOne(name));
    }
    return sensorIndexingConfigs;
  }

  @Override
  public List<String> getAllTypes() throws RestException {
    List<String> types;
    try {
        types = client.getChildren().forPath(ConfigurationType.INDEXING.getZookeeperRoot());
    } catch (KeeperException.NoNodeException e) {
        types = new ArrayList<>();
    } catch (Exception e) {
      throw new RestException(e);
    }
    return types;
  }

  @Override
  public boolean delete(String name) throws RestException {
    try {
        client.delete().forPath(ConfigurationType.INDEXING.getZookeeperRoot() + "/" + name);
    } catch (KeeperException.NoNodeException e) {
        return false;
    } catch (Exception e) {
      throw new RestException(e);
    }
    return true;
  }

}
