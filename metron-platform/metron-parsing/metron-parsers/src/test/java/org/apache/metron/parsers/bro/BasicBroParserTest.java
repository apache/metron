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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Level;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.test.utils.UnitTestHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class BasicBroParserTest {
	private BasicBroParser broParser = new BasicBroParser();
	private JSONParser jsonParser = new JSONParser();

	@BeforeClass
	public static void setup() {
		UnitTestHelper.setLog4jLevel(BasicBroParser.class, Level.FATAL);
	}

	@AfterClass
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
			Assert.assertEquals("Format did not match", pair.getRight(), BasicBroParser.DECIMAL_FORMAT.get().format(pair.getLeft()));
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
	public final static String unwrappedBroMessage;

	@Test
	public void testUnwrappedBroMessage() throws ParseException {
        JSONObject rawJson = (JSONObject)jsonParser.parse(unwrappedBroMessage);
        JSONObject broJson = broParser.parse(unwrappedBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);

	String expectedBroTimestamp = "1449511228.474";
      	Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
        String expectedTimestamp = "1449511228474";
	Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);

	Assert.assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
	Assert.assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
	Assert.assertEquals(broJson.get("ip_src_port"), rawJson.get("id.orig_p"));
        Assert.assertEquals(broJson.get("ip_dst_port"), rawJson.get("id.resp_p"));
        Assert.assertEquals(broJson.get("uid").toString(), rawJson.get("uid").toString());
        Assert.assertEquals(broJson.get("trans_id").toString(), rawJson.get("trans_id").toString());
        Assert.assertEquals(broJson.get("sensor").toString(), rawJson.get("sensor").toString());
        Assert.assertEquals(broJson.get("type").toString(), rawJson.get("type").toString());
        Assert.assertEquals(broJson.get("rcode").toString(), rawJson.get("rcode").toString());
        Assert.assertEquals(broJson.get("rcode_name").toString(), rawJson.get("rcode_name").toString());

	Assert.assertTrue(broJson.get("original_string").toString().startsWith("DNS"));
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
	public final static String httpBroMessage;

	@SuppressWarnings("rawtypes")
	@Test
	public void testHttpBroMessage() throws ParseException {
		Map rawMessageMap = (Map) jsonParser.parse(httpBroMessage);
		JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

		JSONObject broJson = broParser.parse(httpBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
		String expectedBroTimestamp = "1402307733.473";
		Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
		String expectedTimestamp = "1402307733473";
		Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
		Assert.assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
		Assert.assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
		Assert.assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
		Assert.assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
		Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

		Assert.assertEquals(broJson.get("uid").toString(), rawJson.get("uid").toString());
		Assert.assertEquals(broJson.get("method").toString(), rawJson.get("method").toString());
		Assert.assertEquals(broJson.get("host").toString(), rawJson.get("host").toString());
		Assert.assertEquals(broJson.get("resp_mime_types").toString(), rawJson.get("resp_mime_types").toString());

		Assert.assertTrue(broJson.get("original_string").toString().startsWith("HTTP"));
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
			Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
			String expectedBroTimestamp = "1467657279.0";
			Assert.assertEquals(broJson.get("bro_timestamp").toString(), expectedBroTimestamp);
		}
		{
			String rawMessage = "{\"http\": {\"ts\":1467657279.0,\"uid\":\"CMYLzP3PKiwZAgBa51\",\"id.orig_h\":\"192.168.138.158\",\"id.orig_p\":49206,\"id.resp_h\":\"95.163.121.204\"," +
							"\"id.resp_p\":80,\"trans_depth\":2,\"method\":\"GET\",\"host\":\"7oqnsnzwwnm6zb7y.gigapaysun.com\",\"uri\":\"/img/flags/it.png\",\"referrer\":\"http://7oqnsnzwwnm6zb7y.gigapaysun.com/11iQmfg\",\"user_agent\":\"Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0)\",\"request_body_len\":0,\"response_body_len\":552,\"status_code\":200,\"status_msg\":\"OK\",\"tags\":[],\"resp_fuids\":[\"F3m7vB2RjUe4n01aqj\"],\"resp_mime_types\":[\"image/png\"]}}";

			Map rawMessageMap = (Map) jsonParser.parse(rawMessage);
			JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

			JSONObject broJson = broParser.parse(rawMessage.getBytes(StandardCharsets.UTF_8)).get(0);
			String expectedTimestamp = "1467657279000";
			Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
			String expectedBroTimestamp = "1467657279.0";
			Assert.assertEquals(broJson.get("bro_timestamp").toString(), expectedBroTimestamp);
		}
		{
			String rawMessage = "{\"http\": {\"ts\":1467657279.1,\"uid\":\"CMYLzP3PKiwZAgBa51\",\"id.orig_h\":\"192.168.138.158\",\"id.orig_p\":49206,\"id.resp_h\":\"95.163.121.204\"," +
							"\"id.resp_p\":80,\"trans_depth\":2,\"method\":\"GET\",\"host\":\"7oqnsnzwwnm6zb7y.gigapaysun.com\",\"uri\":\"/img/flags/it.png\",\"referrer\":\"http://7oqnsnzwwnm6zb7y.gigapaysun.com/11iQmfg\",\"user_agent\":\"Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0)\",\"request_body_len\":0,\"response_body_len\":552,\"status_code\":200,\"status_msg\":\"OK\",\"tags\":[],\"resp_fuids\":[\"F3m7vB2RjUe4n01aqj\"],\"resp_mime_types\":[\"image/png\"]}}";

			Map rawMessageMap = (Map) jsonParser.parse(rawMessage);
			JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

			JSONObject broJson = broParser.parse(rawMessage.getBytes(StandardCharsets.UTF_8)).get(0);
			String expectedTimestamp = "1467657279100";
			Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
			String expectedBroTimestamp = "1467657279.1";
			Assert.assertEquals(broJson.get("bro_timestamp").toString(), expectedBroTimestamp);
		}
		{
			String rawMessage = "{\"http\": {\"ts\":1467657279.11,\"uid\":\"CMYLzP3PKiwZAgBa51\",\"id.orig_h\":\"192.168.138.158\",\"id.orig_p\":49206,\"id.resp_h\":\"95.163.121.204\"," +
							"\"id.resp_p\":80,\"trans_depth\":2,\"method\":\"GET\",\"host\":\"7oqnsnzwwnm6zb7y.gigapaysun.com\",\"uri\":\"/img/flags/it.png\",\"referrer\":\"http://7oqnsnzwwnm6zb7y.gigapaysun.com/11iQmfg\",\"user_agent\":\"Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0)\",\"request_body_len\":0,\"response_body_len\":552,\"status_code\":200,\"status_msg\":\"OK\",\"tags\":[],\"resp_fuids\":[\"F3m7vB2RjUe4n01aqj\"],\"resp_mime_types\":[\"image/png\"]}}";

			Map rawMessageMap = (Map) jsonParser.parse(rawMessage);
			JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

			JSONObject broJson = broParser.parse(rawMessage.getBytes(StandardCharsets.UTF_8)).get(0);
			String expectedTimestamp = "1467657279110";
			Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
			String expectedBroTimestamp = "1467657279.11";
			Assert.assertEquals(broJson.get("bro_timestamp").toString(), expectedBroTimestamp);
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
	public final static String httpBroDecimalMessage;

	@SuppressWarnings("rawtypes")
	@Test
	public void testHttpBroDecimalMessage() throws ParseException {
		Map rawMessageMap = (Map) jsonParser.parse(httpBroDecimalMessage);
		JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

		JSONObject broJson = broParser.parse(httpBroDecimalMessage.getBytes(StandardCharsets.UTF_8)).get(0);
		String expectedBroTimestamp = "1457149494.166991";
		Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
		String expectedTimestamp = "1457149494166";
		Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
		Assert.assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
		Assert.assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
		Assert.assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
		Assert.assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
		Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

		Assert.assertEquals(broJson.get("uid").toString(), rawJson.get("uid").toString());
		Assert.assertEquals(broJson.get("method").toString(), rawJson.get("method").toString());
		Assert.assertEquals(broJson.get("host").toString(), rawJson.get("host").toString());
		Assert.assertEquals(broJson.get("resp_mime_types").toString(), rawJson.get("resp_mime_types").toString());

		Assert.assertTrue(broJson.get("original_string").toString().startsWith("HTTP"));
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
	public final static String dnsBroMessage;

	@SuppressWarnings("rawtypes")
	@Test
	public void testDnsBroMessage() throws ParseException {
		Map rawMessageMap = (Map) jsonParser.parse(dnsBroMessage);
		JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

		JSONObject broJson = broParser.parse(dnsBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
		String expectedBroTimestamp = "1402308259.609";
		Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
		String expectedTimestamp = "1402308259609";
		Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
		Assert.assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
		Assert.assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
		Assert.assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
		Assert.assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
		Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

		Assert.assertEquals(broJson.get("qtype").toString(), rawJson.get("qtype").toString());
		Assert.assertEquals(broJson.get("trans_id").toString(), rawJson.get("trans_id").toString());

		Assert.assertTrue(broJson.get("original_string").toString().startsWith("DNS"));
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
	public final static String filesBroMessage;

	@SuppressWarnings("rawtypes")
	@Test
	public void testFilesBroMessage() throws ParseException {
		Map rawMessageMap = (Map) jsonParser.parse(filesBroMessage);
		JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

		JSONObject broJson = broParser.parse(filesBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
		String expectedBroTimestamp = "1425845251.334";
		Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
		String expectedTimestamp = "1425845251334";
		Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
		Assert.assertEquals(broJson.get("ip_src_addr").toString(), ((JSONArray)rawJson.get("tx_hosts")).get(0).toString());
		Assert.assertEquals(broJson.get("ip_dst_addr").toString(), ((JSONArray)rawJson.get("rx_hosts")).get(0).toString());
		Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

		Assert.assertEquals(broJson.get("fuid").toString(), rawJson.get("fuid").toString());
		Assert.assertEquals(broJson.get("md5").toString(), rawJson.get("md5").toString());
		Assert.assertEquals(broJson.get("analyzers").toString(), rawJson.get("analyzers").toString());

		Assert.assertTrue(broJson.get("original_string").toString().startsWith("FILES"));
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
	public final static String connBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testConnBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(connBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(connBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1166289883.163553";
                Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1166289883163";
                Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                Assert.assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                Assert.assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                Assert.assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                Assert.assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                Assert.assertEquals(broJson.get("proto").toString(), rawJson.get("proto").toString());
                Assert.assertEquals(broJson.get("service").toString(), rawJson.get("service").toString());
                Assert.assertEquals(broJson.get("duration").toString(), rawJson.get("duration").toString());
                Assert.assertEquals(broJson.get("orig_bytes").toString(), rawJson.get("orig_bytes").toString());
                Assert.assertEquals(broJson.get("resp_bytes").toString(), rawJson.get("resp_bytes").toString());
                Assert.assertEquals(broJson.get("conn_state").toString(), rawJson.get("conn_state").toString());
                Assert.assertEquals(broJson.get("missed_bytes").toString(), rawJson.get("missed_bytes").toString());
                Assert.assertEquals(broJson.get("history").toString(), rawJson.get("history").toString());
                Assert.assertEquals(broJson.get("orig_pkts").toString(), rawJson.get("orig_pkts").toString());
                Assert.assertEquals(broJson.get("orig_ip_bytes").toString(), rawJson.get("orig_ip_bytes").toString());
                Assert.assertEquals(broJson.get("resp_pkts").toString(), rawJson.get("resp_pkts").toString());
                Assert.assertEquals(broJson.get("resp_ip_bytes").toString(), rawJson.get("resp_ip_bytes").toString());
                Assert.assertEquals(broJson.get("tunnel_parents").toString(), rawJson.get("tunnel_parents").toString());

		Assert.assertTrue(broJson.get("original_string").toString().startsWith("CONN"));
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
	public final static String dpdBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testDpdBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(dpdBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(dpdBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1216704078.712276";
                Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1216704078712";
                Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                Assert.assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                Assert.assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                Assert.assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                Assert.assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                Assert.assertEquals(broJson.get("proto").toString(), rawJson.get("proto").toString());
                Assert.assertEquals(broJson.get("analyzer").toString(), rawJson.get("analyzer").toString());
                Assert.assertEquals(broJson.get("failure_reason").toString(), rawJson.get("failure_reason").toString());

		Assert.assertTrue(broJson.get("original_string").toString().startsWith("DPD"));
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
	public final static String ftpBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testFtpBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(ftpBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(ftpBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1166289883.164645";
                Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1166289883164";
                Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                Assert.assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                Assert.assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                Assert.assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                Assert.assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                Assert.assertEquals(broJson.get("user").toString(), rawJson.get("user").toString());
                Assert.assertEquals(broJson.get("password").toString(), rawJson.get("password").toString());
                Assert.assertEquals(broJson.get("command").toString(), rawJson.get("command").toString());
                Assert.assertEquals(broJson.get("arg").toString(), rawJson.get("arg").toString());
                Assert.assertEquals(broJson.get("mime_type").toString(), rawJson.get("mime_type").toString());
                Assert.assertEquals(broJson.get("file_size").toString(), rawJson.get("file_size").toString());
                Assert.assertEquals(broJson.get("reply_code").toString(), rawJson.get("reply_code").toString());
                Assert.assertEquals(broJson.get("reply_msg").toString(), rawJson.get("reply_msg").toString());
                Assert.assertEquals(broJson.get("fuid").toString(), rawJson.get("fuid").toString());

		Assert.assertTrue(broJson.get("original_string").toString().startsWith("FTP"));
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
	public final static String knownCertsBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testKnownCertsBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(knownCertsBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(knownCertsBroMessage.getBytes(
                    StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1216706999.896836";
                Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1216706999896";
                Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                Assert.assertEquals(broJson.get("host").toString(), rawJson.get("host").toString());
                Assert.assertEquals(broJson.get("port_num").toString(), rawJson.get("port_num").toString());
                Assert.assertEquals(broJson.get("subject").toString(), rawJson.get("subject").toString());
                Assert.assertEquals(broJson.get("issuer_subject").toString(), rawJson.get("issuer_subject").toString());
                Assert.assertEquals(broJson.get("serial").toString(), rawJson.get("serial").toString());

		Assert.assertTrue(broJson.get("original_string").toString().startsWith("KNOWN_CERTS"));
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
	public final static String smtpBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testSmtpBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(smtpBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(smtpBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1258568059.130219";
                Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1258568059130";
                Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                Assert.assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                Assert.assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                Assert.assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                Assert.assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                Assert.assertEquals(broJson.get("trans_depth").toString(), rawJson.get("trans_depth").toString());
                Assert.assertEquals(broJson.get("helo").toString(), rawJson.get("helo").toString());
                Assert.assertEquals(broJson.get("last_reply").toString(), rawJson.get("last_reply").toString());
                Assert.assertEquals(broJson.get("path").toString(), rawJson.get("path").toString());
                Assert.assertEquals(broJson.get("tls").toString(), rawJson.get("tls").toString());
                Assert.assertEquals(broJson.get("fuids").toString(), rawJson.get("fuids").toString());
                Assert.assertEquals(broJson.get("is_webmail").toString(), rawJson.get("is_webmail").toString());

		Assert.assertTrue(broJson.get("original_string").toString().startsWith("SMTP"));
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
	public final static String sslBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testSslBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(sslBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(sslBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1216706999.444925";
                Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1216706999444";
                Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                Assert.assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                Assert.assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                Assert.assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                Assert.assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                Assert.assertEquals(broJson.get("version").toString(), rawJson.get("version").toString());
                Assert.assertEquals(broJson.get("cipher").toString(), rawJson.get("cipher").toString());
                Assert.assertEquals(broJson.get("server_name").toString(), rawJson.get("server_name").toString());
                Assert.assertEquals(broJson.get("resumed").toString(), rawJson.get("resumed").toString());
                Assert.assertEquals(broJson.get("established").toString(), rawJson.get("established").toString());
                Assert.assertEquals(broJson.get("cert_chain_fuids").toString(), rawJson.get("cert_chain_fuids").toString());
                Assert.assertEquals(broJson.get("client_cert_chain_fuids").toString(), rawJson.get("client_cert_chain_fuids").toString());
                Assert.assertEquals(broJson.get("subject").toString(), rawJson.get("subject").toString());
                Assert.assertEquals(broJson.get("issuer").toString(), rawJson.get("issuer").toString());
                Assert.assertEquals(broJson.get("validation_status").toString(), rawJson.get("validation_status").toString());

		Assert.assertTrue(broJson.get("original_string").toString().startsWith("SSL"));
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
        public final static String weirdBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testWeirdBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(weirdBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(weirdBroMessage.getBytes(
                    StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1216706886.239896";
                Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1216706886239";
                Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                Assert.assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                Assert.assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                Assert.assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                Assert.assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                Assert.assertEquals(broJson.get("name").toString(), rawJson.get("name").toString());
                Assert.assertEquals(broJson.get("notice").toString(), rawJson.get("notice").toString());
                Assert.assertEquals(broJson.get("peer").toString(), rawJson.get("peer").toString());

		Assert.assertTrue(broJson.get("original_string").toString().startsWith("WEIRD"));
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
        public final static String noticeBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testNoticeBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(noticeBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(noticeBroMessage.getBytes(
                    StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1216706377.196728";
                Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1216706377196";
                Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                Assert.assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                Assert.assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                Assert.assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                Assert.assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                Assert.assertEquals(broJson.get("proto").toString(), rawJson.get("proto").toString());
                Assert.assertEquals(broJson.get("note").toString(), rawJson.get("note").toString());
                Assert.assertEquals(broJson.get("msg").toString(), rawJson.get("msg").toString());
                Assert.assertEquals(broJson.get("sub").toString(), rawJson.get("sub").toString());
                Assert.assertEquals(broJson.get("src").toString(), rawJson.get("src").toString());
                Assert.assertEquals(broJson.get("dst").toString(), rawJson.get("dst").toString());
                Assert.assertEquals(broJson.get("p").toString(), rawJson.get("p").toString());
                Assert.assertEquals(broJson.get("peer_descr").toString(), rawJson.get("peer_descr").toString());
                Assert.assertEquals(broJson.get("actions").toString(), rawJson.get("actions").toString());
                Assert.assertEquals(broJson.get("suppress_for").toString(), rawJson.get("suppress_for").toString());
                Assert.assertEquals(broJson.get("dropped").toString(), rawJson.get("dropped").toString());

		Assert.assertTrue(broJson.get("original_string").toString().startsWith("NOTICE"));
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
        public final static String dhcpBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testDhcpBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(dhcpBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(dhcpBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1258567562.944638";
                Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1258567562944";
                Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                Assert.assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                Assert.assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                Assert.assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                Assert.assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                Assert.assertEquals(broJson.get("mac").toString(), rawJson.get("mac").toString());
                Assert.assertEquals(broJson.get("assigned_ip").toString(), rawJson.get("assigned_ip").toString());
                Assert.assertEquals(broJson.get("lease_time").toString(), rawJson.get("lease_time").toString());
                Assert.assertEquals(broJson.get("trans_id").toString(), rawJson.get("trans_id").toString());

		Assert.assertTrue(broJson.get("original_string").toString().startsWith("DHCP"));
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
        public final static String sshBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testSshBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(sshBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(sshBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1320435870.747967";
                Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1320435870747";
                Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                Assert.assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                Assert.assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                Assert.assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                Assert.assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                Assert.assertEquals(broJson.get("version").toString(), rawJson.get("version").toString());
                Assert.assertEquals(broJson.get("auth_success").toString(), rawJson.get("auth_success").toString());
                Assert.assertEquals(broJson.get("client").toString(), rawJson.get("client").toString());
                Assert.assertEquals(broJson.get("server").toString(), rawJson.get("server").toString());
                Assert.assertEquals(broJson.get("cipher_alg").toString(), rawJson.get("cipher_alg").toString());
                Assert.assertEquals(broJson.get("mac_alg").toString(), rawJson.get("mac_alg").toString());
                Assert.assertEquals(broJson.get("compression_alg").toString(), rawJson.get("compression_alg").toString());
                Assert.assertEquals(broJson.get("kex_alg").toString(), rawJson.get("kex_alg").toString());
                Assert.assertEquals(broJson.get("host_key_alg").toString(), rawJson.get("host_key_alg").toString());
                Assert.assertEquals(broJson.get("host_key").toString(), rawJson.get("host_key").toString());

		Assert.assertTrue(broJson.get("original_string").toString().startsWith("SSH"));
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
        public final static String softwareBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testSoftwareBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(softwareBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(softwareBroMessage.getBytes(
                    StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1216707079.49066";
                Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1216707079490";
                Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                Assert.assertEquals(broJson.get("host").toString(), rawJson.get("host").toString());
                Assert.assertEquals(broJson.get("host_p").toString(), rawJson.get("host_p").toString());
                Assert.assertEquals(broJson.get("software_type").toString(), rawJson.get("software_type").toString());
                Assert.assertEquals(broJson.get("name").toString(), rawJson.get("name").toString());
                Assert.assertEquals(broJson.get("version.major").toString(), rawJson.get("version.major").toString());
                Assert.assertEquals(broJson.get("version.minor").toString(), rawJson.get("version.minor").toString());
                Assert.assertEquals(broJson.get("version.minor2").toString(), rawJson.get("version.minor2").toString());
                Assert.assertEquals(broJson.get("unparsed_version").toString(), rawJson.get("unparsed_version").toString());

		Assert.assertTrue(broJson.get("original_string").toString().startsWith("SOFTWARE"));
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
        public final static String softwareBroMessage2;

        @SuppressWarnings("rawtypes")
	@Test
        public void testSoftwareBroMessage2() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(softwareBroMessage2);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(softwareBroMessage2.getBytes(
                    StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1216707079.518447";
                Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1216707079518";
                Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                Assert.assertEquals(broJson.get("host").toString(), rawJson.get("host").toString());
                Assert.assertEquals(broJson.get("host_p").toString(), rawJson.get("host_p").toString());
                Assert.assertEquals(broJson.get("software_type").toString(), rawJson.get("software_type").toString());
                Assert.assertEquals(broJson.get("name").toString(), rawJson.get("name").toString());
                Assert.assertEquals(broJson.get("unparsed_version").toString(), rawJson.get("unparsed_version").toString());

		Assert.assertTrue(broJson.get("original_string").toString().startsWith("SOFTWARE"));
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
        public final static String radiusBroMessageFailed;

        @SuppressWarnings("rawtypes")
	@Test
        public void testRadiusBroMessageFailed() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(radiusBroMessageFailed);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(radiusBroMessageFailed.getBytes(
                    StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1440447766.441298";
                Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1440447766441";
                Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                Assert.assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                Assert.assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                Assert.assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                Assert.assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                Assert.assertEquals(broJson.get("username").toString(), rawJson.get("username").toString());
                Assert.assertEquals(broJson.get("result").toString(), rawJson.get("result").toString());

		Assert.assertTrue(broJson.get("original_string").toString().startsWith("RADIUS"));
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
        public final static String radiusBroMessageSuccess;

        @SuppressWarnings("rawtypes")
	@Test
        public void testRadiusBroMessageSuccess() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(radiusBroMessageSuccess);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(radiusBroMessageSuccess.getBytes(
                    StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1440447839.947956";
                Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1440447839947";
                Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                Assert.assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                Assert.assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                Assert.assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                Assert.assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                Assert.assertEquals(broJson.get("username").toString(), rawJson.get("username").toString());
                Assert.assertEquals(broJson.get("result").toString(), rawJson.get("result").toString());

		Assert.assertTrue(broJson.get("original_string").toString().startsWith("RADIUS"));
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
        public final static String x509BroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testX509BroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(x509BroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(x509BroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1216706999.661483";
                Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1216706999661";
                Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                Assert.assertEquals(broJson.get("id").toString(), rawJson.get("id").toString());
                Assert.assertEquals(broJson.get("certificate.version").toString(), rawJson.get("certificate.version").toString());
                Assert.assertEquals(broJson.get("certificate.serial").toString(), rawJson.get("certificate.serial").toString());
                Assert.assertEquals(broJson.get("certificate.subject").toString(), rawJson.get("certificate.subject").toString());
                Assert.assertEquals(broJson.get("certificate.issuer").toString(), rawJson.get("certificate.issuer").toString());
                Assert.assertEquals(broJson.get("certificate.not_valid_before").toString(), rawJson.get("certificate.not_valid_before").toString());
                Assert.assertEquals(broJson.get("certificate.not_valid_after").toString(), rawJson.get("certificate.not_valid_after").toString());
                Assert.assertEquals(broJson.get("certificate.key_alg").toString(), rawJson.get("certificate.key_alg").toString());
                Assert.assertEquals(broJson.get("certificate.sig_alg").toString(), rawJson.get("certificate.sig_alg").toString());
                Assert.assertEquals(broJson.get("certificate.key_type").toString(), rawJson.get("certificate.key_type").toString());
                Assert.assertEquals(broJson.get("certificate.key_length").toString(), rawJson.get("certificate.key_length").toString());
                Assert.assertEquals(broJson.get("certificate.exponent").toString(), rawJson.get("certificate.exponent").toString());
                Assert.assertEquals(broJson.get("basic_constraints.ca").toString(), rawJson.get("basic_constraints.ca").toString());
                Assert.assertEquals(broJson.get("basic_constraints.path_len").toString(), rawJson.get("basic_constraints.path_len").toString());

		Assert.assertTrue(broJson.get("original_string").toString().startsWith("X509"));
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
	public final static String knownDevicesBroMessage;

        @SuppressWarnings("rawtypes")
	@Test
        public void testKnownDevicesBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(knownDevicesBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(knownDevicesBroMessage.getBytes(
                    StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1258532046.693816";
                Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1258532046693";
                Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                Assert.assertEquals(broJson.get("mac").toString(), rawJson.get("mac").toString());
                Assert.assertEquals(broJson.get("dhcp_host_name").toString(), rawJson.get("dhcp_host_name").toString());

                Assert.assertTrue(broJson.get("original_string").toString().startsWith("KNOWN_DEVICES"));
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
        public final static String rfbBroMessage;

        @SuppressWarnings("rawtypes")
        @Test
        public void testRfbBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(rfbBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(rfbBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1328634261.675248";
                Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1328634261675";
                Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                Assert.assertEquals(broJson.get("uid").toString(), rawJson.get("uid").toString());
                Assert.assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                Assert.assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                Assert.assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                Assert.assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                Assert.assertEquals(broJson.get("client_major_version").toString(), rawJson.get("client_major_version").toString());
                Assert.assertEquals(broJson.get("client_minor_version").toString(), rawJson.get("client_minor_version").toString());
                Assert.assertEquals(broJson.get("server_major_version").toString(), rawJson.get("server_major_version").toString());
                Assert.assertEquals(broJson.get("server_minor_version").toString(), rawJson.get("server_minor_version").toString());
                Assert.assertEquals(broJson.get("authentication_method").toString(), rawJson.get("authentication_method").toString());
                Assert.assertEquals(broJson.get("auth").toString(), rawJson.get("auth").toString());
                Assert.assertEquals(broJson.get("share_flag").toString(), rawJson.get("share_flag").toString());
                Assert.assertEquals(broJson.get("desktop_name").toString(), rawJson.get("desktop_name").toString());
                Assert.assertEquals(broJson.get("width").toString(), rawJson.get("width").toString());
                Assert.assertEquals(broJson.get("height").toString(), rawJson.get("height").toString());
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
        public final static String statsBroMessage;

        @SuppressWarnings("rawtypes")
        @Test
        public void testStatsBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(statsBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(statsBroMessage.getBytes(
                    StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1440447766.440305";
                Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1440447766440";
                Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                Assert.assertEquals(broJson.get("peer").toString(), rawJson.get("peer").toString());
                Assert.assertEquals(broJson.get("mem").toString(), rawJson.get("mem").toString());
                Assert.assertEquals(broJson.get("pkts_proc").toString(), rawJson.get("pkts_proc").toString());
                Assert.assertEquals(broJson.get("bytes_recv").toString(), rawJson.get("bytes_recv").toString());
                Assert.assertEquals(broJson.get("events_proc").toString(), rawJson.get("events_proc").toString());
                Assert.assertEquals(broJson.get("events_queued").toString(), rawJson.get("events_queued").toString());
                Assert.assertEquals(broJson.get("active_tcp_conns").toString(), rawJson.get("active_tcp_conns").toString());
                Assert.assertEquals(broJson.get("active_udp_conns").toString(), rawJson.get("active_udp_conns").toString());
                Assert.assertEquals(broJson.get("active_icmp_conns").toString(), rawJson.get("active_icmp_conns").toString());
                Assert.assertEquals(broJson.get("tcp_conns").toString(), rawJson.get("tcp_conns").toString());
                Assert.assertEquals(broJson.get("udp_conns").toString(), rawJson.get("udp_conns").toString());
                Assert.assertEquals(broJson.get("icmp_conns").toString(), rawJson.get("icmp_conns").toString());
                Assert.assertEquals(broJson.get("timers").toString(), rawJson.get("timers").toString());
                Assert.assertEquals(broJson.get("active_timers").toString(), rawJson.get("active_timers").toString());
                Assert.assertEquals(broJson.get("files").toString(), rawJson.get("files").toString());
                Assert.assertEquals(broJson.get("active_files").toString(), rawJson.get("active_files").toString());
                Assert.assertEquals(broJson.get("dns_requests").toString(), rawJson.get("dns_requests").toString());
                Assert.assertEquals(broJson.get("active_dns_requests").toString(), rawJson.get("active_dns_requests").toString());
                Assert.assertEquals(broJson.get("reassem_tcp_size").toString(), rawJson.get("reassem_tcp_size").toString());
                Assert.assertEquals(broJson.get("reassem_file_size").toString(), rawJson.get("reassem_file_size").toString());
                Assert.assertEquals(broJson.get("reassem_frag_size").toString(), rawJson.get("reassem_frag_size").toString());
                Assert.assertEquals(broJson.get("reassem_unknown_size").toString(), rawJson.get("reassem_unknown_size").toString());
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
        public final static String captureLossBroMessage;

        @SuppressWarnings("rawtypes")
        @Test
        public void testCaptureLossBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(captureLossBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(captureLossBroMessage.getBytes(
                    StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1320435958.419451";
                Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1320435958419";
                Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                Assert.assertEquals(broJson.get("ts_delta").toString(), rawJson.get("ts_delta").toString());
                Assert.assertEquals(broJson.get("peer").toString(), rawJson.get("peer").toString());
                Assert.assertEquals(broJson.get("gaps").toString(), rawJson.get("gaps").toString());
                Assert.assertEquals(broJson.get("acks").toString(), rawJson.get("acks").toString());
                Assert.assertEquals(broJson.get("percent_lost").toString(), rawJson.get("percent_lost").toString());
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
        public final static String sipBroMessage;

        @SuppressWarnings("rawtypes")
        @Test
        public void testSipBroMessage() throws ParseException {
                Map rawMessageMap = (Map) jsonParser.parse(sipBroMessage);
                JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

                JSONObject broJson = broParser.parse(sipBroMessage.getBytes(StandardCharsets.UTF_8)).get(0);
                String expectedBroTimestamp = "1216698441.346819";
                Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
                String expectedTimestamp = "1216698441346";
                Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                Assert.assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));

                Assert.assertEquals(broJson.get("uid").toString(), rawJson.get("uid").toString());
                Assert.assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                Assert.assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                Assert.assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                Assert.assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
                Assert.assertEquals(broJson.get("trans_depth").toString(), rawJson.get("trans_depth").toString());
                Assert.assertEquals(broJson.get("method").toString(), rawJson.get("method").toString());
                Assert.assertEquals(broJson.get("uri").toString(), rawJson.get("uri").toString());
                Assert.assertEquals(broJson.get("request_from").toString(), rawJson.get("request_from").toString());
                Assert.assertEquals(broJson.get("request_to").toString(), rawJson.get("request_to").toString());
                Assert.assertEquals(broJson.get("response_from").toString(), rawJson.get("response_from").toString());
                Assert.assertEquals(broJson.get("response_to").toString(), rawJson.get("response_to").toString());
                Assert.assertEquals(broJson.get("call_id").toString(), rawJson.get("call_id").toString());
                Assert.assertEquals(broJson.get("seq").toString(), rawJson.get("seq").toString());
                Assert.assertEquals(broJson.get("request_path").toString(), rawJson.get("request_path").toString());
                Assert.assertEquals(broJson.get("response_path").toString(), rawJson.get("response_path").toString());
                Assert.assertEquals(broJson.get("user_agent").toString(), rawJson.get("user_agent").toString());
                Assert.assertEquals(broJson.get("status_code").toString(), rawJson.get("status_code").toString());
                Assert.assertEquals(broJson.get("status_msg").toString(), rawJson.get("status_msg").toString());
                Assert.assertEquals(broJson.get("request_body_len").toString(), rawJson.get("request_body_len").toString());
                Assert.assertEquals(broJson.get("response_body_len").toString(), rawJson.get("response_body_len").toString());
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
	public final static String protocolKeyCleanedUp;

	@SuppressWarnings("rawtypes")
	@Test
	public void testProtocolKeyCleanedUp() throws ParseException {
		Map rawMessageMap = (Map) jsonParser.parse(protocolKeyCleanedUp);
		JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());

		JSONObject broJson = broParser.parse(protocolKeyCleanedUp.getBytes(StandardCharsets.UTF_8)).get(0);
		String expectedBroTimestamp = "1402307733.473";
		Assert.assertEquals(broJson.get("bro_timestamp"), expectedBroTimestamp);
		String expectedTimestamp = "1402307733473";
		Assert.assertEquals(broJson.get("timestamp").toString(), expectedTimestamp);
                Assert.assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
                Assert.assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
                Assert.assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
                Assert.assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
		Assert.assertTrue(broJson.get("original_string").toString().startsWith("HTTP"));
	}

	@Test(expected=IllegalStateException.class)
	public void testBadMessage()  throws ParseException {
		broParser.parse("{ \"foo\" : \"bar\"}".getBytes(StandardCharsets.UTF_8));
	}

	@Test(expected=IllegalStateException.class)
	public void testBadMessageNonJson() {
		broParser.parse("foo bar".getBytes(StandardCharsets.UTF_8));
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
