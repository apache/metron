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

package org.apache.metron.parsers.cef;

import static org.junit.Assert.assertEquals;
import java.util.List;
import org.json.simple.JSONObject;
import org.junit.Test;

public class CEFParserTest {
	
	private CEFParser parser = new CEFParser();
	
	//Tests a CEF message from a FireEye device
	@Test
	public void testParseFireEyeCEF() {
		String testString = "<161>fenotify-1315069887.alert: CEF:0|FireEye|CMS|7.7.1.439492|MO|malware-object|4|rt=Mar 30 2016 09:55:01 UTC "
				+ "fileHash=481e959d8qrstbeaf67a30eff7e989c5 filePath=/3qZjLW6xPpz5C8dn-files_doc_5D6D7B.zip dvchost=SERVER003 cs4Label=link "
				+ "cs4=https://server0003/emps/eanalysis?e_id\\=129555927&type\\=attch duser=RickUser72@hotmail.com cn1Label=vlan cn1=0 externalId=1377069887 "
				+ "dvc=101.4.103.75 act=notified msg=5d777dfe-e4b9-4700-99e1-477ee8eab15b@msn.com flexString1Label=sname flexString1=Archive";
		
		parser = parser.withDateFormat("MMM dd yyyy HH:mm:ss Z");
		parser = parser.withTimeZone("UTC");
		
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);

		assertEquals(parsedJSON.get("priority"), "161");
		assertEquals(parsedJSON.get("device_vendor"), "FireEye");
		assertEquals(parsedJSON.get("device_product"), "CMS");
		assertEquals(parsedJSON.get("device_version"), "7.7.1.439492");
		assertEquals(parsedJSON.get("event_class_id"), "MO");
		assertEquals(parsedJSON.get("event_name"), "malware-object");
		assertEquals(parsedJSON.get("severity"), "4");
		assertEquals(String.valueOf(parsedJSON.get("timestamp")), "1459331701000");
		assertEquals(parsedJSON.get("fileHash"), "481e959d8qrstbeaf67a30eff7e989c5");
		assertEquals(parsedJSON.get("filePath"), "/3qZjLW6xPpz5C8dn-files_doc_5D6D7B.zip");
		assertEquals(parsedJSON.get("deviceHostName"), "SERVER003");
		assertEquals(parsedJSON.get("link"), "https://server0003/emps/eanalysis?e_id=129555927&type=attch");
		assertEquals(parsedJSON.get("dst_username"), "RickUser72@hotmail.com");
		assertEquals(parsedJSON.get("vlan"), "0");
		assertEquals(parsedJSON.get("externalId"), "1377069887");
		assertEquals(parsedJSON.get("deviceAddress"), "101.4.103.75");
		assertEquals(parsedJSON.get("deviceAction"), "notified");
		assertEquals(parsedJSON.get("message"), "5d777dfe-e4b9-4700-99e1-477ee8eab15b@msn.com");
		assertEquals(parsedJSON.get("sname"), "Archive");
	}
	
	
	//Tests a CEF message from a CyberArk device
	@Test
	public void testParseCyberArkCEF() {
		String testString = "Mar 21 14:05:00 VDCPVAULTN1 CEF:0|Cyber-Ark|Vault|7.20.0091|194|Backup Process Initiated|5|"
				+ "act=Backup Process Initiated suser=PR fname= dvc= shost=144.99.201.3 dhost= duser= externalId= app= "
				+ "reason= cs1Label=\"Affected User Name\" cs1= cs2Label=\"Safe Name\" cs2=RFCConfig cs3Label=\"Device Type\" "
				+ "cs3= cs4Label=\"Database\" cs4= cs5Label=\"Other info\" cs5= cn1Label=\"Request Id\" cn1= cn2Label=\"Ticket Id\" "
				+ "cn2=  msg=";
		
		parser = parser.withHeaderTimestampRegex("\\w\\w\\w \\d\\d \\d\\d:\\d\\d:\\d\\d").withDateFormat("MMM dd HH:mm:ss");
		parser = parser.withTimeZone("UTC");
		
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		
		assertEquals(parsedJSON.get("device_vendor"), "Cyber-Ark");
		assertEquals(parsedJSON.get("device_product"), "Vault");
		assertEquals(parsedJSON.get("device_version"), "7.20.0091");
		assertEquals(parsedJSON.get("event_class_id"), "194");
		assertEquals(parsedJSON.get("event_name"), "Backup Process Initiated");
		assertEquals(parsedJSON.get("severity"), "5");
		assertEquals(String.valueOf(parsedJSON.get("timestamp")), "1458569100000");
		assertEquals(parsedJSON.get("deviceAction"), "Backup Process Initiated");
		assertEquals(parsedJSON.get("src_username"), "PR");
		assertEquals(parsedJSON.get("src_hostname"), "144.99.201.3");
		assertEquals(parsedJSON.get("\"Safe Name\""), "RFCConfig");
	}
	
	
	//Tests a CEF message from a WAF device
	@Test
	public void testParseWafCEF() {
		String testString = "<14>CEF:0|Imperva Inc.|SecureSphere|10.0.0.4_16|ABC - Secure Login.vm Page Rate Limit UK - Source IP||High|"
				+ "act=alert dst=17.43.200.42 dpt=88 duser=${Alert.username} src=10.31.45.69 spt=34435 proto=TCP rt=31 March 2016 13:04:55 "
				+ "cat=Alert cs1= cs1Label=Policy cs2=ABC-Secure cs2Label=ServerGroup cs3=servers_svc cs3Label=ServiceName cs4=server_app "
				+ "cs4Label=ApplicationName cs5=QA cs5Label=Description";
		
		parser = parser.withDateFormat("dd MMMMM yyyy HH:mm:ss");
		parser = parser.withTimeZone("UTC");
		
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		
		assertEquals(parsedJSON.get("priority"), "14");
		assertEquals(parsedJSON.get("device_vendor"), "Imperva Inc.");
		assertEquals(parsedJSON.get("device_product"), "SecureSphere");
		assertEquals(parsedJSON.get("device_version"), "10.0.0.4_16");
		assertEquals(parsedJSON.get("event_class_id"), "ABC - Secure Login.vm Page Rate Limit UK - Source IP");
		assertEquals(parsedJSON.get("event_name"), null);
		assertEquals(parsedJSON.get("severity"), "High");	
		assertEquals(parsedJSON.get("deviceAction"), "alert");
		assertEquals(parsedJSON.get("ip_dst_addr"), "17.43.200.42");
		assertEquals(parsedJSON.get("ip_dst_port"), "88");
		assertEquals(parsedJSON.get("dst_username"), "${Alert.username}");
		assertEquals(parsedJSON.get("ip_src_addr"), "10.31.45.69");
		assertEquals(parsedJSON.get("ip_src_port"), "34435");
		assertEquals(parsedJSON.get("protocol"), "TCP");
		assertEquals(String.valueOf(parsedJSON.get("timestamp")), "1459429495000");
		assertEquals(parsedJSON.get("deviceEventCategory"), "Alert");
		assertEquals(parsedJSON.get("ServerGroup"), "ABC-Secure");
		assertEquals(parsedJSON.get("ServiceName"), "servers_svc");
		assertEquals(parsedJSON.get("ApplicationName"), "server_app");
		assertEquals(parsedJSON.get("Description"), "QA");	
	
	}
	
	
	//Test a CEF messages from an Adallom device
	@Test
	public void testParseAdallomCEF() {
		String testString = "2016-04-01T09:29:11.356-0400 CEF:0|Adallom|Adallom|1.0|56fe779ee4b0459f4e9a484a|ALERT_CABINET_EVENT_MATCH_AUDIT|0|"
				+ "msg=Activity policy 'User download/view file' was triggered by 'scolbert@gmail.com' suser=wanderson@rock.com "
				+ "start=1459517280810 end=1459517280810 audits=[\"AVPR-4oIPeFmuZ3CKKrg\",\"AVPR-wx80cd9PUpAu2aj\",\"AVPR-6XGPeFmuZ3CKKvx\","
				+ "\"AVPSALn_qE4Kgs_8_yK9\",\"AVPSASW3gw_f3aEvgEmi\"] services=[\"APPID_SXC\"] users=[\"lvader@hotmail.com\"] "
						+ "cs6=https://abcd-remote.console.arc.com/#/alerts/56fe779ee4b0459f4e9a484a cs6Label=consoleUrl";
		
		parser = parser.withHeaderTimestampRegex("\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d");
		parser = parser.withDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		parser = parser.withTimeZone("UTC");
		
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);
		
		assertEquals(parsedJSON.get("device_vendor"), "Adallom");
		assertEquals(parsedJSON.get("device_product"), "Adallom");
		assertEquals(parsedJSON.get("device_version"), "1.0");
		assertEquals(parsedJSON.get("event_class_id"), "56fe779ee4b0459f4e9a484a");
		assertEquals(parsedJSON.get("event_name"), "ALERT_CABINET_EVENT_MATCH_AUDIT");
		assertEquals(parsedJSON.get("severity"), "0");
		assertEquals(parsedJSON.get("message"), "Activity policy 'User download/view file' was triggered by 'scolbert@gmail.com'");
		assertEquals(parsedJSON.get("src_username"), "wanderson@rock.com");
		assertEquals(parsedJSON.get("startTime"), "1459517280810");
		assertEquals(parsedJSON.get("endTime"), "1459517280810");
		assertEquals(parsedJSON.get("audits"), "[\"AVPR-4oIPeFmuZ3CKKrg\",\"AVPR-wx80cd9PUpAu2aj\",\"AVPR-6XGPeFmuZ3CKKvx\",\"AVPSALn_qE4Kgs_8_yK9\",\"AVPSASW3gw_f3aEvgEmi\"]");
		assertEquals(parsedJSON.get("services"), "[\"APPID_SXC\"]");
		assertEquals(parsedJSON.get("users"), "[\"lvader@hotmail.com\"]");
		assertEquals(parsedJSON.get("consoleUrl"), "https://abcd-remote.console.arc.com/#/alerts/56fe779ee4b0459f4e9a484a");
		assertEquals(String.valueOf(parsedJSON.get("timestamp")), "1459502951000");
	}
	
	
	//Tests a CEF message with malformed fields
	@Test
	public void testParseMalformedCEF() {
		String testString = "2016-04-01T09:29:11.356-0400 CEF:0|Adallom|Adallom|1.0|ALERT_CABINET_EVENT_MATCH_AUDIT|0|"
				+ "msg=Activity policy 'User download/view file' was triggered by 'scolbert@gmail.com' suser=wanderson@rock.com "
				+ "start=1459517280810 end=1459517280810 audits=[\"AVPR-4oIPeFmuZ3CKKrg\",\"AVPR-wx80cd9PUpAu2aj\",\"AVPR-6XGPeFmuZ3CKKvx\","
				+ "\"AVPSALn_qE4Kgs_8_yK9\",\"AVPSASW3gw_f3aEvgEmi\"] services=[\"APPID_SXC\"] users=[\"lvader@hotmail.com\"] "
						+ "cs6=https://abcd-remote.console.arc.com/#/alerts/56fe779ee4b0459f4e9a484a cs6Label=consoleUrl";
		
		List<JSONObject> result = parser.parse(testString.getBytes());		
		assertEquals(null, result);
	}
	
	
	//Tests a CEF message with a malformed timestamp
	@Test
	public void testParseMalformedTimestamp() {
		String testString = "04-01T09:29:11.356-0400 CEF:0|Adallom|Adallom|1.0|ALERT_CABINET_EVENT_MATCH_AUDIT|0|test|"
				+ "msg=Activity policy 'User download/view file' was triggered by 'scolbert@gmail.com' suser=wanderson@rock.com "
				+ "start=1459517280810 end=1459517280810 audits=[\"AVPR-4oIPeFmuZ3CKKrg\",\"AVPR-wx80cd9PUpAu2aj\",\"AVPR-6XGPeFmuZ3CKKvx\","
				+ "\"AVPSALn_qE4Kgs_8_yK9\",\"AVPSASW3gw_f3aEvgEmi\"] services=[\"APPID_SXC\"] users=[\"lvader@hotmail.com\"] "
						+ "cs6=https://abcd-remote.console.arc.com/#/alerts/56fe779ee4b0459f4e9a484a cs6Label=consoleUrl";
		
		parser = parser.withHeaderTimestampRegex("\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d");
		parser = parser.withDateFormat("MM-dd'T'HH:mm:ss");
		parser = parser.withTimeZone("UTC");
		
		List<JSONObject> result = parser.parse(testString.getBytes());	
		JSONObject parsedJSON = result.get(0);
		assertEquals(parsedJSON.get("timestamp"), System.currentTimeMillis());
	}
	
	//Tests an empty line to insure there are no parser errors thrown 
	@Test
	public void testParseEmptyLine() {
		String testString = "";
		List<JSONObject> result = parser.parse(testString.getBytes());		
		assertEquals(null, result);
	}
}
