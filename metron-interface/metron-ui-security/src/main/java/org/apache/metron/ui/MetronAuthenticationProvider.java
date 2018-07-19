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
package org.apache.metron.ui;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class MetronAuthenticationProvider implements AuthenticationProvider {

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (authentication != null) {
      authentication = getSSOAuthentication(authentication);
      if (authentication != null && authentication.isAuthenticated()) {
        return authentication;
      }
    }
    throw new MetronAuthenticationException("Authentication failed");
  }

  private Authentication getSSOAuthentication(Authentication authentication) {
    return authentication;
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return true;
  }

  public static List<GrantedAuthority> getAuthoritiesFromUGI(String userName) {
    // TODO - if we have ldap, we can lookup groups for this user
    
    // TODO - if we have a default mapper we can use that
    
    List<GrantedAuthority> grantedAuths = new ArrayList<GrantedAuthority>();
    grantedAuths.add(new SimpleGrantedAuthority("USER"));
    return grantedAuths;
  }
}
