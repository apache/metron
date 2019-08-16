/*
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

package org.apache.metron.parsers.fireeye;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.parsers.BasicParser;
import org.apache.metron.parsers.utils.ParserUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class BasicFireEyeParser extends BasicParser {

  private static final long serialVersionUID = 6328907550159134550L;
  protected static final Logger LOG = LoggerFactory
          .getLogger(MethodHandles.lookup().lookupClass());


  private static final String tsRegex = "([a-zA-Z]{3})\\s+(\\d+)\\s+(\\d+\\:\\d+\\:\\d+)"
          + "\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+)";
  private static final Pattern tsPattern = Pattern.compile(tsRegex);
  private static final String syslogPriorityRegex = "<[1-9][0-9]*>";
  private static final Pattern syslogPriorityPattern = Pattern.compile(syslogPriorityRegex);
  private static final String nvRegex = "([\\w\\d]+)=([^=]*)(?=\\s*\\w+=|\\s*$) ";
  private static final Pattern nvPattern = Pattern.compile(nvRegex);

  @Override
  public void configure(Map<String, Object> parserConfig) {
    setReadCharset(parserConfig);
  }

  @Override
  public void init() {}


  @Override
  @SuppressWarnings("unchecked")
  public List<JSONObject> parse(byte[] rawMessage) {
    String toParse;
    List<JSONObject> messages = new ArrayList<>();
    try {

      toParse = new String(rawMessage, getReadCharset());

      // because we support what is basically a malformed syslog 3164 message having
      // some form of text before the PRIORITY, we need to use the priority as
      // a delimiter
      Matcher m = syslogPriorityPattern.matcher(toParse);

      String delimiter = "";

      while (m.find()) {
        delimiter = m.group();
      }

      if (!StringUtils.isBlank(delimiter)) {
        String[] tokens = toParse.split(delimiter);
        if (tokens.length > 1) {
          toParse = delimiter + tokens[1];
        }
      }

      // parse the main message
      JSONObject toReturn = parseMessage(toParse);
      toReturn.put("timestamp", getTimeStamp(toParse));
      messages.add(toReturn);
      return messages;
    } catch (Exception e) {
      String message = "Unable to parse " + new String(rawMessage, StandardCharsets.UTF_8) + ": " + e.getMessage();
      LOG.error(message, e);
      throw new IllegalStateException(message, e);
    }
  }

  private long getTimeStamp(String toParse) throws ParseException {
    long timestamp = 0;
    String month;
    String day;
    String time;
    Matcher tsMatcher = tsPattern.matcher(toParse);
    if (tsMatcher.find()) {
      month = tsMatcher.group(1);
      day = tsMatcher.group(2);
      time = tsMatcher.group(3);
      timestamp = ParserUtils.convertToEpoch(month, day, time, true);
    } else {
      LOG.warn("Unable to find timestamp in message: {}", toParse);
    }
    return timestamp;
  }

  @SuppressWarnings("unchecked")
  private JSONObject parseMessage(String toParse) {

    JSONObject toReturn = new JSONObject();
    String[] messageTokens = toParse.split("\\s+");
    String id = messageTokens[4];

    // We are not parsing the fedata for multi part message as we cannot
    // determine how we can split the message and how many multi part
    // messages can there be.
    // The message itself will be stored in the response.

    String[] tokens = id.split("\\.");
    if (tokens.length == 2) {

      String[] array = Arrays.copyOfRange(messageTokens, 1, messageTokens.length - 1);
      String syslog = Joiner.on(" ").join(array);

      Multimap<String, String> multiMap = formatMain(syslog);

      for (String key : multiMap.keySet()) {
        String value = Joiner.on(",").join(multiMap.get(key));
        toReturn.put(key, value.trim());
      }
    }

    toReturn.put("original_string", toParse);

    final String ipSrcAddr = (String) toReturn.get("dvc");
    final String ipSrcPort = (String) toReturn.get("src_port");
    final String ipDstDddr = (String) toReturn.get("dst_ip");
    final String ipDstPort = (String) toReturn.get("dst_port");

    if (ipSrcAddr != null) {
      toReturn.put("ip_src_addr", ipSrcAddr);
    }
    if (ipSrcPort != null) {
      toReturn.put("ip_src_port", ipSrcPort);
    }
    if (ipDstDddr != null) {
      toReturn.put("ip_dst_addr", ipDstDddr);
    }
    if (ipDstPort != null) {
      toReturn.put("ip_dst_port", ipDstPort);
    }
    return toReturn;
  }

  private Multimap<String, String> formatMain(String in) {
    Multimap<String, String> multiMap = ArrayListMultimap.create();
    String input = in.replaceAll("cn3", "dst_port")
            .replaceAll("cs5", "cncHost").replaceAll("proto", "protocol")
            .replaceAll("rt=", "timestamp=").replaceAll("cs1", "malware")
            .replaceAll("dst=", "dst_ip=")
            .replaceAll("shost", "src_hostname")
            .replaceAll("dmac", "dst_mac").replaceAll("smac", "src_mac")
            .replaceAll("spt", "src_port")
            .replaceAll("\\bsrc\\b", "src_ip");
    String[] tokens = input.split("\\|");

    if (tokens.length > 0) {
      String message = tokens[tokens.length - 1];
      Matcher m = nvPattern.matcher(message);

      while (m.find()) {
        String[] str = m.group().split("=");
        multiMap.put(str[0], str[1]);
      }
    }
    return multiMap;
  }
}
