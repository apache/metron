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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.apache.metron.rest.MetronRestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.LdapShaPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
@Controller
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(WebSecurityConfig.class);

    @Autowired
    private Environment environment;

    @Autowired
    private DataSource dataSource;

    @Autowired(required = false)
    private LdapTemplate ldapTemplate;

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

    @Value("${knox.sso.cookie:hadoop-jwt}")
    private String knoxCookie;


    @RequestMapping(value = {"/login", "/logout", "/sensors", "/sensors*/**"}, method = RequestMethod.GET)
    public String handleNGRequests() {
        return "forward:/index.html";
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/", "/home", "/login").permitAll()
                .antMatchers("/app/**").permitAll()
                .antMatchers("/vendor/**").permitAll()
                .antMatchers("/fonts/**").permitAll()
                .antMatchers("/assets/images/**").permitAll()
                .antMatchers("/*.js").permitAll()
                .antMatchers("/*.ttf").permitAll()
                .antMatchers("/*.woff2").permitAll()
                .anyRequest().authenticated()
                .and().httpBasic()
                .and()
                .logout()
                .logoutUrl("/api/v1/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", knoxCookie);

        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (activeProfiles.contains(MetronRestConstants.CSRF_ENABLE_PROFILE)) {
            http.csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
        } else {
            http.csrf().disable();
        }
        if (activeProfiles.contains(MetronRestConstants.KNOX_PROFILE)) {
          http.addFilterAt(new KnoxSSOAuthenticationFilter(userSearchBase, knoxKeyFile, knoxKeyString,
                  knoxCookie, ldapTemplate), UsernamePasswordAuthenticationFilter.class);
        }
    }

    @Autowired
    public void configureJdbc(AuthenticationManagerBuilder auth) throws Exception {
        // Note that we can switch profiles on the fly in Ambari.
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (activeProfiles.contains(MetronRestConstants.LDAP_PROFILE)) {
            LOG.debug("Setting up LDAP authentication against {}.", providerUrl);
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
                .passwordEncoder(new LdapShaPasswordEncoder())
                .passwordAttribute(passwordAttribute);
        } else if (activeProfiles.contains(MetronRestConstants.DEV_PROFILE) ||
            activeProfiles.contains(MetronRestConstants.TEST_PROFILE)) {
            auth.jdbcAuthentication()
                .dataSource(dataSource)
                .withUser("user").password("password").roles(SECURITY_ROLE_USER).and()
                .withUser("user1").password("password").roles(SECURITY_ROLE_USER).and()
                .withUser("user2").password("password").roles(SECURITY_ROLE_USER).and()
                .withUser("admin").password("password").roles(SECURITY_ROLE_USER, SECURITY_ROLE_ADMIN);
        } else {
            auth.jdbcAuthentication().dataSource(dataSource);
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}
