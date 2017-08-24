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
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.service.GrokService;
import org.apache.metron.rest.service.SensorParserConfigService;
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

import java.io.File;
import java.io.IOException;

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
public class SensorParserConfigControllerIntegrationTest {

  /**
   {
   "parserClassName": "org.apache.metron.parsers.GrokParser",
   "sensorTopic": "squidTest",
   "parserConfig": {
   "patternLabel": "SQUIDTEST",
   "grokPath": "target/patterns/squidTest",
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
   "parserConfig": {},
   "readMetadata": true,
   "mergeMetadata": true
   }
   */
  @Multiline
  public static String broJson;

  /**
   {
   "sensorParserConfig":
   {
   "parserClassName": "org.apache.metron.parsers.GrokParser",
   "sensorTopic": "squidTest",
   "parserConfig": {
   "patternLabel": "SQUID_DELIMITED",
   "grokPath":"./squidTest",
   "timestampField": "timestamp"
   }
   },
   "grokStatement":"SQUID_DELIMITED %{NUMBER:timestamp}[^0-9]*%{INT:elapsed} %{IP:ip_src_addr} %{WORD:action}/%{NUMBER:code} %{NUMBER:bytes} %{WORD:method} %{NOTSPACE:url}[^0-9]*(%{IP:ip_dst_addr})?",
   "sampleData":"1467011157.401 415 127.0.0.1 TCP_MISS/200 337891 GET http://www.aliexpress.com/af/shoes.html? - DIRECT/207.109.73.154 text/html"
   }
   */
  @Multiline
  public static String parseRequest;

  /**
   {
   "sensorParserConfig": null,
   "sampleData":"1467011157.401 415 127.0.0.1 TCP_MISS/200 337891 GET http://www.aliexpress.com/af/shoes.html? - DIRECT/207.109.73.154 text/html"
   }
   */
  @Multiline
  public static String missingConfigParseRequest;

  /**
   {
   "sensorParserConfig":
   {
   "sensorTopic": "squidTest",
   "parserConfig": {
   "grokStatement": "%{NUMBER:timestamp} %{INT:elapsed} %{IPV4:ip_src_addr} %{WORD:action}/%{NUMBER:code} %{NUMBER:bytes} %{WORD:method} %{NOTSPACE:url} - %{WORD:UNWANTED}\/%{IPV4:ip_dst_addr} %{WORD:UNWANTED}\/%{WORD:UNWANTED}",
   "patternLabel": "SQUIDTEST",
   "grokPath":"./squidTest",
   "timestampField": "timestamp"
   }
   },
   "sampleData":"1467011157.401 415 127.0.0.1 TCP_MISS/200 337891 GET http://www.aliexpress.com/af/shoes.html? - DIRECT/207.109.73.154 text/html"
   }
   */
  @Multiline
  public static String missingClassParseRequest;

  /**
   {
   "sensorParserConfig":
   {
   "parserClassName": "badClass",
   "sensorTopic": "squidTest",
   "parserConfig": {
   "grokStatement": "%{NUMBER:timestamp} %{INT:elapsed} %{IPV4:ip_src_addr} %{WORD:action}/%{NUMBER:code} %{NUMBER:bytes} %{WORD:method} %{NOTSPACE:url} - %{WORD:UNWANTED}\/%{IPV4:ip_dst_addr} %{WORD:UNWANTED}\/%{WORD:UNWANTED}",
   "patternLabel": "SQUIDTEST",
   "grokPath":"./squidTest",
   "timestampField": "timestamp"
   }
   },
   "sampleData":"1467011157.401 415 127.0.0.1 TCP_MISS/200 337891 GET http://www.aliexpress.com/af/shoes.html? - DIRECT/207.109.73.154 text/html"
   }
   */
  @Multiline
  public static String badClassParseRequest;

  @Autowired
  private Environment environment;

  @Autowired
  private SensorParserConfigService sensorParserConfigService;

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  private String sensorParserConfigUrl = "/api/v1/sensor/parser/config";
  private String user = "user";
  private String password = "password";

  @Before
  public void setup() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
  }

  @Test
  public void testSecurity() throws Exception {
    this.mockMvc.perform(post(sensorParserConfigUrl).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(squidJson))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(sensorParserConfigUrl + "/squidTest"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(sensorParserConfigUrl))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(delete(sensorParserConfigUrl + "/squidTest").with(csrf()))
            .andExpect(status().isUnauthorized());
  }

  @Test
  public void test() throws Exception {
    cleanFileSystem();
    this.sensorParserConfigService.delete("broTest");
    this.sensorParserConfigService.delete("squidTest");

    this.mockMvc.perform(post(sensorParserConfigUrl).with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(squidJson))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.*", hasSize(10)))
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

    this.mockMvc.perform(get(sensorParserConfigUrl + "/squidTest").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.*", hasSize(10)))
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

    this.mockMvc.perform(get(sensorParserConfigUrl).with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$[?(@.parserClassName == 'org.apache.metron.parsers.GrokParser' &&" +
                    "@.sensorTopic == 'squidTest' &&" +
                    "@.parserConfig.grokPath == 'target/patterns/squidTest' &&" +
                    "@.parserConfig.patternLabel == 'SQUIDTEST' &&" +
                    "@.parserConfig.timestampField == 'timestamp' &&" +
                    "@.fieldTransformations[0].transformation == 'STELLAR' &&" +
                    "@.fieldTransformations[0].output[0] == 'full_hostname' &&" +
                    "@.fieldTransformations[0].output[1] == 'domain_without_subdomains' &&" +
                    "@.fieldTransformations[0].config.full_hostname == 'URL_TO_HOST(url)' &&" +
                    "@.fieldTransformations[0].config.domain_without_subdomains == 'DOMAIN_REMOVE_SUBDOMAINS(full_hostname)')]").exists());

    this.mockMvc.perform(post(sensorParserConfigUrl).with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(broJson))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.*", hasSize(10)))
            .andExpect(jsonPath("$.parserClassName").value("org.apache.metron.parsers.bro.BasicBroParser"))
            .andExpect(jsonPath("$.sensorTopic").value("broTest"))
            .andExpect(jsonPath("$.readMetadata").value("true"))
            .andExpect(jsonPath("$.mergeMetadata").value("true"))
            .andExpect(jsonPath("$.parserConfig").isEmpty());

    this.mockMvc.perform(post(sensorParserConfigUrl).with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(broJson))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.*", hasSize(10)))
            .andExpect(jsonPath("$.parserClassName").value("org.apache.metron.parsers.bro.BasicBroParser"))
            .andExpect(jsonPath("$.sensorTopic").value("broTest"))
            .andExpect(jsonPath("$.readMetadata").value("true"))
            .andExpect(jsonPath("$.mergeMetadata").value("true"))
            .andExpect(jsonPath("$.parserConfig").isEmpty());

    this.mockMvc.perform(get(sensorParserConfigUrl).with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$[?(@.parserClassName == 'org.apache.metron.parsers.GrokParser' &&" +
                    "@.sensorTopic == 'squidTest' &&" +
                    "@.parserConfig.grokPath == 'target/patterns/squidTest' &&" +
                    "@.parserConfig.patternLabel == 'SQUIDTEST' &&" +
                    "@.parserConfig.timestampField == 'timestamp' &&" +
                    "@.fieldTransformations[0].transformation == 'STELLAR' &&" +
                    "@.fieldTransformations[0].output[0] == 'full_hostname' &&" +
                    "@.fieldTransformations[0].output[1] == 'domain_without_subdomains' &&" +
                    "@.fieldTransformations[0].config.full_hostname == 'URL_TO_HOST(url)' &&" +
                    "@.fieldTransformations[0].config.domain_without_subdomains == 'DOMAIN_REMOVE_SUBDOMAINS(full_hostname)')]").exists())
            .andExpect(jsonPath("$[?(@.parserClassName == 'org.apache.metron.parsers.bro.BasicBroParser' && " +
                    "@.sensorTopic == 'broTest')]").exists());

    this.mockMvc.perform(delete(sensorParserConfigUrl + "/squidTest").with(httpBasic(user,password)).with(csrf()))
            .andExpect(status().isOk());

    this.mockMvc.perform(get(sensorParserConfigUrl + "/squidTest").with(httpBasic(user,password)))
            .andExpect(status().isNotFound());

    this.mockMvc.perform(delete(sensorParserConfigUrl + "/squidTest").with(httpBasic(user,password)).with(csrf()))
            .andExpect(status().isNotFound());

    this.mockMvc.perform(get(sensorParserConfigUrl).with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$[?(@.sensorTopic == 'squidTest')]").doesNotExist())
            .andExpect(jsonPath("$[?(@.sensorTopic == 'broTest')]").exists());

    this.mockMvc.perform(delete(sensorParserConfigUrl + "/broTest").with(httpBasic(user,password)).with(csrf()))
            .andExpect(status().isOk());

    this.mockMvc.perform(delete(sensorParserConfigUrl + "/broTest").with(httpBasic(user,password)).with(csrf()))
            .andExpect(status().isNotFound());

    this.mockMvc.perform(get(sensorParserConfigUrl).with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$[?(@.sensorTopic == 'squidTest')]").doesNotExist())
            .andExpect(jsonPath("$[?(@.sensorTopic == 'broTest')]").doesNotExist());

    this.mockMvc.perform(get(sensorParserConfigUrl + "/list/available").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.Bro").value("org.apache.metron.parsers.bro.BasicBroParser"))
            .andExpect(jsonPath("$.Grok").value("org.apache.metron.parsers.GrokParser"));

    this.mockMvc.perform(get(sensorParserConfigUrl + "/reload/available").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.Bro").value("org.apache.metron.parsers.bro.BasicBroParser"))
            .andExpect(jsonPath("$.Grok").value("org.apache.metron.parsers.GrokParser"));

    this.mockMvc.perform(post(sensorParserConfigUrl + "/parseMessage").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(parseRequest))
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

    this.mockMvc.perform(post(sensorParserConfigUrl + "/parseMessage").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(missingConfigParseRequest))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.responseCode").value(500))
            .andExpect(jsonPath("$.message").value("SensorParserConfig is missing from ParseMessageRequest"));

    this.mockMvc.perform(post(sensorParserConfigUrl + "/parseMessage").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(missingClassParseRequest))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.responseCode").value(500))
            .andExpect(jsonPath("$.message").value("SensorParserConfig must have a parserClassName"));

    this.mockMvc.perform(post(sensorParserConfigUrl + "/parseMessage").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(badClassParseRequest))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.responseCode").value(500))
            .andExpect(jsonPath("$.message").value("java.lang.ClassNotFoundException: badClass"));

    this.sensorParserConfigService.delete("broTest");
    this.sensorParserConfigService.delete("squidTest");
  }

  private void cleanFileSystem() throws IOException {
    File grokTempPath = new File(environment.getProperty(MetronRestConstants.GROK_TEMP_PATH_SPRING_PROPERTY));
    if (grokTempPath.exists()) {
      FileUtils.cleanDirectory(grokTempPath);
      FileUtils.deleteDirectory(grokTempPath);
    }
    File grokPath = new File(environment.getProperty(MetronRestConstants.GROK_DEFAULT_PATH_SPRING_PROPERTY));
    if (grokPath.exists()) {
      FileUtils.cleanDirectory(grokPath);
      FileUtils.deleteDirectory(grokPath);
    }
  }
}

