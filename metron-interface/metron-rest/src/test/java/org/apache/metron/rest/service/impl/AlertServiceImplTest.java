/*
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
package org.apache.metron.rest.service.impl;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.util.HashMap;
import java.util.Map;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.service.AlertService;
import org.apache.metron.rest.service.KafkaService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.env.Environment;

@SuppressWarnings("unchecked")
public class AlertServiceImplTest {

  private KafkaService kafkaService;
  private Environment environment;
  private AlertService alertService;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    kafkaService = mock(KafkaService.class);
    environment = mock(Environment.class);
    alertService = new AlertServiceImpl(kafkaService, environment);
  }

  @Test
  public void produceMessageShouldProperlyProduceMessage() throws Exception {
    String escalationTopic = "escalation";
    final Map<String, Object> message = new HashMap<>();
    message.put("field", "value");

    when(environment.getProperty(MetronRestConstants.KAFKA_TOPICS_ESCALATION_PROPERTY)).thenReturn(escalationTopic);

    alertService.escalateAlert(message);

    String expectedMessage = "{\"field\":\"value\"}";
    verify(kafkaService).produceMessage("escalation", expectedMessage);
    verifyZeroInteractions(kafkaService);
  }
}
