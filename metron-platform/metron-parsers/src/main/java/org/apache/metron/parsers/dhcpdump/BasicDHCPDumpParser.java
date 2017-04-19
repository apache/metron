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

import org.apache.commons.lang.StringUtils;
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

    msg = msg.replace("||", "");

    int count = StringUtils.countMatches(msg, "|");

    /* TODO find better method to prevent splitting the IP: field content,currently this works only when IP: is the last field in the event */
    String[] multiLineEvent = msg.split(Pattern.quote("|"), count);

    return multiLineEvent;
  }

  private JSONObject convertToKeyValue(String[] multiLineEvent) {
    JSONObject keyValueEvent = new JSONObject();

    for (String s: multiLineEvent) {
      if (s.startsWith("TIME:")) {
        keyValueEvent.put("time", s.split(":", 2)[1].trim());
      } else if (s.startsWith("IP:")) {
        String value = s.split(":", 2)[1];
        value = value.replace("|", ">");

        String[] values = value.split(">");

        keyValueEvent.put(Constants.Fields.SRC_ADDR.getName(), values[0].trim());
        keyValueEvent.put(Constants.Fields.DST_ADDR.getName(), values[1].trim());
        keyValueEvent.put("mac_src_addr", values[2].trim());
        keyValueEvent.put("mac_dst_addr", values[3].trim());
      } else if (s.startsWith("OP:")) {
        keyValueEvent.put("op", s.split(":")[1].split(" ")[1].trim());
      } else if (s.startsWith("CIADDR:")) {
        keyValueEvent.put("ciaddr", s.split(":", 2)[1].trim());
      } else if (s.startsWith("YIADDR:")) {
        keyValueEvent.put("yiaddr", s.split(":", 2)[1].trim());
      } else if (s.startsWith("SIADDR:")) {
        keyValueEvent.put("siaddr", s.split(":", 2)[1].trim());
      } else if (s.startsWith("GIADDR:")) {
        keyValueEvent.put("giaddr", s.split(":", 2)[1].trim());
      } else if (s.startsWith("OPTION:")) {
        /**
         * Input examples
         * OPTION:  12   5 Host name: A1244
         * OPTION:  61   7 Client-identifier: 01:fc:f8:ae:e8:ef:db
         */
        Integer op = Integer.valueOf(s.split(":")[1].trim().split(" ")[0]);

        Integer[] validOptions = {12, 61};

        if (Arrays.asList(validOptions).contains(op)) {
          if (op == 12) {
            keyValueEvent.put("host_name", s.split(":", 3)[2].trim());
          } else if (op == 61) {
            keyValueEvent.put("client_identifier", s.split(":", 3)[2].trim());
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
