/*
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

package org.apache.metron.parsers.syslog;

import com.github.palindromicity.syslog.NilPolicy;
import com.github.palindromicity.syslog.dsl.SyslogFieldKeys;
import org.apache.metron.parsers.interfaces.MessageParserResult;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class Syslog5424ParserTest {
  private static final String SYSLOG_LINE_ALL = "<14>1 2014-06-20T09:14:07+00:00 loggregator"
          + " d0602076-b14a-4c55-852a-981e7afeed38 DEA MSG-01"
          + " [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"]"
          + "[exampleSDID@32480 iut=\"4\" eventSource=\"Other Application\" eventID=\"2022\"] Removing instance";

  private static final String SYSLOG_LINE_MISSING = "<14>1 2014-06-20T09:14:07+00:00 loggregator"
          + " d0602076-b14a-4c55-852a-981e7afeed38 DEA -"
          + " [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"]"
          + "[exampleSDID@32480 iut=\"4\" eventSource=\"Other Application\" eventID=\"2022\"] Removing instance";

  private static final String SYSLOG_LINE_MISSING_DATE = "<14>1 - loggregator"
          + " d0602076-b14a-4c55-852a-981e7afeed38 DEA -"
          + " [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"]"
          + "[exampleSDID@32480 iut=\"4\" eventSource=\"Other Application\" eventID=\"2022\"] Removing instance";

  private static final String expectedVersion = "1";
  private static final String expectedMessage = "Removing instance";
  private static final String expectedAppName = "d0602076-b14a-4c55-852a-981e7afeed38";
  private static final String expectedHostName = "loggregator";
  private static final String expectedPri = "14";
  private static final String expectedFacility = "1";
  private static final String expectedSeverity = "6";
  private static final String expectedProcId = "DEA";
  private static final String expectedTimestamp = "2014-06-20T09:14:07+00:00";
  private static final String expectedMessageId = "MSG-01";

  private static final String expectedIUT1 = "3";
  private static final String expectedIUT2 = "4";
  private static final String expectedEventSource1 = "Application";
  private static final String expectedEventSource2 = "Other Application";
  private static final String expectedEventID1 = "1011";
  private static final String expectedEventID2 = "2022";


  @Test
  public void testConfigureDefault() {
    Map<String, Object> parserConfig = new HashMap<>();
    Syslog5424Parser testParser = new Syslog5424Parser();
    testParser.configure(parserConfig);
    testParser.init();
    assertTrue(testParser.deviceClock.getZone().equals(ZoneOffset.UTC));
  }

  @Test
  public void testConfigureTimeZoneOffset() {
    Map<String, Object> parserConfig = new HashMap<>();
    parserConfig.put("deviceTimeZone", "UTC-05:00");
    Syslog5424Parser testParser = new Syslog5424Parser();
    testParser.configure(parserConfig);
    testParser.init();
    ZonedDateTime deviceTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1475323200), testParser.deviceClock.getZone());
    ZonedDateTime referenceTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1475323200), ZoneOffset.ofHours(-5));
    assertTrue(deviceTime.isEqual(referenceTime));
  }

  @Test
  public void testConfigureTimeZoneText() {
    Map<String, Object> parserConfig = new HashMap<>();
    parserConfig.put("deviceTimeZone", "America/New_York");
    Syslog5424Parser testParser = new Syslog5424Parser();
    testParser.configure(parserConfig);
    testParser.init();
    ZonedDateTime deviceTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1475323200), testParser.deviceClock.getZone());
    ZonedDateTime referenceTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1475323200), ZoneOffset.ofHours(-5));
    assertTrue(deviceTime.isEqual(referenceTime));
  }

  @Test
  public void testHappyPath() {
    test(null, SYSLOG_LINE_ALL, (message) -> assertEquals(expectedMessageId, message.get(SyslogFieldKeys.HEADER_MSGID.getField())));
  }

  @Test
  public void testOmit() {
    test(NilPolicy.OMIT, SYSLOG_LINE_MISSING, (message) -> assertFalse(message.containsKey(SyslogFieldKeys.HEADER_MSGID)));
  }

  @Test
  public void testDash() {
    test(NilPolicy.DASH, SYSLOG_LINE_MISSING, (message) -> assertEquals("-", message.get(SyslogFieldKeys.HEADER_MSGID.getField())));
  }

  @Test()
  public void testNull() {
    test(NilPolicy.NULL, SYSLOG_LINE_MISSING, (message) -> {
      assertTrue(message.containsKey(SyslogFieldKeys.HEADER_MSGID.getField()));
      assertNull(message.get(SyslogFieldKeys.HEADER_MSGID.getField()));
    });
  }

  @Test()
  public void testNotValid() {
    test(null, "not valid", (message) -> assertTrue(false));
  }

  public void test(NilPolicy nilPolicy, String line, Consumer<JSONObject> msgIdChecker) {
    Syslog5424Parser parser = new Syslog5424Parser();
    Map<String, Object> config = new HashMap<>();
    if (nilPolicy != null) {
      config.put(Syslog5424Parser.NIL_POLICY_CONFIG, nilPolicy.name());
    }
    parser.configure(config);

    parser.parseOptionalResult(line.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void testReadMultiLine() throws Exception {
    Syslog5424Parser parser = new Syslog5424Parser();
    Map<String, Object> config = new HashMap<>();
    config.put(Syslog5424Parser.NIL_POLICY_CONFIG, NilPolicy.DASH.name());
    parser.configure(config);
    StringBuilder builder = new StringBuilder();
    builder
            .append(SYSLOG_LINE_ALL)
            .append("\n")
            .append(SYSLOG_LINE_MISSING)
            .append("\n")
            .append(SYSLOG_LINE_ALL);
    Optional<MessageParserResult<JSONObject>> resultOptional = parser.parseOptionalResult(builder.toString().getBytes(
        StandardCharsets.UTF_8));
    assertNotNull(resultOptional);
    assertTrue(resultOptional.isPresent());
    List<JSONObject> parsedList = resultOptional.get().getMessages();
    assertEquals(3,parsedList.size());
  }

  @Test
  public void testReadMultiLineWithErrors() throws Exception {
    Syslog5424Parser parser = new Syslog5424Parser();
    Map<String, Object> config = new HashMap<>();
    config.put(Syslog5424Parser.NIL_POLICY_CONFIG, NilPolicy.DASH.name());
    parser.configure(config);
    StringBuilder builder = new StringBuilder();
    builder
            .append("HEREWEGO!!!!\n")
            .append(SYSLOG_LINE_ALL)
            .append("\n")
            .append(SYSLOG_LINE_MISSING)
            .append("\n")
            .append("BOOM!\n")
            .append(SYSLOG_LINE_ALL)
            .append("\nOHMY!");
    Optional<MessageParserResult<JSONObject>> output = parser.parseOptionalResult(builder.toString().getBytes(
        StandardCharsets.UTF_8));
    assertTrue(output.isPresent());
    assertEquals(3,output.get().getMessages().size());
    assertEquals(3,output.get().getMessageThrowables().size());
  }

  @Test
  public void testMissingTimestamp() {
    Syslog5424Parser parser = new Syslog5424Parser();
    Map<String, Object> config = new HashMap<>();
    String timeStampString = null;
    config.put(Syslog5424Parser.NIL_POLICY_CONFIG, NilPolicy.DASH.name());
    parser.configure(config);
    Optional<MessageParserResult<JSONObject>> output  = parser.parseOptionalResult(SYSLOG_LINE_MISSING_DATE.getBytes(
        StandardCharsets.UTF_8));
    assertNotNull(output);
    assertTrue(output.isPresent());
    assertNotNull(output.get().getMessages().get(0).get("timestamp").toString());
    config.clear();
    config.put(Syslog5424Parser.NIL_POLICY_CONFIG, NilPolicy.NULL.name());
    parser.configure(config);
    output = parser.parseOptionalResult(SYSLOG_LINE_MISSING_DATE.getBytes(StandardCharsets.UTF_8));
    assertNotNull(output);
    assertTrue(output.isPresent());
    timeStampString = output.get().getMessages().get(0).get("timestamp").toString();
    assertNotNull(timeStampString);
    config.clear();
    config.put(Syslog5424Parser.NIL_POLICY_CONFIG, NilPolicy.OMIT.name());
    parser.configure(config);

    output = parser.parseOptionalResult(SYSLOG_LINE_MISSING_DATE.getBytes(StandardCharsets.UTF_8));
    assertNotNull(output);
    assertTrue(output.isPresent());
  }
}