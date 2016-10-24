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
import org.apache.metron.common.Constants;
import org.apache.metron.parsers.snort.BasicSnortParser;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class SnortParserTest {
  /**
  01/27/16-16:01:04.877970 ,129,12,1,"Consecutive TCP small segments, exceeding threshold",TCP,10.0.2.2,56642,10.0.2.15,22,52:54:00:12:35:02,08:00:27:7F:93:2D,0x4E,***AP***,0x9AFF3D7,0xC8761D52,,0xFFFF,64,0,59677,64,65536,,,,
   **/
  @Multiline
  public static String goodMessage;

  @Test
  public void testDates() {
    String d = "01/27/16-16:01:04.877970";
    OffsetDateTime date = LocalDateTime.parse(d, DateTimeFormatter.ofPattern("MM/dd/yy-HH:mm:ss.SSSSSS")).atOffset(ZoneOffset.UTC);
    System.out.println(date.get(ChronoField.MICRO_OF_SECOND));
    System.out.println(date);
    System.out.println(date.toInstant().toEpochMilli());
//    System.out.println(date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
//    System.out.println(date.atZone(ZoneOffset.UTC).toInstant().toEpochMilli());
//    System.out.println(date.atZone(ZoneId.of("EDT")).toInstant().toEpochMilli());
    System.out.println(new Date().getTime());
    System.out.println(new Date(date.toInstant().toEpochMilli()));
  }

  @Test
  public void testGoodMessage() {
    BasicSnortParser parser = new BasicSnortParser();
    parser.init();
    Map out = parser.parse(goodMessage.getBytes()).get(0);
    Assert.assertEquals(out.get("msg"),"Consecutive TCP small segments, exceeding threshold");
    Assert.assertEquals(out.get("timestamp"), 1453928464877L);
    Assert.assertEquals(out.get("sig_rev"), "1");
    Assert.assertEquals(out.get("ip_dst_addr"), "10.0.2.15");
    Assert.assertEquals(out.get("ip_dst_port"), "22");
    Assert.assertEquals(out.get("ethsrc"), "52:54:00:12:35:02");
    Assert.assertEquals(out.get("tcpseq"),"0x9AFF3D7");
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
    Assert.assertEquals(out.get("ttl"),"64");
    Assert.assertEquals(out.get("ethlen"),"0x4E");
    Assert.assertEquals(out.get("iplen"),"65536");
    Assert.assertEquals(out.get("icmptype"),"");
    Assert.assertEquals(out.get("protocol"),"TCP");
    Assert.assertEquals(out.get("ip_src_port"),"56642");
    Assert.assertEquals(out.get("tcpflags"),"***AP***");
    Assert.assertEquals(out.get("sig_id"),"12");
    Assert.assertEquals(out.get("sig_generator"), "129");
    Assert.assertEquals(out.get("is_alert"), "true");
  }

  @Test(expected=IllegalStateException.class)
  public void testBadMessage() {
    BasicSnortParser parser = new BasicSnortParser();
    parser.init();
    parser.parse("foo bar".getBytes());
  }

  /**
   01/27/2016-16:01:04.877970 ,129,12,1,"Consecutive TCP small segments, exceeding threshold",TCP,10.0.2.2,56642,10.0.2.15,22,52:54:00:12:35:02,08:00:27:7F:93:2D,0x4E,***AP***,0x9AFF3D7,0xC8761D52,,0xFFFF,64,0,59677,64,65536,,,,
   **/
  @Multiline
  public static String dateFormatMessage;

  @Test
  public void uses_configuration() {
    Map<String, Object> parserConfig = new HashMap<>();
    parserConfig.put("dateFormat", "MM/dd/yyyy-HH:mm:ss.SSSSSS");
    //parserConfig.put("timezone", "America/New_York");
    parserConfig.put("timezone", "UTC");
    BasicSnortParser parser = new BasicSnortParser();
    parser.configure(parserConfig);
    Map result = parser.parse(dateFormatMessage.getBytes()).get(0);
    assertThat("timestamp should match", result.get(Constants.Fields.TIMESTAMP.getName()), equalTo(1453910464877L));
  }
}
