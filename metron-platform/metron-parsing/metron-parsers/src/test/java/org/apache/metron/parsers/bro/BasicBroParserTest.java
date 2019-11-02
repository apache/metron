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
package org.apache.metron.parsers.bro;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Level;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.test.utils.UnitTestHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class BasicBroParserTest {
	private BasicBroParser broParser = new BasicBroParser();
	private JSONParser jsonParser = new JSONParser();

	@BeforeAll
	public static void setup() {
		UnitTestHelper.setLog4jLevel(BasicBroParser.class, Level.FATAL);
	}

	@AfterAll
	public static void teardown() {
		UnitTestHelper.setLog4jLevel(BasicBroParser.class, Level.ERROR);
	}

	/**
	 * This test is included as a gut-check about our formatting expectations using the Java JDK
	 * https://docs.oracle.com/javase/tutorial/i18n/format/decimalFormat.html
	 */
	@Test
	public void testDecimalFormatAssumptions() {
		Pair[] pairs = {
						Pair.of(12345678d, "12345678.0"),
						Pair.of(12345678.0d, "12345678.0"),
						Pair.of(12345678.1d, "12345678.1"),
						Pair.of(12345678.11d, "12345678.11"),
						Pair.of(12345678.111d, "12345678.111"),
						Pair.of(12345678.1111d, "12345678.1111"),
						Pair.of(12345678.11111d, "12345678.11111"),
						Pair.of(12345678.111111d, "12345678.111111")
		};
		for (Pair pair : pairs) {
			assertEquals(pair.getRight(), BasicBroParser.DECIMAL_FORMAT.get().format(pair.getLeft()), "Format did not match");
		}
	}

	/**
	 * {
	 * "ts":1449511228.474,
	 * "uid":"CFgSLp4HgsGqXnNjZi",
	 * "id.orig_h":"104.130.172.191",
	 * "id.orig_p":33893,
	 * "id.resp_h":"69.20.0.164",
	 * "id.resp_p":53,
	 * "proto":"udp",
	 * "trans_id":3514,
	 * "rcode":3,
	 * "rcode_name":"NXDOMAIN",
	 * "AA":false,
	 * "TC":false,
	 * "RD":false,
	 * "RA":false,
	 * "Z":0,
	 * "rejected":false,
	 * "sensor":"cloudbro",
	 * "type":"dns"
	 * }
	 */
	@Multiline
	public static String unwrappedBroMessage;

	@Test
	public void testUnwrappedBroMessage() throws ParseException {
        JSONObject rawJson = (JSONObject)jsonParser.parse(unwrappedBroMessage);
        JSONObject broJson = broParser.parse(unwrappedBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);

	String expectedBroTimestamp = "1449511228.474";
      	assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
        String expectedTimestamp = "1449511228474";
	assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);

	assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
	assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
	assertEquals(broJson.get("ip_src_port"), rawJson.get("id.orig_p"));
        assertEquals(broJson.get("ip_dst_port"), rawJson.get("id.resp_p"));
        assertEquals(broJson.get("uid").toString(), rawJson.get("uid").toString());
        assertEquals(broJson.get("trans_id").toString(), rawJson.get("trans_id").toString());
        assertEquals(broJson.get("sensor").toString(), rawJson.get("sensor").toString());
        assertEquals(broJson.get("type").toString(), rawJson.get("type").toString());
        assertEquals(broJson.get("rcode").toString(), rawJson.get("rcode").toString());
        assertEquals(broJson.get("rcode_name").toString(), rawJson.get("rcode_name").toString());

	assertTrue(broJson.get("original_string").toString().startsWith("DNS"));
    }

        /**
	 * {
	 * "http": {
	 *	"ts":1402307733.473,
	 *	"uid":"CTo78A11g7CYbbOHvj",
	 *	"id.orig_h":"192.249.113.37",
	 *	"id.orig_p":58808,
	 *	"id.resp_h":"72.163.4.161",
	 *	"id.resp_p":80,
	 *	"trans_depth":1,
	 *	"method":"GET",
	 *	"host":"www.cisco.com",
	 *	"uri":"/",
	 *	"user_agent":"curl/7.22.0 (x86_64-pc-linux-gnu) libcurl/7.22.0 OpenSSL/1.0.1 zlib/1.2.3.4 libidn/1.23 librtmp/2.3",
	 *	"request_body_len":0,
	 *	"response_body_len":25523,
	 *	"status_code":200,
	 *	"status_msg":"OK",
	 *	"tags":[],
	 *	"resp_fuids":["FJDyMC15lxUn5ngPfd"],
	 *	"resp_mime_types":["text/html"]
	 *	}
	 * }
	 */
        @Multiline
	public static String httpBroMessage;

	@SuppressWarnings("rawtypes")
	@Test
	public void testHttpBroMessage() throws ParseException {
		Map rawMessageMap = (Map) jsonParser.parse(httpBroMessage);
		JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

		JSONObject broJson = broParser.parse(httpBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
		String expectedBroTimestamp = "1402307733.473";
		assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
		String expectedTimestamp = "1402307733473";
		assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
		assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
		assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
		assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
		assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
		assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

		assertEquals(broJson.get("uid").toString(), rawJson.get("uid").toString());
		assertEquals(broJson.get("method").toString(), rawJson.get("method").toString());
		assertEquals(broJson.get("host").toString(), rawJson.get("host").toString());
		assertEquals(broJson.get("resp_mime_types").toString(), rawJson.get("resp_mime_types").toString());

		assertTrue(broJson.get("original_string").toString().startsWith("HTTP"));
	}

	/**
	 * Ensure we are able to parse values with fewer than 13 digits, including the millis after the decimal.
	 * This test verifies we handle parsing timestamps in situations where, for instance, 123.000000 is represented
	 * more compactly as 123.0
	 */
	@SuppressWarnings("rawtypes")
	@Test
	public void testHttpBroMessageWithZeroDecimalTruncation() throws ParseException {
		{
			String rawMessage = "{\"http\": {\"ts\":1467657279,\"uid\":\"CMYLzP3PKiwZAgBa51\",\"id.orig_h\":\"192.168.138.158\",\"id.orig_p\":49206,\"id.resp_h\":\"95.163.121.204\"," +
							"\"id.resp_p\":80,\"trans_depth\":2,\"method\":\"GET\",\"host\":\"7oqnsnzwwnm6zb7y.gigapaysun.com\",\"uri\":\"/img/flags/it.png\",\"referrer\":\"http://7oqnsnzwwnm6zb7y.gigapaysun.com/11iQmfg\",\"user_agent\":\"Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0)\",\"request_body_len\":0,\"response_body_len\":552,\"status_code\":200,\"status_msg\":\"OK\",\"tags\":[],\"resp_fuids\":[\"F3m7vB2RjUe4n01aqj\"],\"resp_mime_types\":[\"image/png\"]}}";

			Map rawMessageMap = (Map) jsonParser.parse(rawMessage);
			JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

			JSONObject broJson = broParser.parse(rawMessage.getBytes(StandardCharsets.UTF_8)).get(0);
			String expectedTimestamp = "1467657279000";
			assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
			String expectedBroTimestamp = "1467657279.0";
			assertEquals(broJson.get("bro_timestamp").toString(), expectedBroTimestamp);
		}
		{
			String rawMessage = "{\"http\": {\"ts\":1467657279.0,\"uid\":\"CMYLzP3PKiwZAgBa51\",\"id.orig_h\":\"192.168.138.158\",\"id.orig_p\":49206,\"id.resp_h\":\"95.163.121.204\"," +
							"\"id.resp_p\":80,\"trans_depth\":2,\"method\":\"GET\",\"host\":\"7oqnsnzwwnm6zb7y.gigapaysun.com\",\"uri\":\"/img/flags/it.png\",\"referrer\":\"http://7oqnsnzwwnm6zb7y.gigapaysun.com/11iQmfg\",\"user_agent\":\"Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0)\",\"request_body_len\":0,\"response_body_len\":552,\"status_code\":200,\"status_msg\":\"OK\",\"tags\":[],\"resp_fuids\":[\"F3m7vB2RjUe4n01aqj\"],\"resp_mime_types\":[\"image/png\"]}}";

			Map rawMessageMap = (Map) jsonParser.parse(rawMessage);
			JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

			JSONObject broJson = broParser.parse(rawMessage.getBytes(StandardCharsets.UTF_8)).get(0);
			String expectedTimestamp = "1467657279000";
			assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
			String expectedBroTimestamp = "1467657279.0";
			assertEquals(broJson.get("bro_timestamp").toString(), expectedBroTimestamp);
		}
		{
			String rawMessage = "{\"http\": {\"ts\":1467657279.1,\"uid\":\"CMYLzP3PKiwZAgBa51\",\"id.orig_h\":\"192.168.138.158\",\"id.orig_p\":49206,\"id.resp_h\":\"95.163.121.204\"," +
							"\"id.resp_p\":80,\"trans_depth\":2,\"method\":\"GET\",\"host\":\"7oqnsnzwwnm6zb7y.gigapaysun.com\",\"uri\":\"/img/flags/it.png\",\"referrer\":\"http://7oqnsnzwwnm6zb7y.gigapaysun.com/11iQmfg\",\"user_agent\":\"Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0)\",\"request_body_len\":0,\"response_body_len\":552,\"status_code\":200,\"status_msg\":\"OK\",\"tags\":[],\"resp_fuids\":[\"F3m7vB2RjUe4n01aqj\"],\"resp_mime_types\":[\"image/png\"]}}";

			Map rawMessageMap = (Map) jsonParser.parse(rawMessage);
			JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

			JSONObject broJson = broParser.parse(rawMessage.getBytes(StandardCharsets.UTF_8)).get(0);
			String expectedTimestamp = "1467657279100";
			assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
			String expectedBroTimestamp = "1467657279.1";
			assertEquals(broJson.get("bro_timestamp").toString(), expectedBroTimestamp);
		}
		{
			String rawMessage = "{\"http\": {\"ts\":1467657279.11,\"uid\":\"CMYLzP3PKiwZAgBa51\",\"id.orig_h\":\"192.168.138.158\",\"id.orig_p\":49206,\"id.resp_h\":\"95.163.121.204\"," +
							"\"id.resp_p\":80,\"trans_depth\":2,\"method\":\"GET\",\"host\":\"7oqnsnzwwnm6zb7y.gigapaysun.com\",\"uri\":\"/img/flags/it.png\",\"referrer\":\"http://7oqnsnzwwnm6zb7y.gigapaysun.com/11iQmfg\",\"user_agent\":\"Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0)\",\"request_body_len\":0,\"response_body_len\":552,\"status_code\":200,\"status_msg\":\"OK\",\"tags\":[],\"resp_fuids\":[\"F3m7vB2RjUe4n01aqj\"],\"resp_mime_types\":[\"image/png\"]}}";

			Map rawMessageMap = (Map) jsonParser.parse(rawMessage);
			JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

			JSONObject broJson = broParser.parse(rawMessage.getBytes(StandardCharsets.UTF_8)).get(0);
			String expectedTimestamp = "1467657279110";
			assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
			String expectedBroTimestamp = "1467657279.11";
			assertEquals(broJson.get("bro_timestamp").toString(), expectedBroTimestamp);
		}
	}

	/**
	 * {
         * "http": {
	 *	"ts":1457149494.166991,
         *	"uid":"CTo78A11g7CYbbOHvj",
         *	"id.orig_h":"192.249.113.37",
         *	"id.orig_p":58808,
         *	"id.resp_h":"72.163.4.161",
         *	"id.resp_p":80,
         *	"trans_depth":1,
         *	"method":"GET",
         *	"host":"www.cisco.com",
         *	"uri":"/",
         *	"user_agent":"curl/7.22.0 (x86_64-pc-linux-gnu) libcurl/7.22.0 OpenSSL/1.0.1 zlib/1.2.3.4 libidn/1.23 librtmp/2.3",
         *	"request_body_len":0,
         *	"response_body_len":25523,
         *	"status_code":200,
         *	"status_msg":"OK",
         *	"tags":[],
         *	"resp_fuids":["FJDyMC15lxUn5ngPfd"],
         *	"resp_mime_types":["text/html"]
	 *	}
         * }
	 */
	@Multiline
	public static String httpBroDecimalMessage;

	@SuppressWarnings("rawtypes")
	@Test
	public void testHttpBroDecimalMessage() throws ParseException {
		Map rawMessageMap = (Map) jsonParser.parse(httpBroDecimalMessage);
		JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

		JSONObject broJson = broParser.parse(httpBroDecimalMessage.getBytes(StandardCharsets.UTF_8)).get(0);
		String expectedBroTimestamp = "1457149494.166991";
		assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
		String expectedTimestamp = "1457149494166";
		assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
		assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
		assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
		assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
		assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
		assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

		assertEquals(broJson.get("uid").toString(), rawJson.get("uid").toString());
		assertEquals(broJson.get("method").toString(), rawJson.get("method").toString());
		assertEquals(broJson.get("host").toString(), rawJson.get("host").toString());
		assertEquals(broJson.get("resp_mime_types").toString(), rawJson.get("resp_mime_types").toString());

		assertTrue(broJson.get("original_string").toString().startsWith("HTTP"));
	}


	/**
	 * {
         * "dns": {
         *	"ts":1402308259.609,
         *	"uid":"CuJT272SKaJSuqO0Ia",
         *	"id.orig_h":"10.122.196.204",
         *	"id.orig_p":33976,
         *	"id.resp_h":"144.254.71.184",
         *	"id.resp_p":53,
         *	"proto":"udp",
         *	"trans_id":62418,
         *	"query":"www.cisco.com",
         *	"qclass":1,
         *	"qclass_name":"C_INTERNET",
         *	"qtype":28,
         *	"qtype_name":"AAAA",
         *	"rcode":0,
         *	"rcode_name":"NOERROR",
         *	"AA":true,
         *	"TC":false,
         *	"RD":true,
         *	"RA":true,
         *	"Z":0,
         *	"answers":["www.cisco.com.akadns.net","origin-www.cisco.com","2001:420:1201:2::a"],
         *	"TTLs":[3600.0,289.0,14.0],
         *	"rejected":false
	 *	}
         * }
	 */
	@Multiline
	public static String dnsBroMessage;

	@SuppressWarnings("rawtypes")
	@Test
	public void testDnsBroMessage() throws ParseException {
		Map rawMessageMap = (Map) jsonParser.parse(dnsBroMessage);
		JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

		JSONObject broJson = broParser.parse(dnsBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
		String expectedBroTimestamp = "1402308259.609";
		assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
		String expectedTimestamp = "1402308259609";
		assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
		assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
		assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
		assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
		assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
		assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

		assertEquals(broJson.get("qtype").toString(), rawJson.get("qtype").toString());
		assertEquals(broJson.get("trans_id").toString(), rawJson.get("trans_id").toString());

		assertTrue(broJson.get("original_string").toString().startsWith("DNS"));
	}

	/**
	 * {
         * "files": {
         *	"analyzers": ["X509","MD5","SHA1"],
         *	"conn_uids":["C4tygJ3qxJBEJEBCeh"],
         *	"depth": 0,
         *	"duration": 0.0,
         *	"fuid":"FZEBC33VySG0nHSoO9",
         *	"is_orig": false,
         *	"local_orig": false,
         *	"md5": "eba37166385e3ef42464ed9752e99f1b",
         *	"missing_bytes": 0,
         *	"overflow_bytes": 0,
         *	"rx_hosts": ["10.220.15.205"],
         *	"seen_bytes": 1136,
         *	"sha1": "73e42686657aece354fbf685712361658f2f4357",
         *	"source": "SSL",
         *	"timedout": false,
         *	"ts": 1425845251.334,
         *	"tx_hosts": ["68.171.237.7"]
	 *	}
         * }
	 */
	@Multiline
	public static String filesBroMessage;

	@SuppressWarnings("rawtypes")
	@Test
	public void testFilesBroMessage() throws ParseException {
		Map rawMessageMap = (Map) jsonParser.parse(filesBroMessage);
		JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

		JSONObject broJson = broParser.parse(filesBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
		String expectedBroTimestamp = "1425845251.334";
		assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
		String expectedTimestamp = "1425845251334";
		assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
		assertEquals(broJson.get("ip_src_addr").toString(), ((JSONArray)rawJson.get("tx_hosts")).get(0).toString());
		assertEquals(broJson.get("ip_dst_addr").toString(), ((JSONArray)rawJson.get("rx_hosts")).get(0).toString());
		assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

		assertEquals(broJson.get("fuid").toString(), rawJson.get("fuid").toString());
		assertEquals(broJson.get("md5").toString(), rawJson.get("md5").toString());
		assertEquals(broJson.get("analyzers").toString(), rawJson.get("analyzers").toString());

		assertTrue(broJson.get("original_string").toString().startsWith("FILES"));
	}

	/**
	 * {
         * "conn": {
         *	"ts":1166289883.163553,
         *	"uid":"CTKCLy1z4C9U8OqU0c",
         *	"id.orig_h":"192.168.0.114",
         *	"id.orig_p":1140,
         *	"id.resp_h":"192.168.0.193",
         *	"id.resp_p":7254,
         *	"proto":"tcp",
         *	"service":"ftp-data",
         *	"duration":0.006635,
         *	"orig_bytes":0,
         *	"resp_bytes":5808,
         *	"conn_state":"S1",
         *	"missed_bytes":0,
         *	"history":"ShAd",
         *	"orig_pkts":3,
         *	"orig_ip_bytes":128,
         *	"resp_pkts":5,
         *	"resp_ip_bytes":6016,
         *	"tunnel_parents":[]
	 *	}
         * }
	 */
	@Multiline
	public static String connBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testConnBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(connBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(connBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1166289883.163553";
                assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1166289883163";
                assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                assertEquals(broJson.get("proto").toString(), rawJson.get("proto").toString());
                assertEquals(broJson.get("service").toString(), rawJson.get("service").toString());
                assertEquals(broJson.get("duration").toString(), rawJson.get("duration").toString());
                assertEquals(broJson.get("orig_bytes").toString(), rawJson.get("orig_bytes").toString());
                assertEquals(broJson.get("resp_bytes").toString(), rawJson.get("resp_bytes").toString());
                assertEquals(broJson.get("conn_state").toString(), rawJson.get("conn_state").toString());
                assertEquals(broJson.get("missed_bytes").toString(), rawJson.get("missed_bytes").toString());
                assertEquals(broJson.get("history").toString(), rawJson.get("history").toString());
                assertEquals(broJson.get("orig_pkts").toString(), rawJson.get("orig_pkts").toString());
                assertEquals(broJson.get("orig_ip_bytes").toString(), rawJson.get("orig_ip_bytes").toString());
                assertEquals(broJson.get("resp_pkts").toString(), rawJson.get("resp_pkts").toString());
                assertEquals(broJson.get("resp_ip_bytes").toString(), rawJson.get("resp_ip_bytes").toString());
                assertEquals(broJson.get("tunnel_parents").toString(), rawJson.get("tunnel_parents").toString());

		assertTrue(broJson.get("original_string").toString().startsWith("CONN"));
        }

        /**
	 * {
         * "dpd": {
         *	"ts":1216704078.712276,
         *	"uid":"CwlB8d119WPanz63J",
         *	"id.orig_h":"192.168.15.4",
         *	"id.orig_p":34508,
         *	"id.resp_h":"66.33.212.43",
         *	"id.resp_p":80,
         *	"proto":"tcp",
         *	"analyzer":"HTTP",
         *	"failure_reason":"not a http reply line"
	 *	}
         * }
	 */
        @Multiline
	public static String dpdBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testDpdBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(dpdBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(dpdBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1216704078.712276";
                assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1216704078712";
                assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                assertEquals(broJson.get("proto").toString(), rawJson.get("proto").toString());
                assertEquals(broJson.get("analyzer").toString(), rawJson.get("analyzer").toString());
                assertEquals(broJson.get("failure_reason").toString(), rawJson.get("failure_reason").toString());

		assertTrue(broJson.get("original_string").toString().startsWith("DPD"));
        }

        /**
	 * {
         * "ftp": {
         *	"ts":1166289883.164645,
         *	"uid":"CuVhX03cii8zrjrtva",
         *	"id.orig_h":"192.168.0.114",
         *	"id.orig_p":1137,
         *	"id.resp_h":"192.168.0.193",
         *	"id.resp_p":21,
         *	"user":"csanders",
         *	"password":"<hidden>",
         *	"command":"RETR",
         *	"arg":"ftp://192.168.0.193/Music.mp3",
         *	"mime_type":"<unknown>",
         *	"file_size":192,
         *	"reply_code":150,
         *	"reply_msg":"Data connection accepted from 192.168.0.114:1140; transfer starting for Music.mp3 (4980924 bytes).",
         *	"fuid":"FlS6Jg1aNdsBxNn9Bf"
	 *	}
         * }
	 */
        @Multiline
    public static String ftpBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testFtpBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(ftpBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(ftpBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1166289883.164645";
                assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1166289883164";
                assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                assertEquals(broJson.get("user").toString(), rawJson.get("user").toString());
                assertEquals(broJson.get("password").toString(), rawJson.get("password").toString());
                assertEquals(broJson.get("command").toString(), rawJson.get("command").toString());
                assertEquals(broJson.get("arg").toString(), rawJson.get("arg").toString());
                assertEquals(broJson.get("mime_type").toString(), rawJson.get("mime_type").toString());
                assertEquals(broJson.get("file_size").toString(), rawJson.get("file_size").toString());
                assertEquals(broJson.get("reply_code").toString(), rawJson.get("reply_code").toString());
                assertEquals(broJson.get("reply_msg").toString(), rawJson.get("reply_msg").toString());
                assertEquals(broJson.get("fuid").toString(), rawJson.get("fuid").toString());

		assertTrue(broJson.get("original_string").toString().startsWith("FTP"));
        }

	/**
	 * {
         * "known_certs": {
         *	"ts":1216706999.896836,
         *	"host":"65.54.186.47",
         *	"port_num":443,
         *	"subject":"CN=login.live.com,OU=MSN-Passport,O=Microsoft Corporation,street=One Microsoft Way,L=Redmond,ST=Washington,postalCode=98052,C=US,serialNumber=600413485,businessCategory=V1.0\u005c, Clause 5.(b),1.3.6.1.4.1.311.60.2.1.2=#130A57617368696E67746F6E,1.3.6.1.4.1.311.60.2.1.3=#13025553",
         *	"issuer_subject":"CN=VeriSign Class 3 Extended Validation SSL CA,OU=Terms of use at https://www.verisign.com/rpa (c)06,OU=VeriSign Trust Network,O=VeriSign\u005c, Inc.,C=US",
         *	"serial":"6905C4A47CFDBF9DBC98DACE38835FB8"
	 *	}
         * }
	 */
	@Multiline
    public static String knownCertsBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testKnownCertsBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(knownCertsBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(knownCertsBroMessage.getBytes(
                    StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1216706999.896836";
                assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1216706999896";
                assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                assertEquals(broJson.get("host").toString(), rawJson.get("host").toString());
                assertEquals(broJson.get("port_num").toString(), rawJson.get("port_num").toString());
                assertEquals(broJson.get("subject").toString(), rawJson.get("subject").toString());
                assertEquals(broJson.get("issuer_subject").toString(), rawJson.get("issuer_subject").toString());
                assertEquals(broJson.get("serial").toString(), rawJson.get("serial").toString());

		assertTrue(broJson.get("original_string").toString().startsWith("KNOWN_CERTS"));
        }

	/**
	 * {
         * "smtp": {
         *	"ts":1258568059.130219,
         *	"uid":"CMeLem2ouYvV8fzUp9",
         *	"id.orig_h":"192.168.1.103",
         *	"id.orig_p":1836,
         *	"id.resp_h":"192.168.1.1",
         *	"id.resp_p":25,
         *	"trans_depth":1,
         *	"helo":"m57pat",
         *	"last_reply":"220 2.0.0 Ready to start TLS",
         *	"path":["192.168.1.1","192.168.1.103"],
         *	"tls":true,
         *	"fuids":[],
         *	"is_webmail":false
	 *	}
         * }
	 */
	@Multiline
    public static String smtpBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testSmtpBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(smtpBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(smtpBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1258568059.130219";
                assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1258568059130";
                assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                assertEquals(broJson.get("trans_depth").toString(), rawJson.get("trans_depth").toString());
                assertEquals(broJson.get("helo").toString(), rawJson.get("helo").toString());
                assertEquals(broJson.get("last_reply").toString(), rawJson.get("last_reply").toString());
                assertEquals(broJson.get("path").toString(), rawJson.get("path").toString());
                assertEquals(broJson.get("tls").toString(), rawJson.get("tls").toString());
                assertEquals(broJson.get("fuids").toString(), rawJson.get("fuids").toString());
                assertEquals(broJson.get("is_webmail").toString(), rawJson.get("is_webmail").toString());

		assertTrue(broJson.get("original_string").toString().startsWith("SMTP"));
        }

	/**
	 * {
         * "ssl": {
         *	"ts":1216706999.444925,
         *	"uid":"Chy3Ge1k0IceXK4Di",
         *	"id.orig_h":"192.168.15.4",
         *	"id.orig_p":36532,
         *	"id.resp_h":"65.54.186.47",
         *	"id.resp_p":443,
         *	"version":"TLSv10",
         *	"cipher":"TLS_RSA_WITH_RC4_128_MD5",
         *	"server_name":"login.live.com",
         *	"resumed":false,
         *	"established":true,
         *	"cert_chain_fuids":["FkYBO41LPAXxh44KFk","FPrzYN1SuBqHflXZId","FZ71xF13r5XVSam1z1"],
         *	"client_cert_chain_fuids":[],
         *	"subject":"CN=login.live.com,OU=MSN-Passport,O=Microsoft Corporation,street=One Microsoft Way,L=Redmond,ST=Washington,postalCode=98052,C=US,serialNumber=600413485,businessCategory=V1.0\u005c, Clause 5.(b),1.3.6.1.4.1.311.60.2.1.2=#130A57617368696E67746F6E,1.3.6.1.4.1.311.60.2.1.3=#13025553",
         *	"issuer":"CN=VeriSign Class 3 Extended Validation SSL CA,OU=Terms of use at https://www.verisign.com/rpa (c)06,OU=VeriSign Trust Network,O=VeriSign\u005c, Inc.,C=US",
         *	"validation_status":"unable to get local issuer certificate"
	 *	}
         * }
	 */
	@Multiline
    public static String sslBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testSslBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(sslBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(sslBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1216706999.444925";
                assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1216706999444";
                assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                assertEquals(broJson.get("version").toString(), rawJson.get("version").toString());
                assertEquals(broJson.get("cipher").toString(), rawJson.get("cipher").toString());
                assertEquals(broJson.get("server_name").toString(), rawJson.get("server_name").toString());
                assertEquals(broJson.get("resumed").toString(), rawJson.get("resumed").toString());
                assertEquals(broJson.get("established").toString(), rawJson.get("established").toString());
                assertEquals(broJson.get("cert_chain_fuids").toString(), rawJson.get("cert_chain_fuids").toString());
                assertEquals(broJson.get("client_cert_chain_fuids").toString(), rawJson.get("client_cert_chain_fuids").toString());
                assertEquals(broJson.get("subject").toString(), rawJson.get("subject").toString());
                assertEquals(broJson.get("issuer").toString(), rawJson.get("issuer").toString());
                assertEquals(broJson.get("validation_status").toString(), rawJson.get("validation_status").toString());

		assertTrue(broJson.get("original_string").toString().startsWith("SSL"));
        }

        /**
         * {
         * "weird": {
         *	"ts":1216706886.239896,
         *	"uid":"CLSluk42pqbExeZQFl",
         *	"id.orig_h":"192.168.15.4",
         *	"id.orig_p":36336,
         *	"id.resp_h":"66.151.146.194",
         *	"id.resp_p":80,
         *	"name":"unescaped_special_URI_char",
         *	"notice":false,
         *	"peer":"bro"
	 *	}
         * }
         */
        @Multiline
    public static String weirdBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testWeirdBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(weirdBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(weirdBroMessage.getBytes(
                    StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1216706886.239896";
                assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1216706886239";
                assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                assertEquals(broJson.get("name").toString(), rawJson.get("name").toString());
                assertEquals(broJson.get("notice").toString(), rawJson.get("notice").toString());
                assertEquals(broJson.get("peer").toString(), rawJson.get("peer").toString());

		assertTrue(broJson.get("original_string").toString().startsWith("WEIRD"));
        }

        /**
         * {
         * "notice": {
         *	"ts":1216706377.196728,
         *	"uid":"CgpsTT28ZTiuSEsfVi",
         *	"id.orig_h":"192.168.15.4",
         *	"id.orig_p":35736,
         *	"id.resp_h":"74.125.19.104",
         *	"id.resp_p":443,
         *	"proto":"tcp",
         *	"note":"SSL::Invalid_Server_Cert",
         *	"msg":"SSL certificate validation failed with (unable to get local issuer certificate)",
         *	"sub":"CN=www.google.com,O=Google Inc,L=Mountain View,ST=California,C=US",
         *	"src":"192.168.15.4",
         *	"dst":"74.125.19.104",
         *	"p":443,
         *	"peer_descr":"bro",
         *	"actions":["Notice::ACTION_LOG"],
         *	"suppress_for":3600.0,
         *	"dropped":false
	 *	}
         * }
         */
        @Multiline
    public static String noticeBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testNoticeBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(noticeBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(noticeBroMessage.getBytes(
                    StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1216706377.196728";
                assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1216706377196";
                assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                assertEquals(broJson.get("proto").toString(), rawJson.get("proto").toString());
                assertEquals(broJson.get("note").toString(), rawJson.get("note").toString());
                assertEquals(broJson.get("msg").toString(), rawJson.get("msg").toString());
                assertEquals(broJson.get("sub").toString(), rawJson.get("sub").toString());
                assertEquals(broJson.get("src").toString(), rawJson.get("src").toString());
                assertEquals(broJson.get("dst").toString(), rawJson.get("dst").toString());
                assertEquals(broJson.get("p").toString(), rawJson.get("p").toString());
                assertEquals(broJson.get("peer_descr").toString(), rawJson.get("peer_descr").toString());
                assertEquals(broJson.get("actions").toString(), rawJson.get("actions").toString());
                assertEquals(broJson.get("suppress_for").toString(), rawJson.get("suppress_for").toString());
                assertEquals(broJson.get("dropped").toString(), rawJson.get("dropped").toString());

		assertTrue(broJson.get("original_string").toString().startsWith("NOTICE"));
        }

        /**
         * {
         * "dhcp": {
         *	"ts":1258567562.944638,
         *	"uid":"C8rZDh400N68UV9Ulj",
         *	"id.orig_h":"192.168.1.103",
         *	"id.orig_p":68,
         *	"id.resp_h":"192.168.1.1",
         *	"id.resp_p":67,
         *	"mac":"00:0b:db:63:5b:d4",
         *	"assigned_ip":"192.168.1.103",
         *	"lease_time":3564.0,
         *	"trans_id":418901490
	 *	}
         * }
         */
        @Multiline
    public static String dhcpBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testDhcpBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(dhcpBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(dhcpBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1258567562.944638";
                assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1258567562944";
                assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                assertEquals(broJson.get("mac").toString(), rawJson.get("mac").toString());
                assertEquals(broJson.get("assigned_ip").toString(), rawJson.get("assigned_ip").toString());
                assertEquals(broJson.get("lease_time").toString(), rawJson.get("lease_time").toString());
                assertEquals(broJson.get("trans_id").toString(), rawJson.get("trans_id").toString());

		assertTrue(broJson.get("original_string").toString().startsWith("DHCP"));
        }

        /**
         * {
         * "ssh": {
         *	"ts":1320435870.747967,
         *	"uid":"CSbqud1LKhRqlJiLDg",
         *	"id.orig_h":"172.16.238.1",
         *	"id.orig_p":58429,
         *	"id.resp_h":"172.16.238.136",
         *	"id.resp_p":22,
         *	"version":2,
         *	"auth_success":false,
         *	"client":"SSH-2.0-OpenSSH_5.6",
         *	"server":"SSH-2.0-OpenSSH_5.8p1 Debian-7ubuntu1",
         *	"cipher_alg":"aes128-ctr",
         *	"mac_alg":"hmac-md5",
         *	"compression_alg":"none",
         *	"kex_alg":"diffie-hellman-group-exchange-sha256",
         *	"host_key_alg":"ssh-rsa",
         *	"host_key":"87:11:46:da:89:c5:2b:d9:6b:ee:e0:44:7e:73:80:f8"
	 *	}
         * }
         */
        @Multiline
    public static String sshBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testSshBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(sshBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(sshBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1320435870.747967";
                assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1320435870747";
                assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                assertEquals(broJson.get("version").toString(), rawJson.get("version").toString());
                assertEquals(broJson.get("auth_success").toString(), rawJson.get("auth_success").toString());
                assertEquals(broJson.get("client").toString(), rawJson.get("client").toString());
                assertEquals(broJson.get("server").toString(), rawJson.get("server").toString());
                assertEquals(broJson.get("cipher_alg").toString(), rawJson.get("cipher_alg").toString());
                assertEquals(broJson.get("mac_alg").toString(), rawJson.get("mac_alg").toString());
                assertEquals(broJson.get("compression_alg").toString(), rawJson.get("compression_alg").toString());
                assertEquals(broJson.get("kex_alg").toString(), rawJson.get("kex_alg").toString());
                assertEquals(broJson.get("host_key_alg").toString(), rawJson.get("host_key_alg").toString());
                assertEquals(broJson.get("host_key").toString(), rawJson.get("host_key").toString());

		assertTrue(broJson.get("original_string").toString().startsWith("SSH"));
        }

        /**
         * {
         * "software": {
         *	"ts":1216707079.49066,
         *	"host":"38.102.35.231",
         *	"host_p":80,
         *	"software_type":"HTTP::SERVER",
         *	"name":"lighttpd",
         *	"version.major":1,
         *	"version.minor":4,
         *	"version.minor2":18,
         *	"unparsed_version":"lighttpd/1.4.18"
	 *	}
         * }
         */
        @Multiline
    public static String softwareBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testSoftwareBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(softwareBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(softwareBroMessage.getBytes(
                    StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1216707079.49066";
                assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1216707079490";
                assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                assertEquals(broJson.get("host").toString(), rawJson.get("host").toString());
                assertEquals(broJson.get("host_p").toString(), rawJson.get("host_p").toString());
                assertEquals(broJson.get("software_type").toString(), rawJson.get("software_type").toString());
                assertEquals(broJson.get("name").toString(), rawJson.get("name").toString());
                assertEquals(broJson.get("version.major").toString(), rawJson.get("version.major").toString());
                assertEquals(broJson.get("version.minor").toString(), rawJson.get("version.minor").toString());
                assertEquals(broJson.get("version.minor2").toString(), rawJson.get("version.minor2").toString());
                assertEquals(broJson.get("unparsed_version").toString(), rawJson.get("unparsed_version").toString());

		assertTrue(broJson.get("original_string").toString().startsWith("SOFTWARE"));
        }

        /**
         * {
         * "software": {
         *	"ts":1216707079.518447,
         *	"host":"72.21.202.98",
         *	"host_p":80,
         *	"software_type":"HTTP::SERVER",
         *	"name":"AmazonS3",
         *	"unparsed_version":"AmazonS3"
	 *	}
         * }
         */
        @Multiline
    public static String softwareBroMessage2;

        @SuppressWarnings("rawtypes")
	@Test
        public void testSoftwareBroMessage2() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(softwareBroMessage2);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(softwareBroMessage2.getBytes(
                    StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1216707079.518447";
                assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1216707079518";
                assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                assertEquals(broJson.get("host").toString(), rawJson.get("host").toString());
                assertEquals(broJson.get("host_p").toString(), rawJson.get("host_p").toString());
                assertEquals(broJson.get("software_type").toString(), rawJson.get("software_type").toString());
                assertEquals(broJson.get("name").toString(), rawJson.get("name").toString());
                assertEquals(broJson.get("unparsed_version").toString(), rawJson.get("unparsed_version").toString());

		assertTrue(broJson.get("original_string").toString().startsWith("SOFTWARE"));
        }

        /**
         * {
         * "radius": {
         *	"ts":1440447766.441298,
         *	"uid":"Cfvksv4SEJJiqFobPj",
         *	"id.orig_h":"127.0.0.1",
         *	"id.orig_p":53031,
         *	"id.resp_h":"127.0.0.1",
         *	"id.resp_p":1812,
         *	"username":"steve",
         *	"result":"failed"
	 *	}
         * }
         */
        @Multiline
    public static String radiusBroMessageFailed;

        @SuppressWarnings("rawtypes")
	@Test
        public void testRadiusBroMessageFailed() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(radiusBroMessageFailed);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(radiusBroMessageFailed.getBytes(
                    StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1440447766.441298";
                assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1440447766441";
                assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                assertEquals(broJson.get("username").toString(), rawJson.get("username").toString());
                assertEquals(broJson.get("result").toString(), rawJson.get("result").toString());

		assertTrue(broJson.get("original_string").toString().startsWith("RADIUS"));
        }

        /**
         * {
         * "radius": {
         *	"ts":1440447839.947956,
         *	"uid":"CHb5MF3GTmyPniTage",
         *	"id.orig_h":"127.0.0.1",
         *	"id.orig_p":65443,
         *	"id.resp_h":"127.0.0.1",
         *	"id.resp_p":1812,
         *	"username":"steve",
         *	"result":"success"
	 *	}
         * }
         */
        @Multiline
    public static String radiusBroMessageSuccess;

        @SuppressWarnings("rawtypes")
	@Test
        public void testRadiusBroMessageSuccess() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(radiusBroMessageSuccess);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(radiusBroMessageSuccess.getBytes(
                    StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1440447839.947956";
                assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1440447839947";
                assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                assertEquals(broJson.get("username").toString(), rawJson.get("username").toString());
                assertEquals(broJson.get("result").toString(), rawJson.get("result").toString());

		assertTrue(broJson.get("original_string").toString().startsWith("RADIUS"));
        }

	/**
         * {
         * "x509": {
         *	"ts":1216706999.661483,
         *	"id":"FPrzYN1SuBqHflXZId",
         *	"certificate.version":3,
         *	"certificate.serial":"5B7759C61784E15EC727C0329529286B",
         *	"certificate.subject":"CN=VeriSign Class 3 Extended Validation SSL CA,OU=Terms of use at https://www.verisign.com/rpa (c)06,OU=VeriSign Trust Network,O=VeriSign\u005c, Inc.,C=US","certificate.issuer":"CN=VeriSign Class 3 Public Primary Certification Authority - G5,OU=(c) 2006 VeriSign\u005c, Inc. - For authorized use only,OU=VeriSign Trust Network,O=VeriSign\u005c, Inc.,C=US",
         *	"certificate.not_valid_before":1162944000.0,
         *	"certificate.not_valid_after":1478563199.0,
         *	"certificate.key_alg":"rsaEncryption",
         *	"certificate.sig_alg":"sha1WithRSAEncryption",
         *	"certificate.key_type":"rsa",
         *	"certificate.key_length":2048,
         *	"certificate.exponent":"65537",
         *	"basic_constraints.ca":true,
         *	"basic_constraints.path_len":0
	 *	}
         * }
         */
        @Multiline
    public static String x509BroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testX509BroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(x509BroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(x509BroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1216706999.661483";
                assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1216706999661";
                assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                assertEquals(broJson.get("id").toString(), rawJson.get("id").toString());
                assertEquals(broJson.get("certificate.version").toString(), rawJson.get("certificate.version").toString());
                assertEquals(broJson.get("certificate.serial").toString(), rawJson.get("certificate.serial").toString());
                assertEquals(broJson.get("certificate.subject").toString(), rawJson.get("certificate.subject").toString());
                assertEquals(broJson.get("certificate.issuer").toString(), rawJson.get("certificate.issuer").toString());
                assertEquals(broJson.get("certificate.not_valid_before").toString(), rawJson.get("certificate.not_valid_before").toString());
                assertEquals(broJson.get("certificate.not_valid_after").toString(), rawJson.get("certificate.not_valid_after").toString());
                assertEquals(broJson.get("certificate.key_alg").toString(), rawJson.get("certificate.key_alg").toString());
                assertEquals(broJson.get("certificate.sig_alg").toString(), rawJson.get("certificate.sig_alg").toString());
                assertEquals(broJson.get("certificate.key_type").toString(), rawJson.get("certificate.key_type").toString());
                assertEquals(broJson.get("certificate.key_length").toString(), rawJson.get("certificate.key_length").toString());
                assertEquals(broJson.get("certificate.exponent").toString(), rawJson.get("certificate.exponent").toString());
                assertEquals(broJson.get("basic_constraints.ca").toString(), rawJson.get("basic_constraints.ca").toString());
                assertEquals(broJson.get("basic_constraints.path_len").toString(), rawJson.get("basic_constraints.path_len").toString());

		assertTrue(broJson.get("original_string").toString().startsWith("X509"));
        }

	/**
	 * {
         * "known_devices": {
         * 	"ts":1258532046.693816,
         * 	"mac":"00:0b:db:4f:6b:10",
         * 	"dhcp_host_name":"m57-charlie"
	 * 	}
         * }
	 */
	@Multiline
    public static String knownDevicesBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testKnownDevicesBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(knownDevicesBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(knownDevicesBroMessage.getBytes(
                    StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1258532046.693816";
                assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1258532046693";
                assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                assertEquals(broJson.get("mac").toString(), rawJson.get("mac").toString());
                assertEquals(broJson.get("dhcp_host_name").toString(), rawJson.get("dhcp_host_name").toString());

                assertTrue(broJson.get("original_string").toString().startsWith("KNOWN_DEVICES"));
        }

	/**
         * {
         * "rfb": {
         *      "ts":1328634261.675248,
         *      "uid":"CGhHbC1P1kuJYtR4Ul",
         *      "id.orig_h":"192.168.1.10",
         *      "id.orig_p":10254,
         *      "id.resp_h":"192.168.1.114",
         *      "id.resp_p":5900,
         *      "client_major_version":"003",
         *      "client_minor_version":"007",
         *      "server_major_version":"003",
         *      "server_minor_version":"007",
         *      "authentication_method":"VNC",
         *      "auth":true,
         *      "share_flag":false,
         *      "desktop_name":"aneagles@localhost.localdomain",
         *      "width":1280,
         *      "height":800
         *      }
         * }
         */
        @Multiline
    public static String rfbBroMessage;

        @SuppressWarnings("rawtypes")
        @Test
        public void testRfbBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(rfbBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(rfbBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1328634261.675248";
                assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1328634261675";
                assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                assertEquals(broJson.get("uid").toString(), rawJson.get("uid").toString());
                assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                assertEquals(broJson.get("client_major_version").toString(), rawJson.get("client_major_version").toString());
                assertEquals(broJson.get("client_minor_version").toString(), rawJson.get("client_minor_version").toString());
                assertEquals(broJson.get("server_major_version").toString(), rawJson.get("server_major_version").toString());
                assertEquals(broJson.get("server_minor_version").toString(), rawJson.get("server_minor_version").toString());
                assertEquals(broJson.get("authentication_method").toString(), rawJson.get("authentication_method").toString());
                assertEquals(broJson.get("auth").toString(), rawJson.get("auth").toString());
                assertEquals(broJson.get("share_flag").toString(), rawJson.get("share_flag").toString());
                assertEquals(broJson.get("desktop_name").toString(), rawJson.get("desktop_name").toString());
                assertEquals(broJson.get("width").toString(), rawJson.get("width").toString());
                assertEquals(broJson.get("height").toString(), rawJson.get("height").toString());
        }

       /**
         * {
         * "stats": {
         *      "ts":1440447766.440305
         *      "peer":"bro",
         *      "mem":55,
         *      "pkts_proc":1,
         *      "bytes_recv":119,
         *      "events_proc":392,
         *      "events_queued":15,
         *      "active_tcp_conns":0,
         *      "active_udp_conns":1,
         *      "active_icmp_conns":0,
         *      "tcp_conns":0,
         *      "udp_conns":1,
         *      "icmp_conns":0,
         *      "timers":34,
         *      "active_timers":31,
         *      "files":0,
         *      "active_files":0,
         *      "dns_requests":0,
         *      "active_dns_requests":0,
         *      "reassem_tcp_size":0,
         *      "reassem_file_size":0,
         *      "reassem_frag_size":0,
         *      "reassem_unknown_size":0
         *      }
         * }
         */
        @Multiline
    public static String statsBroMessage;

        @SuppressWarnings("rawtypes")
        @Test
        public void testStatsBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(statsBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(statsBroMessage.getBytes(
                    StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1440447766.440305";
                assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1440447766440";
                assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                assertEquals(broJson.get("peer").toString(), rawJson.get("peer").toString());
                assertEquals(broJson.get("mem").toString(), rawJson.get("mem").toString());
                assertEquals(broJson.get("pkts_proc").toString(), rawJson.get("pkts_proc").toString());
                assertEquals(broJson.get("bytes_recv").toString(), rawJson.get("bytes_recv").toString());
                assertEquals(broJson.get("events_proc").toString(), rawJson.get("events_proc").toString());
                assertEquals(broJson.get("events_queued").toString(), rawJson.get("events_queued").toString());
                assertEquals(broJson.get("active_tcp_conns").toString(), rawJson.get("active_tcp_conns").toString());
                assertEquals(broJson.get("active_udp_conns").toString(), rawJson.get("active_udp_conns").toString());
                assertEquals(broJson.get("active_icmp_conns").toString(), rawJson.get("active_icmp_conns").toString());
                assertEquals(broJson.get("tcp_conns").toString(), rawJson.get("tcp_conns").toString());
                assertEquals(broJson.get("udp_conns").toString(), rawJson.get("udp_conns").toString());
                assertEquals(broJson.get("icmp_conns").toString(), rawJson.get("icmp_conns").toString());
                assertEquals(broJson.get("timers").toString(), rawJson.get("timers").toString());
                assertEquals(broJson.get("active_timers").toString(), rawJson.get("active_timers").toString());
                assertEquals(broJson.get("files").toString(), rawJson.get("files").toString());
                assertEquals(broJson.get("active_files").toString(), rawJson.get("active_files").toString());
                assertEquals(broJson.get("dns_requests").toString(), rawJson.get("dns_requests").toString());
                assertEquals(broJson.get("active_dns_requests").toString(), rawJson.get("active_dns_requests").toString());
                assertEquals(broJson.get("reassem_tcp_size").toString(), rawJson.get("reassem_tcp_size").toString());
                assertEquals(broJson.get("reassem_file_size").toString(), rawJson.get("reassem_file_size").toString());
                assertEquals(broJson.get("reassem_frag_size").toString(), rawJson.get("reassem_frag_size").toString());
                assertEquals(broJson.get("reassem_unknown_size").toString(), rawJson.get("reassem_unknown_size").toString());
        }

       /**
         * {
         * "capture_loss": {
         *      "ts":1320435958.419451,
         *      "ts_delta":493.659207,
         *      "peer":"bro",
         *      "gaps":2,
         *      "acks":4854,
         *      "percent_lost":0.041203
         *      }
         * }
         */
        @Multiline
    public static String captureLossBroMessage;

        @SuppressWarnings("rawtypes")
        @Test
        public void testCaptureLossBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(captureLossBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(captureLossBroMessage.getBytes(
                    StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1320435958.419451";
                assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1320435958419";
                assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                assertEquals(broJson.get("ts_delta").toString(), rawJson.get("ts_delta").toString());
                assertEquals(broJson.get("peer").toString(), rawJson.get("peer").toString());
                assertEquals(broJson.get("gaps").toString(), rawJson.get("gaps").toString());
                assertEquals(broJson.get("acks").toString(), rawJson.get("acks").toString());
                assertEquals(broJson.get("percent_lost").toString(), rawJson.get("percent_lost").toString());
        }

       /**
         * {
         * "sip": {
         *      "ts":1216698441.346819,
         *      "uid":"Cf3LPS10DMyCqJMDv9",
         *      "id.orig_h":"192.168.1.64",
         *      "id.orig_p":1032,
         *      "id.resp_h":"216.115.20.143",
         *      "id.resp_p":10000,
         *      "trans_depth":0,
         *      "method":"REGISTER",
         *      "uri":"sip:t.voncp.com:10000",
         *      "request_from":"\\u002216178766111\\u0022 <sip:16178766111@t.voncp.com:10000>",
         *      "request_to":"\\u002216178766111\\u0022 <sip:16178766111@t.voncp.com:10000>",
         *      "response_from":"\\u002216178766111\\u0022 <sip:16178766111@t.voncp.com:10000>",
         *      "response_to":"\\u002216178766111\\u0022 <sip:16178766111@t.voncp.com:10000>",
         *      "call_id":"7757a70e218b95730dd2daeaac7d20b1@192.168.1.64",
         *      "seq":"1761527952 REGISTER",
         *      "request_path":["SIP/2.0/UDP 192.168.1.64:10000"],
         *      "response_path":["SIP/2.0/UDP 192.168.1.64:10000"],
         *      "user_agent":"VDV21 001DD92E4F61 2.8.1_1.4.7 LwooEk3GCD/bcm001DD92E4F61.xml",
         *      "status_code":200,
         *      "status_msg":"OK",
         *      "request_body_len":0,
         *      "response_body_len":0
         *      }
         * }
         */
        @Multiline
    public static String sipBroMessage;

        @SuppressWarnings("rawtypes")
        @Test
        public void testSipBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(sipBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(sipBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1216698441.346819";
                assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1216698441346";
                assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                assertEquals(broJson.get("uid").toString(), rawJson.get("uid").toString());
                assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                assertEquals(broJson.get("trans_depth").toString(), rawJson.get("trans_depth").toString());
                assertEquals(broJson.get("method").toString(), rawJson.get("method").toString());
                assertEquals(broJson.get("uri").toString(), rawJson.get("uri").toString());
                assertEquals(broJson.get("request_from").toString(), rawJson.get("request_from").toString());
                assertEquals(broJson.get("request_to").toString(), rawJson.get("request_to").toString());
                assertEquals(broJson.get("response_from").toString(), rawJson.get("response_from").toString());
                assertEquals(broJson.get("response_to").toString(), rawJson.get("response_to").toString());
                assertEquals(broJson.get("call_id").toString(), rawJson.get("call_id").toString());
                assertEquals(broJson.get("seq").toString(), rawJson.get("seq").toString());
                assertEquals(broJson.get("request_path").toString(), rawJson.get("request_path").toString());
                assertEquals(broJson.get("response_path").toString(), rawJson.get("response_path").toString());
                assertEquals(broJson.get("user_agent").toString(), rawJson.get("user_agent").toString());
                assertEquals(broJson.get("status_code").toString(), rawJson.get("status_code").toString());
                assertEquals(broJson.get("status_msg").toString(), rawJson.get("status_msg").toString());
                assertEquals(broJson.get("request_body_len").toString(), rawJson.get("request_body_len").toString());
                assertEquals(broJson.get("response_body_len").toString(), rawJson.get("response_body_len").toString());
        }

       /**
	 * {
	 * "ht*tp": {
	 *	"ts":1402307733.473,
	 *	"uid":"CTo78A11g7CYbbOHvj",
	 *	"id.orig_h":"192.249.113.37",
	 *	"id.orig_p":58808,
	 *	"id.resp_h":"72.163.4.161",
	 *	"id.resp_p":80,
	 *	"trans_depth":1,
	 *	"method":"GET",
	 *	"host":"www.cisco.com",
	 *	"uri":"/",
	 *	"user_agent":"curl/7.22.0 (x86_64-pc-linux-gnu) libcurl/7.22.0 OpenSSL/1.0.1 zlib/1.2.3.4 libidn/1.23 librtmp/2.3",
	 *	"request_body_len":0,
	 *	"response_body_len":25523,
	 *	"status_code":200,
	 *	"status_msg":"OK",
	 *	"tags":[],
	 *	"resp_fuids":["FJDyMC15lxUn5ngPfd"],
	 *	"resp_mime_types":["text/html"]
	 *	}
	 * }
	 */
	@Multiline
    public static String protocolKeyCleanedUp;

	@SuppressWarnings("rawtypes")
	@Test
	public void testProtocolKeyCleanedUp() throws ParseException {
		Map rawMessageMap = (Map) jsonParser.parse(protocolKeyCleanedUp);
		JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

		JSONObject broJson = broParser.parse(protocolKeyCleanedUp.getBytes(StandardCharsets.UTF_8)).get(0);
		String expectedBroTimestamp = "1402307733.473";
		assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
		String expectedTimestamp = "1402307733473";
		assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
		assertTrue(broJson.get("original_string").toString().startsWith("HTTP"));
	}

	@Test
	public void testBadMessage() {
		assertThrows(IllegalStateException.class, () -> broParser.parse("{ \"foo\" : \"bar\"}".getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testBadMessageNonJson() {
		assertThrows(IllegalStateException.class, () -> broParser.parse("foo bar".getBytes(StandardCharsets.UTF_8)));
	}

  @Test
  public void getsReadCharsetFromConfig() {
	  Map<String, Object> config = new HashMap<>();
    config.put(MessageParser.READ_CHARSET, StandardCharsets.UTF_16.toString());
    broParser.configure(config);
    assertThat(broParser.getReadCharset(), equalTo(StandardCharsets.UTF_16));
  }

  @Test
  public void getsReadCharsetFromDefault() {
    Map<String, Object> config = new HashMap<>();
    broParser.configure(config);
    assertThat(broParser.getReadCharset(), equalTo(StandardCharsets.UTF_8));
  }
}
