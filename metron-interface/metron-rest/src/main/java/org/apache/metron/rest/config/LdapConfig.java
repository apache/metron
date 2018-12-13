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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

import static org.apache.metron.rest.MetronRestConstants.LDAP_PROFILE;
import static org.apache.metron.rest.MetronRestConstants.LDAP_PROVIDER_PASSWORD_SPRING_PROPERTY;
import static org.apache.metron.rest.MetronRestConstants.LDAP_PROVIDER_URL_SPRING_PROPERTY;
import static org.apache.metron.rest.MetronRestConstants.LDAP_PROVIDER_USERDN_SPRING_PROPERTY;

@Configuration
@Profile(LDAP_PROFILE)
public class LdapConfig {

  @Autowired
  private Environment environment;

  @Bean
  public LdapTemplate ldapTemplate() {
    LdapContextSource contextSource = new LdapContextSource();

    contextSource.setUrl(environment.getProperty(LDAP_PROVIDER_URL_SPRING_PROPERTY));
    contextSource.setUserDn(environment.getProperty(LDAP_PROVIDER_USERDN_SPRING_PROPERTY));
    contextSource.setPassword(environment.getProperty(LDAP_PROVIDER_PASSWORD_SPRING_PROPERTY));
    contextSource.afterPropertiesSet();

    return new LdapTemplate(contextSource);
  }

}
