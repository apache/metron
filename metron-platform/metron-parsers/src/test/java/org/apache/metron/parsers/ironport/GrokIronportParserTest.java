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

package org.apache.metron.parsers.ironport;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class GrokIronportParserTest {

	private Map<String, Object> parserConfig;

	@Before
	public void setup() {
		parserConfig = new HashMap<>();
		parserConfig.put("grokPath", "../metron-parsers/src/main/resources/patterns/ironport");
		parserConfig.put("patternLabel", "IRONPORT");
		parserConfig.put("timestampField", "timestamp_string");
		parserConfig.put("dateFormat", "yyyy MMM dd HH:mm:ss");
	}
	
	@Test
	public void testParseRealLine() throws Exception {
		
		//Set up parser, parse message
		GrokIronportParser parser = new GrokIronportParser();
		parser.configure(parserConfig);
		String testString = "<22>May 05 10:41:27 infosec_OutboundMailLogs: Info: MID 33333333 DKIM: signing with abc_com - matches MicrosoftExchange333333eeeeeeeeee3333333333eeeeee@abc.com";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);


		//Compare fields
		assertEquals(parsedJSON.get("priority") + "", "22");
		assertEquals(parsedJSON.get("timestamp") + "", "1462444887000");
		assertEquals(parsedJSON.get("message"), "MID 33333333 DKIM: signing with abc_com - matches MicrosoftExchange333333eeeeeeeeee3333333333eeeeee@abc.com");
		assertEquals(parsedJSON.get("source"), "infosec_OutboundMailLogs");
		assertEquals(parsedJSON.get("level"), "Info");

	}

	@Test
	public void testParseMalformedOtherLine() throws Exception {
		
		//Set up parser, parse message
		GrokIronportParser parser = new GrokIronportParser();
		parser.configure(parserConfig);
		String testString = "<134>Apr 15 17:17:34 SAGPXMLQA333 [0x8240001c][audit][info] trans 191)  admindefaultsystem*): ";
		List<JSONObject> result = parser.parse(testString.getBytes());
		assertEquals(null, result);

	}
	
	
	@Test
	public void testParseEmptyLine() throws Exception {
		
		//Set up parser, attempt to parse malformed message
		GrokIronportParser parser = new GrokIronportParser();
		parser.configure(parserConfig);
		String testString = "";
		List<JSONObject> result = parser.parse(testString.getBytes());		
		assertEquals(null, result);
	}
		
}
