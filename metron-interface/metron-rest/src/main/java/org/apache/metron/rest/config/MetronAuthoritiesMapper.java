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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;

import static org.apache.metron.rest.MetronRestConstants.SECURITY_ROLE_ADMIN;
import static org.apache.metron.rest.MetronRestConstants.SECURITY_ROLE_PREFIX;
import static org.apache.metron.rest.MetronRestConstants.SECURITY_ROLE_USER;

/**
 * Maps the authorities used in Metron to the roles defined at the authentication provider.
 */
@Configuration
public class MetronAuthoritiesMapper implements GrantedAuthoritiesMapper {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * The name of the role at the authentication provider that maps to ROLE_USER.
   */
  @Value("${authorities.user:" + SECURITY_ROLE_USER + "}")
  private String userRole;

  /**
   * The name of the role at the authentication provider that maps to ROLE_ADMIN.
   */
  @Value("${authorities.admin:" + SECURITY_ROLE_ADMIN + "}")
  private String adminRole;

  /**
   * The prefix that is appended to each role after they are retrieved
   * from the authorization provider.
   *
   * <p>This prefix needs to be considered when mapping roles to authorities.
   */
  @Value("${authorities.prefix:" + SECURITY_ROLE_PREFIX + "}")
  private String prefix;

  @Override
  public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
    LOG.debug("Mapping roles to authorities; '{}'->'{}', '{}'->'{}'",
            prefix + userRole, SECURITY_ROLE_PREFIX + SECURITY_ROLE_USER,
            prefix + adminRole, SECURITY_ROLE_PREFIX + SECURITY_ROLE_ADMIN);

    HashSet<GrantedAuthority> mapped = new HashSet(authorities.size());
    Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
    while(iterator.hasNext()) {
      GrantedAuthority authority = iterator.next();
      mapAuthority(authority.getAuthority()).ifPresent(auth -> mapped.add(auth));
    }

    return mapped;
  }

  public Optional<GrantedAuthority> mapAuthority(final String authority) {
    Optional<GrantedAuthority> result;
    if(StringUtils.equals(authority, prefix + userRole)) {
      result = Optional.of(new SimpleGrantedAuthority(SECURITY_ROLE_PREFIX + SECURITY_ROLE_USER));
      LOG.debug("Mapped '{}' to '{}'", authority, result.get().getAuthority());

    } else if(StringUtils.equals(authority, prefix + adminRole)) {
      result = Optional.of(new SimpleGrantedAuthority(SECURITY_ROLE_PREFIX + SECURITY_ROLE_ADMIN));
      LOG.debug("Mapped '{}' to '{}'", authority, result.get().getAuthority());

    } else {
      // otherwise, we do not care about the role
      LOG.debug("Ignoring unused role; role={}", authority);
      result = Optional.empty();
    }

    return result;
  }

  public String getUserRole() {
    return userRole;
  }

  public void setUserRole(String userRole) {
    this.userRole = userRole;
  }

  public String getAdminRole() {
    return adminRole;
  }

  public void setAdminRole(String adminRole) {
    this.adminRole = adminRole;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }
}
