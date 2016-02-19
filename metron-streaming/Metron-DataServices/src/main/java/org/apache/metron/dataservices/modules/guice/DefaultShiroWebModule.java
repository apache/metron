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
package org.apache.metron.dataservices.modules.guice;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.shiro.guice.web.ShiroWebModule;
import org.apache.shiro.web.filter.authc.LogoutFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Provides;
import com.google.inject.name.Names;
import org.apache.metron.dataservices.auth.CustomDomainADRealm;

public class DefaultShiroWebModule extends ShiroWebModule {
    
	private static final Logger logger = LoggerFactory.getLogger( DefaultShiroWebModule.class );
	
	private Properties configProps;
	
	public DefaultShiroWebModule(final ServletContext sc) {
        super(sc);
    }

    public DefaultShiroWebModule(final Properties configProps, final ServletContext sc) {
        super(sc);
        this.configProps = configProps;
    }    
    
    protected void configureShiroWeb() {
        bindConstant().annotatedWith(Names.named("shiro.loginUrl")).to( "/login.jsp" );
    	bindRealm().to(CustomDomainADRealm.class);
    	bind( LogoutFilter.class);
        
        addFilterChain("/login", ANON);
        addFilterChain("/logout", ANON);
        addFilterChain("/withsocket.jsp", AUTHC );
        addFilterChain("/withsocket2.jsp", ANON );
    }
    
    @Provides 
    @javax.inject.Singleton 
    CustomDomainADRealm providesRealm()
    {
    	
    	CustomDomainADRealm realm = new CustomDomainADRealm();
    	
    	String ldapUrl = configProps.getProperty("ldapUrl");
    	logger.info( "got ldapurl from config: " + ldapUrl );
    	realm.setUrl(ldapUrl);
    	
    	// String ldapAuthMechanism = configProps.getProperty( "ldapAuthMechanism", "simple" ).trim();
    	// logger.info( "got ldapAuthMechanism from config: " + ldapAuthMechanism );
    	
    	
    	String activeDirectorySystemUsername = configProps.getProperty( "activeDirectorySystemUsername" ).trim();
    	logger.info( "got activeDirectorySystemUsername from config: " + activeDirectorySystemUsername );
    	realm.setSystemUsername(activeDirectorySystemUsername);
    	
    	String activeDirectorySystemPassword = configProps.getProperty( "activeDirectorySystemPassword" ).trim();
    	logger.info( "got activeDirectorySystemPassword from config: " + activeDirectorySystemPassword );
    	realm.setSystemPassword(activeDirectorySystemPassword);

    	String adDomain = configProps.getProperty( "adDomain" ).trim();
    	realm.setCustomDomain( adDomain );
    	
    	String activeDirectoryBaseSearchDN = configProps.getProperty( "activeDirectoryBaseSearchDN" ).trim();
    	logger.info( "got activeDirectoryBaseSearchDN from config: " + activeDirectoryBaseSearchDN );
    	realm.setSearchBase( activeDirectoryBaseSearchDN );
    	
    	String groupRolesMapStr = configProps.getProperty( "groupRolesMap" );
    	logger.info( "got groupRolesMapStr from config: " + groupRolesMapStr );
    	
    	String[] mappings = groupRolesMapStr.split( "\\|" );
    	
    	Map<String,String> groupRolesMap = new HashMap<String, String>();
    	for( String mapping : mappings )
    	{
    		System.out.println( "mapping: " + mapping );
    		String[] mappingParts = mapping.split(":");
    		groupRolesMap.put( mappingParts[0], mappingParts[1]);
    	}
    	
    	realm.setGroupRolesMap(groupRolesMap);
    	return realm;
    }
}
