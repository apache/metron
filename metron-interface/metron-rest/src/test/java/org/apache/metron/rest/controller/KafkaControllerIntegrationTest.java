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

import kafka.common.TopicAlreadyMarkedForDeletionException;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.UnableToStartException;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.rest.service.KafkaService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.NestedServletException;

import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TEST_PROFILE)
public class KafkaControllerIntegrationTest {

  private static final int KAFKA_RETRY = 10;
  // A bug in Spring and/or Kafka forced us to move into a component that is spun up and down per test-case
  // Given the large spinup time of components, please avoid this pattern until we upgrade Spring.
  // See: https://issues.apache.org/jira/browse/METRON-1009
  @Autowired
  private KafkaComponent kafkaWithZKComponent;
  private ComponentRunner runner;


  interface Evaluation {
    void tryTest() throws Exception;
  }

  private void testAndRetry(Evaluation evaluation) throws Exception{
    testAndRetry(KAFKA_RETRY, evaluation);
  }

  private void testAndRetry(int numRetries, Evaluation evaluation) throws Exception {
    AssertionError lastError = null;
    for(int i = 0;i < numRetries;++i) {
      try {
        evaluation.tryTest();
        return;
      }
      catch(AssertionError error) {
        if(error.getMessage().contains("but was:<404>")) {
          lastError = error;
          Thread.sleep(1000);
          continue;
        }
        else {
          throw error;
        }
      }
    }
    if(lastError != null) {
      throw lastError;
    }
  }

  /**
   {
   "name": "bro",
   "numPartitions": 1,
   "properties": {},
   "replicationFactor": 1
   }
   */
  @Multiline
  public static String broTopic;

  /**
   * {
   *   "type":"message1"
   * }
   */
  @Multiline
  public static String message1;

  /**
   * {
   *   "type":"message2"
   * }
   */
  @Multiline
  public static String message2;

  /**
   * {
   *   "type":"message3"
   * }
   */
  @Multiline
  public static String message3;

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private KafkaService kafkaService;

  private MockMvc mockMvc;

  private String kafkaUrl = "/api/v1/kafka";
  private String user = "user";
  private String password = "password";

  @BeforeEach
  public void setup() throws Exception {
    runner = new ComponentRunner.Builder()
            .withComponent("kafka", kafkaWithZKComponent)
            .withCustomShutdownOrder(new String[]{"kafka"})
            .build();
    try {
      runner.start();
    } catch (UnableToStartException e) {
      e.printStackTrace();
    }
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
  }

  @Test
  public void testSecurity() throws Exception {
    this.mockMvc.perform(post(kafkaUrl + "/topic").with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(broTopic))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(kafkaUrl + "/topic/bro"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(kafkaUrl + "/topic"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(kafkaUrl + "/topic/bro/sample"))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(get(kafkaUrl + "/topic/bro/produce"))
        .andExpect(status().isUnauthorized());

    this.mockMvc.perform(delete(kafkaUrl + "/topic/bro").with(csrf()))
            .andExpect(status().isUnauthorized());
  }

  @Test
  public void test() throws Exception {
    this.kafkaService.deleteTopic("bro");
    this.kafkaService.deleteTopic("someTopic");
    Thread.sleep(1000);
    testAndRetry(() -> this.mockMvc.perform(delete(kafkaUrl + "/topic/bro").with(httpBasic(user,password)).with(csrf()))
            .andExpect(status().isNotFound())
    );

    testAndRetry(() ->
    this.mockMvc.perform(post(kafkaUrl + "/topic").with(httpBasic(user,password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(broTopic))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.name").value("bro"))
            .andExpect(jsonPath("$.numPartitions").value(1))
            .andExpect(jsonPath("$.replicationFactor").value(1))
    );

    Thread.sleep(1000);
    testAndRetry(() ->
    this.mockMvc.perform(get(kafkaUrl + "/topic/bro").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.name").value("bro"))
            .andExpect(jsonPath("$.numPartitions").value(1))
            .andExpect(jsonPath("$.replicationFactor").value(1))
    );


    this.mockMvc.perform(get(kafkaUrl + "/topic/someTopic").with(httpBasic(user,password)))
            .andExpect(status().isNotFound());

    testAndRetry(() ->
    this.mockMvc.perform(get(kafkaUrl + "/topic").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$", Matchers.hasItem("bro")))
    );

    testAndRetry(() ->
        this.mockMvc.perform(post(kafkaUrl + "/topic/bro/produce").with(httpBasic(user,password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(message1))
            .andExpect(status().isOk())
    );
    testAndRetry(() ->
        this.mockMvc.perform(get(kafkaUrl + "/topic/bro/sample").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("text/plain;charset=UTF-8")))
            .andExpect(jsonPath("$.type").value("message1"))
    );

    testAndRetry(() ->
        this.mockMvc.perform(post(kafkaUrl + "/topic/bro/produce").with(httpBasic(user,password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(message2))
            .andExpect(status().isOk())
    );
    testAndRetry(() ->
        this.mockMvc.perform(get(kafkaUrl + "/topic/bro/sample").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("text/plain;charset=UTF-8")))
            .andExpect(jsonPath("$.type").value("message2"))
    );

    testAndRetry(() ->
        this.mockMvc.perform(post(kafkaUrl + "/topic/bro/produce").with(httpBasic(user,password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(message3))
            .andExpect(status().isOk())
    );
    testAndRetry(() ->
        this.mockMvc.perform(get(kafkaUrl + "/topic/bro/sample").with(httpBasic(user,password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("text/plain;charset=UTF-8")))
            .andExpect(jsonPath("$.type").value("message3"))
    );

    this.mockMvc.perform(get(kafkaUrl + "/topic/someTopic/sample").with(httpBasic(user,password)))
            .andExpect(status().isNotFound());

    boolean deleted = false;
    for(int i = 0;i < KAFKA_RETRY;++i) {
      try {
        MvcResult result = this.mockMvc.perform(delete(kafkaUrl + "/topic/bro").with(httpBasic(user, password)).with(csrf())).andReturn();
        if(result.getResponse().getStatus() == 200) {
          deleted = true;
          break;
        }
        Thread.sleep(1000);
      }
      catch(NestedServletException nse) {
        Throwable t = nse.getRootCause();
        if(t instanceof TopicAlreadyMarkedForDeletionException) {
          continue;
        }
        else {
          throw nse;
        }
      }
      catch(Throwable t) {
        throw t;
      }
    }
    if(!deleted) {
      throw new IllegalStateException("Unable to delete kafka topic \"bro\"");
    }
  }

  @AfterEach
  public void tearDown() {
    runner.stop();
  }
}
