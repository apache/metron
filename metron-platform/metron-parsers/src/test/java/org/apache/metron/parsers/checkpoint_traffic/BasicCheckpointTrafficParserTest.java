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
package org.apache.metron.parsers.checkpoint_traffic;

import junit.framework.TestCase;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;

import java.util.Map;

public class BasicCheckpointTrafficParserTest extends TestCase {

	/**
	 * The parser.
	 */
	private BasicCheckpointTrafficParser checkpointParser = null;
	private JSONParser jsonParser = null;

	/**
	 * Constructs a new <code>BasicCheckpointTrafficParserTest</code> instance.
	 *
	 * @throws Exception
	 */
	public BasicCheckpointTrafficParserTest() throws Exception {
		checkpointParser = new BasicCheckpointTrafficParser();
	}

    public void testRealLine() throws ParseException {
        String rawMessage = "<165>Jun  2 15:49:24 ctids042 fw1-loggrabber[9868]: time=2016-06-01 16:48:23|action=accept|orig=100.12.5.1|i/f_dir=inbound|i/f_name=bond12.3198|has_accounting=0|uuid=<574f4a17,00000012,3d000c0a,0001ffff>|product=VPN-1 & FireWall-1|__policy_id_tag=product=VPN-1 & FireWall-1[db_tag={00000051-00CD-004F-9539-AB555A5BC669};mgmt=Ravenwood_KDC3;date=1464667243;policy_name=3pecd-current]|inzone=Internal|outzone=External|rule=3907|rule_uid={6FED6C6A-908D-4BA2-B4E1-97A1ADA9DCCB}|service_id=TCP-8500|src=60.70.149.131|s_port=62264|dst=200.60.40.20|service=8500|proto=tcp";

        JSONObject checkpointJson = checkpointParser.parse(rawMessage.getBytes()).get(0);

		System.out.println(checkpointJson.toJSONString());


		assertEquals(checkpointJson.get("priority"), "165");
		assertEquals(checkpointJson.get("syslog_timestamp"), "Jun  2 15:49:24");
		assertEquals(checkpointJson.get("hostname"), "ctids042");
		assertEquals(checkpointJson.get("process"), "fw1-loggrabber");
		assertEquals(checkpointJson.get("pid"), "9868");
		assertEquals(checkpointJson.get("if_dir"), "inbound");
		assertEquals(checkpointJson.get("product"), "VPN-1 & FireWall-1");
		assertEquals(checkpointJson.get("ip_dst_port"), "8500");
		assertEquals(checkpointJson.get("inzone"), "Internal");
		assertEquals(checkpointJson.get("rule"), "3907");
		assertEquals(checkpointJson.get("rule_uid"), "{6FED6C6A-908D-4BA2-B4E1-97A1ADA9DCCB}");
		assertEquals(checkpointJson.get("outzone"), "External");
		assertEquals(checkpointJson.get("uuid"), "<574f4a17,00000012,3d000c0a,0001ffff>");
		assertEquals(checkpointJson.get("protocol"), "tcp");
		assertEquals(checkpointJson.get("has_accounting"), "0");
		assertEquals(checkpointJson.get("ip_dst_addr"), "200.60.40.20");
		assertEquals(checkpointJson.get("ip_src_port"), "62264");
		assertEquals(checkpointJson.get("orig"), "100.12.5.1");
		assertEquals(checkpointJson.get("if_name"), "bond12.3198");
		assertEquals(checkpointJson.get("service_id"), "TCP-8500");
		assertEquals(checkpointJson.get("action"), "accept");
		assertEquals(checkpointJson.get("__policy_id_tag"), "product=VPN-1 & FireWall-1[db_tag={00000051-00CD-004F-9539-AB555A5BC669};mgmt=Ravenwood_KDC3;date=1464667243;policy_name=3pecd-current]");
		assertEquals(checkpointJson.get("ip_src_addr"), "60.70.149.131");
		assertEquals(checkpointJson.get("timestamp"), 1464814103000L);
    }
}
