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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.hadoop.fs.Path;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.ParseMessageRequest;
import org.apache.metron.rest.service.HdfsService;
import org.apache.metron.rest.service.SensorParserConfigService;
import org.apache.zookeeper.KeeperException;
import org.json.simple.JSONObject;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.metron.rest.MetronRestConstants.GROK_CLASS_NAME;
import static org.apache.metron.rest.MetronRestConstants.GROK_DEFAULT_PATH_SPRING_PROPERTY;
import static org.apache.metron.rest.MetronRestConstants.GROK_PATH_KEY;
import static org.apache.metron.rest.MetronRestConstants.GROK_PATTERN_LABEL_KEY;
import static org.apache.metron.rest.MetronRestConstants.GROK_STATEMENT_KEY;
import static org.apache.metron.rest.MetronRestConstants.GROK_TEMP_PATH_SPRING_PROPERTY;

@Service
public class SensorParserConfigServiceImpl implements SensorParserConfigService {

  @Autowired
  private Environment environment;

  @Autowired
  private ObjectMapper objectMapper;

  private CuratorFramework client;

  @Autowired
  public void setClient(CuratorFramework client) {
    this.client = client;
  }

  @Autowired
  private HdfsService hdfsService;

  private Map<String, String> availableParsers;

  public SensorParserConfig save(SensorParserConfig sensorParserConfig) throws RestException {
    String serializedConfig;
    if (isGrokConfig(sensorParserConfig)) {
      addGrokPathToConfig(sensorParserConfig);
      sensorParserConfig.getParserConfig().putIfAbsent(MetronRestConstants.GROK_PATTERN_LABEL_KEY, sensorParserConfig.getSensorTopic().toUpperCase());
      String statement = (String) sensorParserConfig.getParserConfig().remove(MetronRestConstants.GROK_STATEMENT_KEY);
      serializedConfig = serialize(sensorParserConfig);
      sensorParserConfig.getParserConfig().put(MetronRestConstants.GROK_STATEMENT_KEY, statement);
      saveGrokStatement(sensorParserConfig);
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
      if (isGrokConfig(sensorParserConfig)) {
        addGrokStatementToConfig(sensorParserConfig);
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
      if (isGrokConfig(sensorParserConfig)) {
        saveTemporaryGrokStatement(sensorParserConfig);
        sensorParserConfig.getParserConfig().put(MetronRestConstants.GROK_PATH_KEY, new File(getTemporaryGrokRootPath(), sensorParserConfig.getSensorTopic()).toString());
      }
      parser.configure(sensorParserConfig.getParserConfig());
      JSONObject results = parser.parse(parseMessageRequest.getSampleData().getBytes()).get(0);
      if (isGrokConfig(sensorParserConfig)) {
        deleteTemporaryGrokStatement(sensorParserConfig);
      }
      return results;
    }
  }

  private boolean isGrokConfig(SensorParserConfig sensorParserConfig) {
    return GROK_CLASS_NAME.equals(sensorParserConfig.getParserClassName());
  }

  private void addGrokStatementToConfig(SensorParserConfig sensorParserConfig) throws RestException {
    String grokStatement = "";
    String grokPath = (String) sensorParserConfig.getParserConfig().get(GROK_PATH_KEY);
    if (grokPath != null) {
      String fullGrokStatement = getGrokStatement(grokPath);
      String patternLabel = (String) sensorParserConfig.getParserConfig().get(GROK_PATTERN_LABEL_KEY);
      grokStatement = fullGrokStatement.replaceFirst(patternLabel + " ", "");
    }
    sensorParserConfig.getParserConfig().put(GROK_STATEMENT_KEY, grokStatement);
  }

  private void addGrokPathToConfig(SensorParserConfig sensorParserConfig) {
    if (sensorParserConfig.getParserConfig().get(GROK_PATH_KEY) == null) {
      String grokStatement = (String) sensorParserConfig.getParserConfig().get(GROK_STATEMENT_KEY);
      if (grokStatement != null) {
        sensorParserConfig.getParserConfig().put(GROK_PATH_KEY,
                new Path(environment.getProperty(GROK_DEFAULT_PATH_SPRING_PROPERTY), sensorParserConfig.getSensorTopic()).toString());
      }
    }
  }

  private String getGrokStatement(String path) throws RestException {
    try {
      return new String(hdfsService.read(new Path(path)));
    } catch (IOException e) {
      throw new RestException(e);
    }
  }

  private void saveGrokStatement(SensorParserConfig sensorParserConfig) throws RestException {
    saveGrokStatement(sensorParserConfig, false);
  }

  private void saveTemporaryGrokStatement(SensorParserConfig sensorParserConfig) throws RestException {
    saveGrokStatement(sensorParserConfig, true);
  }

  private void saveGrokStatement(SensorParserConfig sensorParserConfig, boolean isTemporary) throws RestException {
    String patternLabel = (String) sensorParserConfig.getParserConfig().get(GROK_PATTERN_LABEL_KEY);
    String grokPath = (String) sensorParserConfig.getParserConfig().get(GROK_PATH_KEY);
    String grokStatement = (String) sensorParserConfig.getParserConfig().get(GROK_STATEMENT_KEY);
    if (grokStatement != null) {
      String fullGrokStatement = patternLabel + " " + grokStatement;
      try {
        if (!isTemporary) {
          hdfsService.write(new Path(grokPath), fullGrokStatement.getBytes());
        } else {
          File grokDirectory = new File(getTemporaryGrokRootPath());
          if (!grokDirectory.exists()) {
            grokDirectory.mkdirs();
          }
          FileWriter fileWriter = new FileWriter(new File(grokDirectory, sensorParserConfig.getSensorTopic()));
          fileWriter.write(fullGrokStatement);
          fileWriter.close();
        }
      } catch (IOException e) {
        throw new RestException(e);
      }
    } else {
      throw new RestException("A grokStatement must be provided");
    }
  }

  private void deleteTemporaryGrokStatement(SensorParserConfig sensorParserConfig) {
    File file = new File(getTemporaryGrokRootPath(), sensorParserConfig.getSensorTopic());
    file.delete();
  }

  private String getTemporaryGrokRootPath() {
    String grokTempPath = environment.getProperty(GROK_TEMP_PATH_SPRING_PROPERTY);
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return new Path(grokTempPath, authentication.getName()).toString();
  }
}
