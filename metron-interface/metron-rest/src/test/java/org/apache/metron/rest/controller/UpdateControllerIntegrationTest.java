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
import org.adrianwalker.multilinestring.Multiline;
import org.apache.curator.framework.CuratorFramework;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.hbase.mock.MockHTable;
import org.apache.metron.indexing.dao.HBaseDao;
import org.apache.metron.indexing.dao.SearchIntegrationTest;
import org.apache.metron.indexing.dao.update.CommentAddRemoveRequest;
import org.apache.metron.rest.service.UpdateService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.NavigableMap;

import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TEST_PROFILE)
public class UpdateControllerIntegrationTest extends DaoControllerTest {
  @Autowired
  private UpdateService updateService;
  @Autowired
  public CuratorFramework client;

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  private String updateUrl = "/api/v1/update";
  private String searchUrl = "/api/v1/search";
  private String user = "user";
  private String password = "password";
  private String metaAlertIndex = "metaalert_index";

  /**
   {
     "guid" : "bro_2",
     "sensorType" : "bro"
   }
   */
  @Multiline
  public static String findMessage0;

  /**
   {
     "guid" : "bro_2",
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
     "guid" : "bro_2",
     "sensorType" : "bro",
     "comment": "test_comment",
     "username" : "test_username",
     "timestamp":0
   }
   */
  @Multiline
  public static String addComment;

  /**
   {
   "guid" : "bro_2",
   "sensorType" : "bro",
   "comment": "test_comment",
   "username" : "test_username",
   "timestamp":0
   }
   */
  @Multiline
  public static String removeComment;

  @Before
  public void setup() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
    ImmutableMap<String, String> testData = ImmutableMap.of(
        "bro_index_2017.01.01.01", SearchIntegrationTest.broData,
        "snort_index_2017.01.01.01", SearchIntegrationTest.snortData,
        metaAlertIndex, MetaAlertControllerIntegrationTest.metaAlertData
    );
    loadTestData(testData);
  }

  @Test
  public void shouldPatchDocument() throws Exception {
    String guid = "bro_2";

    // request used to find the message
    MockHttpServletRequestBuilder findOneRequest = post(searchUrl + "/findOne")
            .with(httpBasic(user, password))
            .with(csrf())
            .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
            .content(findMessage0);

    // request used to patch the document
    MockHttpServletRequestBuilder patchRequest = patch(updateUrl + "/patch")
            .with(httpBasic(user, password))
            .with(csrf())
            .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
            .content(patch);

    // the document should exist, but without the 'project' field defined
    this.mockMvc.perform(findOneRequest)
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.source:type").value("bro"))
            .andExpect(jsonPath("$.guid").value(guid))
            .andExpect(jsonPath("$.project").doesNotExist())
            .andExpect(jsonPath("$.timestamp").value(2));

    // nothing is recorded in HBase
    MockHTable table = (MockHTable) MockHBaseTableProvider.getFromCache(TABLE);
    Assert.assertEquals(0,table.size());

    // patch the document
    this.mockMvc.perform(patchRequest)
            .andExpect(status().isOk());

    // the document should now have the 'project' field
    this.mockMvc.perform(findOneRequest)
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.source:type").value("bro"))
            .andExpect(jsonPath("$.guid").value(guid))
            .andExpect(jsonPath("$.project").value("metron"))
            .andExpect(jsonPath("$.timestamp").value(2));

    // the change should be recorded in HBase
    Assert.assertEquals(1,table.size());
    {
        //ensure hbase is up to date
        Get g = new Get(new HBaseDao.Key(guid,"bro").toBytes());
        Result r = table.get(g);
        NavigableMap<byte[], byte[]> columns = r.getFamilyMap(CF.getBytes());
        Assert.assertEquals(1, columns.size());
    }
  }

  @Test
  public void shouldAddComment() throws Exception {
    CommentAddRemoveRequest commentAddRemoveRequest = new CommentAddRemoveRequest();
    commentAddRemoveRequest.setGuid("bro_1");
    commentAddRemoveRequest.setSensorType("bro");
    commentAddRemoveRequest.setComment("test_comment");
    commentAddRemoveRequest.setUsername("test_username");
    commentAddRemoveRequest.setTimestamp(0L);

    updateService.addComment(commentAddRemoveRequest);

    ResultActions result = this.mockMvc.perform(
        post(updateUrl + "/add/comment")
            .with(httpBasic(user, password)).with(csrf())
            .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
            .content(addComment));
    result.andExpect(status().isOk());
  }

  @Test
  public void shouldRemoveComment() throws Exception {
    CommentAddRemoveRequest commentAddRemoveRequest = new CommentAddRemoveRequest();
    commentAddRemoveRequest.setGuid("bro_1");
    commentAddRemoveRequest.setSensorType("bro");
    commentAddRemoveRequest.setComment("test_comment");
    commentAddRemoveRequest.setUsername("test_username");
    commentAddRemoveRequest.setTimestamp(0L);

    updateService.removeComment(commentAddRemoveRequest);

    ResultActions result = this.mockMvc.perform(
        post(updateUrl + "/remove/comment")
            .with(httpBasic(user, password)).with(csrf())
            .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
            .content(removeComment));
    result.andExpect(status().isOk());
  }
}
