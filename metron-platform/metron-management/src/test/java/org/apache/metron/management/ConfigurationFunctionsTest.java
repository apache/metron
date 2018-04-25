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
package org.apache.metron.management;

import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.PosixParser;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.apache.log4j.Level;
import org.apache.metron.common.cli.ConfigurationManager;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.test.utils.UnitTestHelper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.apache.metron.TestConstants.PARSER_CONFIGS_PATH;
import static org.apache.metron.TestConstants.SAMPLE_CONFIG_PATH;
import static org.apache.metron.common.configuration.ConfigurationType.GLOBAL;
import static org.apache.metron.common.configuration.ConfigurationType.PROFILER;
import static org.apache.metron.common.configuration.ConfigurationsUtils.writeProfilerConfigToZookeeper;
import static org.apache.metron.management.utils.FileUtils.slurp;
import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.run;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the ConfigurationFunctions class.
 */
public class ConfigurationFunctionsTest {

  private static TestingServer testZkServer;
  private static String zookeeperUrl;
  private static CuratorFramework client;
  private static String goodGlobalConfig = slurp( SAMPLE_CONFIG_PATH+ "/global.json");
  private static String goodTestEnrichmentConfig = slurp( SAMPLE_CONFIG_PATH + "/enrichments/test.json");
  private static String goodBroParserConfig = slurp(PARSER_CONFIGS_PATH + "/parsers/bro.json");
  private static String goodTestIndexingConfig = slurp( SAMPLE_CONFIG_PATH + "/indexing/test.json");

  private Context context;
  private JSONParser parser;

  /**
   * {
   *   "profiles" : [
   *      {
   *        "profile" : "counter",
   *        "foreach" : "ip_src_addr",
   *        "init"    : { "counter" : 0 },
   *        "update"  : { "counter" : "counter + 1" },
   *        "result"  : "counter"
   *      }
   *   ],
   *   "timestampField" : "timestamp"
   * }
   */
  @Multiline
  private static String goodProfilerConfig;

  @BeforeClass
  public static void setupZookeeper() throws Exception {

    // zookeeper server
    testZkServer = new TestingServer(true);
    zookeeperUrl = testZkServer.getConnectString();

    // zookeeper client
    client = ConfigurationsUtils.getClient(zookeeperUrl);
    client.start();
  }

  @Before
  public void setup() throws Exception {

    context = new Context.Builder()
            .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client)
            .build();

    parser = new JSONParser();

    // push configs to zookeeper
    pushConfigs(SAMPLE_CONFIG_PATH, zookeeperUrl);
    pushConfigs(PARSER_CONFIGS_PATH, zookeeperUrl);
    writeProfilerConfigToZookeeper(goodProfilerConfig.getBytes(), client);
  }

  /**
   * Deletes a path within Zookeeper.
   *
   * @param path The path within Zookeeper to delete.
   * @throws Exception
   */
  private void deletePath(String path) throws Exception {
    client.delete().forPath(path);
  }

  /**
   * Transforms a String to a {@link JSONObject}.
   *
   * @param input The input String to transform
   * @return A {@link JSONObject}.
   * @throws org.json.simple.parser.ParseException
   */
  private JSONObject toJSONObject(String input) throws org.json.simple.parser.ParseException {

    if(input == null) {
      return null;
    }
    return (JSONObject) parser.parse(input.trim());
  }

  /**
   * Push configuration values to Zookeeper.
   *
   * @param inputPath The local filesystem path to the configurations.
   * @param zookeeperUrl The URL of Zookeeper.
   * @throws Exception
   */
  private static void pushConfigs(String inputPath, String zookeeperUrl) throws Exception {

    String[] args = new String[] {
            "-z", zookeeperUrl,
            "--mode", "PUSH",
            "--input_dir", inputPath
    };
    CommandLine cli = ConfigurationManager.ConfigurationOptions.parse(new PosixParser(), args);

    ConfigurationManager manager = new ConfigurationManager();
    manager.run(cli);
  }

  /**
   * The CONFIG_GET function should be able to return the Parser configuration
   * for a given sensor.
   */
  @Test
  public void testGetParser() throws Exception {

    String out = (String) run("CONFIG_GET('PARSER', 'bro')", context);

    SensorParserConfig actual = SensorParserConfig.fromBytes(out.getBytes());
    SensorParserConfig expected = SensorParserConfig.fromBytes(goodBroParserConfig.getBytes());
    assertEquals(expected, actual);
  }

  /**
   * The CONFIG_GET function should NOT return any configuration when the
   * Parser configuration for a given sensor is missing AND emptyIfNotPresent = false.
   */
  @Test
  public void testGetParserMissWithoutDefault() {

    // expect null because emptyIfNotPresent = false
    Object out = run("CONFIG_GET('PARSER', 'sensor', false)", context);
    assertNull(out);
  }

  /**
   * The CONFIG_GET function should return a default configuration when none
   * currently exists.
   */
  @Test
  public void testGetParserMissWithDefault() throws Exception {

    SensorParserConfig expected = new SensorParserConfig();
    {
      Object out = run("CONFIG_GET('PARSER', 'sensor')", context);
      SensorParserConfig actual = SensorParserConfig.fromBytes(out.toString().getBytes());
      assertEquals(expected, actual);
    }
    {
      Object out = run("CONFIG_GET('PARSER', 'sensor', true)", context);
      SensorParserConfig actual = SensorParserConfig.fromBytes(out.toString().getBytes());
      assertEquals(expected, actual);
    }
  }

  /**
   * The CONFIG_GET function should be able to return the Enrichment configuration
   * for a given sensor.
   */
  @Test
  public void testGetEnrichment() throws Exception {

    String out = (String) run("CONFIG_GET('ENRICHMENT', 'test')", context);

    SensorEnrichmentConfig actual = SensorEnrichmentConfig.fromBytes(out.getBytes());
    SensorEnrichmentConfig expected = SensorEnrichmentConfig.fromBytes(goodTestEnrichmentConfig.getBytes());
    assertEquals(expected, actual);
  }

  /**
   * No default configuration should be provided in this case.
   */
  @Test
  public void testGetEnrichmentMissWithoutDefault() {

    // expect null because emptyIfNotPresent = false
    Object out = run("CONFIG_GET('ENRICHMENT', 'sense', false)", context);
    assertNull(out);
  }

  /**
   * A default empty configuration should be provided, if one does not exist.
   */
  @Test
  public void testGetEnrichmentMissWithDefault() throws Exception {

    // expect an empty configuration to be returned
    SensorEnrichmentConfig expected = new SensorEnrichmentConfig();
    {
      String out = (String) run("CONFIG_GET('ENRICHMENT', 'missing-sensor')", context);
      SensorEnrichmentConfig actual = SensorEnrichmentConfig.fromBytes(out.getBytes());
      assertEquals(expected, actual);
    }
    {
      String out = (String) run("CONFIG_GET('ENRICHMENT', 'missing-sensor', true)", context);
      SensorEnrichmentConfig actual = SensorEnrichmentConfig.fromBytes(out.getBytes());
      assertEquals(expected, actual);
    }
  }

  /**
   * The CONFIG_GET function should be able to return the Indexing configuration
   * for a given sensor.
   */
  @Test
  public void testGetIndexing() throws Exception {

    String out = (String) run("CONFIG_GET('INDEXING', 'test')", context);

    Map<String, Object> actual = toJSONObject(out);
    Map<String, Object> expected = toJSONObject(goodTestIndexingConfig);
    assertEquals(expected, actual);
  }

  /**
   * No default configuration should be provided in this case.
   */
  @Test
  public void testGetIndexingMissWithoutDefault() {

    // expect null because emptyIfNotPresent = false
    Object out = run("CONFIG_GET('INDEXING', 'sense', false)", context);
    assertNull(out);
  }

  /**
   * A default empty configuration should be provided, if one does not exist.
   */
  @Test
  public void testGetIndexingtMissWithDefault() throws Exception {

    // expect an empty configuration to be returned
    Map<String, Object> expected = Collections.emptyMap();
    {
      String out = (String) run("CONFIG_GET('INDEXING', 'missing-sensor')", context);
      Map<String, Object> actual = toJSONObject(out);
      assertEquals(expected, actual);
    }
    {
      String out = (String) run("CONFIG_GET('INDEXING', 'missing-sensor', true)", context);
      Map<String, Object> actual = toJSONObject(out);
      assertEquals(expected, actual);
    }
  }

  /**
   * The CONFIG_GET function should be able to return the Profiler configuration.
   */
  @Test
  public void testGetProfiler() throws Exception {

    String out = (String) run("CONFIG_GET('PROFILER')", context);

    ProfilerConfig actual = ProfilerConfig.fromBytes(out.getBytes());
    ProfilerConfig expected = ProfilerConfig.fromBytes(goodProfilerConfig.getBytes());
    assertEquals(expected, actual);
  }

  /**
   * No default configuration should be provided in this case.
   */
  @Test
  public void testGetProfilerMissWithoutDefault() throws Exception {

    deletePath(PROFILER.getZookeeperRoot());

    // expect null because emptyIfNotPresent = false
    String out = (String) run("CONFIG_GET('PROFILER', false)", context);
    assertNull(out);
  }

  /**
   * A default empty configuration should be provided, if one does not exist.
   */
  @Test
  public void testGetProfilerMissWithDefault() throws Exception {

    // there is no profiler config in zookeeper
    deletePath(PROFILER.getZookeeperRoot());

    // expect an empty configuration to be returned
    ProfilerConfig expected = new ProfilerConfig();
    {
      String out = (String) run("CONFIG_GET('PROFILER', true)", context);
      ProfilerConfig actual = ProfilerConfig.fromJSON(out);
      assertEquals(expected, actual);
    }
    {
      String out = (String) run("CONFIG_GET('PROFILER')", context);
      ProfilerConfig actual = ProfilerConfig.fromJSON(out);
      assertEquals(expected, actual);
    }
  }

  @Test
  public void testGetGlobal() throws Exception {

    String out = (String) run("CONFIG_GET('GLOBAL')", context);

    Map<String, Object> actual = toJSONObject(out);
    Map<String, Object> expected = toJSONObject(goodGlobalConfig);
    assertEquals(expected, actual);
  }

  /**
   * No default configuration should be provided in this case.
   */
  @Test
  public void testGetGlobalMissWithoutDefault() throws Exception {

    // there is no global config in zookeeper
    deletePath(GLOBAL.getZookeeperRoot());

    // expect null because emptyIfNotPresent = false
    Object out = run("CONFIG_GET('GLOBAL', false)", context);
    assertNull(out);
  }

  /**
   * A default empty configuration should be provided, if one does not exist.
   */
  @Test
  public void testGetGlobalMissWithDefault() throws Exception {

    // there is no global config in zookeeper
    deletePath(GLOBAL.getZookeeperRoot());

    // expect an empty configuration to be returned
    Map<String, Object> expected = Collections.emptyMap();
    {
      String out = (String) run("CONFIG_GET('GLOBAL')", context);
      Map<String, Object> actual = toJSONObject(out);
      assertEquals(expected, actual);
    }
    {
      String out = (String) run("CONFIG_GET('GLOBAL', true)", context);
      Map<String, Object> actual = toJSONObject(out);
      assertEquals(expected, actual);
    }
  }

  @Test
  public void testPutGlobal() throws Exception {

    String out = (String) run("CONFIG_GET('GLOBAL')", context);

    Map<String, Object> actual = toJSONObject(out);
    Map<String, Object> expected = toJSONObject(goodGlobalConfig);
    assertEquals(expected, actual);
  }

  @Test(expected=ParseException.class)
  public void testPutGlobalBad() {
    {
      UnitTestHelper.setLog4jLevel(ConfigurationFunctions.class, Level.FATAL);
      try {
        run("CONFIG_PUT('GLOBAL', 'foo bar')", context);
      } catch(ParseException e) {
        UnitTestHelper.setLog4jLevel(ConfigurationFunctions.class, Level.ERROR);
        throw e;
      }
    }
  }

  @Test
  public void testPutIndexing() throws InterruptedException {
    String brop= (String) run("CONFIG_GET('INDEXING', 'testIndexingPut')", context);
    run("CONFIG_PUT('INDEXING', config, 'testIndexingPut')", ImmutableMap.of("config", brop), context);
    boolean foundMatch = false;
    for(int i = 0;i < 10 && !foundMatch;++i) {
      String bropNew = (String) run("CONFIG_GET('INDEXING', 'testIndexingPut', false)", context);
      foundMatch =  brop.equals(bropNew);
      if(foundMatch) {
        break;
      }
      Thread.sleep(2000);
    }
    assertTrue(foundMatch);
  }

  @Test(expected= ParseException.class)
  public void testPutIndexingBad() throws InterruptedException {
    {
      {
        UnitTestHelper.setLog4jLevel(ConfigurationFunctions.class, Level.FATAL);
        try {
          run("CONFIG_PUT('INDEXING', config, 'brop')", ImmutableMap.of("config", "foo bar"), context);
        } catch(ParseException e) {
          UnitTestHelper.setLog4jLevel(ConfigurationFunctions.class, Level.ERROR);
          throw e;
        }
      }
    }
  }

  @Test
  public void testPutEnrichment() throws InterruptedException {
    String config = (String) run("CONFIG_GET('ENRICHMENT', 'sensor')", context);
    assertNotNull(config);

    run("CONFIG_PUT('ENRICHMENT', config, 'sensor')", ImmutableMap.of("config", config), context);

    boolean foundMatch = false;
    for(int i = 0;i < 10 && !foundMatch;++i) {
      String newConfig = (String) run("CONFIG_GET('ENRICHMENT', 'sensor', false)", context);
      foundMatch = config.equals(newConfig);
      if(foundMatch) {
        break;
      }
      Thread.sleep(2000);
    }
    assertTrue(foundMatch);
  }

  @Test(expected= ParseException.class)
  public void testPutEnrichmentBad() throws InterruptedException {
    {
      {
        UnitTestHelper.setLog4jLevel(ConfigurationFunctions.class, Level.FATAL);
        try {
          run("CONFIG_PUT('ENRICHMENT', config, 'brop')", ImmutableMap.of("config", "foo bar"), context);
        } catch(ParseException e) {
          UnitTestHelper.setLog4jLevel(ConfigurationFunctions.class, Level.ERROR);
          throw e;
        }
      }
    }
  }

  @Test
  public void testPutParser() throws InterruptedException {
    String brop= (String) run("CONFIG_GET('PARSER', 'testParserPut')", context);
    run("CONFIG_PUT('PARSER', config, 'testParserPut')", ImmutableMap.of("config", brop), context);
    boolean foundMatch = false;
    for(int i = 0;i < 10 && !foundMatch;++i) {
      String bropNew = (String) run("CONFIG_GET('PARSER', 'testParserPut', false)", context);
      foundMatch =  brop.equals(bropNew);
      if(foundMatch) {
        break;
      }
      Thread.sleep(2000);
    }
    assertTrue(foundMatch);
  }

  @Test(expected= ParseException.class)
  public void testPutParserBad() throws InterruptedException {
    {
      UnitTestHelper.setLog4jLevel(ConfigurationFunctions.class, Level.FATAL);
      try {
        run("CONFIG_PUT('PARSER', config, 'brop')", ImmutableMap.of("config", "foo bar"), context);
      } catch(ParseException e) {
        UnitTestHelper.setLog4jLevel(ConfigurationFunctions.class, Level.ERROR);
        throw e;
      }
    }
  }
}
