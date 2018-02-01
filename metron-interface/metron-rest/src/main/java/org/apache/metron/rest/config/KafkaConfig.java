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
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.protocol.SecurityProtocol;
import org.apache.metron.common.utils.KafkaUtils;
import org.apache.metron.rest.MetronRestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;

/**
 * Configuration used for connecting to Kafka.
 */
@Configuration
@Profile("!" + TEST_PROFILE)
public class KafkaConfig {
  /**
   * The Spring environment.
   */
  private Environment environment;

  /**
   * Construvtor used to inject {@link Environment}.
   * @param environment Spring environment to inject.
   */
  @Autowired
  public KafkaConfig(final Environment environment) {
    this.environment = environment;
  }

  /**
   * The client used for ZooKeeper.
   */
  @Autowired
  private ZkClient zkClient;

  /**
   * Bean for ZooKeeper
   */
  @Bean
  public ZkUtils zkUtils() {
    return ZkUtils.apply(zkClient, false);
  }

  /**
   * Create properties that will be used by {@link this#createConsumerFactory()}
   *
   * @return Configurations used by {@link this#createConsumerFactory()}.
   */
  @Bean
  public Map<String, Object> consumerProperties() {
    final Map<String, Object> props = new HashMap<>();
    props.put("bootstrap.servers", environment.getProperty(MetronRestConstants.KAFKA_BROKER_URL_SPRING_PROPERTY));
    props.put("group.id", "metron-rest");
    props.put("enable.auto.commit", "false");
    props.put("auto.commit.interval.ms", "1000");
    props.put("session.timeout.ms", "30000");
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    if (environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)) {
      props.put("security.protocol", KafkaUtils.INSTANCE.normalizeProtocol(environment.getProperty(MetronRestConstants.KAFKA_SECURITY_PROTOCOL_SPRING_PROPERTY)));
    }
    return props;
  }

  /**
   * Create a {@link ConsumerFactory} which will be used for certain Kafka interactions within config API.
   *
   * @return a {@link ConsumerFactory} used to create {@link KafkaConsumer} for interactions with Kafka.
   */
  @Bean
  public ConsumerFactory<String, String> createConsumerFactory() {
    return new DefaultKafkaConsumerFactory<>(consumerProperties());
  }

  @Bean
  public Map<String, Object> producerProperties() {
    Map<String, Object> producerConfig = new HashMap<>();
    producerConfig.put("bootstrap.servers", environment.getProperty(MetronRestConstants.KAFKA_BROKER_URL_SPRING_PROPERTY));
    producerConfig.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    producerConfig.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    producerConfig.put("request.required.acks", 1);
    if (environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)) {
      producerConfig.put("security.protocol", KafkaUtils.INSTANCE.normalizeProtocol(environment.getProperty(MetronRestConstants.KAFKA_SECURITY_PROTOCOL_SPRING_PROPERTY)));
    }
    return producerConfig;
  }



  @Bean
  public KafkaProducer kafkaProducer() {
    return new KafkaProducer<>(producerProperties());
  }

  /**
   * Create a bean for {@link AdminUtils$}. This is primarily done to make testing a bit easier.
   *
   * @return {@link AdminUtils$} is written in scala. We return a reference to this class.
   */
  @Bean
  public AdminUtils$ adminUtils() {
    return AdminUtils$.MODULE$;
  }
}
