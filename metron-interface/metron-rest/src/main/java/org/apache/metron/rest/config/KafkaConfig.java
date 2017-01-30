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
import org.apache.metron.rest.MetronRestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.Properties;

import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;

@Configuration
@Profile("!" + TEST_PROFILE)
public class KafkaConfig {

  @Autowired
  private Environment environment;

  @Autowired
  private ZkClient zkClient;

  @Bean
  public ZkUtils zkUtils() {
    return ZkUtils.apply(zkClient, false);
  }

  @Bean(destroyMethod = "close")
  public KafkaConsumer<String, String> kafkaConsumer() {
    Properties props = new Properties();
    props.put("bootstrap.servers", environment.getProperty(MetronRestConstants.KAFKA_BROKER_URL_SPRING_PROPERTY));
    props.put("group.id", "metron-config");
    props.put("enable.auto.commit", "false");
    props.put("auto.commit.interval.ms", "1000");
    props.put("session.timeout.ms", "30000");
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    return new KafkaConsumer<>(props);
  }

  @Bean
  public AdminUtils$ adminUtils() {
    return AdminUtils$.MODULE$;
  }
}
