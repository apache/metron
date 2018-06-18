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

public class FixedPcapRequest extends PcapRequest {

  private String ipSrcAddr;
  private String ipDstAddr;
  private Integer ipSrcPort;
  private Integer ipDstPort;
  private String protocol;
  private String packetFilter;
  private Boolean includeReverse = false;

  public String getIpSrcAddr() {
    return ipSrcAddr;
  }

  public void setIpSrcAddr(String ipSrcAddr) {
    this.ipSrcAddr = ipSrcAddr;
  }

  public String getIpDstAddr() {
    return ipDstAddr;
  }

  public void setIpDstAddr(String ipDstAddr) {
    this.ipDstAddr = ipDstAddr;
  }

  public Integer getIpSrcPort() {
    return ipSrcPort;
  }

  public void setIpSrcPort(Integer ipSrcPort) {
    this.ipSrcPort = ipSrcPort;
  }

  public Integer getIpDstPort() {
    return ipDstPort;
  }

  public void setIpDstPort(Integer ipDstPort) {
    this.ipDstPort = ipDstPort;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public String getPacketFilter() {
    return packetFilter;
  }

  public void setPacketFilter(String packetFilter) {
    this.packetFilter = packetFilter;
  }

  public Boolean getIncludeReverse() {
    return includeReverse;
  }

  public void setIncludeReverse(Boolean includeReverse) {
    this.includeReverse = includeReverse;
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
