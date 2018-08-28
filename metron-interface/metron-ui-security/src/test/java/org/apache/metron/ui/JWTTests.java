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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;

import net.minidev.json.JSONObject;

public class JWTTests {
    private static final String COOKIE_NAME = "hadoop-jwt";
    private static final String knoxUrl = "https://localhost:8443/gateway/default/knoxsso";
    
    private static final Payload DEFAULT_PAYLOAD = new Payload("{ \"sub\": \"test\" }");

    @Test
    public void testValidJWT() throws Exception {
        KeyPair key = createKey();
        requestThatSucceeds(tokenWithKey((RSAPrivateKey) key.getPrivate(),DEFAULT_PAYLOAD), key);
    }

    @Test
    public void testInvalidJWT() throws Exception {
        KeyPair key = createKey();
        KeyPair badKey = createKey();
        assertFalse(key.equals(badKey));
        requestThatFails(tokenWithKey((RSAPrivateKey) badKey.getPrivate(),DEFAULT_PAYLOAD), key);
    }

    @Test()
    public void testExpiredJWT() throws Exception {
      Date date = new Date();
      KeyPair key = createKey();
      
      JSONObject json = new JSONObject();
      json.appendField("sub", "test");
      json.appendField("exp", (date.getTime() - 60000) / 1000);
      
      Payload payload = new Payload(json);
      JWSObject token = tokenWithKey((RSAPrivateKey) key.getPrivate(), payload);
      
      requestThatFails(token, key);
    }
    
    @Test()
    public void testNotYetJWT() throws Exception {
      Date date = new Date();
      KeyPair key = createKey();
      
      JSONObject json = new JSONObject();
      json.appendField("sub", "test");
      json.appendField("exp", (date.getTime() + 60000) / 1000);
      json.appendField("nbf", (date.getTime() + 30000) / 1000);
      
      Payload payload = new Payload(json);
      JWSObject token = tokenWithKey((RSAPrivateKey) key.getPrivate(), payload);
      
      requestThatFails(token, key);
    }

    @Test()
    public void testCorrectTimeWindowJWT() throws Exception {
      Date date = new Date();
      KeyPair key = createKey();
      
      JSONObject json = new JSONObject();
      json.appendField("sub", "test");
      json.appendField("exp", (date.getTime() + 60000) / 1000);
      json.appendField("nbf", (date.getTime() - 30000) / 1000);
      
      Payload payload = new Payload(json);
      JWSObject token = tokenWithKey((RSAPrivateKey) key.getPrivate(), payload);
      
      requestThatSucceeds(token, key);
    }

    private void requestThatSucceeds(JWSObject token, KeyPair key) throws IOException, ServletException {
      MockHttpServletRequest request = requestWithJWT(token);
      MockHttpServletResponse response = new MockHttpServletResponse();
      MockFilterChain chain = new MockFilterChain();

      MetronAuthenticationProvider authenticationProvider = new MetronAuthenticationProvider();
      KnoxSSOAuthenticationFilter knoxSSOAuthenticationFilter = new KnoxSSOAuthenticationFilter(
              authenticationProvider, knoxUrl, null, null, (RSAPublicKey) key.getPublic());

      knoxSSOAuthenticationFilter.doFilter(request, response, chain);

      // ensure that the filter has passed a successful authentication context
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      assertNotNull("Authentication object is set", authentication);
      assertEquals("test", ((User) authentication.getPrincipal()).getUsername());
    }

    private void requestThatFails(JWSObject token, KeyPair key) throws IOException, ServletException {
      MockHttpServletRequest request = requestWithJWT(token);
      MockHttpServletResponse response = new MockHttpServletResponse();
      MockFilterChain chain = new MockFilterChain();

      MetronAuthenticationProvider authenticationProvider = new MetronAuthenticationProvider();
      KnoxSSOAuthenticationFilter knoxSSOAuthenticationFilter = new KnoxSSOAuthenticationFilter(
              authenticationProvider, knoxUrl, null, null, (RSAPublicKey) key.getPublic());

      knoxSSOAuthenticationFilter.doFilter(request, response, chain);
      
      assertRedirectedToKnox(response);
    }

    private KeyPair createKey() throws Exception {
        return KeyPairGenerator.getInstance("RSA").generateKeyPair();
    }

    private JWSObject tokenWithKey(RSAPrivateKey key, Payload payload) throws JOSEException {
        JWSObject jwsObject = new JWSObject(new JWSHeader(JWSAlgorithm.RS256), payload);
        jwsObject.sign(new RSASSASigner(key));
        return jwsObject;
    }

    private MockHttpServletRequest requestWithJWT(JWSObject jwt) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(COOKIE_NAME, jwt.serialize()));
        return request;
    }

    private static void assertRedirectedToKnox(MockHttpServletResponse response) {
        assertTrue("Reponse is redirect to SSO", response.getHeader("Location").startsWith(knoxUrl));
    }

}
