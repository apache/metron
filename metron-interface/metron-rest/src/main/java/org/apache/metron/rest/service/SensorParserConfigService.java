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
package org.apache.metron.rest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.rest.model.ParseMessageRequest;
import org.apache.metron.rest.model.SensorParserConfigVersion;
import org.apache.metron.rest.repository.SensorParserConfigVersionRepository;
import org.apache.zookeeper.KeeperException;
import org.json.simple.JSONObject;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SensorParserConfigService {

  @Autowired
  private ObjectMapper objectMapper;

  private CuratorFramework client;

  @Autowired
  public void setClient(CuratorFramework client) {
    this.client = client;
  }

  @Autowired
  private GrokService grokService;

  @Autowired
  private SensorParserConfigVersionRepository sensorParserRepository;

  private Map<String, String> availableParsers;

  public SensorParserConfig save(SensorParserConfig sensorParserConfig) throws Exception {
    String serializedConfig;
    if (grokService.isGrokConfig(sensorParserConfig)) {
      grokService.addGrokPathToConfig(sensorParserConfig);
      String statement = (String) sensorParserConfig.getParserConfig().remove(GrokService.GROK_STATEMENT_KEY);
      serializedConfig = objectMapper.writeValueAsString(sensorParserConfig);
      ConfigurationsUtils.writeSensorParserConfigToZookeeper(sensorParserConfig.getSensorTopic(), serializedConfig.getBytes(), client);
      sensorParserConfig.getParserConfig().put(GrokService.GROK_STATEMENT_KEY, statement);
      sensorParserConfig.getParserConfig().put(GrokService.GROK_PATTERN_LABEL_KEY, sensorParserConfig.getSensorTopic().toUpperCase());
      grokService.saveGrokStatement(sensorParserConfig);
    } else {
      serializedConfig = objectMapper.writeValueAsString(sensorParserConfig);
      ConfigurationsUtils.writeSensorParserConfigToZookeeper(sensorParserConfig.getSensorTopic(), serializedConfig.getBytes(), client);
    }
    saveVersion(sensorParserConfig.getSensorTopic(), serializedConfig);
    return sensorParserConfig;
  }

  private void saveVersion(String name, String config) {
    SensorParserConfigVersion sensorParser = new SensorParserConfigVersion();
    sensorParser.setName(name);
    sensorParser.setConfig(config);
    sensorParserRepository.save(sensorParser);
  }

  public SensorParserConfig findOne(String name) throws Exception{
    SensorParserConfig sensorParserConfig = null;
    try {
      sensorParserConfig = ConfigurationsUtils.readSensorParserConfigFromZookeeper(name, client);
      if (grokService.isGrokConfig(sensorParserConfig)) {
        grokService.addGrokStatementToConfig(sensorParserConfig);
      }
    } catch (KeeperException.NoNodeException e) {
    }
    return sensorParserConfig;
  }

  public Iterable<SensorParserConfig> getAll() throws Exception {
    List<SensorParserConfig> sensorParserConfigs = new ArrayList<>();
    List<String> sensorNames = getAllTypes();
    for (String name : sensorNames) {
      sensorParserConfigs.add(findOne(name));
    }
    return sensorParserConfigs;
  }

  public boolean delete(String name) throws Exception {
    try {
      client.delete().forPath(ConfigurationType.PARSER.getZookeeperRoot() + "/" + name);
      sensorParserRepository.delete(name);
    } catch (KeeperException.NoNodeException e) {
      return false;
    }
    return true;
  }

  public List<String> getAllTypes() throws Exception {
    List<String> types;
    try {
      types = client.getChildren().forPath(ConfigurationType.PARSER.getZookeeperRoot());
    } catch (KeeperException.NoNodeException e) {
      types = new ArrayList<>();
    }
    return types;
  }

  public Map<String, String> getAvailableParsers() {
    if (availableParsers == null) {
      availableParsers = new HashMap<>();
      Set<Class<? extends MessageParser>> parserClasses = getParserClasses();
      parserClasses.forEach(parserClass -> {
        if (!"BasicParser".equals(parserClass.getSimpleName())) {
          availableParsers.put(parserClass.getSimpleName().replaceAll("Basic|Parser", ""), parserClass.getName());
        }
      });
    }
    return availableParsers;
  }

  public Map<String, String> reloadAvailableParsers() {
    availableParsers = null;
    return getAvailableParsers();
  }

  private Set<Class<? extends MessageParser>> getParserClasses() {
    Reflections reflections = new Reflections("org.apache.metron.parsers");
    return reflections.getSubTypesOf(MessageParser.class);
  }

  public JSONObject parseMessage(ParseMessageRequest parseMessageRequest) throws Exception {
    SensorParserConfig sensorParserConfig = parseMessageRequest.getSensorParserConfig();
    if (sensorParserConfig == null) {
      throw new Exception("Could not find parser config");
    } else if (sensorParserConfig.getParserClassName() == null) {
      throw new Exception("Could not find parser class name");
    } else {
      MessageParser<JSONObject> parser = (MessageParser<JSONObject>) Class.forName(sensorParserConfig.getParserClassName()).newInstance();
      sensorParserConfig.getParserConfig().put(GrokService.GROK_PATTERN_LABEL_KEY, sensorParserConfig.getSensorTopic().toUpperCase());
      sensorParserConfig.getParserConfig().put(GrokService.GROK_PATH_KEY, grokService.getTempGrokPath(sensorParserConfig.getSensorTopic()).toString());
      grokService.saveTemporaryGrokStatement(sensorParserConfig);
      parser.configure(sensorParserConfig.getParserConfig());
      JSONObject results = parser.parse(parseMessageRequest.getSampleData().getBytes()).get(0);
      grokService.deleteTemporaryGrokStatement(sensorParserConfig);
      return results;
    }
  }
}
