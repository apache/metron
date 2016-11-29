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
import org.apache.metron.rest.service.SensorParserConfigService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

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
@ActiveProfiles("test")
public class SensorParserConfigControllerIntegrationTest {

  /**
   {
   "parserClassName": "org.apache.metron.parsers.GrokParser",
   "sensorTopic": "squidTest",
   "parserConfig": {
   "grokStatement": "SQUID %{NUMBER:timestamp} %{INT:elapsed} %{IPV4:ip_src_addr} %{WORD:action}/%{NUMBER:code} %{NUMBER:bytes} %{WORD:method} %{NOTSPACE:url} - %{WORD:UNWANTED}\/%{IPV4:ip_dst_addr} %{WORD:UNWANTED}\/%{WORD:UNWANTED}",
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
   "sensorTopic":"broTest",
   "parserConfig": {}
   }
   */
  @Multiline
  public static String broJson;

  /**
   {
   "sensorParserConfig":
   {
   "parserClassName": "org.apache.metron.parsers.GrokParser",
   "sensorTopic": "squid",
   "parserConfig": {
   "grokStatement": "SQUID %{NUMBER:timestamp} %{INT:elapsed} %{IPV4:ip_src_addr} %{WORD:action}/%{NUMBER:code} %{NUMBER:bytes} %{WORD:method} %{NOTSPACE:url} - %{WORD:UNWANTED}\/%{IPV4:ip_dst_addr} %{WORD:UNWANTED}\/%{WORD:UNWANTED}",
   "patternLabel": "SQUID",
   "timestampField": "timestamp"
   }
   },
   "sampleData":"1467011157.401 415 127.0.0.1 TCP_MISS/200 337891 GET http://www.aliexpress.com/af/shoes.html? - DIRECT/207.109.73.154 text/html"
   }
   */
  @Multiline
  public static String parseRequest;

  @Autowired
  private SensorParserConfigService sensorParserConfigService;

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private ApplicationContext applicationContext;

  private MockMvc mockMvc;

  private String user = "user";
  private String password = "password";

  @Before
  public void setup() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
  }

  @Test
  public void testSecurity() throws Exception {
    this.mockMvc.perform(post("/sensorParserConfig").with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(squidJson))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get("/sensorParserConfig/squidTest"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get("/sensorParserConfig"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(delete("/sensorParserConfig/squidTest").with(csrf()))
            .andExpect(status().isUnauthorized());
  }

  @Test
  public void test() throws Exception {

    this.mockMvc.perform(post("/sensorParserConfig").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(squidJson))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.parserClassName").value("org.apache.metron.parsers.GrokParser"))
            .andExpect(jsonPath("$.sensorTopic").value("squidTest"))
            .andExpect(jsonPath("$.parserConfig.grokPath").value("target/patterns/squidTest"))
            .andExpect(jsonPath("$.parserConfig.patternLabel").value("SQUIDTEST"))
            .andExpect(jsonPath("$.parserConfig.timestampField").value("timestamp"))
            .andExpect(jsonPath("$.fieldTransformations[0].transformation").value("STELLAR"))
            .andExpect(jsonPath("$.fieldTransformations[0].output[0]").value("full_hostname"))
            .andExpect(jsonPath("$.fieldTransformations[0].output[1]").value("domain_without_subdomains"))
            .andExpect(jsonPath("$.fieldTransformations[0].config.full_hostname").value("URL_TO_HOST(url)"))
            .andExpect(jsonPath("$.fieldTransformations[0].config.domain_without_subdomains").value("DOMAIN_REMOVE_SUBDOMAINS(full_hostname)"));

    this.mockMvc.perform(get("/sensorParserConfig/squidTest").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.parserClassName").value("org.apache.metron.parsers.GrokParser"))
            .andExpect(jsonPath("$.sensorTopic").value("squidTest"))
            .andExpect(jsonPath("$.parserConfig.grokPath").value("target/patterns/squidTest"))
            .andExpect(jsonPath("$.parserConfig.timestampField").value("timestamp"))
            .andExpect(jsonPath("$.fieldTransformations[0].transformation").value("STELLAR"))
            .andExpect(jsonPath("$.fieldTransformations[0].output[0]").value("full_hostname"))
            .andExpect(jsonPath("$.fieldTransformations[0].output[1]").value("domain_without_subdomains"))
            .andExpect(jsonPath("$.fieldTransformations[0].config.full_hostname").value("URL_TO_HOST(url)"))
            .andExpect(jsonPath("$.fieldTransformations[0].config.domain_without_subdomains").value("DOMAIN_REMOVE_SUBDOMAINS(full_hostname)"));

    this.mockMvc.perform(get("/sensorParserConfig").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$[?(@.parserClassName == 'org.apache.metron.parsers.GrokParser' &&" +
                    "@.sensorTopic == 'squidTest' &&" +
                    "@.parserConfig.grokPath == 'target/patterns/squidTest' &&" +
                    "@.parserConfig.timestampField == 'timestamp' &&" +
                    "@.fieldTransformations[0].transformation == 'STELLAR' &&" +
                    "@.fieldTransformations[0].output[0] == 'full_hostname' &&" +
                    "@.fieldTransformations[0].output[1] == 'domain_without_subdomains' &&" +
                    "@.fieldTransformations[0].config.full_hostname == 'URL_TO_HOST(url)' &&" +
                    "@.fieldTransformations[0].config.domain_without_subdomains == 'DOMAIN_REMOVE_SUBDOMAINS(full_hostname)')]").exists());

    this.mockMvc.perform(post("/sensorParserConfig").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(broJson))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.parserClassName").value("org.apache.metron.parsers.bro.BasicBroParser"))
            .andExpect(jsonPath("$.sensorTopic").value("broTest"))
            .andExpect(jsonPath("$.parserConfig").isEmpty());

    this.mockMvc.perform(get("/sensorParserConfig").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$[?(@.parserClassName == 'org.apache.metron.parsers.GrokParser' &&" +
                    "@.sensorTopic == 'squidTest' &&" +
                    "@.parserConfig.grokPath == 'target/patterns/squidTest' &&" +
                    "@.parserConfig.timestampField == 'timestamp' &&" +
                    "@.fieldTransformations[0].transformation == 'STELLAR' &&" +
                    "@.fieldTransformations[0].output[0] == 'full_hostname' &&" +
                    "@.fieldTransformations[0].output[1] == 'domain_without_subdomains' &&" +
                    "@.fieldTransformations[0].config.full_hostname == 'URL_TO_HOST(url)' &&" +
                    "@.fieldTransformations[0].config.domain_without_subdomains == 'DOMAIN_REMOVE_SUBDOMAINS(full_hostname)')]").exists())
            .andExpect(jsonPath("$[?(@.parserClassName == 'org.apache.metron.parsers.bro.BasicBroParser' && " +
                    "@.sensorTopic == 'broTest')]").exists());

    this.mockMvc.perform(delete("/sensorParserConfig/squidTest").with(httpBasic(user,password)).with(csrf()))
            .andExpect(status().isOk());

    this.mockMvc.perform(get("/sensorParserConfig/squidTest").with(httpBasic(user,password)))
            .andExpect(status().isNotFound());

    this.mockMvc.perform(delete("/sensorParserConfig/squidTest").with(httpBasic(user,password)).with(csrf()))
            .andExpect(status().isNotFound());

    this.mockMvc.perform(get("/sensorParserConfig").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$[?(@.sensorTopic == 'squidTest')]").doesNotExist())
            .andExpect(jsonPath("$[?(@.sensorTopic == 'broTest')]").exists());

    this.mockMvc.perform(delete("/sensorParserConfig/broTest").with(httpBasic(user,password)).with(csrf()))
            .andExpect(status().isOk());

    this.mockMvc.perform(delete("/sensorParserConfig/broTest").with(httpBasic(user,password)).with(csrf()))
            .andExpect(status().isNotFound());

    this.mockMvc.perform(get("/sensorParserConfig").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$[?(@.sensorTopic == 'squidTest')]").doesNotExist())
            .andExpect(jsonPath("$[?(@.sensorTopic == 'broTest')]").doesNotExist());

    this.mockMvc.perform(get("/sensorParserConfig/list/available").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.Bro").value("org.apache.metron.parsers.bro.BasicBroParser"))
            .andExpect(jsonPath("$.Grok").value("org.apache.metron.parsers.GrokParser"));

    this.mockMvc.perform(get("/sensorParserConfig/reload/available").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.Bro").value("org.apache.metron.parsers.bro.BasicBroParser"))
            .andExpect(jsonPath("$.Grok").value("org.apache.metron.parsers.GrokParser"));

    this.mockMvc.perform(post("/sensorParserConfig/parseMessage").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(parseRequest))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.elapsed").value(415))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.ip_dst_addr").value("207.109.73.154"))
            .andExpect(jsonPath("$.method").value("GET"))
            .andExpect(jsonPath("$.bytes").value(337891))
            .andExpect(jsonPath("$.action").value("TCP_MISS"))
            .andExpect(jsonPath("$.ip_src_addr").value("127.0.0.1"))
            .andExpect(jsonPath("$.url").value("http://www.aliexpress.com/af/shoes.html?"))
            .andExpect(jsonPath("$.timestamp").value(1467011157401L));

    //runner.stop();
  }
}

