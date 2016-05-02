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

public class SquidParserTest extends GrokParserTest {

  @Override
  public String getRawMessage() {
    return "1461576382.642    161 127.0.0.1 TCP_MISS/200 103701 GET http://www.cnn.com/ - DIRECT/199.27.79.73 text/html";
  }

  /**
   * {
   *   "elapsed":161,
   *   "code":200,
   *   "original_string":"1461576382.642    161 127.0.0.1 TCP_MISS/200 103701 GET http://www.cnn.com/ - DIRECT/199.27.79.73 text/html",
   *   "method":"GET",
   *   "bytes":103701,
   *   "action":"TCP_MISS",
   *   "ip_src_addr":"127.0.0.1",
   *   "ip_dst_addr":"199.27.79.73",
   *   "url":"cnn.com",
   *   "timestamp":1461576382642
   * }
   */
  @Multiline
  public String expectedParsedString;

  @Override
  public String getExpectedParsedString() {
    return expectedParsedString;
  }

  @Override
  public String getGrokPath() {
    return "../metron-parsers/src/main/resources/patterns/squid";
  }

  @Override
  public String getGrokPatternLabel() {
    return "SQUID_DELIMITED";
  }

  @Override
  public String[] getTimeFields() {
    return new String[0];
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
