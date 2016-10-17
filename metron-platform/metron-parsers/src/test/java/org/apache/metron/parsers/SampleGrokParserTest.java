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

import junit.framework.Assert;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SampleGrokParserTest extends GrokParserTest {

  /**
   * {
   * "roct":0,
   * "end_reason":"idle",
   * "ip_dst_addr":"10.0.2.15",
   * "iflags":"AS",
   * "rpkt":0,
   * "original_string":"1453994987000|2016-01-28 15:29:48|   0.000|   0.000|  6|                          216.21.170.221|   80|                               10.0.2.15|39468|      AS|       0|       0|       0|22efa001|00000000|000|000|       1|      44|       0|       0|    0|idle",
   * "tag":0,
   * "risn":0,
   * "ip_dst_port":39468,
   * "ruflags":0,
   * "app":0,
   * "protocol":6
   * ,"isn":"22efa001",
   * "uflags":0,"duration":"0.000",
   * "oct":44,
   * "ip_src_port":80,
   * "end_time":1453994988000,
   * "start_time":1453994987000
   * "timestamp":1453994987000,
   * "riflags":0,
   * "rtt":"0.000",
   * "rtag":0,
   * "pkt":1,
   * "ip_src_addr":"216.21.170.221"
   * }
   */
  @Multiline
  public String result;


  @Override
  public Map getTestData() {

    Map testData = new HashMap<String,String>();
    String input = "1453994987000|2016-01-28 15:29:48|   0.000|   0.000|  6|                          216.21.170.221|   80|                               10.0.2.15|39468|      AS|       0|       0|       0|22efa001|00000000|000|000|       1|      44|       0|       0|    0|idle";
    testData.put(input,result);
    return testData;

  }

  public String[] getGrokPattern() {
    String[] grokPattern = {"YAF_TIME_FORMAT %{YEAR:UNWANTED}-%{MONTHNUM:UNWANTED}-%{MONTHDAY:UNWANTED}[T ]%{HOUR:UNWANTED}:%{MINUTE:UNWANTED}:%{SECOND:UNWANTED}",
            "YAF_DELIMITED %{NUMBER:start_time}\\|%{YAF_TIME_FORMAT:end_time}\\|%{SPACE:UNWANTED}%{BASE10NUM:duration}\\|%{SPACE:UNWANTED}%{BASE10NUM:rtt}\\|%{SPACE:UNWANTED}%{INT:protocol}\\|%{SPACE:UNWANTED}%{IP:ip_src_addr}\\|%{SPACE:UNWANTED}%{INT:ip_src_port}\\|%{SPACE:UNWANTED}%{IP:ip_dst_addr}\\|%{SPACE:UNWANTED}%{INT:ip_dst_port}\\|%{SPACE:UNWANTED}%{DATA:iflags}\\|%{SPACE:UNWANTED}%{DATA:uflags}\\|%{SPACE:UNWANTED}%{DATA:riflags}\\|%{SPACE:UNWANTED}%{DATA:ruflags}\\|%{SPACE:UNWANTED}%{WORD:isn}\\|%{SPACE:UNWANTED}%{DATA:risn}\\|%{SPACE:UNWANTED}%{DATA:tag}\\|%{GREEDYDATA:rtag}\\|%{SPACE:UNWANTED}%{INT:pkt}\\|%{SPACE:UNWANTED}%{INT:oct}\\|%{SPACE:UNWANTED}%{INT:rpkt}\\|%{SPACE:UNWANTED}%{INT:roct}\\|%{SPACE:UNWANTED}%{INT:app}\\|%{GREEDYDATA:end_reason}"};
    return grokPattern;
  }

  public String getGrokPatternLabel() {
    return "YAF_DELIMITED";
  }

  public List<String> getTimeFields() {
    return new ArrayList<String>() {{
      add("end_time");
    }};
  }

  public String getDateFormat() {
    return "yyyy-MM-dd HH:mm:ss";
  }

  public String getTimestampField() {
    return "start_time";
  }

  @Test
  public void testConfigChange() {
    String raw = "123 test";
    String pattern1 = "LABEL %{NUMBER:field_1}";
    String pattern2 = "LABEL %{NUMBER:field_1} %{WORD:field_2}";
    JSONObject expected1 = new JSONObject();
    expected1.put("field_1", 123);
    expected1.put("original_string", raw);
    JSONObject expected2 = new JSONObject();
    expected2.put("field_1", 123);
    expected2.put("field_2", "test");
    expected2.put("original_string", raw);
    Map<String, Object> parserConfig = new HashMap<>();
    parserConfig.put("grokPattern", pattern1);
    parserConfig.put("patternLabel", "LABEL");
    SensorParserConfig sensorParserConfig = new SensorParserConfig();
    sensorParserConfig.setParserConfig(parserConfig);

    GrokParser grokParser = new GrokParser();
    grokParser.configure(parserConfig);
    grokParser.init();

    List<JSONObject> results = grokParser.parse(raw.getBytes());
    Assert.assertEquals(1, results.size());
    compare(expected1, results.get(0));

    parserConfig.put("grokPattern", pattern2);
    grokParser.configurationUpdated(sensorParserConfig.getParserConfig());
    results = grokParser.parse(raw.getBytes());
    Assert.assertEquals(1, results.size());
    compare(expected2, results.get(0));
  }
}
