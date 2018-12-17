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

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.apache.metron.rest.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.metron.rest.MetronRestConstants.SECURITY_ROLE_PREFIX;
import static org.springframework.ldap.query.LdapQueryBuilder.query;

/**
 * This class is a Servlet Filter that authenticates a Knox SSO token.  The token is stored in a cookie and is
 * verified against a public Knox key.  The token expiration and begin time are also validated.  Upon successful validation,
 * a Spring Authentication object is built from the user name and user groups queried from LDAP.  Currently, user groups are
 * mapped directly to Spring roles and prepended with "ROLE_".
 */
public class KnoxSSOAuthenticationFilter implements Filter {
  private static final Logger LOG = LoggerFactory.getLogger(KnoxSSOAuthenticationFilter.class);

  private String userSearchBase;
  private Path knoxKeyFile;
  private String knoxKeyString;
  private String knoxCookie;
  private LdapTemplate ldapTemplate;

  public KnoxSSOAuthenticationFilter(String userSearchBase,
                                     Path knoxKeyFile,
                                     String knoxKeyString,
                                     String knoxCookie,
                                     LdapTemplate ldapTemplate) {
    this.userSearchBase = userSearchBase;
    this.knoxKeyFile = knoxKeyFile;
    this.knoxKeyString = knoxKeyString;
    this.knoxCookie = knoxCookie;
    if (ldapTemplate == null) {
      throw new IllegalStateException("KnoxSSO requires LDAP. You must add 'ldap' to the active profiles.");
    }
    this.ldapTemplate = ldapTemplate;
  }

  @Override
  public void init(FilterConfig filterConfig) {
  }

  @Override
  public void destroy() {
  }

  /**
   * Extracts the Knox token from the configured cookie.  If basic authentication headers are present, SSO authentication
   * is skipped.
   * @param request ServletRequest
   * @param response ServletResponse
   * @param chain FilterChain
   * @throws IOException
   * @throws ServletException
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
          throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;

    // If a basic authentication header is present, use that to authenticate and skip SSO
    String authHeader = httpRequest.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Basic")) {
      String serializedJWT = getJWTFromCookie(httpRequest);
      if (serializedJWT != null) {
        SignedJWT jwtToken;
        try {
          jwtToken = SignedJWT.parse(serializedJWT);
          String userName = jwtToken.getJWTClaimsSet().getSubject();
          LOG.info("SSO login user : {} ", userName);
          if (isValid(jwtToken, userName)) {
            Authentication authentication = getAuthentication(userName, httpRequest);
            SecurityContextHolder.getContext().setAuthentication(authentication);
          }
        } catch (ParseException e) {
          LOG.warn("Unable to parse the JWT token", e);
        }
      }
    }
    chain.doFilter(request, response);
  }

  /**
   * Validates a Knox token with expiration and begin times and verifies the token with a public Knox key.
   * @param jwtToken Knox token
   * @param userName User name associated with the token
   * @return Whether a token is valid or not
   * @throws ParseException JWT Token could not be parsed.
   */
  protected boolean isValid(SignedJWT jwtToken, String userName) throws ParseException {
    // Verify the user name is present
    if (userName == null || userName.isEmpty()) {
      LOG.info("Could not find user name in SSO token");
      return false;
    }

    Date now = new Date();

    // Verify the token has not expired
    Date expirationTime = jwtToken.getJWTClaimsSet().getExpirationTime();
    if (expirationTime != null && now.after(expirationTime)) {
      LOG.info("SSO token expired: {} ", userName);
      return false;
    }

    // Verify the token is not before time
    Date notBeforeTime = jwtToken.getJWTClaimsSet().getNotBeforeTime();
    if (notBeforeTime != null && now.before(notBeforeTime)) {
      LOG.info("SSO token not yet valid: {} ", userName);
      return false;
    }

    return validateSignature(jwtToken);
  }

  /**
   * Verify the signature of the JWT token in this method. This method depends on
   * the public key that was established during init based upon the provisioned
   * public key. Override this method in subclasses in order to customize the
   * signature verification behavior.
   *
   * @param jwtToken The token that contains the signature to be validated.
   * @return valid true if signature verifies successfully; false otherwise
   */
  protected boolean validateSignature(SignedJWT jwtToken) {
    // Verify the token signature algorithm was as expected
    String receivedSigAlg = jwtToken.getHeader().getAlgorithm().getName();

    if (!receivedSigAlg.equals(JWSAlgorithm.RS256.getName())) {
      return false;
    }

    // Verify the token has been properly signed
    if (JWSObject.State.SIGNED == jwtToken.getState()) {
      LOG.debug("SSO token is in a SIGNED state");
      if (jwtToken.getSignature() != null) {
        LOG.debug("SSO token signature is not null");
        try {
          JWSVerifier verifier = new RSASSAVerifier(SecurityUtils.parseRSAPublicKey(getKnoxKey()));
          if (jwtToken.verify(verifier)) {
            LOG.debug("SSO token has been successfully verified");
            return true;
          } else {
            LOG.warn("SSO signature verification failed. Please check the public key.");
          }
        } catch (Exception e) {
          LOG.warn("Error while validating signature", e);
        }
      }
    }
    return false;
  }

  /**
   * Encapsulate the acquisition of the JWT token from HTTP cookies within the
   * request.
   *
   * @param req ServletRequest to get the JWT token from
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

  /**
   * A public Knox key can either be passed in directly or read from a file.
   * @return Public Knox key
   * @throws IOException There was a problem reading the Knox key file.
   */
  protected String getKnoxKey() throws IOException {
    String knoxKey;
    if ((this.knoxKeyString == null || this.knoxKeyString.isEmpty()) && this.knoxKeyFile != null) {
      List<String> keyLines = Files.readAllLines(knoxKeyFile, StandardCharsets.UTF_8);
      knoxKey = String.join("", keyLines);
    } else {
      knoxKey = this.knoxKeyString;
    }
    return knoxKey;
  }

  /**
   * Builds the Spring Authentication object using the supplied user name and groups looked up from LDAP.  Groups are currently
   * mapped directly to Spring roles by converting to upper case and prepending the name with "ROLE_".
   * @param userName The username to build the Authentication object with.
   * @param httpRequest HttpServletRequest
   * @return Authentication object for the given user.
   */
  protected Authentication getAuthentication(String userName, HttpServletRequest httpRequest) {
    String ldapName = LdapNameBuilder.newInstance().add(userSearchBase).add("uid", userName).build().toString();

    // Search ldap for a user's groups and convert to a Spring role
    List<GrantedAuthority> grantedAuths = ldapTemplate.search(query()
            .where("objectclass")
            .is("groupOfNames")
            .and("member")
            .is(ldapName), (AttributesMapper<String>) attrs -> (String) attrs.get("cn").get())
            .stream()
            .map(group -> String.format("%s%s", SECURITY_ROLE_PREFIX, group.toUpperCase()))
            .map(SimpleGrantedAuthority::new).collect(Collectors.toList());

    final UserDetails principal = new User(userName, "", grantedAuths);
    final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            principal, "", grantedAuths);
    WebAuthenticationDetails webDetails = new WebAuthenticationDetails(httpRequest);
    authentication.setDetails(webDetails);
    return authentication;
  }

}
