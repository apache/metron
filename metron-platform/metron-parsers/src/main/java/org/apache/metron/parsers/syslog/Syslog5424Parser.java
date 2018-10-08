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

package org.apache.metron.parsers.syslog;

import com.github.palindromicity.syslog.AllowableDeviations;
import com.github.palindromicity.syslog.NilPolicy;
import com.github.palindromicity.syslog.SyslogParser;
import com.github.palindromicity.syslog.SyslogParserBuilder;
import com.github.palindromicity.syslog.dsl.SyslogFieldKeys;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;


/**
 * Parser for well structured RFC 5424 messages.
 */
public class Syslog5424Parser extends BasicParser {
  public static final String NIL_POLICY_CONFIG = "nilPolicy";
  private transient SyslogParser syslogParser;

  @Override
  public void configure(Map<String, Object> config) {
    // Default to OMIT policy for nil fields
    // this means they will not be in the returned field set
    String nilPolicyStr = (String) config.getOrDefault(NIL_POLICY_CONFIG, NilPolicy.OMIT.name());
    NilPolicy nilPolicy = NilPolicy.valueOf(nilPolicyStr);
    syslogParser = new SyslogParserBuilder()
            .withNilPolicy(nilPolicy)
            .withDeviations(EnumSet.of(AllowableDeviations.PRIORITY,AllowableDeviations.VERSION))
            .build();
  }

  @Override
  public void init() {
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<JSONObject> parse(byte[] rawMessage) {
    try {
      if (rawMessage == null || rawMessage.length == 0) {
        return null;
      }

      String originalString = new String(rawMessage);
      List<JSONObject> returnList = new ArrayList<>();
      try (Reader reader = new BufferedReader(new StringReader(originalString))) {
        syslogParser.parseLines(reader, (m) -> {
          JSONObject jsonObject = new JSONObject(m);
          // be sure to put in the original string, and the timestamp.
          // we wil just copy over the timestamp from the syslog
          jsonObject.put("original_string", originalString);
          setTimestamp(jsonObject);
          returnList.add(jsonObject);
        });

        return returnList;
      }
    } catch (Exception e) {
      String message = "Unable to parse " + new String(rawMessage) + ": " + e.getMessage();
      LOG.error(message, e);
      throw new IllegalStateException(message, e);
    }
  }

  @SuppressWarnings("unchecked")
  private void setTimestamp(JSONObject message) {
    String timeStampString = (String) message.get(SyslogFieldKeys.HEADER_TIMESTAMP.getField());
    if (!StringUtils.isBlank(timeStampString) && !timeStampString.equals("-")) {
      message.put("timestamp", timeStampString);
    } else {
      message.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
    }
  }
}
