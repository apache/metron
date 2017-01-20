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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.io.FileUtils;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.rest.repository.SensorParserConfigVersionRepository;
import org.apache.metron.rest.service.GrokService;
import org.apache.metron.rest.service.SensorParserConfigService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
public class SensorParserConfigHistoryControllerIntegrationTest {

    /**
     {
     "parserClassName":"org.apache.metron.parsers.bro.BasicBroParser",
     "sensorTopic":"broTest",
     "parserConfig": {
        "version":1
     }
     }
     */
    @Multiline
    public static String broJson1;

    /**
     {
     "parserClassName":"org.apache.metron.parsers.bro.BasicBroParser",
     "sensorTopic":"broTest",
     "parserConfig": {
     "version":2
     }
     }
     */
    @Multiline
    public static String broJson2;

    /**
     {
     "parserClassName":"org.apache.metron.parsers.bro.BasicBroParser",
     "sensorTopic":"broTest",
     "parserConfig": {
     "version":3
     }
     }
     */
    @Multiline
    public static String broJson3;

    @Autowired
    private Environment environment;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SensorParserConfigService sensorParserConfigService;

    @Autowired
    private SensorParserConfigVersionRepository sensorParserConfigVersionRepository;

    private MockMvc mockMvc;

    private String sensorParserConfigUrl = "/api/v1/sensor/parser/config";
    private String sensorParserConfigHistoryUrl = "/api/v1/sensor/parser/config/history";
    private String user1 = "user1";
    private String user2 = "user2";
    private String password = "password";

    @Before
    public void setup() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
    }

    @Test
    public void testSecurity() throws Exception {
        this.mockMvc.perform(get(sensorParserConfigHistoryUrl + "/broTest"))
                .andExpect(status().isUnauthorized());

        this.mockMvc.perform(get(sensorParserConfigHistoryUrl + "/getall"))
                .andExpect(status().isUnauthorized());

        this.mockMvc.perform(get(sensorParserConfigHistoryUrl + "/history/bro"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void test() throws Exception {
        cleanFileSystem();
        resetConfigs();
        resetAuditTables();
        SensorParserConfig bro3 = fromJson(broJson3);

        this.mockMvc.perform(get(sensorParserConfigHistoryUrl + "/broTest").with(httpBasic(user1,password)))
                .andExpect(status().isNotFound());

        this.mockMvc.perform(post(sensorParserConfigUrl).with(httpBasic(user1,password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(broJson1));
        Thread.sleep(1000);
        this.mockMvc.perform(post(sensorParserConfigUrl).with(httpBasic(user1,password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(broJson2));
        Thread.sleep(1000);
        this.mockMvc.perform(post(sensorParserConfigUrl).with(httpBasic(user2,password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(broJson3));

        assertEquals(bro3.getParserConfig().get("version"), fromJson(sensorParserConfigVersionRepository.findOne("broTest").getConfig()).getParserConfig().get("version"));

        String json = this.mockMvc.perform(get(sensorParserConfigHistoryUrl + "/broTest").with(httpBasic(user1,password)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
                .andExpect(jsonPath("$.config.parserClassName").value("org.apache.metron.parsers.bro.BasicBroParser"))
                .andExpect(jsonPath("$.config.sensorTopic").value("broTest"))
                .andExpect(jsonPath("$.config.parserConfig.version").value(3))
                .andExpect(jsonPath("$.createdBy").value(user1))
                .andExpect(jsonPath("$.modifiedBy").value(user2))
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> result = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        validateCreateModifiedByDate(result, false);

        this.mockMvc.perform(get(sensorParserConfigHistoryUrl).with(httpBasic(user1,password)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
                .andExpect(jsonPath("$[?(@.config.sensorTopic == 'broTest')]").exists());

        this.mockMvc.perform(delete(sensorParserConfigUrl + "/broTest").with(httpBasic(user2,password)).with(csrf()));

        json = this.mockMvc.perform(get(sensorParserConfigHistoryUrl + "/history/broTest").with(httpBasic(user1,password)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
                .andExpect(jsonPath("$[0].config", isEmptyOrNullString()))
                .andExpect(jsonPath("$[0].createdBy").value(user1))
                .andExpect(jsonPath("$[0].modifiedBy").value(user2))
                .andExpect(jsonPath("$[1].config.parserClassName").value("org.apache.metron.parsers.bro.BasicBroParser"))
                .andExpect(jsonPath("$[1].config.sensorTopic").value("broTest"))
                .andExpect(jsonPath("$[1].config.parserConfig.version").value(3))
                .andExpect(jsonPath("$[1].createdBy").value(user1))
                .andExpect(jsonPath("$[1].modifiedBy").value(user2))
                .andExpect(jsonPath("$[2].config.parserClassName").value("org.apache.metron.parsers.bro.BasicBroParser"))
                .andExpect(jsonPath("$[2].config.sensorTopic").value("broTest"))
                .andExpect(jsonPath("$[2].config.parserConfig.version").value(2))
                .andExpect(jsonPath("$[2].createdBy").value(user1))
                .andExpect(jsonPath("$[2].modifiedBy").value(user1))
                .andExpect(jsonPath("$[3].config.parserClassName").value("org.apache.metron.parsers.bro.BasicBroParser"))
                .andExpect(jsonPath("$[3].config.sensorTopic").value("broTest"))
                .andExpect(jsonPath("$[3].config.parserConfig.version").value(1))
                .andExpect(jsonPath("$[3].createdBy").value(user1))
                .andExpect(jsonPath("$[3].modifiedBy").value(user1))
                .andReturn().getResponse().getContentAsString();

        Map<String, Object>[] historyResults = objectMapper.readValue(json, new TypeReference<Map<String, Object>[]>() {});
        validateCreateModifiedByDate(historyResults[0], false);
        validateCreateModifiedByDate(historyResults[1], false);
        validateCreateModifiedByDate(historyResults[2], false);
        validateCreateModifiedByDate(historyResults[3], true);

        assertNull(sensorParserConfigVersionRepository.findOne("broTest"));
    }

    private void validateCreateModifiedByDate(Map<String, Object> result, boolean shouldBeEqual) {
        String createdDate = (String) result.get("createdDate");
        assertTrue("createdDate is not empty", createdDate != null && !"".equals(createdDate));
        String modifiedByDate = (String) result.get("modifiedByDate");
        assertTrue("modifiedByDate is not empty", modifiedByDate != null && !"".equals(modifiedByDate));
        if (shouldBeEqual) {
            assertTrue("createdDate and modifiedByDate should be equal", createdDate.equals(modifiedByDate));
        } else {
            assertTrue("createdDate and modifiedByDate should be different", !createdDate.equals(modifiedByDate));
        }
    }

    private void cleanFileSystem() throws IOException {
        File grokTempPath = new File(environment.getProperty(GrokService.GROK_TEMP_PATH_SPRING_PROPERTY));
        if (grokTempPath.exists()) {
            FileUtils.cleanDirectory(grokTempPath);
            FileUtils.deleteDirectory(grokTempPath);
        }
        File grokPath = new File(environment.getProperty(GrokService.GROK_DEFAULT_PATH_SPRING_PROPERTY));
        if (grokPath.exists()) {
          FileUtils.cleanDirectory(grokPath);
          FileUtils.deleteDirectory(grokPath);
        }
    }

    private void resetConfigs() throws Exception {
        for(SensorParserConfig sensorParserConfig: sensorParserConfigService.getAll()) {
            sensorParserConfigService.delete(sensorParserConfig.getSensorTopic());
        }
    }

    private void resetAuditTables() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("truncate table sensor_parser_config_version_aud");
        jdbcTemplate.execute("truncate table user_rev_entity");
        jdbcTemplate.execute("truncate table sensor_parser_config_version");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    private SensorParserConfig fromJson(String json) throws IOException {
        return objectMapper.readValue(json, new TypeReference<SensorParserConfig>() {});
    }
}
