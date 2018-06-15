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
  private String ipSrcPort;
  private String ipDstPort;
  private String protocol;
  private String packetFilter;
  private String includeReverse;

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

  public String getIpSrcPort() {
    return ipSrcPort;
  }

  public void setIpSrcPort(String ipSrcPort) {
    this.ipSrcPort = ipSrcPort;
  }

  public String getIpDstPort() {
    return ipDstPort;
  }

  public void setIpDstPort(String ipDstPort) {
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

  public String getIncludeReverse() {
    return includeReverse;
  }

  public void setIncludeReverse(String includeReverse) {
    this.includeReverse = includeReverse;
  }
}
