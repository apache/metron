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

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.filter.fixed.FixedPcapFilter;
import org.apache.metron.rest.mock.MockPcapJob;
import org.apache.metron.rest.model.PcapResponse;
import org.apache.metron.rest.service.PcapService;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TEST_PROFILE)
public class PcapControllerIntegrationTest {

  /**
   {
   "basePath": "/apps/metron/pcap",
   "baseOutputPath": "/tmp",
   "endTime": 10,
   "includeReverse": "true",
   "ipDstAddr": "192.168.1.1",
   "ipDstPort": "1000",
   "ipSrcAddr": "192.168.1.2",
   "ipSrcPort": "2000",
   "numReducers": 2,
   "packetFilter": "filter",
   "protocol": "TCP",
   "startTime": 1
   }
   */
  @Multiline
  public static String fixedJson;

  @Autowired
  private PcapService pcapService;

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  private String pcapUrl = "/api/v1/pcap";
  private String user = "user";
  private String password = "password";

  @Before
  public void setup() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
  }

  @Test
  public void testSecurity() throws Exception {
    this.mockMvc.perform(post(pcapUrl + "/fixed").with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(fixedJson))
            .andExpect(status().isUnauthorized());
  }

  @Test
  public void testFixed() throws Exception {
    MockPcapJob mockPcapJob = (MockPcapJob) wac.getBean("mockPcapJob");
    List<byte[]> results = Arrays.asList("pcap1".getBytes(), "pcap2".getBytes());
    mockPcapJob.setResults(results);

    PcapResponse expectedReponse = new PcapResponse();
    expectedReponse.setPcaps(results);
    this.mockMvc.perform(post(pcapUrl + "/fixed").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(fixedJson))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(content().json(JSONUtils.INSTANCE.toJSON(expectedReponse, false)));

    Assert.assertEquals("/apps/metron/pcap", mockPcapJob.getBasePath());
    Assert.assertEquals("/tmp", mockPcapJob.getBaseOutputPath());
    Assert.assertEquals(1, mockPcapJob.getStartTime());
    Assert.assertEquals(10, mockPcapJob.getEndTime());
    Assert.assertEquals(2, mockPcapJob.getNumReducers());
    Assert.assertTrue(mockPcapJob.getFilterImpl() instanceof FixedPcapFilter.Configurator);
    Map<String, String> actualFixedFields = mockPcapJob.getFixedFields();
    Assert.assertEquals("192.168.1.2", actualFixedFields.get(Constants.Fields.SRC_ADDR.getName()));
    Assert.assertEquals("2000", actualFixedFields.get(Constants.Fields.SRC_PORT.getName()));
    Assert.assertEquals("192.168.1.1", actualFixedFields.get(Constants.Fields.DST_ADDR.getName()));
    Assert.assertEquals("1000", actualFixedFields.get(Constants.Fields.DST_PORT.getName()));
    Assert.assertEquals("true", actualFixedFields.get(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName()));
    Assert.assertEquals("TCP", actualFixedFields.get(Constants.Fields.PROTOCOL.getName()));
    Assert.assertEquals("filter", actualFixedFields.get(PcapHelper.PacketFields.PACKET_FILTER.getName()));

  }
}
