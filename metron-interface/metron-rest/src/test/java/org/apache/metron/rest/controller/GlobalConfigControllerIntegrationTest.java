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
import org.apache.metron.rest.service.GlobalConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.apache.metron.integration.utils.TestUtils.assertEventually;
import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TEST_PROFILE)
public class GlobalConfigControllerIntegrationTest {

    /**
     {
     "solr.zookeeper": "solr:2181",
     "solr.collection": "metron",
     "solr.numShards": 1,
     "solr.replicationFactor": 1
     }
     */
    @Multiline
    public static String globalJson;

    @Autowired
    private GlobalConfigService globalConfigService;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    private String globalConfigUrl = "/api/v1/global/config";
    private String user = "user";
    private String password = "password";

    @BeforeEach
    public void setup() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
    }

    @Test
    public void testSecurity() throws Exception {
        this.mockMvc.perform(post(globalConfigUrl).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(globalJson))
                .andExpect(status().isUnauthorized());

        this.mockMvc.perform(get(globalConfigUrl))
                .andExpect(status().isUnauthorized());

        this.mockMvc.perform(delete(globalConfigUrl).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void test() throws Exception {
        this.globalConfigService.delete();

        assertEventually(() -> this.mockMvc.perform(get(globalConfigUrl).with(httpBasic(user,password)))
                .andExpect(status().isNotFound())
        );

        this.mockMvc.perform(post(globalConfigUrl).with(httpBasic(user,password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(globalJson))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")));

        assertEventually(() -> this.mockMvc.perform(post(globalConfigUrl).with(httpBasic(user,password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(globalJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
        );

        this.mockMvc.perform(get(globalConfigUrl).with(httpBasic(user,password)))
                .andExpect(status().isOk());

        this.mockMvc.perform(delete(globalConfigUrl).with(httpBasic(user,password)).with(csrf()))
                .andExpect(status().isOk());

        assertEventually(() -> this.mockMvc.perform(delete(globalConfigUrl).with(httpBasic(user,password)).with(csrf()))
                .andExpect(status().isNotFound())
        );

        this.globalConfigService.delete();
    }
}
