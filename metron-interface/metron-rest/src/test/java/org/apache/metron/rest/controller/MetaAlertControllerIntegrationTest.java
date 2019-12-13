/*
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

import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.dao.InMemoryMetaAlertDao;
import org.apache.metron.indexing.dao.SearchIntegrationTest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertAddRemoveRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConstants;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.rest.service.MetaAlertService;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;

import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TEST_PROFILE)
public class MetaAlertControllerIntegrationTest extends DaoControllerTest {

  @Autowired
  private MetaAlertService metaAlertService;
  @Autowired
  public CuratorFramework client;

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  private String metaalertUrl = "/api/v1/metaalert";
  private String user = "user";
  private String password = "password";
  private String metaAlertIndex = "metaalert_index";

  /**
   {
   "alerts" : [
   {
   "guid": "bro_1",
   "sensorType": "bro",
   "index": "bro_index_2017.01.01.01"
   },
   {
   "guid": "snort_2",
   "sensorType": "snort",
   "index": "snort_index_2017.01.01.01"
   }
   ],
   "groups" : ["group_one", "group_two"]
   }
   */
  @Multiline
  public static String create;

  /**
   * [
   *{"guid":"meta_1","metron_alert":[{"guid":"bro_1", "source.type":"bro"}],"average":"5.0","min":"5.0","median":"5.0","max":"5.0","count":"1.0","sum":"5.0", "status":"active"},
   *{"guid":"meta_2","metron_alert":[{"guid":"bro_1", "source.type":"bro"},{"guid":"bro_2", "source.type":"bro"},{"guid":"snort_1", "source.type":"snort"}],"average":"5.0","min":"0.0","median":"5.0","max":"10.0","count":"3.0","sum":"15.0"}
   * ]
   */
  @Multiline
  public static String metaAlertData;

  @BeforeEach
  public void setup() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
    ImmutableMap<String, String> testData = ImmutableMap.of(
        "bro_index_2017.01.01.01", SearchIntegrationTest.broData,
        "snort_index_2017.01.01.01", SearchIntegrationTest.snortData,
        metaAlertIndex, metaAlertData
    );
    loadTestData(testData);
  }

  @AfterEach
  public void cleanup() {
    InMemoryMetaAlertDao.clear();
  }

  @Test
  public void test() throws Exception {
    // Testing searching by alert
    // Test no meta alert
    String guid = "missing_1";
    ResultActions result = this.mockMvc.perform(
        post(metaalertUrl + "/searchByAlert")
            .with(httpBasic(user, password)).with(csrf())
            .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
            .content(guid));
    result.andExpect(status().isOk())
        .andExpect(
            content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
        .andExpect(jsonPath("$.total").value(0));

    // Test single meta alert
    guid = "snort_1";
    result = this.mockMvc.perform(
        post(metaalertUrl + "/searchByAlert")
            .with(httpBasic(user, password)).with(csrf())
            .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
            .content(guid));
    result.andExpect(status().isOk())
        .andExpect(
            content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.results[0].source.guid").value("meta_2"))
        .andExpect(jsonPath("$.results[0].source.count").value(3.0));

    // Test multiple meta alerts
    guid = "bro_1";
    result = this.mockMvc.perform(
        post(metaalertUrl + "/searchByAlert")
            .with(httpBasic(user, password)).with(csrf())
            .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
            .content(guid));
    result.andExpect(status().isOk())
        .andExpect(
            content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.results[0].source.guid").value("meta_2"))
        .andExpect(jsonPath("$.results[0].source.count").value(3.0))
        .andExpect(jsonPath("$.results[1].source.guid").value("meta_1"))
        .andExpect(jsonPath("$.results[1].source.count").value(1.0));
  }

  @Test
  public void shouldCreateMetaAlert() throws Exception {
    ResultActions result = this.mockMvc.perform(
            post(metaalertUrl + "/create")
                    .with(httpBasic(user, password)).with(csrf())
                    .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
                    .content(create));
    result.andExpect(status().isOk())
            .andExpect(jsonPath("$.guid", notNullValue()))
            .andExpect(jsonPath("$.timestamp", greaterThan(0L)))
            .andExpect(jsonPath("$.sensorType").value(MetaAlertConstants.METAALERT_TYPE))
            .andExpect(jsonPath("$.document.timestamp", greaterThan(0L)))
            .andExpect(jsonPath("$.document['source.type']").value(MetaAlertConstants.METAALERT_TYPE))
            .andExpect(jsonPath("$.document.status").value("active"))
            .andExpect(jsonPath("$.document.groups[0]").value("group_one"))
            .andExpect(jsonPath("$.document.groups[1]").value("group_two"))
            .andExpect(jsonPath("$.document.metron_alert[0].guid").value("bro_1"))
            .andExpect(jsonPath("$.document.metron_alert[1].guid").value("snort_2"));
  }

  @Test
  public void shouldAddRemoveAlerts() throws Exception {
    MetaAlertAddRemoveRequest addRequest = new MetaAlertAddRemoveRequest();
    addRequest.setMetaAlertGuid("meta_1");
    addRequest.setAlerts(new ArrayList<GetRequest>() {{
      add(new GetRequest("bro_2", "bro", "bro_index_2017.01.01.01"));
      add(new GetRequest("bro_3", "bro", "bro_index_2017.01.01.01"));
    }});

    ResultActions result = this.mockMvc.perform(
        post(metaalertUrl + "/add/alert")
            .with(httpBasic(user, password)).with(csrf())
            .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
            .content(JSONUtils.INSTANCE.toJSON(addRequest, false)));
    result.andExpect(status().isOk())
            .andExpect(jsonPath("$.guid").value("meta_1"))
            .andExpect(jsonPath("$.sensorType").value(MetaAlertConstants.METAALERT_TYPE))
            .andExpect(jsonPath("$.document.metron_alert[0].guid").value("bro_1"))
            .andExpect(jsonPath("$.document.metron_alert[1].guid").value("bro_2"))
            .andExpect(jsonPath("$.document.metron_alert[2].metaalerts").value("meta_1"))
            .andExpect(jsonPath("$.document.metron_alert[2].guid").value("bro_3"))
            .andExpect(jsonPath("$.document.metron_alert[2].metaalerts").value("meta_1"));

    MetaAlertAddRemoveRequest addDuplicateRequest = new MetaAlertAddRemoveRequest();
    addDuplicateRequest.setMetaAlertGuid("meta_1");
    addDuplicateRequest.setAlerts(new ArrayList<GetRequest>() {{
      add(new GetRequest("bro_1", "bro"));
    }});

    result = this.mockMvc.perform(
        post(metaalertUrl + "/add/alert")
            .with(httpBasic(user, password)).with(csrf())
            .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
            .content(JSONUtils.INSTANCE.toJSON(addDuplicateRequest, false)));
    result.andExpect(status().isOk())
            .andExpect(jsonPath("$.guid").value("meta_1"))
            .andExpect(jsonPath("$.sensorType").value(MetaAlertConstants.METAALERT_TYPE))
            .andExpect(jsonPath("$.document.metron_alert[0].guid").value("bro_1"))
            .andExpect(jsonPath("$.document.metron_alert[1].guid").value("bro_2"))
            .andExpect(jsonPath("$.document.metron_alert[2].metaalerts").value("meta_1"))
            .andExpect(jsonPath("$.document.metron_alert[2].guid").value("bro_3"))
            .andExpect(jsonPath("$.document.metron_alert[2].metaalerts").value("meta_1"));

    MetaAlertAddRemoveRequest removeRequest = new MetaAlertAddRemoveRequest();
    removeRequest.setMetaAlertGuid("meta_1");
    removeRequest.setAlerts(new ArrayList<GetRequest>() {{
      add(new GetRequest("bro_2", "bro"));
      add(new GetRequest("bro_3", "bro"));
    }});

    result = this.mockMvc.perform(
        post(metaalertUrl + "/remove/alert")
            .with(httpBasic(user, password)).with(csrf())
            .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
            .content(JSONUtils.INSTANCE.toJSON(removeRequest, false)));
    result.andExpect(status().isOk())
            .andExpect(jsonPath("$.guid").value("meta_1"))
            .andExpect(jsonPath("$.sensorType").value(MetaAlertConstants.METAALERT_TYPE))
            .andExpect(jsonPath("$.document.metron_alert.*", hasSize(equalTo(1))))
            .andExpect(jsonPath("$.document.metron_alert[0].guid").value("bro_1"));

    MetaAlertAddRemoveRequest removeMissingRequest = new MetaAlertAddRemoveRequest();
    removeMissingRequest.setMetaAlertGuid("meta_1");
    removeMissingRequest.setAlerts(new ArrayList<GetRequest>() {{
      add(new GetRequest("bro_2", "bro"));
    }});

    result = this.mockMvc.perform(
        post(metaalertUrl + "/remove/alert")
            .with(httpBasic(user, password)).with(csrf())
            .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
            .content(JSONUtils.INSTANCE.toJSON(removeMissingRequest, false)));
    result.andExpect(status().isOk())
            .andExpect(jsonPath("$.guid").value("meta_1"))
            .andExpect(jsonPath("$.sensorType").value(MetaAlertConstants.METAALERT_TYPE))
            .andExpect(jsonPath("$.document.metron_alert.*", hasSize(equalTo(1))))
            .andExpect(jsonPath("$.document.metron_alert[0].guid").value("bro_1"));

    MetaAlertAddRemoveRequest emptyMetaAlertRequest = new MetaAlertAddRemoveRequest();
    emptyMetaAlertRequest.setMetaAlertGuid("meta_1");
    emptyMetaAlertRequest.setAlerts(new ArrayList<GetRequest>() {{
      add(new GetRequest("bro_1", "bro"));
    }});

    result = this.mockMvc.perform(
            post(metaalertUrl + "/remove/alert")
                    .with(httpBasic(user, password)).with(csrf())
                    .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
                    .content(JSONUtils.INSTANCE.toJSON(emptyMetaAlertRequest, false)));
    result.andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Removing these alerts will result in an empty meta alert.  Empty meta alerts are not allowed."))
            .andExpect(jsonPath("$.fullMessage").value("IllegalStateException: Removing these alerts will result in an empty meta alert.  Empty meta alerts are not allowed."));
  }

  @Test
  public void shouldUpdateStatus() throws Exception {
    ResultActions result = this.mockMvc.perform(
        post(metaalertUrl + "/update/status/meta_2/inactive")
            .with(httpBasic(user, password)).with(csrf())
            .contentType(MediaType.parseMediaType("application/json;charset=UTF-8")));
    result.andExpect(status().isOk())
            .andExpect(jsonPath("$.guid").value("meta_2"))
            .andExpect(jsonPath("$.sensorType").value(MetaAlertConstants.METAALERT_TYPE))
            .andExpect(jsonPath("$.document.status").value("inactive"));

    result = this.mockMvc.perform(
        post(metaalertUrl + "/update/status/meta_2/active")
            .with(httpBasic(user, password)).with(csrf())
            .contentType(MediaType.parseMediaType("application/json;charset=UTF-8")));
    result.andExpect(status().isOk())
            .andExpect(jsonPath("$.guid").value("meta_2"))
            .andExpect(jsonPath("$.sensorType").value(MetaAlertConstants.METAALERT_TYPE))
            .andExpect(jsonPath("$.document.status").value("active"));
  }

}
