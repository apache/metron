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
package org.apache.metron.rest.controller;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;

import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TEST_PROFILE)
public class GrokControllerIntegrationTest {

    /**
     {
     "sampleData":"1467011157.401 415 127.0.0.1 TCP_MISS/200 337891 GET http://www.aliexpress.com/af/shoes.html? - DIRECT/207.109.73.154 text/html",
     "patternLabel":"SQUID",
     "statement":"SQUID %{NUMBER:timestamp} %{INT:elapsed} %{IPV4:ip_src_addr} %{WORD:action}/%{NUMBER:code} %{NUMBER:bytes} %{WORD:method} %{NOTSPACE:url} - %{WORD:UNWANTED}\/%{IPV4:ip_dst_addr} %{WORD:UNWANTED}\/%{WORD:UNWANTED}"
     }
     */
    @Multiline
    public static String grokValidationJson;

    /**
     {
     "sampleData":"1467011157.401 415 127.0.0.1 TCP_MISS/200 337891 GET http://www.aliexpress.com/af/shoes.html? - DIRECT/207.109.73.154 text/html",
     "statement":"SQUID %{NUMBER:timestamp} %{INT:elapsed} %{IPV4:ip_src_addr} %{WORD:action}/%{NUMBER:code} %{NUMBER:bytes} %{WORD:method} %{NOTSPACE:url} - %{WORD:UNWANTED}\/%{IPV4:ip_dst_addr} %{WORD:UNWANTED}\/%{WORD:UNWANTED}"
     }
     */
    @Multiline
    public static String missingPatternLabelGrokValidationJson;

    /**
     {
     "sampleData":"1467011157.401 415 127.0.0.1 TCP_MISS/200 337891 GET http://www.aliexpress.com/af/shoes.html? - DIRECT/207.109.73.154 text/html",
     "patternLabel":"SQUID"
     }
     */
    @Multiline
    public static String missingStatementGrokValidationJson;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    private String grokUrl = "/api/v1/grok";
    private String user = "user";
    private String password = "password";

    @Before
    public void setup() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
    }

    @Test
    public void testSecurity() throws Exception {
        this.mockMvc.perform(post(grokUrl + "/validate").with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(grokValidationJson))
                .andExpect(status().isUnauthorized());

        this.mockMvc.perform(get(grokUrl + "/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void test() throws Exception {
        this.mockMvc.perform(post(grokUrl + "/validate").with(httpBasic(user,password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(grokValidationJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
                .andExpect(jsonPath("$.results.action").value("TCP_MISS"))
                .andExpect(jsonPath("$.results.bytes").value(337891))
                .andExpect(jsonPath("$.results.code").value(200))
                .andExpect(jsonPath("$.results.elapsed").value(415))
                .andExpect(jsonPath("$.results.ip_dst_addr").value("207.109.73.154"))
                .andExpect(jsonPath("$.results.ip_src_addr").value("127.0.0.1"))
                .andExpect(jsonPath("$.results.method").value("GET"))
                .andExpect(jsonPath("$.results.timestamp").value("1467011157.401"))
                .andExpect(jsonPath("$.results.url").value("http://www.aliexpress.com/af/shoes.html?"));

        this.mockMvc.perform(post(grokUrl + "/validate").with(httpBasic(user,password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(missingPatternLabelGrokValidationJson))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
                .andExpect(jsonPath("$.responseCode").value(500))
                .andExpect(jsonPath("$.message").value("Pattern label is required"));

        this.mockMvc.perform(post(grokUrl + "/validate").with(httpBasic(user,password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(missingStatementGrokValidationJson))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
                .andExpect(jsonPath("$.responseCode").value(500))
                .andExpect(jsonPath("$.message").value("Grok statement is required"));

        this.mockMvc.perform(get(grokUrl + "/list").with(httpBasic(user,password)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
                .andExpect(jsonPath("$").isNotEmpty());

        String statement = FileUtils.readFileToString(new File("../../metron-platform/metron-parsing/metron-parsers/src/main/resources/patterns/squid"));
        this.mockMvc.perform(get(grokUrl + "/get/statement?path=/patterns/squid").with(httpBasic(user,password)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.parseMediaType("text/plain;charset=UTF-8")))
                .andExpect(content().bytes(statement.getBytes()));

        this.mockMvc.perform(get(grokUrl + "/get/statement?path=/bad/path").with(httpBasic(user,password)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.responseCode").value(500))
                .andExpect(jsonPath("$.message").value("Could not find a statement at path /bad/path"));
    }
}
