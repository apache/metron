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

package org.apache.metron.storm.common.bolt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.curator.test.TestingServer;
import org.apache.log4j.Level;
import org.apache.metron.TestConstants;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.FieldValidator;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.test.utils.UnitTestHelper;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

  @BeforeEach
  public void setupConfiguration() throws Exception {
    TestingServer testZkServer = new TestingServer(true);
    this.zookeeperUrl = testZkServer.getConnectString();
    byte[] globalConfig = ConfigurationsUtils.readGlobalConfigFromFile("../" + TestConstants.SAMPLE_CONFIG_PATH);
    ConfigurationsUtils.writeGlobalConfigToZookeeper(globalConfig, zookeeperUrl);
    parserConfigurationTypes.add(ConfigurationType.GLOBAL.getTypeName());
    Map<String, byte[]> sensorEnrichmentConfigs = ConfigurationsUtils.readSensorEnrichmentConfigsFromFile("../" + TestConstants.ENRICHMENTS_CONFIGS_PATH);
    for (String sensorType : sensorEnrichmentConfigs.keySet()) {
      ConfigurationsUtils.writeSensorEnrichmentConfigToZookeeper(sensorType, sensorEnrichmentConfigs.get(sensorType), zookeeperUrl);
    }
    Map<String, byte[]> sensorParserConfigs = ConfigurationsUtils.readSensorParserConfigsFromFile("../" + TestConstants.PARSER_CONFIGS_PATH);
    for (String sensorType : sensorParserConfigs.keySet()) {
      ConfigurationsUtils.writeSensorParserConfigToZookeeper(sensorType, sensorParserConfigs.get(sensorType), zookeeperUrl);
      parserConfigurationTypes.add(sensorType);
    }
  }

  @Test
  public void test() throws Exception {
    ParserConfigurations sampleConfigurations = new ParserConfigurations();
    UnitTestHelper.setLog4jLevel(ConfiguredBolt.class, Level.FATAL);
    StandAloneConfiguredParserBolt configuredBoltNullZk = new ConfiguredParserBoltTest.StandAloneConfiguredParserBolt(null);
    assertThrows(RuntimeException.class, () -> configuredBoltNullZk.prepare(new HashMap(), topologyContext, outputCollector),
        "A valid zookeeper url must be supplied");
    UnitTestHelper.setLog4jLevel(ConfiguredBolt.class, Level.ERROR);

    configsUpdated = new HashSet<>();
    sampleConfigurations.updateGlobalConfig(ConfigurationsUtils.readGlobalConfigFromFile("../" + TestConstants.SAMPLE_CONFIG_PATH));
    Map<String, byte[]> sensorParserConfigs = ConfigurationsUtils.readSensorParserConfigsFromFile("../" + TestConstants.PARSER_CONFIGS_PATH);
    for (String sensorType : sensorParserConfigs.keySet()) {
      sampleConfigurations.updateSensorParserConfig(sensorType, sensorParserConfigs.get(sensorType));
    }

    StandAloneConfiguredParserBolt configuredBolt = new StandAloneConfiguredParserBolt(zookeeperUrl);
    configuredBolt.prepare(new HashMap(), topologyContext, outputCollector);
    waitForConfigUpdate(parserConfigurationTypes);
    assertEquals(sampleConfigurations, configuredBolt.getConfigurations());

    configsUpdated = new HashSet<>();
    Map<String, Object> sampleGlobalConfig = sampleConfigurations.getGlobalConfig();
    sampleGlobalConfig.put("newGlobalField", "newGlobalValue");
    ConfigurationsUtils.writeGlobalConfigToZookeeper(sampleGlobalConfig, zookeeperUrl);
    waitForConfigUpdate(ConfigurationType.GLOBAL.getTypeName());
    assertEquals(sampleConfigurations.getGlobalConfig(), configuredBolt.getConfigurations().getGlobalConfig(), "Add global config field");

    configsUpdated = new HashSet<>();
    sampleGlobalConfig.remove("newGlobalField");
    ConfigurationsUtils.writeGlobalConfigToZookeeper(sampleGlobalConfig, zookeeperUrl);
    waitForConfigUpdate(ConfigurationType.GLOBAL.getTypeName());
    assertEquals(sampleConfigurations, configuredBolt.getConfigurations(), "Remove global config field");

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
    ParserConfigurations configuredBoltConfigs = configuredBolt.getConfigurations();
    if(!sampleConfigurations.equals(configuredBoltConfigs)) {
      //before we fail, let's try to dump out some info.
      if(sampleConfigurations.getFieldValidations().size() != configuredBoltConfigs.getFieldValidations().size()) {
        System.out.println("Field validations don't line up");
      }
      for(int i = 0;i < sampleConfigurations.getFieldValidations().size();++i) {
        FieldValidator l = sampleConfigurations.getFieldValidations().get(i);
        FieldValidator r = configuredBoltConfigs.getFieldValidations().get(i);
        if(!l.equals(r)) {
          System.out.println(l + " != " + r);
        }
      }
      if(sampleConfigurations.getConfigurations().size() != configuredBoltConfigs.getConfigurations().size()) {
        System.out.println("Configs don't line up");
      }
      for(Map.Entry<String, Object> kv : sampleConfigurations.getConfigurations().entrySet() ) {
        Object l = kv.getValue();
        Object r = configuredBoltConfigs.getConfigurations().get(kv.getKey());
        if(!l.equals(r)) {
          System.out.println(kv.getKey() + " config does not line up: " );
          System.out.println(l);
          System.out.println(r);
        }
      }
      assertEquals(sampleConfigurations, configuredBoltConfigs, "Add new sensor config");
    }
    assertEquals(sampleConfigurations, configuredBoltConfigs, "Add new sensor config");
    configuredBolt.cleanup();
  }
}
