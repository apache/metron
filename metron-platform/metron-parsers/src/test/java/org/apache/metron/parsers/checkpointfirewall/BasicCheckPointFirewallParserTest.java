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

package org.apache.metron.parsers.checkpointfirewall;

import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class BasicCheckPointFirewallParserTest {

    private BasicCheckPointFirewallParser basicCheckPointFirewallParser = new BasicCheckPointFirewallParser();

    public BasicCheckPointFirewallParserTest() throws Exception {
        super();
    }

    @Test
    public void testParseWithKeyValue() throws Exception {
        String testString = "Apr 01 00:17:41 [10.255.255.255] Apr 01 2016 00:17:40: %CHKPNT-2-080040: Origin=Death_STAR,Application=HeavyBlaster," +
                "Name=finn,Type=firewall_application,Category=applications,Operation=\"Install Policy\",Uid={00000035-00B7-0042-9209-1DEE70DDFA4C}," +
                "Administrator=rey007,Client=LUKE1-SKYWALKER2,Subject=\"Policy Installation\",Audit Status=Success,Info=\"Security Policy : finn-current\"," +
                "Operation Number=7,client_ip=10.255.255.255,";

        List<JSONObject> result = basicCheckPointFirewallParser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        //Compare fields
        assertEquals(parsedJSON.get("timestamp"), 1459469861000L);
        assertEquals(parsedJSON.get("firewallName"), "%CHKPNT-2-080040");
        assertEquals(parsedJSON.get("timestamp2"), "Apr 01 2016 00:17:40");
        assertEquals(parsedJSON.get("audit_status"), "Success");
        assertEquals(parsedJSON.get("subject"), "\"Policy Installation\"");
        assertEquals(parsedJSON.get("origin"), "Death_STAR");
        assertEquals(parsedJSON.get("ipAddress"), "10.255.255.255");
        assertEquals(parsedJSON.get("type"), "firewall_application");
        assertEquals(parsedJSON.get("uid"), "{00000035-00B7-0042-9209-1DEE70DDFA4C}");
        assertEquals(parsedJSON.get("administrator"), "rey007");
        assertEquals(parsedJSON.get("application"), "HeavyBlaster");
        assertEquals(parsedJSON.get("operation_number"), "7");
        assertEquals(parsedJSON.get("name"), "finn");
        assertEquals(parsedJSON.get("client"), "LUKE1-SKYWALKER2");
        assertEquals(parsedJSON.get("client_ip"), "10.255.255.255");
        assertEquals(parsedJSON.get("category"), "applications");
        assertEquals(parsedJSON.get("operation"), "\"Install Policy\"");
        assertEquals(parsedJSON.get("info"), "\"Security Policy : finn-current\"");
    }

    @Test
    public void testParseWithoutKeyValue() throws Exception {
        String testString = "Apr 01 00:00:03 [10.255.255.255] Apr 01 2016 00:00:02: %CHKPNT-5-100023: monitor, rey3finn004, inbound, abc3-01, 10.255.255.255, 41655, 10.255.255.255," +
                " 11500, 11500, tcp, 1, , , , , , , , , illegal header format detected: 0, , , , , , , , , , *** Confidential ***, , , , ,  1Apr2016  0:00:02, 0, SmartDefense," +
                " , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , 100023, Block HTTP Non Compliant, illegal header format detected, , , , , , , , , , , ," +
                " {B78F2121-0028-4369-A684-6422FFE76063}";
        List<JSONObject> result = basicCheckPointFirewallParser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        //Compare fields
        assertEquals(parsedJSON.get("timestamp"), 1459468803000L);
        assertEquals(parsedJSON.get("firewallName"), "%CHKPNT-5-100023");
        assertEquals(parsedJSON.get("timestamp2"), "Apr 01 2016 00:00:02");
        assertEquals(parsedJSON.get("sourcePort"), "41655");
        assertEquals(parsedJSON.get("interfaceDirection"), "inbound");
        assertEquals(parsedJSON.get("destinationIp"), "10.255.255.255");
        assertEquals(parsedJSON.get("protocol"), "tcp");
        assertEquals(parsedJSON.get("action"), "monitor");
        assertEquals(parsedJSON.get("ipAddress"), "10.255.255.255");
        assertEquals(parsedJSON.get("sourceIp"), "10.255.255.255");
        assertEquals(parsedJSON.get("eventDate"), "1Apr2016  0:00:02");
        assertEquals(parsedJSON.get("destinationPort"), "11500");
        assertEquals(parsedJSON.get("tbd54"), "100023");
        assertEquals(parsedJSON.get("tbd55"), "Block HTTP Non Compliant");
        assertEquals(parsedJSON.get("origin"), "rey3finn004");
        assertEquals(parsedJSON.get("tbd56"), "illegal header format detected");
        assertEquals(parsedJSON.get("eventSource"), "SmartDefense");
        assertEquals(parsedJSON.get("tbd17"), "*** Confidential ***");
        assertEquals(parsedJSON.get("messageType1"), "illegal header format detected: 0");
        assertEquals(parsedJSON.get("interfaceName"), "abc3-01");
        assertEquals(parsedJSON.get("tbd22"), "0");
        assertEquals(parsedJSON.get("ruleNumber"), "1");
        assertEquals(parsedJSON.get("tbd68"), "{B78F2121-0028-4369-A684-6422FFE76063}");
        assertEquals(parsedJSON.get("service"), "11500");
    }

    @Test
    public void tetsParseMalformedOtherLine() throws Exception {
        String testString = "Apr 01 00:17:41 [10.255.255.255] Apr 01 2016 00:17:40: %CHKPNT-2-080040: Origin=Death_STAR,Application=HeavyBlaster," +
                "Name=finn,Type=firewall_application,,,,Category=applications,Operation=\"Install Policy\",Uid={00000035-00B7-0042-9209-1DEE70DDFA4C}," +
                "Administrator=rey007,Client=LUKE1-SKYWALKER2,Subject=\"Policy Installation\",Audit Status=Success,Info=\"Security Policy : finn-current\"," +
                "Operation Number=7,client_ip=10.255.255.255,";
        List<JSONObject> result = basicCheckPointFirewallParser.parse(testString.getBytes());
        assertEquals(null, result);
    }

    @Test
    public void testParseEmptyLine() throws Exception {
        String testString = "";
        List<JSONObject> result = basicCheckPointFirewallParser.parse(testString.getBytes());
        assertEquals(null, result);
    }

}
