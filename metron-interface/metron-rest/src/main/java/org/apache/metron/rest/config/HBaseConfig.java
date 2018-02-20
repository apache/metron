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
package org.apache.metron.rest.config;

import org.apache.metron.hbase.HTableProvider;
import org.apache.metron.hbase.client.UserSettingsClient;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.GlobalConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;

@Configuration
@Profile("!" + TEST_PROFILE)
public class HBaseConfig {

    @Autowired
    private GlobalConfigService globalConfigService;

    @Autowired
    public HBaseConfig(GlobalConfigService globalConfigService) {
        this.globalConfigService = globalConfigService;
    }

    @Bean()
    public UserSettingsClient userSettingsClient() {
      UserSettingsClient userSettingsClient = new UserSettingsClient();
      userSettingsClient.init(() -> {
        try {
          return globalConfigService.get();
        } catch (RestException e) {
          throw new IllegalStateException("Unable to retrieve the global config.", e);
        }
      }, new HTableProvider());
      return userSettingsClient;
    }
}
