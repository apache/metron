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
import org.apache.metron.rest.service.SensorIndexingConfigService;
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

import static org.apache.metron.integration.utils.TestUtils.assertEventually;
import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TEST_PROFILE)
public class SensorIndexingConfigControllerIntegrationTest {

  /**
   {
   "index": "broTest",
   "batchSize": 1
   }
   */
  @Multiline
  public static String broJson;

  @Autowired
  private SensorIndexingConfigService sensorIndexingConfigService;

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  private String sensorIndexingConfigUrl = "/api/v1/sensor/indexing/config";
  private String user = "user";
  private String password = "password";

  @Before
  public void setup() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
  }

  @Test
  public void testSecurity() throws Exception {
    this.mockMvc.perform(post(sensorIndexingConfigUrl).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(broJson))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(sensorIndexingConfigUrl + "/broTest"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(sensorIndexingConfigUrl))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(delete(sensorIndexingConfigUrl + "/broTest").with(csrf()))
            .andExpect(status().isUnauthorized());
  }

  @Test
  public void test() throws Exception {
    sensorIndexingConfigService.delete("broTest");

    this.mockMvc.perform(get(sensorIndexingConfigUrl).with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(content().bytes("{}".getBytes()));

    this.mockMvc.perform(post(sensorIndexingConfigUrl + "/broTest").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(broJson))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.index").value("broTest"))
            .andExpect(jsonPath("$.batchSize").value(1));

    assertEventually(() -> this.mockMvc.perform(post(sensorIndexingConfigUrl + "/broTest").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(broJson))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.index").value("broTest"))
            .andExpect(jsonPath("$.batchSize").value(1))
    );

    this.mockMvc.perform(get(sensorIndexingConfigUrl + "/broTest").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.index").value("broTest"))
            .andExpect(jsonPath("$.batchSize").value(1));

    this.mockMvc.perform(get(sensorIndexingConfigUrl).with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$[?(@.broTest.index == 'broTest' &&" +
                    "@.broTest.batchSize == 1" +
                    ")]").exists());

    this.mockMvc.perform(delete(sensorIndexingConfigUrl + "/broTest").with(httpBasic(user,password)).with(csrf()))
            .andExpect(status().isOk());

    this.mockMvc.perform(get(sensorIndexingConfigUrl + "/broTest").with(httpBasic(user,password)))
            .andExpect(status().isNotFound());

    this.mockMvc.perform(delete(sensorIndexingConfigUrl + "/broTest").with(httpBasic(user,password)).with(csrf()))
            .andExpect(status().isNotFound());

    this.mockMvc.perform(get(sensorIndexingConfigUrl).with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$[?(@.sensorTopic == 'broTest')]").doesNotExist());

    sensorIndexingConfigService.delete("broTest");
  }
}

