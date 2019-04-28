/*
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
package org.apache.metron.common.configuration;

import com.google.common.base.Preconditions;
import java.util.Map;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.TestConstants;
import org.junit.Assert;


/**
 * Adaptor class to load configuration into zookeeper
 */
public class ZKConfigAdapter {
  private byte[] expectedGlobalConfig;
  private Map<String, byte[]> expectedSensorParserConfigMap;
  private Map<String, byte[]> expectedSensorEnrichmentConfigMap;
  private CuratorFramework client;

  ZKConfigAdapter(CuratorFramework client) {
    this.client =  Preconditions.checkNotNull(client);
  }

  public byte[] getExpectedGlobalConfig() {
    Assert.assertNotNull(expectedGlobalConfig);
    return expectedGlobalConfig;
  }

  public Map<String, byte[]> getExpectedSensorParserConfigMap() {
    Assert.assertNotNull(expectedSensorParserConfigMap);
    return expectedSensorParserConfigMap;
  }

  public Map<String, byte[]> getExpectedSensorEnrichmentConfigMap() {
    Assert.assertNotNull(expectedSensorEnrichmentConfigMap);
    return expectedSensorEnrichmentConfigMap;
  }


  public void loadGlobalConfig(final String configFilePath) throws Exception {
    expectedGlobalConfig = ConfigurationsUtils.readGlobalConfigFromFile(configFilePath);
    //ConfigurationType type = ConfigurationType.GLOBAL;
    Assert.assertTrue(expectedGlobalConfig.length > 0);
    ConfigurationsUtils.writeGlobalConfigToZookeeper(expectedGlobalConfig,
            client.getZookeeperClient().getCurrentConnectionString());
  }

  public void loadSensorParserConfig(final String configFilePath, final String singleSensorType) throws Exception {
    expectedSensorParserConfigMap = ConfigurationsUtils.readSensorParserConfigsFromFile(configFilePath);

    if (singleSensorType != null) {
      byte[] expectedSensorParserConfigBytes = expectedSensorParserConfigMap.get(singleSensorType);
      ConfigurationsUtils.writeSensorParserConfigToZookeeper(singleSensorType, expectedSensorParserConfigBytes, client);
    } else {
      for (String sensorType : expectedSensorParserConfigMap.keySet()) {
        byte[] configData = expectedSensorParserConfigMap.get(sensorType);
        ConfigurationsUtils.writeSensorParserConfigToZookeeper(sensorType, configData, client);
      }
    }
    Assert.assertFalse(expectedSensorParserConfigMap.isEmpty());
  }

  public void loadSensorEnrichmentConfig(String configFilePath, final String singleSensorType) throws Exception {
    expectedSensorEnrichmentConfigMap = ConfigurationsUtils.readSensorEnrichmentConfigsFromFile(TestConstants.ENRICHMENTS_CONFIGS_PATH);
    if (singleSensorType != null) {
      byte[] expectedSensorParserConfigBytes = expectedSensorEnrichmentConfigMap.get(singleSensorType);
      ConfigurationsUtils.writeSensorEnrichmentConfigToZookeeper(singleSensorType, expectedSensorParserConfigBytes, client);
    } else {
      for (String sensorType : expectedSensorEnrichmentConfigMap.keySet()) {
        byte[] configData = expectedSensorEnrichmentConfigMap.get(sensorType);
        ConfigurationsUtils.writeSensorEnrichmentConfigToZookeeper(sensorType, configData, client);
      }
    }
    Assert.assertFalse(expectedSensorEnrichmentConfigMap.isEmpty());
  }
}
