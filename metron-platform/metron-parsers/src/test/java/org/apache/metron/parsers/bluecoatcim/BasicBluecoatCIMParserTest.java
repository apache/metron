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
package org.apache.metron.parsers.bluecoatcim;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.json.simple.JSONObject;
import org.junit.Test;

public class BasicBluecoatCIMParserTest {

	private BasicBluecoatCIMParser bbcp = new BasicBluecoatCIMParser();

	@Test
	public void testBluecoatCIMLine() {
		String testString = "<13>May 26 13:13:58 abcproxy11a.abc.letters.com bcproxysg|date=2016-05-26|time=1 13:58|duration=8|src_ip=12.32.42.52|user=-|cs_auth_group=-|x_exception_id=authentication_failed|filter_result=DENIED|category=\"Web Ads/Analytics\"|http_referrer=-|status=407|action=TCP_DENIED|http_method=CONNECT|http_content_type=-|cs_uri_scheme=tcp|dest=n2.test.com|uri_port=123|uri_path=/|uri_query=-|uri_extension=-|http_user_agent=Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/12.0.3456.78 Safari/537.36|dest_ip=12.34.56.78|bytes_in=302|bytes_out=215|x_virus_id=-|x_bluecoat_application_name=\"none\"|x_bluecoat_application_operation=\"none\"|target_ip=12.34.432.10|proxy_connect_type=Explicit|proxy_name=\"abcproxy11a\"|proxy_ip=123.456.789.876|proxy_src_ip=-|dest_duration=-|x_rs_certificate_validate_status=-|x_rs_certificate_observed_errors=-|x_cs_ocsp_error=-|x_rs_ocsp_error=-|x_rs_connection_negotiated_ssl_version=-|x_rs_connection_negotiated_cipher=none|x_rs_connection_negotiated_cipher_size=-|x_rs_certificate_hostname=-|x_rs_certificate_hostname_category=-|x_cs_connection_negotiated_ssl_version=-|x_cs_connection_negotiated_cipher=none|x_cs_connection_negotiated_cipher_size=-|x_cs_certificate_subject=-|cs_realm=proxy_AD_realm|cs_auth_type=Certificate|x_auth_credential_type=-|%0D%0A ";

		List<JSONObject> result = bbcp.parse(testString.getBytes());

		JSONObject jo = result.get(0);

		assertEquals(jo.get("date"), "2016-05-26");
		assertEquals(jo.get("cs_uri_scheme"), "tcp");
		assertEquals(jo.get("dest"), "n2.test.com");
		assertEquals(jo.get("filter_result"), "DENIED");
		assertEquals(jo.get("http_method"), "CONNECT");
		assertEquals(jo.get("uri_path"), "/");
		assertEquals(jo.get("cs_realm"), "proxy_AD_realm");
		assertEquals(jo.get("action"), "TCP_DENIED");
		assertEquals(jo.get("cs_auth_type"), "Certificate");
		assertEquals(jo.get("priority"), "13");
		assertEquals(jo.get("proxy_name"), "abcproxy11a");
		assertEquals(jo.get("bytes_out"), "215");
		assertEquals(jo.get("ip_dst_addr"), "12.34.56.78");
		assertEquals(jo.get("proxy_connect_type"), "Explicit");
		assertEquals(jo.get("x_rs_connection_negotiated_cipher"), "none");
		assertEquals(jo.get("status"), "407");
		assertEquals(jo.get("bytes_in"), "302");
		assertEquals(jo.get("x_cs_connection_negotiated_cipher"), "none");
		assertEquals(jo.get("http_user_agent"), "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/12.0.3456.78 Safari/537.36");
		assertEquals(jo.get("duration"), "8");
		assertEquals(jo.get("ip_src_addr"), "12.32.42.52");
		assertEquals(jo.get("x_bluecoat_application_name"), "none");
		assertEquals(jo.get("timestamp").toString(), "1464268438000");
		assertEquals(jo.get("proxy_ip"), "123.456.789.876");
		assertEquals(jo.get("x_exception_id"), "authentication_failed");
		assertEquals(jo.get("x_bluecoat_application_operation"), "none");
		assertEquals(jo.get("target_ip"), "12.34.432.10");
		assertEquals(jo.get("uri_port"), "123");
		assertEquals(jo.get("time"), "1 13:58");
		assertEquals(jo.get("category"), "Web Ads/Analytics");
	}
}
