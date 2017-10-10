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
package org.apache.metron.common.zookeeper;

import com.fasterxml.jackson.core.type.TypeReference;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.io.IOUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.apache.metron.TestConstants;
import org.apache.metron.common.configuration.*;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.integration.components.ZKServerComponent;
import org.junit.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

public class ZKConfigurationsCacheIntegrationTest {
  private CuratorFramework client;

  /**
   {
    "hdfs" : {
      "index": "yaf",
      "batchSize": 1,
      "enabled" : true
    },
    "elasticsearch" : {
    "index": "yaf",
    "batchSize": 25,
    "batchTimeout": 7,
    "enabled" : false
    },
    "solr" : {
    "index": "yaf",
    "batchSize": 5,
    "enabled" : false
    }
  }
   */
  @Multiline
  public static String testIndexingConfig;

  /**
   {
    "enrichment": {
      "fieldMap": { }
    ,"fieldToTypeMap": { }
   },
    "threatIntel": {
      "fieldMap": { },
      "fieldToTypeMap": { },
      "triageConfig" : { }
     }
   }
   */
  @Multiline
  public static String testEnrichmentConfig;

  /**
  {
  "parserClassName":"org.apache.metron.parsers.bro.BasicBroParser",
  "sensorTopic":"brop",
  "parserConfig": {}
  }
   */
  @Multiline
  public static String testParserConfig;

  /**
   *{
  "es.clustername": "metron",
  "es.ip": "localhost",
  "es.port": 9300,
  "es.date.format": "yyyy.MM.dd.HH",
   }
   */
  @Multiline
  public static String globalConfig;

  public ConfigurationsCache cache;

  public ZKServerComponent zkComponent;

  @Before
  public void setup() throws Exception {
    zkComponent = new ZKServerComponent();
    zkComponent.start();
    client = ConfigurationsUtils.getClient(zkComponent.getConnectionString());
    client.start();
    {
      //parser
      byte[] config = IOUtils.toByteArray(new FileInputStream(new File(TestConstants.PARSER_CONFIGS_PATH + "/parsers/bro.json")));
      ConfigurationsUtils.writeSensorParserConfigToZookeeper("bro", config, client);
    }
    {
      //indexing
      byte[] config = IOUtils.toByteArray(new FileInputStream(new File(TestConstants.SAMPLE_CONFIG_PATH + "/indexing/test.json")));
      ConfigurationsUtils.writeSensorIndexingConfigToZookeeper("test", config, client);
    }
    {
      //enrichments
      byte[] config = IOUtils.toByteArray(new FileInputStream(new File(TestConstants.SAMPLE_CONFIG_PATH + "/enrichments/test.json")));
      ConfigurationsUtils.writeSensorEnrichmentConfigToZookeeper("test", config, client);
    }
    {
      //global config
      byte[] config = IOUtils.toByteArray(new FileInputStream(new File(TestConstants.SAMPLE_CONFIG_PATH + "/global.json")));
      ConfigurationsUtils.writeGlobalConfigToZookeeper(config, client);
    }
    cache = ZKConfigurationsCache.INSTANCE;
  }

  @After
  public void teardown() throws Exception {
    if(cache != null) {
      cache.reset();
    }
    if(client != null) {
      client.close();
    }
    if(zkComponent != null) {
      zkComponent.stop();
    }
  }

  @Test
  public void validateDelete() throws IOException {

  }

  @Test
  public void validateUpdate() throws IOException {

  }

  @Test
  public void validateBaseWrite() throws IOException {
    File globalConfigFile = new File(TestConstants.SAMPLE_CONFIG_PATH + "/global.json");
    Map<String, Object> expectedGlobalConfig = JSONUtils.INSTANCE.load(globalConfigFile, new TypeReference<Map<String, Object>>() { });
    //indexing
    {
      File inFile = new File(TestConstants.SAMPLE_CONFIG_PATH + "/indexing/test.json");
      Map<String, Object> expectedConfig = JSONUtils.INSTANCE.load(inFile, new TypeReference<Map<String, Object>>() {
      });
      IndexingConfigurations config = cache.get(client, IndexingConfigurations.class);
      Assert.assertEquals(expectedConfig, config.getSensorIndexingConfig("test"));
      Assert.assertEquals(expectedGlobalConfig, config.getGlobalConfig());
      Assert.assertNull(config.getSensorIndexingConfig("notthere", false));
    }
    //enrichment
    {
      File inFile = new File(TestConstants.SAMPLE_CONFIG_PATH + "/enrichments/test.json");
      SensorEnrichmentConfig expectedConfig = JSONUtils.INSTANCE.load(inFile, SensorEnrichmentConfig.class);
      EnrichmentConfigurations config = cache.get(client, EnrichmentConfigurations.class);
      Assert.assertEquals(expectedConfig, config.getSensorEnrichmentConfig("test"));
      Assert.assertEquals(expectedGlobalConfig, config.getGlobalConfig());
      Assert.assertNull(config.getSensorEnrichmentConfig("notthere"));
    }
    //parsers
    {
      File inFile = new File(TestConstants.PARSER_CONFIGS_PATH + "/parsers/bro.json");
      SensorParserConfig expectedConfig = JSONUtils.INSTANCE.load(inFile, SensorParserConfig.class);
      ParserConfigurations config = cache.get(client, ParserConfigurations.class);
      Assert.assertEquals(expectedConfig, config.getSensorParserConfig("bro"));
      Assert.assertEquals(expectedGlobalConfig, config.getGlobalConfig());
      Assert.assertNull(config.getSensorParserConfig("notthere"));
    }
  }
}
