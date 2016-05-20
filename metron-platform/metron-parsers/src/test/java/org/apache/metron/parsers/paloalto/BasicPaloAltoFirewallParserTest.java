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
package org.apache.metron.parsers.paloalto;

import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BasicPaloAltoFirewallParserTest {

    private BasicPaloAltoFirewallParser basicPaloAltoFirewallParser = new BasicPaloAltoFirewallParser();

    public BasicPaloAltoFirewallParserTest() throws Exception {
        super();        
    }

    @Test
    public void assertThreatTuple() {

        String testString = "<11>Jan  5 05:38:59 PAN1.exampleCustomer.com 1,2015/01/05 05:38:58,0006C110285,THREAT,vulnerability,1,2015/01/05 05:38:58,10.0.0.115," +
                "216.0.10.198,0.0.0.0,0.0.0.0,EX-Allow,example\\user.name,,web-browsing,vsys1,internal,external,ethernet1/2,ethernet1/1,LOG-Default," +
                "2015/01/05 05:38:58,12031,1,54180,80,0,0,0x80004000,tcp,reset-both,\"ad.aspx?f=300x250&id=12;tile=1;ord=67AF705D60B1119C0F18BEA336F9\"," +
                "HTTP: IIS Denial Of Service Attempt(40019),any,high,client-to-server,347368099,0x0,10.0.0.0-10.255.255.255,US,0,,1200568889751109656,,";

        List<JSONObject> result = basicPaloAltoFirewallParser.parse(testString.getBytes());

        JSONObject json = result.get(0);

        //Compare all the fields
        assertEquals(json.get("timestamp"), 1451972339000L);
        assertEquals(json.get("priority"), "11");
        assertEquals(json.get("hostname"), "PAN1.exampleCustomer.com");
        assertEquals(json.get("futureUse"), "1");
        assertEquals(json.get("receiveTime"), "2015/01/05 05:38:58");
        assertEquals(json.get("serialNumber"), "0006C110285");
        assertEquals(json.get("type"), "THREAT");
        assertEquals(json.get("subtype"), "vulnerability");
        assertEquals(json.get("futureUse2"), "1");
        assertEquals(json.get("generatedTime"), "2015/01/05 05:38:58");
        assertEquals(json.get("ipSrcAddr"), "10.0.0.115");
        assertEquals(json.get("ipDstAddr"), "216.0.10.198");
        assertEquals(json.get("natSourceIp"), "0.0.0.0");
        assertEquals(json.get("natDestinationIp"), "0.0.0.0");
        assertEquals(json.get("ruleName"), "EX-Allow");
        assertEquals(json.get("srcUserName"), "example\\user.name");
        assertEquals(json.get("dstUserName"), "");
        assertEquals(json.get("application"), "web-browsing");
        assertEquals(json.get("virtualSystem"), "vsys1");
        assertEquals(json.get("sourceZone"), "internal");
        assertEquals(json.get("destinationZone"), "external");
        assertEquals(json.get("ingressInterface"), "ethernet1/2");
        assertEquals(json.get("egressInterface"), "ethernet1/1");
        assertEquals(json.get("logForwardingProfile"), "LOG-Default");
        assertEquals(json.get("futureUse3"), "2015/01/05 05:38:58");
        assertEquals(json.get("sessionId"), "12031");
        assertEquals(json.get("repeatCount"), "1");
        assertEquals(json.get("ipSrcPort"), "54180");
        assertEquals(json.get("ipDstPort"), "80");
        assertEquals(json.get("natSourcePort"), "0");
        assertEquals(json.get("natDestinationPort"), "0");
        assertEquals(json.get("flags"), "0x80004000");
        assertEquals(json.get("protocol"), "tcp");
        assertEquals(json.get("action"), "reset-both");
        assertEquals(json.get("miscellaneous"), "\"ad.aspx?f=300x250&id=12;tile=1;ord=67AF705D60B1119C0F18BEA336F9\"");
        assertEquals(json.get("threatId"), "HTTP: IIS Denial Of Service Attempt(40019)");
        assertEquals(json.get("category"), "any");
        assertEquals(json.get("severity"), "high");
        assertEquals(json.get("direction"), "client-to-server");
        assertEquals(json.get("sequenceNumber"), "347368099");
        assertEquals(json.get("actionFlags"), "0x0");
        assertEquals(json.get("sourceLocation"), "10.0.0.0-10.255.255.255");
        assertEquals(json.get("destinationLocation"), "US");
        assertEquals(json.get("futureUse4"), "0");
        assertEquals(json.get("contentType"), "");
        assertEquals(json.get("pcapId"), "1200568889751109656");
        assertEquals(json.get("fileDigest"), "");
        assertEquals(json.get("cloud"), "");
        assertEquals(json.get("urlIndex"), "");
        assertEquals(json.get("userAgent"), "");
        assertEquals(json.get("fileType"), "");
        assertEquals(json.get("xForwardedFor"), "");
        assertEquals(json.get("referrer"), "");
        assertEquals(json.get("sender"), "");
        assertEquals(json.get("subject"), "");
        assertEquals(json.get("recipient"), "");
        assertEquals(json.get("reportId"), "");
        assertEquals(json.get("deviceGroupHierarchyLevel1"), "");
        assertEquals(json.get("deviceGroupHierarchyLevel2"), "");
        assertEquals(json.get("deviceGroupHierarchyLevel3"), "");
        assertEquals(json.get("deviceGroupHierarchyLevel4"), "");
        assertEquals(json.get("virtualSystemName"), "");
        assertEquals(json.get("deviceName"), "");
        assertEquals(json.get("futureUse5"), "");
    }

    @Test
    public void testEmpty() {
        String testString = "";
        List<JSONObject> result = basicPaloAltoFirewallParser.parse(testString.getBytes());
        assertNull(result);
    }

    @Test
    public void testMalformed() {
        String testString = "<11>  Jan  5 05:3  5 9 PAN1.exampleCustomer .com 1,2015/01/05 05:38:58,0006C110285,THREAT,vulnerability,1,2015/01/05 05:38:58,10.0.0.115," +
                "216.0.10.198,0.0.0.0,0.0.0.0,EX-Allow,example\\user.name,,web-browsing,vsys1,internal,external,ethernet1/2,ethernet1/1,LOG-Default," +
                "2015/01/05 05:38:58,12031,1,54180,80,0,0,0x80004000,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,tcp,reset-both,\"ad.aspx?f=300x250&id=12;tile=1;ord=67AF705D60B1119C0F18BEA336F9\"," +
                "HTTP: IIS Denial Of Service Attempt(40019),any,high,client-to-server,347368099,0x0,10.0.0.0-10.255.255.255,US,0,,1200568889751109656,,";
        List<JSONObject> result = basicPaloAltoFirewallParser.parse(testString.getBytes());
        assertNull(result);
    }


}
