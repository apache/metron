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
package org.apache.metron.rest.service.impl;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.metron.common.Constants;
import org.apache.metron.job.JobException;
import org.apache.metron.job.JobStatus;
import org.apache.metron.job.Pageable;
import org.apache.metron.job.manager.InMemoryJobManager;
import org.apache.metron.job.manager.JobManager;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.filter.fixed.FixedPcapFilter;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.config.PcapJobSupplier;
import org.apache.metron.rest.mock.MockPcapJob;
import org.apache.metron.rest.mock.MockPcapJobSupplier;
import org.apache.metron.rest.model.pcap.FixedPcapRequest;
import org.apache.metron.rest.model.pcap.PcapStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("ALL")
public class PcapServiceImplTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  Environment environment;
  Configuration configuration;
  MockPcapJobSupplier mockPcapJobSupplier;

  @Before
  public void setUp() throws Exception {
    environment = mock(Environment.class);
    configuration = mock(Configuration.class);
    mockPcapJobSupplier = new MockPcapJobSupplier();

    when(environment.getProperty(MetronRestConstants.PCAP_BASE_PATH_SPRING_PROPERTY)).thenReturn("/base/path");
    when(environment.getProperty(MetronRestConstants.PCAP_BASE_INTERIM_RESULT_PATH_SPRING_PROPERTY)).thenReturn("/base/interim/result/path");
    when(environment.getProperty(MetronRestConstants.PCAP_FINAL_OUTPUT_PATH_SPRING_PROPERTY)).thenReturn("/final/output/path");
    when(environment.getProperty(MetronRestConstants.PCAP_PAGE_SIZE_SPRING_PROPERTY)).thenReturn("100");
  }

  @Test
  public void fixedShouldProperlyCallPcapJobQuery() throws Exception {
    FixedPcapRequest fixedPcapRequest = new FixedPcapRequest();
    fixedPcapRequest.setBasePath("basePath");
    fixedPcapRequest.setBaseInterimResultPath("baseOutputPath");
    fixedPcapRequest.setFinalOutputPath("finalOutputPath");
    fixedPcapRequest.setStartTimeMs(1L);
    fixedPcapRequest.setEndTimeMs(2L);
    fixedPcapRequest.setNumReducers(2);
    fixedPcapRequest.setIpSrcAddr("ip_src_addr");
    fixedPcapRequest.setIpDstAddr("ip_dst_addr");
    fixedPcapRequest.setIpSrcPort(1000);
    fixedPcapRequest.setIpDstPort(2000);
    fixedPcapRequest.setProtocol("tcp");
    fixedPcapRequest.setPacketFilter("filter");
    fixedPcapRequest.setIncludeReverse(true);
    MockPcapJob mockPcapJob = new MockPcapJob();
    mockPcapJobSupplier.setMockPcapJob(mockPcapJob);
    JobManager jobManager = new InMemoryJobManager<>();

    PcapServiceImpl pcapService = spy(new PcapServiceImpl(environment, configuration, mockPcapJobSupplier, jobManager));
    FileSystem fileSystem = mock(FileSystem.class);
    doReturn(fileSystem).when(pcapService).getFileSystem();
    mockPcapJob.setStatus(new JobStatus()
            .withJobId("jobId")
            .withDescription("description")
            .withPercentComplete(0L)
            .withState(JobStatus.State.RUNNING));

    Map<String, String> expectedFields = new HashMap<String, String>() {{
      put(Constants.Fields.SRC_ADDR.getName(), "ip_src_addr");
      put(Constants.Fields.DST_ADDR.getName(), "ip_dst_addr");
      put(Constants.Fields.SRC_PORT.getName(), "1000");
      put(Constants.Fields.DST_PORT.getName(), "2000");
      put(Constants.Fields.PROTOCOL.getName(), "tcp");
      put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName(), "true");
      put(PcapHelper.PacketFields.PACKET_FILTER.getName(), "filter");
    }};
    PcapStatus expectedPcapStatus = new PcapStatus();
    expectedPcapStatus.setJobId("jobId");
    expectedPcapStatus.setJobStatus(JobStatus.State.RUNNING.name());
    expectedPcapStatus.setDescription("description");

    Assert.assertEquals(expectedPcapStatus, pcapService.fixed("user", fixedPcapRequest));
    Assert.assertEquals(expectedPcapStatus, pcapService.jobStatusToPcapStatus(jobManager.getJob("user", "jobId").getStatus()));
    Assert.assertEquals("basePath", mockPcapJob.getBasePath());
    Assert.assertEquals("baseOutputPath", mockPcapJob.getBaseInterrimResultPath());
    Assert.assertEquals("finalOutputPath", mockPcapJob.getFinalOutputPath());
    Assert.assertEquals(1000000, mockPcapJob.getStartTimeNs());
    Assert.assertEquals(2000000, mockPcapJob.getEndTimeNs());
    Assert.assertEquals(2, mockPcapJob.getNumReducers());
    Assert.assertEquals(100, mockPcapJob.getRecPerFile());
    Assert.assertTrue(mockPcapJob.getFilterImpl() instanceof FixedPcapFilter.Configurator);
    Map<String, String> actualFixedFields = mockPcapJob.getFixedFields();
    Assert.assertEquals("ip_src_addr", actualFixedFields.get(Constants.Fields.SRC_ADDR.getName()));
    Assert.assertEquals("1000", actualFixedFields.get(Constants.Fields.SRC_PORT.getName()));
    Assert.assertEquals("ip_dst_addr", actualFixedFields.get(Constants.Fields.DST_ADDR.getName()));
    Assert.assertEquals("2000", actualFixedFields.get(Constants.Fields.DST_PORT.getName()));
    Assert.assertEquals("true", actualFixedFields.get(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName()));
    Assert.assertEquals("tcp", actualFixedFields.get(Constants.Fields.PROTOCOL.getName()));
    Assert.assertEquals("filter", actualFixedFields.get(PcapHelper.PacketFields.PACKET_FILTER.getName()));
  }

  @Test
  public void fixedShouldProperlyCallPcapJobQueryWithDefaults() throws Exception {
    long beforeJobTime = System.currentTimeMillis();

    FixedPcapRequest fixedPcapRequest = new FixedPcapRequest();
    MockPcapJob mockPcapJob = new MockPcapJob();
    mockPcapJobSupplier.setMockPcapJob(mockPcapJob);
    JobManager jobManager = new InMemoryJobManager<>();

    PcapServiceImpl pcapService = spy(new PcapServiceImpl(environment, configuration, mockPcapJobSupplier, jobManager));
    FileSystem fileSystem = mock(FileSystem.class);
    doReturn(fileSystem).when(pcapService).getFileSystem();
    mockPcapJob.setStatus(new JobStatus()
            .withJobId("jobId")
            .withDescription("description")
            .withPercentComplete(0L)
            .withState(JobStatus.State.RUNNING));

    PcapStatus expectedPcapStatus = new PcapStatus();
    expectedPcapStatus.setJobId("jobId");
    expectedPcapStatus.setJobStatus(JobStatus.State.RUNNING.name());
    expectedPcapStatus.setDescription("description");

    Assert.assertEquals(expectedPcapStatus, pcapService.fixed("user", fixedPcapRequest));
    Assert.assertEquals("/base/path", mockPcapJob.getBasePath());
    Assert.assertEquals("/base/interim/result/path", mockPcapJob.getBaseInterrimResultPath());
    Assert.assertEquals("/final/output/path", mockPcapJob.getFinalOutputPath());
    Assert.assertEquals(0, mockPcapJob.getStartTimeNs());
    Assert.assertTrue(beforeJobTime <= mockPcapJob.getEndTimeNs() / 1000000);
    Assert.assertTrue(System.currentTimeMillis() >= mockPcapJob.getEndTimeNs() / 1000000);
    Assert.assertEquals(10, mockPcapJob.getNumReducers());
    Assert.assertEquals(100, mockPcapJob.getRecPerFile());
    Assert.assertTrue(mockPcapJob.getFilterImpl() instanceof FixedPcapFilter.Configurator);
    Assert.assertEquals(new HashMap<>(), mockPcapJob.getFixedFields());
  }

  @Test
  public void fixedShouldThrowRestException() throws Exception {
    exception.expect(RestException.class);
    exception.expectMessage("some job exception");

    FixedPcapRequest fixedPcapRequest = new FixedPcapRequest();
    JobManager jobManager = mock(JobManager.class);
    PcapJobSupplier pcapJobSupplier = new PcapJobSupplier();
    PcapServiceImpl pcapService = spy(new PcapServiceImpl(environment, configuration, pcapJobSupplier, jobManager));
    FileSystem fileSystem = mock(FileSystem.class);
    doReturn(fileSystem).when(pcapService).getFileSystem();
    when(jobManager.submit(pcapJobSupplier, "user")).thenThrow(new JobException("some job exception"));

    pcapService.fixed("user", fixedPcapRequest);
  }

  @Test
  public void getStatusShouldProperlyReturnStatus() throws Exception {
    MockPcapJob mockPcapJob = mock(MockPcapJob.class);
    JobManager jobManager = mock(JobManager.class);
    JobStatus actualJobStatus = new JobStatus()
            .withJobId("jobId")
            .withState(JobStatus.State.SUCCEEDED)
            .withDescription("description")
            .withPercentComplete(100.0);
    Pageable pageable = mock(Pageable.class);
    when(pageable.getSize()).thenReturn(2);
    when(mockPcapJob.getStatus()).thenReturn(actualJobStatus);
    when(mockPcapJob.isDone()).thenReturn(true);
    when(mockPcapJob.get()).thenReturn(pageable);
    when(jobManager.getJob("user", "jobId")).thenReturn(mockPcapJob);

    PcapServiceImpl pcapService = new PcapServiceImpl(environment, configuration, mockPcapJobSupplier, jobManager);
    PcapStatus expectedPcapStatus = new PcapStatus();
    expectedPcapStatus.setJobId("jobId");
    expectedPcapStatus.setJobStatus(JobStatus.State.SUCCEEDED.name());
    expectedPcapStatus.setDescription("description");
    expectedPcapStatus.setPercentComplete(100.0);
    expectedPcapStatus.setPageTotal(2);

    Assert.assertEquals(expectedPcapStatus, pcapService.getJobStatus("user", "jobId"));
  }

  @Test
  public void getStatusShouldReturnNullOnMissingStatus() throws Exception {
    JobManager jobManager = new InMemoryJobManager();
    PcapServiceImpl pcapService = new PcapServiceImpl(environment, configuration, new PcapJobSupplier(), jobManager);

    Assert.assertNull(pcapService.getJobStatus("user", "jobId"));
  }

  @Test
  public void getStatusShouldThrowRestException() throws Exception {
    exception.expect(RestException.class);
    exception.expectMessage("some job exception");

    JobManager jobManager = mock(JobManager.class);
    when(jobManager.getJob("user", "jobId")).thenThrow(new JobException("some job exception"));

    PcapServiceImpl pcapService = new PcapServiceImpl(environment, configuration, new PcapJobSupplier(), jobManager);
    pcapService.getJobStatus("user", "jobId");
  }

}
