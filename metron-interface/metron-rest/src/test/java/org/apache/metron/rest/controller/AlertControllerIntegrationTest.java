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
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.UnableToStartException;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.rest.model.AlertProfile;
import org.apache.metron.rest.service.AlertService;
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

    // A bug in Spring and/or Kafka forced us to move into a component that is spun up and down per test-case
    // Given the large spinup time of components, please avoid this pattern until we upgrade Spring.
    // See: https://issues.apache.org/jira/browse/METRON-1009
    @Autowired
    private KafkaComponent kafkaWithZKComponent;
    private ComponentRunner runner;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private AlertService alertService;

    private MockMvc mockMvc;

    private String alertUrl = "/api/v1/alert";
    private String user1 = "user1";
    private String user2 = "user2";
    private String admin = "admin";
    private String password = "password";

    @Before
    public void setup() throws Exception {
        for (AlertProfile alertsProfile : alertService.findAllProfiles()) {
          alertService.deleteProfile(alertsProfile.getId());
        }
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
    }

    @Test
    public void testSecurity() throws Exception {
        this.mockMvc.perform(post(alertUrl + "/escalate").with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(alerts))
                .andExpect(status().isUnauthorized());
        this.mockMvc.perform(get(alertUrl + "/profile"))
            .andExpect(status().isUnauthorized());
        this.mockMvc.perform(get(alertUrl + "/profile/all"))
            .andExpect(status().isUnauthorized());
        this.mockMvc.perform(post(alertUrl + "/profile").with(csrf())
            .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
            .content(user1ProfileJson))
            .andExpect(status().isUnauthorized());
        this.mockMvc.perform(get(alertUrl + "/profile/all").with(httpBasic(user1, password)).with(csrf()))
            .andExpect(status().isForbidden());
        this.mockMvc.perform(delete(alertUrl + "/profile/user1").with(httpBasic(user1, password)).with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    public void escalateShouldEscalateAlerts() throws Exception {
        startKafka();
        this.mockMvc.perform(post(alertUrl + "/escalate").with(httpBasic(user1,password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(alerts))
                .andExpect(status().isOk());
        stopKafka();
    }

    @Test
    public void testAlertProfiles() throws Exception {
        emptyProfileShouldReturnNotFound();
        alertsProfilesShouldBeCreatedOrUpdated();
        alertsProfilesShouldBeProperlyDeleted();
    }

    /** Ensures a 404 is returned when an alerts profile cannot be found.  In the case of an admin getting
     * all profiles, an empty list should be returned.  This tests depends on the alertsProfileRepository
     * being empty.
     *
     * @throws Exception
     */
    private void emptyProfileShouldReturnNotFound() throws Exception {

      // user1 should get a 404 because an alerts profile has not been created
      this.mockMvc.perform(get(alertUrl + "/profile").with(httpBasic(user1, password)))
          .andExpect(status().isNotFound());

      // user2 should get a 404 because an alerts profile has not been created
      this.mockMvc.perform(get(alertUrl + "/profile").with(httpBasic(user2, password)))
          .andExpect(status().isNotFound());

      // getting all alerts profiles should return an empty list
      this.mockMvc.perform(get(alertUrl + "/profile/all").with(httpBasic(admin, password)))
          .andExpect(status().isOk())
          .andExpect(
              content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
          .andExpect(jsonPath("$.*", hasSize(0)));
    }

    /** Ensures users can update their profiles independently of other users.  When user1 updates an
     * alerts profile, alerts profile for user2 should not be affected.  Tests that an initial update
     * returns a 201 status and subsequent updates return 200 statuses.  A call to get all alerts profiles
     * by an admin user should also work properly.  This tests depends on the alertsProfileRepository
     * being empty initially.
     *
     * @throws Exception
     */
    private void alertsProfilesShouldBeCreatedOrUpdated() throws Exception {

      // user1 creates their alerts profile
      this.mockMvc.perform(post(alertUrl + "/profile").with(httpBasic(user1, password)).with(csrf())
          .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
          .content(user1ProfileJson))
          .andExpect(status().isCreated())
          .andExpect(content().json(user1ProfileJson));

      // user1 updates their alerts profile
      this.mockMvc.perform(post(alertUrl + "/profile").with(httpBasic(user1, password)).with(csrf())
          .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
          .content(user1ProfileJson))
          .andExpect(status().isOk())
          .andExpect(content().json(user1ProfileJson));

      // user1 gets their alerts profile
      this.mockMvc.perform(get(alertUrl + "/profile").with(httpBasic(user1, password)))
          .andExpect(status().isOk())
          .andExpect(
              content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
          .andExpect(content().json(user1ProfileJson));

      // user2 alerts profile should still be empty
      this.mockMvc.perform(get(alertUrl + "/profile").with(httpBasic(user2, password)))
          .andExpect(status().isNotFound());

      // getting all alerts profiles should only return user1's
      this.mockMvc.perform(get(alertUrl + "/profile/all").with(httpBasic(admin, password)))
          .andExpect(status().isOk())
          .andExpect(
              content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
          .andExpect(content().json("[" + user1ProfileJson + "]"));

      // user2 creates their alerts profile
      this.mockMvc.perform(post(alertUrl + "/profile").with(httpBasic(user2, password)).with(csrf())
          .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
          .content(user2ProfileJson))
          .andExpect(status().isCreated())
          .andExpect(content().json(user2ProfileJson));

      // user2 updates their alerts profile
      this.mockMvc.perform(get(alertUrl + "/profile").with(httpBasic(user1, password)))
          .andExpect(status().isOk())
          .andExpect(
              content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
          .andExpect(content().json(user1ProfileJson));

      // user2 gets their alerts profile
      this.mockMvc.perform(get(alertUrl + "/profile").with(httpBasic(user2, password)))
          .andExpect(status().isOk())
          .andExpect(
              content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
          .andExpect(content().json(user2ProfileJson));

      // getting all alerts profiles should return both
      this.mockMvc.perform(get(alertUrl + "/profile/all").with(httpBasic(admin, password)))
          .andExpect(status().isOk())
          .andExpect(
              content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
          .andExpect(content().json("[" + user1ProfileJson + "," + user2ProfileJson + "]"));
    }

    /** Ensures users can delete their profiles independently of other users.  When user1 deletes an
     * alerts profile, alerts profile for user2 should not be deleted.  This tests depends on alerts
     * profiles existing for user1 and user2.
     *
     * @throws Exception
     */
    private void alertsProfilesShouldBeProperlyDeleted() throws Exception {

      // user1 deletes their profile
      this.mockMvc.perform(delete(alertUrl + "/profile/user1").with(httpBasic(admin, password)))
          .andExpect(status().isOk());

      // user1 should get a 404 when trying to delete an alerts profile that doesn't exist
      this.mockMvc.perform(delete(alertUrl + "/profile/user1").with(httpBasic(admin, password)))
          .andExpect(status().isNotFound());

      // user1 should get a 404 when trying to retrieve their alerts profile
      this.mockMvc.perform(get(alertUrl + "/profile").with(httpBasic(user1, password)))
          .andExpect(status().isNotFound());

      // user2's alerts profile should still exist
      this.mockMvc.perform(get(alertUrl + "/profile").with(httpBasic(user2, password)))
          .andExpect(status().isOk())
          .andExpect(
              content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
          .andExpect(content().json(user2ProfileJson));

      // getting all alerts profiles should only return user2's
      this.mockMvc.perform(get(alertUrl + "/profile/all").with(httpBasic(admin, password)))
          .andExpect(status().isOk())
          .andExpect(
              content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
          .andExpect(content().json("[" + user2ProfileJson + "]"));

      // user2 deletes their profile
      this.mockMvc.perform(delete(alertUrl + "/profile/user2").with(httpBasic(admin, password)))
          .andExpect(status().isOk());

      // user2 should get a 404 when trying to delete an alerts profile that doesn't exist
      this.mockMvc.perform(get(alertUrl + "/profile").with(httpBasic(user1, password)))
          .andExpect(status().isNotFound());

      // user2 should get a 404 when trying to retrieve their alerts profile
      this.mockMvc.perform(get(alertUrl + "/profile").with(httpBasic(user2, password)))
          .andExpect(status().isNotFound());

      // getting all alerts profiles should return an empty list
      this.mockMvc.perform(get(alertUrl + "/profile/all").with(httpBasic(admin, password)))
          .andExpect(status().isOk())
          .andExpect(
              content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
          .andExpect(jsonPath("$.*", hasSize(0)));
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
