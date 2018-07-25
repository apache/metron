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
package org.apache.metron.rest.model.pcap;

import org.apache.metron.common.Constants;
import org.apache.metron.pcap.config.PcapOptions;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.filter.fixed.FixedPcapFilter;

import java.util.HashMap;
import java.util.Map;

public class FixedPcapRequest extends PcapRequest {

  public FixedPcapRequest() {
    PcapOptions.FILTER_IMPL.put(this, new FixedPcapFilter.Configurator());
  }

  public String getIpSrcAddr() {
    return FixedPcapOptions.IP_SRC_ADDR.get(this, String.class);
  }

  public void setIpSrcAddr(String ipSrcAddr) {
    FixedPcapOptions.IP_SRC_ADDR.put(this, ipSrcAddr);
  }

  public String getIpDstAddr() {
    return FixedPcapOptions.IP_DST_ADDR.get(this, String.class);
  }

  public void setIpDstAddr(String ipDstAddr) {
    FixedPcapOptions.IP_DST_ADDR.put(this, ipDstAddr);
  }

  public Integer getIpSrcPort() {
    return FixedPcapOptions.IP_SRC_PORT.get(this, Integer.class);
  }

  public void setIpSrcPort(Integer ipSrcPort) {
    FixedPcapOptions.IP_SRC_PORT.put(this, ipSrcPort);
  }

  public Integer getIpDstPort() {
    return FixedPcapOptions.IP_DST_PORT.get(this, Integer.class);
  }

  public void setIpDstPort(Integer ipDstPort) {
    FixedPcapOptions.IP_DST_PORT.put(this, ipDstPort);
  }

  public String getProtocol() {
    return FixedPcapOptions.PROTOCOL.get(this, String.class);
  }

  public void setProtocol(String protocol) {
    FixedPcapOptions.PROTOCOL.put(this, protocol);
  }

  public String getPacketFilter() {
    return FixedPcapOptions.PACKET_FILTER.get(this, String.class);
  }

  public void setPacketFilter(String packetFilter) {
    FixedPcapOptions.PACKET_FILTER.put(this, packetFilter);
  }

  public Boolean getIncludeReverse() {
    return FixedPcapOptions.INCLUDE_REVERSE.get(this, Boolean.class);
  }

  public void setIncludeReverse(Boolean includeReverse) {
    FixedPcapOptions.INCLUDE_REVERSE.put(this, includeReverse);
  }

  @Override
  public void setFields() {
    Map<String, String> fields = new HashMap<>();
    if (getIpSrcAddr() != null) {
      fields.put(Constants.Fields.SRC_ADDR.getName(), getIpSrcAddr());
    }
    if (getIpDstAddr() != null) {
      fields.put(Constants.Fields.DST_ADDR.getName(), getIpDstAddr());
    }
    if (getIpSrcPort() != null) {
      fields.put(Constants.Fields.SRC_PORT.getName(), getIpSrcPort().toString());
    }
    if (getIpDstPort() != null) {
      fields.put(Constants.Fields.DST_PORT.getName(), getIpDstPort().toString());
    }
    if (getProtocol() != null) {
      fields.put(Constants.Fields.PROTOCOL.getName(), getProtocol());
    }
    if (getIncludeReverse() != null) {
      fields.put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName(), getIncludeReverse().toString());
    }
    if (getPacketFilter() != null) {
      fields.put(PcapHelper.PacketFields.PACKET_FILTER.getName(), getPacketFilter());
    }
    PcapOptions.FIELDS.put(this, fields);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FixedPcapRequest fixedPcapRequest = (FixedPcapRequest) o;

    return (super.equals(o)) &&
            (getIpSrcAddr() != null ? getIpSrcAddr().equals(fixedPcapRequest.getIpSrcAddr()) : fixedPcapRequest.getIpSrcAddr() != null) &&
            (getIpDstAddr() != null ? getIpDstAddr().equals(fixedPcapRequest.getIpDstAddr()) : fixedPcapRequest.getIpDstAddr() != null) &&
            (getIpSrcPort() != null ? getIpSrcPort().equals(fixedPcapRequest.getIpSrcPort()) : fixedPcapRequest.getIpSrcPort() != null) &&
            (getIpDstPort() != null ? getIpDstPort().equals(fixedPcapRequest.getIpDstPort()) : fixedPcapRequest.getIpDstPort() != null) &&
            (getProtocol() != null ? getProtocol().equals(fixedPcapRequest.getProtocol()) : fixedPcapRequest.getProtocol() != null) &&
            (getPacketFilter() != null ? getPacketFilter().equals(fixedPcapRequest.getPacketFilter()) : fixedPcapRequest.getPacketFilter() != null) &&
            (getIncludeReverse() != null ? getIncludeReverse().equals(fixedPcapRequest.getIncludeReverse()) : fixedPcapRequest.getIncludeReverse() != null);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (getIpSrcAddr() != null ? getIpSrcAddr().hashCode() : 0);
    result = 31 * result + (getIpDstAddr() != null ? getIpDstAddr().hashCode() : 0);
    result = 31 * result + (getIpSrcPort() != null ? getIpSrcPort().hashCode() : 0);
    result = 31 * result + (getIpDstPort() != null ? getIpDstPort().hashCode() : 0);
    result = 31 * result + (getProtocol() != null ? getProtocol().hashCode() : 0);
    result = 31 * result + (getPacketFilter() != null ? getPacketFilter().hashCode() : 0);
    result = 31 * result + (getIncludeReverse() != null ? getIncludeReverse().hashCode() : 0);
    return result;
  }
}
