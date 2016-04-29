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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;
import org.apache.metron.spout.pcap.Endianness;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.krakenapps.pcap.decoder.ethernet.EthernetType;
import org.krakenapps.pcap.decoder.ip.IpDecoder;
import org.krakenapps.pcap.decoder.ip.Ipv4Packet;
import org.krakenapps.pcap.decoder.tcp.TcpPacket;
import org.krakenapps.pcap.decoder.udp.UdpPacket;
import org.krakenapps.pcap.file.GlobalHeader;
import org.krakenapps.pcap.packet.PacketHeader;
import org.krakenapps.pcap.packet.PcapPacket;
import org.krakenapps.pcap.util.Buffer;
import org.krakenapps.pcap.util.ByteOrderConverter;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static org.apache.metron.pcap.Constants.*;

public class PcapHelper {

  public static final int PACKET_HEADER_SIZE = 4*Integer.BYTES;
  public static final int GLOBAL_HEADER_SIZE = 24;
  private static final Logger LOG = Logger.getLogger(PcapHelper.class);
  public static ThreadLocal<MetronEthernetDecoder> ETHERNET_DECODER = new ThreadLocal<MetronEthernetDecoder>() {
    @Override
    protected MetronEthernetDecoder initialValue() {
      return createDecoder();
    }
  };

  public static Long getTimestamp(String filename) {
    try {
      return Long.parseUnsignedLong(Iterables.get(Splitter.on('_').split(filename), 2));
    }
    catch(Exception e) {
      //something went wrong here.
      return null;
    }
  }

  public static String toFilename(String topic, long timestamp, String partition, String uuid)
  {
    return Joiner.on("_").join("pcap"
                              ,topic
                              , Long.toUnsignedString(timestamp)
                              ,partition
                              , uuid
                              );
  }

  public static boolean swapBytes(org.apache.metron.spout.pcap.Endianness endianness) {
    return endianness == org.apache.metron.spout.pcap.Endianness.LITTLE;
  }

  public static byte[] getPcapGlobalHeader(Endianness endianness) {
    if(swapBytes(endianness)) {
      //swap
      return new byte[] {
              (byte) 0xd4, (byte) 0xc3, (byte) 0xb2, (byte) 0xa1 //swapped magic number 0xa1b2c3d4
              , 0x02, 0x00 //swapped major version 2
              , 0x04, 0x00 //swapped minor version 4
              , 0x00, 0x00, 0x00, 0x00 //GMT to local tz offset (= 0)
              , 0x00, 0x00, 0x00, 0x00 //sigfigs (= 0)
              , (byte) 0xff, (byte) 0xff, 0x00, 0x00 //snaplen (=65535)
              , 0x01, 0x00, 0x00, 0x00 // swapped link layer header type (1 = ethernet)
                        };
    }
    else {
      //no need to swap
      return new byte[] {
              (byte) 0xa1, (byte) 0xb2, (byte) 0xc3, (byte) 0xd4 //magic number 0xa1b2c3d4
              , 0x00, 0x02 //major version 2
              , 0x00, 0x04 //minor version 4
              , 0x00, 0x00, 0x00, 0x00 //GMT to local tz offset (= 0)
              , 0x00, 0x00, 0x00, 0x00 //sigfigs (= 0)
              , 0x00, 0x00, (byte) 0xff, (byte) 0xff //snaplen (=65535)
              , 0x00, 0x00, 0x00, 0x01 // link layer header type (1 = ethernet)
                        };
    }
  }

  public static Long getTimestamp(byte[] pcap) {
    PcapByteInputStream pcapByteInputStream = null;
    try {
      pcapByteInputStream = new PcapByteInputStream(pcap);
      PcapPacket packet = pcapByteInputStream.getPacket();
      GlobalHeader globalHeader = pcapByteInputStream.getGlobalHeader();
      PacketHeader packetHeader = packet.getPacketHeader();
      if ( globalHeader.getMagicNumber() == 0xa1b2c3d4 || globalHeader.getMagicNumber() == 0xd4c3b2a1 )
      {
        //Time is in micro assemble as nano
        LOG.info("Times are in micro according to the magic number");
        return packetHeader.getTsSec() * 1000000000L + packetHeader.getTsUsec() * 1000L ;
      }
      else if ( globalHeader.getMagicNumber() == 0xa1b23c4d || globalHeader.getMagicNumber() == 0x4d3cb2a1 ) {
        //Time is in nano assemble as nano
        LOG.info("Times are in nano according to the magic number");
        return packetHeader.getTsSec() * 1000000000L + packetHeader.getTsUsec() ;
      }
      //Default assume time is in micro assemble as nano
      LOG.warn("Unknown magic number. Defaulting to micro");
      return packetHeader.getTsSec() * 1000000000L + packetHeader.getTsUsec() * 1000L ;
    }
    catch(IOException ioe) {
      //we cannot read the packet, so we return null here.
      LOG.error("Unable to read packet", ioe);
    }
    finally {
      if(pcapByteInputStream != null) {
        try {
          pcapByteInputStream.close();
        } catch (IOException e) {
          LOG.error("Unable to close stream", e);
        }
      }
    }
    return null;
  }
  public static byte[] addGlobalHeader(byte[] packet, Endianness endianness) {
    byte[] globalHeader = getPcapGlobalHeader(endianness);
    byte[] ret = new byte[packet.length + GLOBAL_HEADER_SIZE];
    int offset = 0;
    System.arraycopy(globalHeader, 0, ret, offset, GLOBAL_HEADER_SIZE);
    offset += globalHeader.length;
    System.arraycopy(packet, 0, ret, offset, packet.length);
    return ret;
  }

  public static byte[] addPacketHeader(long tsNano, byte[] packet, Endianness endianness) {
    boolean swapBytes = swapBytes(endianness);
    long micros = Long.divideUnsigned(tsNano, 1000);
    int secs = (int)(micros / 1000000);
    int usec = (int)(micros % 1000000);
    int capLen = packet.length;
    byte[] ret = new byte[PACKET_HEADER_SIZE + packet.length];
    int offset = 0;
    {
      byte[] b = Bytes.toBytes(swapBytes?ByteOrderConverter.swap(secs):secs);
      System.arraycopy(b, 0, ret, offset, Integer.BYTES);
      offset += Integer.BYTES;
    }
    {
      byte[] b = Bytes.toBytes(swapBytes?ByteOrderConverter.swap(usec):usec);
      System.arraycopy(b, 0, ret, offset, Integer.BYTES);
      offset += Integer.BYTES;
    }
    {
      byte[] b = Bytes.toBytes(swapBytes?ByteOrderConverter.swap(capLen):capLen);
      System.arraycopy(b, 0, ret, offset, Integer.BYTES);
      offset += Integer.BYTES;
    }
    {
      byte[] b = Bytes.toBytes(swapBytes?ByteOrderConverter.swap(capLen):capLen);
      System.arraycopy(b, 0, ret, offset, Integer.BYTES);
      offset += Integer.BYTES;
    }
    System.arraycopy(packet, 0, ret, offset, packet.length);
    return ret;
  }
  public static EnumMap<org.apache.metron.common.Constants.Fields, Object> packetToFields(PacketInfo pi) {
    EnumMap<org.apache.metron.common.Constants.Fields, Object> ret = new EnumMap(org.apache.metron.common.Constants.Fields.class);
    if(pi.getTcpPacket() != null) {
      if(pi.getTcpPacket().getSourceAddress() != null) {
        ret.put(org.apache.metron.common.Constants.Fields.SRC_ADDR, pi.getTcpPacket().getSourceAddress().getHostAddress());
      }
      if(pi.getTcpPacket().getSource() != null ) {
        ret.put(org.apache.metron.common.Constants.Fields.SRC_PORT, pi.getTcpPacket().getSource().getPort());
      }
      if(pi.getTcpPacket().getDestinationAddress() != null ) {
        ret.put(org.apache.metron.common.Constants.Fields.DST_ADDR, pi.getTcpPacket().getDestinationAddress().getHostAddress());
      }
      if(pi.getTcpPacket().getDestination() != null ) {
        ret.put(org.apache.metron.common.Constants.Fields.DST_PORT, pi.getTcpPacket().getDestination().getPort());
      }
      if(pi.getIpv4Packet() != null) {
        ret.put(org.apache.metron.common.Constants.Fields.PROTOCOL, pi.getIpv4Packet().getProtocol());
      }
    }
    return ret;
  }

  public static List<PacketInfo> toPacketInfo(byte[] packet) throws IOException {
    return toPacketInfo(ETHERNET_DECODER.get(), packet);
  }
  public static MetronEthernetDecoder createDecoder() {
    MetronEthernetDecoder ethernetDecoder = new MetronEthernetDecoder();
    IpDecoder ipDecoder = new IpDecoder();
    ethernetDecoder.register(EthernetType.IPV4, ipDecoder);
    return ethernetDecoder;
  }

/**
   * Parses the.
   *
   * @param pcap
   *          the pcap
   * @return the list * @throws IOException Signals that an I/O exception has
   *         occurred. * @throws IOException * @throws IOException * @throws
   *         IOException
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public static List<PacketInfo> toPacketInfo(MetronEthernetDecoder decoder, byte[] pcap) throws IOException {
    List<PacketInfo> packetInfoList = new ArrayList<>();

    PcapByteInputStream pcapByteInputStream = new PcapByteInputStream(pcap);

    GlobalHeader globalHeader = pcapByteInputStream.getGlobalHeader();
    while (true) {
      try

      {
        PcapPacket packet = pcapByteInputStream.getPacket();
        // int packetCounter = 0;
        // PacketHeader packetHeader = null;
        // Ipv4Packet ipv4Packet = null;
        TcpPacket tcpPacket = null;
        UdpPacket udpPacket = null;
        // Buffer packetDataBuffer = null;
        int sourcePort = 0;
        int destinationPort = 0;

        // LOG.trace("Got packet # " + ++packetCounter);

        // LOG.trace(packet.getPacketData());
        decoder.decode(packet);

        PacketHeader packetHeader = packet.getPacketHeader();
        Ipv4Packet ipv4Packet = Ipv4Packet.parse(packet.getPacketData());

        if (ipv4Packet.getProtocol() == Constants.PROTOCOL_TCP) {
          tcpPacket = TcpPacket.parse(ipv4Packet);

        }

        if (ipv4Packet.getProtocol() == Constants.PROTOCOL_UDP) {

          Buffer packetDataBuffer = ipv4Packet.getData();
          sourcePort = packetDataBuffer.getUnsignedShort();
          destinationPort = packetDataBuffer.getUnsignedShort();

          udpPacket = new UdpPacket(ipv4Packet, sourcePort, destinationPort);

          udpPacket.setLength(packetDataBuffer.getUnsignedShort());
          udpPacket.setChecksum(packetDataBuffer.getUnsignedShort());
          packetDataBuffer.discardReadBytes();
          udpPacket.setData(packetDataBuffer);
        }

        packetInfoList.add(new PacketInfo(globalHeader, packetHeader, packet,
            ipv4Packet, tcpPacket, udpPacket));
      } catch (NegativeArraySizeException ignored) {
        LOG.debug("Ignorable exception while parsing packet.", ignored);
      } catch (EOFException eof) { // $codepro.audit.disable logExceptions
        // Ignore exception and break
        break;
      }
    }
    return packetInfoList;
  }

  public static List<JSONObject> toJSON(List<PacketInfo> packetInfoList) {
    List<JSONObject> messages = new ArrayList<>();
    for (PacketInfo packetInfo : packetInfoList) {
      JSONObject message = (JSONObject) JSONValue.parse(packetInfo.getJsonIndexDoc());
      messages.add(message);
    }
    return messages;
  }
}
