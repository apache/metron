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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.system.FakeClock;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.model.AlertsUIUserSettings;
import org.apache.metron.rest.service.KafkaService;
import org.apache.metron.rest.user.UserSettingsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AlertsUIServiceImplTest {

  public static ThreadLocal<ObjectMapper> _mapper = ThreadLocal.withInitial(() ->
          new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL));

  /**
   * {
   *   "tableColumns": ["user1_field"]
   * }
   */
  @Multiline
  public static String user1AlertUserSettings;

  /**
   * {
   *   "tableColumns": ["user2_field"]
   * }
   */
  @Multiline
  public static String user2AlertUserSettings;

  private KafkaService kafkaService;
  private Environment environment;
  private UserSettingsClient userSettingsClient;
  private AlertsUIServiceImpl alertsUIService;
  private String user1 = "user1";
  private String user2 = "user2";
  private FakeClock clock;

  @BeforeEach
  public void setUp() {
    kafkaService = mock(KafkaService.class);
    environment = mock(Environment.class);
    userSettingsClient = mock(UserSettingsClient.class);
    alertsUIService = new AlertsUIServiceImpl(kafkaService, environment, userSettingsClient);

    // use a fake clock for testing
    clock = new FakeClock();
    clock.elapseSeconds(1000);
    alertsUIService.setClock(clock);

    // assume user1 is logged in for tests
    Authentication authentication = Mockito.mock(Authentication.class);
    UserDetails userDetails = Mockito.mock(UserDetails.class);
    when(authentication.getPrincipal()).thenReturn(userDetails);
    when(userDetails.getUsername()).thenReturn(user1);
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  @Test
  public void escalateAlertShouldSendMessageToKafka() throws Exception {
    final String field = "field";
    final String value1 = "value1";
    final String value2 = "value2";

    // define the escalation topic
    final String escalationTopic = "escalation";
    when(environment.getProperty(MetronRestConstants.KAFKA_TOPICS_ESCALATION_PROPERTY)).thenReturn(escalationTopic);

    // create an alert along with the expected escalation message that is sent to kafka
    final Map<String, Object> alert1 = mapOf(field, value1);
    String escalationMessage1 = escalationMessage(field, value1, user1, clock.currentTimeMillis());

    final Map<String, Object> alert2 = mapOf(field, value2);
    String escalationMessage2 = escalationMessage(field, value2, user1, clock.currentTimeMillis());

    // escalate the alerts and validate
    alertsUIService.escalateAlerts(Arrays.asList(alert1, alert2));
    verify(kafkaService).produceMessage(escalationTopic, escalationMessage1);
    verify(kafkaService).produceMessage(escalationTopic, escalationMessage2);
    verifyNoMoreInteractions(kafkaService);
  }

  @Test
  public void getShouldProperlyReturnActiveProfile() throws Exception {
    when(userSettingsClient.findOne(user1, AlertsUIServiceImpl.ALERT_USER_SETTING_TYPE)).thenReturn(Optional.of(user1AlertUserSettings));

    AlertsUIUserSettings expectedAlertsUIUserSettings = new AlertsUIUserSettings();
    expectedAlertsUIUserSettings.setTableColumns(Collections.singletonList("user1_field"));
    assertEquals(expectedAlertsUIUserSettings, alertsUIService.getAlertsUIUserSettings().get());
    verify(userSettingsClient, times(1)).findOne(user1, AlertsUIServiceImpl.ALERT_USER_SETTING_TYPE);
    verifyNoMoreInteractions(userSettingsClient);
  }

  @Test
  public void findAllShouldProperlyReturnActiveProfiles() throws Exception {
    AlertsUIUserSettings alertsProfile1 = new AlertsUIUserSettings();
    alertsProfile1.setUser(user1);
    AlertsUIUserSettings alertsProfile2 = new AlertsUIUserSettings();
    alertsProfile2.setUser(user1);
    when(userSettingsClient.findAll(AlertsUIServiceImpl.ALERT_USER_SETTING_TYPE))
            .thenReturn(new HashMap<String, Optional<String>>() {{
              put(user1,  Optional.of(user1AlertUserSettings));
              put(user2, Optional.of(user2AlertUserSettings));
              }});

    AlertsUIUserSettings expectedAlertsUIUserSettings1 = new AlertsUIUserSettings();
    expectedAlertsUIUserSettings1.setTableColumns(Collections.singletonList("user1_field"));
    AlertsUIUserSettings expectedAlertsUIUserSettings2 = new AlertsUIUserSettings();
    expectedAlertsUIUserSettings2.setTableColumns(Collections.singletonList("user2_field"));
    Map<String, AlertsUIUserSettings> actualAlertsProfiles = alertsUIService.findAllAlertsUIUserSettings();
    assertEquals(2, actualAlertsProfiles.size());
    assertEquals(expectedAlertsUIUserSettings1, actualAlertsProfiles.get(user1));
    assertEquals(expectedAlertsUIUserSettings2, actualAlertsProfiles.get(user2));

    verify(userSettingsClient, times(1)).findAll(AlertsUIServiceImpl.ALERT_USER_SETTING_TYPE);
    verifyNoMoreInteractions(userSettingsClient);
  }

  @Test
  public void saveShouldProperlySaveActiveProfile() throws Exception {
    AlertsUIUserSettings alertsUIUserSettings = new AlertsUIUserSettings();
    alertsUIUserSettings.setTableColumns(Collections.singletonList("user1_field"));

    alertsUIService.saveAlertsUIUserSettings(alertsUIUserSettings);

    String expectedAlertUserSettings = _mapper.get().writeValueAsString(alertsUIUserSettings);
    verify(userSettingsClient, times(1))
            .save(user1, AlertsUIServiceImpl.ALERT_USER_SETTING_TYPE, expectedAlertUserSettings);
    verifyNoMoreInteractions(userSettingsClient);
  }

  @Test
  public void deleteShouldProperlyDeleteActiveProfile() throws Exception {
    assertTrue(alertsUIService.deleteAlertsUIUserSettings(user1));

    doThrow(new IOException()).when(userSettingsClient).delete(user1, AlertsUIServiceImpl.ALERT_USER_SETTING_TYPE);
    assertFalse(alertsUIService.deleteAlertsUIUserSettings(user1));

    verify(userSettingsClient, times(2)).delete(user1, AlertsUIServiceImpl.ALERT_USER_SETTING_TYPE);
    verifyNoMoreInteractions(userSettingsClient);
  }

  /**
   * Defines what the message sent to Kafka should look-like when an alert is escalated.
   *
   * @param field The field name.
   * @param value The value of the field.
   * @param user The user who escalated the alert.
   * @param timestamp When the alert was escalated.
   * @return The escalated message.
   */
  private String escalationMessage(String field, String value, String user, Long timestamp) {
    return String.format("{\"%s\":\"%s\",\"%s\":\"%s\",\"%s\":%d}",
            field,
            value,
            MetronRestConstants.METRON_ESCALATION_USER_FIELD,
            user,
            MetronRestConstants.METRON_ESCALATION_TIMESTAMP_FIELD,
            timestamp);
  }

  private Map<String, Object> mapOf(String key, Object value) {
    Map<String, Object> map = new HashMap<>();
    map.put(key, value);
    return map;
  }
}
