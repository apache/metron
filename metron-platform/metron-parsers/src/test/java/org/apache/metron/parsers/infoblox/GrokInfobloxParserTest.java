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

package org.apache.metron.parsers.infoblox;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GrokInfobloxParserTest {

    private Map<String, Object> parserConfig;

    @Before
    public void setup() {
        parserConfig = new HashMap<>();
        parserConfig.put("grokPath", "../metron-parsers/src/main/resources/patterns/infoblox");
        parserConfig.put("patternLabel", "INFOBLOX");
        parserConfig.put("timestampField", "timestamp");
        parserConfig.put("dateFormat", "yyyy MMM dd HH:mm:ss");
    }

    @Test
    public void testParseQueryLine() {

        //Set up parser, parse message
        GrokInfobloxParser parser = new GrokInfobloxParser();
        parser.configure(parserConfig);
        String testString = "<30>Mar 31 13:48:57 10.26.9.26 named[19446]: client 101.26.165.24#59335 " +
                "(asdf-network-ach-server.data.org): query: asdf-network-ach-server.data.org IN A + (120.126.219.25)";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        //Compare fields
        assertEquals(parsedJSON.get("ip_address"), "10.26.9.26");
        assertEquals(parsedJSON.get("process_id"), 19446);
        assertEquals(parsedJSON.get("process"), "named");
        assertEquals(parsedJSON.get("dns_record_type"), "IN,A");
        assertEquals(parsedJSON.get("dns_query"), "asdf-network-ach-server.data.org");
        assertEquals(parsedJSON.get("priority"), 30);
        assertEquals(parsedJSON.get("dns_result"), "success");
        assertEquals(parsedJSON.get("original_string"), "<30>Mar 31 13:48:57 10.26.9.26 named[19446]: client 101.26.165.24#59335 (asdf-network-ach-server.data.org): query: asdf-network-ach-server.data.org IN A + (120.126.219.25)");
        assertEquals(parsedJSON.get("dns_server_interface"), "120.126.219.25");
        assertEquals(parsedJSON.get("dns_action_type"), "query");
        assertEquals(parsedJSON.get("dns_bind_parameters"), "+");
        assertEquals(parsedJSON.get("ip_src_addr"), "101.26.165.24");
        assertEquals(parsedJSON.get("ip_src_port"), 59335);
        assertEquals(parsedJSON.get("timestamp"), 1459432137000L);
    }

    @Test
    public void testParseErrorLine() {
        GrokInfobloxParser parser = new GrokInfobloxParser();
        parser.configure(parserConfig);
        String testString = "<30>Mar 31 09:48:59 219.44.24.107 named[22289]: error (unexpected RCODE REFUSED) resolving" +
                " '251.143.24.214.IN-ADDR.ARPA/PTR/IN': 208.178.170.234#53";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        //Compare fields
        assertEquals(parsedJSON.get("ip_address"), "219.44.24.107");
        assertEquals(parsedJSON.get("process_id"), 22289);
        assertEquals(parsedJSON.get("process"), "named");
        assertEquals(parsedJSON.get("dns_record_type"), "PTR,IN");
        assertEquals(parsedJSON.get("priority"), 30);
        assertEquals(parsedJSON.get("original_string"), "<30>Mar 31 09:48:59 219.44.24.107 named[22289]: error (unexpected RCODE REFUSED) resolving '251.143.24.214.IN-ADDR.ARPA/PTR/IN': 208.178.170.234#53");
        assertEquals(parsedJSON.get("dns_result"), "error");
        assertEquals(parsedJSON.get("dns_forward_return_code"), "REFUSED");
        assertEquals(parsedJSON.get("dns_action_type"), "query");
        assertEquals(parsedJSON.get("dns_query"), "251.143.24.214.IN-ADDR.ARPA");
        assertEquals(parsedJSON.get("dns_forward_server"), "208.178.170.234");
        assertEquals(parsedJSON.get("dns_forward_port"), 53);
        assertEquals(parsedJSON.get("timestamp"), 1459417739000L);
    }

    @Test
    public void testParseZoneLine() {
        GrokInfobloxParser parser = new GrokInfobloxParser();
        parser.configure(parserConfig);
        String testString = "<30>Mar 31 09:48:59 102.137.26.213 named[22628]: zone";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        assertEquals(parsedJSON.get("priority"), 30);
        assertEquals(parsedJSON.get("timestamp"), 1459417739000L);
        assertEquals(parsedJSON.get("ip_address"), "102.137.26.213");
        assertEquals(parsedJSON.get("process"), "named");
        assertEquals(parsedJSON.get("process_id"), 22628);
        assertEquals(parsedJSON.get("dns_action_type"), "zone_update");
    }

    @Test
    public void testParseUpdateFailLine() {
        GrokInfobloxParser parser = new GrokInfobloxParser();
        parser.configure(parserConfig);
        String testString = "<27>Mar 31 09:48:54 210.137.216.36 named[4018]: client 40.155.218.101#61440: update 'sharingbuilding.com/IN' denied";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        assertEquals(parsedJSON.get("priority"), 27);
        assertEquals(parsedJSON.get("timestamp"), 1459417734000L);
        assertEquals(parsedJSON.get("ip_address"), "210.137.216.36");
        assertEquals(parsedJSON.get("process"), "named");
        assertEquals(parsedJSON.get("process_id"), 4018);
        assertEquals(parsedJSON.get("ip_src_addr"), "40.155.218.101");
        assertEquals(parsedJSON.get("ip_src_port"), 61440);
        assertEquals(parsedJSON.get("dns_action_type"), "update");
        assertEquals(parsedJSON.get("dns_update_target"), "sharingbuilding.com");
        assertEquals(parsedJSON.get("dns_record_type"), "IN");
        assertEquals(parsedJSON.get("dns_result"), "denied");
    }

    @Test
    public void testParseUpdateSuccessLine() {
        GrokInfobloxParser parser = new GrokInfobloxParser();
        parser.configure(parserConfig);
        String testString = "<30>Mar 31 09:48:58 80.70.21.243 named[12172]: client 33.54.213.105#6714/key " +
                "dhcp_updater_default: updating zone '28.210.in-addr.arpa/IN': adding an RR at " +
                "'155.17.128.20.in-addr.arpa' PTR";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        assertEquals(parsedJSON.get("priority"), 30);
        assertEquals(parsedJSON.get("timestamp"), 1459417738000L);
        assertEquals(parsedJSON.get("ip_address"), "80.70.21.243");
        assertEquals(parsedJSON.get("process"), "named");
        assertEquals(parsedJSON.get("process_id"), 12172);
        assertEquals(parsedJSON.get("ip_src_addr"), "33.54.213.105");
        assertEquals(parsedJSON.get("ip_src_port"), 6714);
        assertEquals(parsedJSON.get("dns_auth_keyname"), "dhcp_updater_default");
        assertEquals(parsedJSON.get("original_string"), "<30>Mar 31 09:48:58 80.70.21.243 named[12172]: client 33.54.213.105#6714/key dhcp_updater_default: updating zone '28.210.in-addr.arpa/IN': adding an RR at '155.17.128.20.in-addr.arpa' PTR");
        assertEquals(parsedJSON.get("dns_result"), "success");
        assertEquals(parsedJSON.get("dns_update_message"), "updating zone '28.210.in-addr.arpa/IN': adding an RR at '155.17.128.20.in-addr.arpa' PTR");
    }

    @Test
    public void testParseDHCPRequestLine() {
        GrokInfobloxParser parser = new GrokInfobloxParser();
        parser.configure(parserConfig);
        String testString = "<30>Mar 31 09:48:59 190.124.222.103 dhcpd[6947]: DHCPREQUEST for 101.116.173.209 from" +
                " 01:1e:33:3c:4d:e2 (SEP001D535D6DE2) via 103.116.172.252 uid 01:11:2e:33:1c:4e:d2 (RENEW)";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        assertEquals(parsedJSON.get("priority"), 30);
        assertEquals(parsedJSON.get("timestamp"), 1459417739000L);
        assertEquals(parsedJSON.get("ip_address"), "190.124.222.103");
        assertEquals(parsedJSON.get("process"), "dhcpd");
        assertEquals(parsedJSON.get("process_id"), 6947);
        assertEquals(parsedJSON.get("dhcp_type"), "DHCPREQUEST");
        assertEquals(parsedJSON.get("original_string"), "<30>Mar 31 09:48:59 190.124.222.103 dhcpd[6947]: DHCPREQUEST for 101.116.173.209 from 01:1e:33:3c:4d:e2 (SEP001D535D6DE2) via 103.116.172.252 uid 01:11:2e:33:1c:4e:d2 (RENEW)");
        assertEquals(parsedJSON.get("dhcp_requested_ip"), "101.116.173.209");
        assertEquals(parsedJSON.get("src_mac"), "01:1e:33:3c:4d:e2");
        assertEquals(parsedJSON.get("dhcp_hostname"), "SEP001D535D6DE2");
        assertEquals(parsedJSON.get("dhcp_relay_ip"), "103.116.172.252");
        assertEquals(parsedJSON.get("dhcp_uid"), "01:11:2e:33:1c:4e:d2");
        assertEquals(parsedJSON.get("dhcp_options"), "RENEW");
    }

    @Test
    public void testParseDHCPackLine() {
        GrokInfobloxParser parser = new GrokInfobloxParser();
        parser.configure(parserConfig);
        String testString = "<30>Mar 31 09:48:59 122.214.23.121 dhcpd[17697]: DHCPACK on 170.215.176.151 to" +
                " 01:1e:85:e2:15:d4 (SEP0017955205C4) via eth2 relay 10.215.176.213 lease-duration 691084 (RENEW) uid" +
                " 01:11:1e:85:d2:25:e4";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        assertEquals(parsedJSON.get("dhcp_type"), "DHCPACK");
        assertEquals(parsedJSON.get("original_string"), "<30>Mar 31 09:48:59 122.214.23.121 dhcpd[17697]: DHCPACK on 170.215.176.151 to 01:1e:85:e2:15:d4 (SEP0017955205C4) via eth2 relay 10.215.176.213 lease-duration 691084 (RENEW) uid 01:11:1e:85:d2:25:e4");
        assertEquals(parsedJSON.get("priority"), 30);
        assertEquals(parsedJSON.get("dhcp_hostname"), "SEP0017955205C4");
        assertEquals(parsedJSON.get("dhcp_interface"), "eth2");
        assertEquals(parsedJSON.get("process_id"), 17697);
        assertEquals(parsedJSON.get("dhcp_uid"), "01:11:1e:85:d2:25:e4");
        assertEquals(parsedJSON.get("dhcp_lease_duration"), 691084);
        assertEquals(parsedJSON.get("timestamp"), 1459417739000L);
        assertEquals(parsedJSON.get("process"), "dhcpd");
        assertEquals(parsedJSON.get("dhcp_relay_ip"), "10.215.176.213");
        assertEquals(parsedJSON.get("dhcp_options"), "RENEW");
        assertEquals(parsedJSON.get("ip_address"), "122.214.23.121");
        assertEquals(parsedJSON.get("dst_mac"), "01:1e:85:e2:15:d4");
    }

    @Test
    public void testParseDHCPInformLine() {
        GrokInfobloxParser parser = new GrokInfobloxParser();
        parser.configure(parserConfig);
        String testString = "<30>Mar 11 10:09:34 122.214.23.121 dhcpd[31169]: DHCPINFORM from 10.22.26.17 via 102.220.26.133";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        assertEquals(parsedJSON.get("dhcp_type"), "DHCPINFORM");
        assertEquals(parsedJSON.get("original_string"), "<30>Mar 11 10:09:34 122.214.23.121 dhcpd[31169]: DHCPINFORM from 10.22.26.17 via 102.220.26.133");
        assertEquals(parsedJSON.get("priority"), 30);
        assertEquals(parsedJSON.get("process_id"), 31169);
        assertEquals(parsedJSON.get("ip_src_addr"), "10.22.26.17");
        assertEquals(parsedJSON.get("timestamp"), 1457690974000L);
        assertEquals(parsedJSON.get("process"), "dhcpd");
        assertEquals(parsedJSON.get("dhcp_relay_ip"), "102.220.26.133");
        assertEquals(parsedJSON.get("ip_address"), "122.214.23.121");
    }

    @Test
    public void testCheckhintsLine() {
        GrokInfobloxParser parser = new GrokInfobloxParser();
        parser.configure(parserConfig);
        String testString = "<28>Mar 11 10:09:33 19.214.214.106 named[20084]: checkhints: view 4: " +
                "h.root-servers.net/AAAA (2101:50:10::53) missing from hints";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        assertEquals(parsedJSON.get("dns_result"), "success");
        assertEquals(parsedJSON.get("original_string"), "<28>Mar 11 10:09:33 19.214.214.106 named[20084]: checkhints: view 4: h.root-servers.net/AAAA (2101:50:10::53) missing from hints");
        assertEquals(parsedJSON.get("priority"), 28);
        assertEquals(parsedJSON.get("process_id"), 20084);
        assertEquals(parsedJSON.get("timestamp"), 1457690973000L);
        assertEquals(parsedJSON.get("process"), "named");
        assertEquals(parsedJSON.get("ip_address"), "19.214.214.106");
        assertEquals(parsedJSON.get("message"), "checkhints: view 4: h.root-servers.net/AAAA (2101:50:10::53) missing from hints");
    }

    @Test
    public void testDHCPUnknownLine() {
        GrokInfobloxParser parser = new GrokInfobloxParser();
        parser.configure(parserConfig);
        String testString = "<30>Mar 11 10:09:34 122.214.23.121 dhcpd[31169]: " +
                "r-l-e:107.120.246.124,Renewed,SEPA0CF5B810783,a0:cc:d9:80:07:e3,1457708974,1458400174,,$";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        assertEquals(parsedJSON.get("original_string"), "<30>Mar 11 10:09:34 122.214.23.121 dhcpd[31169]: r-l-e:107.120.246.124,Renewed,SEPA0CF5B810783,a0:cc:d9:80:07:e3,1457708974,1458400174,,$");
        assertEquals(parsedJSON.get("priority"), 30);
        assertEquals(parsedJSON.get("process_id"), 31169);
        assertEquals(parsedJSON.get("timestamp"), 1457690974000L);
        assertEquals(parsedJSON.get("process"), "dhcpd");
        assertEquals(parsedJSON.get("ip_address"), "122.214.23.121");
        assertEquals(parsedJSON.get("message"), "r-l-e:107.120.246.124,Renewed,SEPA0CF5B810783,a0:cc:d9:80:07:e3,1457708974,1458400174,,$");
    }

    @Test
    public void testUnknownLine() {
        GrokInfobloxParser parser = new GrokInfobloxParser();
        parser.configure(parserConfig);
        String testString = "<14>Mar 11 10:09:34 110.167.112.121 db_jnld: Resolved conflict for replicated delete of " +
                "PTR \"5.233\" in zone \"21.110.in-addr.arpa\".";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        assertEquals(parsedJSON.get("original_string"), "<14>Mar 11 10:09:34 110.167.112.121 db_jnld: Resolved conflict for replicated delete of PTR \"5.233\" in zone \"21.110.in-addr.arpa\".");
        assertEquals(parsedJSON.get("priority"), 14);
        assertEquals(parsedJSON.get("timestamp"), 1457690974000L);
        assertEquals(parsedJSON.get("process"), "db_jnld");
        assertEquals(parsedJSON.get("ip_address"), "110.167.112.121");
        assertEquals(parsedJSON.get("message"), "Resolved conflict for replicated delete of PTR \"5.233\" in zone \"21.110.in-addr.arpa\".");
    }

    @Test
    public void testParseEmptyLine() {
        GrokInfobloxParser parser = new GrokInfobloxParser();
        parser.configure(parserConfig);
        String testString = "";
        List<JSONObject> result = parser.parse(testString.getBytes());

        assertNull(result);
    }

    @Test
    public void testMalformedLine() {
        GrokInfobloxParser parser = new GrokInfobloxParser();
        parser.configure(parserConfig);
        String testString = "<30>Mar 31 13:48: 57 10.26.9.26 named[19446]: client 101.26.165.24#59335 " +
                "(asdf-network-ach-server.data.org): query: asdf-network-ach-server.data.org IN A + (120.126.219.25)";
        List<JSONObject> result = parser.parse(testString.getBytes());

        assertNull(result);
    }

}
