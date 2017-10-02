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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.model.AlertProfile;
import org.apache.metron.rest.repository.AlertProfileRepository;
import org.apache.metron.rest.service.AlertService;
import org.apache.metron.rest.service.KafkaService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@SuppressWarnings("unchecked")
public class AlertServiceImplTest {

  private KafkaService kafkaService;
  private Environment environment;
  private AlertProfileRepository alertProfileRepository;
  private AlertService alertService;
  private String testUser = "user1";

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    kafkaService = mock(KafkaService.class);
    environment = mock(Environment.class);
    alertProfileRepository = Mockito.mock(AlertProfileRepository.class);
    alertService = new AlertServiceImpl(kafkaService, environment, alertProfileRepository);

    Authentication authentication = Mockito.mock(Authentication.class);
    UserDetails userDetails = Mockito.mock(UserDetails.class);
    when(authentication.getPrincipal()).thenReturn(userDetails);
    when(userDetails.getUsername()).thenReturn(testUser);
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

    alertService.escalateAlerts(messages);

    String expectedMessage1 = "{\"field\":\"value1\"}";
    String expectedMessage2 = "{\"field\":\"value2\"}";
    verify(kafkaService).produceMessage("escalation", expectedMessage1);
    verify(kafkaService).produceMessage("escalation", expectedMessage2);
    verifyZeroInteractions(kafkaService);
  }

  @Test
  public void getShouldProperlyReturnActiveProfile() throws Exception {
    AlertProfile alertsProfile = new AlertProfile();
    alertsProfile.setId(testUser);
    when(alertProfileRepository.findOne(testUser)).thenReturn(alertsProfile);

    AlertProfile expectedAlertsProfile = new AlertProfile();
    expectedAlertsProfile.setId(testUser);
    assertEquals(expectedAlertsProfile, alertService.getProfile());
    verify(alertProfileRepository, times(1)).findOne(testUser);
    verifyNoMoreInteractions(alertProfileRepository);
  }

  @Test
  public void findAllShouldProperlyReturnActiveProfiles() throws Exception {
    AlertProfile alertsProfile1 = new AlertProfile();
    alertsProfile1.setId(testUser);
    AlertProfile alertsProfile2 = new AlertProfile();
    alertsProfile2.setId(testUser);
    when(alertProfileRepository.findAll())
        .thenReturn(Arrays.asList(alertsProfile1, alertsProfile2));

    AlertProfile expectedAlertsProfile1 = new AlertProfile();
    expectedAlertsProfile1.setId(testUser);
    AlertProfile expectedAlertsProfile2 = new AlertProfile();
    expectedAlertsProfile2.setId(testUser);
    Iterator<AlertProfile> actualAlertsProfiles = alertService.findAllProfiles().iterator();
    assertEquals(expectedAlertsProfile1, actualAlertsProfiles.next());
    assertEquals(expectedAlertsProfile2, actualAlertsProfiles.next());
    assertFalse(actualAlertsProfiles.hasNext());
    verify(alertProfileRepository, times(1)).findAll();
    verifyNoMoreInteractions(alertProfileRepository);
  }

  @Test
  public void saveShouldProperlySaveActiveProfile() throws Exception {
    AlertProfile savedAlertsProfile = new AlertProfile();
    savedAlertsProfile.setId(testUser);
    when(alertProfileRepository.save(savedAlertsProfile)).thenReturn(savedAlertsProfile);

    AlertProfile expectedAlertsProfile = new AlertProfile();
    expectedAlertsProfile.setId(testUser);
    AlertProfile alertsProfile = new AlertProfile();
    assertEquals(expectedAlertsProfile, alertService.saveProfile(alertsProfile));

    verify(alertProfileRepository, times(1)).save(savedAlertsProfile);
    verifyNoMoreInteractions(alertProfileRepository);
  }

  @Test
  public void deleteShouldProperlyDeleteActiveProfile() throws Exception {
    assertTrue(alertService.deleteProfile(testUser));

    doThrow(new EmptyResultDataAccessException(1)).when(alertProfileRepository).delete(testUser);
    assertFalse(alertService.deleteProfile(testUser));

    verify(alertProfileRepository, times(2)).delete(testUser);
    verifyNoMoreInteractions(alertProfileRepository);
  }
}
