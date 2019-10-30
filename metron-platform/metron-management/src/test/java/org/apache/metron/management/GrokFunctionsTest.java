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
package org.apache.metron.management;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.metron.stellar.dsl.Context;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GrokFunctionsTest {
  private String grokExpr = "%{NUMBER:timestamp}[^0-9]*%{INT:elapsed} %{IP:ip_src_addr} %{WORD:action}/%{NUMBER:code} %{NUMBER:bytes} %{WORD:method} %{NOTSPACE:url}[^0-9]*(%{IP:ip_dst_addr})?";


  @Test
  public void testGrokEvalSingleMessage() {
    String message = "1474583120.343    142 127.0.0.1 TCP_MISS/301 494 GET http://cnn.com/ - DIRECT/157.166.226.26 text/html";
    String out = (String) run( "GROK_EVAL( grok, messages )"
                                                      , ImmutableMap.of("messages", ImmutableList.of(message), "grok", grokExpr)
                                                      , Context.EMPTY_CONTEXT()
                                                      );
    assertTrue(out.contains("TCP_MISS"));
    assertTrue(out.contains(" 494 "));
    assertTrue(out.contains("157.166.226.26"));
  }

  @Test
  public void testGrokEvalMultiMessages() {
    String message = "1474583120.343    142 127.0.0.1 TCP_MISS/301 494 GET http://cnn.com/ - DIRECT/157.166.226.26 text/html";
    String message2 = "1474583120.343    142 127.0.0.1 TCP_MISS/404 494 GET http://google.com/ - DIRECT/157.166.226.26 text/html";
    String out = (String) run( "GROK_EVAL( grok, messages )"
                                                      , ImmutableMap.of("messages", ImmutableList.of(message, message2), "grok", grokExpr)
                                                      , Context.EMPTY_CONTEXT()
                                                      );
    assertTrue(out.contains("TCP_MISS"));
    assertTrue(out.contains(" 494 "));
    assertTrue(out.contains("157.166.226.26"));
    assertTrue(out.contains("404"));
  }

  @Test
  public void testGrokEvalBadData() {
    String message = "1474583120.343    142 foo TCP_MISS/301 494 GET http://cnn.com/ - DIRECT/157.166.226.26 text/html";
    String out = (String) run( "GROK_EVAL( grok, message )"
                                                      , ImmutableMap.of("message", message, "grok", grokExpr)
                                                      , Context.EMPTY_CONTEXT()
                                                      );
    assertEquals("NO MATCH", out);
  }

  @Test
  public void testGrokEvalBadDataMultiMessages() {
    String message = "1474583120.343    142 foo TCP_MISS/301 494 GET http://cnn.com/ - DIRECT/157.166.226.26 text/html";
    String message2 = "1474583120.343    142 127.0.0.1 TCP_MISS/404 494 GET http://google.com/ - DIRECT/157.166.226.26 text/html";
    String out = (String) run( "GROK_EVAL( grok, messages )"
                                                      , ImmutableMap.of("messages", ImmutableList.of(message, message2), "grok", grokExpr)
                                                      , Context.EMPTY_CONTEXT()
                                                      );
    assertTrue(out.contains("MISSING"));
    assertTrue(out.contains("404"));
  }

  @Test
  public void testGrokDiscover() {
    String out = (String) run("GROK_PREDICT( '1474583120.343    142 127.0.0.1 TCP_MISS/301')"
                                                      , new HashMap<>()
                                                      , Context.EMPTY_CONTEXT()
                                                      );
    assertEquals("%{BASE10NUM}    142 %{IP} TCP_MISS%{PATH}", out);
  }
}
