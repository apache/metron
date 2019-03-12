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

import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserGroup;
import org.apache.metron.common.zookeeper.ConfigurationsCache;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.GlobalConfigService;
import org.apache.metron.rest.service.SensorParserConfigService;
import org.apache.metron.rest.service.SensorParserGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class SensorParserGroupServiceImpl implements SensorParserGroupService {

  private ConfigurationsCache cache;
  private GlobalConfigService globalConfigService;
  private SensorParserConfigService sensorParserConfigService;

  @Autowired
  public SensorParserGroupServiceImpl(ConfigurationsCache cache, GlobalConfigService globalConfigService, SensorParserConfigService sensorParserConfigService) {
    this.cache = cache;
    this.globalConfigService = globalConfigService;
    this.sensorParserConfigService = sensorParserConfigService;
  }

  /**
   * Saves a SensorParserGroup in Zookeeper.  Checks for various error conditions including empty sensors field, missing
   * configs for each sensor and sensors already included in another group.
   * @param sensorParserGroup
   * @return
   * @throws RestException
   */
  @Override
  public SensorParserGroup save(SensorParserGroup sensorParserGroup) throws RestException {
    ParserConfigurations parserConfigurations = cache.get( ParserConfigurations.class);
    Map<String, SensorParserGroup> groups = new HashMap<>(parserConfigurations.getSensorParserGroups());
    groups.remove(sensorParserGroup.getName());

    if (sensorParserGroup.getSensors().size() == 0) {
      throw new RestException("A parser group must contain sensors");
    }

    for(String sensor: sensorParserGroup.getSensors()) {
      // check if sensor config exists
      if (sensorParserConfigService.findOne(sensor) == null) {
        throw new RestException(String.format("Could not find config for sensor %s", sensor));
      }
      // check if sensor is in another group
      for (SensorParserGroup group : groups.values()) {
        Set<String> groupSensors = group.getSensors();
        if (groupSensors.contains(sensor)) {
          throw new RestException(String.format("Sensor %s is already in group %s", sensor, group.getName()));
        }
      }
    }
    groups.put(sensorParserGroup.getName(), sensorParserGroup);
    saveGroups(parserConfigurations, new HashSet<>(groups.values()));
    return sensorParserGroup;
  }

  /**
   * Retrieves a single SensorParserGroup by name.
   * @param name SensorParserGroup name
   * @return SensorParserGroup or null if group is missing
   */
  @Override
  public SensorParserGroup findOne(String name) {
    return getAll().get(name);
  }

  /**
   * Retrieves all SensorParserGroups as a Map with key being the SensorParserGroup name
   * @return All SensorParserGroups
   */
  @Override
  public Map<String, SensorParserGroup> getAll() {
    ParserConfigurations configs = cache.get( ParserConfigurations.class);
    return configs.getSensorParserGroups();
  }

  /**
   * Deletes a SensorParserGroup from Zookeeper.
   * @param name SensorParserGroup name
   * @return True if a SensorParserGroup was deleted
   * @throws RestException Writing to Zookeeper resulted in an error
   */
  @Override
  public boolean delete(String name) throws RestException {
    ParserConfigurations parserConfigurations = cache.get( ParserConfigurations.class);
    Map<String, SensorParserGroup> groups = parserConfigurations.getSensorParserGroups();
    boolean deleted = false;
    if (groups.containsKey(name)) {
      groups.remove(name);
      saveGroups(parserConfigurations, new HashSet<>(groups.values()));
      deleted = true;
    }
    return deleted;
  }

  /**
   * Saves SensorParserGroups in Zookeeper.
   * @param parserConfigurations ParserConfigurations
   * @param groups SensorParserGroups
   * @throws RestException Writing to Zookeeper resulted in an error
   */
  private void saveGroups(ParserConfigurations parserConfigurations, Collection<SensorParserGroup> groups) throws RestException {
    Map<String, Object> globalConfig = parserConfigurations.getGlobalConfig(true);
    globalConfig.put(ParserConfigurations.PARSER_GROUPS_CONF, groups);
    globalConfigService.save(globalConfig);
  }

}
