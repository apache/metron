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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.io.FileUtils;
import org.apache.metron.rest.security.SecurityUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.FilterChain;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({KnoxSSOAuthenticationFilter.class, SignedJWT.class, SecurityContextHolder.class, SecurityUtils.class, RSASSAVerifier.class})
public class KnoxSSOAuthenticationFilterTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void shouldThrowExceptionOnMissingLdapTemplate() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("KnoxSSO requires LDAP. You must add 'ldap' to the active profiles.");

    new KnoxSSOAuthenticationFilter("userSearchBase",
            mock(Path.class),
            "knoxKeyString",
            "knoxCookie",
            null
            );
  }

  @Test
  public void doFilterShouldProperlySetAuthentication() throws Exception {
    KnoxSSOAuthenticationFilter knoxSSOAuthenticationFilter = spy(new KnoxSSOAuthenticationFilter("userSearchBase",
            mock(Path.class),
            "knoxKeyString",
            "knoxCookie",
            mock(LdapTemplate.class)
    ));
    HttpServletRequest request = mock(HttpServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    SignedJWT signedJWT = mock(SignedJWT.class);
    mockStatic(SignedJWT.class);
    JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder().subject("userName").build();
    Authentication authentication = mock(Authentication.class);
    SecurityContext securityContext = mock(SecurityContext.class);
    mockStatic(SecurityContextHolder.class);

    when(request.getHeader("Authorization")).thenReturn(null);
    doReturn("serializedJWT").when(knoxSSOAuthenticationFilter).getJWTFromCookie(request);
    when(SignedJWT.parse("serializedJWT")).thenReturn(signedJWT);
    when(signedJWT.getJWTClaimsSet()).thenReturn(jwtClaimsSet);
    doReturn(true).when(knoxSSOAuthenticationFilter).isValid(signedJWT, "userName");
    doReturn(authentication).when(knoxSSOAuthenticationFilter).getAuthentication("userName", request);
    when(SecurityContextHolder.getContext()).thenReturn(securityContext);

    knoxSSOAuthenticationFilter.doFilter(request, response, chain);

    verify(securityContext).setAuthentication(authentication);
    verify(chain).doFilter(request, response);
    verifyNoMoreInteractions(chain, securityContext);
  }

  @Test
  public void doFilterShouldContinueOnBasicAuthenticationHeader() throws Exception {
    KnoxSSOAuthenticationFilter knoxSSOAuthenticationFilter = spy(new KnoxSSOAuthenticationFilter("userSearchBase",
            mock(Path.class),
            "knoxKeyString",
            "knoxCookie",
            mock(LdapTemplate.class)
    ));
    HttpServletRequest request = mock(HttpServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getHeader("Authorization")).thenReturn("Basic ");

    knoxSSOAuthenticationFilter.doFilter(request, response, chain);

    verify(knoxSSOAuthenticationFilter, times(0)).getJWTFromCookie(request);
    verify(chain).doFilter(request, response);
    verifyNoMoreInteractions(chain);
  }

  @Test
  public void doFilterShouldContinueOnParseException() throws Exception {
    KnoxSSOAuthenticationFilter knoxSSOAuthenticationFilter = spy(new KnoxSSOAuthenticationFilter("userSearchBase",
            mock(Path.class),
            "knoxKeyString",
            "knoxCookie",
            mock(LdapTemplate.class)
    ));
    HttpServletRequest request = mock(HttpServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    SignedJWT signedJWT = mock(SignedJWT.class);
    mockStatic(SignedJWT.class);

    when(request.getHeader("Authorization")).thenReturn(null);
    doReturn("serializedJWT").when(knoxSSOAuthenticationFilter).getJWTFromCookie(request);
    when(SignedJWT.parse("serializedJWT")).thenThrow(new ParseException("parse exception", 0));

    knoxSSOAuthenticationFilter.doFilter(request, response, chain);

    verify(knoxSSOAuthenticationFilter, times(0)).getAuthentication("userName", request);
    verify(chain).doFilter(request, response);
    verifyNoMoreInteractions(chain);
  }

  @Test
  public void doFilterShouldContinueOnInvalidToken() throws Exception {
    KnoxSSOAuthenticationFilter knoxSSOAuthenticationFilter = spy(new KnoxSSOAuthenticationFilter("userSearchBase",
            mock(Path.class),
            "knoxKeyString",
            "knoxCookie",
            mock(LdapTemplate.class)
    ));
    HttpServletRequest request = mock(HttpServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    SignedJWT signedJWT = mock(SignedJWT.class);
    mockStatic(SignedJWT.class);
    JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder().subject("userName").build();

    when(request.getHeader("Authorization")).thenReturn(null);
    doReturn("serializedJWT").when(knoxSSOAuthenticationFilter).getJWTFromCookie(request);
    when(SignedJWT.parse("serializedJWT")).thenReturn(signedJWT);
    when(signedJWT.getJWTClaimsSet()).thenReturn(jwtClaimsSet);
    doReturn(false).when(knoxSSOAuthenticationFilter).isValid(signedJWT, "userName");

    knoxSSOAuthenticationFilter.doFilter(request, response, chain);

    verify(knoxSSOAuthenticationFilter, times(0)).getAuthentication("userName", request);
    verify(chain).doFilter(request, response);
    verifyNoMoreInteractions(chain);
  }

  @Test
  public void isValidShouldProperlyValidateToken() throws Exception {
    KnoxSSOAuthenticationFilter knoxSSOAuthenticationFilter = spy(new KnoxSSOAuthenticationFilter("userSearchBase",
            mock(Path.class),
            "knoxKeyString",
            "knoxCookie",
            mock(LdapTemplate.class)
    ));

    SignedJWT jwtToken = mock(SignedJWT.class);

    {
      // Should be invalid on emtpy user name
      assertFalse(knoxSSOAuthenticationFilter.isValid(jwtToken, null));
    }

    {
      // Should be invalid on expired token
      Date expiredDate = new Date(System.currentTimeMillis() - 10000);
      JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder().expirationTime(expiredDate).build();
      when(jwtToken.getJWTClaimsSet()).thenReturn(jwtClaimsSet);

      assertFalse(knoxSSOAuthenticationFilter.isValid(jwtToken, "userName"));
    }

    {
      // Should be invalid when date is before notBeforeTime
      Date notBeforeDate = new Date(System.currentTimeMillis() + 10000);
      JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder().notBeforeTime(notBeforeDate).build();
      when(jwtToken.getJWTClaimsSet()).thenReturn(jwtClaimsSet);

      assertFalse(knoxSSOAuthenticationFilter.isValid(jwtToken, "userName"));
    }

    {
      // Should be valid if user name is present and token is within time constraints
      Date expiredDate = new Date(System.currentTimeMillis() + 10000);
      Date notBeforeDate = new Date(System.currentTimeMillis() - 10000);
      JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder().expirationTime(expiredDate).notBeforeTime(notBeforeDate).build();
      when(jwtToken.getJWTClaimsSet()).thenReturn(jwtClaimsSet);
      doReturn(true).when(knoxSSOAuthenticationFilter).validateSignature(jwtToken);

      assertTrue(knoxSSOAuthenticationFilter.isValid(jwtToken, "userName"));
    }

  }

  @Test
  public void validateSignatureShouldProperlyValidateToken() throws Exception {
    KnoxSSOAuthenticationFilter knoxSSOAuthenticationFilter = spy(new KnoxSSOAuthenticationFilter("userSearchBase",
            mock(Path.class),
            "knoxKeyString",
            "knoxCookie",
            mock(LdapTemplate.class)
    ));

    SignedJWT jwtToken = mock(SignedJWT.class);

    {
      // Should be invalid if algorithm is not ES256
      JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.ES384);
      when(jwtToken.getHeader()).thenReturn(jwsHeader);

      assertFalse(knoxSSOAuthenticationFilter.validateSignature(jwtToken));
    }
    {
      // Should be invalid if state is not SIGNED
      JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.RS256);
      when(jwtToken.getHeader()).thenReturn(jwsHeader);
      when(jwtToken.getState()).thenReturn(JWSObject.State.UNSIGNED);

      assertFalse(knoxSSOAuthenticationFilter.validateSignature(jwtToken));
    }
    {
      // Should be invalid if signature is null
      JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.RS256);
      when(jwtToken.getHeader()).thenReturn(jwsHeader);
      when(jwtToken.getState()).thenReturn(JWSObject.State.SIGNED);

      assertFalse(knoxSSOAuthenticationFilter.validateSignature(jwtToken));
    }
    {
      Base64URL signature = mock(Base64URL.class);
      when(jwtToken.getSignature()).thenReturn(signature);
      RSAPublicKey rsaPublicKey = mock(RSAPublicKey.class);
      RSASSAVerifier rsaSSAVerifier = mock(RSASSAVerifier.class);
      mockStatic(SecurityUtils.class);
      when(SecurityUtils.parseRSAPublicKey("knoxKeyString")).thenReturn(rsaPublicKey);
      whenNew(RSASSAVerifier.class).withArguments(rsaPublicKey).thenReturn(rsaSSAVerifier);
      {
        // Should be invalid if token verify throws an exception
        when(jwtToken.verify(rsaSSAVerifier)).thenThrow(new JOSEException("verify exception"));

        assertFalse(knoxSSOAuthenticationFilter.validateSignature(jwtToken));
      }
      {
        // Should be invalid if RSA verification fails
        doReturn(false).when(jwtToken).verify(rsaSSAVerifier);

        assertFalse(knoxSSOAuthenticationFilter.validateSignature(jwtToken));
      }
      {
        // Should be valid if RSA verification succeeds
        doReturn(true).when(jwtToken).verify(rsaSSAVerifier);

        assertTrue(knoxSSOAuthenticationFilter.validateSignature(jwtToken));
      }
    }
  }

  @Test
  public void getJWTFromCookieShouldProperlyReturnToken() {
    KnoxSSOAuthenticationFilter knoxSSOAuthenticationFilter = spy(new KnoxSSOAuthenticationFilter("userSearchBase",
            mock(Path.class),
            "knoxKeyString",
            "knoxCookie",
            mock(LdapTemplate.class)
    ));

    HttpServletRequest request = mock(HttpServletRequest.class);

    {
      // Should be null if cookies are empty
      assertNull(knoxSSOAuthenticationFilter.getJWTFromCookie(request));
    }
    {
      // Should be null if Knox cookie is missing
      Cookie cookie = new Cookie("someCookie", "someValue");
      when(request.getCookies()).thenReturn(new Cookie[]{cookie});

      assertNull(knoxSSOAuthenticationFilter.getJWTFromCookie(request));
    }
    {
      // Should return token from knoxCookie
      Cookie cookie = new Cookie("knoxCookie", "token");
      when(request.getCookies()).thenReturn(new Cookie[]{cookie});

      assertEquals("token", knoxSSOAuthenticationFilter.getJWTFromCookie(request));
    }

  }

  @Test
  public void getKnoxKeyShouldProperlyReturnKnoxKey() throws Exception {
    {
      KnoxSSOAuthenticationFilter knoxSSOAuthenticationFilter = spy(new KnoxSSOAuthenticationFilter("userSearchBase",
              mock(Path.class),
              "knoxKeyString",
              "knoxCookie",
              mock(LdapTemplate.class)
      ));

      assertEquals("knoxKeyString", knoxSSOAuthenticationFilter.getKnoxKey());
    }
    {

      FileUtils.writeStringToFile(new File("./target/knoxKeyFile"), "knoxKeyFileKeyString", StandardCharsets.UTF_8);
      KnoxSSOAuthenticationFilter knoxSSOAuthenticationFilter = spy(new KnoxSSOAuthenticationFilter("userSearchBase",
              Paths.get("./target/knoxKeyFile"),
              null,
              "knoxCookie",
              mock(LdapTemplate.class)
      ));

      assertEquals("knoxKeyFileKeyString", knoxSSOAuthenticationFilter.getKnoxKey());
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void getAuthenticationShouldProperlyPopulateAuthentication() throws Exception {
    LdapTemplate ldapTemplate = mock(LdapTemplate.class);
    KnoxSSOAuthenticationFilter knoxSSOAuthenticationFilter = spy(new KnoxSSOAuthenticationFilter("ou=people,dc=hadoop,dc=apache,dc=org",
            mock(Path.class),
            "knoxKeyString",
            "knoxCookie",
            ldapTemplate
    ));

    HttpServletRequest request = mock(HttpServletRequest.class);

    when(ldapTemplate.search(any(LdapQuery.class), any(AttributesMapper.class))).thenReturn(Arrays.asList("USER", "ADMIN"));

    Authentication authentication = knoxSSOAuthenticationFilter.getAuthentication("userName", request);
    Object[] grantedAuthorities = authentication.getAuthorities().toArray();
    assertEquals("ROLE_USER", grantedAuthorities[0].toString());
    assertEquals("ROLE_ADMIN", grantedAuthorities[1].toString());
    assertEquals("userName", authentication.getName());
  }

}
