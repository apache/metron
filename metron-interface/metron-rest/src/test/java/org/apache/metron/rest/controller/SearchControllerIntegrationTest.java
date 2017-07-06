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
import org.apache.metron.rest.model.SearchRequest;
import org.apache.metron.rest.model.SearchResponse;
import org.apache.metron.rest.model.SortField;
import org.apache.metron.rest.model.SortOrder;
import org.apache.metron.rest.service.GlobalConfigService;
import org.apache.metron.rest.service.SearchService;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
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
public class SearchControllerIntegrationTest {

  /**
   * [
   * {"source:type": "bro", "ip_src_addr":"192.168.1.1", "ip_src_port": 8010, "timestamp":1, "rejected":true},
   * {"source:type": "bro" "ip_src_addr":"192.168.1.2", "ip_src_port": 8009, "timestamp":2, "rejected":false},
   * {"source:type": "bro" "ip_src_addr":"192.168.1.3", "ip_src_port": 8008, "timestamp":3, "rejected":true},
   * {"source:type": "bro" "ip_src_addr":"192.168.1.4", "ip_src_port": 8007, "timestamp":4, "rejected":false},
   * {"source:type": "bro" "ip_src_addr":"192.168.1.5", "ip_src_port": 8006, "timestamp":5, "rejected":true}
   * ]
   */
  @Multiline
  public static String broData;

  /**
   * [
   * {"source:type": "snort" "ip_src_addr":"192.168.1.6", "ip_src_port": 8005, "timestamp":6, "is_alert":false},
   * {"source:type": "snort" "ip_src_addr":"192.168.1.1", "ip_src_port": 8004, "timestamp":7, "is_alert":true},
   * {"source:type": "snort" "ip_src_addr":"192.168.1.7", "ip_src_port": 8003, "timestamp":8, "is_alert":false},
   * {"source:type": "snort" "ip_src_addr":"192.168.1.1", "ip_src_port": 8002, "timestamp":9, "is_alert":true},
   * {"source:type": "snort" "ip_src_addr":"192.168.1.8", "ip_src_port": 8001, "timestamp":10, "is_alert":false}
   * ]
   */
  @Multiline
  public static String snortData;

  /**
   * {
   * "indices": ["bro", "snort"],
   * "query": "*",
   * "from": 0,
   * "size": 10,
   * "sort": [
   *   {
   *     "field": "timestamp",
   *     "sortOrder": "desc"
   *   }
   * ]
   * }
   */
  @Multiline
  public static String allQuery;

  /**
   * {
   * "indices": ["bro", "snort"],
   * "query": "ip_src_addr:192.168.1.1",
   * "from": 0,
   * "size": 10,
   * "sort": [
   *   {
   *     "field": "timestamp",
   *     "sortOrder": "desc"
   *   }
   * ]
   * }
   */
  @Multiline
  public static String filterQuery;

  /**
   * {
   * "indices": ["bro", "snort"],
   * "query": "*",
   * "from": 0,
   * "size": 10,
   * "sort": [
   *   {
   *     "field": "ip_src_port",
   *     "sortOrder": "asc"
   *   }
   * ]
   * }
   */
  @Multiline
  public static String sortQuery;

  /**
   * {
   * "indices": ["bro", "snort"],
   * "query": "*",
   * "from": 4,
   * "size": 3,
   * "sort": [
   *   {
   *     "field": "timestamp",
   *     "sortOrder": "desc"
   *   }
   * ]
   * }
   */
  @Multiline
  public static String paginationQuery;

  /**
   * {
   * "indices": ["bro"],
   * "query": "*",
   * "from": 0,
   * "size": 10,
   * "sort": [
   *   {
   *     "field": "timestamp",
   *     "sortOrder": "desc"
   *   }
   * ]
   * }
   */
  @Multiline
  public static String indexQuery;


  @Autowired
  private TransportClient client;

  @Autowired
  private SearchService searchService;

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  private String searchUrl = "/api/v1/search";
  private String user = "user";
  private String password = "password";

  @Before
  public void setup() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
  }

  @Test
  public void testSecurity() throws Exception {
    this.mockMvc.perform(post(searchUrl + "/search").with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(allQuery))
            .andExpect(status().isUnauthorized());
  }

  @Test
  public void test() throws Exception {
    loadTestData();

    this.mockMvc.perform(post(searchUrl + "/search").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(allQuery))
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

    this.mockMvc.perform(post(searchUrl + "/search").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(filterQuery))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.results[0].source.source:type").value("snort"))
            .andExpect(jsonPath("$.results[0].source.timestamp").value(9))
            .andExpect(jsonPath("$.results[1].source.source:type").value("snort"))
            .andExpect(jsonPath("$.results[1].source.timestamp").value(7))
            .andExpect(jsonPath("$.results[2].source.source:type").value("bro"))
            .andExpect(jsonPath("$.results[2].source.timestamp").value(1));


    this.mockMvc.perform(post(searchUrl + "/search").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(sortQuery))
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

    this.mockMvc.perform(post(searchUrl + "/search").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(paginationQuery))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.total").value(10))
            .andExpect(jsonPath("$.results[0].source.source:type").value("snort"))
            .andExpect(jsonPath("$.results[0].source.timestamp").value(6))
            .andExpect(jsonPath("$.results[1].source.source:type").value("bro"))
            .andExpect(jsonPath("$.results[1].source.timestamp").value(5))
            .andExpect(jsonPath("$.results[2].source.source:type").value("bro"))
            .andExpect(jsonPath("$.results[2].source.timestamp").value(4));

    this.mockMvc.perform(post(searchUrl + "/search").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(indexQuery))
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
  }



  private void loadTestData() throws ParseException {
    BulkRequestBuilder bulkRequest = client.prepareBulk().setRefresh(true);
    JSONArray broArray = (JSONArray) new JSONParser().parse(broData);
    for(Object o: broArray) {
      JSONObject jsonObject = (JSONObject) o;
      IndexRequestBuilder indexRequestBuilder = client.prepareIndex("bro_index_2017.01.01.01", "bro_doc");
      indexRequestBuilder = indexRequestBuilder.setSource(jsonObject.toJSONString());
      indexRequestBuilder = indexRequestBuilder.setTimestamp(jsonObject.get("timestamp").toString());
      bulkRequest.add(indexRequestBuilder);
    }
    JSONArray snortArray = (JSONArray) new JSONParser().parse(snortData);
    for(Object o: snortArray) {
      JSONObject jsonObject = (JSONObject) o;
      IndexRequestBuilder indexRequestBuilder = client.prepareIndex("snort_index_2017.01.01.02", "snort_doc");
      indexRequestBuilder = indexRequestBuilder.setSource(jsonObject.toJSONString());
      indexRequestBuilder = indexRequestBuilder.setTimestamp(jsonObject.get("timestamp").toString());
      bulkRequest.add(indexRequestBuilder);
    }
    BulkResponse bulkResponse = bulkRequest.execute().actionGet();
    if (bulkResponse.hasFailures()) {
      throw new RuntimeException("Failed to index test data");
    }
  }
}
