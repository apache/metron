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
import org.apache.hadoop.fs.Path;
import org.apache.metron.common.Constants;
import org.apache.metron.common.hadoop.SequenceFileIterable;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.filter.fixed.FixedPcapFilter;
import org.apache.metron.pcap.mr.PcapJob;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.PcapResponse;
import org.apache.metron.rest.model.pcap.FixedPcapRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@SuppressWarnings("ALL")
public class PcapServiceImplTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  Environment environment;
  Configuration configuration;
  PcapJob pcapJob;

  @Before
  public void setUp() throws Exception {
    environment = mock(Environment.class);
    pcapJob = mock(PcapJob.class);
    configuration = mock(Configuration.class);

    when(environment.getProperty(MetronRestConstants.PCAP_INPUT_PATH_SPRING_PROPERTY)).thenReturn("/input/path");
    when(environment.getProperty(MetronRestConstants.PCAP_OUTPUT_PATH_SPRING_PROPERTY)).thenReturn("/output/path");
  }


  @Test
  public void fixedShouldProperlyCallPcapJobQuery() throws Exception {
    FixedPcapRequest fixedPcapRequest = new FixedPcapRequest();
    fixedPcapRequest.setBaseOutputPath("baseOutputPath");
    fixedPcapRequest.setBasePath("basePath");
    fixedPcapRequest.setStartTime(1L);
    fixedPcapRequest.setEndTime(2L);
    fixedPcapRequest.setNumReducers(2);
    fixedPcapRequest.setIpSrcAddr("ip_src_addr");
    fixedPcapRequest.setIpDstAddr("ip_dst_addr");
    fixedPcapRequest.setIpSrcPort(1000);
    fixedPcapRequest.setIpDstPort(2000);
    fixedPcapRequest.setProtocol("tcp");
    fixedPcapRequest.setPacketFilter("filter");
    fixedPcapRequest.setIncludeReverse(true);

    PcapServiceImpl pcapService = spy(new PcapServiceImpl(environment, configuration, pcapJob));
    FileSystem fileSystem = mock(FileSystem.class);
    doReturn(fileSystem).when(pcapService).getFileSystem();
    Map<String, String> expectedFields = new HashMap<String, String>() {{
      put(Constants.Fields.SRC_ADDR.getName(), "ip_src_addr");
      put(Constants.Fields.DST_ADDR.getName(), "ip_dst_addr");
      put(Constants.Fields.SRC_PORT.getName(), "1000");
      put(Constants.Fields.DST_PORT.getName(), "2000");
      put(Constants.Fields.PROTOCOL.getName(), "tcp");
      put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName(), "true");
      put(PcapHelper.PacketFields.PACKET_FILTER.getName(), "filter");
    }};
    List<byte[]> expectedPcaps = Arrays.asList("pcap1".getBytes(), "pcap2".getBytes());
    SequenceFileIterable results = mock(SequenceFileIterable.class);
    when(results.iterator()).thenReturn(expectedPcaps.iterator());
    when(pcapJob.query(eq(new Path("basePath")),
            eq(new Path("baseOutputPath")),
            eq(1000000L),
            eq(2000000L),
            eq(2),
            eq(expectedFields),
            eq(configuration),
            any(FileSystem.class),
            any(FixedPcapFilter.Configurator.class))).thenReturn(results);

    PcapResponse pcapsResponse = pcapService.fixed(fixedPcapRequest);
    Assert.assertEquals(expectedPcaps, pcapsResponse.getPcaps());
  }

  @Test
  public void fixedShouldProperlyCallPcapJobQueryWithDefaults() throws Exception {
    FixedPcapRequest fixedPcapRequest = new FixedPcapRequest();

    PcapServiceImpl pcapService = spy(new PcapServiceImpl(environment, configuration, pcapJob));
    FileSystem fileSystem = mock(FileSystem.class);
    doReturn(fileSystem).when(pcapService).getFileSystem();
    Map<String, String> expectedFields = new HashMap<String, String>() {{
      put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName(), "false");
    }};
    List<byte[]> expectedPcaps = Arrays.asList("pcap1".getBytes(), "pcap2".getBytes());
    SequenceFileIterable results = mock(SequenceFileIterable.class);
    when(results.iterator()).thenReturn(expectedPcaps.iterator());
    when(pcapJob.query(eq(new Path("/input/path")),
            eq(new Path("/output/path")),
            eq(0L),
            eq(fixedPcapRequest.getEndTime() * 1000000),
            eq(1),
            eq(expectedFields),
            eq(configuration),
            any(FileSystem.class),
            any(FixedPcapFilter.Configurator.class))).thenReturn(results);

    PcapResponse pcapsResponse = pcapService.fixed(fixedPcapRequest);
    Assert.assertEquals(expectedPcaps, pcapsResponse.getPcaps());
  }

  @Test
  public void fixedShouldThrowRestException() throws Exception {
    exception.expect(RestException.class);
    exception.expectMessage("some exception");

    FixedPcapRequest fixedPcapRequest = new FixedPcapRequest();

    PcapServiceImpl pcapService = spy(new PcapServiceImpl(environment, configuration, pcapJob));
    FileSystem fileSystem = mock(FileSystem.class);
    doReturn(fileSystem).when(pcapService).getFileSystem();

    when(pcapJob.query(any(),
            any(),
            eq(0L),
            eq(fixedPcapRequest.getEndTime() * 1000000),
            eq(1),
            any(),
            any(),
            any(FileSystem.class),
            any(FixedPcapFilter.Configurator.class))).thenThrow(new IOException("some exception"));

    pcapService.fixed(fixedPcapRequest);
  }
}
