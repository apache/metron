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

import org.adrianwalker.multilinestring.Multiline;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.common.Constants;
import org.apache.metron.common.hadoop.SequenceFileIterable;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.filter.fixed.FixedPcapFilter;
import org.apache.metron.pcap.mr.PcapJob;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.PcapResponse;
import org.apache.metron.rest.model.pcap.FixedPcapRequest;
import org.apache.metron.rest.model.pcap.Pdml;
import org.apache.metron.rest.service.HdfsService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("ALL")
@RunWith(PowerMockRunner.class)
@PrepareForTest({PcapServiceImpl.class, ProcessBuilder.class})
public class PcapServiceImplTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  /**
   *<?xml version="1.0" encoding="utf-8"?>
   *<?xml-stylesheet type="text/xsl" href="pdml2html.xsl"?>
   *<pdml version="0" creator="wireshark/2.6.1" time="Thu Jun 28 14:14:38 2018" capture_file="/tmp/pcap-data-201806272004-289365c53112438ca55ea047e13a12a5+0001.pcap">
   *<packet>
   *<proto name="geninfo" pos="0" showname="General information" size="722" hide="no">
   *<field name="num" pos="0" show="1" showname="Number" value="1" size="722"/>
   *</proto>
   *<proto name="ip" showname="Internet Protocol Version 4, Src: 192.168.66.1, Dst: 192.168.66.121" size="20" pos="14" hide="yes">
   *<field name="ip.addr" showname="Source or Destination Address: 192.168.66.121" hide="yes" size="4" pos="30" show="192.168.66.121" value="c0a84279"/>
   *<field name="ip.flags" showname="Flags: 0x4000, Don&#x27;t fragment" size="2" pos="20" show="0x00004000" value="4000">
   *<field name="ip.flags.mf" showname="..0. .... .... .... = More fragments: Not set" size="2" pos="20" show="0" value="0" unmaskedvalue="4000"/>
   *</field>
   *</proto>
   *</packet>
   *</pdml>
   */
  @Multiline
  private String pdmlXml;

  /**
   *{
   "version": "0",
   "creator": "wireshark/2.6.1",
   "time": "Thu Jun 28 14:14:38 2018",
   "captureFile": "/tmp/pcap-data-201806272004-289365c53112438ca55ea047e13a12a5+0001.pcap",
   "packets": [
   {
   "protos": [
   {
   "name": "geninfo",
   "pos": "0",
   "showname": "General information",
   "size": "722",
   "hide": "no",
   "fields": [
   {
   "name": "num",
   "pos": "0",
   "showname": "Number",
   "size": "722",
   "value": "1",
   "show": "1"
   }
   ]
   },
   {
   "name": "ip",
   "pos": "14",
   "showname": "Internet Protocol Version 4, Src: 192.168.66.1, Dst: 192.168.66.121",
   "size": "20",
   "hide": "yes",
   "fields": [
   {
   "name": "ip.addr",
   "pos": "30",
   "showname": "Source or Destination Address: 192.168.66.121",
   "size": "4",
   "value": "c0a84279",
   "show": "192.168.66.121",
   "hide": "yes"
   },
   {
   "name": "ip.flags",
   "pos": "20",
   "showname": "Flags: 0x4000, Don't fragment",
   "size": "2",
   "value": "4000",
   "show": "0x00004000",
   "fields": [
   {
   "name": "ip.flags.mf",
   "pos": "20",
   "showname": "..0. .... .... .... = More fragments: Not set",
   "size": "2",
   "value": "0",
   "show": "0",
   "unmaskedvalue": "4000"
   }
   ]
   }
   ]
   }
   ]
   }
   ]
   }
   */
  @Multiline
  private String expectedPdml;

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

//    PcapResponse pcapsResponse = pcapService.fixed(fixedPcapRequest);
//    Assert.assertEquals(expectedPcaps, pcapsResponse.getPcaps());
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

//    PcapResponse pcapsResponse = pcapService.fixed(fixedPcapRequest);
//    Assert.assertEquals(expectedPcaps, pcapsResponse.getPcaps());
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

  @Test
  public void getPdmlShouldGetPdml() throws Exception {
    Path path = new Path("./target");
    PcapServiceImpl pcapService = new PcapServiceImpl(environment, configuration, pcapJob);
    ProcessBuilder pb = mock(ProcessBuilder.class);
    Process p = mock(Process.class);
    whenNew(ProcessBuilder.class).withParameterTypes(String[].class).withArguments(anyVararg()).thenReturn(pb);
    when(pb.start()).thenReturn(p);
    when(p.getInputStream()).thenReturn(new ByteArrayInputStream(pdmlXml.getBytes()));

    assertEquals(JSONUtils.INSTANCE.load(expectedPdml, Pdml.class), pcapService.getPdml(path));
  }

  @Test
  public void getPdmlShouldReturnNullOnNonexistentPath() throws Exception {
    Path path = new Path("/some/path");

    PcapServiceImpl pcapService = new PcapServiceImpl(environment, configuration, pcapJob);

    assertNull(pcapService.getPdml(path));
  }

  @Test
  public void getPdmlShouldThrowException() throws Exception {
    exception.expect(RestException.class);
    exception.expectMessage("some exception");

    Path path = new Path("./target");
    PcapServiceImpl pcapService = new PcapServiceImpl(environment, configuration, pcapJob);
    ProcessBuilder pb = mock(ProcessBuilder.class);
    whenNew(ProcessBuilder.class).withParameterTypes(String[].class).withArguments(anyVararg()).thenReturn(pb);
    when(pb.start()).thenThrow(new IOException("some exception"));

    pcapService.getPdml(path);
  }
}
