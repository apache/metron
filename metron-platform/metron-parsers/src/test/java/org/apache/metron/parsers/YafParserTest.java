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

public class YafParserTest extends GrokParserTest {

  @Override
  public String getRawMessage() {
    return "2016-01-28 15:29:48.512|2016-01-28 15:29:48.512|   0.000|   0.000|  6|                          216.21.170.221|   80|                               10.0.2.15|39468|      AS|       0|       0|       0|22efa001|00000000|000|000|       1|      44|       0|       0|    0|idle";
  }

  @Override
  public String getGrokPath() {
    return "../metron-parsers/src/main/resources/patterns/yaf";
  }

  /**
   * {
   "iflags": "AS",
   "uflags": 0,
   "isn": "22efa001",
   "ip_dst_addr": "10.0.2.15",
   "ip_dst_port": 39468,
   "duration": "0.000",
   "rpkt": 0,
   "original_string": "2016-01-28 15:29:48.512|2016-01-28 15:29:48.512|   0.000|   0.000|  6|                          216.21.170.221|   80|                               10.0.2.15|39468|      AS|       0|       0|       0|22efa001|00000000|000|000|       1|      44|       0|       0|    0|idle",
   "pkt": 1,
   "ruflags": 0,
   "roct": 0,
   "ip_src_addr": "216.21.170.221",
   "tag": 0,
   "rtag": 0,
   "ip_src_port": 80,
   "timestamp": 1453994988512,
   "app": 0,
   "oct": 44,
   "end_reason": "idle",
   "risn": 0,
   "end_time": 1453994988512,
   "start_time": 1453994988512,
   "riflags": 0,
   "rtt": "0.000",
   "protocol": 6
   }
   */
  @Multiline
  public String expectedParsedString;

  @Override
  public String getExpectedParsedString() {
    return expectedParsedString;
  }

  @Override
  public String getGrokPatternLabel() {
    return "YAF_DELIMITED";
  }

  @Override
  public String[] getTimeFields() {
    return new String[]{"start_time", "end_time"};
  }

  @Override
  public String getDateFormat() {
    return "yyyy-MM-dd HH:mm:ss.S";
  }

  @Override
  public String getTimestampField() {
    return "start_time";
  }
}
