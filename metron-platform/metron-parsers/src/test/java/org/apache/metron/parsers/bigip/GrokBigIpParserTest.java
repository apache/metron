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

package org.apache.metron.parsers.bigip;

import org.apache.metron.parsers.bigip.GrokBigIpParser;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class GrokBigIpParserTest {

	private Map<String, Object> parserConfig;

	@Before
	public void setup() {
		parserConfig = new HashMap<>();
		parserConfig.put("grokPath", "../metron-parsers/src/main/resources/patterns/bigip");
		parserConfig.put("patternLabel", "BIGIP");
		parserConfig.put("timestampField", "timestamp_string");
		parserConfig.put("dateFormat", "yyyy MMM dd HH:mm:ss");
	}


	@Test
	public void testParseNewSessionLine() throws Exception {
		
		//Set up parser, parse message
		GrokBigIpParser parser = new GrokBigIpParser();
		parser.configure(parserConfig);
		String testString = "<141>Apr 19 19:24:29 mfugjchwna38k notice mdd4[6456]: 90329500:5: 37451372: New session from client IP 10.000.11.000 (ST=/CC=/C=) at VIP 240.61.45.23 Listener /Common/access.google.com_443 (Reputation=Unknown)";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		
		//Compare fields
		assertEquals(parsedJSON.get("priority").toString(), "141");
		assertEquals(parsedJSON.get("timestamp").toString(), "1461093869000");
		assertEquals(parsedJSON.get("hostname").toString(), "mfugjchwna38k");
		assertEquals(parsedJSON.get("severity").toString(), "notice");
		assertEquals(parsedJSON.get("process").toString(), "mdd4");
		assertEquals(parsedJSON.get("process_id").toString(), "6456");
		assertEquals(parsedJSON.get("ip_src_addr").toString(), "10.000.11.000");
		assertEquals(parsedJSON.get("ip_vip").toString(), "240.61.45.23");
		assertEquals(parsedJSON.get("big_ip_log_code").toString(), "90329500");
		assertEquals(parsedJSON.get("big_ip_message_type").toString(), "session");
		assertEquals(parsedJSON.get("vpn_session_id").toString(), "37451372");
		assertEquals(parsedJSON.get("original_string").toString(), "<141>Apr 19 19:24:29 mfugjchwna38k notice mdd4[6456]: 90329500:5: 37451372: New session from client IP 10.000.11.000 (ST=/CC=/C=) at VIP 240.61.45.23 Listener /Common/access.google.com_443 (Reputation=Unknown)");

	}
	
	@Test
	public void testParseLoginLine() throws Exception {
		
		//Set up parser, parse message
		GrokBigIpParser parser = new GrokBigIpParser();
		parser.configure(parserConfig);
		String testString = "<141>Mar 31 13:46:35 mfugjchwna38k notice apd[6848]: 90329113:5: 1d0bf7c7: session.logon.euid is ABC906";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		 
		
		//Compare fields
		assertEquals(parsedJSON.get("priority").toString(), "141");
		assertEquals(parsedJSON.get("timestamp").toString(), "1459431995000");
		assertEquals(parsedJSON.get("hostname").toString(), "mfugjchwna38k");
		assertEquals(parsedJSON.get("severity").toString(), "notice");
		assertEquals(parsedJSON.get("process").toString(), "apd");
		assertEquals(parsedJSON.get("process_id").toString(), "6848");
		assertEquals(parsedJSON.get("big_ip_log_code").toString(), "90329113");
		assertEquals(parsedJSON.get("big_ip_message_type").toString(), "login");
		assertEquals(parsedJSON.get("vpn_session_id").toString(), "1d0bf7c7");
		assertEquals(parsedJSON.get("user_id").toString(), "ABC906");
		assertEquals(parsedJSON.get("original_string").toString(), "<141>Mar 31 13:46:35 mfugjchwna38k notice apd[6848]: 90329113:5: 1d0bf7c7: session.logon.euid is ABC906");
	}
	
	@Test
	public void testParseSessionStatisticsLine() throws Exception {
		
		//Set up parser, parse message
		GrokBigIpParser parser = new GrokBigIpParser();
		parser.configure(parserConfig);
		String testString = "<141>Mar 31 13:59:37 mfugjchwna38k notice mdd[6456]: 90329521:5: 690fe490: Session statistics - bytes in: 3032, bytes out: 469";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		
		//Compare fields
		assertEquals(parsedJSON.get("priority").toString(), "141");
		assertEquals(parsedJSON.get("timestamp").toString(), "1459432777000");
		assertEquals(parsedJSON.get("hostname").toString(), "mfugjchwna38k");
		assertEquals(parsedJSON.get("severity").toString(), "notice");
		assertEquals(parsedJSON.get("process").toString(), "mdd");
		assertEquals(parsedJSON.get("process_id").toString(), "6456");
		assertEquals(parsedJSON.get("big_ip_log_code").toString(), "90329521");
		assertEquals(parsedJSON.get("vpn_session_id").toString(), "690fe490");
		assertEquals(parsedJSON.get("big_ip_message_type").toString(), "statistics");
		assertEquals(parsedJSON.get("bytes_in").toString(), "3032");
		assertEquals(parsedJSON.get("bytes_out").toString(), "469");
		assertEquals(parsedJSON.get("original_string").toString(), "<141>Mar 31 13:59:37 mfugjchwna38k notice mdd[6456]: 90329521:5: 690fe490: Session statistics - bytes in: 3032, bytes out: 469");
	}
	
	@Test
	public void testParseAccessPolicyResultLine() throws Exception {
		
		//Set up parser, parse message
		GrokBigIpParser parser = new GrokBigIpParser();
		parser.configure(parserConfig);
		String testString = "<141>Mar 31 13:59:32 vdcbigaion02p notice apd[26861]: 90329102:5: 01030c62: Access policy result: Network_Access";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		
		//Compare fields
		assertEquals(parsedJSON.get("priority").toString(), "141");
		assertEquals(parsedJSON.get("timestamp").toString(), "1459432772000");
		assertEquals(parsedJSON.get("hostname").toString(), "vdcbigaion02p");
		assertEquals(parsedJSON.get("severity").toString(), "notice");
		assertEquals(parsedJSON.get("process").toString(), "apd");
		assertEquals(parsedJSON.get("process_id").toString(), "26861");
		assertEquals(parsedJSON.get("big_ip_log_code").toString(), "90329102");
		assertEquals(parsedJSON.get("vpn_session_id").toString(), "01030c62");
		assertEquals(parsedJSON.get("big_ip_message_type").toString(), "access policy result");
		assertEquals(parsedJSON.get("access_policy_result").toString(), "Network_Access");
		assertEquals(parsedJSON.get("original_string").toString(), "<141>Mar 31 13:59:32 vdcbigaion02p notice apd[26861]: 90329102:5: 01030c62: Access policy result: Network_Access");
	}

	@Test
	public void testParseOtherSessionLine() throws Exception {

		//Set up parser, parse message
		GrokBigIpParser parser = new GrokBigIpParser();
		parser.configure(parserConfig);
		String testString = "<141>Mar 31 13:59:31 mfugjchwna38k notice apd[6848]: 90329115:5: 090faa3e: Following rule 'fallback' from item 'clog1220' to terminalout 'Out'";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);

		//Compare fields
		assertEquals(parsedJSON.get("priority").toString(), "141");
		assertEquals(parsedJSON.get("timestamp").toString(), "1459432771000");
		assertEquals(parsedJSON.get("hostname").toString(), "mfugjchwna38k");
		assertEquals(parsedJSON.get("severity").toString(), "notice");
		assertEquals(parsedJSON.get("process").toString(), "apd");
		assertEquals(parsedJSON.get("process_id").toString(), "6848");
		assertEquals(parsedJSON.get("big_ip_log_code").toString(), "90329115");
		assertEquals(parsedJSON.get("big_ip_message_type").toString(), "session");
		assertEquals(parsedJSON.get("vpn_session_id").toString(), "090faa3e");
		assertEquals(parsedJSON.get("message").toString(), "Following rule 'fallback' from item 'clog1220' to terminalout 'Out'");
		assertEquals(parsedJSON.get("original_string").toString(), "<141>Mar 31 13:59:31 mfugjchwna38k notice apd[6848]: 90329115:5: 090faa3e: Following rule 'fallback' from item 'clog1220' to terminalout 'Out'");
	}

	@Test
	public void testParseSystemLine() throws Exception {

		//Set up parser, parse message
		GrokBigIpParser parser = new GrokBigIpParser();
		parser.configure(parserConfig);
		String testString = "<182>Mar 31 13:59:34 vdcbigaion01p info logger: [ssl_acc] 10.24.248.20 - admin [31/Mar/2016:13:59:34 +0000] \"/iControl/iControlPortal.cgi\" 200 670";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);

		//Compare fields
		assertEquals(parsedJSON.get("priority").toString(), "182");
		assertEquals(parsedJSON.get("timestamp").toString(), "1459432774000");
		assertEquals(parsedJSON.get("hostname").toString(), "vdcbigaion01p");
		assertEquals(parsedJSON.get("severity").toString(), "info");
		assertEquals(parsedJSON.get("big_ip_message_type").toString(), "system");
		assertEquals(parsedJSON.get("message").toString(), "logger: [ssl_acc] 10.24.248.20 - admin [31/Mar/2016:13:59:34 +0000] \"/iControl/iControlPortal.cgi\" 200 670");
		assertEquals(parsedJSON.get("original_string").toString(), "<182>Mar 31 13:59:34 vdcbigaion01p info logger: [ssl_acc] 10.24.248.20 - admin [31/Mar/2016:13:59:34 +0000] \"/iControl/iControlPortal.cgi\" 200 670");
	}

	@Test
	public void testParseMalformedNewSessionLine() throws Exception {

		//Set up parser, attempt to parse malformed message
		GrokBigIpParser parser = new GrokBigIpParser();
		parser.configure(parserConfig);
		String testString = "141>Apr 19 19:24:29 mfugjchwna38k notice mdd4[6456]: 90329500:5: 37451372: New session from client IP 10.000.11.000 (ST=/CC=/C=) at VIP 123.45.678.90 Listener /Common/access.google.com_443 (Reputation=Unknown)";
		List<JSONObject> result = parser.parse(testString.getBytes());
		assertEquals(null, result);
	}
	@Test
	public void testParseMalformedLoginLine() throws Exception {

		//Set up parser, attempt to parse malformed message
		GrokBigIpParser parser = new GrokBigIpParser();
		parser.configure(parserConfig);
		String testString = "<141>Mar 31 13:46:35 mfugjchwna38k apd[6848]: 90329113:5: 1d0bf7c7: session.logon.euid is ABC906";
		List<JSONObject> result = parser.parse(testString.getBytes());
		assertEquals(null, result);
	}
	
	@Test
	public void testParseMalformedSessionStatisticsLine() throws Exception {

		//Set up parser, attempt to parse malformed message
		GrokBigIpParser parser = new GrokBigIpParser();
		parser.configure(parserConfig);
		String testString = "<141>\nMar 31 13:59:37 mfugjchwna38k notice mdd[6456]: 90329521:5: 690fe490: Session statistics - bytes in: 3032, bytes out: 469";
		List<JSONObject> result = parser.parse(testString.getBytes());
		assertEquals(null, result);
	}
	
	@Test
	public void testParseMalformedAccessPolicyResultLine() throws Exception {

		//Set up parser, attempt to parse malformed message
		GrokBigIpParser parser = new GrokBigIpParser();
		parser.configure(parserConfig);
		String testString = "<141>Mar 31 13:59:32 vdcbigaion02p notice apd[26861]: 90329102:5: 01030c62: Access policy result:";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);

		//Compare fields
		assertEquals(parsedJSON.get("priority").toString(), "141");
		assertEquals(parsedJSON.get("timestamp").toString(), "1459432772000");
		assertEquals(parsedJSON.get("hostname").toString(), "vdcbigaion02p");
		assertEquals(parsedJSON.get("severity").toString(), "notice");
		assertEquals(parsedJSON.get("big_ip_message_type").toString(), "access policy result");
		assertEquals(parsedJSON.get("original_string").toString(), "<141>Mar 31 13:59:32 vdcbigaion02p notice apd[26861]: 90329102:5: 01030c62: Access policy result:");

	}
	
	@Test
	public void testParseMalformedOtherSessionLine() throws Exception {

		//Set up parser, attempt to parse malformed message
		GrokBigIpParser parser = new GrokBigIpParser();
		parser.configure(parserConfig);
		String testString = "Mar 31 13:59:31 mfugjchwna38k notice apd[6848]: 90329115:5: 090faa3e: Following rule 'fallback' from item 'clog1220' to terminalout 'Out'";
		List<JSONObject> result = parser.parse(testString.getBytes());
		assertEquals(null, result);
	}

	@Test
	public void testParseMalformedSystemLine() throws Exception {

		//Set up parser, attempt to parse malformed message
		GrokBigIpParser parser = new GrokBigIpParser();
		parser.configure(parserConfig);
		String testString = "<182>Mar 31 13:59:34 vdcbigaion01p info logger: [ssl_acc] 10.24.248.20 - admin [31/Mar/2016:13:59:34 +0000] \"/iControl/iControlPortal.cgi\" 200 670";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);

		//Compare fields
		assertEquals(parsedJSON.get("priority").toString(), "182");
		assertEquals(parsedJSON.get("timestamp").toString(), "1459432774000");
		assertEquals(parsedJSON.get("hostname").toString(), "vdcbigaion01p");
		assertEquals(parsedJSON.get("severity").toString(), "info");
		assertEquals(parsedJSON.get("big_ip_message_type").toString(), "system");
		assertEquals(parsedJSON.get("message").toString(), "logger: [ssl_acc] 10.24.248.20 - admin [31/Mar/2016:13:59:34 +0000] \"/iControl/iControlPortal.cgi\" 200 670");
		assertEquals(parsedJSON.get("original_string").toString(), "<182>Mar 31 13:59:34 vdcbigaion01p info logger: [ssl_acc] 10.24.248.20 - admin [31/Mar/2016:13:59:34 +0000] \"/iControl/iControlPortal.cgi\" 200 670");

	}
	
	@Test
	public void testParseEmptyLine() throws Exception {
		
		//Set up parser, attempt to parse empty message
		GrokBigIpParser parser = new GrokBigIpParser();
		parser.configure(parserConfig);
		String testString = "";
		List<JSONObject> result = parser.parse(testString.getBytes());		
		assertEquals(null, result);
	}
		
}
