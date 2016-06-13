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
package org.apache.metron.parsers.passthrough;

import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class BasicPassThroughParserTest {

	private BasicPassThroughParser parser = new BasicPassThroughParser();

	public BasicPassThroughParserTest() throws Exception {
		super();

	}

	@Test
	public void testPassThrough() {
		String testString = "Pass through sample input line";

		try {
			List<JSONObject> result = parser.parse(testString.getBytes());

			JSONObject jo = result.get(0);

			assertEquals(jo.get("original_string"), "Pass through sample input line");
			assertEquals(jo.get("is_parsed"), "false");
			assertNotNull(jo.get("timestamp"));

			System.out.println(result);
		} catch (Exception e){
			fail();
		}
	}

    @Test
    public void testOffice365PassThrough() {
        String testString = "BN3P103CA0002,308237415,2016-04-12 13:08:23 141.251.172.79 POST /mapi/emsmdb/ " +
                "MailboxId=a4faa1ea-e353-493a-8794-54dd0e2ad3c5@capitalone.com&CorrelationID=<empty>;&Live" +
                "IdBasicMemberName=XMA848@login.capone.com&cafeReqId=e797ca7f-6512-41ba-b411-ee36da4e19d4; " +
                "443 XMA848@login.capone.com 204.63.37.5 Microsoft+Office/16.0+(Windows+NT+6.1;+Microsoft+" +
                "Outlook+16.0.6001;+Pro) mail.capitalone.com 200 0 0 1508 1184 218";

		try {
			List<JSONObject> result = parser.parse(testString.getBytes());

			JSONObject jo = result.get(0);

			assertEquals(jo.get("original_string"), "BN3P103CA0002,308237415,2016-04-12 13:08:23 141.251.172.79 POST /mapi/emsmdb/ " +
					"MailboxId=a4faa1ea-e353-493a-8794-54dd0e2ad3c5@capitalone.com&CorrelationID=<empty>;&Live" +
					"IdBasicMemberName=XMA848@login.capone.com&cafeReqId=e797ca7f-6512-41ba-b411-ee36da4e19d4; " +
					"443 XMA848@login.capone.com 204.63.37.5 Microsoft+Office/16.0+(Windows+NT+6.1;+Microsoft+" +
					"Outlook+16.0.6001;+Pro) mail.capitalone.com 200 0 0 1508 1184 218");
			assertEquals(jo.get("is_parsed"), "false");
			assertNotNull(jo.get("timestamp"));

			System.out.println(result);
		} catch (Exception e){
			fail();
		}
    }
}
