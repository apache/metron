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
package org.apache.metron.parsers.asa;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.test.utils.UnitTestHelper;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BasicAsaParserTest {

    private static BasicAsaParser asaParser;
    private Map<String, Object> parserConfig;

    @Before
    public void setUp() throws Exception {
        parserConfig = new HashMap<>();
        asaParser = new BasicAsaParser();
        asaParser.configure(parserConfig);
        asaParser.init();
    }

    @Test
    public void testConfigureDefault() {
        BasicAsaParser testParser = new BasicAsaParser();
        testParser.configure(parserConfig);
        testParser.init();
        assertTrue(testParser.deviceClock.getZone().equals(ZoneOffset.UTC));
    }

    @Test
    public void testConfigureTimeZoneOffset() {
        parserConfig.put("deviceTimeZone", "UTC-05:00");
        BasicAsaParser testParser = new BasicAsaParser();
        testParser.configure(parserConfig);
        testParser.init();
        ZonedDateTime deviceTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1475323200), testParser.deviceClock.getZone());
        ZonedDateTime referenceTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1475323200), ZoneOffset.ofHours(-5));
        assertTrue(deviceTime.isEqual(referenceTime));
    }

    @Test
    public void testConfigureTimeZoneText() {
        parserConfig.put("deviceTimeZone", "America/New_York");
        BasicAsaParser testParser = new BasicAsaParser();
        testParser.configure(parserConfig);
        testParser.init();
        ZonedDateTime deviceTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1475323200), testParser.deviceClock.getZone());
        ZonedDateTime referenceTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1475323200), ZoneOffset.ofHours(-5));
        assertTrue(deviceTime.isEqual(referenceTime));
    }

    @Test
    public void testCISCOFW106023() {
        String rawMessage = "<164>Aug 05 2016 01:01:34: %ASA-4-106023: Deny tcp src Inside:10.30.9.121/54580 dst Outside:192.168.135.51/42028 by access-group \"Inside_access_in\" [0x962df600, 0x0]";
        JSONObject asaJson = asaParser.parse(rawMessage.getBytes(StandardCharsets.UTF_8)).get(0);
        assertEquals(asaJson.get("original_string"), rawMessage);
        assertTrue(asaJson.get("ip_src_addr").equals("10.30.9.121"));
        assertTrue(asaJson.get("ip_dst_addr").equals("192.168.135.51"));
        assertTrue(asaJson.get("ip_src_port").equals(54580));
        assertTrue(asaJson.get("ip_dst_port").equals(42028));
        assertTrue((long) asaJson.get("timestamp") == 1470358894000L);
    }

    @Test
    public void testCISCOFW106006() {
        String rawMessage = "<162>Aug 05 2016 01:02:25: %ASA-2-106006: Deny inbound UDP from 10.25.177.164/63279 to 10.2.52.71/161 on interface Inside";
        JSONObject asaJson = asaParser.parse(rawMessage.getBytes(StandardCharsets.UTF_8)).get(0);
        assertEquals(asaJson.get("original_string"), rawMessage);
        assertTrue(asaJson.get("ip_src_addr").equals("10.25.177.164"));
        assertTrue(asaJson.get("ip_dst_addr").equals("10.2.52.71"));
        assertTrue(asaJson.get("ip_src_port").equals(63279));
        assertTrue(asaJson.get("ip_dst_port").equals(161));
        assertTrue((long) asaJson.get("timestamp") == 1470358945000L);
    }

    @Test
    public void testShortTimestamp() {
        String rawMessage = "<174>Jan  5 14:52:35 10.22.8.212 %ASA-6-302015: Built inbound UDP connection 76245506 for outside:10.22.8.110/49886 (10.22.8.110/49886) to inside:192.111.72.8/8612 (192.111.72.8/8612) (user.name)";

        ZonedDateTime fixedInstant =
                ZonedDateTime.of(2016, 1, 6, 1, 30, 30, 0, ZoneOffset.UTC);
        Clock fixedClock = Clock.fixed(fixedInstant.toInstant(), fixedInstant.getZone());

        BasicAsaParser fixedClockParser = new BasicAsaParser();
        fixedClockParser.deviceClock = fixedClock;
        fixedClockParser.init();

        JSONObject asaJson = fixedClockParser.parse(rawMessage.getBytes(StandardCharsets.UTF_8)).get(0);
        assertEquals(asaJson.get("original_string"), rawMessage);
        assertTrue(asaJson.get("ip_src_addr").equals("10.22.8.110"));
        assertTrue(asaJson.get("ip_dst_addr").equals("192.111.72.8"));
        assertTrue(asaJson.get("ip_src_port").equals(49886));
        assertTrue(asaJson.get("ip_dst_port").equals(8612));
        assertTrue((long) asaJson.get("timestamp") == 1452005555000L);
    }

    @Test
    public void testNoPatternForTag() {
        String rawMessage = "<165>Aug 16 2016 04:08:36: %ASA-5-713049: Group = 172.22.136.20, IP = 172.22.136.20, Security negotiation complete for LAN-to-LAN Group (172.22.136.20)  Initiator, Inbound SPI = 0x891fb03f, Outbound SPI = 0xbe4b5d8d";
        JSONObject asaJson = asaParser.parse(rawMessage.getBytes(StandardCharsets.UTF_8)).get(0);
        assertEquals(asaJson.get("original_string"), rawMessage);
        assertTrue((long) asaJson.get("timestamp") == 1471320516000L);
    }

    @Test
    public void testInvalidIpAddr() {
        String rawMessage = "<164>Aug 05 2016 01:01:34: %ASA-4-106023: Deny tcp src Inside:10.30.9.121/54580 dst Outside:192.168.256.51/42028 by access-group \"Inside_access_in\" [0x962df600, 0x0]";
        JSONObject asaJson = asaParser.parse(rawMessage.getBytes(StandardCharsets.UTF_8)).get(0);
        assertEquals(asaJson.get("original_string"), rawMessage);
        assertTrue((long) asaJson.get("timestamp") == 1470358894000L);
        assertNull(asaJson.get("ip_dst_addr"));
    }

    @Test
    public void testIp6Addr() {
        String rawMessage = "<174>Jan 05 2016 14:52:35 10.22.8.212 %ASA-6-302015: Built inbound UDP connection 76245506 for outside:2001:db8:85a3::8a2e:370:7334/49886 (10.22.8.110/49886) to inside:2001:0db8:85a3:0000:0000:8a2e:0370:7334/8612 (192.111.72.8/8612) (user.name)";
        JSONObject asaJson = asaParser.parse(rawMessage.getBytes(StandardCharsets.UTF_8)).get(0);
        assertEquals(rawMessage, asaJson.get("original_string"));
        assertEquals("2001:db8:85a3::8a2e:370:7334", asaJson.get("ip_src_addr"));
        assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", asaJson.get("ip_dst_addr"));
        assertEquals(49886, asaJson.get("ip_src_port"));
        assertEquals(8612, asaJson.get("ip_dst_port"));
        assertEquals(1452005555000L, asaJson.get("timestamp"));
    }

    @Test 
    public void testSyslogIpHost() {
    	String rawMessage = "<174>Jan  5 14:52:35 10.22.8.212 %ASA-6-302015: Built inbound UDP connection 76245506 for outside:10.22.8.110/49886 (10.22.8.110/49886) to inside:192.111.72.8/8612 (192.111.72.8/8612) (user.name)";
    	JSONObject asaJson = asaParser.parse(rawMessage.getBytes(StandardCharsets.UTF_8)).get(0);
        assertEquals("10.22.8.212", asaJson.get("syslog_host"));
    }
    
    @Test 
    public void testSyslogHost() {
    	String rawMessage = "<174>Jan  5 14:52:35 hostname-2 %ASA-6-302015: Built inbound UDP connection 76245506 for outside:10.22.8.110/49886 (10.22.8.110/49886) to inside:192.111.72.8/8612 (192.111.72.8/8612) (user.name)";
    	JSONObject asaJson = asaParser.parse(rawMessage.getBytes(StandardCharsets.UTF_8)).get(0);
        assertEquals("hostname-2", asaJson.get("syslog_host"));
    }
    
    @Test 
    public void testSyslogHostAndProg() {
    	String rawMessage = "<174>Jan  5 14:52:35 hostname-2 progName-2 %ASA-6-302015: Built inbound UDP connection 76245506 for outside:10.22.8.110/49886 (10.22.8.110/49886) to inside:192.111.72.8/8612 (192.111.72.8/8612) (user.name)";
    	JSONObject asaJson = asaParser.parse(rawMessage.getBytes(StandardCharsets.UTF_8)).get(0);
    	assertEquals("hostname-2", asaJson.get("syslog_host"));
    	assertEquals("progName-2", asaJson.get("syslog_prog"));
    }
    
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testUnexpectedMessage() {
        String rawMessage = "-- MARK --";
        UnitTestHelper.setLog4jLevel(BasicAsaParser.class, Level.FATAL);
        thrown.expect(RuntimeException.class);
        thrown.expectMessage(startsWith("[Metron] Message '-- MARK --'"));
        JSONObject asaJson = asaParser.parse(rawMessage.getBytes(StandardCharsets.UTF_8)).get(0);
        UnitTestHelper.setLog4jLevel(BasicAsaParser.class, Level.ERROR);
    }

    @Test
    public void getsReadCharsetFromConfig() {
      parserConfig.put(MessageParser.READ_CHARSET, StandardCharsets.UTF_16.toString());
      asaParser.configure(parserConfig);
      assertThat(asaParser.getReadCharset(), equalTo(StandardCharsets.UTF_16));
    }

    @Test
    public void getsReadCharsetFromDefault() {
      asaParser.configure(parserConfig);
      assertThat(asaParser.getReadCharset(), equalTo(StandardCharsets.UTF_8));
    }
}
