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
import org.apache.curator.framework.CuratorFramework;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.metron.hbase.mock.MockHTable;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.rest.service.UpdateService;
import org.junit.Assert;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.NavigableMap;

import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TEST_PROFILE)
public class UpdateControllerIntegrationTest extends DaoControllerTest {
  @Autowired
  private UpdateService searchService;
  @Autowired
  public CuratorFramework client;

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  private String updateUrl = "/api/v1/update";
  private String searchUrl = "/api/v1/search";
  private String user = "user";
  private String password = "password";

  /**
   {
     "guid" : "bro_index_2017.01.01.01:1",
     "sensorType" : "bro"
   }
   */
  @Multiline
  public static String findMessage0;

  /**
   {
     "guid" : "bro_index_2017.01.01.01:1",
     "sensorType" : "bro",
     "patch" : [
      {
                "op": "add"
               , "path": "/project"
               , "value": "metron"
      }
              ]
   }
   */
  @Multiline
  public static String patch;

  /**
   {
     "guid" : "bro_index_2017.01.01.01:1",
     "sensorType" : "bro",
     "replacement" : {
       "source:type": "bro",
       "guid" : "bro_index_2017.01.01.01:1",
       "ip_src_addr":"192.168.1.2",
       "ip_src_port": 8009,
       "timestamp":200,
       "rejected":false
      }
   }
   */
  @Multiline
  public static String replace;


  @BeforeClass
  public static void setupHbase() {
    MockHBaseTableProvider.addToCache(TABLE, CF);
  }

  @Before
  public void setup() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
    loadTestData();
  }

  @Test
  public void test() throws Exception {
    String guid = "bro_index_2017.01.01.01:1";
    ResultActions result =   this.mockMvc.perform(post(searchUrl + "/findOne").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(findMessage0));
    try {
     result.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.source:type").value("bro"))
            .andExpect(jsonPath("$.guid").value(guid))
            .andExpect(jsonPath("$.project").doesNotExist())
            .andExpect(jsonPath("$.timestamp").value(2))
      ;
    }
    catch(Throwable t) {
      System.err.println(result.andReturn().getResponse().getContentAsString());
      throw t;
    }
    MockHTable table = (MockHTable) MockHBaseTableProvider.getFromCache(TABLE);
    Assert.assertEquals(0,table.size());
    this.mockMvc.perform(patch(updateUrl+ "/patch").with(httpBasic(user, password))
                                                   .with(csrf())
                                                   .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
                                                   .content(patch)
                        )
            .andExpect(status().isOk());
    this.mockMvc.perform(post(searchUrl + "/findOne").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(findMessage0))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.source:type").value("bro"))
            .andExpect(jsonPath("$.guid").value(guid))
            .andExpect(jsonPath("$.project").value("metron"))
            .andExpect(jsonPath("$.timestamp").value(2))
            ;
    Assert.assertEquals(1,table.size());
    {
        //ensure hbase is up to date
        Get g = new Get(guid.getBytes());
        Result r = table.get(g);
        NavigableMap<byte[], byte[]> columns = r.getFamilyMap(CF.getBytes());
        Assert.assertEquals(1, columns.size());
    }
    this.mockMvc.perform(post(updateUrl+ "/replace").with(httpBasic(user, password))
                                                   .with(csrf())
                                                   .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
                                                   .content(replace)
                        )
    .andExpect(status().isOk());
    this.mockMvc.perform(post(searchUrl + "/findOne").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(findMessage0))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.source:type").value("bro"))
            .andExpect(jsonPath("$.guid").value(guid))
            .andExpect(jsonPath("$.project").doesNotExist())
            .andExpect(jsonPath("$.timestamp").value(200))
            ;
    Assert.assertEquals(1,table.size());
    {
        //ensure hbase is up to date
        Get g = new Get(guid.getBytes());
        Result r = table.get(g);
        NavigableMap<byte[], byte[]> columns = r.getFamilyMap(CF.getBytes());
        Assert.assertEquals(2, columns.size());
    }
  }

}
