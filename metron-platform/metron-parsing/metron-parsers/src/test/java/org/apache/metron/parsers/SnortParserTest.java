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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.log4j.Level;
import org.apache.metron.common.Constants;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.parsers.snort.BasicSnortParser;
import org.apache.metron.test.utils.UnitTestHelper;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SnortParserTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  /**
   01/27/16-16:01:04.877970 ,129,12,1,"Consecutive TCP small segments, exceeding threshold",TCP,10.0.2.2,56642,10.0.2.15,22,52:54:00:12:35:02,08:00:27:7F:93:2D,0x4E,***AP***,0x9AFF3D7,0xC8761D52,,0xFFFF,64,0,59677,64,65536,,,,
   **/
  @Multiline
  public static String goodMessage;

  // we will test timestamp conversion/parsing separately
  @Test
  public void testGoodMessage() {
    BasicSnortParser parser = new BasicSnortParser();
    parser.configure(new HashMap());
    Map out = parser.parse(goodMessage.getBytes(StandardCharsets.UTF_8)).get(0);
    Assert.assertEquals(out.get("msg"), "Consecutive TCP small segments, exceeding threshold");
    Assert.assertEquals(out.get("sig_rev"), "1");
    Assert.assertEquals(out.get("ip_dst_addr"), "10.0.2.15");
    Assert.assertEquals(out.get("ip_dst_port"), "22");
    Assert.assertEquals(out.get("ethsrc"), "52:54:00:12:35:02");
    Assert.assertEquals(out.get("tcpseq"), "0x9AFF3D7");
    Assert.assertEquals(out.get("dgmlen"), "64");
    Assert.assertEquals(out.get("icmpid"), "");
    Assert.assertEquals(out.get("tcplen"), "");
    Assert.assertEquals(out.get("tcpwindow"), "0xFFFF");
    Assert.assertEquals(out.get("icmpseq").toString().trim(), "");
    Assert.assertEquals(out.get("tcpack"), "0xC8761D52");
    Assert.assertEquals(out.get("icmpcode"), "");
    Assert.assertEquals(out.get("tos"), "0");
    Assert.assertEquals(out.get("id"), "59677");
    Assert.assertEquals(out.get("ethdst"), "08:00:27:7F:93:2D");
    Assert.assertEquals(out.get("ip_src_addr"), "10.0.2.2");
    Assert.assertEquals(out.get("ttl"), "64");
    Assert.assertEquals(out.get("ethlen"), "0x4E");
    Assert.assertEquals(out.get("iplen"), "65536");
    Assert.assertEquals(out.get("icmptype"), "");
    Assert.assertEquals(out.get("protocol"), "TCP");
    Assert.assertEquals(out.get("ip_src_port"), "56642");
    Assert.assertEquals(out.get("tcpflags"), "***AP***");
    Assert.assertEquals(out.get("sig_id"), "12");
    Assert.assertEquals(out.get("sig_generator"), "129");
    Assert.assertEquals(out.get("is_alert"), "true");
  }

  @Test
  public void testBadMessage() {
    thrown.expect(IllegalStateException.class);
    BasicSnortParser parser = new BasicSnortParser();
    parser.init();
    UnitTestHelper.setLog4jLevel(BasicSnortParser.class, Level.FATAL);
    parser.parse("foo bar".getBytes(StandardCharsets.UTF_8));
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
      Assert.assertEquals(out.get("timestamp"), 1453928464877L);
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
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(startsWith("Unable to find ZoneId"));
    Map<String, Object> parserConfig = new HashMap<>();
    parserConfig.put("dateFormat", "MM/dd/yyyy-HH:mm:ss.SSSSSS");
    parserConfig.put("timeZone", "blahblahBADZONE");
    BasicSnortParser parser = new BasicSnortParser();
    parser.configure(parserConfig);
  }

  @Test
  public void throws_exception_on_bad_config_date_format() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(startsWith("Unknown pattern letter:"));
    Map<String, Object> parserConfig = new HashMap<>();
    parserConfig.put("dateFormat", "BADFORMAT");
    BasicSnortParser parser = new BasicSnortParser();
    parser.configure(parserConfig);
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
