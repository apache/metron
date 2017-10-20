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
import org.apache.metron.TestConstants;
import org.apache.metron.common.configuration.*;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.common.configuration.profiler.ProfilerConfigurations;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.integration.components.ZKServerComponent;
import org.junit.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import static org.apache.metron.integration.utils.TestUtils.assertEventually;

public class ZKConfigurationsCacheIntegrationTest {
  private CuratorFramework client;

  /**
   {
  "profiles": [
    {
      "profile": "example2",
      "foreach": "ip_src_addr",
      "onlyif": "protocol == 'HTTP'",
      "init": {
        "total_bytes": 0.0
      },
      "update": {
        "total_bytes": "total_bytes + bytes_in"
      },
      "result": "total_bytes",
      "expires": 30
    }
  ]
}
   */
  @Multiline
  public static String profilerConfig;
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
  "es.date.format": "yyyy.MM.dd.HH"
   }
   */
  @Multiline
  public static String globalConfig;

  public static File profilerDir = new File("../../metron-analytics/metron-profiler/src/test/config/zookeeper");
  public ConfigurationsCache cache;

  public ZKServerComponent zkComponent;

  @Before
  public void setup() throws Exception {
    zkComponent = new ZKServerComponent();
    zkComponent.start();
    client = ConfigurationsUtils.getClient(zkComponent.getConnectionString());
    client.start();
    cache = new ZKConfigurationsCache(client);
    cache.start();
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
      //enrichments
      byte[] config = IOUtils.toByteArray(new FileInputStream(new File(TestConstants.SAMPLE_CONFIG_PATH + "/enrichments/test.json")));
      ConfigurationsUtils.writeSensorEnrichmentConfigToZookeeper("test", config, client);
    }
    {
      //profiler
      byte[] config = IOUtils.toByteArray(new FileInputStream(new File(profilerDir, "/readme-example-1/profiler.json")));
      ConfigurationsUtils.writeProfilerConfigToZookeeper( config, client);
    }
    {
      //global config
      byte[] config = IOUtils.toByteArray(new FileInputStream(new File(TestConstants.SAMPLE_CONFIG_PATH + "/global.json")));
      ConfigurationsUtils.writeGlobalConfigToZookeeper(config, client);
    }
  }

  @After
  public void teardown() throws Exception {
    if(cache != null) {
      cache.close();
    }
    if(client != null) {
      client.close();
    }
    if(zkComponent != null) {
      zkComponent.stop();
    }
  }


  @Test
  public void validateDelete() throws Exception {
    client.delete().forPath(ConfigurationType.GLOBAL.getZookeeperRoot());
    client.delete().forPath(ConfigurationType.INDEXING.getZookeeperRoot() + "/test");
    client.delete().forPath(ConfigurationType.ENRICHMENT.getZookeeperRoot() + "/test");
    client.delete().forPath(ConfigurationType.PARSER.getZookeeperRoot() + "/bro");
    client.delete().forPath(ConfigurationType.PROFILER.getZookeeperRoot() );
    //global
    {
      IndexingConfigurations config = cache.get( IndexingConfigurations.class);
      assertEventually(() -> Assert.assertNull(config.getGlobalConfig(false)));
    }
    //indexing
    {
      IndexingConfigurations config = cache.get( IndexingConfigurations.class);
      assertEventually(() -> Assert.assertNull(config.getSensorIndexingConfig("test", false)));
      assertEventually(() -> Assert.assertNull(config.getGlobalConfig(false)));
    }
    //enrichment
    {
      EnrichmentConfigurations config = cache.get( EnrichmentConfigurations.class);
      assertEventually(() -> Assert.assertNull(config.getSensorEnrichmentConfig("test")));
      assertEventually(()-> Assert.assertNull(config.getGlobalConfig(false)));
    }
    //parser
    {
      ParserConfigurations config = cache.get( ParserConfigurations.class);
      assertEventually(() -> Assert.assertNull(config.getSensorParserConfig("bro")));
      assertEventually(() -> Assert.assertNull(config.getGlobalConfig(false)));
    }
    //profiler
    {
      ProfilerConfigurations config = cache.get( ProfilerConfigurations.class);
      assertEventually(() -> Assert.assertNull(config.getProfilerConfig()));
      assertEventually(() -> Assert.assertNull(config.getGlobalConfig(false)));
    }
  }

  @Test
  public void validateUpdate() throws Exception {
    ConfigurationsUtils.writeSensorIndexingConfigToZookeeper("test", testIndexingConfig.getBytes(), client);
    ConfigurationsUtils.writeGlobalConfigToZookeeper(globalConfig.getBytes(), client);
    ConfigurationsUtils.writeSensorEnrichmentConfigToZookeeper("test", testEnrichmentConfig.getBytes(), client);
    ConfigurationsUtils.writeSensorParserConfigToZookeeper("bro", testParserConfig.getBytes(), client);
    ConfigurationsUtils.writeProfilerConfigToZookeeper( profilerConfig.getBytes(), client);
    //indexing
    {
      Map<String, Object> expectedConfig = JSONUtils.INSTANCE.load(testIndexingConfig, new TypeReference<Map<String, Object>>() {});
      IndexingConfigurations config = cache.get( IndexingConfigurations.class);
      assertEventually(() -> Assert.assertEquals(expectedConfig, config.getSensorIndexingConfig("test")));
    }
    //enrichment
    {
      SensorEnrichmentConfig expectedConfig = JSONUtils.INSTANCE.load(testEnrichmentConfig, SensorEnrichmentConfig.class);
      Map<String, Object> expectedGlobalConfig = JSONUtils.INSTANCE.load(globalConfig, new TypeReference<Map<String, Object>>() {});
      EnrichmentConfigurations config = cache.get( EnrichmentConfigurations.class);
      assertEventually(() -> Assert.assertEquals(expectedConfig, config.getSensorEnrichmentConfig("test")));
      assertEventually(() -> Assert.assertEquals(expectedGlobalConfig, config.getGlobalConfig()));
    }
    //parsers
    {
      SensorParserConfig expectedConfig = JSONUtils.INSTANCE.load(testParserConfig, SensorParserConfig.class);
      ParserConfigurations config = cache.get( ParserConfigurations.class);
      assertEventually(() -> Assert.assertEquals(expectedConfig, config.getSensorParserConfig("bro")));
    }
    //profiler
    {
      ProfilerConfig expectedConfig = JSONUtils.INSTANCE.load(profilerConfig, ProfilerConfig.class);
      ProfilerConfigurations config = cache.get( ProfilerConfigurations.class);
      assertEventually(() -> Assert.assertEquals(expectedConfig, config.getProfilerConfig()));
    }
  }

  @Test
  public void validateBaseWrite() throws Exception {
    File globalConfigFile = new File(TestConstants.SAMPLE_CONFIG_PATH + "/global.json");
    Map<String, Object> expectedGlobalConfig = JSONUtils.INSTANCE.load(globalConfigFile, new TypeReference<Map<String, Object>>() { });
    //indexing
    {
      File inFile = new File(TestConstants.SAMPLE_CONFIG_PATH + "/indexing/test.json");
      Map<String, Object> expectedConfig = JSONUtils.INSTANCE.load(inFile, new TypeReference<Map<String, Object>>() {
      });
      IndexingConfigurations config = cache.get( IndexingConfigurations.class);
      assertEventually(() -> Assert.assertEquals(expectedConfig, config.getSensorIndexingConfig("test")));
      assertEventually(() -> Assert.assertEquals(expectedGlobalConfig, config.getGlobalConfig()));
      assertEventually(() -> Assert.assertNull(config.getSensorIndexingConfig("notthere", false)));
    }
    //enrichment
    {
      File inFile = new File(TestConstants.SAMPLE_CONFIG_PATH + "/enrichments/test.json");
      SensorEnrichmentConfig expectedConfig = JSONUtils.INSTANCE.load(inFile, SensorEnrichmentConfig.class);
      EnrichmentConfigurations config = cache.get( EnrichmentConfigurations.class);
      assertEventually(() -> Assert.assertEquals(expectedConfig, config.getSensorEnrichmentConfig("test")));
      assertEventually(() -> Assert.assertEquals(expectedGlobalConfig, config.getGlobalConfig()));
      assertEventually(() -> Assert.assertNull(config.getSensorEnrichmentConfig("notthere")));
    }
    //parsers
    {
      File inFile = new File(TestConstants.PARSER_CONFIGS_PATH + "/parsers/bro.json");
      SensorParserConfig expectedConfig = JSONUtils.INSTANCE.load(inFile, SensorParserConfig.class);
      ParserConfigurations config = cache.get( ParserConfigurations.class);
      assertEventually(() -> Assert.assertEquals(expectedConfig, config.getSensorParserConfig("bro")));
      assertEventually(() -> Assert.assertEquals(expectedGlobalConfig, config.getGlobalConfig()));
      assertEventually(() -> Assert.assertNull(config.getSensorParserConfig("notthere")));
    }
    //profiler
    {
      File inFile = new File(profilerDir, "/readme-example-1/profiler.json");
      ProfilerConfig expectedConfig = JSONUtils.INSTANCE.load(inFile, ProfilerConfig.class);
      ProfilerConfigurations config = cache.get( ProfilerConfigurations.class);
      assertEventually(() -> Assert.assertEquals(expectedConfig, config.getProfilerConfig()));
      assertEventually(() -> Assert.assertEquals(expectedGlobalConfig, config.getGlobalConfig()));
    }
  }
}
