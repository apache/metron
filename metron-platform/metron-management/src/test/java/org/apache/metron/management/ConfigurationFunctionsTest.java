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
import org.apache.commons.cli.PosixParser;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.apache.log4j.Level;
import org.apache.metron.common.cli.ConfigurationManager;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.test.utils.UnitTestHelper;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;

import static org.apache.metron.TestConstants.PARSER_CONFIGS_PATH;
import static org.apache.metron.TestConstants.SAMPLE_CONFIG_PATH;
import static org.apache.metron.management.utils.FileUtils.slurp;
import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.run;

public class ConfigurationFunctionsTest {
  private static TestingServer testZkServer;
  private static CuratorFramework client;
  private static String zookeeperUrl;
  private Context context = new Context.Builder()
            .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client)
            .build();
  @BeforeClass
  public static void setup() throws Exception {
    testZkServer = new TestingServer(true);
    zookeeperUrl = testZkServer.getConnectString();
    client = ConfigurationsUtils.getClient(zookeeperUrl);
    client.start();

    pushConfigs(SAMPLE_CONFIG_PATH);
    pushConfigs(PARSER_CONFIGS_PATH);


  }

  private static void pushConfigs(String inputPath) throws Exception {
    String[] args = new String[]{
            "-z", zookeeperUrl
            , "--mode", "PUSH"
            , "--input_dir", inputPath
    };
    ConfigurationManager manager = new ConfigurationManager();
    manager.run(ConfigurationManager.ConfigurationOptions.parse(new PosixParser(), args));
  }


  static String goodBroParserConfig = slurp(PARSER_CONFIGS_PATH + "/parsers/bro.json");

  /**
    {
      "sensorTopic" : "brop",
      "parserConfig" : { },
      "fieldTransformations" : [ ],
      "readMetadata":false,
      "mergeMetadata":false,
      "parserParallelism" : 1,
      "errorWriterParallelism" : 1,
      "spoutNumTasks" : 1,
      "stormConfig" : {},
      "errorWriterNumTasks":1,
      "spoutConfig":{},
      "parserNumTasks":1,
      "spoutParallelism":1
    }
   */
  @Multiline
  static String defaultBropParserConfig;


  @Test
  public void testParserGetHappyPath() {

    Object out = run("CONFIG_GET('PARSER', 'bro')", new HashMap<>(), context);
    Assert.assertEquals(goodBroParserConfig, out);
  }

  @Test
  public void testParserGetMissWithoutDefault() {

    {
      Object out = run("CONFIG_GET('PARSER', 'brop', false)", new HashMap<>(), context);
      Assert.assertNull(out);
    }
  }

  @Test
  public void testParserGetMissWithDefault() throws Exception {
    JSONObject expected = (JSONObject) new JSONParser().parse(defaultBropParserConfig);

    {
      Object out = run("CONFIG_GET('PARSER', 'brop')", new HashMap<>(), context);
      JSONObject actual = (JSONObject) new JSONParser().parse(out.toString().trim());
      Assert.assertEquals(expected, actual);
    }
    {
      Object out = run("CONFIG_GET('PARSER', 'brop', true)", new HashMap<>(), context);
      JSONObject actual = (JSONObject) new JSONParser().parse(out.toString().trim());
      Assert.assertEquals(expected, actual);
    }
  }

  static String goodTestEnrichmentConfig = slurp( SAMPLE_CONFIG_PATH + "/enrichments/test.json");

  /**
    {
      "enrichment" : {
        "fieldMap" : { },
        "fieldToTypeMap" : { },
        "config" : { }
      },
      "threatIntel" : {
        "fieldMap" : { },
        "fieldToTypeMap" : { },
        "config" : { },
        "triageConfig" : {
          "riskLevelRules" : [ ],
          "aggregator" : "MAX",
          "aggregationConfig" : { }
        }
      },
      "configuration" : { }
    }
   */
  @Multiline
  static String defaultBropEnrichmentConfig;


  @Test
  public void testEnrichmentGetHappyPath() {

    Object out = run("CONFIG_GET('ENRICHMENT', 'test')", new HashMap<>(), context);
    Assert.assertEquals(goodTestEnrichmentConfig, out.toString().trim());
  }

  @Test
  public void testEnrichmentGetMissWithoutDefault() {

    {
      Object out = run("CONFIG_GET('ENRICHMENT', 'brop', false)", new HashMap<>(), context);
      Assert.assertNull(out);
    }
  }

  @Test
  public void testEnrichmentGetMissWithDefault() throws Exception {
    JSONObject expected = (JSONObject) new JSONParser().parse(defaultBropEnrichmentConfig);

    {
      Object out = run("CONFIG_GET('ENRICHMENT', 'brop')", new HashMap<>(), context);
      JSONObject actual = (JSONObject) new JSONParser().parse(out.toString().trim());
      Assert.assertEquals(expected, actual);
    }
    {
      Object out = run("CONFIG_GET('ENRICHMENT', 'brop', true)", new HashMap<>(), context);
      JSONObject actual = (JSONObject) new JSONParser().parse(out.toString().trim());
      Assert.assertEquals(expected, actual);
    }
  }

  static String goodGlobalConfig = slurp( SAMPLE_CONFIG_PATH+ "/global.json");

  @Test
  public void testGlobalGet() {

    Object out = run("CONFIG_GET('GLOBAL')", new HashMap<>(), context);
    Assert.assertEquals(goodGlobalConfig, out.toString().trim());
  }

  @Test
  public void testGlobalPut() {

    Object out = run("CONFIG_GET('GLOBAL')", new HashMap<>(), context);
    Assert.assertEquals(goodGlobalConfig, out.toString().trim());
  }

  @Test(expected=ParseException.class)
  public void testGlobalPutBad() {
    {
      UnitTestHelper.setLog4jLevel(ConfigurationFunctions.class, Level.FATAL);
      try {
        run("CONFIG_PUT('GLOBAL', 'foo bar')", new HashMap<>(), context);
      } catch(ParseException e) {
        UnitTestHelper.setLog4jLevel(ConfigurationFunctions.class, Level.ERROR);
        throw e;
      }
    }
  }

  @Test
  public void testIndexingPut() throws InterruptedException {
    String brop= (String) run("CONFIG_GET('INDEXING', 'testIndexingPut')", new HashMap<>(), context);
    run("CONFIG_PUT('INDEXING', config, 'testIndexingPut')", ImmutableMap.of("config", brop), context);
    boolean foundMatch = false;
    for(int i = 0;i < 10 && !foundMatch;++i) {
      String bropNew = (String) run("CONFIG_GET('INDEXING', 'testIndexingPut', false)", new HashMap<>(), context);
      foundMatch =  brop.equals(bropNew);
      if(foundMatch) {
        break;
      }
      Thread.sleep(2000);
    }
    Assert.assertTrue(foundMatch);
  }

  @Test(expected= ParseException.class)
  public void testIndexingPutBad() throws InterruptedException {
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
  public void testEnrichmentPut() throws InterruptedException {
    String brop= (String) run("CONFIG_GET('ENRICHMENT', 'testEnrichmentPut')", new HashMap<>(), context);
    run("CONFIG_PUT('ENRICHMENT', config, 'testEnrichmentPut')", ImmutableMap.of("config", brop), context);
    boolean foundMatch = false;
    for(int i = 0;i < 10 && !foundMatch;++i) {
      String bropNew = (String) run("CONFIG_GET('ENRICHMENT', 'testEnrichmentPut', false)", new HashMap<>(), context);
      foundMatch =  brop.equals(bropNew);
      if(foundMatch) {
        break;
      }
      Thread.sleep(2000);
    }
    Assert.assertTrue(foundMatch);
  }

  @Test(expected= ParseException.class)
  public void testEnrichmentPutBad() throws InterruptedException {
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
  public void testParserPut() throws InterruptedException {
    String brop= (String) run("CONFIG_GET('PARSER', 'testParserPut')", new HashMap<>(), context);
    run("CONFIG_PUT('PARSER', config, 'testParserPut')", ImmutableMap.of("config", brop), context);
    boolean foundMatch = false;
    for(int i = 0;i < 10 && !foundMatch;++i) {
      String bropNew = (String) run("CONFIG_GET('PARSER', 'testParserPut', false)", new HashMap<>(), context);
      foundMatch =  brop.equals(bropNew);
      if(foundMatch) {
        break;
      }
      Thread.sleep(2000);
    }
    Assert.assertTrue(foundMatch);
  }

  @Test(expected= ParseException.class)
  public void testParserPutBad() throws InterruptedException {
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
