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
package org.apache.metron.parsers.snort;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.common.csv.CSVConverter;
import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class BasicSnortParser extends BasicParser {

  private static final Logger _LOG = LoggerFactory.getLogger(BasicSnortParser.class);

  /**
   * The default field names for Snort Alerts.
   */
  private String[] fieldNames = new String[] {
          Constants.Fields.TIMESTAMP.getName(),
          "sig_generator",
          "sig_id",
          "sig_rev",
          "msg",
          Constants.Fields.PROTOCOL.getName(),
          Constants.Fields.SRC_ADDR.getName(),
          Constants.Fields.SRC_PORT.getName(),
          Constants.Fields.DST_ADDR.getName(),
          Constants.Fields.DST_PORT.getName(),
          "ethsrc",
          "ethdst",
          "ethlen",
          "tcpflags",
          "tcpseq",
          "tcpack",
          "tcplen",
          "tcpwindow",
          "ttl",
          "tos",
          "id",
          "dgmlen",
          "iplen",
          "icmptype",
          "icmpcode",
          "icmpid",
          "icmpseq"
  };


  /**
   * Snort alerts are received as CSV records
   */
  private String recordDelimiter = ",";

  private transient CSVConverter converter;

  private static String defaultDateFormat = "MM/dd/yy-HH:mm:ss.SSSSSS";
  private transient DateTimeFormatter dateTimeFormatter;

  public BasicSnortParser() {

  }

  @Override
  public void configure(Map<String, Object> parserConfig) {
    dateTimeFormatter = getDateFormatter(parserConfig);
    dateTimeFormatter = getDateFormatterWithZone(dateTimeFormatter, parserConfig);
    init();
  }

  private DateTimeFormatter getDateFormatter(Map<String, Object> parserConfig) {
    String format = (String) parserConfig.get("dateFormat");
    if (StringUtils.isNotEmpty(format)) {
      _LOG.info("Using date format '{}'", format);
      return DateTimeFormatter.ofPattern(format);
    } else {
      _LOG.info("Using default date format '{}'", defaultDateFormat);
      return DateTimeFormatter.ofPattern(defaultDateFormat);
    }
  }

  private DateTimeFormatter getDateFormatterWithZone(DateTimeFormatter formatter, Map<String, Object> parserConfig) {
    String timezone = (String) parserConfig.get("timeZone");
    if (StringUtils.isNotEmpty(timezone)) {
      if(ZoneId.getAvailableZoneIds().contains(timezone)) {
        _LOG.info("Using timezone '{}'", timezone);
        return formatter.withZone(ZoneId.of(timezone));
      } else {
        throw new IllegalArgumentException("Unable to find ZoneId '" + timezone + "'");
      }
    } else {
      _LOG.info("Using default timezone '{}'", ZoneId.systemDefault());
      return formatter.withZone(ZoneId.systemDefault());
    }
  }

  @Override
  public void init() {
    if(converter == null) {
      converter = new CSVConverter();
      Map<String, Object> config = new HashMap<>();
      config.put(CSVConverter.SEPARATOR_KEY, recordDelimiter);
      config.put(CSVConverter.COLUMNS_KEY, Lists.newArrayList(fieldNames));
      converter.initialize(config);
    }
  }

  @Override
  public List<JSONObject> parse(byte[] rawMessage) {

    JSONObject jsonMessage = new JSONObject();
    List<JSONObject> messages = new ArrayList<>();
    try {
      // snort alerts expected as csv records
      String csvMessage = new String(rawMessage, "UTF-8");
      Map<String, String> records = null;
      try {
         records = converter.toMap(csvMessage);
      }
      catch(ArrayIndexOutOfBoundsException aioob) {
        throw new IllegalArgumentException("Unexpected number of fields, expected: " + fieldNames.length + " in " + csvMessage);
      }

      // validate the number of fields
      if (records.size() != fieldNames.length) {
        throw new IllegalArgumentException("Unexpected number of fields, expected: " + fieldNames.length + " got: " + records.size());
      }
      long timestamp = 0L;
      // build the json record from each field
      for (Map.Entry<String, String> kv : records.entrySet()) {

        String field = kv.getKey();
        String record = kv.getValue();

        if("timestamp".equals(field)) {

          // convert the timestamp to epoch
          timestamp = toEpoch(record);
          jsonMessage.put("timestamp", timestamp);

        } else {
          jsonMessage.put(field, record);
        }
      }

      // add original msg; required by 'checkForSchemaCorrectness'
      jsonMessage.put("original_string", csvMessage);
      jsonMessage.put("is_alert", "true");
      messages.add(jsonMessage);
    } catch (Exception e) {
      String message = "Unable to parse message: " + (rawMessage == null?"null" : new String(rawMessage));
      _LOG.error(message, e);
      throw new IllegalStateException(message, e);
    }

    return messages;
  }

  /**
   * Parses Snort's default date-time representation and
   * converts to epoch.
   * @param snortDatetime Snort's default date-time as String '01/27-16:01:04.877970'
   * @return epoch time
   * @throws java.text.ParseException
   */
  private long toEpoch(String snortDatetime) throws ParseException {
    ZonedDateTime zonedDateTime = ZonedDateTime.parse(snortDatetime.trim(), dateTimeFormatter);
    return zonedDateTime.toInstant().toEpochMilli();
  }

  public String getRecordDelimiter() {
    return this.recordDelimiter;
  }

  public void setRecordDelimiter(String recordDelimiter) {
    this.recordDelimiter = recordDelimiter;
  }

  public String[] getFieldNames() {
    return this.fieldNames;
  }

  public void setFieldNames(String[] fieldNames) {
    this.fieldNames = fieldNames;
  }

}
