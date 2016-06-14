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

package org.apache.metron.parsers.mcafeefirewall;

import org.apache.metron.parsers.websphere.GrokWebSphereParser;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GrokMcAfeeFirewallParserTest {

	private Map<String, Object> parserConfig;

	@Before
	public void setup() {
		parserConfig = new HashMap<>();
		parserConfig.put("grokPath", "../metron-parsers/src/main/resources/patterns/mcafeefirewall");
		parserConfig.put("patternLabel", "MCAFEEFIREWALL");
		parserConfig.put("timestampField", "timestamp_string");
		parserConfig.put("dateFormat", "yyyy MMM dd HH:mm:ss");
	}
	@Test
	public void testParseRealLine() {
		
		//Set up parser, parse message
		GrokMcAfeeFirewallParser parser = new GrokMcAfeeFirewallParser();
		parser.configure(parserConfig);
		String testString = "<188>Apr 15 16:35:41 GMT mabm011q AclLog: mabm011q matched Outbound ACL rule (COM Baseline Firewall/#3) 60.210.64.70 -> 200.60.213.21:443 (ssl/SSL/TLS (HTTPS)) = ->PERMIT|N/A|N/A";
		List<JSONObject> result = null;
		try {
			result = parser.parse(testString.getBytes());
		} catch (Exception e) {
			fail();
		}
		JSONObject parsedJSON = result.get(0);
		
		//Compare fields
		assertEquals(parsedJSON.get("priority") + "", "188");
		assertEquals(parsedJSON.get("timestamp") + "", "1460738141000");
		assertEquals(parsedJSON.get("hostname"), "mabm011q");
		assertEquals(parsedJSON.get("firewall_rule"), "COM Baseline Firewall/#3");
		assertEquals(parsedJSON.get("firewall_direction"), "Outbound");
		assertEquals(parsedJSON.get("ip_src_addr"), "60.210.64.70");
		assertEquals(parsedJSON.get("ip_dst_addr"), "200.60.213.21");
		assertEquals(parsedJSON.get("ip_dst_port") + "", "443");
		assertEquals(parsedJSON.get("protocol"), "ssl");
		assertEquals(parsedJSON.get("subprotocol"), "SSL/TLS (HTTPS)");
		assertEquals(parsedJSON.get("action"), "PERMIT");

	}
	
	
	@Test
	public void testParseEmptyLine() {
		
		//Set up parser, attempt to parse malformed message
		GrokMcAfeeFirewallParser parser = new GrokMcAfeeFirewallParser();
		parser.configure(parserConfig);
		String testString = "";
		List<JSONObject> result = null;
		boolean hitException = false;
		try {
			result = parser.parse(testString.getBytes());
		} catch (Exception e) {
			hitException = true;
		}
		assertTrue(hitException);
	}

	@Test
	public void TestParseMalformedLine() {

		//Set up parser, parse message
		GrokMcAfeeFirewallParser parser = new GrokMcAfeeFirewallParser();
		parser.configure(parserConfig);
		String testString = "<188>Apr 15 16:35:41 GMT mabm011q AclLog: mabm011q matched Outbound ACL rule (COM Baseline Firewall/#3) 60.210.64.70 -> 200.60.213.21:443 (ssl/SSL/TLS (HTTPS))";
		List<JSONObject> result = null;
		boolean hitException = false;
		try {
			result = parser.parse(testString.getBytes());
		} catch (Exception e) {
			hitException = true;
		}
		assertTrue(hitException);
	}
		
}
