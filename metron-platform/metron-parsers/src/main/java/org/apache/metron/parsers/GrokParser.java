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

import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.Match;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class GrokParser implements MessageParser<JSONObject>, Serializable {

  protected static final Logger LOG = LoggerFactory.getLogger(GrokParser.class);

  private transient Grok grok;
  private String grokHdfsPath;
  private String patternLabel;
  private String[] timeFields = new String[0];
  private String timestampField;
  private String dateFormat = "yyyy-MM-dd HH:mm:ss.S z";
  private TimeZone timeZone = TimeZone.getTimeZone("UTC");

  private String metronHdfsHome = "/apps/metron";
  public GrokParser(String grokHdfsPath, String patterLabel) {
    this.grokHdfsPath = grokHdfsPath;
    this.patternLabel = patterLabel;
  }

  public GrokParser withMetronHDFSHome(String home) {
    this.metronHdfsHome= home;
    return this;
  }

  public GrokParser withTimestampField(String timestampField) {
    this.timestampField = timestampField;
    return this;
  }

  public GrokParser withTimeFields(String... timeFields) {
    this.timeFields = timeFields;
    return this;
  }

  public GrokParser withDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
    return this;
  }

  public GrokParser withTimeZone(String timeZone) {
    this.timeZone = TimeZone.getTimeZone(timeZone);
    return this;
  }

  public InputStream openInputStream(String streamName) throws IOException {
    InputStream is = getClass().getResourceAsStream(streamName);
    if(is == null) {
      FileSystem fs = FileSystem.get(new Configuration());
      Path path = new Path((metronHdfsHome != null && metronHdfsHome.length() > 0?metronHdfsHome + "/":"") + streamName);
      if(fs.exists(path)) {
        return fs.open(path);
      }
    }
    return is;
  }

  @Override
  public void init() {
    grok = new Grok();
    try {
      InputStream commonInputStream = openInputStream("/patterns/common");
      if(commonInputStream == null) {
        throw new RuntimeException("Unable to initialize grok parser: Unable to load /patterns/common from either classpath or HDFS" );
      }
      grok.addPatternFromReader(new InputStreamReader(commonInputStream));
      InputStream patterInputStream = openInputStream(grokHdfsPath);
      if(patterInputStream == null) {
        throw new RuntimeException("Unable to initialize grok parser: Unable to load " + grokHdfsPath + " from either classpath or HDFS" );
      }
      grok.addPatternFromReader(new InputStreamReader(patterInputStream));
      grok.compile("%{" + patternLabel + "}");
    } catch (Throwable e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException("Grok parser Error: " + e.getMessage(), e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<JSONObject> parse(byte[] rawMessage) {
    if (grok == null) init();
    List<JSONObject> messages = new ArrayList<>();
    try {
      String originalMessage = new String(rawMessage, "UTF-8");
      Match gm = grok.match(originalMessage);
      gm.captures();
      JSONObject message = new JSONObject();
      message.putAll(gm.toMap());
      message.put("original_string", originalMessage);
      for(String timeField: timeFields) {
        String fieldValue = (String) message.get(timeField);
        if (fieldValue != null) {
          message.put(timeField, toEpoch(fieldValue));
        }
      }
      if (timestampField != null) {
        message.put("timestamp", message.get(timestampField));
      }
      message.remove(patternLabel);
      messages.add(message);
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      return null;
    }
    return messages;
  }

  @Override
  public boolean validate(JSONObject message) {
    Object timestampObject = message.get("timestamp");
    if (timestampObject instanceof Long) {
      Long timestamp = (Long) timestampObject;
      if (timestamp > 0) {
        return true;
      }
    }
    return false;
  }

  private long toEpoch(String datetime) throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
    sdf.setTimeZone(timeZone);
    Date date = sdf.parse(datetime);
    return date.getTime();
  }

}
