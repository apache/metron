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

package org.apache.metron.parsers.aruba;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

public class GrokArubaParserTest {
	
	private Map<String, Object> parserConfig;
	
	@Before
	public void setup() {
		parserConfig = new HashMap<>();
		parserConfig.put("grokPath", "../metron-parsers/src/main/resources/patterns/aruba");
		parserConfig.put("patternLabel", "ARUBA");
		parserConfig.put("dateFormat", "yyyy-MM-dd HH:mm:ss");
	}
	
	//Tests a well-formed Aruba line
	@Test
	public void testParseSampleLine() throws Exception {
		
		//Set up parser, parse message
		GrokArubaParser parser = new GrokArubaParser();
		parser.configure(parserConfig);
		String testString = "<143>2016-04-29 04:27:31,823  110.137.3.2 CPPM_Session_Detail 473964876 1 0 "
				+ "id=5983679670,session_id=R011cf48e-04-57231aa3,type=RADIUS_IN,attr_name=Radius:IETF:User-Name,"
				+ "attr_value=ac81125d02ea,timestamp=2016-04-29 04:26:13.002367-04";
		System.out.println(testString);
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		System.out.println(parsedJSON);

    Assert.assertEquals(parsedJSON.get("priority"), 143);
		Assert.assertEquals(parsedJSON.get("ip_src_addr"), "110.137.3.2");
		Assert.assertEquals(parsedJSON.get("category"), "CPPM_Session_Detail");
		Assert.assertEquals(parsedJSON.get("message_id"), 473964876);
		Assert.assertEquals(parsedJSON.get("timestamp") , 1461904051000L);
		Assert.assertEquals(parsedJSON.get("attr_name"), "Radius:IETF:User-Name");
		Assert.assertEquals(parsedJSON.get("session_id"), "R011cf48e-04-57231aa3");
		Assert.assertEquals(parsedJSON.get("message_id"), 473964876);
		Assert.assertEquals(parsedJSON.get("id"), "5983679670");
		Assert.assertEquals(parsedJSON.get("type"), "RADIUS_IN");
		Assert.assertEquals(parsedJSON.get("request_timestamp"), "2016-04-29 04:26:13.002367-04");
  }
	
	//Tests a malformed Aruba message; parser should return null
	@Test
	public void testParseMalformedLine() throws Exception {
		//Set up parser, attempt to parse message
		GrokArubaParser parser = new GrokArubaParser();
		parser.configure(parserConfig);
		String testString = "<143>2016-04-9 04:27:31,823  110.137.3.2 CPPM_Session_Detail 473964876 1 0 "
				+ "id=5983679670,session_id=R011cf48e-04-57231aa3,type=RADIUS_IN,atr_name=Radius:IETF:User-Name,"
				+ "attr_value=ac81125d02ea,timestamp=2016-04-29 04:26:13.002367-04";
		List<JSONObject> result = parser.parse(testString.getBytes());
		
		//Result should be null due to malformed message
		Assert.assertEquals(result, null);
	}
	
	//Tests a blank Aruba message; parser should return null
	@Test
	public void testParseEmptyLine() throws Exception {
		//Set up parser, attempt to parse message
		GrokArubaParser parser = new GrokArubaParser();
		parser.configure(parserConfig);
		String testString = "";
		List<JSONObject> result = parser.parse(testString.getBytes());
		
		//Result should be null due to malformed message
		Assert.assertEquals(result, null);
	}

	@Test
	public void testPrint() throws Exception {
		GrokArubaParser parser = new GrokArubaParser();
		parser.configure(parserConfig);
		String[] lines = { "<143>2016-04-29 04:27:31,817  120.44.205.9 CPPM_Session_Detail 473963971 1 0 id=5983678767,session_id=R011cf475-04-57231a94,type=AUTH_IN,attr_name=Authorization:[Insight Repository]:Minutes-Since-Auth,attr_value=82775,timestamp=2016-04-29 04:25:56.77251-04",
				"<143>2016-04-29 04:27:31,819  10.148.5.141 CPPM_Session_Detail 473964265 1 0 id=5983679060,session_id=R011cf47d-04-57231a9b,type=INTERNAL_IN,attr_name=Connection:SSID,attr_value= ,timestamp=2016-04-29 04:26:04.866339-04",
				"<143>2016-04-29 04:27:31,823  110.137.3.2 CPPM_Session_Detail 473964876 1 0 id=5983679670,session_id=R011cf48e-04-57231aa3,type=RADIUS_IN,attr_name=Radius:IETF:User-Name,attr_value=ac81125d02ea,timestamp=2016-04-29 04:26:13.002367-04"
				};
		for (String line : lines) {
			System.out.println(			parser.parse(line.getBytes()).get(0) );
		}
	}

}
