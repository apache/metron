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
import org.apache.hadoop.fs.Path;

public class PcapHelper {


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

  public static byte[] headerizeIfNecessary(byte[] packet) {
    if( packet[0] == PartitionHDFSWriter.PCAP_GLOBAL_HEADER[0]
    &&  packet[1] == PartitionHDFSWriter.PCAP_GLOBAL_HEADER[1]
    &&  packet[2] == PartitionHDFSWriter.PCAP_GLOBAL_HEADER[2]
    &&  packet[3] == PartitionHDFSWriter.PCAP_GLOBAL_HEADER[3]
      )
    {
      //if we match the pcap magic number, then we don't need to add the header.
      return packet;
    }
    else {
      byte[] ret = new byte[packet.length + PartitionHDFSWriter.PCAP_GLOBAL_HEADER.length];
      int offset = 0;
      System.arraycopy(PartitionHDFSWriter.PCAP_GLOBAL_HEADER, 0, ret, offset, PartitionHDFSWriter.PCAP_GLOBAL_HEADER.length);
      offset += PartitionHDFSWriter.PCAP_GLOBAL_HEADER.length;
      System.arraycopy(packet, 0, ret, offset, packet.length);
      return ret;
    }
  }
}
