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
package org.apache.metron.rest.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.AlertService;
import org.apache.metron.rest.service.KafkaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * The default service layer implementation of {@link AlertService}.
 *
 * @see AlertService
 */
@Service
public class AlertServiceImpl implements AlertService {

  private Environment environment;
  private final KafkaService kafkaService;

  @Autowired
  public AlertServiceImpl(final KafkaService kafkaService,
                          final Environment environment) {
    this.kafkaService = kafkaService;
    this.environment = environment;
  }

  @Override
  public void escalateAlerts(List<Map<String, Object>> alerts) throws RestException {
    try {
      for (Map<String, Object> alert : alerts) {
        kafkaService.produceMessage(
            environment.getProperty(MetronRestConstants.KAFKA_TOPICS_ESCALATION_PROPERTY),
            JSONUtils.INSTANCE.toJSON(alert, false));
      }
    } catch (JsonProcessingException e) {
      throw new RestException(e);
    }
  }
}
