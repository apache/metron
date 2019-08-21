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

import java.nio.charset.StandardCharsets;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.Path;
import org.apache.metron.common.Constants;
import org.apache.metron.job.JobStatus;
import org.apache.metron.job.Pageable;
import org.apache.metron.job.manager.InMemoryJobManager;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.PcapPages;
import org.apache.metron.pcap.filter.fixed.FixedPcapFilter;
import org.apache.metron.pcap.filter.query.QueryPcapFilter;
import org.apache.metron.rest.mock.MockPcapJob;
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

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TEST_PROFILE)
public class PcapControllerIntegrationTest {

  /**
   {
   "basePath": "/base/path",
   "baseInterimResultPath": "/base/interim/result/path",
   "finalOutputPath": "/final/output/path",
   "startTimeMs": 10,
   "endTimeMs": 20,
   "numReducers": 2,
   "includeReverse": "true",
   "ipDstAddr": "192.168.1.1",
   "ipDstPort": "1000",
   "ipSrcAddr": "192.168.1.2",
   "ipSrcPort": "2000",
   "packetFilter": "filter",
   "protocol": "TCP"
   }
   */
  @Multiline
  public static String fixedJson;

  /**
   {
   "includeReverse": "true",
   "ipDstAddr": "192.168.1.1",
   "ipDstPort": "1000",
   "ipSrcAddr": "192.168.1.2",
   "ipSrcPort": "2000",
   "packetFilter": "filter",
   "protocol": "TCP"
   }
   */
  @Multiline
  public static String fixedWithDefaultsJson;

  /**
   {
   "basePath": "/base/path",
   "baseInterimResultPath": "/base/interim/result/path",
   "finalOutputPath": "/final/output/path",
   "startTimeMs": 10,
   "endTimeMs": 20,
   "numReducers": 2,
   "query": "query"
   }
   */
  @Multiline
  public static String queryJson;

  @Autowired
  private PcapService pcapService;

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  private String pcapUrl = "/api/v1/pcap";
  private String user = "user";
  private String user2 = "user2";
  private String password = "password";

  @Before
  public void setup() throws Exception {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
    InMemoryJobManager jobManager = (InMemoryJobManager) wac.getBean("jobManager");
    jobManager.clear();
  }

  @Test
  public void testSecurity() throws Exception {
    this.mockMvc.perform(post(pcapUrl + "/fixed").with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(fixedJson))
            .andExpect(status().isUnauthorized());

    this.mockMvc.perform(post(pcapUrl + "/query").with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(queryJson))
            .andExpect(status().isUnauthorized());
  }

  @Test
  public void testFixedRequest() throws Exception {
    MockPcapJob mockPcapJob = (MockPcapJob) wac.getBean("mockPcapJob");
    mockPcapJob.setStatus(new JobStatus().withState(JobStatus.State.RUNNING));

    this.mockMvc.perform(post(pcapUrl + "/fixed").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(fixedJson))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.jobStatus").value("RUNNING"));

    Assert.assertEquals("/base/path", mockPcapJob.getBasePath());
    Assert.assertEquals("/base/interim/result/path", mockPcapJob.getBaseInterrimResultPath());
    Assert.assertEquals("/final/output/path", mockPcapJob.getFinalOutputPath());
    Assert.assertEquals(10000000, mockPcapJob.getStartTimeNs());
    Assert.assertEquals(20000000, mockPcapJob.getEndTimeNs());
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


  @Test
  public void testFixedRequestDefaults() throws Exception {
    MockPcapJob mockPcapJob = (MockPcapJob) wac.getBean("mockPcapJob");
    mockPcapJob.setStatus(new JobStatus().withState(JobStatus.State.RUNNING));
    long beforeJobTime = System.currentTimeMillis();

    this.mockMvc.perform(post(pcapUrl + "/fixed").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(fixedWithDefaultsJson))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.jobStatus").value("RUNNING"));

    Assert.assertEquals("/apps/metron/pcap/input", mockPcapJob.getBasePath());
    Assert.assertEquals("/apps/metron/pcap/interim", mockPcapJob.getBaseInterrimResultPath());
    Assert.assertEquals("/apps/metron/pcap/output", mockPcapJob.getFinalOutputPath());
    Assert.assertEquals(0, mockPcapJob.getStartTimeNs());
    Assert.assertTrue(beforeJobTime < mockPcapJob.getEndTimeNs() / 1000000);
    Assert.assertTrue(System.currentTimeMillis() > mockPcapJob.getEndTimeNs() / 1000000);
    Assert.assertEquals(10, mockPcapJob.getNumReducers());
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

  @Test
  public void testQueryRequest() throws Exception {
    MockPcapJob mockPcapJob = (MockPcapJob) wac.getBean("mockPcapJob");
    mockPcapJob.setStatus(new JobStatus().withState(JobStatus.State.RUNNING));

    this.mockMvc.perform(post(pcapUrl + "/query").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(queryJson))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.jobStatus").value("RUNNING"));

    Assert.assertEquals("/base/path", mockPcapJob.getBasePath());
    Assert.assertEquals("/base/interim/result/path", mockPcapJob.getBaseInterrimResultPath());
    Assert.assertEquals("/final/output/path", mockPcapJob.getFinalOutputPath());
    Assert.assertEquals(10000000, mockPcapJob.getStartTimeNs());
    Assert.assertEquals(20000000, mockPcapJob.getEndTimeNs());
    Assert.assertEquals(2, mockPcapJob.getNumReducers());
    Assert.assertTrue(mockPcapJob.getFilterImpl() instanceof QueryPcapFilter.Configurator);
    Assert.assertEquals("query", mockPcapJob.getQuery());
  }

  @Test
  public void testTooManyJobs() throws Exception {
    MockPcapJob mockPcapJob = (MockPcapJob) wac.getBean("mockPcapJob");

    mockPcapJob.setStatus(new JobStatus().withJobId("jobId").withState(JobStatus.State.RUNNING));

    this.mockMvc.perform(post(pcapUrl + "/fixed").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(fixedJson))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.jobId").value("jobId"))
            .andExpect(jsonPath("$.jobStatus").value("RUNNING"));

    this.mockMvc.perform(post(pcapUrl + "/fixed").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(fixedJson))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.message").value("Cannot submit job because a job is already running.  Please contact the administrator to cancel job(s) with id(s) jobId"));

  }

  @Test
  public void testGetStatus() throws Exception {
    MockPcapJob mockPcapJob = (MockPcapJob) wac.getBean("mockPcapJob");

    mockPcapJob.setStatus(new JobStatus().withJobId("jobId").withState(JobStatus.State.RUNNING));

    this.mockMvc.perform(post(pcapUrl + "/fixed").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(fixedJson))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.jobId").value("jobId"))
            .andExpect(jsonPath("$.jobStatus").value("RUNNING"));

    mockPcapJob.setStatus(new JobStatus().withJobId("jobId").withState(JobStatus.State.SUCCEEDED));

    Pageable<Path> pageable = new PcapPages(Arrays.asList(new Path("path1"), new Path("path1")));
    mockPcapJob.setIsDone(true);
    mockPcapJob.setPageable(pageable);

    this.mockMvc.perform(get(pcapUrl + "/jobId").with(httpBasic(user, password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.jobStatus").value("SUCCEEDED"))
            .andExpect(jsonPath("$.pageTotal").value(2));

    mockPcapJob.setStatus(new JobStatus().withJobId("jobId").withState(JobStatus.State.FINALIZING));

    this.mockMvc.perform(get(pcapUrl + "/jobId").with(httpBasic(user, password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.jobStatus").value("FINALIZING"));

    mockPcapJob.setStatus(new JobStatus().withJobId("jobId").withState(JobStatus.State.FAILED));

    this.mockMvc.perform(get(pcapUrl + "/jobId").with(httpBasic(user, password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.jobStatus").value("FAILED"));

    mockPcapJob.setStatus(new JobStatus().withJobId("jobId").withState(JobStatus.State.KILLED));

    this.mockMvc.perform(get(pcapUrl + "/jobId").with(httpBasic(user, password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.jobStatus").value("KILLED"));

    this.mockMvc.perform(get(pcapUrl + "/someJobId").with(httpBasic(user, password)))
            .andExpect(status().isNotFound());
  }

  @Test
  public void testGetStatusList() throws Exception {
    MockPcapJob mockPcapJob = (MockPcapJob) wac.getBean("mockPcapJob");

    mockPcapJob.setStatus(new JobStatus().withJobId("jobId").withState(JobStatus.State.RUNNING));

    this.mockMvc.perform(post(pcapUrl + "/fixed").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(fixedJson))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.jobId").value("jobId"))
            .andExpect(jsonPath("$.jobStatus").value("RUNNING"));

    this.mockMvc.perform(get(pcapUrl + "?state=RUNNING").with(httpBasic(user, password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$[0].jobId").value("jobId"))
            .andExpect(jsonPath("$[0].jobStatus").value("RUNNING"));

    this.mockMvc.perform(get(pcapUrl + "?state=SUCCEEDED").with(httpBasic(user, password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(content().json("[]"));
  }

  @Test
  public void testKillJob() throws Exception {
    MockPcapJob mockPcapJob = (MockPcapJob) wac.getBean("mockPcapJob");

    this.mockMvc.perform(get(pcapUrl + "/jobId123").with(httpBasic(user, password)))
            .andExpect(status().isNotFound());

    mockPcapJob.setStatus(new JobStatus().withJobId("jobId123").withState(JobStatus.State.RUNNING));

    this.mockMvc.perform(post(pcapUrl + "/fixed").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(fixedJson))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.jobId").value("jobId123"))
            .andExpect(jsonPath("$.jobStatus").value("RUNNING"));

    mockPcapJob.setStatus(new JobStatus().withJobId("jobId123").withState(JobStatus.State.KILLED));

    this.mockMvc.perform(delete(pcapUrl + "/kill/{id}", "jobId123").with(httpBasic(user, password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.jobId").value("jobId123"))
            .andExpect(jsonPath("$.jobStatus").value("KILLED"));

    mockPcapJob.setStatus(new JobStatus().withJobId("jobId").withState(JobStatus.State.KILLED));
  }

  @Test
  public void testKillNonExistentJobReturns404() throws Exception {
    MockPcapJob mockPcapJob = (MockPcapJob) wac.getBean("mockPcapJob");

    this.mockMvc.perform(get(pcapUrl + "/jobId123").with(httpBasic(user, password)))
            .andExpect(status().isNotFound());

    this.mockMvc.perform(delete(pcapUrl + "/kill/{id}", "jobId123").with(httpBasic(user, password)))
            .andExpect(status().isNotFound());
  }

  @Test
  public void testGetPdml() throws Exception {
    MockPcapJob mockPcapJob = (MockPcapJob) wac.getBean("mockPcapJob");

    mockPcapJob.setStatus(new JobStatus().withJobId("jobId").withState(JobStatus.State.RUNNING));

    this.mockMvc.perform(post(pcapUrl + "/fixed").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(fixedJson))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.jobId").value("jobId"))
            .andExpect(jsonPath("$.jobStatus").value("RUNNING"));

    Pageable<Path> pageable = new PcapPages(Arrays.asList(new Path("./target")));
    mockPcapJob.setIsDone(true);
    mockPcapJob.setPageable(pageable);

    this.mockMvc.perform(get(pcapUrl + "/jobId/pdml?page=1").with(httpBasic(user, password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.version").value("0"))
            .andExpect(jsonPath("$.creator").value("wireshark/2.6.1"))
            .andExpect(jsonPath("$.time").value("Thu Jun 28 14:14:38 2018"))
            .andExpect(jsonPath("$.captureFile").value("/tmp/pcap-data-201806272004-289365c53112438ca55ea047e13a12a5+0001.pcap"))
            .andExpect(jsonPath("$.packets[0].protos[0].name").value("geninfo"))
            .andExpect(jsonPath("$.packets[0].protos[0].fields[0].name").value("num"))
            .andExpect(jsonPath("$.packets[0].protos[1].name").value("ip"))
            .andExpect(jsonPath("$.packets[0].protos[1].fields[0].name").value("ip.addr"))
    ;

    this.mockMvc.perform(get(pcapUrl + "/jobId/pdml?page=0").with(httpBasic(user, password)))
            .andExpect(status().isNotFound());

    this.mockMvc.perform(get(pcapUrl + "/jobId/pdml?page=2").with(httpBasic(user, password)))
            .andExpect(status().isNotFound());
  }

  @Test
  public void testRawDownload() throws Exception {
    String pcapFileContents = "pcap file contents";
    FileUtils.write(new File("./target/pcapFile"), pcapFileContents, "UTF8");

    MockPcapJob mockPcapJob = (MockPcapJob) wac.getBean("mockPcapJob");

    mockPcapJob.setStatus(new JobStatus().withJobId("jobId").withState(JobStatus.State.RUNNING));

    this.mockMvc.perform(post(pcapUrl + "/fixed").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(fixedJson))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.jobId").value("jobId"))
            .andExpect(jsonPath("$.jobStatus").value("RUNNING"));

    Pageable<Path> pageable = new PcapPages(Arrays.asList(new Path("./target/pcapFile")));
    mockPcapJob.setIsDone(true);
    mockPcapJob.setPageable(pageable);

    this.mockMvc.perform(get(pcapUrl + "/jobId/raw?page=1").with(httpBasic(user, password)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"pcap_jobId_1.pcap\""))
            .andExpect(header().string("Content-Length", Integer.toString(pcapFileContents.length())))
            .andExpect(content().contentType(MediaType.parseMediaType("application/octet-stream")))
            .andExpect(content().bytes(pcapFileContents.getBytes(StandardCharsets.UTF_8)));

    this.mockMvc.perform(get(pcapUrl + "/jobId/raw?page=1&fileName=pcapFile.pcap").with(httpBasic(user, password)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"pcapFile.pcap\""))
            .andExpect(header().string("Content-Length", Integer.toString(pcapFileContents.length())))
            .andExpect(content().contentType(MediaType.parseMediaType("application/octet-stream")))
            .andExpect(content().bytes(pcapFileContents.getBytes(StandardCharsets.UTF_8)));

    this.mockMvc.perform(get(pcapUrl + "/jobId/raw?page=2").with(httpBasic(user, password)))
            .andExpect(status().isNotFound());
  }

  @Test
  public void testGetFixedFilterConfiguration() throws Exception {
    MockPcapJob mockPcapJob = (MockPcapJob) wac.getBean("mockPcapJob");

    mockPcapJob.setStatus(new JobStatus().withJobId("jobId").withState(JobStatus.State.RUNNING));

    this.mockMvc.perform(post(pcapUrl + "/fixed").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(fixedJson))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.jobId").value("jobId"))
            .andExpect(jsonPath("$.jobStatus").value("RUNNING"));

    this.mockMvc.perform(get(pcapUrl + "/jobId/config").with(httpBasic(user, password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.basePath").value("/base/path"))
            .andExpect(jsonPath("$.finalOutputPath").value("/final/output/path"))
            .andExpect(jsonPath("$.startTimeMs").value(10))
            .andExpect(jsonPath("$.endTimeMs").value(20))
            .andExpect(jsonPath("$.numReducers").value(2))
            .andExpect(jsonPath("$.ipSrcAddr").value("192.168.1.2"))
            .andExpect(jsonPath("$.ipDstAddr").value("192.168.1.1"))
            .andExpect(jsonPath("$.ipSrcPort").value("2000"))
            .andExpect(jsonPath("$.ipDstPort").value("1000"))
            .andExpect(jsonPath("$.protocol").value("TCP"))
            .andExpect(jsonPath("$.packetFilter").value("filter"))
            .andExpect(jsonPath("$.includeReverse").value("true"));
  }

  @Test
  public void testGetQueryFilterConfiguration() throws Exception {
    MockPcapJob mockPcapJob = (MockPcapJob) wac.getBean("mockPcapJob");

    mockPcapJob.setStatus(new JobStatus().withJobId("jobId").withState(JobStatus.State.RUNNING));

    this.mockMvc.perform(post(pcapUrl + "/query").with(httpBasic(user, password)).with(csrf()).contentType(MediaType.parseMediaType("application/json;charset=UTF-8")).content(queryJson))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.jobId").value("jobId"))
            .andExpect(jsonPath("$.jobStatus").value("RUNNING"));

    this.mockMvc.perform(get(pcapUrl + "/jobId/config").with(httpBasic(user, password)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/json;charset=UTF-8")))
            .andExpect(jsonPath("$.basePath").value("/base/path"))
            .andExpect(jsonPath("$.finalOutputPath").value("/final/output/path"))
            .andExpect(jsonPath("$.startTimeMs").value(10))
            .andExpect(jsonPath("$.endTimeMs").value(20))
            .andExpect(jsonPath("$.numReducers").value(2))
            .andExpect(jsonPath("$.query").value("query"));
  }
}
