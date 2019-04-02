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

import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.apache.metron.rest.MetronRestConstants.SECURITY_ROLE_ADMIN;
import static org.apache.metron.rest.MetronRestConstants.SECURITY_ROLE_PREFIX;
import static org.apache.metron.rest.MetronRestConstants.SECURITY_ROLE_USER;

public class MetronAuthoritiesMapperTest {

  private MetronAuthoritiesMapper mapper;

  @Test
  public void shouldMapUserRole() {
    mapper = new MetronAuthoritiesMapper();
    mapper.setUserRole("ACME_USER");
    mapper.setAdminRole("ACME_ADMIN");
    mapper.setPrefix("ROLE_");

    // spring will prefix the roles retrieved from the authorization provider with "ROLE_" by default
    List<GrantedAuthority> input = new ArrayList<>();
    input.add(new SimpleGrantedAuthority("ROLE_" + "ACME_USER"));

    // should map "ACME_METRON_USER" to "ROLE_USER"
    Collection<? extends GrantedAuthority> actuals = mapper.mapAuthorities(input);
    Assert.assertEquals(1, actuals.size());
    Assert.assertEquals(SECURITY_ROLE_PREFIX + SECURITY_ROLE_USER, actuals.iterator().next().getAuthority());
  }

  @Test
  public void shouldMapAdminRole() {
    mapper = new MetronAuthoritiesMapper();
    mapper.setUserRole("ACME_USER");
    mapper.setAdminRole("ACME_ADMIN");
    mapper.setPrefix("ROLE_");

    List<GrantedAuthority> input = new ArrayList<>();
    input.add(new SimpleGrantedAuthority("ROLE_" + "ACME_ADMIN"));

    // should map "ACME_ADMIN" to "ROLE_ADMIN"
    Collection<? extends GrantedAuthority> actuals = mapper.mapAuthorities(input);
    Assert.assertEquals(1, actuals.size());
    Assert.assertEquals(SECURITY_ROLE_PREFIX + SECURITY_ROLE_ADMIN, actuals.iterator().next().getAuthority());
  }

  @Test
  public void shouldIgnoreOtherRoles() {
    mapper = new MetronAuthoritiesMapper();
    mapper.setUserRole("ACME_USER");
    mapper.setAdminRole("ACME_ADMIN");
    mapper.setPrefix("ROLE_");

    List<GrantedAuthority> input = new ArrayList<>();
    input.add(new SimpleGrantedAuthority("ROLE_" + "ANOTHER_ROLE"));
    input.add(new SimpleGrantedAuthority("ROLE_" + "YET_ANOTHER_ROLE"));

    Collection<? extends GrantedAuthority> actuals = mapper.mapAuthorities(input);
    Assert.assertEquals(0, actuals.size());
  }

  @Test
  public void shouldMapRolesWithNoPrefix() {
    // change the prefix
    mapper = new MetronAuthoritiesMapper();
    mapper.setUserRole("ACME_USER");
    mapper.setAdminRole("ACME_ADMIN");
    mapper.setPrefix("");

    List<GrantedAuthority> input = new ArrayList<>();
    input.add(new SimpleGrantedAuthority("ACME_ADMIN"));

    // should map "ACME_ADMIN" to "ROLE_ADMIN"
    Collection<? extends GrantedAuthority> actuals = mapper.mapAuthorities(input);
    Assert.assertEquals(1, actuals.size());
    Assert.assertEquals(SECURITY_ROLE_PREFIX + SECURITY_ROLE_ADMIN, actuals.iterator().next().getAuthority());
  }
}
