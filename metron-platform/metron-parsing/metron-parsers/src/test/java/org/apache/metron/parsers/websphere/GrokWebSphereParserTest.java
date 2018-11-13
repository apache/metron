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

import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.metron.parsers.interfaces.MessageParserResult;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GrokWebSphereParserTest {

	private static final ZoneId UTC = ZoneId.of("UTC");
	private Map<String, Object> parserConfig;

	@Before
	public void setup() {
		parserConfig = new HashMap<>();
		parserConfig.put("grokPath", "src/main/resources/patterns/websphere");
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
		Optional<MessageParserResult<JSONObject>> resultOptional = parser.parseOptionalResult(testString.getBytes());
		Assert.assertNotNull(resultOptional);
		Assert.assertTrue(resultOptional.isPresent());
		List<JSONObject> result = resultOptional.get().getMessages();
		JSONObject parsedJSON = result.get(0);

		long expectedTimestamp = ZonedDateTime.of(Year.now(UTC).getValue(), 4, 15, 17, 47, 28, 0, UTC).toInstant().toEpochMilli();

		//Compare fields
		assertEquals(133, parsedJSON.get("priority"));
		assertEquals(expectedTimestamp, parsedJSON.get("timestamp"));
		assertEquals("ABCXML1413", parsedJSON.get("hostname"));
		assertEquals("rojOut", parsedJSON.get("security_domain"));
		assertEquals("0x81000033", parsedJSON.get("event_code"));
		assertEquals("auth", parsedJSON.get("event_type"));
		assertEquals("notice", parsedJSON.get("severity"));
		assertEquals("login", parsedJSON.get("event_subtype"));
		assertEquals("rick007", parsedJSON.get("username"));
		assertEquals("120.43.200.6", parsedJSON.get("ip_src_addr"));
	}
	
	@Test
	public void testParseLogoutLine() throws Exception {
		
		//Set up parser, parse message
		GrokWebSphereParser parser = new GrokWebSphereParser();
		parser.configure(parserConfig);
		String testString = "<134>Apr 15 18:02:27 PHIXML3RWD [0x81000019][auth][info] [14.122.2.201]: "
				+ "User 'hjpotter' logged out from 'default'.";
		Optional<MessageParserResult<JSONObject>> resultOptional = parser.parseOptionalResult(testString.getBytes());
		Assert.assertNotNull(resultOptional);
		Assert.assertTrue(resultOptional.isPresent());
		List<JSONObject> result = resultOptional.get().getMessages();
		JSONObject parsedJSON = result.get(0);

		long expectedTimestamp = ZonedDateTime.of(Year.now(UTC).getValue(), 4, 15, 18, 2, 27, 0, UTC).toInstant().toEpochMilli();
		
		//Compare fields
		assertEquals(134, parsedJSON.get("priority"));
		assertEquals(expectedTimestamp, parsedJSON.get("timestamp"));
		assertEquals("PHIXML3RWD", parsedJSON.get("hostname"));
		assertEquals("0x81000019", parsedJSON.get("event_code"));
		assertEquals("auth", parsedJSON.get("event_type"));
		assertEquals("info", parsedJSON.get("severity"));
		assertEquals("14.122.2.201", parsedJSON.get("ip_src_addr"));
		assertEquals("hjpotter", parsedJSON.get("username"));
		assertEquals("default", parsedJSON.get("security_domain"));
	}
	
	@Test
	public void testParseRBMLine() throws Exception {
		
		//Set up parser, parse message
		GrokWebSphereParser parser = new GrokWebSphereParser();
		parser.configure(parserConfig);
		String testString = "<131>Apr 15 17:36:35 ROBXML3QRS [0x80800018][auth][error] rbm(RBM-Settings): "
				+ "trans(3502888135)[request] gtid(3502888135): RBM: Resource access denied.";
		Optional<MessageParserResult<JSONObject>> resultOptional = parser.parseOptionalResult(testString.getBytes());
		Assert.assertNotNull(resultOptional);
		Assert.assertTrue(resultOptional.isPresent());
		List<JSONObject> result = resultOptional.get().getMessages();
		JSONObject parsedJSON = result.get(0);

		long expectedTimestamp = ZonedDateTime.of(Year.now(UTC).getValue(), 4, 15, 17, 36, 35, 0, UTC).toInstant().toEpochMilli();
		
		//Compare fields
		assertEquals(131, parsedJSON.get("priority"));
		assertEquals(expectedTimestamp, parsedJSON.get("timestamp"));
		assertEquals("ROBXML3QRS", parsedJSON.get("hostname"));
		assertEquals("0x80800018", parsedJSON.get("event_code"));
		assertEquals("auth", parsedJSON.get("event_type"));
		assertEquals("error", parsedJSON.get("severity"));
		assertEquals("rbm", parsedJSON.get("process"));
		assertEquals("trans(3502888135)[request] gtid(3502888135): RBM: Resource access denied.", parsedJSON.get("message"));
	}
	
	@Test
	public void testParseOtherLine() throws Exception {
		
		//Set up parser, parse message
		GrokWebSphereParser parser = new GrokWebSphereParser();
		parser.configure(parserConfig);
		String testString = "<134>Apr 15 17:17:34 SAGPXMLQA333 [0x8240001c][audit][info] trans(191): (admin:default:system:*): "
				+ "ntp-service 'NTP Service' - Operational state down";
		Optional<MessageParserResult<JSONObject>> resultOptional = parser.parseOptionalResult(testString.getBytes());
		Assert.assertNotNull(resultOptional);
		Assert.assertTrue(resultOptional.isPresent());
		List<JSONObject> result = resultOptional.get().getMessages();
		JSONObject parsedJSON = result.get(0);
		long expectedTimestamp = ZonedDateTime.of(Year.now(UTC).getValue(), 4, 15, 17, 17, 34, 0, UTC).toInstant().toEpochMilli();
		
		//Compare fields
		assertEquals(134, parsedJSON.get("priority"));
		assertEquals(expectedTimestamp, parsedJSON.get("timestamp"));
		assertEquals("SAGPXMLQA333", parsedJSON.get("hostname"));
		assertEquals("0x8240001c", parsedJSON.get("event_code"));
		assertEquals("audit", parsedJSON.get("event_type"));
		assertEquals("info", parsedJSON.get("severity"));
		assertEquals("trans", parsedJSON.get("process"));
		assertEquals("(admin:default:system:*): ntp-service 'NTP Service' - Operational state down", parsedJSON.get("message"));
	}
	
	@Test
	public void testParseMalformedLoginLine() throws Exception {
		
		//Set up parser, attempt to parse malformed message
		GrokWebSphereParser parser = new GrokWebSphereParser();
		parser.configure(parserConfig);
		String testString = "<133>Apr 15 17:47:28 ABCXML1413 [rojOut][0x81000033][auth][notice] rick007): "
				+ "[120.43.200. User logged into 'cohlOut'.";
		Optional<MessageParserResult<JSONObject>> resultOptional = parser.parseOptionalResult(testString.getBytes());
		Assert.assertNotNull(resultOptional);
		Assert.assertTrue(resultOptional.isPresent());
		List<JSONObject> result = resultOptional.get().getMessages();
		JSONObject parsedJSON = result.get(0);

		long expectedTimestamp = ZonedDateTime.of(Year.now(UTC).getValue(), 4, 15, 17, 47, 28, 0, UTC).toInstant().toEpochMilli();

		//Compare fields
		assertEquals(133, parsedJSON.get("priority"));
		assertEquals(expectedTimestamp, parsedJSON.get("timestamp"));
		assertEquals("ABCXML1413", parsedJSON.get("hostname"));
		assertEquals("rojOut", parsedJSON.get("security_domain"));
		assertEquals("0x81000033", parsedJSON.get("event_code"));
		assertEquals("auth", parsedJSON.get("event_type"));
		assertEquals("notice", parsedJSON.get("severity"));
		assertEquals("login", parsedJSON.get("event_subtype"));
		assertEquals(null, parsedJSON.get("username"));
		assertEquals(null, parsedJSON.get("ip_src_addr"));
	}
	
	@Test
	public void testParseMalformedLogoutLine() throws Exception {
		
		//Set up parser, attempt to parse malformed message
		GrokWebSphereParser parser = new GrokWebSphereParser();
		parser.configure(parserConfig);
		String testString = "<134>Apr 15 18:02:27 PHIXML3RWD [0x81000019][auth][info] [14.122.2.201: "
				+ "User 'hjpotter' logged out from 'default.";
		Optional<MessageParserResult<JSONObject>> resultOptional = parser.parseOptionalResult(testString.getBytes());
		Assert.assertNotNull(resultOptional);
		Assert.assertTrue(resultOptional.isPresent());
		List<JSONObject> result = resultOptional.get().getMessages();
		JSONObject parsedJSON = result.get(0);

		long expectedTimestamp = ZonedDateTime.of(Year.now(UTC).getValue(), 4, 15, 18, 2, 27, 0, UTC).toInstant().toEpochMilli();
		
		//Compare fields
		assertEquals(134, parsedJSON.get("priority"));
		assertEquals(expectedTimestamp, parsedJSON.get("timestamp"));
		assertEquals("PHIXML3RWD", parsedJSON.get("hostname"));
		assertEquals("0x81000019", parsedJSON.get("event_code"));
		assertEquals("auth", parsedJSON.get("event_type"));
		assertEquals("info", parsedJSON.get("severity"));
		assertEquals(null, parsedJSON.get("ip_src_addr"));
		assertEquals(null, parsedJSON.get("username"));
		assertEquals(null, parsedJSON.get("security_domain"));
	}
	
	@Test
	public void testParseMalformedRBMLine() throws Exception {
		
		//Set up parser, parse message
		GrokWebSphereParser parser = new GrokWebSphereParser();
		parser.configure(parserConfig);
		String testString = "<131>Apr 15 17:36:35 ROBXML3QRS [0x80800018][auth][error] rbmRBM-Settings): "
				+ "trans3502888135)[request] gtid3502888135) RBM: Resource access denied.";
		Optional<MessageParserResult<JSONObject>> resultOptional = parser.parseOptionalResult(testString.getBytes());
		Assert.assertNotNull(resultOptional);
		Assert.assertTrue(resultOptional.isPresent());
		List<JSONObject> result = resultOptional.get().getMessages();
		JSONObject parsedJSON = result.get(0);

		long expectedTimestamp = ZonedDateTime.of(Year.now(UTC).getValue(), 4, 15, 17, 36, 35, 0, UTC).toInstant().toEpochMilli();
		
		//Compare fields
		assertEquals(131, parsedJSON.get("priority"));
		assertEquals(expectedTimestamp, parsedJSON.get("timestamp"));
		assertEquals("ROBXML3QRS", parsedJSON.get("hostname"));
		assertEquals("0x80800018", parsedJSON.get("event_code"));
		assertEquals("auth", parsedJSON.get("event_type"));
		assertEquals("error", parsedJSON.get("severity"));
		assertEquals(null, parsedJSON.get("process"));
		assertEquals("rbmRBM-Settings): trans3502888135)[request] gtid3502888135) RBM: Resource access denied.", parsedJSON.get("message"));
	}
	
	@Test
	public void testParseMalformedOtherLine() throws Exception {
		
		//Set up parser, parse message
		GrokWebSphereParser parser = new GrokWebSphereParser();
		parser.configure(parserConfig);
		String testString = "<134>Apr 15 17:17:34 SAGPXMLQA333 [0x8240001c][audit][info] trans 191)  admindefaultsystem*): "
				+ "ntp-service 'NTP Service' - Operational state down:";
		Optional<MessageParserResult<JSONObject>> resultOptional = parser.parseOptionalResult(testString.getBytes());
		Assert.assertNotNull(resultOptional);
		Assert.assertTrue(resultOptional.isPresent());
		List<JSONObject> result = resultOptional.get().getMessages();
		JSONObject parsedJSON = result.get(0);

		long expectedTimestamp = ZonedDateTime.of(Year.now(UTC).getValue(), 4, 15, 17, 17, 34, 0, UTC).toInstant().toEpochMilli();
		
		//Compare fields
		assertEquals(134, parsedJSON.get("priority"));
		assertEquals(expectedTimestamp, parsedJSON.get("timestamp"));
		assertEquals("SAGPXMLQA333", parsedJSON.get("hostname"));
		assertEquals("0x8240001c", parsedJSON.get("event_code"));
		assertEquals("audit", parsedJSON.get("event_type"));
		assertEquals("info", parsedJSON.get("severity"));
		assertEquals(null, parsedJSON.get("process"));
		assertEquals("trans 191)  admindefaultsystem*): ntp-service 'NTP Service' - Operational state down:", parsedJSON.get("message"));
	}
	
}
