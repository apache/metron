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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Iterator;
import org.apache.metron.rest.model.AlertsProfile;
import org.apache.metron.rest.repository.AlertsProfileRepository;
import org.apache.metron.rest.service.AlertsProfileService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class AlertsProfileServiceImplTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  private AlertsProfileRepository alertsProfileRepository;
  private AlertsProfileService alertsProfileService;
  private String testUser = "user1";

  @Before
  public void setUp() throws Exception {
    alertsProfileRepository = mock(AlertsProfileRepository.class);
    alertsProfileService = new AlertsProfileServiceImpl(alertsProfileRepository);

    Authentication authentication = mock(Authentication.class);
    UserDetails userDetails = mock(UserDetails.class);
    when(authentication.getPrincipal()).thenReturn(userDetails);
    when(userDetails.getUsername()).thenReturn(testUser);
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }


  @Test
  public void getShouldProperlyReturnActiveProfile() throws Exception {
    AlertsProfile alertsProfile = new AlertsProfile();
    alertsProfile.setId(testUser);
    when(alertsProfileRepository.findOne(testUser)).thenReturn(alertsProfile);

    AlertsProfile expectedAlertsProfile = new AlertsProfile();
    expectedAlertsProfile.setId(testUser);
    assertEquals(expectedAlertsProfile, alertsProfileService.get());
    verify(alertsProfileRepository, times(1)).findOne(testUser);
    verifyNoMoreInteractions(alertsProfileRepository);
  }

  @Test
  public void findAllShouldProperlyReturnActiveProfiles() throws Exception {
    AlertsProfile alertsProfile1 = new AlertsProfile();
    alertsProfile1.setId(testUser);
    AlertsProfile alertsProfile2 = new AlertsProfile();
    alertsProfile2.setId(testUser);
    when(alertsProfileRepository.findAll())
        .thenReturn(Arrays.asList(alertsProfile1, alertsProfile2));

    AlertsProfile expectedAlertsProfile1 = new AlertsProfile();
    expectedAlertsProfile1.setId(testUser);
    AlertsProfile expectedAlertsProfile2 = new AlertsProfile();
    expectedAlertsProfile2.setId(testUser);
    Iterator<AlertsProfile> actualAlertsProfiles = alertsProfileService.findAll().iterator();
    assertEquals(expectedAlertsProfile1, actualAlertsProfiles.next());
    assertEquals(expectedAlertsProfile2, actualAlertsProfiles.next());
    assertFalse(actualAlertsProfiles.hasNext());
    verify(alertsProfileRepository, times(1)).findAll();
    verifyNoMoreInteractions(alertsProfileRepository);
  }

  @Test
  public void saveShouldProperlySaveActiveProfile() throws Exception {
    AlertsProfile savedAlertsProfile = new AlertsProfile();
    savedAlertsProfile.setId(testUser);
    when(alertsProfileRepository.save(savedAlertsProfile)).thenReturn(savedAlertsProfile);

    AlertsProfile expectedAlertsProfile = new AlertsProfile();
    expectedAlertsProfile.setId(testUser);
    AlertsProfile alertsProfile = new AlertsProfile();
    assertEquals(expectedAlertsProfile, alertsProfileService.save(alertsProfile));

    verify(alertsProfileRepository, times(1)).save(savedAlertsProfile);
    verifyNoMoreInteractions(alertsProfileRepository);
  }

  @Test
  public void deleteShouldProperlyDeleteActiveProfile() throws Exception {
    assertTrue(alertsProfileService.delete(testUser));

    doThrow(new EmptyResultDataAccessException(1)).when(alertsProfileRepository).delete(testUser);
    assertFalse(alertsProfileService.delete(testUser));

    verify(alertsProfileRepository, times(2)).delete(testUser);
    verifyNoMoreInteractions(alertsProfileRepository);
  }


}
