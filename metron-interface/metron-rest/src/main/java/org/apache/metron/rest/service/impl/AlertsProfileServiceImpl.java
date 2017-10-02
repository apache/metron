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

import org.apache.metron.rest.model.AlertProfile;
import org.apache.metron.rest.repository.AlertProfileRepository;
import org.apache.metron.rest.security.SecurityUtils;
import org.apache.metron.rest.service.AlertsProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

@Service
public class AlertsProfileServiceImpl implements AlertsProfileService {

  private AlertProfileRepository alertsProfileRepository;

  @Autowired
  public AlertsProfileServiceImpl(AlertProfileRepository alertsProfileRepository) {
    this.alertsProfileRepository = alertsProfileRepository;
  }

  @Override
  public AlertProfile get() {
    return alertsProfileRepository.findOne(SecurityUtils.getCurrentUser());
  }

  @Override
  public Iterable<AlertProfile> findAll() {
    return alertsProfileRepository.findAll();
  }

  @Override
  public AlertProfile save(AlertProfile alertsProfile) {
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
