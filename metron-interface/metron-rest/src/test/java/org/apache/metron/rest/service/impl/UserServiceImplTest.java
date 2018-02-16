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

import org.apache.metron.rest.model.UserSettings;
import org.apache.metron.rest.repository.UserSettingsRepository;
import org.apache.metron.rest.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mock;

@SuppressWarnings("unchecked")
public class UserServiceImplTest {

  private UserSettingsRepository userSettingsRepository;
  private UserService userService;
  private String user1 = "user1";
  private String user2 = "user2";

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    userSettingsRepository = mock(UserSettingsRepository.class);
    userService = new UserServiceImpl(userSettingsRepository);

    Authentication authentication = Mockito.mock(Authentication.class);
    UserDetails userDetails = Mockito.mock(UserDetails.class);
    when(authentication.getPrincipal()).thenReturn(userDetails);
    when(userDetails.getUsername()).thenReturn(user1);
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  @Test
  public void getShouldProperlyReturnActiveProfile() throws Exception {
    UserSettings alertsProfile = new UserSettings();
    alertsProfile.setUser(user1);
    when(userSettingsRepository.findOne(user1)).thenReturn(alertsProfile);

    UserSettings expectedAlertsProfile = new UserSettings();
    expectedAlertsProfile.setUser(user1);
    assertEquals(expectedAlertsProfile, userService.getUserSettings());
    verify(userSettingsRepository, times(1)).findOne(user1);
    verifyNoMoreInteractions(userSettingsRepository);
  }

  @Test
  public void findAllShouldProperlyReturnActiveProfiles() throws Exception {
    UserSettings alertsProfile1 = new UserSettings();
    alertsProfile1.setUser(user1);
    UserSettings alertsProfile2 = new UserSettings();
    alertsProfile2.setUser(user1);
    when(userSettingsRepository.findAll())
        .thenReturn(new HashMap<String, UserSettings>() {{
          put(user1, alertsProfile1);
          put(user2, alertsProfile2);
        }});

    UserSettings expectedAlertsProfile1 = new UserSettings();
    expectedAlertsProfile1.setUser(user1);
    UserSettings expectedAlertsProfile2 = new UserSettings();
    expectedAlertsProfile2.setUser(user1);
    Map<String, UserSettings> actualAlertsProfiles = userService.findAllUserSettings();
    assertEquals(2, actualAlertsProfiles.size());
    assertEquals(expectedAlertsProfile1, actualAlertsProfiles.get(user1));
    assertEquals(expectedAlertsProfile2, actualAlertsProfiles.get(user2));

    verify(userSettingsRepository, times(1)).findAll();
    verifyNoMoreInteractions(userSettingsRepository);
  }

  @Test
  public void saveShouldProperlySaveActiveProfile() throws Exception {
    UserSettings savedAlertsProfile = new UserSettings();
    savedAlertsProfile.setUser(user1);
    when(userSettingsRepository.save(savedAlertsProfile)).thenReturn(savedAlertsProfile);

    UserSettings expectedAlertsProfile = new UserSettings();
    expectedAlertsProfile.setUser(user1);
    UserSettings alertsProfile = new UserSettings();
    assertEquals(expectedAlertsProfile, userService.saveUserSettings(alertsProfile));

    verify(userSettingsRepository, times(1)).save(savedAlertsProfile);
    verifyNoMoreInteractions(userSettingsRepository);
  }

  @Test
  public void deleteShouldProperlyDeleteActiveProfile() throws Exception {
    assertTrue(userService.deleteUserSettings(user1));

    doThrow(new IOException()).when(userSettingsRepository).delete(user1);
    assertFalse(userService.deleteUserSettings(user1));

    verify(userSettingsRepository, times(2)).delete(user1);
    verifyNoMoreInteractions(userSettingsRepository);
  }
}
