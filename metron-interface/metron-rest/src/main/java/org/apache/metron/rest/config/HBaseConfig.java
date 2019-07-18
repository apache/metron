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

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.metron.hbase.client.HBaseClient;
import org.apache.metron.hbase.client.HBaseClientFactory;
import org.apache.metron.hbase.client.HBaseConnectionFactory;
import org.apache.metron.hbase.client.HBaseTableClient;
import org.apache.metron.hbase.client.HBaseTableClientFactory;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.GlobalConfigService;
import org.apache.metron.rest.user.HBaseUserSettingsClient;
import org.apache.metron.rest.user.UserSettingsClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Map;
import java.util.function.Supplier;

import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;

@Configuration
@Profile("!" + TEST_PROFILE)
public class HBaseConfig {

  @Autowired
  private GlobalConfigService globalConfigService;

  private Supplier<Map<String, Object>> globals = () -> {
    try {
      return globalConfigService.get();
    } catch (RestException e) {
      throw new IllegalStateException("Unable to retrieve the global config.", e);
    }
  };

  @Autowired
  public HBaseConfig(GlobalConfigService globalConfigService) {
    this.globalConfigService = globalConfigService;
  }

  @Bean
  public HBaseConnectionFactory hBaseConnectionFactory() {
    return new HBaseConnectionFactory();
  }

  @Bean
  org.apache.hadoop.conf.Configuration hBaseConfiguration() {
    return HBaseConfiguration.create();
  }

  @Bean
  HBaseClientFactory hBaseClientFactory() {
    return new HBaseTableClientFactory();
  }

  @Bean(destroyMethod = "close")
  public UserSettingsClient userSettingsClient(GlobalConfigService globalConfigService,
                                               HBaseClientFactory hBaseClientFactory,
                                               HBaseConnectionFactory hBaseConnectionFactory,
                                               org.apache.hadoop.conf.Configuration hBaseConfiguration) {
    UserSettingsClient userSettingsClient = new HBaseUserSettingsClient(
            globals,
            hBaseClientFactory,
            hBaseConnectionFactory,
            hBaseConfiguration);
    userSettingsClient.init();
    return userSettingsClient;
  }
}
