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
package org.apache.metron.rest.config;

import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.io.IOUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.zookeeper.ConfigurationsCache;
import org.apache.metron.common.zookeeper.ZKConfigurationsCache;
import org.apache.metron.hbase.client.FakeHBaseClient;
import org.apache.metron.hbase.client.FakeHBaseClientFactory;
import org.apache.metron.hbase.client.FakeHBaseConnectionFactory;
import org.apache.metron.hbase.client.HBaseClient;
import org.apache.metron.hbase.client.HBaseClientFactory;
import org.apache.metron.hbase.client.HBaseConnectionFactory;
import org.apache.metron.hbase.client.LegacyHBaseClient;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.UnableToStartException;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.job.manager.InMemoryJobManager;
import org.apache.metron.job.manager.JobManager;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.mock.MockPcapJob;
import org.apache.metron.rest.mock.MockPcapJobSupplier;
import org.apache.metron.rest.mock.MockPcapToPdmlScriptWrapper;
import org.apache.metron.rest.mock.MockStormCLIClientWrapper;
import org.apache.metron.rest.mock.MockStormRestTemplate;
import org.apache.metron.rest.service.StormStatusService;
import org.apache.metron.rest.service.impl.CachedStormStatusServiceImpl;
import org.apache.metron.rest.service.impl.PcapToPdmlScriptWrapper;
import org.apache.metron.rest.service.impl.StormCLIWrapper;
import org.apache.metron.rest.user.HBaseUserSettingsClient;
import org.apache.metron.rest.user.UserSettingsClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;

@Configuration
@Profile(TEST_PROFILE)
public class TestConfig {

  @Autowired
  private HBaseConnectionFactory hBaseConnectionFactory;

  @Autowired
  private org.apache.hadoop.conf.Configuration hBaseConfiguration;

  @Autowired
  private HBaseClientFactory hBaseClientFactory;

  @Bean
  public Properties zkProperties() {
    return new Properties();
  }

  @Bean
  public ZKServerComponent zkServerComponent(Properties zkProperties) {
    return new ZKServerComponent()
      .withPostStartCallback((zkComponent) -> zkProperties.setProperty(ZKServerComponent.ZOOKEEPER_PROPERTY, zkComponent.getConnectionString()));
  }

  @Bean
  public KafkaComponent kafkaWithZKComponent(Properties zkProperties) {
    return new KafkaComponent().withTopologyProperties(zkProperties);
  }

  @Bean(initMethod = "start", destroyMethod="close")
  public ConfigurationsCache cache(CuratorFramework client) {
    return new ZKConfigurationsCache( client
                                    , ZKConfigurationsCache.ConfiguredTypes.ENRICHMENT
                                    , ZKConfigurationsCache.ConfiguredTypes.PARSER
                                    , ZKConfigurationsCache.ConfiguredTypes.INDEXING
                                    );
  }

  @Bean(destroyMethod = "stop")
  public ComponentRunner componentRunner(ZKServerComponent zkServerComponent, KafkaComponent kafkaWithZKComponent) {
    ComponentRunner runner = new ComponentRunner.Builder()
      .withComponent("zk", zkServerComponent)
      .withCustomShutdownOrder(new String[]{"search", "zk"})
      .build();
    try {
      runner.start();
      File globalConfigFile = new File("src/test/resources/zookeeper/global.json");
      try(BufferedReader r = new BufferedReader(new FileReader(globalConfigFile))){
        String globalConfig = IOUtils.toString(r);
        ConfigurationsUtils.writeGlobalConfigToZookeeper(globalConfig.getBytes(), zkServerComponent.getConnectionString());
      } catch (Exception e) {
        throw new IllegalStateException("Unable to upload global config", e);
      }
    } catch (UnableToStartException e) {
      e.printStackTrace();
    }
    return runner;
  }

  @Bean(initMethod = "start", destroyMethod = "close")
  public CuratorFramework client(ComponentRunner componentRunner) {
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    ZKServerComponent zkServerComponent = componentRunner.getComponent("zk", ZKServerComponent.class);
    return CuratorFrameworkFactory.newClient(zkServerComponent.getConnectionString(), retryPolicy);
  }

  @Bean(destroyMethod = "close")
  public ZkClient zkClient(ComponentRunner componentRunner) {
    ZKServerComponent zkServerComponent = componentRunner.getComponent("zk", ZKServerComponent.class);
    return new ZkClient(zkServerComponent.getConnectionString(), 10000, 10000, ZKStringSerializer$.MODULE$);
  }

  @Bean
  public ZkUtils zkUtils(ZkClient zkClient) {
    return ZkUtils.apply(zkClient, false);
  }

  @Bean
  public Map<String, Object> kafkaConsumer(KafkaComponent kafkaWithZKComponent) {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaWithZKComponent.getBrokerList());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "metron-config");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
    return props;
  }

  @Bean
  public ConsumerFactory<String, String> createConsumerFactory() {
    return new DefaultKafkaConsumerFactory<>(kafkaConsumer(kafkaWithZKComponent(zkProperties())));
  }

  @Bean
  public Map<String, Object> producerProperties(KafkaComponent kafkaWithZKComponent) {
    Map<String, Object> producerConfig = new HashMap<>();
    producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaWithZKComponent.getBrokerList());
    producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
    producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
    producerConfig.put(ProducerConfig.ACKS_CONFIG, "1");
    return producerConfig;
  }

  @Bean
  public KafkaProducer kafkaProducer(KafkaComponent kafkaWithZKComponent) {
    return new KafkaProducer<>(producerProperties(kafkaWithZKComponent));
  }

  @Bean
  public StormCLIWrapper stormCLIClientWrapper() {
    return new MockStormCLIClientWrapper();
  }

  @Bean
  public RestTemplate restTemplate(StormCLIWrapper stormCLIClientWrapper) {
    MockStormRestTemplate restTemplate = new MockStormRestTemplate();
    restTemplate.setMockStormCLIClientWrapper((MockStormCLIClientWrapper) stormCLIClientWrapper);
    return restTemplate;
  }

  @Bean
  public AdminClient adminUtils(KafkaComponent kafkaWithZKComponent) {
    Map<String, Object> adminConfig = new HashMap<>();
    adminConfig.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaWithZKComponent.getBrokerList());
    return AdminClient.create(adminConfig);
  }

  @Bean(destroyMethod = "close")
  public UserSettingsClient userSettingsClient() {
    Map<String, Object> globals = new HashMap<String, Object>() {{
      put(HBaseUserSettingsClient.USER_SETTINGS_HBASE_TABLE, "user_settings");
      put(HBaseUserSettingsClient.USER_SETTINGS_HBASE_CF, "cf");
    }};

    UserSettingsClient userSettingsClient = new HBaseUserSettingsClient(
            () -> globals,
            hBaseClientFactory,
            hBaseConnectionFactory,
            hBaseConfiguration);
    userSettingsClient.init();
    return userSettingsClient;
  }

  @Bean
  public HBaseConnectionFactory hBaseConnectionFactory() {
    return new FakeHBaseConnectionFactory();
  }

  @Bean
  org.apache.hadoop.conf.Configuration hBaseConfiguration() {
    return HBaseConfiguration.create();
  }

  @Bean
  HBaseClientFactory hBaseClientFactory() {
    return new FakeHBaseClientFactory();
  }

  @Bean(destroyMethod = "close")
  public HBaseClient hBaseClient() {
    return new FakeHBaseClient();
  }

  @Bean()
  public LegacyHBaseClient legacyHBaseClient() throws RestException, IOException {
    final String cf = "t";
    final String cq = "v";
    HTableInterface table = MockHBaseTableProvider.addToCache("enrichment_list", cf);
    List<String> enrichmentTypes = new ArrayList<String>() {{
      add("foo");
      add("bar");
      add("baz");
    }};
    for (String type : enrichmentTypes) {
      Put put = new Put(Bytes.toBytes(type));
      put.addColumn(Bytes.toBytes(cf), Bytes.toBytes(cq), "{}".getBytes(StandardCharsets.UTF_8));
      table.put(put);
    }
    return new LegacyHBaseClient(new MockHBaseTableProvider(), HBaseConfiguration.create(), "enrichment_list");
  }

  @Bean
  public JobManager jobManager() {
    return new InMemoryJobManager();
  }

  @Bean
  public MockPcapJob mockPcapJob() {
    return new MockPcapJob();
  }

  @Bean
  public PcapJobSupplier pcapJobSupplier(MockPcapJob mockPcapJob) {
    MockPcapJobSupplier mockPcapJobSupplier = new MockPcapJobSupplier();
    mockPcapJobSupplier.setMockPcapJob(mockPcapJob);
    return mockPcapJobSupplier;
  }

  @Bean
  public PcapToPdmlScriptWrapper pcapToPdmlScriptWrapper() {
    return new MockPcapToPdmlScriptWrapper();
  }

  @Bean
  public StormStatusService stormStatusService(
      @Autowired @Qualifier("StormStatusServiceImpl") StormStatusService wrappedService) {
    long maxCacheSize = 0L;
    long maxCacheTimeoutSeconds = 0L;
    return new CachedStormStatusServiceImpl(wrappedService, maxCacheSize, maxCacheTimeoutSeconds);
  }
}
