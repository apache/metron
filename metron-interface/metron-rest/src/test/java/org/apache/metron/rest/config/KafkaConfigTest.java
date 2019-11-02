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

import org.apache.metron.rest.MetronRestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KafkaConfigTest {

  private Environment environment;
  private KafkaConfig kafkaConfig;

  @BeforeEach
  public void setUp() {
    environment = mock(Environment.class);
    kafkaConfig = new KafkaConfig(environment);
  }

  @Test
  public void kafkaConfigShouldProperlyReturnConsumerProperties() {
    when(environment.getProperty(MetronRestConstants.KAFKA_BROKER_URL_SPRING_PROPERTY)).thenReturn("broker urls");
    when(environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)).thenReturn(false);

    Map<String, Object> consumerProperties = kafkaConfig.consumerProperties();
    assertEquals("broker urls", consumerProperties.get("bootstrap.servers"));
    assertNull(consumerProperties.get("security.protocol"));

    when(environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)).thenReturn(true);
    when(environment.getProperty(MetronRestConstants.KAFKA_SECURITY_PROTOCOL_SPRING_PROPERTY)).thenReturn("kafka security protocol");

    consumerProperties = kafkaConfig.consumerProperties();
    assertEquals("kafka security protocol", consumerProperties.get("security.protocol"));
  }

  @Test
  public void kafkaConfigShouldProperlyReturnProducerProperties() {
    when(environment.getProperty(MetronRestConstants.KAFKA_BROKER_URL_SPRING_PROPERTY)).thenReturn("broker urls");
    when(environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)).thenReturn(false);

    Map<String, Object> producerProperties = kafkaConfig.producerProperties();
    assertEquals("broker urls", producerProperties.get("bootstrap.servers"));
    assertNull(producerProperties.get("security.protocol"));

    when(environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)).thenReturn(true);
    when(environment.getProperty(MetronRestConstants.KAFKA_SECURITY_PROTOCOL_SPRING_PROPERTY)).thenReturn("kafka security protocol");

    producerProperties = kafkaConfig.consumerProperties();
    assertEquals("kafka security protocol", producerProperties.get("security.protocol"));
  }


}
