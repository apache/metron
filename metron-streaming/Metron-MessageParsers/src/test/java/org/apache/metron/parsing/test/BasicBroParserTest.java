package org.apache.metron.parsing.test;

import java.util.Map;

import junit.framework.TestCase;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.apache.metron.parsing.parsers.BasicBroParser;

public class BasicBroParserTest extends TestCase {

	/**
	 * The parser.
	 */
	private BasicBroParser broParser = null;
	private JSONParser jsonParser = null;

	/**
	 * Constructs a new <code>BasicBroParserTest</code> instance.
	 * 
	 * @throws Exception
	 */
	public BasicBroParserTest() throws Exception {
		broParser = new BasicBroParser();
		jsonParser = new JSONParser();		
	}

    public void testUnwrappedBroMessage() throws ParseException {
        String rawMessage = "{\"timestamp\":\"1449511228474\",\"uid\":\"CFgSLp4HgsGqXnNjZi\",\"source_ip\":\"104.130.172.191\",\"source_port\":33893,\"dest_ip\":\"69.20.0.164\",\"dest_port\":53,\"proto\":\"udp\",\"trans_id\":3514,\"rcode\":3,\"rcode_name\":\"NXDOMAIN\",\"AA\":false,\"TC\":false,\"RD\":false,\"RA\":false,\"Z\":0,\"rejected\":false,\"sensor\":\"cloudbro\",\"type\":\"dns\"}";

        JSONObject rawJson = (JSONObject)jsonParser.parse(rawMessage);

        JSONObject broJson = broParser.parse(rawMessage.getBytes());

        assertEquals(broJson.get("timestamp"), Long.parseLong(rawJson.get("timestamp").toString()));
        assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("source_ip").toString());
        assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("dest_ip").toString());
        assertEquals(broJson.get("ip_src_port"), rawJson.get("source_port"));
        assertEquals(broJson.get("ip_dst_port"), rawJson.get("dest_port"));
        assertEquals(broJson.get("uid").toString(), rawJson.get("uid").toString());
        assertEquals(broJson.get("trans_id").toString(), rawJson.get("trans_id").toString());
        assertEquals(broJson.get("sensor").toString(), rawJson.get("sensor").toString());
        assertEquals(broJson.get("protocol").toString(), rawJson.get("type").toString());
        assertEquals(broJson.get("rcode").toString(), rawJson.get("rcode").toString());
        assertEquals(broJson.get("rcode_name").toString(), rawJson.get("rcode_name").toString());
        assertTrue(broJson.get("original_string").toString().startsWith("DNS"));
    }

	@SuppressWarnings("rawtypes")
	public void testHttpBroMessage() throws ParseException {
		String rawMessage = "{\"http\":{\"ts\":1402307733473,\"uid\":\"CTo78A11g7CYbbOHvj\",\"id.orig_h\":\"192.249.113.37\",\"id.orig_p\":58808,\"id.resp_h\":\"72.163.4.161\",\"id.resp_p\":80,\"trans_depth\":1,\"method\":\"GET\",\"host\":\"www.cisco.com\",\"uri\":\"/\",\"user_agent\":\"curl/7.22.0 (x86_64-pc-linux-gnu) libcurl/7.22.0 OpenSSL/1.0.1 zlib/1.2.3.4 libidn/1.23 librtmp/2.3\",\"request_body_len\":0,\"response_body_len\":25523,\"status_code\":200,\"status_msg\":\"OK\",\"tags\":[],\"resp_fuids\":[\"FJDyMC15lxUn5ngPfd\"],\"resp_mime_types\":[\"text/html\"]}}";
		
		Map rawMessageMap = (Map) jsonParser.parse(rawMessage);
		JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());
		
		JSONObject broJson = broParser.parse(rawMessage.getBytes());
		assertEquals(broJson.get("timestamp").toString(), rawJson.get("ts").toString());
		assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
		assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
		assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
		assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
		assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));
		
		assertEquals(broJson.get("uid").toString(), rawJson.get("uid").toString());
		assertEquals(broJson.get("method").toString(), rawJson.get("method").toString());
		assertEquals(broJson.get("host").toString(), rawJson.get("host").toString());
		assertEquals(broJson.get("resp_mime_types").toString(), rawJson.get("resp_mime_types").toString());
	}
	
	@SuppressWarnings("rawtypes")
	public void testDnsBroMessage() throws ParseException {
		String rawMessage = "{\"dns\":{\"ts\":1402308259609,\"uid\":\"CuJT272SKaJSuqO0Ia\",\"id.orig_h\":\"10.122.196.204\",\"id.orig_p\":33976,\"id.resp_h\":\"144.254.71.184\",\"id.resp_p\":53,\"proto\":\"udp\",\"trans_id\":62418,\"query\":\"www.cisco.com\",\"qclass\":1,\"qclass_name\":\"C_INTERNET\",\"qtype\":28,\"qtype_name\":\"AAAA\",\"rcode\":0,\"rcode_name\":\"NOERROR\",\"AA\":true,\"TC\":false,\"RD\":true,\"RA\":true,\"Z\":0,\"answers\":[\"www.cisco.com.akadns.net\",\"origin-www.cisco.com\",\"2001:420:1201:2::a\"],\"TTLs\":[3600.0,289.0,14.0],\"rejected\":false}}";
		
		Map rawMessageMap = (Map) jsonParser.parse(rawMessage);
		JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());
		
		JSONObject broJson = broParser.parse(rawMessage.getBytes());
		assertEquals(broJson.get("timestamp").toString(), rawJson.get("ts").toString());
		assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
		assertEquals(broJson.get("ip_dst_addr").toString(), rawJson.get("id.resp_h").toString());
		assertEquals(broJson.get("ip_src_port").toString(), rawJson.get("id.orig_p").toString());
		assertEquals(broJson.get("ip_dst_port").toString(), rawJson.get("id.resp_p").toString());
		assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));
		
		assertEquals(broJson.get("qtype").toString(), rawJson.get("qtype").toString());
		assertEquals(broJson.get("trans_id").toString(), rawJson.get("trans_id").toString());
	}
	
	@SuppressWarnings("rawtypes")
	public void testFilesBroMessage() throws ParseException {
		String rawMessage = "{\"files\":{\"analyzers\": [\"X509\",\"MD5\",\"SHA1\"],\"conn_uids\":[\"C4tygJ3qxJBEJEBCeh\"],\"depth\": 0,\"duration\": 0.0,\"fuid\":\"FZEBC33VySG0nHSoO9\",\"is_orig\": false,\"local_orig\": false,\"md5\": \"eba37166385e3ef42464ed9752e99f1b\",\"missing_bytes\": 0,\"overflow_bytes\": 0,\"rx_hosts\": [\"10.220.15.205\"],\"seen_bytes\": 1136,\"sha1\": \"73e42686657aece354fbf685712361658f2f4357\",\"source\": \"SSL\",\"timedout\": false,\"ts\": \"1425845251334\",\"tx_hosts\": [\"68.171.237.7\"]}}";
		
		Map rawMessageMap = (Map) jsonParser.parse(rawMessage);
		JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());
		
		JSONObject broJson = broParser.parse(rawMessage.getBytes());
		assertEquals(broJson.get("timestamp").toString(), rawJson.get("ts").toString());
		assertEquals(broJson.get("ip_src_addr").toString(), ((JSONArray)rawJson.get("tx_hosts")).get(0).toString());
		assertEquals(broJson.get("ip_dst_addr").toString(), ((JSONArray)rawJson.get("rx_hosts")).get(0).toString());
		assertTrue(broJson.get("original_string").toString().startsWith(rawMessageMap.keySet().iterator().next().toString().toUpperCase()));
		
		assertEquals(broJson.get("fuid").toString(), rawJson.get("fuid").toString());
		assertEquals(broJson.get("md5").toString(), rawJson.get("md5").toString());
		assertEquals(broJson.get("analyzers").toString(), rawJson.get("analyzers").toString());
	}
	
	@SuppressWarnings("rawtypes")
	public void testProtocolKeyCleanedUp() throws ParseException {
		String rawMessage = "{\"ht*tp\":{\"ts\":1402307733473,\"uid\":\"CTo78A11g7CYbbOHvj\",\"id.orig_h\":\"192.249.113.37\",\"id.orig_p\":58808,\"id.resp_h\":\"72.163.4.161\",\"id.resp_p\":80,\"trans_depth\":1,\"method\":\"GET\",\"host\":\"www.cisco.com\",\"uri\":\"/\",\"user_agent\":\"curl/7.22.0 (x86_64-pc-linux-gnu) libcurl/7.22.0 OpenSSL/1.0.1 zlib/1.2.3.4 libidn/1.23 librtmp/2.3\",\"request_body_len\":0,\"response_body_len\":25523,\"status_code\":200,\"status_msg\":\"OK\",\"tags\":[],\"resp_fuids\":[\"FJDyMC15lxUn5ngPfd\"],\"resp_mime_types\":[\"text/html\"]}}";
		
		Map rawMessageMap = (Map) jsonParser.parse(rawMessage);
		JSONObject rawJson = (JSONObject) rawMessageMap.get(rawMessageMap.keySet().iterator().next());
		
		JSONObject broJson = broParser.parse(rawMessage.getBytes());
		
		assertEquals(broJson.get("timestamp").toString(), rawJson.get("ts").toString());
		assertEquals(broJson.get("ip_src_addr").toString(), rawJson.get("id.orig_h").toString());
		assertTrue(broJson.get("original_string").toString().startsWith("HTTP"));
	}
}
