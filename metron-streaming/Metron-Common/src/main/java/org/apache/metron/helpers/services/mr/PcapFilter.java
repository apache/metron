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

package org.apache.metron.helpers.services.mr;

import com.google.common.base.Predicate;
import org.apache.metron.Constants;
import org.json.simple.JSONObject;

import javax.annotation.Nullable;
import java.util.Map;


public class PcapFilter implements Predicate<JSONObject> {
  private String srcAddr;
  private Integer srcPort;
  private String dstAddr;
  private Integer dstPort;
  private String protocol;

  public PcapFilter( String srcAddr
                   , Integer srcPort
                   , String dstAddr
                   , Integer dstPort
                   , String protocol
                   )
  {
    this.srcAddr = srcAddr;
    this.srcPort = srcPort;
    this.dstAddr = dstAddr;
    this.dstPort = dstPort;
    this.protocol = protocol;
  }

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
    }
  }


  @Override
  public boolean apply(@Nullable JSONObject input) {
    boolean isMatch = true;
    if(srcAddr != null) {
      Object o = input.get(Constants.Fields.SRC_ADDR.getName());
      isMatch &= o != null && o instanceof String && ((String)o).equals(srcAddr);
    }
    if(isMatch && srcPort != null) {
      Object o = input.get(Constants.Fields.SRC_PORT.getName());
      isMatch &= o != null && o.toString().equals(srcPort);
    }
    if(isMatch && dstAddr != null) {
      Object o = input.get(Constants.Fields.DST_ADDR.getName());
      isMatch &= o != null && o instanceof String && ((String)o).equals(dstAddr);
    }
    if(isMatch && dstPort != null) {
      Object o = input.get(Constants.Fields.DST_PORT.getName());
      isMatch &= o != null && o.toString().equals(dstPort);
    }
    if(isMatch && protocol != null) {
      Object o = input.get(Constants.Fields.PROTOCOL.getName());
      isMatch &= o != null && o.toString().equals(protocol);
    }
    return isMatch;
  }
}
