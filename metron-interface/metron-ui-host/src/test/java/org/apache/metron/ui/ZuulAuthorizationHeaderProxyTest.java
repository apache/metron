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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.SecureRandom;

import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;

public class ZuulAuthorizationHeaderProxyTest {

    private static final String BASIC_AUTH_HEADER = "Basic dGVzdDp0ZXN0";

    private RequestContext context;

    private byte[] sharedKey = new byte[32];

    private String validToken;

    private boolean keyInited = false;

    @Before
    public void setTestRequestcontext() {
        context = new RequestContext();
        context.setResponse(new MockHttpServletResponse());
        context.setResponseGZipped(false);

        RequestContext.testSetCurrentContext(context);
    }

    @Test
    public void testThatZuulPassesCookiesToAuthorization() throws ZuulException, KeyLengthException, JOSEException {
        ZuulAuthenticationFilter zuulAuthenticationFilter = new ZuulAuthenticationFilter();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(validCookie());
        context.setRequest(request);
        zuulAuthenticationFilter.run();

        String header = context.getZuulRequestHeaders().get("Authorization");
        assertTrue("Authorization contains bearer", header.startsWith("Bearer "));
        assertTrue("Authorization contains cookie value", header.endsWith(validToken()));
    }

    @Test
    public void testDoesNotReplaceAuthorizationHeader() throws ZuulException, KeyLengthException, JOSEException {
        ZuulAuthenticationFilter zuulAuthenticationFilter = new ZuulAuthenticationFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(validCookie());
        request.addHeader("Authorization", BASIC_AUTH_HEADER);
        context.setRequest(request);
        assertFalse(zuulAuthenticationFilter.shouldFilter());
    }

    private Cookie validCookie() throws KeyLengthException, JOSEException {
        return new Cookie(ZuulAuthenticationFilter.COOKIE_NAME, validToken());
    }

    private String validToken() throws KeyLengthException, JOSEException {
        if (!this.keyInited ) {
            new SecureRandom().nextBytes(sharedKey);
            this.keyInited = true;
        }
        if (this.validToken == null) {
            JWSObject jwsObject = new JWSObject(new JWSHeader(JWSAlgorithm.HS256), new Payload("Test"));
            jwsObject.sign(new MACSigner(sharedKey));
            this.validToken = jwsObject.serialize();
        }
        return this.validToken;
    }
}
