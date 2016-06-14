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

package org.apache.metron.pcapservice;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.pcap.filter.PcapFilterConfigurator;
import org.apache.metron.pcap.mr.PcapJob;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;

public class PcapReceiverImplRestEasyTest {

  public static class MockQueryHandler<R> extends PcapJob {
    Path basePath;
    Path baseOutputPath;
    long beginNS;
    long endNS;
    R fields;
    PcapFilterConfigurator<R> filterImpl;

    @Override
    public <T> List<byte[]> query( Path basePath
            , Path baseOutputPath
            , long beginNS
            , long endNS
            , T fields
            , Configuration conf
            , FileSystem fs
            , PcapFilterConfigurator<T> filterImpl
    ) throws IOException, ClassNotFoundException, InterruptedException
    {
      this.basePath = basePath;
      this.baseOutputPath = baseOutputPath;
      this.beginNS = beginNS;
      this.endNS = endNS;
      this.fields = (R) fields;
      this.filterImpl = (PcapFilterConfigurator<R>) filterImpl;
      return null;
    }
  }

  final MockQueryHandler<EnumMap<Constants.Fields, String>> fixedQueryHandler = new MockQueryHandler<EnumMap<Constants.Fields, String>>();
  final MockQueryHandler<String> queryQueryHandler = new MockQueryHandler<String>();
  PcapReceiverImplRestEasy fixedRestEndpoint = new PcapReceiverImplRestEasy() {{
    this.queryUtil = fixedQueryHandler;
  }};
  PcapReceiverImplRestEasy queryRestEndpoint = new PcapReceiverImplRestEasy() {{
      this.queryUtil = queryQueryHandler;
  }};

  @Before
  public void setup() throws Exception {
    ConfigurationUtil.setPcapOutputPath("/output");
    ConfigurationUtil.setTempQueryOutputPath("/tmp");
  }

  @Test
  public void testNormalFixedPath() throws Exception {
    String srcIp = "srcIp";
    String dstIp = "dstIp";
    String protocol = "protocol";
    String srcPort = "80";
    String dstPort = "100";
    long startTime = 100;
    long endTime = 1000;

    {
      boolean includeReverseTraffic = false;
      fixedRestEndpoint.getPcapsByIdentifiers(srcIp, dstIp, protocol, srcPort, dstPort, startTime, endTime, includeReverseTraffic, null);
      Assert.assertEquals(new Path(ConfigurationUtil.getPcapOutputPath()), fixedQueryHandler.basePath);
      Assert.assertEquals(new Path(ConfigurationUtil.getTempQueryOutputPath()), fixedQueryHandler.baseOutputPath);
      Assert.assertEquals(srcIp, fixedQueryHandler.fields.get(Constants.Fields.SRC_ADDR));
      Assert.assertEquals(dstIp, fixedQueryHandler.fields.get(Constants.Fields.DST_ADDR));
      Assert.assertEquals(srcPort, fixedQueryHandler.fields.get(Constants.Fields.SRC_PORT));
      Assert.assertEquals(dstPort, fixedQueryHandler.fields.get(Constants.Fields.DST_PORT));
      Assert.assertEquals(TimestampConverters.MILLISECONDS.toNanoseconds(startTime), fixedQueryHandler.beginNS);
      Assert.assertEquals(TimestampConverters.MILLISECONDS.toNanoseconds(endTime), fixedQueryHandler.endNS);
      Assert.assertEquals(includeReverseTraffic, Boolean.parseBoolean(fixedQueryHandler.fields.get(Constants.Fields.INCLUDES_REVERSE_TRAFFIC)));
    }
    {
      boolean includeReverseTraffic = true;
      fixedRestEndpoint.getPcapsByIdentifiers(srcIp, dstIp, protocol, srcPort, dstPort, startTime, endTime, includeReverseTraffic, null);
      Assert.assertEquals(new Path(ConfigurationUtil.getPcapOutputPath()), fixedQueryHandler.basePath);
      Assert.assertEquals(new Path(ConfigurationUtil.getTempQueryOutputPath()), fixedQueryHandler.baseOutputPath);
      Assert.assertEquals(srcIp, fixedQueryHandler.fields.get(Constants.Fields.SRC_ADDR));
      Assert.assertEquals(dstIp, fixedQueryHandler.fields.get(Constants.Fields.DST_ADDR));
      Assert.assertEquals(srcPort, fixedQueryHandler.fields.get(Constants.Fields.SRC_PORT));
      Assert.assertEquals(dstPort, fixedQueryHandler.fields.get(Constants.Fields.DST_PORT));
      Assert.assertEquals(TimestampConverters.MILLISECONDS.toNanoseconds(startTime), fixedQueryHandler.beginNS);
      Assert.assertEquals(TimestampConverters.MILLISECONDS.toNanoseconds(endTime), fixedQueryHandler.endNS);
      Assert.assertEquals(includeReverseTraffic, Boolean.parseBoolean(fixedQueryHandler.fields.get(Constants.Fields.INCLUDES_REVERSE_TRAFFIC)));
    }
  }

  @Test
  public void testNormalQueryPath() throws Exception {
    long startTime = 100;
    long endTime = 1000;
    String query = "ip_src_addr == 'srcIp' and ip_src_port == '80' and ip_dst_addr == 'dstIp' and ip_dst_port == '100' and protocol == 'protocol'";
    queryRestEndpoint.getPcapsByIdentifiers(query, startTime, endTime, null);
    Assert.assertEquals(new Path(ConfigurationUtil.getPcapOutputPath()), queryQueryHandler.basePath);
    Assert.assertEquals(new Path(ConfigurationUtil.getTempQueryOutputPath()), queryQueryHandler.baseOutputPath);
    Assert.assertEquals(query, queryQueryHandler.fields);
    Assert.assertEquals(TimestampConverters.MILLISECONDS.toNanoseconds(startTime), queryQueryHandler.beginNS);
    Assert.assertEquals(TimestampConverters.MILLISECONDS.toNanoseconds(endTime), queryQueryHandler.endNS);
  }

  @Test
  public void testNullSrcIp() throws Exception {
    String srcIp = null;
    String dstIp = "dstIp";
    String protocol = "protocol";
    String srcPort = "80";
    String dstPort = "100";
    long startTime = 100;
    long endTime = 1000;
    boolean includeReverseTraffic = false;
    fixedRestEndpoint.getPcapsByIdentifiers(srcIp, dstIp, protocol, srcPort, dstPort, startTime, endTime, includeReverseTraffic, null);
    Assert.assertEquals(new Path(ConfigurationUtil.getPcapOutputPath()), fixedQueryHandler.basePath);
    Assert.assertEquals(new Path(ConfigurationUtil.getTempQueryOutputPath()), fixedQueryHandler.baseOutputPath);
    Assert.assertEquals(srcIp, fixedQueryHandler.fields.get(Constants.Fields.SRC_ADDR));
    Assert.assertEquals(dstIp, fixedQueryHandler.fields.get(Constants.Fields.DST_ADDR));
    Assert.assertEquals(srcPort, fixedQueryHandler.fields.get(Constants.Fields.SRC_PORT));
    Assert.assertEquals(dstPort, fixedQueryHandler.fields.get(Constants.Fields.DST_PORT));
    Assert.assertEquals(TimestampConverters.MILLISECONDS.toNanoseconds(startTime), fixedQueryHandler.beginNS);
    Assert.assertEquals(TimestampConverters.MILLISECONDS.toNanoseconds(endTime), fixedQueryHandler.endNS);
    Assert.assertEquals(includeReverseTraffic, Boolean.parseBoolean(fixedQueryHandler.fields.get(Constants.Fields.INCLUDES_REVERSE_TRAFFIC)));
  }

  @Test
  public void testNullDstIp() throws Exception {
    String srcIp = "srcIp";
    String dstIp = null;
    String protocol = "protocol";
    String srcPort = "80";
    String dstPort = "100";
    long startTime = 100;
    long endTime = 1000;
    boolean includeReverseTraffic = false;
    fixedRestEndpoint.getPcapsByIdentifiers(srcIp, dstIp, protocol, srcPort, dstPort, startTime, endTime, includeReverseTraffic, null);
    Assert.assertEquals(new Path(ConfigurationUtil.getPcapOutputPath()), fixedQueryHandler.basePath);
    Assert.assertEquals(new Path(ConfigurationUtil.getTempQueryOutputPath()), fixedQueryHandler.baseOutputPath);
    Assert.assertEquals(srcIp, fixedQueryHandler.fields.get(Constants.Fields.SRC_ADDR));
    Assert.assertEquals(dstIp, fixedQueryHandler.fields.get(Constants.Fields.DST_ADDR));
    Assert.assertEquals(srcPort, fixedQueryHandler.fields.get(Constants.Fields.SRC_PORT));
    Assert.assertEquals(dstPort, fixedQueryHandler.fields.get(Constants.Fields.DST_PORT));
    Assert.assertEquals(TimestampConverters.MILLISECONDS.toNanoseconds(startTime), fixedQueryHandler.beginNS);
    Assert.assertEquals(TimestampConverters.MILLISECONDS.toNanoseconds(endTime), fixedQueryHandler.endNS);
    Assert.assertEquals(includeReverseTraffic, Boolean.parseBoolean(fixedQueryHandler.fields.get(Constants.Fields.INCLUDES_REVERSE_TRAFFIC)));
  }

  @Test
  public void testEmptyStartTime() throws Exception {
    String srcIp = "srcIp";
    String dstIp = "dstIp";
    String protocol = "protocol";
    String srcPort = "80";
    String dstPort = "100";
    long startTime = -1;
    long endTime = 1000;
    {
      boolean includeReverseTraffic = false;
      fixedRestEndpoint.getPcapsByIdentifiers(srcIp, dstIp, protocol, srcPort, dstPort, startTime, endTime, includeReverseTraffic, null);
      Assert.assertEquals(new Path(ConfigurationUtil.getPcapOutputPath()), fixedQueryHandler.basePath);
      Assert.assertEquals(new Path(ConfigurationUtil.getTempQueryOutputPath()), fixedQueryHandler.baseOutputPath);
      Assert.assertEquals(srcIp, fixedQueryHandler.fields.get(Constants.Fields.SRC_ADDR));
      Assert.assertEquals(dstIp, fixedQueryHandler.fields.get(Constants.Fields.DST_ADDR));
      Assert.assertEquals(srcPort, fixedQueryHandler.fields.get(Constants.Fields.SRC_PORT));
      Assert.assertEquals(dstPort, fixedQueryHandler.fields.get(Constants.Fields.DST_PORT));
      Assert.assertEquals(0, fixedQueryHandler.beginNS);
      Assert.assertEquals(TimestampConverters.MILLISECONDS.toNanoseconds(endTime), fixedQueryHandler.endNS);
      Assert.assertEquals(includeReverseTraffic, Boolean.parseBoolean(fixedQueryHandler.fields.get(Constants.Fields.INCLUDES_REVERSE_TRAFFIC)));
    }
    {
      String query = "ip_src_addr == 'srcIp' and ip_src_port == '80' and ip_dst_addr == 'dstIp' and ip_dst_port == '100' and protocol == 'protocol'";
      queryRestEndpoint.getPcapsByIdentifiers(query, startTime, endTime, null);
      Assert.assertEquals(new Path(ConfigurationUtil.getPcapOutputPath()), queryQueryHandler.basePath);
      Assert.assertEquals(new Path(ConfigurationUtil.getTempQueryOutputPath()), queryQueryHandler.baseOutputPath);
      Assert.assertEquals(query, queryQueryHandler.fields);
      Assert.assertEquals(0, queryQueryHandler.beginNS);
      Assert.assertEquals(TimestampConverters.MILLISECONDS.toNanoseconds(endTime), queryQueryHandler.endNS);
    }
  }

  @Test
  public void testEmptyEndTime() throws Exception {
    String srcIp = "srcIp";
    String dstIp = "dstIp";
    String protocol = "protocol";
    String srcPort = "80";
    String dstPort = "100";
    long startTime = -1;
    long endTime = -1;
    {
      boolean includeReverseTraffic = false;
      fixedRestEndpoint.getPcapsByIdentifiers(srcIp, dstIp, protocol, srcPort, dstPort, startTime, endTime, includeReverseTraffic, null);
      Assert.assertEquals(new Path(ConfigurationUtil.getPcapOutputPath()), fixedQueryHandler.basePath);
      Assert.assertEquals(new Path(ConfigurationUtil.getTempQueryOutputPath()), fixedQueryHandler.baseOutputPath);
      Assert.assertEquals(srcIp, fixedQueryHandler.fields.get(Constants.Fields.SRC_ADDR));
      Assert.assertEquals(dstIp, fixedQueryHandler.fields.get(Constants.Fields.DST_ADDR));
      Assert.assertEquals(srcPort, fixedQueryHandler.fields.get(Constants.Fields.SRC_PORT));
      Assert.assertEquals(dstPort, fixedQueryHandler.fields.get(Constants.Fields.DST_PORT));
      Assert.assertEquals(0, fixedQueryHandler.beginNS);
      Assert.assertTrue(fixedQueryHandler.endNS > 0);
      Assert.assertEquals(includeReverseTraffic, Boolean.parseBoolean(fixedQueryHandler.fields.get(Constants.Fields.INCLUDES_REVERSE_TRAFFIC)));
    }
    {
      String query = "ip_src_addr == 'srcIp' and ip_src_port == '80' and ip_dst_addr == 'dstIp' and ip_dst_port == '100' and protocol == 'protocol'";
      queryRestEndpoint.getPcapsByIdentifiers(query, startTime, endTime, null);
      Assert.assertEquals(new Path(ConfigurationUtil.getPcapOutputPath()), queryQueryHandler.basePath);
      Assert.assertEquals(new Path(ConfigurationUtil.getTempQueryOutputPath()), queryQueryHandler.baseOutputPath);
      Assert.assertEquals(query, queryQueryHandler.fields);
      Assert.assertEquals(0, queryQueryHandler.beginNS);
      Assert.assertTrue(queryQueryHandler.endNS > 0);
    }
  }

}
