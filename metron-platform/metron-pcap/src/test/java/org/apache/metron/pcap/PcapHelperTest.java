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

package org.apache.metron.pcap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.metron.spout.pcap.Endianness;
import org.junit.Assert;
import org.junit.Test;
import org.krakenapps.pcap.decoder.ip.Ipv4Packet;
import org.krakenapps.pcap.decoder.tcp.TcpPacket;
import org.krakenapps.pcap.decoder.udp.UdpPacket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.apache.metron.common.Constants.Fields;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PcapHelperTest {
  public static List<byte[]> readSamplePackets(String pcapLoc) throws IOException {
    SequenceFile.Reader reader = new SequenceFile.Reader(new Configuration(),
            SequenceFile.Reader.file(new Path(pcapLoc))
    );
    List<byte[] > ret = new ArrayList<>();
    IntWritable key = new IntWritable();
    BytesWritable value = new BytesWritable();
    while (reader.next(key, value)) {
      byte[] pcapWithHeader = value.copyBytes();
      ret.add(pcapWithHeader);
    }
    return ret;
  }

  public static byte[] stripHeaders(byte[] pcap) {
    byte[] ret = new byte[pcap.length - PcapHelper.GLOBAL_HEADER_SIZE - PcapHelper.PACKET_HEADER_SIZE];
    int offset = PcapHelper.GLOBAL_HEADER_SIZE + PcapHelper.PACKET_HEADER_SIZE;
    System.arraycopy(pcap, offset, ret, 0, ret.length);
    return ret;
  }

  @Test
  public void testLittleEndianHeaderization() throws Exception {
    String pcapSampleFiles = "../metron-integration-test/src/main/sample/data/SampleInput/PCAPExampleOutput";
    List<byte[]> pcaps = readSamplePackets(pcapSampleFiles);
    for(byte[] pcap : pcaps)
    {
      long ts = PcapHelper.getTimestamp(pcap);
      byte[] stripped = stripHeaders(pcap);
      byte[] reconstitutedPacket = PcapHelper.addGlobalHeader(PcapHelper.addPacketHeader(ts, stripped, Endianness.getNativeEndianness()), Endianness.getNativeEndianness());
      if(!Arrays.equals(reconstitutedPacket, pcap)) {
        int eSecs = Bytes.toInt(pcap, 25);
        int rSec = Bytes.toInt(reconstitutedPacket, 25);
        System.out.println(eSecs + " vs " + rSec);
        for(int i = 0;i < reconstitutedPacket.length;++i) {
          System.out.println((i + 1) + ". " + String.format("%02X", pcap[i]) + " = " + String.format("%02X", reconstitutedPacket[i]));
        }
        Assert.assertArrayEquals(reconstitutedPacket, pcap);
      }
    }
  }

  @Test
  public void packetToFieldsShouldProperlyParserTcpPackets() throws Exception {
    PacketInfo packetInfo = mock(PacketInfo.class);
    when(packetInfo.getPacketBytes()).thenReturn("packet bytes".getBytes(StandardCharsets.UTF_8));
    TcpPacket tcpPacket = mock(TcpPacket.class);
    // Tcp source address and port
    InetAddress tcpSourceInetAddress = mock(InetAddress.class);
    when(tcpSourceInetAddress.getHostAddress()).thenReturn("tcp source address");
    when(tcpPacket.getSourceAddress()).thenReturn(tcpSourceInetAddress);
    InetSocketAddress tcpSourceInetSocketAddress = new InetSocketAddress(22);
    when(tcpPacket.getSource()).thenReturn(tcpSourceInetSocketAddress);
    // Tcp destination address and port
    InetAddress tcpDestinationInetAddress = mock(InetAddress.class);
    when(tcpDestinationInetAddress.getHostAddress()).thenReturn("tcp destination address");
    when(tcpPacket.getDestinationAddress()).thenReturn(tcpDestinationInetAddress);
    InetSocketAddress tcpDestinationInetSocketAddress = new InetSocketAddress(55791);
    when(tcpPacket.getDestination()).thenReturn(tcpDestinationInetSocketAddress);
    when(packetInfo.getTcpPacket()).thenReturn(tcpPacket);

    Ipv4Packet ipv4Packet = mock(Ipv4Packet.class);
    when(ipv4Packet.getProtocol()).thenReturn(6);
    when(packetInfo.getIpv4Packet()).thenReturn(ipv4Packet);

    Map<String, Object> actualFields = PcapHelper.packetToFields(packetInfo);
    Assert.assertArrayEquals("packet bytes".getBytes(StandardCharsets.UTF_8),
            (byte[]) actualFields.get(PcapHelper.PacketFields.PACKET_DATA.getName()));
    Assert.assertEquals("tcp source address", actualFields.get(Fields.SRC_ADDR.getName()));
    Assert.assertEquals(22, actualFields.get(Fields.SRC_PORT.getName()));
    Assert.assertEquals("tcp destination address", actualFields.get(Fields.DST_ADDR.getName()));
    Assert.assertEquals(55791, actualFields.get(Fields.DST_PORT.getName()));
    Assert.assertEquals(6, actualFields.get(Fields.PROTOCOL.getName()));
  }

  @Test
  public void packetToFieldsShouldProperlyParserUdpPackets() throws Exception {
    PacketInfo packetInfo = mock(PacketInfo.class);
    when(packetInfo.getPacketBytes()).thenReturn("packet bytes".getBytes(StandardCharsets.UTF_8));

    UdpPacket udpPacket = mock(UdpPacket.class);
    // Udp source address and port
    InetAddress udpSourceInetAddress = mock(InetAddress.class);
    when(udpSourceInetAddress.getHostAddress()).thenReturn("udp source address");
    InetSocketAddress udpSourceInetSocketAddress = new InetSocketAddress(udpSourceInetAddress, 68);
    when(udpPacket.getSource()).thenReturn(udpSourceInetSocketAddress);
    // Udp destination address and port
    InetAddress udpDestinationInetAddress = mock(InetAddress.class);
    when(udpDestinationInetAddress.getHostAddress()).thenReturn("udp destination address");
    InetSocketAddress udpDestinationInetSocketAddress = new InetSocketAddress(udpDestinationInetAddress, 67);
    when(udpPacket.getDestination()).thenReturn(udpDestinationInetSocketAddress);
    when(packetInfo.getUdpPacket()).thenReturn(udpPacket);

    Ipv4Packet ipv4Packet = mock(Ipv4Packet.class);
    when(ipv4Packet.getProtocol()).thenReturn(17);
    when(packetInfo.getIpv4Packet()).thenReturn(ipv4Packet);

    Map<String, Object> actualFields = PcapHelper.packetToFields(packetInfo);
    Assert.assertArrayEquals("packet bytes".getBytes(StandardCharsets.UTF_8),
            (byte[]) actualFields.get(PcapHelper.PacketFields.PACKET_DATA.getName()));
    Assert.assertEquals("udp source address", actualFields.get(Fields.SRC_ADDR.getName()));
    Assert.assertEquals(68, actualFields.get(Fields.SRC_PORT.getName()));
    Assert.assertEquals("udp destination address", actualFields.get(Fields.DST_ADDR.getName()));
    Assert.assertEquals(67, actualFields.get(Fields.DST_PORT.getName()));
    Assert.assertEquals(17, actualFields.get(Fields.PROTOCOL.getName()));
  }
}
