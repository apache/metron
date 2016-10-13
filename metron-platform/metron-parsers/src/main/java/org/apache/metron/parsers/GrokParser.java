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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.Match;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

public class GrokParser implements MessageParser<JSONObject>, Serializable {

  protected static final Logger LOG = LoggerFactory.getLogger(GrokParser.class);

  protected transient Grok grok;
  protected String grokPattern;
  protected String patternLabel;
  protected List<String> timeFields = new ArrayList<>();
  protected String timestampField;
  protected SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S z");
  protected String patternsCommonPath = "/patterns/common";

  @SuppressWarnings("unchecked")
  @Override
  public void configure(Map<String, Object> parserConfig) {
    Object grokPattern = parserConfig.get("grokPattern");
    if (grokPattern instanceof String) {
      this.grokPattern = (String) grokPattern;
    } else if (grokPattern instanceof String[]){
      String[] patterns = (String[]) grokPattern;
      this.grokPattern = Joiner.on('\n').join(patterns);
    } else if (grokPattern instanceof Iterable) {
      Iterable<String> patterns = (Iterable<String>) grokPattern;
      this.grokPattern = Joiner.on('\n').join(patterns);
    }
    this.patternLabel = (String) parserConfig.get("patternLabel");
    this.timestampField = (String) parserConfig.get("timestampField");
    List<String> timeFieldsParam = (List<String>) parserConfig.get("timeFields");
    if (timeFieldsParam != null) {
      this.timeFields = timeFieldsParam;
    }
    String dateFormatParam = (String) parserConfig.get("dateFormat");
    if (dateFormatParam != null) {
      this.dateFormat = new SimpleDateFormat(dateFormatParam);
    }
    String timeZoneParam = (String) parserConfig.get("timeZone");
    if (timeZoneParam != null) {
      dateFormat.setTimeZone(TimeZone.getTimeZone(timeZoneParam));
      LOG.debug("Grok Parser using provided TimeZone: {}", timeZoneParam);
    } else {
      dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      LOG.debug("Grok Parser using default TimeZone (UTC)");
    }
  }

  @Override
  public void init() {
    grok = new Grok();
    try {
      InputStream commonInputStream = getClass().getResourceAsStream(patternsCommonPath);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Grok parser loading common patterns from: " + patternsCommonPath);
      }

      if (commonInputStream == null) {
        throw new RuntimeException(
                "Unable to initialize grok parser: Unable to load " + patternsCommonPath + " from classpath");
      }

      grok.addPatternFromReader(new InputStreamReader(commonInputStream));

      if (LOG.isDebugEnabled()) {
        LOG.debug("Loading parser-specific patterns: " + grokPattern);
      }

      if (grokPattern == null) {
        throw new RuntimeException("Unable to initialize grok parser: grokPattern config property is empty");
      }
      grok.addPatternFromReader(new InputStreamReader(new ByteArrayInputStream(grokPattern.getBytes())));

      if (LOG.isDebugEnabled()) {
        LOG.debug("Grok parser set the following grok expression: " + grok.getNamedRegexCollectionById(patternLabel));
      }

      String grokPattern = "%{" + patternLabel + "}";

      grok.compile(grokPattern);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Compiled grok pattern" + grokPattern);
      }

    } catch (Throwable e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException("Grok parser Error: " + e.getMessage(), e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<JSONObject> parse(byte[] rawMessage, SensorParserConfig sensorParserConfig) {
    if (grok == null || isGrokPatternUpdated(sensorParserConfig) || isPatternLabelUpdated(sensorParserConfig)) {
      configure(sensorParserConfig.getParserConfig());
      init();
    }
    List<JSONObject> messages = new ArrayList<>();
    String originalMessage = null;
    try {
      originalMessage = new String(rawMessage, "UTF-8");
      if (LOG.isDebugEnabled()) {
        LOG.debug("Grok parser parsing message: " + originalMessage);
      }
      Match gm = grok.match(originalMessage);
      gm.captures();
      JSONObject message = new JSONObject();
      message.putAll(gm.toMap());

      if (message.size() == 0)
        throw new RuntimeException("Grok statement produced a null message. Original message was: "
                + originalMessage + " , parsed message was: " + message + " , pattern was: "
                + grokPattern);

      message.put("original_string", originalMessage);
      for (String timeField : timeFields) {
        String fieldValue = (String) message.get(timeField);
        if (fieldValue != null) {
          message.put(timeField, toEpoch(fieldValue));
        }
      }
      if (timestampField != null) {
        message.put(Constants.Fields.TIMESTAMP.getName(), formatTimestamp(message.get(timestampField)));
      }
      message.remove(patternLabel);
      postParse(message);
      messages.add(message);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Grok parser parsed message: " + message);
      }
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new IllegalStateException("Grok parser Error: " + e.getMessage() + " on " + originalMessage , e);
    }
    return messages;
  }

  @Override
  public boolean validate(JSONObject message) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Grok parser validating message: " + message);
    }

    Object timestampObject = message.get(Constants.Fields.TIMESTAMP.getName());
    if (timestampObject instanceof Long) {
      Long timestamp = (Long) timestampObject;
      if (timestamp > 0) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Grok parser validated message: " + message);
        }
        return true;
      }
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Grok parser did not validate message: " + message);
    }
    return false;
  }

  protected boolean isGrokPatternUpdated(SensorParserConfig sensorParserConfig) {
    Map<String, Object> parserConfig = sensorParserConfig.getParserConfig();
    return parserConfig != null && !Objects.equals(grokPattern, parserConfig.get("grokPattern"));
  }

  protected boolean isPatternLabelUpdated(SensorParserConfig sensorParserConfig) {
    Map<String, Object> parserConfig = sensorParserConfig.getParserConfig();
    return parserConfig != null && !Objects.equals(patternLabel, parserConfig.get("patternLabel"));
  }

  protected void postParse(JSONObject message) {}

  protected long toEpoch(String datetime) throws ParseException {

    LOG.debug("Grok parser converting timestamp to epoch: {}", datetime);
    LOG.debug("Grok parser's DateFormat has TimeZone: {}", dateFormat.getTimeZone());

    Date date = dateFormat.parse(datetime);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Grok parser converted timestamp to epoch: " + date);
    }

    return date.getTime();
  }

  protected long formatTimestamp(Object value) {

    if (LOG.isDebugEnabled()) {
      LOG.debug("Grok parser formatting timestamp" + value);
    }


    if (value == null) {
      throw new RuntimeException(patternLabel + " pattern does not include field " + timestampField);
    }
    if (value instanceof Number) {
      return ((Number) value).longValue();
    } else {
      return Long.parseLong(Joiner.on("").join(Splitter.on('.').split(value + "")));
    }
  }

}
