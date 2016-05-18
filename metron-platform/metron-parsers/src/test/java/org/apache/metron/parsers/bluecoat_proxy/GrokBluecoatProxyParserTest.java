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

package org.apache.metron.parsers.bluecoat_proxy;

import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class GrokBluecoatProxyParserTest {

	private final String grokPath = "../metron-parsers/src/main/resources/patterns/bluecoat_proxy";
	private final String grokLabel = "BLUECOAT";
	private final String dateFormat = "yyyy-MM-dd HH:mm:ss";
	private final String timestampField = "timestamp";

	@Test
	public void testParseLine() throws Exception {
		
		//Set up parser, parse message
		GrokBluecoatProxyParser parser = new GrokBluecoatProxyParser(grokPath, grokLabel);
		parser.withDateFormat(dateFormat).withTimestampField(timestampField);
		String testString = "2016-05-16 15:17:26 283 101.215.31.19 200 TCP_NC_MISS 375 1046" +
				" GET http www.saavnarnia.com 80 /stats.php" +
				" ?ev=site:player:progressing:300&songid=RT6VpBOP&_t=1463411845618" +
				" qam351 ORG\\GR%20GG%20COF%20USR%20Companyweb - www.saavnarnia.com" +
				" text/html;%20charset=UTF-8" +
				" http://www.saavnarnia.com/s/album/hindi/Greatest-Hits-Of-Coke-Studio-@-MTV-Vol.-1-2013/i9JkxZIt6Zk_" +
				" \"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML," +
				" like Gecko) Chrome/49.200.263.110 Safari/537.36\" OBSERVED \"Entertainment;Audio/Video Clips\" - 170.237.230.7 Certificate";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);

		assertEquals(parsedJSON.get("csauthtype"), "Certificate");
		assertEquals(parsedJSON.get("ip_dst_port"), 80);
		assertEquals(parsedJSON.get("csusername"), "qam351");
		assertEquals(parsedJSON.get("http_uripath"), "/stats.php");
		assertEquals(parsedJSON.get("rscontenttype"), "text/html;%20charset=UTF-8");
		assertEquals(parsedJSON.get("protocol"), "http");
		assertEquals(parsedJSON.get("http_method"), "GET");
		assertEquals(parsedJSON.get("original_string"), "2016-05-16 15:17:26 283 101.215.31.19 200 TCP_NC_MISS 375 1046 GET http www.saavnarnia.com 80 /stats.php ?ev=site:player:progressing:300&songid=RT6VpBOP&_t=1463411845618 qam351 ORG\\GR%20GG%20COF%20USR%20Companyweb - www.saavnarnia.com text/html;%20charset=UTF-8 http://www.saavnarnia.com/s/album/hindi/Greatest-Hits-Of-Coke-Studio-@-MTV-Vol.-1-2013/i9JkxZIt6Zk_ \"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.200.263.110 Safari/537.36\" OBSERVED \"Entertainment;Audio/Video Clips\" - 170.237.230.7 Certificate");
		assertEquals(parsedJSON.get("csauthgroup"), "ORG\\GR%20GG%20COF%20USR%20Companyweb");
		assertEquals(parsedJSON.get("csbytes"), 1046);
		assertEquals(parsedJSON.get("proxy_ip_addr"), "170.237.230.7");
		assertEquals(parsedJSON.get("ip_src_addr"), "101.215.31.19");
		assertEquals(parsedJSON.get("http_uriquery"), "?ev=site:player:progressing:300&songid=RT6VpBOP&_t=1463411845618");
		assertEquals(parsedJSON.get("timestamp"), 1463411846000L);
		assertEquals(parsedJSON.get("scbytes"), 375);
		assertEquals(parsedJSON.get("cshost"), "www.saavnarnia.com");
		assertEquals(parsedJSON.get("scfilterresult"), "OBSERVED");
		assertEquals(parsedJSON.get("time_taken"), 283);
		assertEquals(parsedJSON.get("saction"), "TCP_NC_MISS");
		assertEquals(parsedJSON.get("http_referer"), "http://www.saavnarnia.com/s/album/hindi/Greatest-Hits-Of-Coke-Studio-@-MTV-Vol.-1-2013/i9JkxZIt6Zk_");
		assertEquals(parsedJSON.get("cscategories"), "Entertainment;Audio/Video Clips");
		assertEquals(parsedJSON.get("http_status"), 200);
		assertEquals(parsedJSON.get("http_useragent"), "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.200.263.110 Safari/537.36");
		assertEquals(parsedJSON.get("ssupliername"),"www.saavnarnia.com");
	}
	
	@Test
	public void testMalformedLine() throws Exception {
		//Set up parser, attempt to parse malformed message
		GrokBluecoatProxyParser parser = new GrokBluecoatProxyParser(grokPath, grokLabel);
		String testString = "2016-05-16  15:17:26 283 101.215.31.19 200 TCP_NC_MISS 375 1046" +
				" GET http www.saavnarnia.com 80 /stats.php" +
				" ?ev=site:player:progressing:300&songid=RT6VpBOP&_t=1463411845618" +
				" qam351 ORG\\GR%20GG%20COF%20USR%20Companyweb - www.saavnarnia.com" +
				" text/html;%20charset=UTF-8" +
				" http://www.saavnarnia.com/s/album/hindi/Greatest-Hits-Of-Coke-Studio-@-MTV-Vol.-1-2013/i9JkxZIt6Zk_" +
				" \"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML," +
				" like Gecko) Chrome/49.200.263.110 Safari/537.36\" OBSERVED \"Entertainment;Audio/Video Clips\" - 170.237.230.7 Certificate";		List<JSONObject> result = parser.parse(testString.getBytes());
		assertEquals(null, result);
	}
	
	@Test
	public void testParseEmptyLine() throws Exception {
		
		//Set up parser, attempt to parse malformed message
		GrokBluecoatProxyParser parser = new GrokBluecoatProxyParser(grokPath, grokLabel);
		String testString = "";
		List<JSONObject> result = parser.parse(testString.getBytes());		
		assertEquals(null, result);
	}
		
}
