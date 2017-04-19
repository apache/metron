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

package org.apache.metron.parsers.dhcpdump;

import junit.framework.TestCase;
import org.apache.metron.parsers.ParseException;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.text.SimpleDateFormat;

public class BasicDHCPDumpParserTest extends TestCase {

    /**
     * The parser.
     */
    private BasicDHCPDumpParser DHCPDumpParser = null;

    /**
     * Constructs a new <code>BasicDHCPDumpParserTest</code> instance.
     *
     * @throws Exception
     */
    public BasicDHCPDumpParserTest() throws Exception {
        DHCPDumpParser = new BasicDHCPDumpParser();
    }

    @Test
    public void testSimpleMessage() throws ParseException, IOException, java.text.ParseException {
        String rawMessage = "TIME: 2017-01-16 17:39:36.581|INTERFACE: eth2|OP:1 BOOTPREQUEST|CIADDR: 172.20.75.77|YIADDR: 0.0.0.0|SIADDR: 0.0.0.0|GIADDR: 172.20.75.8|CHADDR: fc:f8:ae:e8:ef:db:00:00:00:00:00:00:00:00:00:00|OPTION:  53   1 DHCP message type: 8 |DHCPINFORM|OPTION:  61   7 Client-identifier: 01:fc:f8:ae:e8:ef:db|OPTION:  12   5 Host name: A1244|OPTION:  60   8 Vendor class identifier: MSFT 5.0|OPTION:  55  13 Parameter Request List:   1 (Subnet mask)|| 15 (Domainname)||  3 (Routers)||  6 (DNS server)|| 44 (NetBIOS name server)|| 46 (NetBIOS node type)|| 47 (NetBIOS scope)|| 31 (Perform router discovery)|| 33 (Static route)||121 (Classless Static Route)||249 (MSFT - Classless route)|| 43 (Vendor specific info)||252 (MSFT - WinSock Proxy Auto Detect)|||IP: 100.100.100.177 > 172.20.1.11 | b8:ca:3a:67:95:8a > 0:50:56:84:68:43";

        JSONObject metronJSONObject = DHCPDumpParser.parse(rawMessage.getBytes()).get(0);

        Assert.assertEquals(metronJSONObject.get("original_string").toString(), rawMessage);
        Assert.assertEquals(new SimpleDateFormat(BasicDHCPDumpParser.TIMESTAMP_PATTERN).parse(metronJSONObject.get("time").toString()).getTime(), (long) metronJSONObject.get("timestamp"));

        Assert.assertEquals(metronJSONObject.get("ip_src_addr").toString(), "100.100.100.177");
        Assert.assertEquals(metronJSONObject.get("ip_dst_addr").toString(), "172.20.1.11");

        Assert.assertEquals(metronJSONObject.get("time").toString(), "2017-01-16 17:39:36.581");
    }


}
