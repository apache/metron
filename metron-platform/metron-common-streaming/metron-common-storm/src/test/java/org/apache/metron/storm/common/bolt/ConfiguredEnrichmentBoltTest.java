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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.curator.test.TestingServer;
import org.apache.log4j.Level;
import org.apache.metron.TestConstants;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.test.utils.UnitTestHelper;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConfiguredEnrichmentBoltTest extends BaseConfiguredBoltTest {

  private static final String sampleConfigPath = "../" + TestConstants.SAMPLE_CONFIG_PATH;
  private static final String enrichmentsConfigPath = "../" + TestConstants.ENRICHMENTS_CONFIGS_PATH;
  private static final String parserConfigsPath = "../" + TestConstants.PARSER_CONFIGS_PATH;

  private Set<String> enrichmentConfigurationTypes = new HashSet<>();
  private String zookeeperUrl;

  public static class StandAloneConfiguredEnrichmentBolt extends ConfiguredEnrichmentBolt {

    public StandAloneConfiguredEnrichmentBolt(String zookeeperUrl) {
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
    byte[] globalConfig = ConfigurationsUtils.readGlobalConfigFromFile(sampleConfigPath);
    ConfigurationsUtils.writeGlobalConfigToZookeeper(globalConfig, zookeeperUrl);
    enrichmentConfigurationTypes.add(ConfigurationType.GLOBAL.getTypeName());
    Map<String, byte[]> sensorEnrichmentConfigs = ConfigurationsUtils.readSensorEnrichmentConfigsFromFile(enrichmentsConfigPath);
    for (String sensorType : sensorEnrichmentConfigs.keySet()) {
      ConfigurationsUtils.writeSensorEnrichmentConfigToZookeeper(sensorType, sensorEnrichmentConfigs.get(sensorType), zookeeperUrl);
      enrichmentConfigurationTypes.add(sensorType);
    }
    Map<String, byte[]> sensorParserConfigs = ConfigurationsUtils.readSensorParserConfigsFromFile(parserConfigsPath);
    for (String sensorType : sensorParserConfigs.keySet()) {
      ConfigurationsUtils.writeSensorParserConfigToZookeeper(sensorType, sensorParserConfigs.get(sensorType), zookeeperUrl);
    }
  }

  @Test
  public void test() throws Exception {
    EnrichmentConfigurations sampleConfigurations = new EnrichmentConfigurations();
    UnitTestHelper.setLog4jLevel(ConfiguredBolt.class, Level.FATAL);
    StandAloneConfiguredEnrichmentBolt configuredBoltNullZk = new StandAloneConfiguredEnrichmentBolt(null);
    assertThrows(RuntimeException.class, () -> configuredBoltNullZk.prepare(new HashMap(), topologyContext, outputCollector),
        "A valid zookeeper url must be supplied");
    UnitTestHelper.setLog4jLevel(ConfiguredBolt.class, Level.ERROR);

    configsUpdated = new HashSet<>();
    sampleConfigurations.updateGlobalConfig(ConfigurationsUtils.readGlobalConfigFromFile(sampleConfigPath));
    Map<String, byte[]> sensorEnrichmentConfigs = ConfigurationsUtils.readSensorEnrichmentConfigsFromFile(enrichmentsConfigPath);
    for (String sensorType : sensorEnrichmentConfigs.keySet()) {
      sampleConfigurations.updateSensorEnrichmentConfig(sensorType, sensorEnrichmentConfigs.get(sensorType));
    }

    StandAloneConfiguredEnrichmentBolt configuredBolt = new StandAloneConfiguredEnrichmentBolt(zookeeperUrl);
    configuredBolt.prepare(new HashMap(), topologyContext, outputCollector);
    waitForConfigUpdate(enrichmentConfigurationTypes);
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
    SensorEnrichmentConfig testSensorConfig = new SensorEnrichmentConfig();
    Map<String, Object> enrichmentFieldMap = new HashMap<>();
    enrichmentFieldMap.put("enrichmentTest", new ArrayList<String>() {{
      add("enrichmentField");
    }});
    testSensorConfig.getEnrichment().setFieldMap(enrichmentFieldMap);
    Map<String, Object> threatIntelFieldMap = new HashMap<>();
    threatIntelFieldMap.put("threatIntelTest", new ArrayList<String>() {{
      add("threatIntelField");
    }});
    testSensorConfig.getThreatIntel().setFieldMap(threatIntelFieldMap);
    sampleConfigurations.updateSensorEnrichmentConfig(sensorType, testSensorConfig);
    ConfigurationsUtils.writeSensorEnrichmentConfigToZookeeper(sensorType, testSensorConfig, zookeeperUrl);
    waitForConfigUpdate(sensorType);
    assertEquals(sampleConfigurations, configuredBolt.getConfigurations(), "Add new sensor config");
    configuredBolt.cleanup();
  }
}
