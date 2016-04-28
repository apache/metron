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

package org.apache.metron.pcap.mr;

import com.google.common.base.Predicate;
import org.apache.metron.common.Constants;
import org.apache.metron.pcap.PacketInfo;
import org.apache.metron.pcap.PcapHelper;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;


public class PcapFilter implements Predicate<PacketInfo> {

  private String srcAddr;
  private Integer srcPort;
  private String dstAddr;
  private Integer dstPort;
  private String protocol;
  private boolean includesReverseTraffic = false;


  public PcapFilter(Iterable<Map.Entry<String, String>> config) {
    for(Map.Entry<String, String> kv : config) {
      if(kv.getKey().equals(Constants.Fields.DST_ADDR.getName())) {
        this.dstAddr = kv.getValue();
      }
      if(kv.getKey().equals(Constants.Fields.SRC_ADDR.getName())) {
        this.srcAddr = kv.getValue();
      }
      if(kv.getKey().equals(Constants.Fields.DST_PORT.getName())) {
        this.dstPort = Integer.parseInt(kv.getValue());
      }
      if(kv.getKey().equals(Constants.Fields.SRC_PORT.getName())) {
        this.srcPort = Integer.parseInt(kv.getValue());
      }
      if(kv.getKey().equals(Constants.Fields.PROTOCOL.getName())) {
        this.protocol= kv.getValue();
      }
      if(kv.getKey().equals(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName())) {
        this.includesReverseTraffic = Boolean.parseBoolean(kv.getValue());
      }
    }
  }

  private boolean matchSourceAndDestination(Object srcAddrObj
                                           , Object srcPortObj
                                           , Object dstAddrObj
                                           , Object dstPortObj
                                            )
  {
    boolean isMatch = true;
    if(srcAddr != null ) {
      Object o = srcAddrObj;
      isMatch &= o != null && o instanceof String && ((String)o).equals(srcAddr);
    }
    if(isMatch && srcPort != null ) {
      Object o = srcPortObj;
      isMatch &= o != null && o.toString().equals(srcPort.toString());
    }
    if(isMatch && dstAddr != null ) {
      Object o = dstAddrObj;
      isMatch &= o != null &&  o instanceof String && ((String)o).equals(dstAddr);
    }
    if(isMatch && dstPort != null) {
      Object o = dstPortObj;
      isMatch &= o != null && o.toString().equals(dstPort.toString());
    }
    return isMatch;
  }


  protected EnumMap<Constants.Fields, Object> packetToFields(PacketInfo pi) {
    return PcapHelper.packetToFields(pi);
  }

  @Override
  public boolean apply(@Nullable PacketInfo pi ) {
    boolean isMatch = true;
    EnumMap<Constants.Fields, Object> input= packetToFields(pi);
    Object srcAddrObj = input.get(Constants.Fields.SRC_ADDR);
    Object srcPortObj = input.get(Constants.Fields.SRC_PORT);
    Object dstAddrObj = input.get(Constants.Fields.DST_ADDR);
    Object dstPortObj = input.get(Constants.Fields.DST_PORT);
    Object protocolObj = input.get(Constants.Fields.PROTOCOL);

    //first we ensure the protocol matches if you pass one in
    if(isMatch && protocol != null ) {
      Object o = protocolObj;
      isMatch &= o != null && o.toString().equals(protocol);
    }
    if(isMatch) {
      //if we're still a match, then we try to match the source and destination
      isMatch &= matchSourceAndDestination(srcAddrObj, srcPortObj, dstAddrObj, dstPortObj);
      if (!isMatch && includesReverseTraffic) {
        isMatch = true;
        //then we have to try the other direction if that the forward direction isn't a match
        isMatch &= matchSourceAndDestination(dstAddrObj, dstPortObj, srcAddrObj, srcPortObj);
      }
    }
    return isMatch;
  }
}
