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

package org.apache.metron.parsers.rfc3164;

import oi.thekraken.grok.api.Grok;
import org.apache.metron.parsers.websphere.GrokWebSphereParser;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class GrokRFC3164Test {



	private Map<String, Object> parserConfig;


	@Before
	public void setup() {
		parserConfig = new HashMap<>();
		parserConfig.put("grokPath", "../metron-parsers/src/main/resources/patterns/rfc3164");
		parserConfig.put("patternLabel", "RFC3164");
		parserConfig.put("timestampField", "timestamp_string");
		parserConfig.put("dateFormat", "yyyy MMM dd HH:mm:ss");
	}

	@Test
	public void testParseRealLine() throws Exception {
		
		//Set up parser, parse message
		GrokRFC3164Parser parser = new GrokRFC3164Parser();
		parser.configure(parserConfig);
		String testString = "<14>May 20 08:57:57 abcen123 marathon[3040]: [2016-05-20 08:57:57,048] INFO Connecting to Zookeeper... (mesosphere.marathon.Main$:39)";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		
		//Compare fields
		assertEquals(parsedJSON.get("priority") + "", "14");
		assertEquals(parsedJSON.get("timestamp") + "", "1463749077000");
		assertEquals(parsedJSON.get("hostname"), "abcen123");
		assertEquals(parsedJSON.get("tag"), "marathon[3040]");
		assertEquals(parsedJSON.get("message"), "[2016-05-20 08:57:57,048] INFO Connecting to Zookeeper... (mesosphere.marathon.Main$:39)");

	}
	

	
	@Test
	public void tetsParseMalformedLine() throws Exception {

		//Set up parser, parse message
		GrokRFC3164Parser parser = new GrokRFC3164Parser();
		parser.configure(parserConfig);
		String testString = "<14>May 20 08:0:57  marathon[3040]: asdf9)";
		List<JSONObject> result = parser.parse(testString.getBytes());
		assertEquals(null,result);

	}
	

	
	
	@Test
	public void testParseEmptyLine() throws Exception {
		
		//Set up parser, attempt to parse malformed message
		GrokRFC3164Parser parser = new GrokRFC3164Parser();
		parser.configure(parserConfig);
		String testString = "";
		List<JSONObject> result = parser.parse(testString.getBytes());		
		assertEquals(null, result);
	}
		
}
