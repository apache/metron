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
import org.apache.metron.TestConstants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.integration.TestZKServer;
import org.junit.Assert;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ConfigurationsUtilsTest {
  private static final String SENSOR_TYPE = "yaf";

  @Test
  public void testReadGlobalConfigZK() throws Exception {
    TestZKServer.runWithZK( (zkServer, zkClient) -> {
      ZKConfigAdapter config = new ZKConfigAdapter(zkClient);
      config.loadGlobalConfig(TestConstants.SAMPLE_CONFIG_PATH);

      byte[] actualGlobalConfigBytes = ConfigurationsUtils.readGlobalConfigBytesFromZookeeper(zkClient);
      Assert.assertArrayEquals(config.getExpectedGlobalConfig(), actualGlobalConfigBytes);
    });
  }

  @Test
  public void testReadSensorParserConfigZK() throws Exception {
    TestZKServer.runWithZK( (zkServer, zkClient) -> {
      ZKConfigAdapter config = new ZKConfigAdapter(zkClient);
      config.loadSensorParserConfig(TestConstants.PARSER_CONFIGS_PATH, SENSOR_TYPE);

      byte[] actualSensorParserConfigBytes = ConfigurationsUtils.readSensorParserConfigBytesFromZookeeper(SENSOR_TYPE,
              zkClient);
      Assert.assertArrayEquals(config.getExpectedSensorParserConfigMap().get(SENSOR_TYPE),
              actualSensorParserConfigBytes);
    });
  }

  @Test
  public void testReadSensorEnrichmentConfigZK() throws Exception {
    TestZKServer.runWithZK( (zkServer, zkClient) -> {
      ZKConfigAdapter config = new ZKConfigAdapter(zkClient);
      config.loadSensorEnrichmentConfig(TestConstants.ENRICHMENTS_CONFIGS_PATH, SENSOR_TYPE);

      byte[] actualSensorEnrichmentConfigBytes = ConfigurationsUtils.readSensorEnrichmentConfigBytesFromZookeeper(SENSOR_TYPE,
              zkClient);
      Assert.assertArrayEquals(config.getExpectedSensorEnrichmentConfigMap().get(SENSOR_TYPE),
              actualSensorEnrichmentConfigBytes);
    });
  }

  @Test
  public void testReadAdhocConfigZK() throws Exception {
    TestZKServer.runWithZK( (zkServer, zkClient) -> {
      String name = "testConfig";
      Map<String, Object> testConfig = new HashMap<>();
      testConfig.put("stringField", "value");
      testConfig.put("intField", 1);
      testConfig.put("doubleField", 1.1);
      ConfigurationsUtils.writeConfigToZookeeper(name, testConfig, zkServer.getZookeeperUrl());
      byte[] readConfigBytes = ConfigurationsUtils.readConfigBytesFromZookeeper(name, zkClient);
      Assert.assertArrayEquals(JSONUtils.INSTANCE.toJSONPretty(testConfig), readConfigBytes);
    });
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
    TestZKServer.runWithZK( (zkServer, zkClient) -> {
      ConfigurationType type = ConfigurationType.GLOBAL;
      ConfigurationsUtils.writeConfigToZookeeper(type, JSONUtils.INSTANCE.toJSONPretty(someParserConfig), zkServer.getZookeeperUrl());

      // validate the modified global configuration
      byte[] actual = ConfigurationsUtils.readConfigBytesFromZookeeper(type, zkServer.getZookeeperUrl());
      assertThat(actual, equalTo(JSONUtils.INSTANCE.toJSONPretty(someParserConfig)));
    });
  }

  @Test
  public void modifiesSingleParserConfiguration() throws Exception {
    TestZKServer.runWithZK( (zkServer, zkClient) -> {
      // write parser configuration
      ConfigurationType type = ConfigurationType.PARSER;
      String parserName = "a-happy-metron-parser";
      byte[] config = JSONUtils.INSTANCE.toJSONPretty(someParserConfig);

      ConfigurationsUtils.writeConfigToZookeeper(type, Optional.of(parserName), config, zkServer.getZookeeperUrl());

      // validate the modified parser configuration
      byte[] actual = ConfigurationsUtils.readConfigBytesFromZookeeper(type, Optional.of(parserName), zkServer.getZookeeperUrl());
      assertThat(actual, equalTo(JSONUtils.INSTANCE.toJSONPretty(someParserConfig)));
    });
  }

  /**
   * Note: the current configuration structure mixes abstractions based on the configuration type
   * and requires testing each type. GLOBAL is a actually representative of the final node name,
   * whereas the other types, e.g. PARSER, represent a directory/path and not a ZK node where values
   * are stored. The semantics are similar but slightly different.
   */
  @Test
  public void patchesGlobalConfigurationViaPatchJSON() throws Exception {
    TestZKServer.runWithZK( (zkServer, zkClient) -> {
      // setup zookeeper with a configuration
      final ConfigurationType type = ConfigurationType.GLOBAL;
      byte[] config = JSONUtils.INSTANCE.toJSONPretty(someGlobalConfig);
      ConfigurationsUtils.writeConfigToZookeeper(type, config, zkServer.getZookeeperUrl());

      // patch the configuration
      byte[] patch = JSONUtils.INSTANCE.toJSONPretty(patchGlobalConfig);
      ConfigurationsUtils.applyConfigPatchToZookeeper(type, patch, zkServer.getZookeeperUrl());

      // validate the patched configuration
      byte[] actual = ConfigurationsUtils.readConfigBytesFromZookeeper(type, zkServer.getZookeeperUrl());
      byte[] expected = JSONUtils.INSTANCE.toJSONPretty(modifiedGlobalConfig);
      assertThat(actual, equalTo(expected));
    });
  }

  @Test
  public void patchesParserConfigurationViaPatchJSON() throws Exception {
    TestZKServer.runWithZK((zkServer, zkClient) -> {
      // setup zookeeper with a configuration
      final ConfigurationType type = ConfigurationType.PARSER;
      final String parserName = "patched-metron-parser";
      byte[] config = JSONUtils.INSTANCE.toJSONPretty(someParserConfig);
      ConfigurationsUtils.writeConfigToZookeeper(type, Optional.of(parserName), config, zkServer.getZookeeperUrl());

      // patch the configuration
      byte[] patch = JSONUtils.INSTANCE.toJSONPretty(patchParserConfig);
      ConfigurationsUtils.applyConfigPatchToZookeeper(type, Optional.of(parserName), patch, zkServer.getZookeeperUrl());

      // validate the patched configuration
      byte[] actual = ConfigurationsUtils.readConfigBytesFromZookeeper(type, Optional.of(parserName), zkServer.getZookeeperUrl());
      byte[] expected = JSONUtils.INSTANCE.toJSONPretty(modifiedParserConfig);
      assertThat(actual, equalTo(expected));
    });
  }

}
