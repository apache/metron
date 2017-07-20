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

import com.google.common.collect.ImmutableMap;
import org.apache.metron.hbase.mock.MockProvider;
import org.apache.metron.indexing.dao.InMemoryDao;
import org.apache.metron.indexing.dao.SearchIntegrationTest;
import org.apache.metron.rest.service.SearchService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TEST_PROFILE)
public class SearchControllerIntegrationTest extends DaoControllerTest {



  @Autowired
  private SearchService searchService;

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  private String searchUrl = "/api/v1/search";
  private String user = "user";
  private String password = "password";

  @BeforeClass
  public static void setupHbase() {
    MockProvider.addToCache("updates", "t");
  }

  @Before
  public void setup() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
    loadTestData();
  }

  @After
  public void cleanup() throws Exception {
    InMemoryDao.clear();
  }

  @Test
  public void testSecurity() throws Exception {
    this.mockMvc.perform(post(searchUrl + "/search").with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(SearchIntegrationTest.allQuery))
            .andExpect(status().isUnauthorized());
  }

  @Test
  public void test() throws Exception {

    this.mockMvc.perform(post(searchUrl + "/search").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(SearchIntegrationTest.allQuery))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.total").value(10))
            .andExpect(jsonPath("$.results[0].source.source:type").value("snort"))
            .andExpect(jsonPath("$.results[0].source.timestamp").value(10))
            .andExpect(jsonPath("$.results[1].source.source:type").value("snort"))
            .andExpect(jsonPath("$.results[1].source.timestamp").value(9))
            .andExpect(jsonPath("$.results[2].source.source:type").value("snort"))
            .andExpect(jsonPath("$.results[2].source.timestamp").value(8))
            .andExpect(jsonPath("$.results[3].source.source:type").value("snort"))
            .andExpect(jsonPath("$.results[3].source.timestamp").value(7))
            .andExpect(jsonPath("$.results[4].source.source:type").value("snort"))
            .andExpect(jsonPath("$.results[4].source.timestamp").value(6))
            .andExpect(jsonPath("$.results[5].source.source:type").value("bro"))
            .andExpect(jsonPath("$.results[5].source.timestamp").value(5))
            .andExpect(jsonPath("$.results[6].source.source:type").value("bro"))
            .andExpect(jsonPath("$.results[6].source.timestamp").value(4))
            .andExpect(jsonPath("$.results[7].source.source:type").value("bro"))
            .andExpect(jsonPath("$.results[7].source.timestamp").value(3))
            .andExpect(jsonPath("$.results[8].source.source:type").value("bro"))
            .andExpect(jsonPath("$.results[8].source.timestamp").value(2))
            .andExpect(jsonPath("$.results[9].source.source:type").value("bro"))
            .andExpect(jsonPath("$.results[9].source.timestamp").value(1));

    this.mockMvc.perform(post(searchUrl + "/search").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(SearchIntegrationTest.filterQuery))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.results[0].source.source:type").value("snort"))
            .andExpect(jsonPath("$.results[0].source.timestamp").value(9))
            .andExpect(jsonPath("$.results[1].source.source:type").value("snort"))
            .andExpect(jsonPath("$.results[1].source.timestamp").value(7))
            .andExpect(jsonPath("$.results[2].source.source:type").value("bro"))
            .andExpect(jsonPath("$.results[2].source.timestamp").value(1));


    this.mockMvc.perform(post(searchUrl + "/search").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(SearchIntegrationTest.sortQuery))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.total").value(10))
            .andExpect(jsonPath("$.results[0].source.ip_src_port").value(8001))
            .andExpect(jsonPath("$.results[1].source.ip_src_port").value(8002))
            .andExpect(jsonPath("$.results[2].source.ip_src_port").value(8003))
            .andExpect(jsonPath("$.results[3].source.ip_src_port").value(8004))
            .andExpect(jsonPath("$.results[4].source.ip_src_port").value(8005))
            .andExpect(jsonPath("$.results[5].source.ip_src_port").value(8006))
            .andExpect(jsonPath("$.results[6].source.ip_src_port").value(8007))
            .andExpect(jsonPath("$.results[7].source.ip_src_port").value(8008))
            .andExpect(jsonPath("$.results[8].source.ip_src_port").value(8009))
            .andExpect(jsonPath("$.results[9].source.ip_src_port").value(8010));

    this.mockMvc.perform(post(searchUrl + "/search").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(SearchIntegrationTest.paginationQuery))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.total").value(10))
            .andExpect(jsonPath("$.results[0].source.source:type").value("snort"))
            .andExpect(jsonPath("$.results[0].source.timestamp").value(6))
            .andExpect(jsonPath("$.results[1].source.source:type").value("bro"))
            .andExpect(jsonPath("$.results[1].source.timestamp").value(5))
            .andExpect(jsonPath("$.results[2].source.source:type").value("bro"))
            .andExpect(jsonPath("$.results[2].source.timestamp").value(4));

    this.mockMvc.perform(post(searchUrl + "/search").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(SearchIntegrationTest.indexQuery))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.total").value(5))
            .andExpect(jsonPath("$.results[0].source.source:type").value("bro"))
            .andExpect(jsonPath("$.results[0].source.timestamp").value(5))
            .andExpect(jsonPath("$.results[1].source.source:type").value("bro"))
            .andExpect(jsonPath("$.results[1].source.timestamp").value(4))
            .andExpect(jsonPath("$.results[2].source.source:type").value("bro"))
            .andExpect(jsonPath("$.results[2].source.timestamp").value(3))
            .andExpect(jsonPath("$.results[3].source.source:type").value("bro"))
            .andExpect(jsonPath("$.results[3].source.timestamp").value(2))
            .andExpect(jsonPath("$.results[4].source.source:type").value("bro"))
            .andExpect(jsonPath("$.results[4].source.timestamp").value(1));

    this.mockMvc.perform(post(searchUrl + "/search").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(SearchIntegrationTest.exceededMaxResultsQuery))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.responseCode").value(500))
            .andExpect(jsonPath("$.message").value("Search result size must be less than 100"));
  }
}
