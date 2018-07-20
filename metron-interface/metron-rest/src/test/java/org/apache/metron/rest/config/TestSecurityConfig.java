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

import static org.apache.metron.rest.MetronRestConstants.SECURITY_ROLE_ADMIN;
import static org.apache.metron.rest.MetronRestConstants.SECURITY_ROLE_USER;
import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.test.context.ActiveProfiles;

@Configuration
@ActiveProfiles(TEST_PROFILE)
@Order(99)
public class TestSecurityConfig extends WebSecurityConfigurerAdapter {
  @Override
  public void configure(AuthenticationManagerBuilder auth) throws Exception {
    // @formatter:off
    auth.inMemoryAuthentication()
        .withUser("user").password("password").roles(SECURITY_ROLE_USER).and()
        .withUser("user1").password("password").roles(SECURITY_ROLE_USER).and()
        .withUser("user2").password("password").roles(SECURITY_ROLE_USER).and()
        .withUser("admin").password("password").roles(SECURITY_ROLE_USER, SECURITY_ROLE_ADMIN);
    // @formatter:on
  }
}