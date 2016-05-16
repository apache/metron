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

import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;
import org.apache.curator.test.TestingServer;
import org.apache.metron.TestConstants;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConfiguredParserBoltTest extends BaseConfiguredBoltTest {

  private Set<String> parserConfigurationTypes = new HashSet<>();
  private String zookeeperUrl;

  public static class StandAloneConfiguredParserBolt extends ConfiguredParserBolt {

    public StandAloneConfiguredParserBolt(String zookeeperUrl) {
      super(zookeeperUrl);
    }

    @Override
    public void execute(Tuple input) {
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
    }

    @Override
    public void reloadCallback(String name, ConfigurationType type) {
      configsUpdated.add(name);
    }
  }

  @Before
  public void setupConfiguration() throws Exception {
    TestingServer testZkServer = new TestingServer(true);
    this.zookeeperUrl = testZkServer.getConnectString();
    byte[] globalConfig = ConfigurationsUtils.readGlobalConfigFromFile(TestConstants.SAMPLE_CONFIG_PATH);
    ConfigurationsUtils.writeGlobalConfigToZookeeper(globalConfig, zookeeperUrl);
    parserConfigurationTypes.add(ConfigurationType.GLOBAL.getName());
    Map<String, byte[]> sensorEnrichmentConfigs = ConfigurationsUtils.readSensorEnrichmentConfigsFromFile(TestConstants.ENRICHMENTS_CONFIGS_PATH);
    for (String sensorType : sensorEnrichmentConfigs.keySet()) {
      ConfigurationsUtils.writeSensorEnrichmentConfigToZookeeper(sensorType, sensorEnrichmentConfigs.get(sensorType), zookeeperUrl);
    }
    Map<String, byte[]> sensorParserConfigs = ConfigurationsUtils.readSensorParserConfigsFromFile(TestConstants.PARSER_CONFIGS_PATH);
    for (String sensorType : sensorParserConfigs.keySet()) {
      ConfigurationsUtils.writeSensorParserConfigToZookeeper(sensorType, sensorParserConfigs.get(sensorType), zookeeperUrl);
      parserConfigurationTypes.add(sensorType);
    }
  }

  @Test
  public void test() throws Exception {
    ParserConfigurations sampleConfigurations = new ParserConfigurations();
    try {
      StandAloneConfiguredParserBolt configuredBolt = new StandAloneConfiguredParserBolt(null);
      configuredBolt.prepare(new HashMap(), topologyContext, outputCollector);
      Assert.fail("A valid zookeeper url must be supplied");
    } catch (RuntimeException e){}

    configsUpdated = new HashSet<>();
    sampleConfigurations.updateGlobalConfig(ConfigurationsUtils.readGlobalConfigFromFile(TestConstants.SAMPLE_CONFIG_PATH));
    Map<String, byte[]> sensorParserConfigs = ConfigurationsUtils.readSensorParserConfigsFromFile(TestConstants.PARSER_CONFIGS_PATH);
    for (String sensorType : sensorParserConfigs.keySet()) {
      sampleConfigurations.updateSensorParserConfig(sensorType, sensorParserConfigs.get(sensorType));
    }

    StandAloneConfiguredParserBolt configuredBolt = new StandAloneConfiguredParserBolt(zookeeperUrl);
    configuredBolt.prepare(new HashMap(), topologyContext, outputCollector);
    waitForConfigUpdate(parserConfigurationTypes);
    Assert.assertEquals(sampleConfigurations, configuredBolt.configurations);

    configsUpdated = new HashSet<>();
    Map<String, Object> sampleGlobalConfig = sampleConfigurations.getGlobalConfig();
    sampleGlobalConfig.put("newGlobalField", "newGlobalValue");
    ConfigurationsUtils.writeGlobalConfigToZookeeper(sampleGlobalConfig, zookeeperUrl);
    waitForConfigUpdate(ConfigurationType.GLOBAL.getName());
    Assert.assertEquals("Add global config field", sampleConfigurations.getGlobalConfig(), configuredBolt.configurations.getGlobalConfig());

    configsUpdated = new HashSet<>();
    sampleGlobalConfig.remove("newGlobalField");
    ConfigurationsUtils.writeGlobalConfigToZookeeper(sampleGlobalConfig, zookeeperUrl);
    waitForConfigUpdate(ConfigurationType.GLOBAL.getName());
    Assert.assertEquals("Remove global config field", sampleConfigurations, configuredBolt.configurations);

    configsUpdated = new HashSet<>();
    String sensorType = "testSensorConfig";
    SensorParserConfig testSensorConfig = new SensorParserConfig();
    testSensorConfig.setParserClassName("className");
    testSensorConfig.setSensorTopic("sensorTopic");
    testSensorConfig.setParserConfig(new HashMap<String, Object>() {{
      put("configName", "configObject");
    }});
    sampleConfigurations.updateSensorParserConfig(sensorType, testSensorConfig);
    ConfigurationsUtils.writeSensorParserConfigToZookeeper(sensorType, testSensorConfig, zookeeperUrl);
    waitForConfigUpdate(sensorType);
    Assert.assertEquals("Add new sensor config", sampleConfigurations, configuredBolt.configurations);
    configuredBolt.cleanup();
  }
}