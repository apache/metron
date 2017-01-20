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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.ParseMessageRequest;
import org.apache.zookeeper.KeeperException;
import org.json.simple.JSONObject;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
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

  private Map<String, String> availableParsers;

  public SensorParserConfig save(SensorParserConfig sensorParserConfig) throws RestException {
    String serializedConfig;
    if (grokService.isGrokConfig(sensorParserConfig)) {
      grokService.addGrokPathToConfig(sensorParserConfig);
      sensorParserConfig.getParserConfig().putIfAbsent(GrokService.GROK_PATTERN_LABEL_KEY, sensorParserConfig.getSensorTopic().toUpperCase());
      String statement = (String) sensorParserConfig.getParserConfig().remove(GrokService.GROK_STATEMENT_KEY);
      serializedConfig = serialize(sensorParserConfig);
      sensorParserConfig.getParserConfig().put(GrokService.GROK_STATEMENT_KEY, statement);
      grokService.saveGrokStatement(sensorParserConfig);
    } else {
      serializedConfig = serialize(sensorParserConfig);
    }
    try {
      ConfigurationsUtils.writeSensorParserConfigToZookeeper(sensorParserConfig.getSensorTopic(), serializedConfig.getBytes(), client);
    } catch (Exception e) {
      throw new RestException(e);
    }
    return sensorParserConfig;
  }

  private String serialize(SensorParserConfig sensorParserConfig) throws RestException {
    String serializedConfig;
    try {
      serializedConfig = objectMapper.writeValueAsString(sensorParserConfig);
    } catch (JsonProcessingException e) {
      throw new RestException("Could not serialize SensorParserConfig", "Could not serialize " + sensorParserConfig.toString(), e.getCause());
    }
    return serializedConfig;
  }

  public SensorParserConfig findOne(String name) throws RestException {
    SensorParserConfig sensorParserConfig;
    try {
      sensorParserConfig = ConfigurationsUtils.readSensorParserConfigFromZookeeper(name, client);
      if (grokService.isGrokConfig(sensorParserConfig)) {
        grokService.addGrokStatementToConfig(sensorParserConfig);
      }
    } catch (KeeperException.NoNodeException e) {
      return null;
    } catch (Exception e) {
      throw new RestException(e);
    }
    return sensorParserConfig;
  }

  public Iterable<SensorParserConfig> getAll() throws RestException {
    List<SensorParserConfig> sensorParserConfigs = new ArrayList<>();
    List<String> sensorNames = getAllTypes();
    for (String name : sensorNames) {
      sensorParserConfigs.add(findOne(name));
    }
    return sensorParserConfigs;
  }

  public boolean delete(String name) throws RestException {
    try {
      client.delete().forPath(ConfigurationType.PARSER.getZookeeperRoot() + "/" + name);
    } catch (KeeperException.NoNodeException e) {
      return false;
    } catch (Exception e) {
      throw new RestException(e);
    }
    return true;
  }

  public List<String> getAllTypes() throws RestException {
    List<String> types;
    try {
      types = client.getChildren().forPath(ConfigurationType.PARSER.getZookeeperRoot());
    } catch (KeeperException.NoNodeException e) {
      types = new ArrayList<>();
    } catch (Exception e) {
      throw new RestException(e);
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

  public JSONObject parseMessage(ParseMessageRequest parseMessageRequest) throws RestException {
    SensorParserConfig sensorParserConfig = parseMessageRequest.getSensorParserConfig();
    if (sensorParserConfig == null) {
      throw new RestException("SensorParserConfig is missing from ParseMessageRequest");
    } else if (sensorParserConfig.getParserClassName() == null) {
      throw new RestException("SensorParserConfig must have a parserClassName");
    } else {
      MessageParser<JSONObject> parser = null;
      try {
        parser = (MessageParser<JSONObject>) Class.forName(sensorParserConfig.getParserClassName()).newInstance();
      } catch (Exception e) {
        throw new RestException(e.toString(), e.getCause());
      }
      if (grokService.isGrokConfig(sensorParserConfig)) {
        grokService.saveTemporaryGrokStatement(sensorParserConfig);
        sensorParserConfig.getParserConfig().put(GrokService.GROK_PATH_KEY, new File(grokService.getTemporaryGrokRootPath(), sensorParserConfig.getSensorTopic()).toString());
      }
      parser.configure(sensorParserConfig.getParserConfig());
      JSONObject results = parser.parse(parseMessageRequest.getSampleData().getBytes()).get(0);
      if (grokService.isGrokConfig(sensorParserConfig)) {
        grokService.deleteTemporaryGrokStatement(sensorParserConfig);
      }
      return results;
    }
  }
}
