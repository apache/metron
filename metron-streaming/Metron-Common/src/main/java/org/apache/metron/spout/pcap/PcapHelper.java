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

package org.apache.metron.spout.pcap;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;
import org.apache.metron.pcap.PcapByteInputStream;
import org.krakenapps.pcap.file.GlobalHeader;
import org.krakenapps.pcap.packet.PacketHeader;
import org.krakenapps.pcap.packet.PcapPacket;
import org.krakenapps.pcap.util.ByteOrderConverter;

import javax.xml.bind.annotation.XmlElementDecl;
import java.io.IOException;

public class PcapHelper {

  public static final int PACKET_HEADER_SIZE = 4*Integer.BYTES;
  public static final int GLOBAL_HEADER_SIZE = 24;
  private static final Logger LOG = Logger.getLogger(PcapHelper.class);

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

  public static boolean swapBytes(Endianness endianness) {
    return endianness == Endianness.LITTLE;
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
              , 0x00, 0x04 //swapped minor version 4
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
}
