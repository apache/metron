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

import java.util.List;

import org.json.simple.JSONObject;
import org.junit.Test;

import junit.framework.Assert;

public class GrokArubaParserTest {
	
	private final String grokPath = "../metron-parsers/src/main/resources/patterns/aruba";
	private final String grokLabel = "ARUBA";
	private final String dateFormat = "yyyy-MM-dd HH:mm:ss";
	
	//Tests a well-formed Aruba line
	@Test
	public void testParseSampleLine() throws Exception {
		
		//Set up parser, parse message
		GrokArubaParser parser = new GrokArubaParser(grokPath, grokLabel);
		parser.withDateFormat(dateFormat);
		String testString = "<143>2016-04-29 04:27:31,823  110.137.3.2 CPPM_Session_Detail 473964876 1 0 "
				+ "id=5983679670,session_id=R011cf48e-04-57231aa3,type=RADIUS_IN,attr_name=Radius:IETF:User-Name,"
				+ "attr_value=ac81125d02ea,timestamp=2016-04-29 04:26:13.002367-04";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		
		Assert.assertEquals(parsedJSON.get("priority"), 143);
		Assert.assertEquals(parsedJSON.get("ip_src_addr"), "110.137.3.2");
		Assert.assertEquals(parsedJSON.get("message_hex"), 473964876);
		Assert.assertEquals(parsedJSON.get("message_type"), "CPPM_Session_Detail");
		Assert.assertEquals(parsedJSON.get("timestamp") + "", "1461904051000");
		Assert.assertEquals(parsedJSON.get("attr_name"), "Radius:IETF:User-Name");
		Assert.assertEquals(parsedJSON.get("session_id"), "R011cf48e-04-57231aa3");
		Assert.assertEquals(parsedJSON.get("message_id"), "5983679670");
		Assert.assertEquals(parsedJSON.get("type"), "RADIUS_IN");
		Assert.assertEquals(parsedJSON.get("result_timestamp"), "2016-04-29 04:26:13.002367-04");
	}

}
