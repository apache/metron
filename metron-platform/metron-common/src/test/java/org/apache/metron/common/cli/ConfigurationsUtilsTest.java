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
package org.apache.metron.common.cli;

import junit.framework.Assert;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.apache.metron.TestConstants;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.utils.JSONUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationsUtilsTest {

  private TestingServer testZkServer;
  private String zookeeperUrl;
  private CuratorFramework client;
  private byte[] expectedGlobalConfig;
  private Map<String, byte[]> expectedSensorParserConfigMap;
  private Map<String, byte[]> expectedSensorEnrichmentConfigMap;

  @Before
  public void setup() throws Exception {
    testZkServer = new TestingServer(true);
    zookeeperUrl = testZkServer.getConnectString();
    client = ConfigurationsUtils.getClient(zookeeperUrl);
    client.start();
    expectedGlobalConfig = ConfigurationsUtils.readGlobalConfigFromFile(TestConstants.SAMPLE_CONFIG_PATH);
    expectedSensorParserConfigMap = ConfigurationsUtils.readSensorParserConfigsFromFile(TestConstants.PARSER_CONFIGS_PATH);
    expectedSensorEnrichmentConfigMap = ConfigurationsUtils.readSensorEnrichmentConfigsFromFile(TestConstants.ENRICHMENTS_CONFIGS_PATH);
  }

  @Test
  public void test() throws Exception {
    Assert.assertTrue(expectedGlobalConfig.length > 0);
    ConfigurationsUtils.writeGlobalConfigToZookeeper(expectedGlobalConfig, zookeeperUrl);
    byte[] actualGlobalConfigBytes = ConfigurationsUtils.readGlobalConfigBytesFromZookeeper(client);
    Assert.assertTrue(Arrays.equals(expectedGlobalConfig, actualGlobalConfigBytes));

    Assert.assertTrue(expectedSensorParserConfigMap.size() > 0);
    String testSensorType = "yaf";
    byte[] expectedSensorParserConfigBytes = expectedSensorParserConfigMap.get(testSensorType);
    ConfigurationsUtils.writeSensorParserConfigToZookeeper(testSensorType, expectedSensorParserConfigBytes, zookeeperUrl);
    byte[] actualSensorParserConfigBytes = ConfigurationsUtils.readSensorParserConfigBytesFromZookeeper(testSensorType, client);
    Assert.assertTrue(Arrays.equals(expectedSensorParserConfigBytes, actualSensorParserConfigBytes));

    Assert.assertTrue(expectedSensorEnrichmentConfigMap.size() > 0);
    byte[] expectedSensorEnrichmentConfigBytes = expectedSensorEnrichmentConfigMap.get(testSensorType);
    ConfigurationsUtils.writeSensorEnrichmentConfigToZookeeper(testSensorType, expectedSensorEnrichmentConfigBytes, zookeeperUrl);
    byte[] actualSensorEnrichmentConfigBytes = ConfigurationsUtils.readSensorEnrichmentConfigBytesFromZookeeper(testSensorType, client);
    Assert.assertTrue(Arrays.equals(expectedSensorEnrichmentConfigBytes, actualSensorEnrichmentConfigBytes));

    String name = "testConfig";
    Map<String, Object> testConfig = new HashMap<>();
    testConfig.put("stringField", "value");
    testConfig.put("intField", 1);
    testConfig.put("doubleField", 1.1);
    ConfigurationsUtils.writeConfigToZookeeper(name, testConfig, zookeeperUrl);
    byte[] readConfigBytes = ConfigurationsUtils.readConfigBytesFromZookeeper(name, client);
    Assert.assertTrue(Arrays.equals(JSONUtils.INSTANCE.toJSON(testConfig), readConfigBytes));

  }

  @After
  public void tearDown() throws IOException {
    client.close();
    testZkServer.close();
    testZkServer.stop();
  }
}
