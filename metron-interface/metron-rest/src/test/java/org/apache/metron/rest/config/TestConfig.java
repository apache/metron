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

import kafka.admin.AdminUtils$;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.metron.bundles.util.BundleProperties;
import org.apache.metron.enrichment.integration.components.ConfigUploadComponent;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.UnableToStartException;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.rest.mock.MockStormCLIClientWrapper;
import org.apache.metron.rest.mock.MockStormRestTemplate;
import org.apache.metron.rest.service.impl.StormCLIWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;

@Configuration
@Profile(TEST_PROFILE)
public class TestConfig {

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

  @Bean
  public ConfigUploadComponent configUploadComponent(Properties zkProperties) {
    ConfigUploadComponent component = null;
    try {
      // copy the correct things in
      copyResources("./src/test/resources", "./target/remote");
      component = new ConfigUploadComponent()
              .withTopologyProperties(zkProperties)
              .withGlobalConfigsPath("./target/remote/zookeeper/");
    }catch(Exception e) {
      e.printStackTrace();
    }
    return component;
  }

  public static void copyResources(String source, String target) throws IOException {
    final java.nio.file.Path sourcePath = Paths.get(source);
    final java.nio.file.Path targetPath = Paths.get(target);

    Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult preVisitDirectory(java.nio.file.Path dir, BasicFileAttributes attrs)
              throws IOException {

        java.nio.file.Path relativeSource = sourcePath.relativize(dir);
        java.nio.file.Path target = targetPath.resolve(relativeSource);

        if(!Files.exists(target)) {
          Files.createDirectories(target);
        }
        return FileVisitResult.CONTINUE;

      }

      @Override
      public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs)
              throws IOException {

        java.nio.file.Path relativeSource = sourcePath.relativize(file);
        java.nio.file.Path target = targetPath.resolve(relativeSource);

        Files.copy(file, target, REPLACE_EXISTING);

        return FileVisitResult.CONTINUE;
      }
    });
  }
  //@Bean(destroyMethod = "stop")
  @Bean(destroyMethod = "stop")
  public ComponentRunner componentRunner(ZKServerComponent zkServerComponent, KafkaComponent kafkaWithZKComponent, ConfigUploadComponent configUploadComponent) {
    ComponentRunner runner = new ComponentRunner.Builder()
      .withComponent("zk", zkServerComponent)
            .withComponent("configUpload", configUploadComponent)
      .withCustomShutdownOrder(new String[]{"configUpload","search","zk"})
      .build();
    try {
      runner.start();
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

}
