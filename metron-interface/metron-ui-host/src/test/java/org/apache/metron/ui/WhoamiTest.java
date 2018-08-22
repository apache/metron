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

import static org.apache.metron.ui.EmbeddedLdap.EMBEDDED_LDAP_PROFILE;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", EMBEDDED_LDAP_PROFILE})
public class WhoamiTest {

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  private String username = "admin";
  private String password = "password";

  @Value("${knox.sso.pubkey}")
  private String publickey;

  @Value("${knox.sso.privatekey}")
  private String privatekey;

  @Value("${knox.sso.url}")
  private String knoxUrl;

  @Before
  public void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  public void testWhoamiNoAuth() throws Exception {
    mockMvc.perform(get("/whoami")).andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(knoxUrl + "?originalUrl=http://localhost/whoami"));
  }

  @Test
  public void testWhoamiBasicAuth() throws Exception {
    assertLoginCorrect(mockMvc.perform(get("/whoami").with(httpBasic(username, password))));
  }

  private ResultActions assertLoginCorrect(ResultActions actions) throws Exception {
    return actions.andExpect(status().isOk()).andExpect(content().string(containsString(username)));
  }

  @Test
  public void testWhoamiJwtAuth() throws Exception {
    String keyStr = String.join("", privatekey.split("\\s*|\\r|\\n"));
    byte[] dec = Base64.getDecoder().decode(keyStr);
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(dec);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    PrivateKey privKey = kf.generatePrivate(keySpec);

    JWSObject jwsObject = new JWSObject(new JWSHeader(JWSAlgorithm.RS256),
        new Payload("{ \"sub\": \"" + username + "\" }"));
    jwsObject.sign(new RSASSASigner(privKey));
    String token = jwsObject.serialize();

    assertLoginCorrect(mockMvc.perform(get("/whoami").cookie(new Cookie("hadoop-jwt", token))));
  }

  @Test
  public void testWhoamiRoles() throws Exception {
    mockMvc.perform(get("/whoami/roles").with(httpBasic(username, password))).andExpect(status().isOk())
        .andExpect(
            content().string("[\"ROLE_USER\",\"ROLE_ADMIN\"]"));
  }

}
