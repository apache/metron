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

import com.google.common.base.Function;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.components.KafkaWithZKComponent;
import org.apache.metron.rest.service.SensorParserConfigService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.Nullable;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SensorParserConfigControllerIntegrationTest {

  /**
   {
   "parserClassName": "org.apache.metron.parsers.GrokParser",
   "sensorTopic": "squid",
   "parserConfig": {
   "grokPath": "/patterns/squid",
   "patternLabel": "SQUID_DELIMITED",
   "timestampField": "timestamp"
   },
   "fieldTransformations" : [
   {
   "transformation" : "STELLAR"
   ,"output" : [ "full_hostname", "domain_without_subdomains" ]
   ,"config" : {
   "full_hostname" : "URL_TO_HOST(url)"
   ,"domain_without_subdomains" : "DOMAIN_REMOVE_SUBDOMAINS(full_hostname)"
   }
   }
   ]
   }
   */
  @Multiline
  public static String squidJson;

  /**
   {
   "parserClassName":"org.apache.metron.parsers.bro.BasicBroParser",
   "sensorTopic":"bro",
   "parserConfig": {}
   }
   */
  @Multiline
  public static String broJson;

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @Before
  public void setup() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  public void test() throws Exception {
    final KafkaWithZKComponent kafkaWithZKComponent = new KafkaWithZKComponent().withPostStartCallback(new Function<KafkaWithZKComponent, Void>() {
      @Nullable
      @Override
      public Void apply(@Nullable KafkaWithZKComponent kafkaWithZKComponent) {
        SensorParserConfigService sensorParserConfigService = (SensorParserConfigService) applicationContext.getBean("sensorParserConfigService");
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.newClient(kafkaWithZKComponent.getZookeeperConnect(), retryPolicy);
        client.start();
        sensorParserConfigService.setClient(client);
        return null;
      }
    });
    ComponentRunner runner = new ComponentRunner.Builder()
            .withComponent("kafka", kafkaWithZKComponent)
            .build();
    runner.start();

    this.mockMvc.perform(post("/sensorParserConfigs").contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(squidJson))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.parserClassName").value("org.apache.metron.parsers.GrokParser"))
            .andExpect(jsonPath("$.sensorTopic").value("squid"))
            .andExpect(jsonPath("$.parserConfig.grokPath").value("/patterns/squid"))
            .andExpect(jsonPath("$.parserConfig.patternLabel").value("SQUID_DELIMITED"))
            .andExpect(jsonPath("$.parserConfig.timestampField").value("timestamp"))
            .andExpect(jsonPath("$.fieldTransformations[0].transformation").value("STELLAR"))
            .andExpect(jsonPath("$.fieldTransformations[0].output[0]").value("full_hostname"))
            .andExpect(jsonPath("$.fieldTransformations[0].output[1]").value("domain_without_subdomains"))
            .andExpect(jsonPath("$.fieldTransformations[0].config.full_hostname").value("URL_TO_HOST(url)"))
            .andExpect(jsonPath("$.fieldTransformations[0].config.domain_without_subdomains").value("DOMAIN_REMOVE_SUBDOMAINS(full_hostname)"));

    this.mockMvc.perform(get("/sensorParserConfigs/squid"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.parserClassName").value("org.apache.metron.parsers.GrokParser"))
            .andExpect(jsonPath("$.sensorTopic").value("squid"))
            .andExpect(jsonPath("$.parserConfig.grokPath").value("/patterns/squid"))
            .andExpect(jsonPath("$.parserConfig.patternLabel").value("SQUID_DELIMITED"))
            .andExpect(jsonPath("$.parserConfig.timestampField").value("timestamp"))
            .andExpect(jsonPath("$.fieldTransformations[0].transformation").value("STELLAR"))
            .andExpect(jsonPath("$.fieldTransformations[0].output[0]").value("full_hostname"))
            .andExpect(jsonPath("$.fieldTransformations[0].output[1]").value("domain_without_subdomains"))
            .andExpect(jsonPath("$.fieldTransformations[0].config.full_hostname").value("URL_TO_HOST(url)"))
            .andExpect(jsonPath("$.fieldTransformations[0].config.domain_without_subdomains").value("DOMAIN_REMOVE_SUBDOMAINS(full_hostname)"));

    this.mockMvc.perform(get("/sensorParserConfigs"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].parserClassName").value("org.apache.metron.parsers.GrokParser"))
            .andExpect(jsonPath("$[0].sensorTopic").value("squid"))
            .andExpect(jsonPath("$[0].parserConfig.grokPath").value("/patterns/squid"))
            .andExpect(jsonPath("$[0].parserConfig.patternLabel").value("SQUID_DELIMITED"))
            .andExpect(jsonPath("$[0].parserConfig.timestampField").value("timestamp"))
            .andExpect(jsonPath("$[0].fieldTransformations[0].transformation").value("STELLAR"))
            .andExpect(jsonPath("$[0].fieldTransformations[0].output[0]").value("full_hostname"))
            .andExpect(jsonPath("$[0].fieldTransformations[0].output[1]").value("domain_without_subdomains"))
            .andExpect(jsonPath("$[0].fieldTransformations[0].config.full_hostname").value("URL_TO_HOST(url)"))
            .andExpect(jsonPath("$[0].fieldTransformations[0].config.domain_without_subdomains").value("DOMAIN_REMOVE_SUBDOMAINS(full_hostname)"));

    this.mockMvc.perform(post("/sensorParserConfigs").contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(broJson))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.parserClassName").value("org.apache.metron.parsers.bro.BasicBroParser"))
            .andExpect(jsonPath("$.sensorTopic").value("bro"))
            .andExpect(jsonPath("$.parserConfig").isEmpty());

    this.mockMvc.perform(get("/sensorParserConfigs"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].parserClassName").value("org.apache.metron.parsers.GrokParser"))
            .andExpect(jsonPath("$[0].sensorTopic").value("squid"))
            .andExpect(jsonPath("$[0].parserConfig.grokPath").value("/patterns/squid"))
            .andExpect(jsonPath("$[0].parserConfig.patternLabel").value("SQUID_DELIMITED"))
            .andExpect(jsonPath("$[0].parserConfig.timestampField").value("timestamp"))
            .andExpect(jsonPath("$[0].fieldTransformations[0].transformation").value("STELLAR"))
            .andExpect(jsonPath("$[0].fieldTransformations[0].output[0]").value("full_hostname"))
            .andExpect(jsonPath("$[0].fieldTransformations[0].output[1]").value("domain_without_subdomains"))
            .andExpect(jsonPath("$[0].fieldTransformations[0].config.full_hostname").value("URL_TO_HOST(url)"))
            .andExpect(jsonPath("$[0].fieldTransformations[0].config.domain_without_subdomains").value("DOMAIN_REMOVE_SUBDOMAINS(full_hostname)"))
            .andExpect(jsonPath("$[1].parserClassName").value("org.apache.metron.parsers.bro.BasicBroParser"))
            .andExpect(jsonPath("$[1].sensorTopic").value("bro"))
            .andExpect(jsonPath("$[1].parserConfig").isEmpty());

    this.mockMvc.perform(delete("/sensorParserConfigs/squid"))
            .andExpect(status().isOk());

    this.mockMvc.perform(get("/sensorParserConfigs/squid"))
            .andExpect(status().isNotFound());

    this.mockMvc.perform(delete("/sensorParserConfigw/squid"))
            .andExpect(status().isNotFound());

    this.mockMvc.perform(get("/sensorParserConfigs"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$[0].parserClassName").value("org.apache.metron.parsers.bro.BasicBroParser"))
            .andExpect(jsonPath("$[0].sensorTopic").value("bro"))
            .andExpect(jsonPath("$[0].parserConfig").isEmpty());

    this.mockMvc.perform(delete("/sensorParserConfigs/bro"))
            .andExpect(status().isOk());

    this.mockMvc.perform(delete("/sensorParserConfigs/bro"))
            .andExpect(status().isNotFound());

    this.mockMvc.perform(get("/sensorParserConfigs"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$", hasSize(0)));

    runner.stop();
  }
}

