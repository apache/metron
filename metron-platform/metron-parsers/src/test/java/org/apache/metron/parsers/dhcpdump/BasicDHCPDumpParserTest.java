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
    public void testBootprequestMessage() throws ParseException, IOException, java.text.ParseException {
      String rawMessage = "TIME: 2017-04-21 15:43:17.673|IP: 172.20.3.28 (84:2b:2b:86:62:43) > 255.255.255.255 (ff:ff:ff:ff:ff:ff)|OP: 1 (BOOTPREQUEST)|HTYPE: 1 (Ethernet)|HLEN: 6|HOPS: 0|XID: 23850fd5|SECS: 0|FLAGS: 0|CIADDR: 172.20.3.28|YIADDR: 0.0.0.0|SIADDR: 0.0.0.0|GIADDR: 0.0.0.0|CHADDR: 84:2b:2b:86:62:43:00:00:00:00:00:00:00:00:00:00|SNAME: .|FNAME: .|OPTION:  53 (  1) DHCP message type         8 (DHCPINFORM)|OPTION:  61 (  7) Client-identifier         01:84:2b:2b:86:62:43|OPTION:  12 (  5) Host name                 Q0619|OPTION:  60 (  8) Vendor class identifier   MSFT 5.0|OPTION:  55 ( 13) Parameter Request List    1 (Subnet mask),15 (Domainname),3 (Routers),6 (DNS server),44 (NetBIOS name server),46 (NetBIOS node type),47 (NetBIOS scope),31 (Perform router discovery),33 (Static route),121 (Classless Static Route),249 (MSFT - Classless route),43 (Vendor specific info),252 (MSFT - WinSock Proxy Auto Detect)";

      JSONObject metronJSONObject = DHCPDumpParser.parse(rawMessage.getBytes()).get(0);

      Assert.assertEquals(metronJSONObject.get("original_string").toString(), rawMessage);
      Assert.assertEquals(new SimpleDateFormat(BasicDHCPDumpParser.TIMESTAMP_PATTERN).parse(metronJSONObject.get("time").toString()).getTime(), (long) metronJSONObject.get("timestamp"));

      Assert.assertEquals(metronJSONObject.get("ip_src_addr").toString(), "172.20.3.28");
      Assert.assertEquals(metronJSONObject.get("ip_dst_addr").toString(), "255.255.255.255");
      Assert.assertEquals(metronJSONObject.get("mac_src_addr").toString(), "84:2b:2b:86:62:43");
      Assert.assertEquals(metronJSONObject.get("mac_dst_addr").toString(), "ff:ff:ff:ff:ff:ff");

      Assert.assertEquals(metronJSONObject.get("host_name").toString(), "Q0619");
      Assert.assertEquals(metronJSONObject.get("client_identifier").toString(), "01:84:2b:2b:86:62:43");

      Assert.assertEquals(metronJSONObject.get("op").toString(), "1 (BOOTPREQUEST)");

      Assert.assertEquals(metronJSONObject.get("ciaddr").toString(), "172.20.3.28");
      Assert.assertEquals(metronJSONObject.get("yiaddr").toString(), "0.0.0.0");
      Assert.assertEquals(metronJSONObject.get("siaddr").toString(), "0.0.0.0");
      Assert.assertEquals(metronJSONObject.get("giaddr").toString(), "0.0.0.0");

      Assert.assertEquals(metronJSONObject.get("time").toString(), "2017-04-21 15:43:17.673");
    }

    @Test
    public void testBootpreplyMessage() throws ParseException, IOException, java.text.ParseException {
      String rawMessage = "TIME: 2017-04-21 15:44:35.456|IP: 172.20.3.7 (20:fd:f1:71:6d:f1) > 255.255.255.255 (ff:ff:ff:ff:ff:ff)|OP: 2 (BOOTPREPLY)|HTYPE: 1 (Ethernet)|HLEN: 6|HOPS: 0|XID: cbc8934d|SECS: 0|FLAGS: 7f80|CIADDR: 172.20.3.56|YIADDR: 0.0.0.0|SIADDR: 0.0.0.0|GIADDR: 172.20.3.7|CHADDR: b8:ac:6f:c4:0f:88:00:00:00:00:00:00:00:00:00:00|SNAME: .|FNAME: .|OPTION:  53 (  1) DHCP message type         5 (DHCPACK)|OPTION:  54 (  4) Server identifier         172.20.1.10|OPTION:   1 (  4) Subnet mask               255.255.255.0|OPTION:  43 (  5) Vendor specific info      dc034e4150       ..NAP|OPTION:  15 ( 14) Domainname                oa.onsight.nl|OPTION:   3 (  4) Routers                   172.20.3.1|OPTION:   6 (  8) DNS server                172.20.1.10,172.20.1.11|OPTION:  44 (  8) NetBIOS name server       172.20.1.10,172.20.1.11|OPTION:  46 (  1) NetBIOS node type         8 (H-node)";

      JSONObject metronJSONObject = DHCPDumpParser.parse(rawMessage.getBytes()).get(0);

      Assert.assertEquals(metronJSONObject.get("original_string").toString(), rawMessage);
      Assert.assertEquals(new SimpleDateFormat(BasicDHCPDumpParser.TIMESTAMP_PATTERN).parse(metronJSONObject.get("time").toString()).getTime(), (long) metronJSONObject.get("timestamp"));

      Assert.assertEquals(metronJSONObject.get("ip_src_addr").toString(), "172.20.3.7");
      Assert.assertEquals(metronJSONObject.get("ip_dst_addr").toString(), "255.255.255.255");
      Assert.assertEquals(metronJSONObject.get("mac_src_addr").toString(), "20:fd:f1:71:6d:f1");
      Assert.assertEquals(metronJSONObject.get("mac_dst_addr").toString(), "ff:ff:ff:ff:ff:ff");

      Assert.assertEquals(metronJSONObject.get("op").toString(), "2 (BOOTPREPLY)");

      Assert.assertEquals(metronJSONObject.get("ciaddr").toString(), "172.20.3.56");
      Assert.assertEquals(metronJSONObject.get("yiaddr").toString(), "0.0.0.0");
      Assert.assertEquals(metronJSONObject.get("siaddr").toString(), "0.0.0.0");
      Assert.assertEquals(metronJSONObject.get("giaddr").toString(), "172.20.3.7");

      Assert.assertEquals(metronJSONObject.get("time").toString(), "2017-04-21 15:44:35.456");
    }

}
