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
import org.apache.metron.pcap.mr.PcapJob;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;

public class PcapReceiverImplRestEasyTest {
  public static class MockQueryHandler extends PcapJob {
    Path basePath;
    Path baseOutputPath;
    long beginNS;
    long endNS;
    EnumMap<Constants.Fields, String> fields;
    @Override
    public List<byte[]> query( Path basePath
            , Path baseOutputPath
            , long beginNS
            , long endNS
            , EnumMap<Constants.Fields, String> fields
            , Configuration conf
            , FileSystem fs
    ) throws IOException, ClassNotFoundException, InterruptedException
    {
      this.basePath = basePath;
      this.baseOutputPath = baseOutputPath;
      this.beginNS = beginNS;
      this.endNS = endNS;
      this.fields = fields;
      return null;
    }
  }
  final MockQueryHandler queryHandler = new MockQueryHandler();
  PcapReceiverImplRestEasy restEndpoint = new PcapReceiverImplRestEasy() {{
      this.queryUtil = queryHandler;
  }};

  @Before
  public void setup() throws Exception {
    ConfigurationUtil.setPcapOutputPath("/output");
    ConfigurationUtil.setTempQueryOutputPath("/tmp");
  }

  @Test
  public void testNormalPath() throws Exception {
    String srcIp = "srcIp";
    String dstIp = "dstIp";
    String protocol = "protocol";
    String srcPort = "80";
    String dstPort = "100";
    long startTime = 100;
    long endTime = 1000;

    {
      boolean includeReverseTraffic = false;
      restEndpoint.getPcapsByIdentifiers(srcIp, dstIp, protocol, srcPort, dstPort, startTime, endTime, includeReverseTraffic, null);
      Assert.assertEquals(new Path(ConfigurationUtil.getPcapOutputPath()), queryHandler.basePath);
      Assert.assertEquals(new Path(ConfigurationUtil.getTempQueryOutputPath()), queryHandler.baseOutputPath);
      Assert.assertEquals(srcIp, queryHandler.fields.get(Constants.Fields.SRC_ADDR));
      Assert.assertEquals(dstIp, queryHandler.fields.get(Constants.Fields.DST_ADDR));
      Assert.assertEquals(srcPort, queryHandler.fields.get(Constants.Fields.SRC_PORT));
      Assert.assertEquals(dstPort, queryHandler.fields.get(Constants.Fields.DST_PORT));
      Assert.assertEquals(TimestampConverters.MILLISECONDS.toNanoseconds(startTime), queryHandler.beginNS);
      Assert.assertEquals(TimestampConverters.MILLISECONDS.toNanoseconds(endTime), queryHandler.endNS);
      Assert.assertEquals(includeReverseTraffic, Boolean.getBoolean(queryHandler.fields.get(Constants.Fields.INCLUDES_REVERSE_TRAFFIC)));
    }
    {
      boolean includeReverseTraffic = true;
      restEndpoint.getPcapsByIdentifiers(srcIp, dstIp, protocol, srcPort, dstPort, startTime, endTime, includeReverseTraffic, null);
      Assert.assertEquals(new Path(ConfigurationUtil.getPcapOutputPath()), queryHandler.basePath);
      Assert.assertEquals(new Path(ConfigurationUtil.getTempQueryOutputPath()), queryHandler.baseOutputPath);
      Assert.assertEquals(srcIp, queryHandler.fields.get(Constants.Fields.SRC_ADDR));
      Assert.assertEquals(dstIp, queryHandler.fields.get(Constants.Fields.DST_ADDR));
      Assert.assertEquals(srcPort, queryHandler.fields.get(Constants.Fields.SRC_PORT));
      Assert.assertEquals(dstPort, queryHandler.fields.get(Constants.Fields.DST_PORT));
      Assert.assertEquals(TimestampConverters.MILLISECONDS.toNanoseconds(startTime), queryHandler.beginNS);
      Assert.assertEquals(TimestampConverters.MILLISECONDS.toNanoseconds(endTime), queryHandler.endNS);
      Assert.assertEquals(includeReverseTraffic, Boolean.parseBoolean(queryHandler.fields.get(Constants.Fields.INCLUDES_REVERSE_TRAFFIC)));
    }
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
    restEndpoint.getPcapsByIdentifiers(srcIp, dstIp, protocol, srcPort, dstPort, startTime, endTime, includeReverseTraffic, null);
    Assert.assertEquals(new Path(ConfigurationUtil.getPcapOutputPath()), queryHandler.basePath);
    Assert.assertEquals(new Path(ConfigurationUtil.getTempQueryOutputPath()), queryHandler.baseOutputPath);
    Assert.assertEquals(srcIp, queryHandler.fields.get(Constants.Fields.SRC_ADDR));
    Assert.assertEquals(dstIp, queryHandler.fields.get(Constants.Fields.DST_ADDR));
    Assert.assertEquals(srcPort, queryHandler.fields.get(Constants.Fields.SRC_PORT));
    Assert.assertEquals(dstPort, queryHandler.fields.get(Constants.Fields.DST_PORT));
    Assert.assertEquals(TimestampConverters.MILLISECONDS.toNanoseconds(startTime), queryHandler.beginNS);
    Assert.assertEquals(TimestampConverters.MILLISECONDS.toNanoseconds(endTime), queryHandler.endNS);
    Assert.assertEquals(includeReverseTraffic, Boolean.getBoolean(queryHandler.fields.get(Constants.Fields.INCLUDES_REVERSE_TRAFFIC)));
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
    restEndpoint.getPcapsByIdentifiers(srcIp, dstIp, protocol, srcPort, dstPort, startTime, endTime, includeReverseTraffic, null);
    Assert.assertEquals(new Path(ConfigurationUtil.getPcapOutputPath()), queryHandler.basePath);
    Assert.assertEquals(new Path(ConfigurationUtil.getTempQueryOutputPath()), queryHandler.baseOutputPath);
    Assert.assertEquals(srcIp, queryHandler.fields.get(Constants.Fields.SRC_ADDR));
    Assert.assertEquals(dstIp, queryHandler.fields.get(Constants.Fields.DST_ADDR));
    Assert.assertEquals(srcPort, queryHandler.fields.get(Constants.Fields.SRC_PORT));
    Assert.assertEquals(dstPort, queryHandler.fields.get(Constants.Fields.DST_PORT));
    Assert.assertEquals(TimestampConverters.MILLISECONDS.toNanoseconds(startTime), queryHandler.beginNS);
    Assert.assertEquals(TimestampConverters.MILLISECONDS.toNanoseconds(endTime), queryHandler.endNS);
    Assert.assertEquals(includeReverseTraffic, Boolean.getBoolean(queryHandler.fields.get(Constants.Fields.INCLUDES_REVERSE_TRAFFIC)));
  }

  @Test
  public void testEmptyStartTime() throws Exception {
    String srcIp = "srcIp";
    String dstIp = null;
    String protocol = "protocol";
    String srcPort = "80";
    String dstPort = "100";
    long startTime = -1;
    long endTime = 1000;
    boolean includeReverseTraffic = false;
    restEndpoint.getPcapsByIdentifiers(srcIp, dstIp, protocol, srcPort, dstPort, startTime, endTime, includeReverseTraffic, null);
    Assert.assertEquals(new Path(ConfigurationUtil.getPcapOutputPath()), queryHandler.basePath);
    Assert.assertEquals(new Path(ConfigurationUtil.getTempQueryOutputPath()), queryHandler.baseOutputPath);
    Assert.assertEquals(srcIp, queryHandler.fields.get(Constants.Fields.SRC_ADDR));
    Assert.assertEquals(dstIp, queryHandler.fields.get(Constants.Fields.DST_ADDR));
    Assert.assertEquals(srcPort, queryHandler.fields.get(Constants.Fields.SRC_PORT));
    Assert.assertEquals(dstPort, queryHandler.fields.get(Constants.Fields.DST_PORT));
    Assert.assertEquals(0, queryHandler.beginNS);
    Assert.assertEquals(TimestampConverters.MILLISECONDS.toNanoseconds(endTime), queryHandler.endNS);
    Assert.assertEquals(includeReverseTraffic, Boolean.getBoolean(queryHandler.fields.get(Constants.Fields.INCLUDES_REVERSE_TRAFFIC)));
  }

  @Test
  public void testEmptyEndTime() throws Exception {
    String srcIp = "srcIp";
    String dstIp = null;
    String protocol = "protocol";
    String srcPort = "80";
    String dstPort = "100";
    long startTime = -1;
    long endTime = -1;
    boolean includeReverseTraffic = false;
    restEndpoint.getPcapsByIdentifiers(srcIp, dstIp, protocol, srcPort, dstPort, startTime, endTime, includeReverseTraffic, null);
    Assert.assertEquals(new Path(ConfigurationUtil.getPcapOutputPath()), queryHandler.basePath);
    Assert.assertEquals(new Path(ConfigurationUtil.getTempQueryOutputPath()), queryHandler.baseOutputPath);
    Assert.assertEquals(srcIp, queryHandler.fields.get(Constants.Fields.SRC_ADDR));
    Assert.assertEquals(dstIp, queryHandler.fields.get(Constants.Fields.DST_ADDR));
    Assert.assertEquals(srcPort, queryHandler.fields.get(Constants.Fields.SRC_PORT));
    Assert.assertEquals(dstPort, queryHandler.fields.get(Constants.Fields.DST_PORT));
    Assert.assertEquals(0, queryHandler.beginNS);
    Assert.assertTrue(queryHandler.endNS > 0);
    Assert.assertEquals(includeReverseTraffic, Boolean.getBoolean(queryHandler.fields.get(Constants.Fields.INCLUDES_REVERSE_TRAFFIC)));
  }

}
