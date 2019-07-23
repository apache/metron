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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.metron.common.system.FakeClock;
import org.apache.metron.hbase.client.FakeHBaseClientFactory;
import org.apache.metron.hbase.client.FakeHBaseConnectionFactory;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.model.AlertsUIUserSettings;
import org.apache.metron.rest.service.KafkaService;
import org.apache.metron.rest.user.HBaseUserSettingsClient;
import org.apache.metron.rest.user.UserSettingsClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.metron.rest.service.impl.AlertsUIServiceImpl.ALERT_USER_SETTING_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

/**
 * Tests the {@link AlertsUIServiceImpl} class.
 */
@SuppressWarnings("unchecked")
public class AlertsUIServiceImplTest {

  public static ThreadLocal<ObjectMapper> _mapper = ThreadLocal.withInitial(() ->
          new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL));

  private static final String field = "field";
  private static final String value1 = "value1";
  private static final String value2 = "value2";
  private static final String escalationTopic = "escalation";

  private KafkaService kafkaService;
  private Environment environment;
  private UserSettingsClient userSettingsClient;
  private AlertsUIServiceImpl alertsUIService;
  private String user1 = "user1";
  private String user2 = "user2";
  private FakeClock clock;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    kafkaService = mock(KafkaService.class);
    environment = mock(Environment.class);

    Map<String, Object> globals = new HashMap<String, Object>() {{
      put(HBaseUserSettingsClient.USER_SETTINGS_HBASE_TABLE, "some_table");
      put(HBaseUserSettingsClient.USER_SETTINGS_HBASE_CF, "column_family");
      put(HBaseUserSettingsClient.USER_SETTINGS_MAX_SCAN, "100000");
    }};

    userSettingsClient = new HBaseUserSettingsClient(
            () -> globals,
            new FakeHBaseClientFactory(),
            new FakeHBaseConnectionFactory(),
            HBaseConfiguration.create());
    userSettingsClient.init();
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
    // define the escalation topic
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
    verifyZeroInteractions(kafkaService);
  }

  @Test
  public void shouldGetActiveProfile() throws Exception {
    AlertsUIUserSettings expected = new AlertsUIUserSettings();
    expected.setTableColumns(Collections.singletonList("user1_field"));

    // save a profile for current user
    userSettingsClient.save(user1, ALERT_USER_SETTING_TYPE, toJSON(expected));

    // retrieve the active profile
    assertEquals(expected, alertsUIService.getAlertsUIUserSettings().get());
  }

  @Test
  public void shouldFindAllActiveProfiles() throws Exception {
    AlertsUIUserSettings settings1 = new AlertsUIUserSettings();
    settings1.setTableColumns(Collections.singletonList("user1_field"));

    AlertsUIUserSettings settings2 = new AlertsUIUserSettings();
    settings2.setTableColumns(Collections.singletonList("user2_field"));

    // save some profiles
    userSettingsClient.save(user1, ALERT_USER_SETTING_TYPE, toJSON(settings1));
    userSettingsClient.save(user2, ALERT_USER_SETTING_TYPE, toJSON(settings2));

    // retrieve all active profiles
    Map<String, AlertsUIUserSettings> actualAlertsProfiles = alertsUIService.findAllAlertsUIUserSettings();
    assertEquals(2, actualAlertsProfiles.size());
    assertEquals(settings1, actualAlertsProfiles.get(user1));
    assertEquals(settings2, actualAlertsProfiles.get(user2));
  }

  @Test
  public void shouldSaveActiveProfile() throws Exception {
    AlertsUIUserSettings expected = new AlertsUIUserSettings();
    expected.setTableColumns(Collections.singletonList("user1_field"));

    // save an active profile
    alertsUIService.saveAlertsUIUserSettings(expected);

    // get the active profile
    Optional<AlertsUIUserSettings> actual = alertsUIService.getAlertsUIUserSettings();
    assertEquals(expected, actual.get());
  }


  @Test
  public void shouldDeleteActiveProfile() throws Exception {
    AlertsUIUserSettings expected = new AlertsUIUserSettings();
    expected.setTableColumns(Collections.singletonList("user1_field"));

    userSettingsClient.save(user1, ALERT_USER_SETTING_TYPE, toJSON(expected));
    assertTrue(alertsUIService.deleteAlertsUIUserSettings(user1));
  }

  @Test
  public void shouldNotDeleteMissingProfile() throws Exception {
    // no profile saved for 'user999'
    assertFalse(alertsUIService.deleteAlertsUIUserSettings("user999"));
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

  private String toJSON(AlertsUIUserSettings settings) throws JsonProcessingException {
    return _mapper.get().writeValueAsString(settings);
  }
}
