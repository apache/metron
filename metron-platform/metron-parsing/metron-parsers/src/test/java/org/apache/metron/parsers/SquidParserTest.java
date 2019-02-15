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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SquidParserTest extends GrokParserTest {

  /**
   * {
   *   "elapsed":161,
   *   "code":200,
   *   "ip_dst_addr":"199.27.79.73",
   *   "original_string":"1461576382.642    161 127.0.0.1 TCP_MISS/200 103701 GET http://www.cnn.com/ - DIRECT/199.27.79.73 text/html",
   *   "method":"GET",
   *   "bytes":103701,
   *   "action":"TCP_MISS",
   *   "url":"http://www.cnn.com/",
   *   "ip_src_addr":"127.0.0.1",
   *   "timestamp":1461576382642
   * }
   */
  @Multiline
  private String result1;

  /**
   * {
   *   "elapsed":0,
   *   "code":403,
   *   "original_string":"1469539185.270      0 139.196.181.68 TCP_DENIED/403 3617 CONNECT search.yahoo.com:443 - NONE/- text/html",
   *   "method":"CONNECT",
   *   "bytes":3617,
   *   "action":"TCP_DENIED",
   *   "url":"search.yahoo.com:443",
   *   "ip_src_addr":"139.196.181.68",
   *   "timestamp":1469539185270,
   *   "ip_dst_addr": null
   * }
   */
  @Multiline
  private String result2;

  @Override
  public Map<String,String> getTestData() {

    String input1 = "1461576382.642    161 127.0.0.1 TCP_MISS/200 103701 GET http://www.cnn.com/ - DIRECT/199.27.79.73 text/html";
    String input2 = "1469539185.270      0 139.196.181.68 TCP_DENIED/403 3617 CONNECT search.yahoo.com:443 - NONE/- text/html";

    HashMap testData = new HashMap<String,String>();
    testData.put(input1,result1);
    testData.put(input2,result2);
    return testData;

  }

  @Override
  public String getMultiLine() { return "false"; }

  @Override
  public String getGrokPath() {
    return "../metron-parsers/src/main/resources/patterns/squid";
  }

  @Override
  public String getGrokPatternLabel() {
    return "SQUID_DELIMITED";
  }

  @Override
  public List<String> getTimeFields() {
    return new ArrayList<>();
  }

  @Override
  public String getDateFormat() {
    return null;
  }

  @Override
  public String getTimestampField() {
    return "timestamp";
  }
}
