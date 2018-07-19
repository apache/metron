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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@SuppressWarnings("deprecation")
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class MetronSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(MetronSecurityConfig.class);

    @Value("${ldap.provider.url}")
    private String providerUrl;

    @Value("${ldap.provider.userdn}")
    private String providerUserDn;

    @Value("${ldap.provider.password}")
    private String providerPassword;

    @Value("${ldap.user.dn.patterns}")
    private String userDnPatterns;

    @Value("${ldap.user.passwordAttribute}")
    private String passwordAttribute;

    @Value("${ldap.user.searchBase}")
    private String userSearchBase;

    @Value("${ldap.user.searchFilter}")
    private String userSearchFilter;

    @Value("${ldap.group.searchBase}")
    private String groupSearchBase;

    @Value("${ldap.group.roleAttribute}")
    private String groupRoleAttribute;

    @Value("${ldap.group.searchFilter}")
    private String groupSearchFilter;

    @Value("${knox.sso.pubkeyFile:}")
    private Path knoxKeyFile;

    @Value("${knox.sso.pubkey:}")
    private String knoxKeyString;

    @Value("${knox.sso.url}")
    private String knoxUrl;

    @Value("${knox.sso.cookie:hadoop-jwt}")
    private String knoxCookie;

    @Value("${knox.sso.originalUrl:originalUrl}")
    private String knoxOriginalUrl;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        http
            .authorizeRequests().antMatchers(HttpMethod.OPTIONS,"/**").permitAll().and()
            .authorizeRequests().anyRequest().fullyAuthenticated()
            .and()
                .httpBasic()
            .and()
                .logout().disable();
        // @formatter:on

        // allow form based login if knox sso not in use
        if (knoxUrl == null || knoxUrl.isEmpty()) {
            http.formLogin();
        }

        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.NEVER).sessionFixation();
        if (this.knoxUrl != null && !this.knoxUrl.isEmpty()) {
            http.addFilterAt(ssoAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        }
        http.headers().disable();
        http.csrf();
    }

    private KnoxSSOAuthenticationFilter ssoAuthenticationFilter() throws Exception {
        String knoxKey;
        if ((this.knoxKeyString == null || this.knoxKeyString.isEmpty()) && this.knoxKeyFile != null) {
            List<String> keyLines = Files.readAllLines(knoxKeyFile, StandardCharsets.UTF_8);
            if (keyLines != null) {
                knoxKey = String.join("", keyLines);
            } else {
                knoxKey = "";
            }
        } else {
            knoxKey = this.knoxKeyString;
        }
        try {
            RSAPublicKey parseRSAPublicKey = KnoxSSOAuthenticationFilter.parseRSAPublicKey(knoxKey);
            return new KnoxSSOAuthenticationFilter(authenticationProvider(), knoxUrl, knoxCookie, knoxOriginalUrl,
                    parseRSAPublicKey);
        } catch (Exception e) {
            LOG.error("Cannot parse public key for KnoxSSO, please include the PEM string without certificate headers",
                    e);
            throw (e);
        }
    }

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        LOG.debug("Setting up LDAP authentication against %s", providerUrl);
        // @formatter:off
        if(this.providerUrl != null && !this.providerUrl.isEmpty()) {
            auth.ldapAuthentication()
                .userDnPatterns(userDnPatterns)
                .userSearchBase(userSearchBase)
                .userSearchFilter(userSearchFilter)
                .groupRoleAttribute(groupRoleAttribute)
                .groupSearchFilter(groupSearchFilter)
                .groupSearchBase(groupSearchBase)
                .contextSource()
                    .url(providerUrl)
                    .managerDn(providerUserDn)
                    .managerPassword(providerPassword)
                    .and()
                .passwordCompare()
                    .passwordEncoder(passwordEncoder())
                    .passwordAttribute(passwordAttribute);
        }
        // @formatter:on
        try {
          auth
              .authenticationProvider(authenticationProvider());
        } catch (Exception e){ 
          LOG.error("Cannot setup authentication", e);
        }
        auth.userDetailsService(userDetailsService());
    }

    @Bean
    public MetronAuthenticationProvider authenticationProvider() {
        return new MetronAuthenticationProvider();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // this currently uses plaintext passwords, which is not ideal
        // TODO replace with a delegating encoder which runs through the good algos, or
        // a config option based on the strength of passwords in the ldap store

        return NoOpPasswordEncoder.getInstance();
    }

}
