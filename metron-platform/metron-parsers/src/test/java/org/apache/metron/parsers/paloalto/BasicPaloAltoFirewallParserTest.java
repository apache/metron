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

import static org.junit.Assert.assertEquals;

import org.apache.metron.parsers.AbstractParserConfigTest;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

public class BasicPaloAltoFirewallParserTest extends AbstractParserConfigTest {

  @Before
  public void setUp() throws Exception {
    parser = new BasicPaloAltoFirewallParser();
  }

  public static final String THREAT_60 = "<11>Jan  5 05:38:59 PAN1.exampleCustomer.com 1,2015/01/05 05:38:58,0006C110285,THREAT,vulnerability,1,2015/01/05 05:38:58,10.0.0.115,216.0.10.198,0.0.0.0,0.0.0.0,EX-Allow,example\\user.name,,web-browsing,vsys1,internal,external,ethernet1/2,ethernet1/1,LOG-Default,2015/01/05 05:38:58,12031,1,54180,80,0,0,0x80004000,tcp,reset-both,\"ad.aspx?f=300x250&id=12;tile=1;ord=67AF705D60B1119C0F18BEA336F9\",HTTP: IIS Denial Of Service Attempt(40019),any,high,client-to-server,347368099,0x0,10.0.0.0-10.255.255.255,US,0,,1200568889751109656,,";
  @SuppressWarnings("unchecked")
  @Test
  public void testParseThreat60() throws ParseException {
    JSONObject actual = parser.parse(THREAT_60.getBytes()).get(0);

    JSONObject expected = new JSONObject();
    expected.put(BasicPaloAltoFirewallParser.Action, "reset-both");
    expected.put(BasicPaloAltoFirewallParser.ActionFlags, "0x0");
    expected.put(BasicPaloAltoFirewallParser.Application, "web-browsing");
    expected.put(BasicPaloAltoFirewallParser.Category, "any");
    expected.put(BasicPaloAltoFirewallParser.WFCloud, "");
    expected.put(BasicPaloAltoFirewallParser.ConfigVersion, "1");
    expected.put(BasicPaloAltoFirewallParser.ContentType, "");
    expected.put(BasicPaloAltoFirewallParser.Direction, "client-to-server");
    expected.put(BasicPaloAltoFirewallParser.DestinationLocation, "US");
    expected.put(BasicPaloAltoFirewallParser.DestinationUser, "");
    expected.put(BasicPaloAltoFirewallParser.WFFileDigest, "");
    expected.put(BasicPaloAltoFirewallParser.Flags, "0x80004000");
    expected.put(BasicPaloAltoFirewallParser.SourceZone, "internal");
    expected.put(BasicPaloAltoFirewallParser.InboundInterface, "ethernet1/2");
    expected.put(BasicPaloAltoFirewallParser.DestinationAddress, "216.0.10.198");
    expected.put(BasicPaloAltoFirewallParser.DestinationPort, "80");
    expected.put(BasicPaloAltoFirewallParser.SourceAddress, "10.0.0.115");
    expected.put(BasicPaloAltoFirewallParser.SourcePort, "54180");
    expected.put(BasicPaloAltoFirewallParser.LogAction, "LOG-Default");
    expected.put(BasicPaloAltoFirewallParser.NATDestinationPort, "0");
    expected.put(BasicPaloAltoFirewallParser.NATDestinationIP, "0.0.0.0");
    expected.put(BasicPaloAltoFirewallParser.NATSourcePort, "0");
    expected.put(BasicPaloAltoFirewallParser.NATSourceIP, "0.0.0.0");
    expected.put("original_string", THREAT_60);
    expected.put(BasicPaloAltoFirewallParser.OutboundInterface, "ethernet1/1");
    expected.put(BasicPaloAltoFirewallParser.PaloAltoDomain, "<11>Jan  5 05:38:59 PAN1.exampleCustomer.com 1");
    expected.put(BasicPaloAltoFirewallParser.ParserVersion, 60);
    expected.put(BasicPaloAltoFirewallParser.PCAPID, "1200568889751109656");
    expected.put(BasicPaloAltoFirewallParser.IPProtocol, "tcp");
    expected.put(BasicPaloAltoFirewallParser.ReceiveTime, "2015/01/05 05:38:58");
    expected.put(BasicPaloAltoFirewallParser.RepeatCount, "1");
    expected.put(BasicPaloAltoFirewallParser.Rule, "EX-Allow");
    expected.put(BasicPaloAltoFirewallParser.Seqno, "347368099");
    expected.put(BasicPaloAltoFirewallParser.SerialNum, "0006C110285");
    expected.put(BasicPaloAltoFirewallParser.SessionID, "12031");
    expected.put(BasicPaloAltoFirewallParser.Severity, "high");
    expected.put(BasicPaloAltoFirewallParser.SourceLocation, "10.0.0.0-10.255.255.255");
    expected.put(BasicPaloAltoFirewallParser.SourceUser, "example\\user.name");
    expected.put(BasicPaloAltoFirewallParser.StartTime, "2015/01/05 05:38:58");
    expected.put(BasicPaloAltoFirewallParser.ThreatContentType, "vulnerability");
    expected.put(BasicPaloAltoFirewallParser.ThreatID, "HTTP: IIS Denial Of Service Attempt(40019)");
    expected.put(BasicPaloAltoFirewallParser.GenerateTime, "2015/01/05 05:38:58");
    expected.put("timestamp", actual.get("timestamp"));
    expected.put(BasicPaloAltoFirewallParser.DestinationZone, "external");
    expected.put(BasicPaloAltoFirewallParser.Type, "THREAT");
    expected.put(BasicPaloAltoFirewallParser.URL, "\"ad.aspx?f=300x250&id=12;tile=1;ord=67AF705D60B1119C0F18BEA336F9\"");
    expected.put(BasicPaloAltoFirewallParser.VirtualSystem, "vsys1");
    assertEquals(expected, actual);
  }

  public static final String TRAFFIC_60 = "<14>Jan  5 12:51:34 PAN1.exampleCustomer.com 1,2015/01/05 12:51:33,0011C103117,TRAFFIC,end,1,2015/01/05 12:51:33,10.0.0.39,10.1.0.163,0.0.0.0,0.0.0.0,EX-Allow,,example\\\\user.name,ms-ds-smb,vsys1,v_external,v_internal,ethernet1/2,ethernet1/1,LOG-Default,2015/01/05 12:51:33,33760927,1,52688,445,0,0,0x401a,tcp,allow,2229,1287,942,10,2015/01/05 12:51:01,30,any,0,17754932062,0x0,10.0.0.0-10.255.255.255,10.0.0.0-10.255.255.255,0,6,";
  @SuppressWarnings("unchecked")
  @Test
  public void testParseTraffic60() throws ParseException {
    JSONObject actual = parser.parse(TRAFFIC_60.getBytes()).get(0);

    JSONObject expected = new JSONObject();
    expected.put(BasicPaloAltoFirewallParser.Action, "allow");
    expected.put(BasicPaloAltoFirewallParser.ActionFlags, "0x0");
    expected.put(BasicPaloAltoFirewallParser.Application, "ms-ds-smb");
    expected.put(BasicPaloAltoFirewallParser.Bytes, "2229");
    expected.put(BasicPaloAltoFirewallParser.BytesReceived, "942");
    expected.put(BasicPaloAltoFirewallParser.BytesSent, "1287");
    expected.put(BasicPaloAltoFirewallParser.Category, "any");
    expected.put(BasicPaloAltoFirewallParser.ConfigVersion, "1");
    expected.put(BasicPaloAltoFirewallParser.DestinationLocation, "10.0.0.0-10.255.255.255");
    expected.put(BasicPaloAltoFirewallParser.DestinationUser, "example\\\\user.name");
    expected.put(BasicPaloAltoFirewallParser.ElapsedTimeInSec, "30");
    expected.put(BasicPaloAltoFirewallParser.Flags, "0x401a");
    expected.put(BasicPaloAltoFirewallParser.SourceZone, "v_external");
    expected.put(BasicPaloAltoFirewallParser.InboundInterface, "ethernet1/2");
    expected.put(BasicPaloAltoFirewallParser.DestinationAddress, "10.1.0.163");
    expected.put(BasicPaloAltoFirewallParser.DestinationPort, "445");
    expected.put(BasicPaloAltoFirewallParser.SourceAddress, "10.0.0.39");
    expected.put(BasicPaloAltoFirewallParser.SourcePort, "52688");
    expected.put(BasicPaloAltoFirewallParser.LogAction, "LOG-Default");
    expected.put(BasicPaloAltoFirewallParser.NATDestinationPort, "0");
    expected.put(BasicPaloAltoFirewallParser.NATDestinationIP, "0.0.0.0");
    expected.put(BasicPaloAltoFirewallParser.NATSourcePort, "0");
    expected.put(BasicPaloAltoFirewallParser.NATSourceIP, "0.0.0.0");
    expected.put("original_string", TRAFFIC_60);
    expected.put(BasicPaloAltoFirewallParser.OutboundInterface, "ethernet1/1");
    expected.put(BasicPaloAltoFirewallParser.Packets, "10");
    expected.put(BasicPaloAltoFirewallParser.PaloAltoDomain, "<14>Jan  5 12:51:34 PAN1.exampleCustomer.com 1");
    expected.put(BasicPaloAltoFirewallParser.ParserVersion, 60);
    expected.put(BasicPaloAltoFirewallParser.PktsReceived, "");
    expected.put(BasicPaloAltoFirewallParser.PktsSent, "6");
    expected.put(BasicPaloAltoFirewallParser.IPProtocol, "tcp");
    expected.put(BasicPaloAltoFirewallParser.ReceiveTime, "2015/01/05 12:51:33");
    expected.put(BasicPaloAltoFirewallParser.RepeatCount, "1");
    expected.put(BasicPaloAltoFirewallParser.Rule, "EX-Allow");
    expected.put(BasicPaloAltoFirewallParser.Seqno, "17754932062");
    expected.put(BasicPaloAltoFirewallParser.SerialNum, "0011C103117");
    expected.put(BasicPaloAltoFirewallParser.SessionID, "33760927");
    expected.put(BasicPaloAltoFirewallParser.SourceLocation, "10.0.0.0-10.255.255.255");
    expected.put(BasicPaloAltoFirewallParser.SourceUser, "");
    expected.put(BasicPaloAltoFirewallParser.StartTime, "2015/01/05 12:51:01");
    expected.put(BasicPaloAltoFirewallParser.ThreatContentType, "end");
    expected.put(BasicPaloAltoFirewallParser.GenerateTime, "2015/01/05 12:51:33");
    expected.put("timestamp", actual.get("timestamp"));
    expected.put(BasicPaloAltoFirewallParser.DestinationZone, "v_internal");
    expected.put(BasicPaloAltoFirewallParser.Type, "TRAFFIC");
    expected.put(BasicPaloAltoFirewallParser.VirtualSystem, "vsys1");
    assertEquals(expected, actual);
  }
}
