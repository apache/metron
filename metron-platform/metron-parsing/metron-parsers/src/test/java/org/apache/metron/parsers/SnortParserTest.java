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
import org.apache.log4j.Level;
import org.apache.metron.common.Constants;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.parsers.snort.BasicSnortParser;
import org.apache.metron.test.utils.UnitTestHelper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class SnortParserTest {
  /**
   01/27/16-16:01:04.877970 ,129,12,1,"Consecutive TCP small segments, exceeding threshold",TCP,10.0.2.2,56642,10.0.2.15,22,52:54:00:12:35:02,08:00:27:7F:93:2D,0x4E,***AP***,0x9AFF3D7,0xC8761D52,,0xFFFF,64,0,59677,64,65536,,,,
   **/
  @Multiline
  public static String goodMessage;

  // we will test timestamp conversion/parsing separately
  @Test
  public void testGoodMessage() {
    BasicSnortParser parser = new BasicSnortParser();
    parser.configure(Collections.emptyMap());
    Map out = parser.parse(goodMessage.getBytes(StandardCharsets.UTF_8)).get(0);
    assertEquals(out.get("msg"), "Consecutive TCP small segments, exceeding threshold");
    assertEquals(out.get("sig_rev"), "1");
    assertEquals(out.get("ip_dst_addr"), "10.0.2.15");
    assertEquals(out.get("ip_dst_port"), "22");
    assertEquals(out.get("ethsrc"), "52:54:00:12:35:02");
    assertEquals(out.get("tcpseq"), "0x9AFF3D7");
    assertEquals(out.get("dgmlen"), "64");
    assertEquals(out.get("icmpid"), "");
    assertEquals(out.get("tcplen"), "");
    assertEquals(out.get("tcpwindow"), "0xFFFF");
    assertEquals(out.get("icmpseq").toString().trim(), "");
    assertEquals(out.get("tcpack"), "0xC8761D52");
    assertEquals(out.get("icmpcode"), "");
    assertEquals(out.get("tos"), "0");
    assertEquals(out.get("id"), "59677");
    assertEquals(out.get("ethdst"), "08:00:27:7F:93:2D");
    assertEquals(out.get("ip_src_addr"), "10.0.2.2");
    assertEquals(out.get("ttl"), "64");
    assertEquals(out.get("ethlen"), "0x4E");
    assertEquals(out.get("iplen"), "65536");
    assertEquals(out.get("icmptype"), "");
    assertEquals(out.get("protocol"), "TCP");
    assertEquals(out.get("ip_src_port"), "56642");
    assertEquals(out.get("tcpflags"), "***AP***");
    assertEquals(out.get("sig_id"), "12");
    assertEquals(out.get("sig_generator"), "129");
    assertEquals(out.get("is_alert"), "true");
  }

  @Test
  public void testBadMessage() {
    BasicSnortParser parser = new BasicSnortParser();
    parser.init();
    UnitTestHelper.setLog4jLevel(BasicSnortParser.class, Level.FATAL);
    assertThrows(IllegalStateException.class, () -> parser.parse("foo bar".getBytes(StandardCharsets.UTF_8)));
    UnitTestHelper.setLog4jLevel(BasicSnortParser.class, Level.ERROR);
  }

  @Test
  public void parses_timestamp_as_local_zone_by_default() {
    // test needs to be able to run from context of multiple timezones so we will set the default manually
    TimeZone defaultTimeZone = TimeZone.getDefault();
    try {
      TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("America/New_York")));
      BasicSnortParser parser = new BasicSnortParser();
      parser.configure(new HashMap());
      Map out = parser.parse(goodMessage.getBytes(StandardCharsets.UTF_8)).get(0);
      assertEquals(out.get("timestamp"), 1453928464877L);
    } finally {
      // make sure we don't mess with other tests
      TimeZone.setDefault(defaultTimeZone);
    }
  }

  /**
   01/27/2016-16:01:04.877970 ,129,12,1,"Consecutive TCP small segments, exceeding threshold",TCP,10.0.2.2,56642,10.0.2.15,22,52:54:00:12:35:02,08:00:27:7F:93:2D,0x4E,***AP***,0x9AFF3D7,0xC8761D52,,0xFFFF,64,0,59677,64,65536,,,,
   **/
  @Multiline
  public static String dateFormattedMessage;

  @Test
  public void uses_configuration_to_parse() {
    Map<String, Object> parserConfig = new HashMap<>();
    parserConfig.put("dateFormat", "MM/dd/yyyy-HH:mm:ss.SSSSSS");
    parserConfig.put("timeZone", "America/New_York");
    BasicSnortParser parser = new BasicSnortParser();
    parser.configure(parserConfig);
    Map result = parser.parse(dateFormattedMessage.getBytes(StandardCharsets.UTF_8)).get(0);
    assertThat("timestamp should match", result.get(Constants.Fields.TIMESTAMP.getName()), equalTo(1453928464877L));
  }

  @Test
  public void throws_exception_on_bad_config_timezone() {
    Map<String, Object> parserConfig = new HashMap<>();
    parserConfig.put("dateFormat", "MM/dd/yyyy-HH:mm:ss.SSSSSS");
    parserConfig.put("timeZone", "blahblahBADZONE");
    BasicSnortParser parser = new BasicSnortParser();
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> parser.configure(parserConfig));
    assertTrue(e.getMessage().startsWith("Unable to find ZoneId"));
  }

  @Test
  public void throws_exception_on_bad_config_date_format() {
    Map<String, Object> parserConfig = new HashMap<>();
    parserConfig.put("dateFormat", "BADFORMAT");
    BasicSnortParser parser = new BasicSnortParser();
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> parser.configure(parserConfig));
    assertTrue(e.getMessage().startsWith("Unknown pattern letter:"));

  }

  @Test
  public void getsReadCharsetFromConfig() {
    Map<String, Object> config = new HashMap<>();
    config.put(MessageParser.READ_CHARSET, StandardCharsets.UTF_16.toString());
    BasicSnortParser parser = new BasicSnortParser();
    parser.configure(config);
    assertThat(parser.getReadCharset(), equalTo(StandardCharsets.UTF_16));
  }

  @Test
  public void getsReadCharsetFromDefault() {
    Map<String, Object> config = new HashMap<>();
    BasicSnortParser parser = new BasicSnortParser();
    parser.configure(config);
    assertThat(parser.getReadCharset(), equalTo(StandardCharsets.UTF_8));
  }
}
