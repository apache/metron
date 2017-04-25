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
package org.apache.metron.parsers.dhcpdump;

import org.apache.metron.common.Constants;
import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * NOTE: This parser is not compatible with the original DHCPDump-1.8 output.
 * Please modify the DHCPDump output to match the required format or download the source
 * from github (https://github.com/basvdl/dhcpdump/tree/master).
 *
 * Format changes:
 *      - new lines `\n` were replaced by pipes `|`
 *      - the parameters list (printReqParmList()) is separated by commas `,` instead of new lines `\n`
 *
 * @input TIME: 2017-04-21 15:43:17.673|IP: 172.20.3.28 (84:2b:2b:86:62:43) > 255.255.255.255 (ff:ff:ff:ff:ff:ff)|OP: 1 (BOOTPREQUEST)|HTYPE: 1 (Ethernet)|HLEN: 6|HOPS: 0|XID: 23850fd5|SECS: 0|FLAGS: 0|CIADDR: 172.20.3.28|YIADDR: 0.0.0.0|SIADDR: 0.0.0.0|GIADDR: 0.0.0.0|CHADDR: 84:2b:2b:86:62:43:00:00:00:00:00:00:00:00:00:00|SNAME: .|FNAME: .|OPTION:  53 (  1) DHCP message type         8 (DHCPINFORM)|OPTION:  61 (  7) Client-identifier         01:84:2b:2b:86:62:43|OPTION:  12 (  5) Host name                 Q0619|OPTION:  60 (  8) Vendor class identifier   MSFT 5.0|OPTION:  55 ( 13) Parameter Request List    1 (Subnet mask),15 (Domainname),3 (Routers),6 (DNS server),44 (NetBIOS name server),46 (NetBIOS node type),47 (NetBIOS scope),31 (Perform router discovery),33 (Static route),121 (Classless Static Route),249 (MSFT - Classless route),43 (Vendor specific info),252 (MSFT - WinSock Proxy Auto Detect)
 */
public class BasicDHCPDumpParser  extends BasicParser {

  protected static final Logger LOG = LoggerFactory.getLogger(BasicDHCPDumpParser.class);

  public static final String TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

  public static final String TIMESTAMP_CONFIG = "timestamp_pattern";
  private SimpleDateFormat timestampFormat = new SimpleDateFormat(TIMESTAMP_PATTERN);


  @Override
  public void configure(Map<String, Object> config) {
    timestampFormat = new SimpleDateFormat((String) config.getOrDefault(TIMESTAMP_CONFIG, TIMESTAMP_PATTERN));
  }

  @Override
  public void init() {

  }

  @Override
  public List<JSONObject> parse(byte[] rawMessage) {
    LOG.trace("[Metron] Starting to parse incoming message");

    String decodedMessage = null;
    JSONObject jsonMessage;
    List<JSONObject> messages = new ArrayList<>();

    try {
      decodedMessage = new String(rawMessage, "UTF-8");
      LOG.trace("[Metron] Received message: " + decodedMessage);

      String[] multiLineEvent = convertToMultiLine(decodedMessage);

      jsonMessage = convertToKeyValue(multiLineEvent);

      jsonMessage.put(Constants.Fields.ORIGINAL.getName(), decodedMessage);
      if (jsonMessage.containsKey("time")) {
        try {
          // convert timestamp to milli since epoch
          long timestamp = timestampToEpoch((String) jsonMessage.get("time"));
          jsonMessage.put(Constants.Fields.TIMESTAMP.getName(), timestamp);
        } catch (java.text.ParseException e) {
          LOG.warn(String.format("[Metron] Timestamp format mismatch %s, is invalid: %s", timestampFormat.toPattern(), jsonMessage.get("timestamp")));
          jsonMessage.put(Constants.Fields.TIMESTAMP.getName(), System.currentTimeMillis());
        }
      } else {
        LOG.warn(String.format("[Metron] Timestamp is missing: %s", jsonMessage));
        jsonMessage.put(Constants.Fields.TIMESTAMP.getName(), System.currentTimeMillis());
      }

    } catch (RuntimeException e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e.getMessage(), e);
    } catch (Exception e) {
      String message = "Unable to parse Message: " + decodedMessage;
      LOG.error(message, e);
      throw new IllegalStateException(message, e);
    }

    messages.add(jsonMessage);
    return messages;
  }

  private String[] convertToMultiLine(String msg) {
    String[] multiLineEvent = msg.split(Pattern.quote("|"));

    return multiLineEvent;
  }

  private JSONObject convertToKeyValue(String[] multiLineEvent) {
    JSONObject keyValueEvent = new JSONObject();

    for (String s: multiLineEvent) {
      if (s.startsWith("TIME:")) {
        keyValueEvent.put("time", s.split(":", 2)[1].trim());
      } else if (s.startsWith("CIADDR:")) {
        keyValueEvent.put("ciaddr", s.split(":", 2)[1].trim());
      } else if (s.startsWith("YIADDR:")) {
        keyValueEvent.put("yiaddr", s.split(":", 2)[1].trim());
      } else if (s.startsWith("SIADDR:")) {
        keyValueEvent.put("siaddr", s.split(":", 2)[1].trim());
      } else if (s.startsWith("GIADDR:")) {
        keyValueEvent.put("giaddr", s.split(":", 2)[1].trim());
      } else if (s.startsWith("INTERFACE:")) {
        keyValueEvent.put("interface", s.split(":", 2)[1].trim());
      } else if (s.startsWith("IP:")) {
        /**
         * Input example
         * IP: 172.20.3.28 (84:2b:2b:86:62:43) > 255.255.255.255 (ff:ff:ff:ff:ff:ff)
         */
        String value = s.split(":", 2)[1].trim();
        value = value.replaceAll("\\(|\\)|>\\s", ""); // replace `(`, `)` and `> `

        String[] values = value.split(" ");

        keyValueEvent.put(Constants.Fields.SRC_ADDR.getName(), values[0].trim());
        keyValueEvent.put(Constants.Fields.DST_ADDR.getName(), values[2].trim());
        keyValueEvent.put("mac_src_addr", values[1].trim());
        keyValueEvent.put("mac_dst_addr", values[3].trim());
      } else if (s.startsWith("OP:")) {
        /**
         * Input example
         * OP: 1 (BOOTPREQUEST)
         */
        keyValueEvent.put("op", s.split(":")[1].split(" ")[1].trim());
      } else if (s.startsWith("OPTION:")) {
        /**
         * Input examples
         * OPTION:  12 (  5) Host name                 Q0619
         * OPTION:  61 (  7) Client-identifier         01:84:2b:2b:86:62:43
         */
        Integer op = Integer.valueOf(s.substring(7, 12).trim());

        Integer[] validOptions = {12, 61};

        if (Arrays.asList(validOptions).contains(op)) {
          String value = s.substring(44, s.length()).trim();
          if (op == 12) {
            keyValueEvent.put("host_name", value);
          } else if (op == 61) {
            keyValueEvent.put("client_identifier", value);
          }
        }

      }
    }

    return keyValueEvent;
  }

  private long timestampToEpoch(String timestamp) throws java.text.ParseException {
    return timestampFormat.parse(timestamp).getTime();
  }

}
