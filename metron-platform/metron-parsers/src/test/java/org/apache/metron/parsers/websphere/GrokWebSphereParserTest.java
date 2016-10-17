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

package org.apache.metron.parsers.websphere;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GrokWebSphereParserTest {

	private Map<String, Object> parserConfig;

	@Before
	public void setup() {
    String[] grokPattern = {"# Months - only three-letter code is used",
            "MONTH \\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec?)\\b",
            "# Days - two digit number is used",
            "DAY \\d{1,2}",
            "# Time - two digit hour, minute, and second",
            "TIME \\d{2}:\\d{2}:\\d{2}",
            "# Timestamp - month, day, and time",
            "TIMESTAMP %{MONTH:UNWANTED}\\s+%{DAY:UNWANTED} %{TIME:UNWANTED}",
            "# Generic word field",
            "WORD \\w+",
            "# Priority",
            "PRIORITY \\d+",
            "# Log start - the first part of the log line",
            "LOGSTART <%{PRIORITY:priority}>?%{TIMESTAMP:timestamp_string} %{WORD:hostname}",
            "# Security domain",
            "SECURITY_DOMAIN [%{WORD:security_domain}]",
            "# Log middle - the middle part of the log line",
            "LOGMIDDLE (\\[%{WORD:security_domain}\\])?\\[%{WORD:event_code}\\]\\[%{WORD:event_type}\\]\\[%{WORD:severity}\\]",
            "# Define IP address formats",
            "IPV6 ((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:)))(%.+)?",
            "IPV4 (?<![0-9])(?:(?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})[.](?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})[.](?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})[.](?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2}))(?![0-9])",
            "IP (?:%{IPV6:UNWANTED}|%{IPV4:UNWANTED})",
            "# Message - the message body of the log",
            "MESSAGE .*",
            "# WebSphere - the entire log message",
            "WEBSPHERE %{LOGSTART:UNWANTED} %{LOGMIDDLE:UNWANTED} %{MESSAGE:message}"};
		parserConfig = new HashMap<>();
		parserConfig.put("grokPattern", grokPattern);
		parserConfig.put("patternLabel", "WEBSPHERE");
		parserConfig.put("timestampField", "timestamp_string");
		parserConfig.put("dateFormat", "yyyy MMM dd HH:mm:ss");
	}
	
	@Test
	public void testParseLoginLine() throws Exception {
		
		//Set up parser, parse message
		GrokWebSphereParser parser = new GrokWebSphereParser();
		parser.configure(parserConfig);
		String testString = "<133>Apr 15 17:47:28 ABCXML1413 [rojOut][0x81000033][auth][notice] user(rick007): "
				+ "[120.43.200.6]: User logged into 'cohlOut'.";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		
		//Compare fields
		assertEquals(parsedJSON.get("priority") + "", "133");
		assertEquals(parsedJSON.get("timestamp") + "", "1460742448000");
		assertEquals(parsedJSON.get("hostname"), "ABCXML1413");
		assertEquals(parsedJSON.get("security_domain"), "rojOut");
		assertEquals(parsedJSON.get("event_code"), "0x81000033");
		assertEquals(parsedJSON.get("event_type"), "auth");
		assertEquals(parsedJSON.get("severity"), "notice");
		assertEquals(parsedJSON.get("event_subtype"), "login");
		assertEquals(parsedJSON.get("username"), "rick007");
		assertEquals(parsedJSON.get("ip_src_addr"), "120.43.200.6");
	}
	
	@Test
	public void tetsParseLogoutLine() throws Exception {
		
		//Set up parser, parse message
		GrokWebSphereParser parser = new GrokWebSphereParser();
		parser.configure(parserConfig);
		String testString = "<134>Apr 15 18:02:27 PHIXML3RWD [0x81000019][auth][info] [14.122.2.201]: "
				+ "User 'hjpotter' logged out from 'default'.";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		
		//Compare fields
		assertEquals(parsedJSON.get("priority") + "", "134");
		assertEquals(parsedJSON.get("timestamp") + "", "1460743347000");
		assertEquals(parsedJSON.get("hostname"), "PHIXML3RWD");
		assertEquals(parsedJSON.get("event_code"), "0x81000019");
		assertEquals(parsedJSON.get("event_type"), "auth");
		assertEquals(parsedJSON.get("severity"), "info");
		assertEquals(parsedJSON.get("ip_src_addr"), "14.122.2.201");
		assertEquals(parsedJSON.get("username"), "hjpotter");
		assertEquals(parsedJSON.get("security_domain"), "default");
	}
	
	@Test
	public void tetsParseRBMLine() throws Exception {
		
		//Set up parser, parse message
		GrokWebSphereParser parser = new GrokWebSphereParser();
		parser.configure(parserConfig);
		String testString = "<131>Apr 15 17:36:35 ROBXML3QRS [0x80800018][auth][error] rbm(RBM-Settings): "
				+ "trans(3502888135)[request] gtid(3502888135): RBM: Resource access denied.";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		
		//Compare fields
		assertEquals(parsedJSON.get("priority") + "", "131");
		assertEquals(parsedJSON.get("timestamp") + "", "1460741795000");
		assertEquals(parsedJSON.get("hostname"), "ROBXML3QRS");
		assertEquals(parsedJSON.get("event_code"), "0x80800018");
		assertEquals(parsedJSON.get("event_type"), "auth");
		assertEquals(parsedJSON.get("severity"), "error");
		assertEquals(parsedJSON.get("process"), "rbm");
		assertEquals(parsedJSON.get("message"), "trans(3502888135)[request] gtid(3502888135): RBM: Resource access denied.");
	}
	
	@Test
	public void tetsParseOtherLine() throws Exception {
		
		//Set up parser, parse message
		GrokWebSphereParser parser = new GrokWebSphereParser();
		parser.configure(parserConfig);
		String testString = "<134>Apr 15 17:17:34 SAGPXMLQA333 [0x8240001c][audit][info] trans(191): (admin:default:system:*): "
				+ "ntp-service 'NTP Service' - Operational state down";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		
		//Compare fields
		assertEquals(parsedJSON.get("priority") + "", "134");
		assertEquals(parsedJSON.get("timestamp") + "", "1460740654000");
		assertEquals(parsedJSON.get("hostname"), "SAGPXMLQA333");
		assertEquals(parsedJSON.get("event_code"), "0x8240001c");
		assertEquals(parsedJSON.get("event_type"), "audit");
		assertEquals(parsedJSON.get("severity"), "info");
		assertEquals(parsedJSON.get("process"), "trans");
		assertEquals(parsedJSON.get("message"), "(admin:default:system:*): ntp-service 'NTP Service' - Operational state down");
	}
	
	@Test
	public void testParseMalformedLoginLine() throws Exception {
		
		//Set up parser, attempt to parse malformed message
		GrokWebSphereParser parser = new GrokWebSphereParser();
		parser.configure(parserConfig);
		String testString = "<133>Apr 15 17:47:28 ABCXML1413 [rojOut][0x81000033][auth][notice] rick007): "
				+ "[120.43.200. User logged into 'cohlOut'.";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);

		//Compare fields
		assertEquals(parsedJSON.get("priority") + "", "133");
		assertEquals(parsedJSON.get("timestamp") + "", "1460742448000");
		assertEquals(parsedJSON.get("hostname"), "ABCXML1413");
		assertEquals(parsedJSON.get("security_domain"), "rojOut");
		assertEquals(parsedJSON.get("event_code"), "0x81000033");
		assertEquals(parsedJSON.get("event_type"), "auth");
		assertEquals(parsedJSON.get("severity"), "notice");
		assertEquals(parsedJSON.get("event_subtype"), "login");
		assertEquals(parsedJSON.get("username"), null);
		assertEquals(parsedJSON.get("ip_src_addr"), null);
	}
	
	@Test
	public void tetsParseMalformedLogoutLine() throws Exception {
		
		//Set up parser, attempt to parse malformed message
		GrokWebSphereParser parser = new GrokWebSphereParser();
		parser.configure(parserConfig);
		String testString = "<134>Apr 15 18:02:27 PHIXML3RWD [0x81000019][auth][info] [14.122.2.201: "
				+ "User 'hjpotter' logged out from 'default.";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		
		//Compare fields
		assertEquals(parsedJSON.get("priority") + "", "134");
		assertEquals(parsedJSON.get("timestamp") + "", "1460743347000");
		assertEquals(parsedJSON.get("hostname"), "PHIXML3RWD");
		assertEquals(parsedJSON.get("event_code"), "0x81000019");
		assertEquals(parsedJSON.get("event_type"), "auth");
		assertEquals(parsedJSON.get("severity"), "info");
		assertEquals(parsedJSON.get("ip_src_addr"), null);
		assertEquals(parsedJSON.get("username"), null);
		assertEquals(parsedJSON.get("security_domain"), null);
	}
	
	@Test
	public void tetsParseMalformedRBMLine() throws Exception {
		
		//Set up parser, parse message
		GrokWebSphereParser parser = new GrokWebSphereParser();
		parser.configure(parserConfig);
		String testString = "<131>Apr 15 17:36:35 ROBXML3QRS [0x80800018][auth][error] rbmRBM-Settings): "
				+ "trans3502888135)[request] gtid3502888135) RBM: Resource access denied.";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		
		//Compare fields
		assertEquals(parsedJSON.get("priority") + "", "131");
		assertEquals(parsedJSON.get("timestamp") + "", "1460741795000");
		assertEquals(parsedJSON.get("hostname"), "ROBXML3QRS");
		assertEquals(parsedJSON.get("event_code"), "0x80800018");
		assertEquals(parsedJSON.get("event_type"), "auth");
		assertEquals(parsedJSON.get("severity"), "error");
		assertEquals(parsedJSON.get("process"), null);
		assertEquals(parsedJSON.get("message"), "rbmRBM-Settings): trans3502888135)[request] gtid3502888135) RBM: Resource access denied.");
	}
	
	@Test
	public void tetsParseMalformedOtherLine() throws Exception {
		
		//Set up parser, parse message
		GrokWebSphereParser parser = new GrokWebSphereParser();
		parser.configure(parserConfig);
		String testString = "<134>Apr 15 17:17:34 SAGPXMLQA333 [0x8240001c][audit][info] trans 191)  admindefaultsystem*): "
				+ "ntp-service 'NTP Service' - Operational state down:";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		
		//Compare fields
		assertEquals(parsedJSON.get("priority") + "", "134");
		assertEquals(parsedJSON.get("timestamp") + "", "1460740654000");
		assertEquals(parsedJSON.get("hostname"), "SAGPXMLQA333");
		assertEquals(parsedJSON.get("event_code"), "0x8240001c");
		assertEquals(parsedJSON.get("event_type"), "audit");
		assertEquals(parsedJSON.get("severity"), "info");
		assertEquals(parsedJSON.get("process"), null);
		assertEquals(parsedJSON.get("message"), "trans 191)  admindefaultsystem*): "
				+ "ntp-service 'NTP Service' - Operational state down:");
	}
	
	
	@Test(expected=RuntimeException.class)
	public void testParseEmptyLine() throws Exception {
		
		//Set up parser, attempt to parse malformed message
		GrokWebSphereParser parser = new GrokWebSphereParser();
		parser.configure(parserConfig);
		String testString = "";
		List<JSONObject> result = parser.parse(testString.getBytes());
	}
		
}
