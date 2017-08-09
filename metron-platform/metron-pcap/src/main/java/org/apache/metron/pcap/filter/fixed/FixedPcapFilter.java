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
import org.apache.metron.stellar.dsl.MapVariableResolver;
import org.apache.metron.stellar.dsl.VariableResolver;
import org.apache.metron.pcap.PacketInfo;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.filter.PcapFilter;
import org.apache.metron.pcap.filter.PcapFilterConfigurator;
import org.apache.metron.pcap.filter.PcapFilters;
import org.apache.metron.pcap.pattern.ByteArrayMatchingUtil;

import javax.xml.bind.DatatypeConverter;
import java.util.Map;
import java.util.concurrent.ExecutionException;


public class FixedPcapFilter implements PcapFilter {

  public static class Configurator implements PcapFilterConfigurator<Map<String, String>> {
    @Override
    public void addToConfig(Map<String, String> fields, Configuration conf) {
      for (Map.Entry<String, String> kv : fields.entrySet()) {
        conf.set(kv.getKey(), kv.getValue());
      }
      conf.set(PCAP_FILTER_NAME_CONF, PcapFilters.FIXED.name());
    }

    @Override
    public String queryToString(Map<String, String> fields) {
      return (fields == null ? "" : Joiner.on("_").join(fields.values()));
    }
  }

  private String packetFilter;
  private String srcAddr;
  private Integer srcPort;
  private String dstAddr;
  private Integer dstPort;
  private String protocol;
  private boolean includesReverseTraffic = false;
  private boolean doHeaderFiltering = false;

  @Override
  public void configure(Iterable<Map.Entry<String, String>> config) {
    for (Map.Entry<String, String> kv : config) {
      if (kv.getKey().equals(Constants.Fields.DST_ADDR.getName())) {
        System.out.println("Processing: " + kv.getKey() + " => " + kv.getValue());
        this.dstAddr = kv.getValue();
        doHeaderFiltering = true;
      }
      if (kv.getKey().equals(Constants.Fields.SRC_ADDR.getName())) {
        System.out.println("Processing: " + kv.getKey() + " => " + kv.getValue());
        this.srcAddr = kv.getValue();
        doHeaderFiltering = true;
      }
      if (kv.getKey().equals(Constants.Fields.DST_PORT.getName())) {
        System.out.println("Processing: " + kv.getKey() + " => " + kv.getValue());
        this.dstPort = Integer.parseInt(kv.getValue());
        doHeaderFiltering = true;
      }
      if (kv.getKey().equals(Constants.Fields.SRC_PORT.getName())) {
        System.out.println("Processing: " + kv.getKey() + " => " + kv.getValue());
        this.srcPort = Integer.parseInt(kv.getValue());
        doHeaderFiltering = true;
      }
      if (kv.getKey().equals(Constants.Fields.PROTOCOL.getName())) {
        System.out.println("Processing: " + kv.getKey() + " => " + kv.getValue());
        this.protocol = kv.getValue();
        doHeaderFiltering = true;
      }
      if (kv.getKey().equals(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName())) {
        System.out.println("Processing: " + kv.getKey() + " => " + kv.getValue());
        this.includesReverseTraffic = Boolean.parseBoolean(kv.getValue());
      }
      if(kv.getKey().equals(PcapHelper.PacketFields.PACKET_FILTER.getName())) {
        System.out.println("Processing: " + kv.getKey() + " => " + kv.getValue());
        this.packetFilter = kv.getValue();
      }
    }
  }


  @Override
  public boolean test(PacketInfo pi) {
    Map<String, Object> fields = packetToFields(pi);
    VariableResolver resolver = new MapVariableResolver(fields);
    String srcAddrIn = (String) resolver.resolve(Constants.Fields.SRC_ADDR.getName());
    Integer srcPortIn = (Integer) resolver.resolve(Constants.Fields.SRC_PORT.getName());
    String dstAddrIn = (String) resolver.resolve(Constants.Fields.DST_ADDR.getName());
    Integer dstPortIn = (Integer) resolver.resolve(Constants.Fields.DST_PORT.getName());

    String protocolIn = "" + resolver.resolve(Constants.Fields.PROTOCOL.getName());
    if(!doHeaderFiltering || testHeader(srcAddrIn, srcPortIn, dstAddrIn, dstPortIn, protocolIn)) {
      //if we don't do header filtering *or* if we have tested the header and decided it's a match
      if(packetFilter != null) {
        //and we have a packet filter, then we need to filter the packet
        byte[] data = (byte[])resolver.resolve(PcapHelper.PacketFields.PACKET_DATA.getName());
        try {
          return ByteArrayMatchingUtil.INSTANCE.match(packetFilter, data);
        } catch (ExecutionException e) {
          throw new IllegalStateException("Unable to perform binary filter: " + packetFilter + " on " +  DatatypeConverter.printHexBinary(data), e);
        }
      }
      else if(!doHeaderFiltering){
        //otherwise we aren't doing packet filtering either, so we aren't doing any filtering, then we should
        //pass the test
        return true;
      }
      else {
        //and if we *are* doing header filtering and not packet filtering, then we want to pass the test
        return true;
      }
    }
    else {
      //in this case we're doing header filtering and we failed the header filter test.
      return false;
    }
  }

  private boolean testHeader( String srcAddrIn
                            , Integer srcPortIn
                            , String dstAddrIn
                            , Integer dstPortIn
                            , String protocolIn
  ) {
    if (areMatch(protocol, protocolIn)) {
      if (matchesSourceAndDestination(srcAddrIn, srcPortIn, dstAddrIn, dstPortIn)) {
        return true;
      } else if (includesReverseTraffic) {
        return matchesReverseSourceAndDestination(srcAddrIn, srcPortIn, dstAddrIn, dstPortIn);
      }
    }
    return false;
  }

  private boolean areMatch(Integer filter, Integer input) {
    return filter == null || areMatch(Integer.toUnsignedString(filter), input == null?null:Integer.toUnsignedString(input));
  }

  private boolean areMatch(String filter, String input) {
    if (filter != null) {
      return input != null && input.equals(filter);
    } else {
      return true;
    }
  }

  protected Map<String, Object> packetToFields(PacketInfo pi) {
    return PcapHelper.packetToFields(pi);
  }

  private boolean matchesSourceAndDestination(String srcAddrComp,
                                              Integer srcPortComp,
                                              String dstAddrComp,
                                              Integer dstPortComp) {
    boolean isMatch = true;
    isMatch &= areMatch(this.srcAddr, srcAddrComp);
    isMatch &= areMatch(this.srcPort, srcPortComp);
    isMatch &= areMatch(this.dstAddr, dstAddrComp);
    isMatch &= areMatch(this.dstPort, dstPortComp);
    return isMatch;
  }

  private boolean matchesReverseSourceAndDestination(String srcAddr,
                                                     Integer srcPort,
                                                     String dstAddr,
                                                     Integer dstPort) {
    return matchesSourceAndDestination(dstAddr, dstPort, srcAddr, srcPort);
  }

}
