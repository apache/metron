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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.AlertsUIUserSettings;
import org.apache.metron.hbase.client.UserSettingsClient;
import org.apache.metron.rest.security.SecurityUtils;
import org.apache.metron.rest.service.AlertsUIService;
import org.apache.metron.rest.service.KafkaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * The default service layer implementation of {@link AlertsUIService}.
 *
 * @see AlertsUIService
 */
@Service
public class AlertsUIServiceImpl implements AlertsUIService {

  public static final String ALERT_USER_SETTING_TYPE = "metron-alerts-ui";
  public static ThreadLocal<ObjectMapper> _mapper = ThreadLocal.withInitial(() ->
          new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL));

  private Environment environment;
  private final KafkaService kafkaService;
  private UserSettingsClient userSettingsClient;

  @Autowired
  public AlertsUIServiceImpl(final KafkaService kafkaService,
                             final Environment environment,
                             final UserSettingsClient userSettingsClient) {
    this.kafkaService = kafkaService;
    this.environment = environment;
    this.userSettingsClient = userSettingsClient;
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

  @Override
  public Optional<AlertsUIUserSettings> getAlertsUIUserSettings() throws RestException {
    try {
      Optional<String> alertUserSettings = userSettingsClient.findOne(SecurityUtils.getCurrentUser(), ALERT_USER_SETTING_TYPE);
      if (alertUserSettings.isPresent()) {
        return Optional.of(_mapper.get().readValue(alertUserSettings.get(), AlertsUIUserSettings.class));
      } else {
        return Optional.empty();
      }
    } catch (IOException e) {
      throw new RestException(e);
    }
  }

  @Override
  public Map<String, AlertsUIUserSettings> findAllAlertsUIUserSettings() throws RestException {
    Map<String, AlertsUIUserSettings> allAlertUserSettings = new HashMap<>();
    try {
      Map<String, Optional<String>> alertUserSettingsStrings = userSettingsClient.findAll(ALERT_USER_SETTING_TYPE);
      for (Map.Entry<String, Optional<String>> entry: alertUserSettingsStrings.entrySet()) {
        Optional<String> alertUserSettings = entry.getValue();
        if (alertUserSettings.isPresent()) {
          allAlertUserSettings.put(entry.getKey(), _mapper.get().readValue(alertUserSettings.get(), AlertsUIUserSettings.class));
        }
      }
    } catch (IOException e) {
      throw new RestException(e);
    }
    return allAlertUserSettings;
  }

  @Override
  public void saveAlertsUIUserSettings(AlertsUIUserSettings alertsUIUserSettings) throws RestException{
    String user = SecurityUtils.getCurrentUser();
    try {
      userSettingsClient.save(user, ALERT_USER_SETTING_TYPE, _mapper.get().writeValueAsString(alertsUIUserSettings));
    } catch (IOException e) {
      throw new RestException(e);
    }
  }

  @Override
  public boolean deleteAlertsUIUserSettings(String user) {
    boolean success = true;
    try {
      userSettingsClient.delete(user, ALERT_USER_SETTING_TYPE);
    } catch (IOException e) {
      success = false;
    }
    return success;
  }
}
