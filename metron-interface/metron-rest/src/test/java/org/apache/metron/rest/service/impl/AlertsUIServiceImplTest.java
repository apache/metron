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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.model.AlertsUIUserSettings;
import org.apache.metron.hbase.client.UserSettingsClient;
import org.apache.metron.rest.service.AlertsUIService;
import org.apache.metron.rest.service.KafkaService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@SuppressWarnings("unchecked")
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
  private AlertsUIService alertsUIService;
  private String user1 = "user1";
  private String user2 = "user2";

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    kafkaService = mock(KafkaService.class);
    environment = mock(Environment.class);
    userSettingsClient = mock(UserSettingsClient.class);
    alertsUIService = new AlertsUIServiceImpl(kafkaService, environment, userSettingsClient);

    // assume user1 is logged in for tests
    Authentication authentication = Mockito.mock(Authentication.class);
    UserDetails userDetails = Mockito.mock(UserDetails.class);
    when(authentication.getPrincipal()).thenReturn(userDetails);
    when(userDetails.getUsername()).thenReturn(user1);
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  @Test
  public void produceMessageShouldProperlyProduceMessage() throws Exception {
    String escalationTopic = "escalation";
    final Map<String, Object> message1 = new HashMap<>();
    message1.put("field", "value1");
    final Map<String, Object> message2 = new HashMap<>();
    message2.put("field", "value2");
    List<Map<String, Object>> messages = Arrays.asList(message1, message2);
    when(environment.getProperty(MetronRestConstants.KAFKA_TOPICS_ESCALATION_PROPERTY)).thenReturn(escalationTopic);

    alertsUIService.escalateAlerts(messages);

    String expectedMessage1 = "{\"field\":\"value1\"}";
    String expectedMessage2 = "{\"field\":\"value2\"}";
    verify(kafkaService).produceMessage("escalation", expectedMessage1);
    verify(kafkaService).produceMessage("escalation", expectedMessage2);
    verifyZeroInteractions(kafkaService);
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
}
