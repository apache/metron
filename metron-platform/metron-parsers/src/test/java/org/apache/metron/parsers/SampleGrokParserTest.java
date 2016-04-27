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

import org.adrianwalker.multilinestring.Multiline;

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
  public String expectedParsedString;

  public String getExpectedParsedString() {
    return expectedParsedString;
  }

  public String getRawMessage() {
    return "1453994987000|2016-01-28 15:29:48|   0.000|   0.000|  6|                          216.21.170.221|   80|                               10.0.2.15|39468|      AS|       0|       0|       0|22efa001|00000000|000|000|       1|      44|       0|       0|    0|idle";
  }

  public String getGrokPath() {
    return "../metron-integration-test/src/main/resources/sample/patterns/test";
  }

  public String getGrokPatternLabel() {
    return "YAF_DELIMITED";
  }

  public String[] getTimeFields() {
    return new String[]{"end_time"};
  }

  public String getDateFormat() {
    return "yyyy-MM-dd HH:mm:ss";
  }

  public String getTimestampField() {
    return "start_time";
  }
}
