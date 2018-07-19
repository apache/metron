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

import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import kafka.admin.AdminUtils$;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.io.IOUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.zookeeper.ConfigurationsCache;
import org.apache.metron.common.zookeeper.ZKConfigurationsCache;
import org.apache.metron.hbase.client.UserSettingsClient;
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
import org.apache.metron.rest.mock.MockStormCLIClientWrapper;
import org.apache.metron.rest.mock.MockStormRestTemplate;
import org.apache.metron.rest.service.impl.StormCLIWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@Profile(TEST_PROFILE)
public class TestConfig {

  static {
    MockHBaseTableProvider.addToCache("updates", "t");
  }

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
    props.put("bootstrap.servers", kafkaWithZKComponent.getBrokerList());
    props.put("group.id", "metron-config");
    props.put("enable.auto.commit", "false");
    props.put("auto.commit.interval.ms", "1000");
    props.put("session.timeout.ms", "30000");
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    return props;
  }

  @Bean
  public ConsumerFactory<String, String> createConsumerFactory() {
    return new DefaultKafkaConsumerFactory<>(kafkaConsumer(kafkaWithZKComponent(zkProperties())));
  }

  @Bean
  public Map<String, Object> producerProperties(KafkaComponent kafkaWithZKComponent) {
    Map<String, Object> producerConfig = new HashMap<>();
    producerConfig.put("bootstrap.servers", kafkaWithZKComponent.getBrokerList());
    producerConfig.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    producerConfig.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    producerConfig.put("request.required.acks", 1);
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
  public AdminUtils$ adminUtils() {
    return AdminUtils$.MODULE$;
  }


  @Bean()
  public UserSettingsClient userSettingsClient() throws RestException, IOException {
    return new UserSettingsClient(new MockHBaseTableProvider().addToCache("user_settings", "cf"), Bytes.toBytes("cf"));
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
}
