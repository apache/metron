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

package org.apache.metron.parsers.csv;

import com.google.common.collect.ImmutableList;
import org.apache.metron.common.csv.CSVConverter;
import org.apache.metron.common.utils.ConversionUtils;
import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CSVParser extends BasicParser {
  protected static final Logger LOG = LoggerFactory.getLogger(CSVParser.class);
  public static final String TIMESTAMP_FORMAT_CONF = "timestampFormat";
  private transient CSVConverter converter;
  private SimpleDateFormat timestampFormat;
  @Override
  public void configure(Map<String, Object> parserConfig) {
    converter = new CSVConverter();
    converter.initialize(parserConfig);
    Object tsFormatObj = parserConfig.get(TIMESTAMP_FORMAT_CONF);
    if(tsFormatObj != null) {
      timestampFormat = new SimpleDateFormat(tsFormatObj.toString());
    }
  }

  @Override
  public void init() {

  }


  @Override
  public List<JSONObject> parse(byte[] rawMessage) {
    try {
      String msg = new String(rawMessage, "UTF-8");
      Map<String, String> value = converter.toMap(msg);
      if(value != null) {
        value.put("original_string", msg);
        Object timestampObj = value.get("timestamp");
        Long timestamp = null;
        if(timestampObj == null) {
          timestamp = System.currentTimeMillis();
        }
        else {
          if(timestampFormat == null) {
            timestamp = ConversionUtils.convert(timestampObj, Long.class);
          }
          else {
            try {
              timestamp = timestampFormat.parse(timestampObj.toString()).getTime();
            }
            catch(Exception e) {
              LOG.error("Unable to format " + timestampObj.toString());
            }
          }
        }
        JSONObject jsonVal = new JSONObject(value);
        jsonVal.put("timestamp", timestamp);
        return ImmutableList.of(jsonVal);
      }
      else {
        return Collections.emptyList();
      }
    } catch (Exception e) {
      LOG.error("Unable to parse " + new String(rawMessage), e);
      return Collections.emptyList();
    }
  }
}
