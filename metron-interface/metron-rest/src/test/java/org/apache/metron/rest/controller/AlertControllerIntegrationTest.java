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

import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.UnableToStartException;
import org.apache.metron.integration.components.KafkaComponent;
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
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TEST_PROFILE)
public class AlertControllerIntegrationTest {

    /**
     [
     {
     "is_alert": true,
     "field": "value1"
     },
     {
     "is_alert": true,
     "field": "value2"
     }
     ]
     */
    @Multiline
    public static String alerts;

    // A bug in Spring and/or Kafka forced us to move into a component that is spun up and down per test-case
    // Given the large spinup time of components, please avoid this pattern until we upgrade Spring.
    // See: https://issues.apache.org/jira/browse/METRON-1009
    @Autowired
    private KafkaComponent kafkaWithZKComponent;
    private ComponentRunner runner;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    private String alertUrl = "/api/v1/alert";
    private String user = "user";
    private String password = "password";

    @Before
    public void setup() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
    }

    @Test
    public void testSecurity() throws Exception {
        this.mockMvc.perform(post(alertUrl + "/escalate").with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(alerts))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void escalateShouldEscalateAlerts() throws Exception {
        startKafka();
        this.mockMvc.perform(post(alertUrl + "/escalate").with(httpBasic(user,password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(alerts))
                .andExpect(status().isOk());
        stopKafka();
    }

    private void startKafka() {
        runner = new ComponentRunner.Builder()
            .withComponent("kafka", kafkaWithZKComponent)
            .withCustomShutdownOrder(new String[]{"kafka"})
            .build();
        try {
            runner.start();
        } catch (UnableToStartException e) {
            e.printStackTrace();
        }
    }

    private  void stopKafka() {
        runner.stop();
    }
}
