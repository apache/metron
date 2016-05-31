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

package org.apache.metron.parsers.unixsyslog5424;

import org.apache.metron.parsers.websphere.GrokWebSphereParser;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class GrokUnixSyslog5424ParserTest {


	private Map<String, Object> parserConfig;

	@Before
	public void setup() {
		parserConfig = new HashMap<>();
		parserConfig.put("grokPath", "../metron-parsers/src/main/resources/patterns/unixsyslog5424");
		parserConfig.put("patternLabel", "UNIXSYSLOG5424");
		parserConfig.put("timestampField", "timestamp_string");
		parserConfig.put("dateFormat", "yyyy-MM-dd'T'HH:mm:ss.SSS");
	}


	@Test
	public void testParseRealLine() throws Exception {
		
		//Set up parser, parse message
		GrokUnixSyslog5424Parser parser = new GrokUnixSyslog5424Parser();
		parser.configure(parserConfig);
		String testString = "<166>2016-05-20T12:53:01.034Z vpcr07.abc.google.com Vpxa: [71237B90 verbose 'hostdstats'] Set internal stats for VM: 22 (vpxa VM id), 30997 (vpxd VM id). Is FT primary? false";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		
		//Compare fields
		assertEquals(parsedJSON.get("priority") + "", "166");
		assertEquals(parsedJSON.get("timestamp") + "", "1463748781034");
		assertEquals(parsedJSON.get("hostname"), "vpcr07.abc.google.com");
		assertEquals(parsedJSON.get("tag"), "Vpxa");
		assertEquals(parsedJSON.get("extra_info"), "71237B90 verbose 'hostdstats'");
		assertEquals(parsedJSON.get("message"), "Set internal stats for VM: 22 (vpxa VM id), 30997 (vpxd VM id). Is FT primary? false");

	}

	@Test
	public void testParseWithoutExtraInfo() throws Exception {

		//Set up parser, parse message
		GrokUnixSyslog5424Parser parser = new GrokUnixSyslog5424Parser();
		parser.configure(parserConfig);
		String testString = "<166>2016-05-20T12:53:01.034Z vpcr07.abc.google.com Vpxa: Set internal stats for VM: 22 (vpxa VM id), 30997 (vpxd VM id). Is FT primary? false";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);

		//Compare fields
		assertEquals(parsedJSON.get("priority") + "", "166");
		assertEquals(parsedJSON.get("timestamp") + "", "1463748781034");
		assertEquals(parsedJSON.get("hostname"), "vpcr07.abc.google.com");
		assertEquals(parsedJSON.get("tag"), "Vpxa");
		assertEquals(parsedJSON.get("message"), "Set internal stats for VM: 22 (vpxa VM id), 30997 (vpxd VM id). Is FT primary? false");

	}

	@Test
	public void testParseShortTimestamp() throws Exception {

		//Set up parser, parse message
		GrokUnixSyslog5424Parser parser = new GrokUnixSyslog5424Parser();
		parser.configure(parserConfig);
		String testString = "<166>2016-05-20T12:53:01.03Z vpcr07.abc.google.com Vpxa: [71237B90 verbose 'hostdstats'] Set internal stats for VM: 22 (vpxa VM id), 30997 (vpxd VM id). Is FT primary? false";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);

		//Compare fields
		assertEquals(parsedJSON.get("priority") + "", "166");
		assertEquals(parsedJSON.get("timestamp") + "", "1463748781030");
		assertEquals(parsedJSON.get("hostname"), "vpcr07.abc.google.com");
		assertEquals(parsedJSON.get("tag"), "Vpxa");
		assertEquals(parsedJSON.get("extra_info"), "71237B90 verbose 'hostdstats'");
		assertEquals(parsedJSON.get("message"), "Set internal stats for VM: 22 (vpxa VM id), 30997 (vpxd VM id). Is FT primary? false");

	}

	@Test
	public void testParseMalformedLine() throws Exception {

		//Set up parser, parse message
		GrokUnixSyslog5424Parser parser = new GrokUnixSyslog5424Parser();
		parser.configure(parserConfig);
		String testString = "<1662016-05-20T12:53:01.03Z Vpxa Set internal stats for VM: 22 (vpxa VM id), 30997 (vpxd VM id). Is FT primary? false";
		List<JSONObject> result = parser.parse(testString.getBytes());

		assertEquals(result, null);

	}

	
	@Test
	public void testParseEmptyLine() throws Exception {
		
		//Set up parser, attempt to parse malformed message
		GrokUnixSyslog5424Parser parser = new GrokUnixSyslog5424Parser();
		parser.configure(parserConfig);
		String testString = "";
		List<JSONObject> result = parser.parse(testString.getBytes());		
		assertEquals(null, result);
	}
		
}
