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

package org.apache.metron.pcap.filter.fixed;

import com.google.common.base.Joiner;
import org.apache.hadoop.conf.Configuration;
import org.apache.metron.common.Constants;
import org.apache.metron.common.dsl.VariableResolver;
import org.apache.metron.pcap.PacketInfo;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.filter.PcapFilter;
import org.apache.metron.pcap.filter.PcapFilterConfigurator;
import org.apache.metron.pcap.filter.PcapFilters;
import org.apache.metron.pcap.filter.PcapFieldResolver;

import java.util.EnumMap;
import java.util.Map;


public class FixedPcapFilter implements PcapFilter {

  public static class Configurator implements PcapFilterConfigurator<EnumMap<Constants.Fields, String>> {
    @Override
    public void addToConfig(EnumMap<Constants.Fields, String> fields, Configuration conf) {
      for (Map.Entry<Constants.Fields, String> kv : fields.entrySet()) {
        conf.set(kv.getKey().getName(), kv.getValue());
      }
      conf.set(PCAP_FILTER_NAME_CONF, PcapFilters.FIXED.name());
    }

    @Override
    public String queryToString(EnumMap<Constants.Fields, String> fields) {
      return (fields == null ? "" : Joiner.on("_").join(fields.values()));
    }
  }

  private String srcAddr;
  private Integer srcPort;
  private String dstAddr;
  private Integer dstPort;
  private String protocol;
  private boolean includesReverseTraffic = false;

  @Override
  public void configure(Iterable<Map.Entry<String, String>> config) {
    for (Map.Entry<String, String> kv : config) {
      if (kv.getKey().equals(Constants.Fields.DST_ADDR.getName())) {
        this.dstAddr = kv.getValue();
      }
      if (kv.getKey().equals(Constants.Fields.SRC_ADDR.getName())) {
        this.srcAddr = kv.getValue();
      }
      if (kv.getKey().equals(Constants.Fields.DST_PORT.getName())) {
        this.dstPort = Integer.parseInt(kv.getValue());
      }
      if (kv.getKey().equals(Constants.Fields.SRC_PORT.getName())) {
        this.srcPort = Integer.parseInt(kv.getValue());
      }
      if (kv.getKey().equals(Constants.Fields.PROTOCOL.getName())) {
        this.protocol = kv.getValue();
      }
      if (kv.getKey().equals(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName())) {
        this.includesReverseTraffic = Boolean.parseBoolean(kv.getValue());
      }
    }
  }


  @Override
  public boolean test(PacketInfo pi) {
    VariableResolver resolver = new PcapFieldResolver(packetToFields(pi));
    String srcAddrIn = (String) resolver.resolve(Constants.Fields.SRC_ADDR.getName());
    String srcPortIn = (String) resolver.resolve(Constants.Fields.SRC_PORT.getName());
    String dstAddrIn = (String) resolver.resolve(Constants.Fields.DST_ADDR.getName());
    String dstPortIn = (String) resolver.resolve(Constants.Fields.DST_PORT.getName());
    String protocolIn = (String) resolver.resolve(Constants.Fields.PROTOCOL.getName());

    if (areMatch(protocol, protocolIn)) {
      if (matchesSourceAndDestination(srcAddrIn, srcPortIn, dstAddrIn, dstPortIn)) {
        return true;
      } else if (includesReverseTraffic) {
        return matchesReverseSourceAndDestination(srcAddrIn, srcPortIn, dstAddrIn, dstPortIn);
      }
    }
    return false;
  }

  private boolean areMatch(Integer filter, String input) {
    return filter == null || areMatch(filter.toString(), input);
  }

  private boolean areMatch(String filter, String input) {
    if (filter != null) {
      return input != null && input.equals(filter);
    } else {
      return true;
    }
  }

  protected EnumMap<Constants.Fields, Object> packetToFields(PacketInfo pi) {
    return PcapHelper.packetToFields(pi);
  }

  private boolean matchesSourceAndDestination(String srcAddrComp,
                                              String srcPortComp,
                                              String dstAddrComp,
                                              String dstPortComp) {
    boolean isMatch = true;
    isMatch &= areMatch(this.srcAddr, srcAddrComp);
    isMatch &= areMatch(this.srcPort, srcPortComp);
    isMatch &= areMatch(this.dstAddr, dstAddrComp);
    isMatch &= areMatch(this.dstPort, dstPortComp);
    return isMatch;
  }

  private boolean matchesReverseSourceAndDestination(String srcAddr,
                                                     String srcPort,
                                                     String dstAddr,
                                                     String dstPort) {
    return matchesSourceAndDestination(dstAddr, dstPort, srcAddr, srcPort);
  }

}
