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

import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.integration.utils.TestUtils;
import org.apache.metron.rest.model.TopologyStatusCode;
import org.apache.metron.rest.service.GlobalConfigService;
import org.apache.metron.rest.service.SensorParserConfigService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TEST_PROFILE)
public class StormControllerIntegrationTest {

  @Autowired
  private Environment environment;

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private GlobalConfigService globalConfigService;

  @Autowired
  private SensorParserConfigService sensorParserConfigService;

  private MockMvc mockMvc;

  private String stormUrl = "/api/v1/storm";
  private String user = "user";
  private String password = "password";

  private String metronVersion;

  @Before
  public void setup() throws Exception {
    this.metronVersion = this.environment.getProperty("metron.version");
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
  }

  @Test
  public void testSecurity() throws Exception {
    this.mockMvc.perform(get(stormUrl))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(stormUrl + "/supervisors"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(stormUrl + "/broTest"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(stormUrl + "/parser/start/broTest"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(stormUrl + "/parser/stop/broTest"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(stormUrl + "/parser/activate/broTest"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(stormUrl + "/parser/deactivate/broTest"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get("/enrichment"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(stormUrl + "/enrichment/start"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(stormUrl + "/enrichment/stop"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(stormUrl + "/enrichment/activate"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(stormUrl + "/enrichment/deactivate"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get("/indexing"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(stormUrl + "/indexing/start"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(stormUrl + "/indexing/stop"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(stormUrl + "/indexing/activate"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(stormUrl + "/indexing/deactivate"))
            .andExpect(status().isUnauthorized());
  }

  @Test
  public void test() throws Exception {
    this.mockMvc.perform(get(stormUrl).with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

    this.mockMvc.perform(get(stormUrl + "/broTest").with(httpBasic(user,password)))
            .andExpect(status().isNotFound());

    Map<String, Object> globalConfig = globalConfigService.get();
    if (globalConfig == null) {
      globalConfig = new HashMap<>();
    }
    globalConfigService.delete();
    sensorParserConfigService.delete("broTest");

    this.mockMvc.perform(get(stormUrl + "/parser/stop/broTest?stopNow=true").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.STOP_ERROR.toString()));

    this.mockMvc.perform(get(stormUrl + "/parser/activate/broTest").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.TOPOLOGY_NOT_FOUND.name()));

    this.mockMvc.perform(get(stormUrl + "/parser/deactivate/broTest").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.TOPOLOGY_NOT_FOUND.name()));

    this.mockMvc.perform(get(stormUrl + "/parser/start/broTest").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.GLOBAL_CONFIG_MISSING.name()));

    globalConfigService.save(globalConfig);
    {
      final Map<String, Object> expectedGlobalConfig = globalConfig;
      //we must wait for the config to find its way into the config.
      TestUtils.assertEventually(() -> Assert.assertEquals(expectedGlobalConfig, globalConfigService.get()));
    }

    this.mockMvc.perform(get(stormUrl + "/parser/start/broTest").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.SENSOR_PARSER_CONFIG_MISSING.name()));

    SensorParserConfig sensorParserConfig = new SensorParserConfig();
    sensorParserConfig.setParserClassName("org.apache.metron.parsers.bro.BasicBroParser");
    sensorParserConfig.setSensorTopic("broTest");
    sensorParserConfigService.save(sensorParserConfig);
    {
      final SensorParserConfig expectedSensorParserConfig = sensorParserConfig;
      //we must wait for the config to find its way into the config.
      TestUtils.assertEventually(() -> Assert.assertEquals(expectedSensorParserConfig, sensorParserConfigService.findOne("broTest")));
    }

    this.mockMvc.perform(get(stormUrl + "/parser/start/broTest").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.STARTED.name()));

    this.mockMvc.perform(get(stormUrl + "/supervisors").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.supervisors[0]").exists())
            .andExpect(jsonPath("$.supervisors[0].id").exists())
            .andExpect(jsonPath("$.supervisors[0].host").exists())
            .andExpect(jsonPath("$.supervisors[0].uptime").exists())
            .andExpect(jsonPath("$.supervisors[0].slotsTotal").exists())
            .andExpect(jsonPath("$.supervisors[0].slotsUsed").exists());

    this.mockMvc.perform(get(stormUrl + "/broTest").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.name").value("broTest"))
            .andExpect(jsonPath("$.id", containsString("broTest")))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.latency").exists())
            .andExpect(jsonPath("$.throughput").exists())
            .andExpect(jsonPath("$.emitted").exists())
            .andExpect(jsonPath("$.acked").exists());

    this.mockMvc.perform(get(stormUrl).with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$[?(@.name == 'broTest' && @.status == 'ACTIVE')]").exists());

    this.mockMvc.perform(get(stormUrl + "/parser/stop/broTest?stopNow=true").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.STOPPED.name()));

    this.mockMvc.perform(get(stormUrl + "/enrichment").with(httpBasic(user,password)))
            .andExpect(status().isNotFound());

    this.mockMvc.perform(get(stormUrl + "/enrichment/activate").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.TOPOLOGY_NOT_FOUND.name()));

    this.mockMvc.perform(get(stormUrl + "/enrichment/deactivate").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.TOPOLOGY_NOT_FOUND.name()));

    this.mockMvc.perform(get(stormUrl + "/enrichment/stop?stopNow=true").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.STOP_ERROR.toString()));

    this.mockMvc.perform(get(stormUrl + "/enrichment/start").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.STARTED.toString()));

    this.mockMvc.perform(get(stormUrl + "/enrichment/deactivate").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.INACTIVE.name()));

    this.mockMvc.perform(get(stormUrl + "/enrichment/deactivate").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.INACTIVE.name()));

    this.mockMvc.perform(get(stormUrl + "/enrichment/activate").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.ACTIVE.name()));

    this.mockMvc.perform(get(stormUrl + "/enrichment").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.name").value("enrichment"))
            .andExpect(jsonPath("$.id", containsString("enrichment")))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.latency").exists())
            .andExpect(jsonPath("$.throughput").exists())
            .andExpect(jsonPath("$.emitted").exists())
            .andExpect(jsonPath("$.acked").exists());

    this.mockMvc.perform(get(stormUrl).with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$[?(@.name == 'enrichment' && @.status == 'ACTIVE')]").exists());

    this.mockMvc.perform(get(stormUrl + "/enrichment/stop").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.STOPPED.name()));

    this.mockMvc.perform(get(stormUrl + "/indexing").with(httpBasic(user,password)))
            .andExpect(status().isNotFound());

    this.mockMvc.perform(get(stormUrl + "/indexing/activate").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.TOPOLOGY_NOT_FOUND.name()));

    this.mockMvc.perform(get(stormUrl + "/indexing/deactivate").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.TOPOLOGY_NOT_FOUND.name()));

    this.mockMvc.perform(get(stormUrl + "/indexing/stop?stopNow=true").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.STOP_ERROR.toString()));

    this.mockMvc.perform(get(stormUrl + "/indexing/start").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.STARTED.toString()));

    this.mockMvc.perform(get(stormUrl + "/indexing/deactivate").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.INACTIVE.name()));

    this.mockMvc.perform(get(stormUrl + "/indexing/activate").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.ACTIVE.name()));

    this.mockMvc.perform(get(stormUrl + "/indexing").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.name").value("indexing"))
            .andExpect(jsonPath("$.id", containsString("indexing")))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.latency").exists())
            .andExpect(jsonPath("$.throughput").exists())
            .andExpect(jsonPath("$.emitted").exists())
            .andExpect(jsonPath("$.acked").exists());

    this.mockMvc.perform(get(stormUrl).with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$[?(@.name == 'indexing' && @.status == 'ACTIVE')]").exists());

    this.mockMvc.perform(get(stormUrl + "/indexing/stop").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value(TopologyStatusCode.STOPPED.name()));

    this.mockMvc.perform(get(stormUrl + "/client/status").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stormClientVersionInstalled").value("1.0.1"))
            .andExpect(jsonPath("$.parserScriptPath").value("/usr/metron/" + metronVersion + "/bin/start_parser_topology.sh"))
            .andExpect(jsonPath("$.enrichmentScriptPath").value("/usr/metron/" + metronVersion + "/bin/start_enrichment_topology.sh"))
            .andExpect(jsonPath("$.indexingScriptPath").value("/usr/metron/" + metronVersion + "/bin/start_elasticsearch_topology.sh"));

    globalConfigService.delete();
    sensorParserConfigService.delete("broTest");
  }
}
