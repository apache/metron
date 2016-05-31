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

package org.apache.metron.parsers.windowsyslog;

import org.apache.metron.parsers.BasicParser;
import org.apache.metron.parsers.GrokParser;
import org.apache.metron.parsers.bluecoat.BasicBluecoatParser;
import org.apache.metron.parsers.websphere.GrokWebSphereParser;
import org.apache.metron.parsers.windowssyslog.WindowsSyslogParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WindowsSyslogParserTest {

	private final String dateFormat = "yyyy MMM dd HH:mm:ss";
	private final String timestampField = "timestamp_string";

	private static final Logger LOGGER = LoggerFactory
			.getLogger(WindowsSyslogParserTest.class);

	public WindowsSyslogParserTest() throws Exception {
		super();
	}

	@Test
	public void testParseLine() throws Exception {
		//Set up parser, parse message
		WindowsSyslogParser parser = new WindowsSyslogParser();
		//parser.withDateFormat(dateFormat).withTimestampField(timestampField);
		String testString = "<13> ABC 02/05/2016 09:54:39 AM\n" +
				"LogName=Security\n" +
				"SourceName=Microsoft Windows security auditing.\n" +
				"EventCode=4624\n" +
				"EventType=0\n" +
				"Type=Information\n" +
				"ComputerName=ABC.google.com\n" +
				"TaskCategory=Logon\n" +
				"OpCode=Info\n" +
				"RecordNumber=112720121\n" +
				"Keywords=Audit Success\n" +
				"Message=An account was successfully logged on.\n" +
				"\n" +
				"\n" +
				"\n" +
				"Subject:\n" +
				"\n" +
				"\tSecurity ID:\t\tNULL SID\n" +
				"\n" +
				"\tAccount Name:\t\t-\n" +
				"\n" +
				"\tAccount Domain:\t\t-\n" +
				"\n" +
				"\tLogon ID:\t\t0x0\n" +
				"\n" +
				"\n" +
				"\n" +
				"Logon Type:\t\t\t3\n" +
				"\n" +
				"\n" +
				"\n" +
				"New Logon:\n" +
				"\n" +
				"\tSecurity ID:\t\tCOF\\ABC\n" +
				"\n" +
				"\tAccount Name:\t\tABC\n" +
				"\n" +
				"\tAccount Domain:\t\tCOF\n" +
				"\n" +
				"\tLogon ID:\t\t0x4e149e04\n" +
				"\n" +
				"\tLogon GUID:\t\t{89C4AB77-51D6-D17B-3EAD-BC8676D1A4D2}\n" +
				"\n" +
				"\n" +
				"\n" +
				"Process Information:\n" +
				"\n" +
				"\tProcess ID:\t\t0x0\n" +
				"\n" +
				"\tProcess Name:\t\t-\n" +
				"\n" +
				"\n" +
				"\n" +
				"Network Information:\n" +
				"\n" +
				"\tWorkstation Name:\n" +
				"\n" +
				"\tSource Network Address:\t10.0.0.0\n" +
				"\n" +
				"\tSource Port:\t\t64340\n" +
				"\n" +
				"\n" +
				"\n" +
				"Detailed Authentication Information:\n" +
				"\n" +
				"\tLogon Process:\t\tKerberos\n" +
				"\n" +
				"\tAuthentication Package:\tKerberos\n" +
				"\n" +
				"\tTransited Services:\t-\n" +
				"\n" +
				"\tPackage Name (NTLM only):\t-\n" +
				"\n" +
				"\tKey Length:\t\t0\n" +
				"\n" +
				"\n" +
				"\n" +
				"This event is generated when a logon session is created. It is generated on the computer that was accessed.\n" +
				"\n" +
				"\n" +
				"\n" +
				"The subject fields indicate the account on the local system which requested the logon. This is most commonly a service such as the Server service, or a local process such as Winlogon.exe or Services.exe.\n" +
				"\n" +
				"\n" +
				"\n" +
				"The logon type field indicates the kind of logon that occurred. The most common types are 2 (interactive) and 3 (network).\n" +
				"\n" +
				"\n" +
				"\n" +
				"The New Logon fields indicate the account for whom the new logon was created, i.e. the account that was logged on.\n" +
				"\n" +
				"\n" +
				"\n" +
				"The network fields indicate where a remote logon request originated. Workstation name is not always available and may be left blank in some cases.\n" +
				"\n" +
				"\n" +
				"\n" +
				"The authentication information fields provide detailed information about this specific logon request.\n" +
				"\n" +
				"\t- Logon GUID is a unique identifier that can be used to correlate this event with a KDC event.\n" +
				"\n" +
				"\t- Transited services indicate which intermediate services have participated in this logon request.\n" +
				"\n" +
				"\t- Package name indicates which sub-protocol was used among the NTLM protocols.\n" +
				"\n" +
				"\t- Key length indicates the length of the generated session key. This will be 0 if no session key was requested.";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);

		JSONObject json = parsedJSON;



		// ensure json is not null
		assertNotNull(json);
		// ensure json is not empty
		assertTrue(!json.isEmpty());

		Iterator iter = json.entrySet().iterator();

		// ensure there are no null keys
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			assertNotNull(entry);

			String key = (String) entry.getKey();
			assertNotNull(key);
		}

		// Test the values in the json against the actual data from the file
		assertEquals(json.get("computer_name_simple"), "ABC");

		//assertEquals(dateFormat.format(json.get("device_generated_timestamp")), "02/05/2016 09:54:39 AM");
		assertEquals(json.get("log_name"), "Security");
		assertEquals(json.get("source_name"), "Microsoft Windows security auditing.");
		assertEquals(json.get("event_code"), "4624");
		assertEquals(json.get("event_type"), "0");
		assertEquals(json.get("type"), "Information");
		assertEquals(json.get("computer_name"), "ABC.google.com");
		assertEquals(json.get("task_category"), "Logon");
		assertEquals(json.get("op_code"), "Info");
		assertEquals(json.get("record_number"), "112720121");
		assertEquals(json.get("keywords"), "Audit Success");
		assertEquals(json.get("logon_type"), "3");
		assertEquals(json.get("security_id"), "ABC");
		assertEquals(json.get("subjectAccountName"), "-");
		assertEquals(json.get("newLogonAccountName"), "ABC");
		assertEquals(json.get("message"), getRealLineMessageForTesting());

	}

	/**
	 * Checks the input JSON object for any null keys. If a particular value in the JSONObject
	 * is another JSONObject, then recursively call this method again for the nested JSONObject
	 *
	 * @param jsonObj: the input JSON object for which to check null keys
	 */
	private void testKeysNotNull(JSONObject jsonObj) {
		for (Object key : jsonObj.keySet()) {
			assertNotNull(key);
			Object jsonValue = jsonObj.get(key);
			if (jsonValue.getClass().equals(JSONObject.class)) {
				testKeysNotNull((JSONObject) jsonValue);
			}
		}
	}

	/**
	 * Gets the entire message field from the example Windows Syslog. This is
	 * just a helper method to get the String.
	 * @return The entire message String for the real test data
	 */
	private String getRealLineMessageForTesting() {
		StringBuilder sb = new StringBuilder();
		appendWithNewLine(sb, "An account was successfully logged on.");
		appendWithNewLine(sb, "Subject:");
		appendWithNewLine(sb, "	Security ID:		NULL SID");
		appendWithNewLine(sb, "	Account Name:		-");
		appendWithNewLine(sb, "	Account Domain:		-");
		appendWithNewLine(sb, "	Logon ID:		0x0");
		appendWithNewLine(sb, "Logon Type:			3");
		appendWithNewLine(sb, "New Logon:");
		appendWithNewLine(sb, "	Security ID:		COF\\ABC"); // escaped back slash
		appendWithNewLine(sb, "	Account Name:		ABC");
		appendWithNewLine(sb, "	Account Domain:		COF");
		appendWithNewLine(sb, "	Logon ID:		0x4e149e04");
		appendWithNewLine(sb, "	Logon GUID:		{89C4AB77-51D6-D17B-3EAD-BC8676D1A4D2}");
		appendWithNewLine(sb, "Process Information:");
		appendWithNewLine(sb, "	Process ID:		0x0");
		appendWithNewLine(sb, "	Process Name:		-");
		appendWithNewLine(sb, "Network Information:");
		appendWithNewLine(sb, "	Workstation Name:");
		appendWithNewLine(sb, "	Source Network Address:	10.0.0.0");
		appendWithNewLine(sb, "	Source Port:		64340");
		appendWithNewLine(sb, "Detailed Authentication Information:");
		appendWithNewLine(sb, "	Logon Process:		Kerberos");
		appendWithNewLine(sb, "	Authentication Package:	Kerberos");
		appendWithNewLine(sb, "	Transited Services:	-");
		appendWithNewLine(sb, "	Package Name (NTLM only):	-");
		appendWithNewLine(sb, "	Key Length:		0");
		appendWithNewLine(sb, "This event is generated when a logon session is created. It is generated on the computer that was accessed.");
		appendWithNewLine(sb, "The subject fields indicate the account on the local system which requested the logon. This is most commonly a service such as the Server service, or a local process such as Winlogon.exe or Services.exe.");
		appendWithNewLine(sb, "The logon type field indicates the kind of logon that occurred. The most common types are 2 (interactive) and 3 (network).");
		appendWithNewLine(sb, "The New Logon fields indicate the account for whom the new logon was created, i.e. the account that was logged on.");
		appendWithNewLine(sb, "The network fields indicate where a remote logon request originated. Workstation name is not always available and may be left blank in some cases.");
		appendWithNewLine(sb, "The authentication information fields provide detailed information about this specific logon request.");
		appendWithNewLine(sb, "	- Logon GUID is a unique identifier that can be used to correlate this event with a KDC event.");
		appendWithNewLine(sb, "	- Transited services indicate which intermediate services have participated in this logon request.");
		appendWithNewLine(sb, "	- Package name indicates which sub-protocol was used among the NTLM protocols.");
		appendWithNewLine(sb, "	- Key length indicates the length of the generated session key. This will be 0 if no session key was requested.");

		return sb.toString();
	}

	private void appendWithNewLine(StringBuilder sb, String toAppend) {
		sb.append(toAppend + '\n');
	}
}
