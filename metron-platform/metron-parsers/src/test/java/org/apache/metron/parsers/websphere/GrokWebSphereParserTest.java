package org.apache.metron.parsers.websphere;

import static org.junit.Assert.assertEquals;
import java.util.List;
import org.json.simple.JSONObject;
import org.junit.Test;

public class GrokWebSphereParserTest {
	
	@Test
	public void testParseLoginLine() throws Exception {
		
		//Set up parser, parse message
		GrokWebSphereParser parser = new GrokWebSphereParser();
		String testString = "<133>Apr 15 17:47:28 ABCXML1413 [rojOut][0x81000033][auth][notice] user(rick007): "
				+ "[120.43.200.6]: User logged into 'cohlOut'.";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		
		//Compare fields
		assertEquals(parsedJSON.get("priority") + "", "133");
		assertEquals(parsedJSON.get("timestamp") + "", "1460742448000");
		assertEquals(parsedJSON.get("hostname"), "ABCXML1413");
		assertEquals(parsedJSON.get("security_domain"), "rojOut");
		assertEquals(parsedJSON.get("event_code"), "0x81000033");
		assertEquals(parsedJSON.get("event_type"), "auth");
		assertEquals(parsedJSON.get("severity"), "notice");
		assertEquals(parsedJSON.get("event_subtype"), "login");
		assertEquals(parsedJSON.get("username"), "rick007");
		assertEquals(parsedJSON.get("ip_src_addr"), "120.43.200.6");
	}
	
	@Test
	public void tetsParseLogoutLine() throws Exception {
		
		//Set up parser, parse message
		GrokWebSphereParser parser = new GrokWebSphereParser();
		String testString = "<134>Apr 15 18:02:27 PHIXML3RWD [0x81000019][auth][info] [14.122.2.201]: "
				+ "User 'hjpotter' logged out from 'default'.";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		
		//Compare fields
		assertEquals(parsedJSON.get("priority") + "", "134");
		assertEquals(parsedJSON.get("timestamp") + "", "1460743347000");
		assertEquals(parsedJSON.get("hostname"), "PHIXML3RWD");
		assertEquals(parsedJSON.get("event_code"), "0x81000019");
		assertEquals(parsedJSON.get("event_type"), "auth");
		assertEquals(parsedJSON.get("severity"), "info");
		assertEquals(parsedJSON.get("ip_src_addr"), "14.122.2.201");
		assertEquals(parsedJSON.get("username"), "hjpotter");
		assertEquals(parsedJSON.get("security_domain"), "default");
	}
	
	@Test
	public void tetsParseRBMLine() throws Exception {
		
		//Set up parser, parse message
		GrokWebSphereParser parser = new GrokWebSphereParser();
		String testString = "<131>Apr 15 17:36:35 ROBXML3QRS [0x80800018][auth][error] rbm(RBM-Settings): "
				+ "trans(3502888135)[request] gtid(3502888135): RBM: Resource access denied.";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		
		//Compare fields
		assertEquals(parsedJSON.get("priority") + "", "131");
		assertEquals(parsedJSON.get("timestamp") + "", "1460741795000");
		assertEquals(parsedJSON.get("hostname"), "ROBXML3QRS");
		assertEquals(parsedJSON.get("event_code"), "0x80800018");
		assertEquals(parsedJSON.get("event_type"), "auth");
		assertEquals(parsedJSON.get("severity"), "error");
		assertEquals(parsedJSON.get("process"), "rbm");
		assertEquals(parsedJSON.get("message"), "trans(3502888135)[request] gtid(3502888135): RBM: Resource access denied.");
	}
	
	@Test
	public void tetsParseOtherLine() throws Exception {
		
		//Set up parser, parse message
		GrokWebSphereParser parser = new GrokWebSphereParser();
		String testString = "<134>Apr 15 17:17:34 SAGPXMLQA333 [0x8240001c][audit][info] trans(191): (admin:default:system:*): "
				+ "ntp-service 'NTP Service' - Operational state down";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		
		//Compare fields
		assertEquals(parsedJSON.get("priority") + "", "134");
		assertEquals(parsedJSON.get("timestamp") + "", "1460740654000");
		assertEquals(parsedJSON.get("hostname"), "SAGPXMLQA333");
		assertEquals(parsedJSON.get("event_code"), "0x8240001c");
		assertEquals(parsedJSON.get("event_type"), "audit");
		assertEquals(parsedJSON.get("severity"), "info");
		assertEquals(parsedJSON.get("process"), "trans");
		assertEquals(parsedJSON.get("message"), "(admin:default:system:*): ntp-service 'NTP Service' - Operational state down");
	}
		
}
