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
package org.apache.metron.parsers;

import org.apache.metron.parsers.interfaces.MessageParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public abstract class BasicParser implements
        MessageParser<JSONObject>,
        Serializable {

  protected static final Logger LOG = LoggerFactory
          .getLogger(BasicParser.class);

  @Override
  public boolean validate(JSONObject message) {
    JSONObject value = message;
    if (!(value.containsKey("original_string"))) {
      LOG.trace("[Metron] Message does not have original_string: " + message);
      return false;
    } else if (!(value.containsKey("timestamp"))) {
      LOG.trace("[Metron] Message does not have timestamp: " + message);
      return false;
    } else {
      LOG.trace("[Metron] Message conforms to schema: "
              + message);
      return true;
    }
  }

  public String getKey(JSONObject value) {
    try {
      String ipSrcAddr = null;
      String ipDstAddr = null;
      if (value.containsKey("ip_src_addr"))
        ipSrcAddr = value.get("ip_src_addr").toString();
      if (value.containsKey("ip_dst_addr"))
        ipDstAddr = value.get("ip_dst_addr").toString();
      if (ipSrcAddr == null && ipDstAddr == null)
        return "0";
      if (ipSrcAddr == null || ipSrcAddr.length() == 0)
        return ipDstAddr;
      if (ipDstAddr == null || ipDstAddr.length() == 0)
        return ipSrcAddr;
      double ip1 = Double.parseDouble(ipSrcAddr.replace(".", ""));
      double ip2 = Double.parseDouble(ipDstAddr.replace(".", ""));
      return String.valueOf(ip1 + ip2);
    } catch (Exception e) {
      return "0";
    }
  }
}
