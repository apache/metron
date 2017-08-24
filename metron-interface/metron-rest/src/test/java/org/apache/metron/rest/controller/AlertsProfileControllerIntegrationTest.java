/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.  You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.metron.rest.controller;

import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.rest.model.AlertsProfile;
import org.apache.metron.rest.service.AlertsProfileService;
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

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TEST_PROFILE)
public class AlertsProfileControllerIntegrationTest {

  /**
   * {
   *   "tableColumns": ["user1_field"],
   *   "savedSearches": [
   *     {
   *       "name": "user1 search 1",
   *       "searchRequest": {
   *         "from": 0,
   *         "indices": ["bro"],
   *         "query": "*",
   *         "size": 5
   *       }
   *     },
   *     {
   *       "name": "user1 search 2",
   *       "searchRequest": {
   *         "from": 10,
   *         "indices": ["snort"],
   *         "query": "*",
   *         "size": 10
   *       }
   *     }
   *   ]
   * }
   */
  @Multiline
  public static String user1ProfileJson;


  /**
   * {
   *   "tableColumns": ["user2_field"],
   *   "savedSearches": [
   *     {
   *       "name": "user2 search 1",
   *       "searchRequest": {
   *         "from": 0,
   *         "indices": ["bro", "snort"],
   *         "query": "ip_src_addr:192.168.1.1",
   *         "size": 100
   *       }
   *     }
   *   ]
   * }
   */
  @Multiline
  public static String user2ProfileJson;

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private AlertsProfileService alertsProfileService;

  private MockMvc mockMvc;

  private String url = "/api/v1/alerts/profile";
  private String user1 = "user1";
  private String user2 = "user2";
  private String admin = "admin";
  private String password = "password";

  @Before
  public void setup() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
  }

  @Test
  public void testSecurity() throws Exception {
    this.mockMvc.perform(get(url))
        .andExpect(status().isUnauthorized());
    this.mockMvc.perform(get(url + "/all"))
        .andExpect(status().isUnauthorized());
    this.mockMvc.perform(post(url).with(csrf())
        .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
        .content(user1ProfileJson))
        .andExpect(status().isUnauthorized());
    this.mockMvc.perform(get(url + "/all").with(httpBasic(user1, password)).with(csrf()))
        .andExpect(status().isForbidden());
    this.mockMvc.perform(delete(url + "/user1").with(httpBasic(user1, password)).with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  public void test() throws Exception {
    for (AlertsProfile alertsProfile : alertsProfileService.findAll()) {
      alertsProfileService.delete(alertsProfile.getId());
    }

    this.mockMvc.perform(get(url).with(httpBasic(user1, password)))
        .andExpect(status().isNotFound());

    this.mockMvc.perform(get(url).with(httpBasic(user2, password)))
        .andExpect(status().isNotFound());

    this.mockMvc.perform(get(url + "/all").with(httpBasic(admin, password)))
        .andExpect(status().isOk())
        .andExpect(
            content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
        .andExpect(jsonPath("$.*", hasSize(0)));

    this.mockMvc.perform(post(url).with(httpBasic(user1, password)).with(csrf())
        .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
        .content(user1ProfileJson))
        .andExpect(status().isCreated())
        .andExpect(content().json(user1ProfileJson));

    this.mockMvc.perform(post(url).with(httpBasic(user1, password)).with(csrf())
        .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
        .content(user1ProfileJson))
        .andExpect(status().isOk())
        .andExpect(content().json(user1ProfileJson));

    this.mockMvc.perform(get(url).with(httpBasic(user1, password)))
        .andExpect(status().isOk())
        .andExpect(
            content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
        .andExpect(content().json(user1ProfileJson));

    this.mockMvc.perform(get(url).with(httpBasic(user2, password)))
        .andExpect(status().isNotFound());

    this.mockMvc.perform(get(url + "/all").with(httpBasic(admin, password)))
        .andExpect(status().isOk())
        .andExpect(
            content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
        .andExpect(content().json("[" + user1ProfileJson + "]"));

    this.mockMvc.perform(post(url).with(httpBasic(user2, password)).with(csrf())
        .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
        .content(user2ProfileJson))
        .andExpect(status().isCreated())
        .andExpect(content().json(user2ProfileJson));

    this.mockMvc.perform(get(url).with(httpBasic(user1, password)))
        .andExpect(status().isOk())
        .andExpect(
            content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
        .andExpect(content().json(user1ProfileJson));

    this.mockMvc.perform(get(url).with(httpBasic(user2, password)))
        .andExpect(status().isOk())
        .andExpect(
            content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
        .andExpect(content().json(user2ProfileJson));

    this.mockMvc.perform(get(url + "/all").with(httpBasic(admin, password)))
        .andExpect(status().isOk())
        .andExpect(
            content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
        .andExpect(content().json("[" + user1ProfileJson + "," + user2ProfileJson + "]"));

    this.mockMvc.perform(delete(url + "/user1").with(httpBasic(admin, password)))
        .andExpect(status().isOk());

    this.mockMvc.perform(delete(url + "/user1").with(httpBasic(admin, password)))
        .andExpect(status().isNotFound());

    this.mockMvc.perform(get(url).with(httpBasic(user1, password)))
        .andExpect(status().isNotFound());

    this.mockMvc.perform(get(url).with(httpBasic(user2, password)))
        .andExpect(status().isOk())
        .andExpect(
            content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
        .andExpect(content().json(user2ProfileJson));

    this.mockMvc.perform(get(url + "/all").with(httpBasic(admin, password)))
        .andExpect(status().isOk())
        .andExpect(
            content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
        .andExpect(content().json("[" + user2ProfileJson + "]"));

    this.mockMvc.perform(delete(url + "/user2").with(httpBasic(admin, password)))
        .andExpect(status().isOk());

    this.mockMvc.perform(get(url).with(httpBasic(user1, password)))
        .andExpect(status().isNotFound());

    this.mockMvc.perform(get(url).with(httpBasic(user2, password)))
        .andExpect(status().isNotFound());

    this.mockMvc.perform(get(url + "/all").with(httpBasic(admin, password)))
        .andExpect(status().isOk())
        .andExpect(
            content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
        .andExpect(jsonPath("$.*", hasSize(0)));
  }
}
