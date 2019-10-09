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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.metron.parsers.AbstractParserConfigTest;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

public class BasicPaloAltoFirewallParserTest extends AbstractParserConfigTest {

  @Before
  public void setUp() throws Exception {
    parser = new BasicPaloAltoFirewallParser();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testParseSystem61() throws ParseException {
    final String SYSTEM_61 = "1,2017/08/11 12:37:58,008900008659,SYSTEM,general,1,2017/08/11 11:37:58,vsys1,eventId_test,object_test,Futureuse1_test,futureuse2_test,management,high,Description_test,1354,0x0";

    JSONObject actual = parser.parse(SYSTEM_61.getBytes(StandardCharsets.UTF_8)).get(0);

    JSONObject expected = new JSONObject();
    expected.put(BasicPaloAltoFirewallParser.PaloAltoDomain, "1");
    expected.put(BasicPaloAltoFirewallParser.ReceiveTime, "2017/08/11 12:37:58");
    expected.put(BasicPaloAltoFirewallParser.SerialNum, "008900008659");
    expected.put(BasicPaloAltoFirewallParser.Type, "SYSTEM");
    expected.put(BasicPaloAltoFirewallParser.ThreatContentType, "general");
    expected.put(BasicPaloAltoFirewallParser.ConfigVersion, "1");
    expected.put(BasicPaloAltoFirewallParser.GenerateTime, "2017/08/11 11:37:58");
    expected.put(BasicPaloAltoFirewallParser.VirtualSystem, "vsys1");
    expected.put(BasicPaloAltoFirewallParser.EventId, "eventId_test");
    expected.put(BasicPaloAltoFirewallParser.Object, "object_test");
    expected.put(BasicPaloAltoFirewallParser.Module, "management");
    expected.put(BasicPaloAltoFirewallParser.Severity, "high");
    expected.put(BasicPaloAltoFirewallParser.Description, "Description_test");
    expected.put(BasicPaloAltoFirewallParser.Seqno, "1354");
    expected.put(BasicPaloAltoFirewallParser.ActionFlags, "0x0");
    expected.put(BasicPaloAltoFirewallParser.ParserVersion, 61);
    expected.put("original_string", SYSTEM_61);
    expected.put("timestamp", actual.get("timestamp"));

    assertEquals(expected, actual);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testParseSystem80() throws ParseException {
    final String SYSTEM_80 = "1,2017/08/11 12:37:58,008900008659,SYSTEM,general,1,2017/08/11 11:37:58,vsys1,eventId_test,object_test,Futureuse1_test,futureuse2_test,management,high,Description_test,1354,0x0,12,34,45,0,virSys1,dev-something200-01";

    JSONObject actual = parser.parse(SYSTEM_80.getBytes(StandardCharsets.UTF_8)).get(0);

    JSONObject expected = new JSONObject();
    expected.put(BasicPaloAltoFirewallParser.PaloAltoDomain, "1");
    expected.put(BasicPaloAltoFirewallParser.ReceiveTime, "2017/08/11 12:37:58");
    expected.put(BasicPaloAltoFirewallParser.SerialNum, "008900008659");
    expected.put(BasicPaloAltoFirewallParser.Type, "SYSTEM");
    expected.put(BasicPaloAltoFirewallParser.ThreatContentType, "general");
    expected.put(BasicPaloAltoFirewallParser.ConfigVersion, "1");
    expected.put(BasicPaloAltoFirewallParser.GenerateTime, "2017/08/11 11:37:58");
    expected.put(BasicPaloAltoFirewallParser.VirtualSystem, "vsys1");
    expected.put(BasicPaloAltoFirewallParser.EventId, "eventId_test");
    expected.put(BasicPaloAltoFirewallParser.Object, "object_test");
    expected.put(BasicPaloAltoFirewallParser.Module, "management");
    expected.put(BasicPaloAltoFirewallParser.Severity, "high");
    expected.put(BasicPaloAltoFirewallParser.Description, "Description_test");
    expected.put(BasicPaloAltoFirewallParser.Seqno, "1354");
    expected.put(BasicPaloAltoFirewallParser.ActionFlags, "0x0");
    expected.put(BasicPaloAltoFirewallParser.DGH1, "12");
    expected.put(BasicPaloAltoFirewallParser.DGH2, "34");
    expected.put(BasicPaloAltoFirewallParser.DGH3, "45");
    expected.put(BasicPaloAltoFirewallParser.DGH4, "0");
    expected.put(BasicPaloAltoFirewallParser.VSYSName, "virSys1");
    expected.put(BasicPaloAltoFirewallParser.DeviceName, "dev-something200-01");

    expected.put(BasicPaloAltoFirewallParser.ParserVersion, 80);
    expected.put("original_string", SYSTEM_80);
    expected.put("timestamp", actual.get("timestamp"));

    assertEquals(expected, actual);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testParseConfig61NoCustomFields() throws ParseException {
    final String CONFIG_61_customFields = "1,2017/08/11 12:37:58,008900008659,CONFIG,0,1,2017/08/11 11:37:58,192.168.14.162,vsys1,edit,admin,Web,Succeeded, config shared log-settings config,1354,0x0";

    JSONObject actual = parser.parse(CONFIG_61_customFields.getBytes(StandardCharsets.UTF_8)).get(0);

    JSONObject expected = new JSONObject();
    expected.put(BasicPaloAltoFirewallParser.PaloAltoDomain, "1");
    expected.put(BasicPaloAltoFirewallParser.ReceiveTime, "2017/08/11 12:37:58");
    expected.put(BasicPaloAltoFirewallParser.SerialNum, "008900008659");
    expected.put(BasicPaloAltoFirewallParser.Type, "CONFIG");
    expected.put(BasicPaloAltoFirewallParser.ThreatContentType, "0");
    expected.put(BasicPaloAltoFirewallParser.ConfigVersion, "1");
    expected.put(BasicPaloAltoFirewallParser.GenerateTime, "2017/08/11 11:37:58");

    expected.put(BasicPaloAltoFirewallParser.HOST, "192.168.14.162");
    expected.put(BasicPaloAltoFirewallParser.VirtualSystem, "vsys1");
    expected.put(BasicPaloAltoFirewallParser.Command, "edit");
    expected.put(BasicPaloAltoFirewallParser.Admin, "admin");
    expected.put(BasicPaloAltoFirewallParser.Client, "Web");
    expected.put(BasicPaloAltoFirewallParser.Result, "Succeeded");
    expected.put(BasicPaloAltoFirewallParser.ConfigurationPath, "config shared log-settings config");
    expected.put(BasicPaloAltoFirewallParser.Seqno, "1354");
    expected.put(BasicPaloAltoFirewallParser.ActionFlags, "0x0");

    expected.put(BasicPaloAltoFirewallParser.ParserVersion, 61);
    expected.put("original_string", CONFIG_61_customFields);
    expected.put("timestamp", actual.get("timestamp"));

    assertEquals(expected, actual);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testParseConfig61CustomFields() throws ParseException {
    final String CONFIG_61_noCustomFields = "1,2017/08/11 12:37:58,008900008659,CONFIG,0,1,2017/08/11 11:37:58,192.168.14.162,vsys1,edit,admin,Web,Succeeded, config shared log-settings config,1354,0x0,/FatherNode/KidNode/GrandsonNode1,/FatherNode/KidNode/GrandsonNode2";

    JSONObject actual = parser.parse(CONFIG_61_noCustomFields.getBytes(StandardCharsets.UTF_8)).get(0);

    JSONObject expected = new JSONObject();
    expected.put(BasicPaloAltoFirewallParser.PaloAltoDomain, "1");
    expected.put(BasicPaloAltoFirewallParser.ReceiveTime, "2017/08/11 12:37:58");
    expected.put(BasicPaloAltoFirewallParser.SerialNum, "008900008659");
    expected.put(BasicPaloAltoFirewallParser.Type, "CONFIG");
    expected.put(BasicPaloAltoFirewallParser.ThreatContentType, "0");
    expected.put(BasicPaloAltoFirewallParser.ConfigVersion, "1");
    expected.put(BasicPaloAltoFirewallParser.GenerateTime, "2017/08/11 11:37:58");

    expected.put(BasicPaloAltoFirewallParser.HOST, "192.168.14.162");
    expected.put(BasicPaloAltoFirewallParser.VirtualSystem, "vsys1");
    expected.put(BasicPaloAltoFirewallParser.Command, "edit");
    expected.put(BasicPaloAltoFirewallParser.Admin, "admin");
    expected.put(BasicPaloAltoFirewallParser.Client, "Web");
    expected.put(BasicPaloAltoFirewallParser.Result, "Succeeded");
    expected.put(BasicPaloAltoFirewallParser.ConfigurationPath, "config shared log-settings config");
    expected.put(BasicPaloAltoFirewallParser.Seqno, "1354");
    expected.put(BasicPaloAltoFirewallParser.ActionFlags, "0x0");
    expected.put(BasicPaloAltoFirewallParser.BeforeChangeDetail, "/FatherNode/KidNode/GrandsonNode1");
    expected.put(BasicPaloAltoFirewallParser.AfterChangeDetail, "/FatherNode/KidNode/GrandsonNode2");

    expected.put(BasicPaloAltoFirewallParser.ParserVersion, 61);
    expected.put("original_string", CONFIG_61_noCustomFields);
    expected.put("timestamp", actual.get("timestamp"));

    assertEquals(expected, actual);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testParseConfig70And80NoCustomFields() throws ParseException {
    final String CONFIG_70_80_noCustomFields = "1,2017/08/11 12:37:58,008900008659,CONFIG,0,1,2017/08/11 11:37:58,192.168.14.162,vsys1,edit,admin,Web,Succeeded, config shared log-settings config,1354,0x0,12,34,45,0,virSys1,dev-something200-01";

    JSONObject actual = parser.parse(CONFIG_70_80_noCustomFields.getBytes(StandardCharsets.UTF_8)).get(0);

    JSONObject expected = new JSONObject();
    expected.put(BasicPaloAltoFirewallParser.PaloAltoDomain, "1");
    expected.put(BasicPaloAltoFirewallParser.ReceiveTime, "2017/08/11 12:37:58");
    expected.put(BasicPaloAltoFirewallParser.SerialNum, "008900008659");
    expected.put(BasicPaloAltoFirewallParser.Type, "CONFIG");
    expected.put(BasicPaloAltoFirewallParser.ThreatContentType, "0");
    expected.put(BasicPaloAltoFirewallParser.ConfigVersion, "1");
    expected.put(BasicPaloAltoFirewallParser.GenerateTime, "2017/08/11 11:37:58");

    expected.put(BasicPaloAltoFirewallParser.HOST, "192.168.14.162");
    expected.put(BasicPaloAltoFirewallParser.VirtualSystem, "vsys1");
    expected.put(BasicPaloAltoFirewallParser.Command, "edit");
    expected.put(BasicPaloAltoFirewallParser.Admin, "admin");
    expected.put(BasicPaloAltoFirewallParser.Client, "Web");
    expected.put(BasicPaloAltoFirewallParser.Result, "Succeeded");
    expected.put(BasicPaloAltoFirewallParser.ConfigurationPath, "config shared log-settings config");
    expected.put(BasicPaloAltoFirewallParser.Seqno, "1354");
    expected.put(BasicPaloAltoFirewallParser.ActionFlags, "0x0");
    expected.put(BasicPaloAltoFirewallParser.DGH1, "12");
    expected.put(BasicPaloAltoFirewallParser.DGH2, "34");
    expected.put(BasicPaloAltoFirewallParser.DGH3, "45");
    expected.put(BasicPaloAltoFirewallParser.DGH4, "0");
    expected.put(BasicPaloAltoFirewallParser.VSYSName, "virSys1");
    expected.put(BasicPaloAltoFirewallParser.DeviceName, "dev-something200-01");

    expected.put(BasicPaloAltoFirewallParser.ParserVersion, 80);
    expected.put("original_string", CONFIG_70_80_noCustomFields);
    expected.put("timestamp", actual.get("timestamp"));

    assertEquals(expected, actual);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testParseConfig70And80CustomFields() throws ParseException {
    final String CONFIG_70_80_customFields = "1,2017/08/11 12:37:58,008900008659,CONFIG,0,1,2017/08/11 11:37:58,192.168.14.162,vsys1,edit,admin,Web,Succeeded,config shared log-settings config,/FatherNode/KidNode/GrandsonNode1,/FatherNode/KidNode/GrandsonNode2,1354,0x0,12,34,45,0,virSys1,dev-something200-01";

    JSONObject actual = parser.parse(CONFIG_70_80_customFields.getBytes(StandardCharsets.UTF_8)).get(0);

    JSONObject expected = new JSONObject();
    expected.put(BasicPaloAltoFirewallParser.PaloAltoDomain, "1");
    expected.put(BasicPaloAltoFirewallParser.ReceiveTime, "2017/08/11 12:37:58");
    expected.put(BasicPaloAltoFirewallParser.SerialNum, "008900008659");
    expected.put(BasicPaloAltoFirewallParser.Type, "CONFIG");
    expected.put(BasicPaloAltoFirewallParser.ThreatContentType, "0");
    expected.put(BasicPaloAltoFirewallParser.ConfigVersion, "1");
    expected.put(BasicPaloAltoFirewallParser.GenerateTime, "2017/08/11 11:37:58");

    expected.put(BasicPaloAltoFirewallParser.HOST, "192.168.14.162");
    expected.put(BasicPaloAltoFirewallParser.VirtualSystem, "vsys1");
    expected.put(BasicPaloAltoFirewallParser.Command, "edit");
    expected.put(BasicPaloAltoFirewallParser.Admin, "admin");
    expected.put(BasicPaloAltoFirewallParser.Client, "Web");
    expected.put(BasicPaloAltoFirewallParser.Result, "Succeeded");
    expected.put(BasicPaloAltoFirewallParser.ConfigurationPath, "config shared log-settings config");
    expected.put(BasicPaloAltoFirewallParser.BeforeChangeDetail, "/FatherNode/KidNode/GrandsonNode1");
    expected.put(BasicPaloAltoFirewallParser.AfterChangeDetail, "/FatherNode/KidNode/GrandsonNode2");
    expected.put(BasicPaloAltoFirewallParser.Seqno, "1354");
    expected.put(BasicPaloAltoFirewallParser.ActionFlags, "0x0");
    expected.put(BasicPaloAltoFirewallParser.DGH1, "12");
    expected.put(BasicPaloAltoFirewallParser.DGH2, "34");
    expected.put(BasicPaloAltoFirewallParser.DGH3, "45");
    expected.put(BasicPaloAltoFirewallParser.DGH4, "0");
    expected.put(BasicPaloAltoFirewallParser.VSYSName, "virSys1");
    expected.put(BasicPaloAltoFirewallParser.DeviceName, "dev-something200-01");

    expected.put(BasicPaloAltoFirewallParser.ParserVersion, 80);
    expected.put("original_string", CONFIG_70_80_customFields);
    expected.put("timestamp", actual.get("timestamp"));

    assertEquals(expected, actual);
  }

  public static final String THREAT_60 = "1,2015/01/05 05:38:58,0006C110285,THREAT,vulnerability,1,2015/01/05 05:38:58,10.0.0.115,216.0.10.198,0.0.0.0,0.0.0.0,EX-Allow,example\\user.name,,web-browsing,vsys1,internal,external,ethernet1/2,ethernet1/1,LOG-Default,2015/01/05 05:38:58,12031,1,54180,80,0,0,0x80004000,tcp,reset-both,\"ad.aspx?f=300x250&id=12;tile=1;ord=67AF705D60B1119C0F18BEA336F9\",HTTP: IIS Denial Of Service Attempt(40019),any,high,client-to-server,347368099,0x0,10.0.0.0-10.255.255.255,US,0,,1200568889751109656,,";

  @SuppressWarnings("unchecked")
  @Test
  public void testParseThreat60() throws ParseException {
    JSONObject actual = parser.parse(THREAT_60.getBytes(StandardCharsets.UTF_8)).get(0);

    JSONObject expected = new JSONObject();
    expected.put(BasicPaloAltoFirewallParser.Action, "reset-both");
    expected.put(BasicPaloAltoFirewallParser.ActionFlags, "0x0");
    expected.put(BasicPaloAltoFirewallParser.Application, "web-browsing");
    expected.put(BasicPaloAltoFirewallParser.Category, "any");

    expected.put(BasicPaloAltoFirewallParser.ConfigVersion, "1");
    expected.put(BasicPaloAltoFirewallParser.Direction, "client-to-server");
    expected.put(BasicPaloAltoFirewallParser.DestinationLocation, "US");
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
    expected.put(BasicPaloAltoFirewallParser.PaloAltoDomain, "1");
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
    expected.put(BasicPaloAltoFirewallParser.URL, "ad.aspx?f=300x250&id=12;tile=1;ord=67AF705D60B1119C0F18BEA336F9");
    expected.put(BasicPaloAltoFirewallParser.VirtualSystem, "vsys1");
    assertEquals(expected, actual);
  }

  public static final String TRAFFIC_60 = "1,2015/01/05 12:51:33,0011C103117,TRAFFIC,end,1,2015/01/05 12:51:33,10.0.0.39,10.1.0.163,0.0.0.0,0.0.0.0,EX-Allow,,example\\\\user.name,ms-ds-smb,vsys1,v_external,v_internal,ethernet1/2,ethernet1/1,LOG-Default,2015/01/05 12:51:33,33760927,1,52688,445,0,0,0x401a,tcp,allow,2229,1287,942,10,2015/01/05 12:51:01,30,any,0,17754932062,0x0,10.0.0.0-10.255.255.255,10.0.0.0-10.255.255.255,0,6,";

  @SuppressWarnings("unchecked")
  @Test
  public void testParseTraffic60() throws ParseException {
    JSONObject actual = parser.parse(TRAFFIC_60.getBytes(StandardCharsets.UTF_8)).get(0);

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
    expected.put(BasicPaloAltoFirewallParser.PaloAltoDomain, "1");
    expected.put(BasicPaloAltoFirewallParser.ParserVersion, 60);
    expected.put(BasicPaloAltoFirewallParser.PktsSent, "6");
    expected.put(BasicPaloAltoFirewallParser.IPProtocol, "tcp");
    expected.put(BasicPaloAltoFirewallParser.ReceiveTime, "2015/01/05 12:51:33");
    expected.put(BasicPaloAltoFirewallParser.RepeatCount, "1");
    expected.put(BasicPaloAltoFirewallParser.Rule, "EX-Allow");
    expected.put(BasicPaloAltoFirewallParser.Seqno, "17754932062");
    expected.put(BasicPaloAltoFirewallParser.SerialNum, "0011C103117");
    expected.put(BasicPaloAltoFirewallParser.SessionID, "33760927");
    expected.put(BasicPaloAltoFirewallParser.SourceLocation, "10.0.0.0-10.255.255.255");
    expected.put(BasicPaloAltoFirewallParser.StartTime, "2015/01/05 12:51:01");
    expected.put(BasicPaloAltoFirewallParser.ThreatContentType, "end");
    expected.put(BasicPaloAltoFirewallParser.GenerateTime, "2015/01/05 12:51:33");
    expected.put("timestamp", actual.get("timestamp"));
    expected.put(BasicPaloAltoFirewallParser.DestinationZone, "v_internal");
    expected.put(BasicPaloAltoFirewallParser.Type, "TRAFFIC");
    expected.put(BasicPaloAltoFirewallParser.VirtualSystem, "vsys1");
    assertEquals(expected, actual);
  }

  public static final String THREAT_70 = "1,2017/05/24 09:53:10,001801000001,THREAT,virus,0,2017/05/24 09:53:10,217.1.2.3,10.1.8.7,217.1.2.3,214.123.1.2,WLAN-Internet,,user,web-browsing,vsys1,Untrust,wifi_zone,ethernet1/1,vlan.1,Std-Log-Forward,2017/05/24 09:53:10,49567,1,80,51787,80,25025,0x400000,tcp,reset-both,\"abcdef310.exe\",Virus/Win32.WGeneric.lumeo(2457399),computer-and-internet-info,medium,server-to-client,329423829,0x0,DE,10.0.0.0-10.255.255.255,0,,0,,,1,,,\"\",\"\",,,,0,19,0,0,0,,PAN1,";

  @SuppressWarnings("unchecked")
  @Test
  public void testParseThreat70() throws ParseException {
    JSONObject actual = parser.parse(THREAT_70.getBytes(StandardCharsets.UTF_8)).get(0);

    JSONObject expected = new JSONObject();
    expected.put(BasicPaloAltoFirewallParser.Action, "reset-both");
    expected.put(BasicPaloAltoFirewallParser.ActionFlags, "0x0");
    expected.put(BasicPaloAltoFirewallParser.Application, "web-browsing");
    expected.put(BasicPaloAltoFirewallParser.Category, "computer-and-internet-info");
    expected.put(BasicPaloAltoFirewallParser.ConfigVersion, "0");
    expected.put(BasicPaloAltoFirewallParser.Direction, "server-to-client");
    expected.put(BasicPaloAltoFirewallParser.DestinationLocation, "10.0.0.0-10.255.255.255");
    expected.put(BasicPaloAltoFirewallParser.DestinationUser, "user");
    expected.put(BasicPaloAltoFirewallParser.Flags, "0x400000");
    expected.put(BasicPaloAltoFirewallParser.SourceZone, "Untrust");
    expected.put(BasicPaloAltoFirewallParser.InboundInterface, "ethernet1/1");
    expected.put(BasicPaloAltoFirewallParser.DestinationAddress, "10.1.8.7");
    expected.put(BasicPaloAltoFirewallParser.DestinationPort, "51787");
    expected.put(BasicPaloAltoFirewallParser.SourceAddress, "217.1.2.3");
    expected.put(BasicPaloAltoFirewallParser.SourcePort, "80");
    expected.put(BasicPaloAltoFirewallParser.LogAction, "Std-Log-Forward");
    expected.put(BasicPaloAltoFirewallParser.NATDestinationPort, "25025");
    expected.put(BasicPaloAltoFirewallParser.NATDestinationIP, "214.123.1.2");
    expected.put(BasicPaloAltoFirewallParser.NATSourcePort, "80");
    expected.put(BasicPaloAltoFirewallParser.NATSourceIP, "217.1.2.3");
    expected.put("original_string", THREAT_70);
    expected.put(BasicPaloAltoFirewallParser.OutboundInterface, "vlan.1");
    expected.put(BasicPaloAltoFirewallParser.PaloAltoDomain, "1");
    expected.put(BasicPaloAltoFirewallParser.ParserVersion, 70);
    expected.put(BasicPaloAltoFirewallParser.PCAPID, "0");
    expected.put(BasicPaloAltoFirewallParser.IPProtocol, "tcp");
    expected.put(BasicPaloAltoFirewallParser.ReceiveTime, "2017/05/24 09:53:10");
    expected.put(BasicPaloAltoFirewallParser.RepeatCount, "1");
    expected.put(BasicPaloAltoFirewallParser.Rule, "WLAN-Internet");
    expected.put(BasicPaloAltoFirewallParser.Seqno, "329423829");
    expected.put(BasicPaloAltoFirewallParser.SerialNum, "001801000001");
    expected.put(BasicPaloAltoFirewallParser.SessionID, "49567");
    expected.put(BasicPaloAltoFirewallParser.Severity, "medium");
    expected.put(BasicPaloAltoFirewallParser.SourceLocation, "DE");
    expected.put(BasicPaloAltoFirewallParser.StartTime, "2017/05/24 09:53:10");
    expected.put(BasicPaloAltoFirewallParser.ThreatContentType, "virus");
    expected.put(BasicPaloAltoFirewallParser.ThreatID, "Virus/Win32.WGeneric.lumeo(2457399)");
    expected.put(BasicPaloAltoFirewallParser.GenerateTime, "2017/05/24 09:53:10");
    expected.put("timestamp", actual.get("timestamp"));
    expected.put(BasicPaloAltoFirewallParser.DestinationZone, "wifi_zone");
    expected.put(BasicPaloAltoFirewallParser.Type, "THREAT");
    expected.put(BasicPaloAltoFirewallParser.URL, "abcdef310.exe");
    expected.put(BasicPaloAltoFirewallParser.VirtualSystem, "vsys1");
    expected.put(BasicPaloAltoFirewallParser.URLIndex, "1");
    expected.put(BasicPaloAltoFirewallParser.WFReportID, "0");
    expected.put(BasicPaloAltoFirewallParser.DGH1, "19");
    expected.put(BasicPaloAltoFirewallParser.DGH2, "0");
    expected.put(BasicPaloAltoFirewallParser.DGH3, "0");
    expected.put(BasicPaloAltoFirewallParser.DGH4, "0");
    expected.put(BasicPaloAltoFirewallParser.DeviceName, "PAN1");
    assertEquals(expected, actual);
  }

  public static final String TRAFFIC_70 = "1,2017/05/25 21:38:13,001606000003,TRAFFIC,drop,1,2017/05/25 21:38:13,10.2.1.8,192.168.1.10,0.0.0.0,0.0.0.0,DropLog,,,not-applicable,vsys1,intern,VPN,vlan.1,,Std-Log-Forward,2017/05/25 21:38:13,0,1,137,137,0,0,0x0,udp,deny,114,114,0,1,2017/05/25 21:38:12,0,any,0,9953744,0x0,192.168.0.0-192.168.255.255,DE,0,1,0,policy-deny,19,0,0,0,,PAN1,from-policy";

  @SuppressWarnings("unchecked")
  @Test
  public void testParseTraffic70() throws ParseException {
    JSONObject actual = parser.parse(TRAFFIC_70.getBytes(StandardCharsets.UTF_8)).get(0);

    JSONObject expected = new JSONObject();
    expected.put(BasicPaloAltoFirewallParser.Action, "deny");
    expected.put(BasicPaloAltoFirewallParser.ActionFlags, "0x0");
    expected.put(BasicPaloAltoFirewallParser.ActionSource, "from-policy");
    expected.put(BasicPaloAltoFirewallParser.Application, "not-applicable");
    expected.put(BasicPaloAltoFirewallParser.Bytes, "114");
    expected.put(BasicPaloAltoFirewallParser.BytesReceived, "0");
    expected.put(BasicPaloAltoFirewallParser.BytesSent, "114");
    expected.put(BasicPaloAltoFirewallParser.Category, "any");
    expected.put(BasicPaloAltoFirewallParser.ConfigVersion, "1");
    expected.put(BasicPaloAltoFirewallParser.DestinationLocation, "DE");
    expected.put(BasicPaloAltoFirewallParser.ElapsedTimeInSec, "0");
    expected.put(BasicPaloAltoFirewallParser.Flags, "0x0");
    expected.put(BasicPaloAltoFirewallParser.SourceZone, "intern");
    expected.put(BasicPaloAltoFirewallParser.InboundInterface, "vlan.1");
    expected.put(BasicPaloAltoFirewallParser.DestinationAddress, "192.168.1.10");
    expected.put(BasicPaloAltoFirewallParser.DestinationPort, "137");
    expected.put(BasicPaloAltoFirewallParser.SourceAddress, "10.2.1.8");
    expected.put(BasicPaloAltoFirewallParser.SourcePort, "137");
    expected.put(BasicPaloAltoFirewallParser.LogAction, "Std-Log-Forward");
    expected.put(BasicPaloAltoFirewallParser.NATDestinationPort, "0");
    expected.put(BasicPaloAltoFirewallParser.NATDestinationIP, "0.0.0.0");
    expected.put(BasicPaloAltoFirewallParser.NATSourcePort, "0");
    expected.put(BasicPaloAltoFirewallParser.NATSourceIP, "0.0.0.0");
    expected.put("original_string", TRAFFIC_70);
    expected.put(BasicPaloAltoFirewallParser.Packets, "1");
    expected.put(BasicPaloAltoFirewallParser.PaloAltoDomain, "1");
    expected.put(BasicPaloAltoFirewallParser.ParserVersion, 70);
    expected.put(BasicPaloAltoFirewallParser.PktsReceived, "0");
    expected.put(BasicPaloAltoFirewallParser.PktsSent, "1");
    expected.put(BasicPaloAltoFirewallParser.IPProtocol, "udp");
    expected.put(BasicPaloAltoFirewallParser.ReceiveTime, "2017/05/25 21:38:13");
    expected.put(BasicPaloAltoFirewallParser.RepeatCount, "1");
    expected.put(BasicPaloAltoFirewallParser.Rule, "DropLog");
    expected.put(BasicPaloAltoFirewallParser.Seqno, "9953744");
    expected.put(BasicPaloAltoFirewallParser.SerialNum, "001606000003");
    expected.put(BasicPaloAltoFirewallParser.EndReason, "policy-deny");
    expected.put(BasicPaloAltoFirewallParser.SessionID, "0");
    expected.put(BasicPaloAltoFirewallParser.SourceLocation, "192.168.0.0-192.168.255.255");
    expected.put(BasicPaloAltoFirewallParser.StartTime, "2017/05/25 21:38:12");
    expected.put(BasicPaloAltoFirewallParser.ThreatContentType, "drop");
    expected.put(BasicPaloAltoFirewallParser.GenerateTime, "2017/05/25 21:38:13");
    expected.put("timestamp", actual.get("timestamp"));
    expected.put(BasicPaloAltoFirewallParser.DestinationZone, "VPN");
    expected.put(BasicPaloAltoFirewallParser.Type, "TRAFFIC");
    expected.put(BasicPaloAltoFirewallParser.VirtualSystem, "vsys1");
    expected.put(BasicPaloAltoFirewallParser.DGH1, "19");
    expected.put(BasicPaloAltoFirewallParser.DGH2, "0");
    expected.put(BasicPaloAltoFirewallParser.DGH3, "0");
    expected.put(BasicPaloAltoFirewallParser.DGH4, "0");
    expected.put(BasicPaloAltoFirewallParser.DeviceName, "PAN1");
    assertEquals(expected, actual);
  }

  public static final String TRAFFIC_71 = "1,2017/05/31 23:59:57,0006C000005,TRAFFIC,drop,0,2017/05/31 23:59:57,185.94.1.1,201.1.4.5,0.0.0.0,0.0.0.0,DropLog,,,not-applicable,vsys1,untrust,untrust,vlan.1,,Standard-Syslog,2017/05/31 23:59:57,0,1,59836,123,0,0,0x0,udp,deny,60,60,0,1,2017/05/31 23:59:57,0,any,0,3433072193,0x0,RU,DE,0,1,0,policy-deny,16,11,0,0,,PAN1,from-policy";

  @SuppressWarnings("unchecked")
  @Test
  public void testParseTraffic71() throws ParseException {
    JSONObject actual = parser.parse(TRAFFIC_71.getBytes(StandardCharsets.UTF_8)).get(0);

    JSONObject expected = new JSONObject();
    expected.put(BasicPaloAltoFirewallParser.Action, "deny");
    expected.put(BasicPaloAltoFirewallParser.ActionFlags, "0x0");
    expected.put(BasicPaloAltoFirewallParser.ActionSource, "from-policy");
    expected.put(BasicPaloAltoFirewallParser.Application, "not-applicable");
    expected.put(BasicPaloAltoFirewallParser.Bytes, "60");
    expected.put(BasicPaloAltoFirewallParser.BytesReceived, "0");
    expected.put(BasicPaloAltoFirewallParser.BytesSent, "60");
    expected.put(BasicPaloAltoFirewallParser.Category, "any");
    expected.put(BasicPaloAltoFirewallParser.ConfigVersion, "0");
    expected.put(BasicPaloAltoFirewallParser.DestinationLocation, "DE");
    expected.put(BasicPaloAltoFirewallParser.ElapsedTimeInSec, "0");
    expected.put(BasicPaloAltoFirewallParser.Flags, "0x0");
    expected.put(BasicPaloAltoFirewallParser.SourceZone, "untrust");
    expected.put(BasicPaloAltoFirewallParser.InboundInterface, "vlan.1");
    expected.put(BasicPaloAltoFirewallParser.DestinationAddress, "201.1.4.5");
    expected.put(BasicPaloAltoFirewallParser.DestinationPort, "123");
    expected.put(BasicPaloAltoFirewallParser.SourceAddress, "185.94.1.1");
    expected.put(BasicPaloAltoFirewallParser.SourcePort, "59836");
    expected.put(BasicPaloAltoFirewallParser.LogAction, "Standard-Syslog");
    expected.put(BasicPaloAltoFirewallParser.NATDestinationPort, "0");
    expected.put(BasicPaloAltoFirewallParser.NATDestinationIP, "0.0.0.0");
    expected.put(BasicPaloAltoFirewallParser.NATSourcePort, "0");
    expected.put(BasicPaloAltoFirewallParser.NATSourceIP, "0.0.0.0");
    expected.put("original_string", TRAFFIC_71);
    expected.put(BasicPaloAltoFirewallParser.Packets, "1");
    expected.put(BasicPaloAltoFirewallParser.PaloAltoDomain, "1");
    expected.put(BasicPaloAltoFirewallParser.ParserVersion, 70);
    expected.put(BasicPaloAltoFirewallParser.PktsReceived, "0");
    expected.put(BasicPaloAltoFirewallParser.PktsSent, "1");
    expected.put(BasicPaloAltoFirewallParser.IPProtocol, "udp");
    expected.put(BasicPaloAltoFirewallParser.ReceiveTime, "2017/05/31 23:59:57");
    expected.put(BasicPaloAltoFirewallParser.RepeatCount, "1");
    expected.put(BasicPaloAltoFirewallParser.Rule, "DropLog");
    expected.put(BasicPaloAltoFirewallParser.Seqno, "3433072193");
    expected.put(BasicPaloAltoFirewallParser.SerialNum, "0006C000005");
    expected.put(BasicPaloAltoFirewallParser.EndReason, "policy-deny");
    expected.put(BasicPaloAltoFirewallParser.SessionID, "0");
    expected.put(BasicPaloAltoFirewallParser.SourceLocation, "RU");
    expected.put(BasicPaloAltoFirewallParser.StartTime, "2017/05/31 23:59:57");
    expected.put(BasicPaloAltoFirewallParser.ThreatContentType, "drop");
    expected.put(BasicPaloAltoFirewallParser.GenerateTime, "2017/05/31 23:59:57");
    expected.put("timestamp", actual.get("timestamp"));
    expected.put(BasicPaloAltoFirewallParser.DestinationZone, "untrust");
    expected.put(BasicPaloAltoFirewallParser.Type, "TRAFFIC");
    expected.put(BasicPaloAltoFirewallParser.VirtualSystem, "vsys1");
    expected.put(BasicPaloAltoFirewallParser.DGH1, "16");
    expected.put(BasicPaloAltoFirewallParser.DGH2, "11");
    expected.put(BasicPaloAltoFirewallParser.DGH3, "0");
    expected.put(BasicPaloAltoFirewallParser.DGH4, "0");
    expected.put(BasicPaloAltoFirewallParser.DeviceName, "PAN1");
    assertEquals(expected, actual);
  }

  public static final String THREAT_71 = "1,2017/05/25 19:31:13,0006C000005,THREAT,url,0,2017/05/25 19:31:13,192.168.1.7,140.177.26.29,201.1.4.5,140.177.26.29,ms_out,,,ssl,vsys1,mgmt,untrust,vlan.199,vlan.1,Standard-Syslog,2017/05/25 19:31:13,50556,1,56059,443,14810,443,0x40b000,tcp,alert,\"settings-win.data.microsoft.com/\",(9999),computer-and-internet-info,informational,client-to-server,10030265,0x0,192.168.0.0-192.168.255.255,IE,0,,0,,,0,,,,,,,,0,16,11,0,0,,PAN1,";

  @SuppressWarnings("unchecked")
  @Test
  public void testParseThreat71() throws ParseException {
    JSONObject actual = parser.parse(THREAT_71.getBytes(StandardCharsets.UTF_8)).get(0);

    JSONObject expected = new JSONObject();
    expected.put(BasicPaloAltoFirewallParser.Action, "alert");
    expected.put(BasicPaloAltoFirewallParser.ActionFlags, "0x0");
    expected.put(BasicPaloAltoFirewallParser.Application, "ssl");
    expected.put(BasicPaloAltoFirewallParser.Category, "computer-and-internet-info");
    expected.put(BasicPaloAltoFirewallParser.ConfigVersion, "0");
    expected.put(BasicPaloAltoFirewallParser.Direction, "client-to-server");
    expected.put(BasicPaloAltoFirewallParser.DestinationLocation, "IE");
    expected.put(BasicPaloAltoFirewallParser.Flags, "0x40b000");
    expected.put(BasicPaloAltoFirewallParser.SourceZone, "mgmt");
    expected.put(BasicPaloAltoFirewallParser.InboundInterface, "vlan.199");
    expected.put(BasicPaloAltoFirewallParser.DestinationAddress, "140.177.26.29");
    expected.put(BasicPaloAltoFirewallParser.DestinationPort, "443");
    expected.put(BasicPaloAltoFirewallParser.SourceAddress, "192.168.1.7");
    expected.put(BasicPaloAltoFirewallParser.SourcePort, "56059");
    expected.put(BasicPaloAltoFirewallParser.LogAction, "Standard-Syslog");
    expected.put(BasicPaloAltoFirewallParser.NATDestinationPort, "443");
    expected.put(BasicPaloAltoFirewallParser.NATDestinationIP, "140.177.26.29");
    expected.put(BasicPaloAltoFirewallParser.NATSourcePort, "14810");
    expected.put(BasicPaloAltoFirewallParser.NATSourceIP, "201.1.4.5");
    expected.put("original_string", THREAT_71);
    expected.put(BasicPaloAltoFirewallParser.OutboundInterface, "vlan.1");
    expected.put(BasicPaloAltoFirewallParser.PaloAltoDomain, "1");
    expected.put(BasicPaloAltoFirewallParser.ParserVersion, 70);
    expected.put(BasicPaloAltoFirewallParser.PCAPID, "0");
    expected.put(BasicPaloAltoFirewallParser.IPProtocol, "tcp");
    expected.put(BasicPaloAltoFirewallParser.ReceiveTime, "2017/05/25 19:31:13");
    expected.put(BasicPaloAltoFirewallParser.RepeatCount, "1");
    expected.put(BasicPaloAltoFirewallParser.Rule, "ms_out");
    expected.put(BasicPaloAltoFirewallParser.Seqno, "10030265");
    expected.put(BasicPaloAltoFirewallParser.SerialNum, "0006C000005");
    expected.put(BasicPaloAltoFirewallParser.SessionID, "50556");
    expected.put(BasicPaloAltoFirewallParser.Severity, "informational");
    expected.put(BasicPaloAltoFirewallParser.SourceLocation, "192.168.0.0-192.168.255.255");
    expected.put(BasicPaloAltoFirewallParser.StartTime, "2017/05/25 19:31:13");
    expected.put(BasicPaloAltoFirewallParser.ThreatContentType, "url");
    expected.put(BasicPaloAltoFirewallParser.ThreatID, "(9999)");
    expected.put(BasicPaloAltoFirewallParser.GenerateTime, "2017/05/25 19:31:13");
    expected.put("timestamp", actual.get("timestamp"));
    expected.put(BasicPaloAltoFirewallParser.DestinationZone, "untrust");
    expected.put(BasicPaloAltoFirewallParser.Type, "THREAT");
    expected.put(BasicPaloAltoFirewallParser.URL, "settings-win.data.microsoft.com/");
    expected.put(BasicPaloAltoFirewallParser.VirtualSystem, "vsys1");
    expected.put(BasicPaloAltoFirewallParser.URLIndex, "0");
    expected.put(BasicPaloAltoFirewallParser.WFReportID, "0");
    expected.put(BasicPaloAltoFirewallParser.DGH1, "16");
    expected.put(BasicPaloAltoFirewallParser.DGH2, "11");
    expected.put(BasicPaloAltoFirewallParser.DGH3, "0");
    expected.put(BasicPaloAltoFirewallParser.DGH4, "0");
    expected.put(BasicPaloAltoFirewallParser.DeviceName, "PAN1");
    assertEquals(expected, actual);
  }

  public static final String THREAT_80 = "1,2018/02/01 21:29:03,001606000007,THREAT,vulnerability,1,2018/02/01 21:29:03,213.211.198.62,172.16.2.6,213.211.198.62,192.168.178.202,Outgoing,,,web-browsing,vsys1,internet,guest,ethernet1/1,ethernet1/2.2,test,2018/02/01 21:29:03,18720,1,80,53161,80,32812,0x402000,tcp,reset-server,\"www.eicar.org/download/eicar.com\",Eicar File Detected(39040),computer-and-internet-info,medium,server-to-client,27438839,0x0,Germany,172.16.0.0-172.31.255.255,0,,0,,,9,,,,,,,,0,0,0,0,0,,PAN1,,,,,0,,0,,N/A,code-execution,AppThreat-771-4450,0x0";

  @SuppressWarnings("unchecked")
  @Test
  public void testParseThreat80() throws ParseException {
    JSONObject actual = parser.parse(THREAT_80.getBytes(StandardCharsets.UTF_8)).get(0);

    JSONObject expected = new JSONObject();
    expected.put(BasicPaloAltoFirewallParser.Action, "reset-server");
    expected.put(BasicPaloAltoFirewallParser.ActionFlags, "0x0");
    expected.put(BasicPaloAltoFirewallParser.Application, "web-browsing");
    expected.put(BasicPaloAltoFirewallParser.Category, "computer-and-internet-info");
    expected.put(BasicPaloAltoFirewallParser.ConfigVersion, "1");
    expected.put(BasicPaloAltoFirewallParser.ContentVersion, "AppThreat-771-4450");
    expected.put(BasicPaloAltoFirewallParser.Direction, "server-to-client");
    expected.put(BasicPaloAltoFirewallParser.DestinationLocation, "172.16.0.0-172.31.255.255");
    expected.put(BasicPaloAltoFirewallParser.Flags, "0x402000");
    expected.put(BasicPaloAltoFirewallParser.SourceZone, "internet");
    expected.put(BasicPaloAltoFirewallParser.InboundInterface, "ethernet1/1");
    expected.put(BasicPaloAltoFirewallParser.DestinationAddress, "172.16.2.6");
    expected.put(BasicPaloAltoFirewallParser.DestinationPort, "53161");
    expected.put(BasicPaloAltoFirewallParser.SourceAddress, "213.211.198.62");
    expected.put(BasicPaloAltoFirewallParser.SourcePort, "80");
    expected.put(BasicPaloAltoFirewallParser.LogAction, "test");
    expected.put(BasicPaloAltoFirewallParser.NATDestinationPort, "32812");
    expected.put(BasicPaloAltoFirewallParser.NATDestinationIP, "192.168.178.202");
    expected.put(BasicPaloAltoFirewallParser.NATSourcePort, "80");
    expected.put(BasicPaloAltoFirewallParser.NATSourceIP, "213.211.198.62");
    expected.put("original_string", THREAT_80);
    expected.put(BasicPaloAltoFirewallParser.OutboundInterface, "ethernet1/2.2");
    expected.put(BasicPaloAltoFirewallParser.PaloAltoDomain, "1");
    expected.put(BasicPaloAltoFirewallParser.ParentSessionId, "0");
    expected.put(BasicPaloAltoFirewallParser.ParserVersion, 80);
    expected.put(BasicPaloAltoFirewallParser.PCAPID, "0");
    expected.put(BasicPaloAltoFirewallParser.IPProtocol, "tcp");
    expected.put(BasicPaloAltoFirewallParser.ReceiveTime, "2018/02/01 21:29:03");
    expected.put(BasicPaloAltoFirewallParser.RepeatCount, "1");
    expected.put(BasicPaloAltoFirewallParser.Rule, "Outgoing");
    expected.put(BasicPaloAltoFirewallParser.Seqno, "27438839");
    expected.put(BasicPaloAltoFirewallParser.SerialNum, "001606000007");
    expected.put(BasicPaloAltoFirewallParser.SessionID, "18720");
    expected.put(BasicPaloAltoFirewallParser.Severity, "medium");
    expected.put(BasicPaloAltoFirewallParser.SourceLocation, "Germany");
    expected.put(BasicPaloAltoFirewallParser.StartTime, "2018/02/01 21:29:03");
    expected.put(BasicPaloAltoFirewallParser.ThreatCategory, "code-execution");
    expected.put(BasicPaloAltoFirewallParser.ThreatContentType, "vulnerability");
    expected.put(BasicPaloAltoFirewallParser.ThreatID, "Eicar File Detected(39040)");
    expected.put(BasicPaloAltoFirewallParser.GenerateTime, "2018/02/01 21:29:03");
    expected.put("timestamp", actual.get("timestamp"));
    expected.put(BasicPaloAltoFirewallParser.DestinationZone, "guest");
    expected.put(BasicPaloAltoFirewallParser.TunnelId, "0");
    expected.put(BasicPaloAltoFirewallParser.TunnelType, "N/A");
    expected.put(BasicPaloAltoFirewallParser.Type, "THREAT");
    expected.put(BasicPaloAltoFirewallParser.URL, "www.eicar.org/download/eicar.com");
    expected.put(BasicPaloAltoFirewallParser.VirtualSystem, "vsys1");
    expected.put(BasicPaloAltoFirewallParser.URLIndex, "9");
    expected.put(BasicPaloAltoFirewallParser.WFReportID, "0");
    expected.put(BasicPaloAltoFirewallParser.DGH1, "0");
    expected.put(BasicPaloAltoFirewallParser.DGH2, "0");
    expected.put(BasicPaloAltoFirewallParser.DGH3, "0");
    expected.put(BasicPaloAltoFirewallParser.DGH4, "0");
    expected.put(BasicPaloAltoFirewallParser.DeviceName, "PAN1");
    assertEquals(expected, actual);
  }

  public static final String TRAFFIC_80 = "1,2018/02/01 21:24:11,001606000007,TRAFFIC,end,1,2018/02/01 21:24:11,172.16.2.31,134.19.6.22,192.168.18.2,134.19.6.22,Outgoing,,,ssl,vsys1,guest,internet,ethernet1/2.2,ethernet1/1,test,2018/02/01 21:24:11,19468,1,41537,443,12211,443,0x40001c,tcp,allow,7936,1731,6205,24,2018/02/01 21:00:42,1395,computer-and-internet-info,0,62977478,0x0,172.16.0.0-172.31.255.255,United States,0,14,10,tcp-rst-from-client,0,0,0,0,,PAN1,from-policy,,,0,,0,,N/A";

  @SuppressWarnings("unchecked")
  @Test
  public void testParseTraffic80() throws ParseException {
    JSONObject actual = parser.parse(TRAFFIC_80.getBytes(StandardCharsets.UTF_8)).get(0);

    JSONObject expected = new JSONObject();
    expected.put(BasicPaloAltoFirewallParser.Action, "allow");
    expected.put(BasicPaloAltoFirewallParser.ActionFlags, "0x0");
    expected.put(BasicPaloAltoFirewallParser.ActionSource, "from-policy");
    expected.put(BasicPaloAltoFirewallParser.Application, "ssl");
    expected.put(BasicPaloAltoFirewallParser.Bytes, "7936");
    expected.put(BasicPaloAltoFirewallParser.BytesReceived, "6205");
    expected.put(BasicPaloAltoFirewallParser.BytesSent, "1731");
    expected.put(BasicPaloAltoFirewallParser.Category, "computer-and-internet-info");
    expected.put(BasicPaloAltoFirewallParser.ConfigVersion, "1");
    expected.put(BasicPaloAltoFirewallParser.DestinationLocation, "United States");
    expected.put(BasicPaloAltoFirewallParser.ElapsedTimeInSec, "1395");
    expected.put(BasicPaloAltoFirewallParser.Flags, "0x40001c");
    expected.put(BasicPaloAltoFirewallParser.SourceZone, "guest");
    expected.put(BasicPaloAltoFirewallParser.InboundInterface, "ethernet1/2.2");
    expected.put(BasicPaloAltoFirewallParser.DestinationAddress, "134.19.6.22");
    expected.put(BasicPaloAltoFirewallParser.DestinationPort, "443");
    expected.put(BasicPaloAltoFirewallParser.SourceAddress, "172.16.2.31");
    expected.put(BasicPaloAltoFirewallParser.SourcePort, "41537");
    expected.put(BasicPaloAltoFirewallParser.LogAction, "test");
    expected.put(BasicPaloAltoFirewallParser.NATDestinationPort, "443");
    expected.put(BasicPaloAltoFirewallParser.NATDestinationIP, "134.19.6.22");
    expected.put(BasicPaloAltoFirewallParser.NATSourcePort, "12211");
    expected.put(BasicPaloAltoFirewallParser.NATSourceIP, "192.168.18.2");
    expected.put("original_string", TRAFFIC_80);
    expected.put(BasicPaloAltoFirewallParser.OutboundInterface, "ethernet1/1");
    expected.put(BasicPaloAltoFirewallParser.Packets, "24");
    expected.put(BasicPaloAltoFirewallParser.PaloAltoDomain, "1");
    expected.put(BasicPaloAltoFirewallParser.ParentSessionId, "0");
    expected.put(BasicPaloAltoFirewallParser.ParserVersion, 80);
    expected.put(BasicPaloAltoFirewallParser.PktsReceived, "10");
    expected.put(BasicPaloAltoFirewallParser.PktsSent, "14");
    expected.put(BasicPaloAltoFirewallParser.IPProtocol, "tcp");
    expected.put(BasicPaloAltoFirewallParser.ReceiveTime, "2018/02/01 21:24:11");
    expected.put(BasicPaloAltoFirewallParser.RepeatCount, "1");
    expected.put(BasicPaloAltoFirewallParser.Rule, "Outgoing");
    expected.put(BasicPaloAltoFirewallParser.Seqno, "62977478");
    expected.put(BasicPaloAltoFirewallParser.SerialNum, "001606000007");
    expected.put(BasicPaloAltoFirewallParser.EndReason, "tcp-rst-from-client");
    expected.put(BasicPaloAltoFirewallParser.SessionID, "19468");
    expected.put(BasicPaloAltoFirewallParser.SourceLocation, "172.16.0.0-172.31.255.255");
    expected.put(BasicPaloAltoFirewallParser.StartTime, "2018/02/01 21:00:42");
    expected.put(BasicPaloAltoFirewallParser.ThreatContentType, "end");
    expected.put(BasicPaloAltoFirewallParser.GenerateTime, "2018/02/01 21:24:11");
    expected.put("timestamp", actual.get("timestamp"));
    expected.put(BasicPaloAltoFirewallParser.DestinationZone, "internet");
    expected.put(BasicPaloAltoFirewallParser.TunnelId, "0");
    expected.put(BasicPaloAltoFirewallParser.TunnelType, "N/A");
    expected.put(BasicPaloAltoFirewallParser.Type, "TRAFFIC");
    expected.put(BasicPaloAltoFirewallParser.VirtualSystem, "vsys1");
    expected.put(BasicPaloAltoFirewallParser.DGH1, "0");
    expected.put(BasicPaloAltoFirewallParser.DGH2, "0");
    expected.put(BasicPaloAltoFirewallParser.DGH3, "0");
    expected.put(BasicPaloAltoFirewallParser.DGH4, "0");
    expected.put(BasicPaloAltoFirewallParser.DeviceName, "PAN1");
    assertEquals(expected, actual);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testParseInvalidLogTypeMessage() throws ParseException {
    final String unsupportedLogTypeMessage = "1,2017/08/11 12:37:58,008900008659,INVALIDlogType,0,1,2017/08/11 11:37:58,192.168.14.162,vsys1,edit,admin,Web,Succeeded, config shared log-settings config,1354,0x0";
    List<JSONObject> actual = parser.parse(unsupportedLogTypeMessage.getBytes(
        StandardCharsets.UTF_8));

    assertNull(actual);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testParseInvalidVersionMessage() throws ParseException {
    final String invalidLengthMessage = "1,2017/08/11 12:37:58,008900008659,CONFIG,0,1,2017/08/11 11:37:58,192.168.14.162,vsys1,edit,admin,Web,Succeeded, config shared log-settings config";

    JSONObject actual = parser.parse(invalidLengthMessage.getBytes(StandardCharsets.UTF_8)).get(0);
    String expectedParserVersion = actual.get(BasicPaloAltoFirewallParser.ParserVersion).toString();
    assertEquals(expectedParserVersion, "0");
  }

  @Test
  public void getsReadCharsetFromConfig() {
    Map<String, Object> config = new HashMap<>();
    config.put(MessageParser.READ_CHARSET, StandardCharsets.UTF_16.toString());
    parser.configure(config);
    assertThat(parser.getReadCharset(), equalTo(StandardCharsets.UTF_16));
  }

  @Test
  public void getsReadCharsetFromDefault() {
    Map<String, Object> config = new HashMap<>();
    parser.configure(config);
    assertThat(parser.getReadCharset(), equalTo(StandardCharsets.UTF_8));
  }
}
