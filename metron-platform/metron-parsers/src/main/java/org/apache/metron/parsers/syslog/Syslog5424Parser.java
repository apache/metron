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
import org.apache.metron.parsers.DefaultMessageParserResult;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.parsers.interfaces.MessageParserResult;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Parser for well structured RFC 5424 messages.
 */
public class Syslog5424Parser implements MessageParser<JSONObject>, Serializable {
  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
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
  public boolean validate(JSONObject message) {
    JSONObject value = message;
    if (!(value.containsKey("original_string"))) {
      LOG.trace("[Metron] Message does not have original_string: {}", message);
      return false;
    } else if (!(value.containsKey("timestamp"))) {
      LOG.trace("[Metron] Message does not have timestamp: {}", message);
      return false;
    } else {
      LOG.trace("[Metron] Message conforms to schema: {}", message);
      return true;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public Optional<MessageParserResult<JSONObject>> parseOptionalResult(byte[] rawMessage) {
    try {
      if (rawMessage == null || rawMessage.length == 0) {
        return Optional.empty();
      }

      String originalString = new String(rawMessage);
      List<JSONObject> returnList = new ArrayList<>();
      Map<Object,Throwable> errorMap = new HashMap<>();
      try (Reader reader = new BufferedReader(new StringReader(originalString))) {
        syslogParser.parseLines(reader, (m) -> {
          JSONObject jsonObject = new JSONObject(m);
          // be sure to put in the original string, and the timestamp.
          // we wil just copy over the timestamp from the syslog
          jsonObject.put("original_string", originalString);
          setTimestamp(jsonObject);
          returnList.add(jsonObject);
        },errorMap::put);

        return Optional.of(new DefaultMessageParserResult<JSONObject>(returnList,errorMap));
      }
    } catch (IOException e) {
      String message = "Unable to read buffer " + new String(rawMessage) + ": " + e.getMessage();
      LOG.error(message, e);
      return Optional.of(new DefaultMessageParserResult<JSONObject>( new IllegalStateException(message, e)));
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
