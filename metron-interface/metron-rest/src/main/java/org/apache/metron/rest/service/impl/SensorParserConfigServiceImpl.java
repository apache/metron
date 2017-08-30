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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.bundles.BundleSystem;
import org.apache.metron.bundles.NotInitializedException;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.parsers.grok.GrokParser;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.config.BundleSystemConfig;
import org.apache.metron.rest.model.ParseMessageRequest;
import org.apache.metron.rest.service.GrokService;
import org.apache.metron.rest.service.SensorParserConfigService;
import org.apache.zookeeper.KeeperException;
import org.json.simple.JSONObject;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.metron.rest.MetronRestConstants.GROK_CLASS_NAME;

@Service
public class SensorParserConfigServiceImpl implements SensorParserConfigService {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private ObjectMapper objectMapper;

  private CuratorFramework client;

  private GrokService grokService;

  private BundleSystem bundleSystem;

  private Environment environment;

  private Map<String,Object> configurationMap;

  @Autowired
  public SensorParserConfigServiceImpl(Environment environment,ObjectMapper objectMapper, CuratorFramework client, GrokService grokService) {
    this.objectMapper = objectMapper;
    this.client = client;
    this.grokService = grokService;
    this.environment = environment;
    configurationMap = new HashMap<>();
    configurationMap.put("metron.apps.hdfs.dir",environment.getProperty(MetronRestConstants.HDFS_METRON_APPS_ROOT));
  }

  private Map<String, String> availableParsers;

  public void setBundleSystem(BundleSystem bundleSystem) {
    this.bundleSystem = bundleSystem;
  }

  private BundleSystem getBundleSystem() {
    if (bundleSystem == null){
      try {
        bundleSystem = BundleSystemConfig.bundleSystem(client);
      } catch (Exception e) {
        LOG.error("Failed to create BundleSystem",e);
      }
    }
    return bundleSystem;
  }
  @Override
  public SensorParserConfig save(SensorParserConfig sensorParserConfig) throws RestException {
    try {
      ConfigurationsUtils.writeSensorParserConfigToZookeeper(sensorParserConfig.getSensorTopic(), objectMapper.writeValueAsString(sensorParserConfig).getBytes(), client);
    } catch (Exception e) {
      throw new RestException(e);
    }
    return sensorParserConfig;
  }

  @Override
  public SensorParserConfig findOne(String name) throws RestException {
    SensorParserConfig sensorParserConfig;
    try {
      sensorParserConfig = ConfigurationsUtils.readSensorParserConfigFromZookeeper(name, client);
    } catch (KeeperException.NoNodeException e) {
      return null;
    } catch (Exception e) {
      throw new RestException(e);
    }
    return sensorParserConfig;
  }

  @Override
  public Iterable<SensorParserConfig> getAll() throws RestException {
    List<SensorParserConfig> sensorParserConfigs = new ArrayList<>();
    List<String> sensorNames = getAllTypes();
    for (String name : sensorNames) {
      sensorParserConfigs.add(findOne(name));
    }
    return sensorParserConfigs;
  }

  @Override
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

  @Override
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

  @Override
  public Map<String, String> getAvailableParsers() throws RestException {
    try {
      if (availableParsers == null) {
        availableParsers = new HashMap<>();
        Set<Class<? extends MessageParser>> parserClasses = getParserClasses();
        parserClasses.forEach(parserClass -> {
          if (!"BasicParser".equals(parserClass.getSimpleName())) {
            availableParsers.put(parserClass.getSimpleName().replaceAll("Basic|Parser", ""),
                parserClass.getName());
          }
        });
      }
      return availableParsers;
    } catch (Exception e) {
      throw new RestException(e);
    }
  }

  @Override
  public Map<String, String> reloadAvailableParsers() throws RestException {
    availableParsers = null;
    return getAvailableParsers();
  }

  @SuppressWarnings("unchecked")
  private Set<Class<? extends MessageParser>> getParserClasses() throws NotInitializedException {
    return (Set<Class<? extends MessageParser>>) getBundleSystem().getExtensionsClassesForExtensionType(MessageParser.class);
  }

  @Override
  @SuppressWarnings("unchecked")
  public JSONObject parseMessage(ParseMessageRequest parseMessageRequest) throws RestException {
    SensorParserConfig sensorParserConfig = parseMessageRequest.getSensorParserConfig();
    if (sensorParserConfig == null) {
      throw new RestException("SensorParserConfig is missing from ParseMessageRequest");
    } else if (sensorParserConfig.getParserClassName() == null) {
      throw new RestException("SensorParserConfig must have a parserClassName");
    } else {
      MessageParser<JSONObject> parser;
      try {
        parser = (MessageParser<JSONObject>) getBundleSystem()
            .createInstance(sensorParserConfig.getParserClassName(), MessageParser.class);

        File temporaryGrokFile = null;
        if (isGrokConfig(sensorParserConfig)) {
          // NOTE: this parse will happen with the common grok file from the metron-parsers
          // classloader, it would be better to load all the groks correctly as the parser does
          // /patterns/common
          // /sensorName/commmon
          // /sensorName/sensorName
          ArrayList<Reader> readers = new ArrayList<Reader>();
          readers.add(new InputStreamReader(GrokParser.class.getResourceAsStream("/patterns/common")));
          readers.add(new StringReader(parseMessageRequest.getGrokStatement()));
          sensorParserConfig.getParserConfig().put("readers",readers);
          sensorParserConfig.getParserConfig().put("loadCommon",false);
        } else {
          // only inject the hdfs path for non - grok parsers, since as above they are loading
          // from the local filesystem for this operation and using the readers
          sensorParserConfig.getParserConfig().put("globalConfig", configurationMap);
        }
        parser.configure(sensorParserConfig.getParserConfig());
        parser.init();
        JSONObject results = parser.parse(parseMessageRequest.getSampleData().getBytes()).get(0);
        return results;
      } catch (Exception e) {
        throw new RestException(e.toString(), e.getCause());
      }
    }
  }

  private boolean isGrokConfig(SensorParserConfig sensorParserConfig) {
    return GROK_CLASS_NAME.equals(sensorParserConfig.getParserClassName());
  }
}
