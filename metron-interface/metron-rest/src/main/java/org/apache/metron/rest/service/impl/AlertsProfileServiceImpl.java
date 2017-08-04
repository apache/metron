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

import org.apache.metron.rest.model.AlertsProfile;
import org.apache.metron.rest.repository.AlertsProfileRepository;
import org.apache.metron.rest.security.SecurityUtils;
import org.apache.metron.rest.service.AlertsProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

@Service
public class AlertsProfileServiceImpl implements AlertsProfileService {

  @Autowired
  private AlertsProfileRepository alertsProfileRepository;

  @Override
  public AlertsProfile get() {
    return alertsProfileRepository.findOne(SecurityUtils.getCurrentUser());
  }

  @Override
  public Iterable<AlertsProfile> findAll() {
    return alertsProfileRepository.findAll();
  }

  @Override
  public AlertsProfile save(AlertsProfile alertsProfile) {
    String user = SecurityUtils.getCurrentUser();
    alertsProfile.setId(user);
    return alertsProfileRepository.save(alertsProfile);
  }

  @Override
  public boolean delete(String user) {
    boolean success = true;
    try {
      alertsProfileRepository.delete(user);
    } catch (EmptyResultDataAccessException e) {
      success = false;
    }
    return success;
  }
}
