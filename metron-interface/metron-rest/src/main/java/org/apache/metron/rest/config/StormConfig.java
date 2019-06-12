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

import static org.apache.metron.rest.MetronRestConstants.DOCKER_PROFILE;
import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;

import java.util.Arrays;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.service.StormStatusService;
import org.apache.metron.rest.service.impl.CachedStormStatusServiceImpl;
import org.apache.metron.rest.service.impl.DockerStormCLIWrapper;
import org.apache.metron.rest.service.impl.StormCLIWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
@Profile("!" + TEST_PROFILE)
public class StormConfig {

  @Autowired
  private Environment environment;

  @Bean
  public StormCLIWrapper stormCLIClientWrapper() {
    if (Arrays.asList(environment.getActiveProfiles()).contains(DOCKER_PROFILE)) {
      return new DockerStormCLIWrapper(environment);
    } else {
      return new StormCLIWrapper();
    }
  }

  @Bean
  public StormStatusService stormStatusService(
      @Autowired @Qualifier("StormStatusServiceImpl") StormStatusService wrappedService) {
    long maxCacheSize = environment.getProperty(MetronRestConstants.STORM_STATUS_CACHE_MAX_SIZE, Long.class, 10000L);
    long maxCacheTimeoutSeconds = environment.getProperty(MetronRestConstants.STORM_STATUS_CACHE_TIMEOUT_SECONDS, Long.class, 5L);
    return new CachedStormStatusServiceImpl(wrappedService, maxCacheSize, maxCacheTimeoutSeconds);
  }
}
