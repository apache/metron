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

import org.apache.metron.common.Constants;
import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings("serial")
public class BasicSnortParser extends BasicParser {

  private static final Logger _LOG = LoggerFactory
          .getLogger(BasicSnortParser.class);

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

  @Override
  public void configure(Map<String, Object> parserConfig) {

  }

  @Override
  public void init() {

  }

  @Override
  public List<JSONObject> parse(byte[] rawMessage) {

    JSONObject jsonMessage = new JSONObject();
    List<JSONObject> messages = new ArrayList<>();
    try {
      // snort alerts expected as csv records
      String csvMessage = new String(rawMessage, "UTF-8");
      String[] records = csvMessage.split(recordDelimiter, -1);

      // validate the number of fields
      if (records.length != fieldNames.length) {
        throw new IllegalArgumentException("Unexpected number of fields, expected: " + fieldNames.length + " got: " + records.length);
      }
      long timestamp = 0L;
      // build the json record from each field
      for (int i=0; i<records.length; i++) {

        String field = fieldNames[i];
        String record = records[i];

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

      _LOG.error("unable to parse message: " + rawMessage);
      e.printStackTrace();
      return null;
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
		
		/*
		 * TODO how does Snort not embed the year in their default timestamp?! need to change this in 
		 * Snort configuration.  for now, just assume current year.
		 */
    int year = Calendar.getInstance().get(Calendar.YEAR);
    String withYear = Integer.toString(year) + " " + snortDatetime;

    // convert to epoch time
    SimpleDateFormat df = new SimpleDateFormat("yyyy MM/dd-HH:mm:ss.S");
    Date date = df.parse(withYear);
    return date.getTime();
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
