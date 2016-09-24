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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.cli.PosixParser;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.apache.metron.TestConstants;
import org.apache.metron.common.cli.ConfigurationManager;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.stellar.StellarTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;

import static org.apache.metron.TestConstants.PARSER_CONFIGS_PATH;
import static org.apache.metron.TestConstants.SAMPLE_CONFIG_PATH;
import static org.apache.metron.management.utils.FileUtils.slurp;

public class ConfigurationFunctionsTest {
  private TestingServer testZkServer;
  private CuratorFramework client;
  private String zookeeperUrl;
  private Context context = new Context.Builder()
            .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client)
            .build();
  @Before
  public void setup() throws Exception {
    testZkServer = new TestingServer(true);
    zookeeperUrl = testZkServer.getConnectString();
    client = ConfigurationsUtils.getClient(zookeeperUrl);
    client.start();

    pushConfigs(SAMPLE_CONFIG_PATH);
    pushConfigs(PARSER_CONFIGS_PATH);


  }

  private void pushConfigs(String inputPath) throws Exception {
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
  "fieldTransformations" : [ ]
}*/
  @Multiline
  static String defaultBropParserConfig;


  @Test
  public void testParserGetHappyPath() {

    Object out = StellarTest.run("CONFIG_GET('PARSER', 'bro')", new HashMap<>(), context);
    Assert.assertEquals(goodBroParserConfig, out);
  }

  @Test
  public void testParserGetMissWithoutDefault() {

    {
      Object out = StellarTest.run("CONFIG_GET('PARSER', 'brop', false)", new HashMap<>(), context);
      Assert.assertNull(out);
    }
  }

  @Test
  public void testParserGetMissWithDefault() {

    {
      Object out = StellarTest.run("CONFIG_GET('PARSER', 'brop')", new HashMap<>(), context);
      Assert.assertEquals(defaultBropParserConfig, out);
    }
    {
      Object out = StellarTest.run("CONFIG_GET('PARSER', 'brop', true)", new HashMap<>(), context);
      Assert.assertEquals(defaultBropParserConfig, out);
    }
  }

  static String goodTestEnrichmentConfig = slurp( SAMPLE_CONFIG_PATH + "/enrichments/test.json");

  /**
{
  "index" : "brop",
  "batchSize" : 0,
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
      "riskLevelRules" : { },
      "aggregator" : "MAX",
      "aggregationConfig" : { }
    }
  },
  "configuration" : { }
}*/
  @Multiline
  static String defaultBropEnrichmentConfig;


  @Test
  public void testEnrichmentGetHappyPath() {

    Object out = StellarTest.run("CONFIG_GET('ENRICHMENT', 'test')", new HashMap<>(), context);
    Assert.assertEquals(goodTestEnrichmentConfig, out.toString().trim());
  }

  @Test
  public void testEnrichmentGetMissWithoutDefault() {

    {
      Object out = StellarTest.run("CONFIG_GET('ENRICHMENT', 'brop', false)", new HashMap<>(), context);
      Assert.assertNull(out);
    }
  }

  @Test
  public void testEnrichmentGetMissWithDefault() {

    {
      Object out = StellarTest.run("CONFIG_GET('ENRICHMENT', 'brop')", new HashMap<>(), context);
      Assert.assertEquals(defaultBropEnrichmentConfig, out.toString().trim());
    }
    {
      Object out = StellarTest.run("CONFIG_GET('ENRICHMENT', 'brop', true)", new HashMap<>(), context);
      Assert.assertEquals(defaultBropEnrichmentConfig, out.toString().trim());
    }
  }

  static String goodGlobalConfig = slurp( SAMPLE_CONFIG_PATH+ "/global.json");

  @Test
  public void testGlobalGet() {

    Object out = StellarTest.run("CONFIG_GET('GLOBAL')", new HashMap<>(), context);
    Assert.assertEquals(goodGlobalConfig, out.toString().trim());
  }

  @Test
  public void testGlobalPut() {

    Object out = StellarTest.run("CONFIG_GET('GLOBAL')", new HashMap<>(), context);
    Assert.assertEquals(goodGlobalConfig, out.toString().trim());
  }

  @Test(expected=ParseException.class)
  public void testGlobalPutBad() {
    StellarTest.run("CONFIG_PUT('GLOBAL', 'foo bar')", new HashMap<>(), context);
  }

  @Test
  public void testEnrichmentPut() throws InterruptedException {
    String brop= (String) StellarTest.run("CONFIG_GET('ENRICHMENT', 'testEnrichmentPut')", new HashMap<>(), context);
    StellarTest.run("CONFIG_PUT('ENRICHMENT', config, 'testEnrichmentPut')", ImmutableMap.of("config", brop), context);
    String bropNew= (String) StellarTest.run("CONFIG_GET('ENRICHMENT', 'testEnrichmentPut', false)", new HashMap<>(), context);
    Assert.assertEquals(brop, bropNew);
  }

  @Test(expected= ParseException.class)
  public void testEnrichmentPutBad() throws InterruptedException {
    {
      StellarTest.run("CONFIG_PUT('ENRICHMENT', config, 'brop')", ImmutableMap.of("config", "foo bar"), context);
    }
  }

  @Test
  public void testParserPut() throws InterruptedException {
    String brop= (String) StellarTest.run("CONFIG_GET('PARSER', 'testParserPut')", new HashMap<>(), context);
    StellarTest.run("CONFIG_PUT('PARSER', config, 'testEnrichmentPut')", ImmutableMap.of("config", brop), context);
    String bropNew= (String) StellarTest.run("CONFIG_GET('PARSER', 'testEnrichmentPut', false)", new HashMap<>(), context);
    Assert.assertEquals(brop, bropNew);
  }

  @Test(expected= ParseException.class)
  public void testParserPutBad() throws InterruptedException {
    {
      StellarTest.run("CONFIG_PUT('PARSER', config, 'brop')", ImmutableMap.of("config", "foo bar"), context);
    }
  }
}
