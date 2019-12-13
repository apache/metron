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

import org.adrianwalker.multilinestring.Multiline;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.apache.metron.TestConstants;
import org.apache.metron.common.utils.JSONUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigurationsUtilsTest {

  private TestingServer testZkServer;
  private String zookeeperUrl;
  private CuratorFramework client;
  private byte[] expectedGlobalConfig;
  private Map<String, byte[]> expectedSensorParserConfigMap;
  private Map<String, byte[]> expectedSensorEnrichmentConfigMap;

  @BeforeEach
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
    assertTrue(expectedGlobalConfig.length > 0);
    ConfigurationsUtils.writeGlobalConfigToZookeeper(expectedGlobalConfig, zookeeperUrl);
    byte[] actualGlobalConfigBytes = ConfigurationsUtils.readGlobalConfigBytesFromZookeeper(client);
    assertArrayEquals(expectedGlobalConfig, actualGlobalConfigBytes);

    assertTrue(expectedSensorParserConfigMap.size() > 0);
    String testSensorType = "yaf";
    byte[] expectedSensorParserConfigBytes = expectedSensorParserConfigMap.get(testSensorType);
    ConfigurationsUtils.writeSensorParserConfigToZookeeper(testSensorType, expectedSensorParserConfigBytes, zookeeperUrl);
    byte[] actualSensorParserConfigBytes = ConfigurationsUtils.readSensorParserConfigBytesFromZookeeper(testSensorType, client);
    assertArrayEquals(expectedSensorParserConfigBytes, actualSensorParserConfigBytes);

    assertTrue(expectedSensorEnrichmentConfigMap.size() > 0);
    byte[] expectedSensorEnrichmentConfigBytes = expectedSensorEnrichmentConfigMap.get(testSensorType);
    ConfigurationsUtils.writeSensorEnrichmentConfigToZookeeper(testSensorType, expectedSensorEnrichmentConfigBytes, zookeeperUrl);
    byte[] actualSensorEnrichmentConfigBytes = ConfigurationsUtils.readSensorEnrichmentConfigBytesFromZookeeper(testSensorType, client);
    assertArrayEquals(expectedSensorEnrichmentConfigBytes, actualSensorEnrichmentConfigBytes);

    String name = "testConfig";
    Map<String, Object> testConfig = new HashMap<>();
    testConfig.put("stringField", "value");
    testConfig.put("intField", 1);
    testConfig.put("doubleField", 1.1);
    ConfigurationsUtils.writeConfigToZookeeper(name, testConfig, zookeeperUrl);
    byte[] readConfigBytes = ConfigurationsUtils.readConfigBytesFromZookeeper(name, client);
    assertArrayEquals(JSONUtils.INSTANCE.toJSONPretty(testConfig), readConfigBytes);

  }

  /**
   * { "foo": "bar" }
   */
  @Multiline
  private static String someGlobalConfig;

  /**
   * [
   *  {
   *     "op": "replace",
   *     "path": "/foo",
   *     "value": "baz"
   *  }
   * ]
   */
  @Multiline
  private static String patchGlobalConfig;

  /**
   * {
   *   "foo" : "baz"
   * }
   */
  @Multiline
  private static String modifiedGlobalConfig;

  /**
   * {
   *    "parserClassName": "org.apache.metron.parsers.GrokParser",
   *    "sensorTopic": "squid"
   * }
   */
  @Multiline
  private static String someParserConfig;

  /**
   * [
   *  {
   *     "op": "replace",
   *     "path": "/sensorTopic",
   *     "value": "new-squid-topic"
   *  }
   * ]
   */
  @Multiline
  private static String patchParserConfig;

  /**
   * {
   *    "parserClassName": "org.apache.metron.parsers.GrokParser",
   *    "sensorTopic": "new-squid-topic"
   * }
   */
  @Multiline
  private static String modifiedParserConfig;

  @Test
  public void modifiedGlobalConfiguration() throws Exception {

    // write global configuration
    ConfigurationType type = ConfigurationType.GLOBAL;
    ConfigurationsUtils.writeConfigToZookeeper(type, JSONUtils.INSTANCE.toJSONPretty(someParserConfig), zookeeperUrl);

    // validate the modified global configuration
    byte[] actual = ConfigurationsUtils.readConfigBytesFromZookeeper(type, zookeeperUrl);
    assertThat(actual, equalTo(JSONUtils.INSTANCE.toJSONPretty(someParserConfig)));
  }

  @Test
  public void modifiesSingleParserConfiguration() throws Exception {

    // write parser configuration
    ConfigurationType type = ConfigurationType.PARSER;
    String parserName = "a-happy-metron-parser";
    byte[] config = JSONUtils.INSTANCE.toJSONPretty(someParserConfig);
    ConfigurationsUtils.writeConfigToZookeeper(type, Optional.of(parserName), config, zookeeperUrl);

    // validate the modified parser configuration
    byte[] actual = ConfigurationsUtils.readConfigBytesFromZookeeper(type, Optional.of(parserName), zookeeperUrl);
    assertThat(actual, equalTo(JSONUtils.INSTANCE.toJSONPretty(someParserConfig)));
  }

  /**
   * Note: the current configuration structure mixes abstractions based on the configuration type
   * and requires testing each type. GLOBAL is a actually representative of the final node name,
   * whereas the other types, e.g. PARSER, represent a directory/path and not a ZK node where values
   * are stored. The semantics are similar but slightly different.
   */
  @Test
  public void patchesGlobalConfigurationViaPatchJSON() throws Exception {

    // setup zookeeper with a configuration
    final ConfigurationType type = ConfigurationType.GLOBAL;
    byte[] config = JSONUtils.INSTANCE.toJSONPretty(someGlobalConfig);
    ConfigurationsUtils.writeConfigToZookeeper(type, config, zookeeperUrl);

    // patch the configuration
    byte[] patch = JSONUtils.INSTANCE.toJSONPretty(patchGlobalConfig);
    ConfigurationsUtils.applyConfigPatchToZookeeper(type, patch, zookeeperUrl);

    // validate the patched configuration
    byte[] actual = ConfigurationsUtils.readConfigBytesFromZookeeper(type, zookeeperUrl);
    byte[] expected = JSONUtils.INSTANCE.toJSONPretty(modifiedGlobalConfig);
    assertThat(actual, equalTo(expected));
  }

  @Test
  public void patchesParserConfigurationViaPatchJSON() throws Exception {

    // setup zookeeper with a configuration
    final ConfigurationType type = ConfigurationType.PARSER;
    final String parserName = "patched-metron-parser";
    byte[] config = JSONUtils.INSTANCE.toJSONPretty(someParserConfig);
    ConfigurationsUtils.writeConfigToZookeeper(type, Optional.of(parserName), config, zookeeperUrl);

    // patch the configuration
    byte[] patch = JSONUtils.INSTANCE.toJSONPretty(patchParserConfig);
    ConfigurationsUtils.applyConfigPatchToZookeeper(type, Optional.of(parserName), patch, zookeeperUrl);

    // validate the patched configuration
    byte[] actual = ConfigurationsUtils.readConfigBytesFromZookeeper(type, Optional.of(parserName), zookeeperUrl);
    byte[] expected = JSONUtils.INSTANCE.toJSONPretty(modifiedParserConfig);
    assertThat(actual, equalTo(expected));
  }

  @AfterEach
  public void tearDown() throws IOException {
    client.close();
    testZkServer.close();
    testZkServer.stop();
  }
}
