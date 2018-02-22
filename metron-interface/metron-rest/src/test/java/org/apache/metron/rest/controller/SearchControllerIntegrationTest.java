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

import static org.apache.metron.integration.utils.TestUtils.assertEventually;
import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.indexing.dao.InMemoryDao;
import org.apache.metron.indexing.dao.SearchIntegrationTest;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.rest.service.AlertsUIService;
import org.apache.metron.rest.service.SensorIndexingConfigService;
import org.json.simple.parser.ParseException;
import org.junit.After;
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TEST_PROFILE)
public class SearchControllerIntegrationTest extends DaoControllerTest {

  /**
   * {
   * "indices": [],
   * "query": "*",
   * "from": 0,
   * "size": 10,
   * "sort": [
   *   {
   *     "field": "timestamp",
   *     "sortOrder": "desc"
   *   }
   * ],
   * "facetFields": []
   * }
   */
  @Multiline
  public static String defaultQuery;

  /**
   * {
   *   "facetFields": ["ip_src_port"]
   * }
   */
  @Multiline
  public static String alertProfile;

  @Autowired
  private SensorIndexingConfigService sensorIndexingConfigService;

  @Autowired
  private AlertsUIService alertsUIService;

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  private String searchUrl = "/api/v1/search";
  private String user = "user";
  private String password = "password";

  @Before
  public void setup() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
    ImmutableMap<String, String> testData = ImmutableMap.of(
            "bro_index_2017.01.01.01", SearchIntegrationTest.broData,
            "snort_index_2017.01.01.01", SearchIntegrationTest.snortData
    );
    loadTestData(testData);
    loadColumnTypes();
    loadFacetCounts();
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
  public void testSearchWithDefaults() throws Exception {
    sensorIndexingConfigService.save("bro", new HashMap<String, Object>() {{
      put("index", "bro");
    }});

    assertEventually(() -> this.mockMvc.perform(post(searchUrl + "/search").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(defaultQuery))
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
            .andExpect(jsonPath("$.results[4].source.timestamp").value(1))
            .andExpect(jsonPath("$.facetCounts.*", hasSize(1)))
            .andExpect(jsonPath("$.facetCounts.ip_src_addr.*", hasSize(2)))
            .andExpect(jsonPath("$.facetCounts.ip_src_addr['192.168.1.1']").value(3))
            .andExpect(jsonPath("$.facetCounts.ip_src_addr['192.168.1.2']").value(1))
    );

    sensorIndexingConfigService.delete("bro");
  }

  @Test
  public void testSearchWithAlertProfileFacetFields() throws Exception {
    assertEventually(() -> this.mockMvc.perform(
        post("/api/v1/alerts/ui/settings").with(httpBasic(user, password)).with(csrf())
            .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
            .content(alertProfile))
        .andExpect(status().isOk())
    );

    assertEventually(() -> this.mockMvc.perform(
        post(searchUrl + "/search").with(httpBasic(user, password)).with(csrf())
            .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
            .content(defaultQuery))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
        .andExpect(jsonPath("$.facetCounts.*", hasSize(1)))
        .andExpect(jsonPath("$.facetCounts.ip_src_port.*", hasSize(2)))
        .andExpect(jsonPath("$.facetCounts.ip_src_port['8010']").value(1))
        .andExpect(jsonPath("$.facetCounts.ip_src_port['8009']").value(2))
    );

    alertsUIService.deleteAlertsUIUserSettings(user);
  }

  @Test
  public void testColumnMetadataUsingDefaultIndices() throws Exception {
    // Setup the default indices of bro and snort
    sensorIndexingConfigService.save("bro", new HashMap<String, Object>() {{
      put("index", "bro");
    }});
    sensorIndexingConfigService.save("snort", new HashMap<String, Object>() {{
      put("index", "snort");
    }});

    // Pass in an empty list to trigger using default indices
    assertEventually(() -> this.mockMvc.perform(post(searchUrl + "/column/metadata").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content("[]"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
        .andExpect(jsonPath("$.*", hasSize(5)))
        .andExpect(jsonPath("$.common_string_field").value("text"))
        .andExpect(jsonPath("$.common_integer_field").value("integer"))
        .andExpect(jsonPath("$.bro_field").value("boolean"))
        .andExpect(jsonPath("$.snort_field").value("double"))
        .andExpect(jsonPath("$.duplicate_field").value("other"))
    );

    sensorIndexingConfigService.delete("bro");
    sensorIndexingConfigService.delete("snort");
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

    this.mockMvc.perform(post(searchUrl + "/group").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(SearchIntegrationTest.groupByQuery))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.*", hasSize(2)))
            .andExpect(jsonPath("$.groupedBy").value("is_alert"))
            .andExpect(jsonPath("$.groupResults.*", hasSize(1)))
            .andExpect(jsonPath("$.groupResults[0].*", hasSize(5)))
            .andExpect(jsonPath("$.groupResults[0].key").value("is_alert_value"))
            .andExpect(jsonPath("$.groupResults[0].total").value(10))
            .andExpect(jsonPath("$.groupResults[0].groupedBy").value("latitude"))
            .andExpect(jsonPath("$.groupResults[0].groupResults.*", hasSize(1)))
            .andExpect(jsonPath("$.groupResults[0].groupResults[0].*", hasSize(3)))
            .andExpect(jsonPath("$.groupResults[0].groupResults[0].key").value("latitude_value"))
            .andExpect(jsonPath("$.groupResults[0].groupResults[0].total").value(10))
            .andExpect(jsonPath("$.groupResults[0].groupResults[0].score").value(50));

    this.mockMvc.perform(post(searchUrl + "/column/metadata").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content("[\"bro\",\"snort\"]"))

        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
        .andExpect(jsonPath("$.*", hasSize(5)))
        .andExpect(jsonPath("$.common_string_field").value("text"))
        .andExpect(jsonPath("$.common_integer_field").value("integer"))
        .andExpect(jsonPath("$.bro_field").value("boolean"))
        .andExpect(jsonPath("$.snort_field").value("double"))
        .andExpect(jsonPath("$.duplicate_field").value("other"));

    this.mockMvc.perform(post(searchUrl + "/column/metadata").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content("[\"bro\"]"))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
          .andExpect(jsonPath("$.*", hasSize(4)))
          .andExpect(jsonPath("$.common_string_field").value("text"))
          .andExpect(jsonPath("$.common_integer_field").value("integer"))
          .andExpect(jsonPath("$.bro_field").value("boolean"))
          .andExpect(jsonPath("$.duplicate_field").value("date"));

    this.mockMvc.perform(post(searchUrl + "/column/metadata").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content("[\"snort\"]"))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
          .andExpect(jsonPath("$.*", hasSize(4)))
          .andExpect(jsonPath("$.common_string_field").value("text"))
          .andExpect(jsonPath("$.common_integer_field").value("integer"))
          .andExpect(jsonPath("$.snort_field").value("double"))
          .andExpect(jsonPath("$.duplicate_field").value("long"));

    this.mockMvc.perform(post(searchUrl + "/column/metadata").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content("[\"someindex\"]"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
        .andExpect(jsonPath("$.*", hasSize(0)));
  }




  private void loadColumnTypes() throws ParseException {
    Map<String, Map<String, FieldType>> columnTypes = new HashMap<>();
    Map<String, FieldType> broTypes = new HashMap<>();
    broTypes.put("common_string_field", FieldType.TEXT);
    broTypes.put("common_integer_field", FieldType.INTEGER);
    broTypes.put("bro_field", FieldType.BOOLEAN);
    broTypes.put("duplicate_field", FieldType.DATE);
    Map<String, FieldType> snortTypes = new HashMap<>();
    snortTypes.put("common_string_field", FieldType.TEXT);
    snortTypes.put("common_integer_field", FieldType.INTEGER);
    snortTypes.put("snort_field", FieldType.DOUBLE);
    snortTypes.put("duplicate_field", FieldType.LONG);
    columnTypes.put("bro", broTypes);
    columnTypes.put("snort", snortTypes);
    InMemoryDao.setColumnMetadata(columnTypes);
  }

  private void loadFacetCounts() {
    Map<String, Map<String, Long>> facetCounts = new HashMap<>();
    Map<String, Long> ipSrcAddrCounts = new HashMap<>();
    ipSrcAddrCounts.put("192.168.1.1", 3L);
    ipSrcAddrCounts.put("192.168.1.2", 1L);
    Map<String, Long> ipSrcPortCounts = new HashMap<>();
    ipSrcPortCounts.put("8010", 1L);
    ipSrcPortCounts.put("8009", 2L);
    facetCounts.put("ip_src_addr", ipSrcAddrCounts);
    facetCounts.put("ip_src_port", ipSrcPortCounts);
    InMemoryDao.setFacetCounts(facetCounts);
  }

}