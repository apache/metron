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
