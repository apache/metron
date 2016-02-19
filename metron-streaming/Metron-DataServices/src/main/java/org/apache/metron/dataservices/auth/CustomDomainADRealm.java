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
package org.apache.metron.dataservices.auth;

import javax.naming.NamingException;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.realm.activedirectory.ActiveDirectoryRealm;
import org.apache.shiro.realm.ldap.LdapContextFactory;

public class CustomDomainADRealm extends ActiveDirectoryRealm {

	private String customDomain;
	
	public void setCustomDomain(String customDomain) {
		this.customDomain = customDomain;
	}
	
	public String getCustomDomain() {
		return customDomain;
	}
	
	@Override
	protected AuthenticationInfo queryForAuthenticationInfo(
			AuthenticationToken token, LdapContextFactory ldapContextFactory)
			throws NamingException {
	
		UsernamePasswordToken upToken = (UsernamePasswordToken)token;
		String userName = upToken.getUsername();
		upToken.setUsername( userName + "@" + customDomain );
		
		return super.queryForAuthenticationInfo(token, ldapContextFactory);
	}
}
