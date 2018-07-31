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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;

public class KnoxSSOAuthenticationFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(KnoxSSOAuthenticationFilter.class);

    private final String knoxUrl;
    private final RSAPublicKey publicKey;
    private final MetronAuthenticationProvider authenticationProvider;
    private String knoxCookie;
    private String knoxOriginalUrl;

    public KnoxSSOAuthenticationFilter(MetronAuthenticationProvider authenticationProvider, String knoxUrl,
            String knoxCookie, String knoxOriginalUrl, RSAPublicKey publicKey) {
        super();
        this.authenticationProvider = authenticationProvider;
        this.knoxUrl = knoxUrl;
        if (knoxCookie == null) {
            this.knoxCookie = "hadoop-jwt";
        } else {
            this.knoxCookie = knoxCookie;
        }
        if (knoxOriginalUrl == null) {
            this.knoxOriginalUrl = "originalUrl";
        } else {
            this.knoxOriginalUrl = knoxOriginalUrl;
        }
        this.publicKey = publicKey;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String authHeader = httpRequest.getHeader("Authorization");
        // if SSO is not enabled, skip this filter
        if (this.knoxUrl.isEmpty() || (authHeader != null && authHeader.startsWith("Basic"))) {
            chain.doFilter(request, response);
        } else {
            String serializedJWT = getJWTFromAuthorization(httpRequest);
            if (serializedJWT == null) {
                serializedJWT = getJWTFromCookie(httpRequest);
            }

            if (serializedJWT != null) {
                SignedJWT jwtToken = null;
                try {
                    jwtToken = SignedJWT.parse(serializedJWT);
                    boolean valid = validateToken(jwtToken);
                    // if the public key provide is correct and also token is not expired the
                    // process token
                    if (valid) {
                        String userName = jwtToken.getJWTClaimsSet().getSubject();
                        LOG.info("SSO login user : {} ", userName);
                        if (userName != null && !userName.trim().isEmpty()) {
                            List<GrantedAuthority> grantedAuths = MetronAuthenticationProvider
                                    .getAuthoritiesFromUGI(userName);
                            final UserDetails principal = new User(userName, "", grantedAuths);
                            final Authentication finalAuthentication = new UsernamePasswordAuthenticationToken(
                                    principal, "", grantedAuths);
                            WebAuthenticationDetails webDetails = new WebAuthenticationDetails(httpRequest);
                            ((AbstractAuthenticationToken) finalAuthentication).setDetails(webDetails);
                            Authentication authentication = authenticationProvider.authenticate(finalAuthentication);
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                        Date expirationTime = jwtToken.getJWTClaimsSet().getExpirationTime();
                        Date notBeforeTime = jwtToken.getJWTClaimsSet().getNotBeforeTime();
                        Date now = new Date();
                        if (expirationTime != null && now.after(expirationTime)) {
                          LOG.info("SSO token expired: {} ", userName);
                          redirectToKnox(httpRequest, httpResponse, chain);
                        } 
                        if (notBeforeTime != null && now.before(notBeforeTime)) {
                          LOG.info("SSO token not yet valid: {} ", userName);
                          redirectToKnox(httpRequest, httpResponse, chain);
                        }
                        chain.doFilter(request, response);
                    } else { // if the token is not valid then redirect to knox sso
                        redirectToKnox(httpRequest, httpResponse, chain);
                    }
                } catch (ParseException e) {
                    LOG.warn("Unable to parse the JWT token", e);
                    redirectToKnox(httpRequest, httpResponse, chain);
                }
            } else { // if there is no token, redirect
                redirectToKnox(httpRequest, httpResponse, chain);
            }
        }
    }

    private void redirectToKnox(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain chain)
            throws IOException, ServletException {
        // should probably check it's a browser
        String ssourl = constructLoginURL(httpRequest);
        httpResponse.sendRedirect(ssourl);
    }

    /**
     * Create the URL to be used for authentication of the user in the absence of a
     * JWT token within the incoming request.
     *
     * @param request
     *            for getting the original request URL
     * @return url to use as login url for redirect
     */
    protected String constructLoginURL(HttpServletRequest request) {
        String delimiter = "?";
        if (knoxUrl.contains("?")) {
            delimiter = "&";
        }
        String loginURL = knoxUrl + delimiter + knoxOriginalUrl + "=" + request.getRequestURL().toString()
                + getOriginalQueryString(request);
        return loginURL;
    }

    private String getOriginalQueryString(HttpServletRequest request) {
        String originalQueryString = request.getQueryString();
        return (originalQueryString == null) ? "" : "?" + originalQueryString;
    }

    /**
     * Verify the signature of the JWT token in this method. This method depends on
     * the public key that was established during init based upon the provisioned
     * public key. Override this method in subclasses in order to customize the
     * signature verification behavior.
     *
     * @param jwtToken
     *            the token that contains the signature to be validated
     * @return valid true if signature verifies successfully; false otherwise
     */
    protected boolean validateToken(SignedJWT jwtToken) {
        boolean valid = false;
        if (JWSObject.State.SIGNED == jwtToken.getState()) {
            LOG.debug("SSO token is in a SIGNED state");
            if (jwtToken.getSignature() != null) {
                LOG.debug("SSO token signature is not null");
                try {
                    JWSVerifier verifier = new RSASSAVerifier(publicKey);
                    if (jwtToken.verify(verifier)) {
                        valid = true;
                        LOG.debug("SSO token has been successfully verified");
                    } else {
                        LOG.warn("SSO signature verification failed.Please check the public key");
                    }
                } catch (JOSEException je) {
                    LOG.warn("Error while validating signature", je);
                } catch (Exception e) {
                    LOG.warn("Error while validating signature", e);
                }
            }
            // Now check that the signature algorithm was as expected
            if (valid) {
                String receivedSigAlg = jwtToken.getHeader().getAlgorithm().getName();
                if (!receivedSigAlg.equals("RS256")) {
                    valid = false;
                }
            }
        }
        return valid;
    }

    private String getJWTFromAuthorization(HttpServletRequest httpRequest) {
        String header = httpRequest.getHeader("Authorization");
        return (header != null && header.matches("Bearer (.*)")) ? header.substring(7) : null;
    }

    /**
     * Encapsulate the acquisition of the JWT token from HTTP cookies within the
     * request.
     *
     * Taken from
     *
     * @param req
     *            servlet request to get the JWT token from
     * @return serialized JWT token
     */
    protected String getJWTFromCookie(HttpServletRequest req) {
        String serializedJWT = null;
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                LOG.debug(String.format("Found cookie: %s [%s]", cookie.getName(), cookie.getValue()));
                if (knoxCookie.equals(cookie.getName())) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(knoxCookie + " cookie has been found and is being processed");
                    }
                    serializedJWT = cookie.getValue();
                    break;
                }
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug(knoxCookie + " not found");
            }
        }
        return serializedJWT;
    }

    public static RSAPublicKey parseRSAPublicKey(String pem)
            throws CertificateException, UnsupportedEncodingException, ServletException {
        String PEM_HEADER = "-----BEGIN CERTIFICATE-----\n";
        String PEM_FOOTER = "\n-----END CERTIFICATE-----";
        String fullPem = (pem.startsWith(PEM_HEADER) && pem.endsWith(PEM_FOOTER)) ? pem : PEM_HEADER + pem + PEM_FOOTER;
        PublicKey key = null;
        try {
            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream is = new ByteArrayInputStream(fullPem.getBytes("UTF8"));
            X509Certificate cer = (X509Certificate) fact.generateCertificate(is);
            key = cer.getPublicKey();
        } catch (CertificateException ce) {
            String message = null;
            if (pem.startsWith(PEM_HEADER)) {
                message = "CertificateException - be sure not to include PEM header "
                        + "and footer in the PEM configuration element.";
            } else {
                message = "CertificateException - PEM may be corrupt";
            }
            throw new ServletException(message, ce);
        } catch (UnsupportedEncodingException uee) {
            throw new ServletException(uee);
        }
        return (RSAPublicKey) key;
    }

}
