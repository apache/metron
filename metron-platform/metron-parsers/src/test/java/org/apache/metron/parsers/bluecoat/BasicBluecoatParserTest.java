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
package org.apache.metron.parsers.bluecoat;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.json.simple.JSONObject;
import org.junit.Test;

public class BasicBluecoatParserTest {

	private BasicBluecoatParser bbp = new BasicBluecoatParser();

	public BasicBluecoatParserTest() throws Exception {
		super();

	}

	@Test
	public void test250017() {
		String testString = "<29>Apr 15 16:46:33 ProxySG: 250017 Authentication failed from 10.118.29.228: user 'UZL193' (realm AD_ldap)(0) NORMAL_EVENT authutility.cpp 113";

		List<JSONObject> result = bbp.parse(testString.getBytes());

		JSONObject jo = result.get(0);

		assertEquals(jo.get("event_type"), "authentication failure");
		assertEquals(jo.get("event_code"), "250017");
		assertEquals(jo.get("realm"), "AD_ldap");
		assertEquals(jo.get("priority"), "29");
		assertEquals(jo.get("designated_host"), "10.118.29.228");

		System.out.println(result);
	}

	@Test
	public void test250018() {
		String testString = "<29>Apr 14 20:31:07 ProxySG: 250018 LDAP: invalid credentials: reason: '80090308: LdapErr: DSID-0C0903AA, comment: AcceptSecurityContext error, data 52e, v1772' dn: 'CN=FJL928,OU=Developers,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com' realm: 'AD_ldap'(2425130) NORMAL_EVENT realm_ldap.cpp 2833";

		List<JSONObject> result = bbp.parse(testString.getBytes());
		JSONObject jo = result.get(0);

		assertEquals(jo.get("event_type"), "authentication failure");
		assertEquals(jo.get("event_code"), "250018");
		assertEquals(jo.get("realm"), "AD_ldap");
		assertEquals(jo.get("priority"), "29");

		System.out.println(result);
	}

	@Test
	public void testE0000() {
		String testString = "<29>Apr 15 18:01:46 ProxySG: E0000 Access Log FTP (main): 230 Login successful.(0) NORMAL_EVENT alog_ftp_client.cpp 1743";

		List<JSONObject> result = bbp.parse(testString.getBytes());
		
		JSONObject jo = result.get(0);

		assertEquals(jo.get("event_type"), "authentication");
		assertEquals(jo.get("event_code"), "E0000");
		assertEquals(jo.get("priority"), "29");

		System.out.println(result);
	}

	@Test
	public void test250001() {
		String testString = "<29>Apr 14 20:31:07 ProxySG: 250001 LDAP: Authentication failed from 10.113.216.196: no such user in realm 'AD_ldap'(102089) NORMAL_EVENT realm_ldap.cpp 2634";

		List<JSONObject> result = bbp.parse(testString.getBytes());
		
		JSONObject jo = result.get(0);

		assertEquals(jo.get("event_type"), "authentication failure");
		assertEquals(jo.get("event_code"), "250001");
		assertEquals(jo.get("realm"), "AD_ldap");
		assertEquals(jo.get("priority"), "29");
		assertEquals(jo.get("designated_host"), "10.113.216.196");

		System.out.println(result);
	}

}
