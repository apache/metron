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
package org.apache.metron.parsing.parsers;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import junit.framework.Assert;
import org.adrianwalker.multilinestring.Multiline;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GrokParserTest {

  public String expectedRaw = "2016-01-28 15:29:48.512|2016-01-28 15:29:48.512|   0.000|   0.000|  6|                          216.21.170.221|   80|                               10.0.2.15|39468|      AS|       0|       0|       0|22efa001|00000000|000|000|       1|      44|       0|       0|    0|idle";

  /**
   * {
   * "roct":0,
   * "end_reason":"idle",
   * "ip_dst_addr":"10.0.2.15",
   * "iflags":"AS",
   * "rpkt":0,
   * "original_string":"2016-01-28 15:29:48.512|2016-01-28 15:29:48.512|   0.000|   0.000|  6|                          216.21.170.221|   80|                               10.0.2.15|39468|      AS|       0|       0|       0|22efa001|00000000|000|000|       1|      44|       0|       0|    0|idle",
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
   * "end_time":"2016-01-28 15:29:48.512",
   * "riflags":0,"start_time":"2016-01-28 15:29:48.512",
   * "rtt":"0.000",
   * "rtag":0,
   * "pkt":1,
   * "ip_src_addr":"216.21.170.221"
   * }
   */
  @Multiline
  private String expectedParsedString;

  private JSONObject expectedParsed;

  @Before
  public void parseJSON() throws ParseException {
    JSONParser jsonParser = new JSONParser();
    expectedParsed = (JSONObject) jsonParser.parse(expectedParsedString);
  }

  @Test
  public void test() throws IOException, ParseException {
    String metronHdfsHome = "../Metron-MessageParsers/src/main/resources";
    String grokHdfsPath = "/patterns/yaf";
    String patternLabel = "YAF_DELIMITED";
    GrokParser grokParser = new GrokParser(grokHdfsPath, patternLabel);
    grokParser.withMetronHDFSHome(metronHdfsHome);
    grokParser.init();
    byte[] rawMessage = expectedRaw.getBytes();
    List<JSONObject> parsedList = grokParser.parse(rawMessage);
    Assert.assertEquals(1, parsedList.size());
    compare(expectedParsed, parsedList.get(0));
  }

  public boolean compare(JSONObject expected, JSONObject actual) {
    MapDifference mapDifferences = Maps.difference(expected, actual);
    if (mapDifferences.entriesOnlyOnLeft().size() > 0) Assert.fail("Expected JSON has extra parameters: " + mapDifferences.entriesOnlyOnLeft());
    if (mapDifferences.entriesOnlyOnRight().size() > 0) Assert.fail("Actual JSON has extra parameters: " + mapDifferences.entriesOnlyOnRight());
    Map actualDifferences = new HashMap();
    if (mapDifferences.entriesDiffering().size() > 0) {
      Map differences = Collections.unmodifiableMap(mapDifferences.entriesDiffering());
      for (Object key : differences.keySet()) {
        Object expectedValueObject = expected.get(key);
        Object actualValueObject = actual.get(key);
        if (expectedValueObject instanceof Long || expectedValueObject instanceof Integer) {
          Long expectedValue = Long.parseLong(expectedValueObject.toString());
          Long actualValue = Long.parseLong(actualValueObject.toString());
          if (!expectedValue.equals(actualValue)) {
            actualDifferences.put(key, differences.get(key));
          }
        } else {
          actualDifferences.put(key, differences.get(key));
        }
      }
    }
    if (actualDifferences.size() > 0) Assert.fail("Expected and Actual JSON values don't match: " + actualDifferences);
    return true;
  }
}
