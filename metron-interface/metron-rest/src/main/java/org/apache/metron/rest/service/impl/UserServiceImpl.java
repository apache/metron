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

import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.UserSettings;
import org.apache.metron.rest.repository.UserSettingsRepository;
import org.apache.metron.rest.security.SecurityUtils;
import org.apache.metron.rest.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

  private UserSettingsRepository userSettingsRepository;

  @Autowired
  public UserServiceImpl(final UserSettingsRepository userSettingsRepository) {
    this.userSettingsRepository = userSettingsRepository;
  }

  @Override
  public UserSettings getUserSettings() throws RestException {
    try {
      return userSettingsRepository.findOne(SecurityUtils.getCurrentUser());
    } catch (IOException e) {
      e.printStackTrace();
      throw new RestException(e);
    }
  }

  @Override
  public Map<String, UserSettings> findAllUserSettings() throws RestException {
    try {
      return userSettingsRepository.findAll();
    } catch (IOException e) {
      throw new RestException(e);
    }
  }

  @Override
  public UserSettings saveUserSettings(UserSettings userSettings) throws RestException{
    String user = SecurityUtils.getCurrentUser();
    userSettings.setUser(user);
    try {
      return userSettingsRepository.save(userSettings);
    } catch (IOException e) {
      throw new RestException(e);
    }
  }

  @Override
  public boolean deleteUserSettings(String user) {
    boolean success = true;
    try {
      userSettingsRepository.delete(user);
    } catch (IOException e) {
      success = false;
    }
    return success;
  }
}
