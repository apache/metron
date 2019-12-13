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
import org.apache.metron.rest.service.SensorEnrichmentConfigService;
import org.hamcrest.core.IsCollectionContaining;
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

import java.nio.charset.StandardCharsets;

import static org.apache.metron.integration.utils.TestUtils.assertEventually;
import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TEST_PROFILE)
public class SensorEnrichmentConfigControllerIntegrationTest {

  /**
   {
   "enrichment": {
   "fieldMap": {
   "geo": [
   "ip_dst_addr"
   ],
   "host": [
   "ip_dst_addr"
   ],
   "hbaseEnrichment": [
   "ip_src_addr"
   ],
   "stellar": {
   "config": {
   "group1": {
   "foo": "1 + 1",
   "bar": "foo"
   },
   "group2": {
   "ALL_CAPS": "TO_UPPER(source.type)"
   }
   }
   }
   },
   "fieldToTypeMap": {
   "ip_src_addr": [
   "sample"
   ]
   }
   },
   "threatIntel": {
   "fieldMap": {
   "hbaseThreatIntel": [
   "ip_src_addr",
   "ip_dst_addr"
   ]
   },
   "fieldToTypeMap": {
   "ip_src_addr": [
   "malicious_ip"
   ],
   "ip_dst_addr": [
   "malicious_ip"
   ]
   },
   "triageConfig": {
   "riskLevelRules": [
   {
   "rule": "ip_src_addr == '10.122.196.204' or ip_dst_addr == '10.122.196.204'",
   "score": 10
   }
   ],
   "aggregator": "MAX"
   }
   }
   }
   */
  @Multiline
  public static String broJson;

  @Autowired
  private SensorEnrichmentConfigService sensorEnrichmentConfigService;

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  private String sensorEnrichmentConfigUrl = "/api/v1/sensor/enrichment/config";
  private String user = "user";
  private String password = "password";

  @BeforeEach
  public void setup() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
  }

  @Test
  public void testSecurity() throws Exception {
    this.mockMvc.perform(post(sensorEnrichmentConfigUrl).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(broJson))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(sensorEnrichmentConfigUrl + "/broTest"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(sensorEnrichmentConfigUrl))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(delete(sensorEnrichmentConfigUrl + "/broTest").with(csrf()))
            .andExpect(status().isUnauthorized());
  }

  /**
   * Note: <a href="https://github.com/json-path/JsonPath#path-examples">JSON Path examples</a>
   */
  @Test
  public void test() throws Exception {
    sensorEnrichmentConfigService.delete("broTest");

    this.mockMvc.perform(get(sensorEnrichmentConfigUrl).with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(content().bytes("{}".getBytes(StandardCharsets.UTF_8)));

    this.mockMvc.perform(post(sensorEnrichmentConfigUrl + "/broTest").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(broJson))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.enrichment.fieldMap.geo[0]").value("ip_dst_addr"))
            .andExpect(jsonPath("$.enrichment.fieldMap.host[0]").value("ip_dst_addr"))
            .andExpect(jsonPath("$.enrichment.fieldMap.hbaseEnrichment[0]").value("ip_src_addr"))
            .andExpect(jsonPath("$.enrichment.fieldToTypeMap.ip_src_addr[0]").value("sample"))
            .andExpect(jsonPath("$.enrichment.fieldMap.stellar.config.group1.foo").value("1 + 1"))
            .andExpect(jsonPath("$.enrichment.fieldMap.stellar.config.group1.bar").value("foo"))
            .andExpect(jsonPath("$.enrichment.fieldMap.stellar.config.group2.ALL_CAPS").value("TO_UPPER(source.type)"))
            .andExpect(jsonPath("$.threatIntel.fieldMap.hbaseThreatIntel[0]").value("ip_src_addr"))
            .andExpect(jsonPath("$.threatIntel.fieldMap.hbaseThreatIntel[1]").value("ip_dst_addr"))
            .andExpect(jsonPath("$.threatIntel.fieldToTypeMap.ip_src_addr[0]").value("malicious_ip"))
            .andExpect(jsonPath("$.threatIntel.fieldToTypeMap.ip_dst_addr[0]").value("malicious_ip"))
            .andExpect(jsonPath("$.threatIntel.triageConfig.riskLevelRules[0].rule").value("ip_src_addr == '10.122.196.204' or ip_dst_addr == '10.122.196.204'"))
            .andExpect(jsonPath("$.threatIntel.triageConfig.riskLevelRules[0].score").value(10))
            .andExpect(jsonPath("$.threatIntel.triageConfig.aggregator").value("MAX"));

    assertEventually(() -> this.mockMvc.perform(post(sensorEnrichmentConfigUrl + "/broTest").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(broJson))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.enrichment.fieldMap.geo[0]").value("ip_dst_addr"))
            .andExpect(jsonPath("$.enrichment.fieldMap.host[0]").value("ip_dst_addr"))
            .andExpect(jsonPath("$.enrichment.fieldMap.hbaseEnrichment[0]").value("ip_src_addr"))
            .andExpect(jsonPath("$.enrichment.fieldToTypeMap.ip_src_addr[0]").value("sample"))
            .andExpect(jsonPath("$.enrichment.fieldMap.stellar.config.group1.foo").value("1 + 1"))
            .andExpect(jsonPath("$.enrichment.fieldMap.stellar.config.group1.bar").value("foo"))
            .andExpect(jsonPath("$.enrichment.fieldMap.stellar.config.group2.ALL_CAPS").value("TO_UPPER(source.type)"))
            .andExpect(jsonPath("$.threatIntel.fieldMap.hbaseThreatIntel[0]").value("ip_src_addr"))
            .andExpect(jsonPath("$.threatIntel.fieldMap.hbaseThreatIntel[1]").value("ip_dst_addr"))
            .andExpect(jsonPath("$.threatIntel.fieldToTypeMap.ip_src_addr[0]").value("malicious_ip"))
            .andExpect(jsonPath("$.threatIntel.fieldToTypeMap.ip_dst_addr[0]").value("malicious_ip"))
            .andExpect(jsonPath("$.threatIntel.triageConfig.riskLevelRules[0].rule").value("ip_src_addr == '10.122.196.204' or ip_dst_addr == '10.122.196.204'"))
            .andExpect(jsonPath("$.threatIntel.triageConfig.riskLevelRules[0].score").value(10))
            .andExpect(jsonPath("$.threatIntel.triageConfig.aggregator").value("MAX") )
    );

    this.mockMvc.perform(get(sensorEnrichmentConfigUrl + "/broTest").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.enrichment.fieldMap.geo[0]").value("ip_dst_addr"))
            .andExpect(jsonPath("$.enrichment.fieldMap.host[0]").value("ip_dst_addr"))
            .andExpect(jsonPath("$.enrichment.fieldMap.hbaseEnrichment[0]").value("ip_src_addr"))
            .andExpect(jsonPath("$.enrichment.fieldToTypeMap.ip_src_addr[0]").value("sample"))
            .andExpect(jsonPath("$.enrichment.fieldMap.stellar.config.group1.foo").value("1 + 1"))
            .andExpect(jsonPath("$.enrichment.fieldMap.stellar.config.group1.bar").value("foo"))
            .andExpect(jsonPath("$.enrichment.fieldMap.stellar.config.group2.ALL_CAPS").value("TO_UPPER(source.type)"))
            .andExpect(jsonPath("$.threatIntel.fieldMap.hbaseThreatIntel[0]").value("ip_src_addr"))
            .andExpect(jsonPath("$.threatIntel.fieldMap.hbaseThreatIntel[1]").value("ip_dst_addr"))
            .andExpect(jsonPath("$.threatIntel.fieldToTypeMap.ip_src_addr[0]").value("malicious_ip"))
            .andExpect(jsonPath("$.threatIntel.fieldToTypeMap.ip_dst_addr[0]").value("malicious_ip"))
            .andExpect(jsonPath("$.threatIntel.triageConfig.riskLevelRules[0].rule").value("ip_src_addr == '10.122.196.204' or ip_dst_addr == '10.122.196.204'"))
            .andExpect(jsonPath("$.threatIntel.triageConfig.riskLevelRules[0].score").value(10))
            .andExpect(jsonPath("$.threatIntel.triageConfig.aggregator").value("MAX"));

    this.mockMvc.perform(get(sensorEnrichmentConfigUrl).with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$[?(@.broTest.enrichment.fieldMap.geo[0] == 'ip_dst_addr' &&" +
                    "@.broTest.enrichment.fieldMap.host[0] == 'ip_dst_addr' &&" +
                    "@.broTest.enrichment.fieldMap.hbaseEnrichment[0] == 'ip_src_addr' &&" +
                    "@.broTest.enrichment.fieldToTypeMap.ip_src_addr[0] == 'sample' &&" +
                    "@.broTest.enrichment.fieldMap.stellar.config.group1.foo == '1 + 1' &&" +
                    "@.broTest.enrichment.fieldMap.stellar.config.group1.bar == 'foo' &&" +
                    "@.broTest.enrichment.fieldMap.stellar.config.group2.ALL_CAPS == 'TO_UPPER(source.type)' &&" +
                    "@.broTest.threatIntel.fieldMap.hbaseThreatIntel[0] == 'ip_src_addr' &&" +
                    "@.broTest.threatIntel.fieldMap.hbaseThreatIntel[1] == 'ip_dst_addr' &&" +
                    "@.broTest.threatIntel.fieldToTypeMap.ip_src_addr[0] == 'malicious_ip' &&" +
                    "@.broTest.threatIntel.fieldToTypeMap.ip_dst_addr[0] == 'malicious_ip' &&" +
                    "@.broTest.threatIntel.triageConfig.riskLevelRules[0].rule == \"ip_src_addr == '10.122.196.204' or ip_dst_addr == '10.122.196.204'\" &&" +
                    "@.broTest.threatIntel.triageConfig.riskLevelRules[0].score == 10 &&" +
                    "@.broTest.threatIntel.triageConfig.aggregator == 'MAX'" +
                    ")]").exists());

    this.mockMvc.perform(delete(sensorEnrichmentConfigUrl + "/broTest").with(httpBasic(user,password)).with(csrf()))
            .andExpect(status().isOk());

    this.mockMvc.perform(get(sensorEnrichmentConfigUrl + "/broTest").with(httpBasic(user,password)))
            .andExpect(status().isNotFound());

    this.mockMvc.perform(delete(sensorEnrichmentConfigUrl + "/broTest").with(httpBasic(user,password)).with(csrf()))
            .andExpect(status().isNotFound());

    this.mockMvc.perform(get(sensorEnrichmentConfigUrl).with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$[?(@.sensorTopic == 'broTest')]").doesNotExist());

    this.mockMvc.perform(get(sensorEnrichmentConfigUrl + "/list/available/enrichments").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.length()").value("3"))
            .andExpect(jsonPath("$.*").value(IsCollectionContaining.hasItems("foo", "bar", "baz")));

    this.mockMvc.perform(get(sensorEnrichmentConfigUrl + "/list/available/threat/triage/aggregators").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$[0]").value("MAX"))
            .andExpect(jsonPath("$[1]").value("MIN"))
            .andExpect(jsonPath("$[2]").value("SUM"))
            .andExpect(jsonPath("$[3]").value("MEAN"))
            .andExpect(jsonPath("$[4]").value("POSITIVE_MEAN"))
    ;

    sensorEnrichmentConfigService.delete("broTest");
  }
}

