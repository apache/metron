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
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.configuration.SensorParserGroup;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.integration.utils.TestUtils;
import org.apache.metron.rest.service.GlobalConfigService;
import org.apache.metron.rest.service.SensorParserConfigService;
import org.apache.metron.rest.service.SensorParserGroupService;
import org.junit.After;
import org.junit.Assert;
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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

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

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TEST_PROFILE)
public class SensorParserGroupControllerIntegrationTest {

  /**
   {
   "name":"group1",
   "description":"group1 description",
   "sensors":["bro","snort"]
   }
   */
  @Multiline
  public static String group1BroSnort;

  /**
   {
   "name":"group1",
   "description":"group1 description",
   "sensors":["bro","squid"]
   }
   */
  @Multiline
  public static String group1BroSquid;

  /**
   {
   "name":"group2",
   "description":"group2 description",
   "sensors":["yaf","jsonMap"]
   }
   */
  @Multiline
  public static String group2YafJsonMap;

  /**
   {
   "name":"errorGroup",
   "description":"error description",
   "sensors":["bro"]
   }
   */
  @Multiline
  public static String errorGroup;

  @Autowired
  private GlobalConfigService globalConfigService;

  @Autowired
  private SensorParserConfigService sensorParserConfigService;

  @Autowired
  private SensorParserGroupService sensorParserGroupService;

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;
  private AtomicInteger numFields;

  private String sensorParserGroupUrl = "/api/v1/sensor/parser/group";
  private String user = "user";
  private String password = "password";

  @Before
  public void setup() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
    Method[] method = SensorParserGroup.class.getMethods();
    numFields = new AtomicInteger(0);
    for(Method m : method) {
      if(m.getName().startsWith("set")) {
        numFields.set(numFields.get() + 1);
      }
    }
    this.globalConfigService.save(new HashMap<>());
    this.sensorParserConfigService.save("bro", new SensorParserConfig());
    this.sensorParserConfigService.save("snort", new SensorParserConfig());
    this.sensorParserConfigService.save("squid", new SensorParserConfig());
    this.sensorParserConfigService.save("yaf", new SensorParserConfig());
    this.sensorParserConfigService.save("jsonMap", new SensorParserConfig());
    TestUtils.assertEventually(() -> Assert.assertNotNull(sensorParserConfigService.findOne("bro")));
    TestUtils.assertEventually(() -> Assert.assertNotNull(sensorParserConfigService.findOne("snort")));
    TestUtils.assertEventually(() -> Assert.assertNotNull(sensorParserConfigService.findOne("squid")));
    TestUtils.assertEventually(() -> Assert.assertNotNull(sensorParserConfigService.findOne("yaf")));
    TestUtils.assertEventually(() -> Assert.assertNotNull(sensorParserConfigService.findOne("jsonMap")));
  }

  @Test
  public void testSecurity() throws Exception {
    this.mockMvc.perform(post(sensorParserGroupUrl).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(group1BroSnort))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(sensorParserGroupUrl + "/group1"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(sensorParserGroupUrl))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(delete(sensorParserGroupUrl + "/group1").with(csrf()))
            .andExpect(status().isUnauthorized());
  }

  @Test
  public void testCreate() throws Exception {
    this.mockMvc.perform(post(sensorParserGroupUrl).with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(group1BroSnort))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.*", hasSize(numFields.get())))
            .andExpect(jsonPath("$.name").value("group1"))
            .andExpect(jsonPath("$.description").value("group1 description"))
            .andExpect(jsonPath("$.sensors[0]").value("bro"))
            .andExpect(jsonPath("$.sensors[1]").value("snort"));

    this.mockMvc.perform(post(sensorParserGroupUrl).with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(group2YafJsonMap))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.*", hasSize(numFields.get())))
            .andExpect(jsonPath("$.name").value("group2"))
            .andExpect(jsonPath("$.description").value("group2 description"))
            .andExpect(jsonPath("$.sensors[0]").value("jsonMap"))
            .andExpect(jsonPath("$.sensors[1]").value("yaf"));
  }

  @Test
  public void testUpdate() throws Exception {
    SensorParserGroup group1 = JSONUtils.INSTANCE.load(group1BroSquid, SensorParserGroup.class);
    this.sensorParserGroupService.save(group1);
    TestUtils.assertEventually(() -> Assert.assertEquals(group1, this.sensorParserGroupService.findOne("group1")));

    this.mockMvc.perform(post(sensorParserGroupUrl).with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(group1BroSquid))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.*", hasSize(numFields.get())))
            .andExpect(jsonPath("$.name").value("group1"))
            .andExpect(jsonPath("$.description").value("group1 description"))
            .andExpect(jsonPath("$.sensors[0]").value("squid"))
            .andExpect(jsonPath("$.sensors[1]").value("bro"));
  }

  @Test
  public void testFindOne() throws Exception {
    SensorParserGroup group1 = JSONUtils.INSTANCE.load(group1BroSquid, SensorParserGroup.class);
    this.sensorParserGroupService.save(group1);
    TestUtils.assertEventually(() -> Assert.assertEquals(group1, this.sensorParserGroupService.findOne("group1")));

    this.mockMvc.perform(get(sensorParserGroupUrl + "/group1").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.*", hasSize(numFields.get())))
            .andExpect(jsonPath("$.name").value("group1"))
            .andExpect(jsonPath("$.description").value("group1 description"))
            .andExpect(jsonPath("$.sensors[0]").value("squid"))
            .andExpect(jsonPath("$.sensors[1]").value("bro"));

    this.mockMvc.perform(get(sensorParserGroupUrl + "/missingGroup").with(httpBasic(user,password)))
            .andExpect(status().isNotFound());
  }

  @Test
  public void testGetAll() throws Exception {
    SensorParserGroup group1 = JSONUtils.INSTANCE.load(group1BroSquid, SensorParserGroup.class);
    this.sensorParserGroupService.save(group1);
    TestUtils.assertEventually(() -> Assert.assertEquals(group1, this.sensorParserGroupService.findOne("group1")));
    SensorParserGroup group2 = JSONUtils.INSTANCE.load(group2YafJsonMap, SensorParserGroup.class);
    this.sensorParserGroupService.save(group2);
    TestUtils.assertEventually(() -> Assert.assertEquals(group2, this.sensorParserGroupService.findOne("group2")));

    this.mockMvc.perform(get(sensorParserGroupUrl).with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.*", hasSize(2)))
            .andExpect(jsonPath("$.group1.*", hasSize(numFields.get())))
            .andExpect(jsonPath("$.group1.name").value("group1"))
            .andExpect(jsonPath("$.group1.description").value("group1 description"))
            .andExpect(jsonPath("$.group1.sensors[0]").value("squid"))
            .andExpect(jsonPath("$.group1.sensors[1]").value("bro"))
            .andExpect(jsonPath("$.group2.*", hasSize(numFields.get())))
            .andExpect(jsonPath("$.group2.name").value("group2"))
            .andExpect(jsonPath("$.group2.description").value("group2 description"))
            .andExpect(jsonPath("$.group2.sensors[0]").value("jsonMap"))
            .andExpect(jsonPath("$.group2.sensors[1]").value("yaf"));
  }

  @Test
  public void testError() throws Exception {
    SensorParserGroup group1 = JSONUtils.INSTANCE.load(group1BroSquid, SensorParserGroup.class);
    this.sensorParserGroupService.save(group1);
    TestUtils.assertEventually(() -> Assert.assertEquals(group1, this.sensorParserGroupService.findOne("group1")));

    this.mockMvc.perform(post(sensorParserGroupUrl).with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(errorGroup))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.responseCode").value(500))
            .andExpect(jsonPath("$.message").value("Sensor bro is already in group group1"))
            .andExpect(jsonPath("$.fullMessage").value("RestException: Sensor bro is already in group group1"));
  }

  @Test
  public void testDelete() throws Exception {
    SensorParserGroup group1 = JSONUtils.INSTANCE.load(group1BroSquid, SensorParserGroup.class);
    this.sensorParserGroupService.save(group1);
    TestUtils.assertEventually(() -> Assert.assertEquals(group1, this.sensorParserGroupService.findOne("group1")));

    this.mockMvc.perform(delete(sensorParserGroupUrl + "/group1").with(httpBasic(user,password)).with(csrf()))
            .andExpect(status().isOk());

    this.mockMvc.perform(delete(sensorParserGroupUrl + "/missingGroup").with(httpBasic(user,password)))
            .andExpect(status().isNotFound());

    {
      //we must wait for the config to find its way into the config.
      TestUtils.assertEventually(() -> Assert.assertNull(sensorParserGroupService.findOne("group1")));
    }
  }

  @After
  public void tearDown() throws Exception {
    this.globalConfigService.delete();
    this.sensorParserConfigService.delete("bro");
    this.sensorParserConfigService.delete("snort");
    this.sensorParserConfigService.delete("squid");
    this.sensorParserConfigService.delete("yaf");
    this.sensorParserConfigService.delete("jsonMap");
  }
}

