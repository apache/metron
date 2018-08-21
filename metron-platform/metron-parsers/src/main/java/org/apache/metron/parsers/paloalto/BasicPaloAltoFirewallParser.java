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


import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class BasicPaloAltoFirewallParser extends BasicParser {

  private static boolean empty_attribute(final String s) {
    return s == null || s.trim().isEmpty() || s.equals("\"\"");
  }

  private static String unquoted_attribute(String s) {
    s = s.trim();
    if (s.startsWith("\"") && s.endsWith("\""))
      return s.substring(1, s.length() - 1);
    return s;
  }

  private static final Logger _LOG = LoggerFactory.getLogger(BasicPaloAltoFirewallParser.class);

  private static final long serialVersionUID = 3147090149725343999L;

  private static final String LogTypeConfig = "CONFIG";
  private static final String LogTypeSystem = "SYSTEM";
  private static final String LogTypeThreat = "THREAT";
  private static final String LogTypeTraffic = "TRAFFIC";

  public static final String PaloAltoDomain = "palo_alto_domain";
  public static final String ReceiveTime = "receive_time";
  public static final String SerialNum = "serial";
  public static final String Type = "type";
  public static final String ThreatContentType = "subtype";
  public static final String ConfigVersion = "config_version";
  public static final String GenerateTime = "time_generated";
  public static final String SourceAddress = "ip_src_addr"; // Palo Alto name: "src"
  public static final String DestinationAddress = "ip_dst_addr"; // Palo Alto name: "dst"
  public static final String NATSourceIP = "natsrc";
  public static final String NATDestinationIP = "natdst";
  public static final String Rule = "rule";
  public static final String SourceUser = "srcuser";
  public static final String DestinationUser = "dstuser";
  public static final String Application = "app";
  public static final String VirtualSystem = "vsys";
  public static final String SourceZone = "from";
  public static final String DestinationZone = "to";
  public static final String InboundInterface = "inbound_if";
  public static final String OutboundInterface = "outbound_if";
  public static final String LogAction = "log_action";
  public static final String TimeLogged = "start";
  public static final String SessionID = "sessionid";
  public static final String RepeatCount = "repeatcnt";
  public static final String SourcePort = "ip_src_port"; // Palo Alto name: "sport"
  public static final String DestinationPort = "ip_dst_port"; // Palo Alto name: "dport"
  public static final String NATSourcePort = "natsport";
  public static final String NATDestinationPort = "natdport";
  public static final String Flags = "flags";
  public static final String IPProtocol = "protocol"; // Palo Alto name: "proto"
  public static final String Action = "action";
  public static final String Seqno = "seqno";
  public static final String ActionFlags = "actionflags";
  public static final String Category = "category";
  public static final String DGH1 = "dg_hier_level_1";
  public static final String DGH2 = "dg_hier_level_2";
  public static final String DGH3 = "dg_hier_level_3";
  public static final String DGH4 = "dg_hier_level_4";
  public static final String VSYSName = "vsys_name";
  public static final String DeviceName = "device_name";
  public static final String ActionSource = "action_source";
  public static final String ParserVersion = "parser_version";
  public static final String Tokens = "tokens_seen";

  public static final String SourceVmUuid = "source_vm_uuid";
  public static final String DestinationVmUuid = "destination_vm_uuid";
  public static final String TunnelId = "tunnel_id";
  public static final String MonitorTag = "monitor_tag";
  public static final String ParentSessionId = "parent_session_id";
  public static final String ParentSessionStartTime = "parent_session_start_time";
  public static final String TunnelType = "tunnel_type";

  //System
  public static final String EventId = "event_id";
  public static final String Object = "object";
  public static final String Module = "module";
  public static final String Description = "description";

  //Config
  public static final String Command = "command";
  public static final String Admin = "admin";
  public static final String Client = "client";
  public static final String Result = "result";
  public static final String ConfigurationPath = "configuration_path";
  public static final String BeforeChangeDetail = "before_change_detail";
  public static final String AfterChangeDetail = "after_change_detail";

  //Threat
  public static final String URL = "url";
  public static final String HOST = "host";
  public static final String ThreatID = "threatid";
  public static final String Severity = "severity";
  public static final String Direction = "direction";
  public static final String SourceLocation = "srcloc";
  public static final String DestinationLocation = "dstloc";
  public static final String ContentType = "contenttype";
  public static final String PCAPID = "pcap_id";
  public static final String WFFileDigest = "filedigest";
  public static final String WFCloud = "cloud";
  public static final String UserAgent = "user_agent";
  public static final String WFFileType = "filetype";
  public static final String XForwardedFor = "xff";
  public static final String Referer = "referer";
  public static final String WFSender = "sender";
  public static final String WFSubject = "subject";
  public static final String WFRecipient = "recipient";
  public static final String WFReportID = "reportid";
  public static final String URLIndex = "url_idx";
  public static final String HTTPMethod = "http_method";
  public static final String ThreatCategory = "threat_category";
  public static final String ContentVersion = "content_version";


  //Traffic
  public static final String Bytes = "bytes";
  public static final String BytesSent = "bytes_sent";
  public static final String BytesReceived = "bytes_received";
  public static final String Packets = "packets";
  public static final String StartTime = "start";
  public static final String ElapsedTimeInSec = "elapsed";
  public static final String PktsSent = "pkts_sent";
  public static final String PktsReceived = "pkts_received";
  public static final String EndReason = "session_end_reason";

  @Override
  public void configure(Map<String, Object> parserConfig) {

  }

  @Override
  public void init() {

  }

  @Override
  @SuppressWarnings({"unchecked", "unused"})
  public List<JSONObject> parse(byte[] msg) {

    JSONObject outputMessage = new JSONObject();
    String toParse = "";
    List<JSONObject> messages = new ArrayList<>();
    try {

      toParse = new String(msg, "UTF-8");
      _LOG.debug("Received message: {}", toParse);
      parseMessage(toParse, outputMessage);
      long timestamp = System.currentTimeMillis();
      outputMessage.put("timestamp", System.currentTimeMillis());
      outputMessage.put("original_string", toParse);
      messages.add(outputMessage);
      return messages;
    } catch (Exception e) {
      e.printStackTrace();
      _LOG.error("Failed to parse: {}", toParse);
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private void parseMessage(String message, JSONObject outputMessage) {

    String[] tokens = Iterables.toArray(Splitter.on(Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")).split(message), String.class);

    int parser_version = 0;

    String type = tokens[3].trim();

    //validate log types
    if (!type.equals(LogTypeConfig) &&
        !type.equals(LogTypeThreat) &&
        !type.equals(LogTypeTraffic) &&
        !type.equals(LogTypeSystem)) {
      throw new UnsupportedOperationException("Unsupported log type.");
    }

    //populate common objects
    if (!empty_attribute(tokens[0])) outputMessage.put(PaloAltoDomain, tokens[0].trim());
    if (!empty_attribute(tokens[1])) outputMessage.put(ReceiveTime, tokens[1].trim());
    if (!empty_attribute(tokens[2])) outputMessage.put(SerialNum, tokens[2].trim());
    outputMessage.put(Type, type);
    if (!empty_attribute(tokens[4])) outputMessage.put(ThreatContentType, unquoted_attribute(tokens[4]));
    if (!empty_attribute(tokens[5])) outputMessage.put(ConfigVersion, tokens[5].trim());
    if (!empty_attribute(tokens[6])) outputMessage.put(GenerateTime, tokens[6].trim());

    if (LogTypeConfig.equals(type.toUpperCase())) {
      // There are two fields in custom logs only and they are not in the default format.
      // But we need to parse them if they exist
      if (tokens.length == 16 || tokens.length == 18) parser_version = 61;
      else if (tokens.length == 22 || tokens.length == 24) parser_version = 80;

      if (parser_version >= 61) {
        if (!empty_attribute(tokens[7])) outputMessage.put(HOST, tokens[7].trim());
        if (!empty_attribute(tokens[8])) outputMessage.put(VirtualSystem, tokens[8].trim());
        if (!empty_attribute(tokens[9])) outputMessage.put(Command, tokens[9].trim());
        if (!empty_attribute(tokens[10])) outputMessage.put(Admin, tokens[10].trim());
        if (!empty_attribute(tokens[11])) outputMessage.put(Client, unquoted_attribute(tokens[11]));
        if (!empty_attribute(tokens[12])) outputMessage.put(Result, unquoted_attribute(tokens[12]));
        if (!empty_attribute(tokens[13])) outputMessage.put(ConfigurationPath, unquoted_attribute(tokens[13]));
      }

      if (parser_version == 61) {
        if (!empty_attribute(tokens[14])) outputMessage.put(Seqno, unquoted_attribute(tokens[14]));
        if (!empty_attribute(tokens[15])) outputMessage.put(ActionFlags, unquoted_attribute(tokens[15]));
        if (tokens.length == 18) {
          if (!empty_attribute(tokens[16]))
            outputMessage.put(BeforeChangeDetail, unquoted_attribute(tokens[16]));
          if (!empty_attribute(tokens[17]))
            outputMessage.put(AfterChangeDetail, unquoted_attribute(tokens[17]));
        }
      }

      if (parser_version >= 70) {
        int custom_fields_offset = 0;
        if (tokens.length == 24) {
          if (!empty_attribute(tokens[14])) {
            outputMessage.put(BeforeChangeDetail, unquoted_attribute(tokens[14 + custom_fields_offset]));
          }
          if (!empty_attribute(tokens[15])) {
            outputMessage.put(AfterChangeDetail, unquoted_attribute(tokens[15 + custom_fields_offset]));
          }
          custom_fields_offset = 2;
        }
        if (!empty_attribute(tokens[14 + custom_fields_offset])) {
          outputMessage.put(Seqno, unquoted_attribute(tokens[14 + custom_fields_offset]));
        }
        if (!empty_attribute(tokens[15 + custom_fields_offset])) {
          outputMessage.put(ActionFlags, unquoted_attribute(tokens[15 + custom_fields_offset]));
        }
        if (!empty_attribute(tokens[16 + custom_fields_offset])) {
          outputMessage.put(DGH1, unquoted_attribute(tokens[16 + custom_fields_offset]));
        }
        if (!empty_attribute(tokens[17 + custom_fields_offset])) {
          outputMessage.put(DGH2, unquoted_attribute(tokens[17 + custom_fields_offset]));
        }
        if (!empty_attribute(tokens[18 + custom_fields_offset])) {
          outputMessage.put(DGH3, unquoted_attribute(tokens[18 + custom_fields_offset]));
        }
        if (!empty_attribute(tokens[19 + custom_fields_offset])) {
          outputMessage.put(DGH4, unquoted_attribute(tokens[19 + custom_fields_offset]));
        }
        if (!empty_attribute(tokens[20 + custom_fields_offset])) {
          outputMessage.put(VSYSName, unquoted_attribute(tokens[20 + custom_fields_offset]));
        }
        if (!empty_attribute(tokens[21 + custom_fields_offset])) {
          outputMessage.put(DeviceName, unquoted_attribute(tokens[21 + custom_fields_offset]));
        }
      }
    } else if (LogTypeSystem.equals(type.toUpperCase())) {
      if (tokens.length == 17) parser_version = 61;
      else if (tokens.length == 23) parser_version = 80;

      if (parser_version >= 61) {
        if (!empty_attribute(tokens[7])) outputMessage.put(VirtualSystem, tokens[7].trim());
        if (!empty_attribute(tokens[8])) outputMessage.put(EventId, tokens[8].trim());
        if (!empty_attribute(tokens[9])) outputMessage.put(Object, tokens[9].trim());

        if (!empty_attribute(tokens[12])) outputMessage.put(Module, tokens[12].trim());
        if (!empty_attribute(tokens[13])) outputMessage.put(Severity, unquoted_attribute(tokens[13]));
        if (!empty_attribute(tokens[14])) outputMessage.put(Description, unquoted_attribute(tokens[14]));
        if (!empty_attribute(tokens[15])) outputMessage.put(Seqno, unquoted_attribute(tokens[15]));
        if (!empty_attribute(tokens[16])) outputMessage.put(ActionFlags, unquoted_attribute(tokens[16]));
      }

      if (parser_version == 80) {
        if (!empty_attribute(tokens[17])) outputMessage.put(DGH1, tokens[17].trim());
        if (!empty_attribute(tokens[18])) outputMessage.put(DGH2, tokens[18].trim());
        if (!empty_attribute(tokens[19])) outputMessage.put(DGH3, tokens[19].trim());
        if (!empty_attribute(tokens[20])) outputMessage.put(DGH4, tokens[20].trim());
        if (!empty_attribute(tokens[21])) outputMessage.put(VSYSName, unquoted_attribute(tokens[21]));
        if (!empty_attribute(tokens[22])) outputMessage.put(DeviceName, unquoted_attribute(tokens[22]));
      }
    } else if (LogTypeThreat.equals(type.toUpperCase()) ||
               LogTypeTraffic.equals(type.toUpperCase())) {
      if (!empty_attribute(tokens[7])) outputMessage.put(SourceAddress, tokens[7].trim());
      if (!empty_attribute(tokens[8])) outputMessage.put(DestinationAddress, tokens[8].trim());
      if (!empty_attribute(tokens[9])) outputMessage.put(NATSourceIP, tokens[9].trim());
      if (!empty_attribute(tokens[10])) outputMessage.put(NATDestinationIP, tokens[10].trim());
      if (!empty_attribute(tokens[11])) outputMessage.put(Rule, unquoted_attribute(tokens[11]));
      if (!empty_attribute(tokens[12])) outputMessage.put(SourceUser, unquoted_attribute(tokens[12]));
      if (!empty_attribute(tokens[13])) outputMessage.put(DestinationUser, unquoted_attribute(tokens[13]));
      if (!empty_attribute(tokens[14])) outputMessage.put(Application, unquoted_attribute(tokens[14]));
      if (!empty_attribute(tokens[15])) outputMessage.put(VirtualSystem, unquoted_attribute(tokens[15]));
      if (!empty_attribute(tokens[16])) outputMessage.put(SourceZone, unquoted_attribute(tokens[16]));
      if (!empty_attribute(tokens[17])) outputMessage.put(DestinationZone, unquoted_attribute(tokens[17]));
      if (!empty_attribute(tokens[18])) outputMessage.put(InboundInterface, unquoted_attribute(tokens[18]));
      if (!empty_attribute(tokens[19])) outputMessage.put(OutboundInterface, unquoted_attribute(tokens[19]));
      if (!empty_attribute(tokens[20])) outputMessage.put(LogAction, unquoted_attribute(tokens[20]));
      if (!empty_attribute(tokens[21])) outputMessage.put(TimeLogged, tokens[21].trim());
      if (!empty_attribute(tokens[22])) outputMessage.put(SessionID, tokens[22].trim());
      if (!empty_attribute(tokens[23])) outputMessage.put(RepeatCount, tokens[23].trim());
      if (!empty_attribute(tokens[24])) outputMessage.put(SourcePort, tokens[24].trim());
      if (!empty_attribute(tokens[25])) outputMessage.put(DestinationPort, tokens[25].trim());
      if (!empty_attribute(tokens[26])) outputMessage.put(NATSourcePort, tokens[26].trim());
      if (!empty_attribute(tokens[27])) outputMessage.put(NATDestinationPort, tokens[27].trim());
      if (!empty_attribute(tokens[28])) outputMessage.put(Flags, tokens[28].trim());
      if (!empty_attribute(tokens[29])) outputMessage.put(IPProtocol, unquoted_attribute(tokens[29]));
      if (!empty_attribute(tokens[30])) outputMessage.put(Action, unquoted_attribute(tokens[30]));

      if (LogTypeThreat.equals(type.toUpperCase())) {
        int p1_offset = 0;
        if      (tokens.length == 45) parser_version = 60;
        else if (tokens.length == 53) parser_version = 61;
        else if (tokens.length == 61) {
          parser_version = 70;
          p1_offset = 1;
        } else if (tokens.length == 72) {
          parser_version = 80;
          p1_offset = 1;
        }
        if (!empty_attribute(tokens[31])) {
          outputMessage.put(URL, unquoted_attribute(tokens[31]));
          try {
            URL url = new URL(unquoted_attribute(tokens[31]));
            outputMessage.put(HOST, url.getHost());
          } catch (MalformedURLException e) {
          }
        }
        if (!empty_attribute(tokens[32])) outputMessage.put(ThreatID, tokens[32].trim());
        if (!empty_attribute(tokens[33])) outputMessage.put(Category, unquoted_attribute(tokens[33]));
        if (!empty_attribute(tokens[34])) outputMessage.put(Severity, unquoted_attribute(tokens[34]));
        if (!empty_attribute(tokens[35])) outputMessage.put(Direction, unquoted_attribute(tokens[35]));
        if (!empty_attribute(tokens[36])) outputMessage.put(Seqno, tokens[36].trim());
        if (!empty_attribute(tokens[37])) outputMessage.put(ActionFlags, unquoted_attribute(tokens[37]));
        if (!empty_attribute(tokens[38])) outputMessage.put(SourceLocation, unquoted_attribute(tokens[38]));
        if (!empty_attribute(tokens[39]))
          outputMessage.put(DestinationLocation, unquoted_attribute(tokens[39]));
        if (!empty_attribute(tokens[41])) outputMessage.put(ContentType, unquoted_attribute(tokens[41]));
        if (!empty_attribute(tokens[42])) outputMessage.put(PCAPID, tokens[42].trim());
        if (!empty_attribute(tokens[43])) outputMessage.put(WFFileDigest, unquoted_attribute(tokens[43]));
        if (!empty_attribute(tokens[44])) outputMessage.put(WFCloud, unquoted_attribute(tokens[44]));
        if (parser_version >= 61) {
          if (!empty_attribute(tokens[(45 + p1_offset)]))
            outputMessage.put(UserAgent, unquoted_attribute(tokens[(45 + p1_offset)]));
          if (!empty_attribute(tokens[(46 + p1_offset)]))
            outputMessage.put(WFFileType, unquoted_attribute(tokens[(46 + p1_offset)]));
          if (!empty_attribute(tokens[(47 + p1_offset)]))
            outputMessage.put(XForwardedFor, unquoted_attribute(tokens[(47 + p1_offset)]));
          if (!empty_attribute(tokens[(48 + p1_offset)]))
            outputMessage.put(Referer, unquoted_attribute(tokens[(48 + p1_offset)]));
          if (!empty_attribute(tokens[(49 + p1_offset)]))
            outputMessage.put(WFSender, unquoted_attribute(tokens[(49 + p1_offset)]));
          if (!empty_attribute(tokens[(50 + p1_offset)]))
            outputMessage.put(WFSubject, unquoted_attribute(tokens[(50 + p1_offset)]));
          if (!empty_attribute(tokens[(51 + p1_offset)]))
            outputMessage.put(WFRecipient, unquoted_attribute(tokens[(51 + p1_offset)]));
          if (!empty_attribute(tokens[(52 + p1_offset)]))
            outputMessage.put(WFReportID, unquoted_attribute(tokens[(52 + p1_offset)]));
        }
        if (parser_version >= 70) {
          if (!empty_attribute(tokens[45])) outputMessage.put(URLIndex, tokens[45].trim());
          if (!empty_attribute(tokens[54])) outputMessage.put(DGH1, tokens[54].trim());
          if (!empty_attribute(tokens[55])) outputMessage.put(DGH2, tokens[55].trim());
          if (!empty_attribute(tokens[56])) outputMessage.put(DGH3, tokens[56].trim());
          if (!empty_attribute(tokens[57])) outputMessage.put(DGH4, tokens[57].trim());
          if (!empty_attribute(tokens[58])) outputMessage.put(VSYSName, unquoted_attribute(tokens[58]));
          if (!empty_attribute(tokens[59])) outputMessage.put(DeviceName, unquoted_attribute(tokens[59]));
        }
        if (parser_version >= 80) {
          if (!empty_attribute(tokens[61])) outputMessage.put(SourceVmUuid, tokens[61].trim());
          if (!empty_attribute(tokens[62])) outputMessage.put(DestinationVmUuid, tokens[62].trim());
          if (!empty_attribute(tokens[63])) outputMessage.put(HTTPMethod, tokens[63].trim());
          if (!empty_attribute(tokens[64])) outputMessage.put(TunnelId, tokens[64].trim());
          if (!empty_attribute(tokens[65])) outputMessage.put(MonitorTag, tokens[65].trim());
          if (!empty_attribute(tokens[66])) outputMessage.put(ParentSessionId, tokens[66].trim());
          if (!empty_attribute(tokens[67])) outputMessage.put(ParentSessionStartTime, tokens[67].trim());
          if (!empty_attribute(tokens[68])) outputMessage.put(TunnelType, tokens[68].trim());
          if (!empty_attribute(tokens[69])) outputMessage.put(ThreatCategory, tokens[69].trim());
          if (!empty_attribute(tokens[70])) outputMessage.put(ContentVersion, tokens[70].trim());
        }
      } else if (LogTypeTraffic.equals(type.toUpperCase())) {
        if (tokens.length == 46) parser_version = 60;
        else if (tokens.length == 47) parser_version = 61;
        else if (tokens.length == 54) parser_version = 70;
        else if (tokens.length == 61) parser_version = 80;
        if (!empty_attribute(tokens[31])) outputMessage.put(Bytes, tokens[31].trim());
        if (!empty_attribute(tokens[32])) outputMessage.put(BytesSent, tokens[32].trim());
        if (!empty_attribute(tokens[33])) outputMessage.put(BytesReceived, tokens[33].trim());
        if (!empty_attribute(tokens[34])) outputMessage.put(Packets, tokens[34].trim());
        if (!empty_attribute(tokens[35])) outputMessage.put(StartTime, tokens[35].trim());
        if (!empty_attribute(tokens[36])) outputMessage.put(ElapsedTimeInSec, tokens[36].trim());
        if (!empty_attribute(tokens[37])) outputMessage.put(Category, unquoted_attribute(tokens[37]));
        if (!empty_attribute(tokens[39])) outputMessage.put(Seqno, tokens[39].trim());
        if (!empty_attribute(tokens[40])) outputMessage.put(ActionFlags, unquoted_attribute(tokens[40]));
        if (!empty_attribute(tokens[41])) outputMessage.put(SourceLocation, unquoted_attribute(tokens[41]));
        if (!empty_attribute(tokens[42]))
          outputMessage.put(DestinationLocation, unquoted_attribute(tokens[42]));
        if (!empty_attribute(tokens[44])) outputMessage.put(PktsSent, tokens[44].trim());
        if (!empty_attribute(tokens[45])) outputMessage.put(PktsReceived, tokens[45].trim());
        if (parser_version >= 61) {
          if (!empty_attribute(tokens[46])) outputMessage.put(EndReason, unquoted_attribute(tokens[46]));
        }
        if (parser_version >= 70) {
          if (!empty_attribute(tokens[47])) outputMessage.put(DGH1, tokens[47].trim());
          if (!empty_attribute(tokens[48])) outputMessage.put(DGH2, tokens[48].trim());
          if (!empty_attribute(tokens[49])) outputMessage.put(DGH3, tokens[49].trim());
          if (!empty_attribute(tokens[50])) outputMessage.put(DGH4, tokens[50].trim());
          if (!empty_attribute(tokens[51])) outputMessage.put(VSYSName, unquoted_attribute(tokens[51]));
          if (!empty_attribute(tokens[52])) outputMessage.put(DeviceName, unquoted_attribute(tokens[52]));
          if (!empty_attribute(tokens[53])) outputMessage.put(ActionSource, unquoted_attribute(tokens[53]));
        }
        if (parser_version >= 80) {
          if (!empty_attribute(tokens[54])) outputMessage.put(SourceVmUuid, tokens[54].trim());
          if (!empty_attribute(tokens[55])) outputMessage.put(DestinationVmUuid, tokens[55].trim());
          if (!empty_attribute(tokens[56])) outputMessage.put(TunnelId, tokens[56].trim());
          if (!empty_attribute(tokens[57])) outputMessage.put(MonitorTag, tokens[57].trim());
          if (!empty_attribute(tokens[58])) outputMessage.put(ParentSessionId, tokens[58].trim());
          if (!empty_attribute(tokens[59])) outputMessage.put(ParentSessionStartTime, tokens[59].trim());
          if (!empty_attribute(tokens[60])) outputMessage.put(TunnelType, tokens[60].trim());
        }
      }
    }
    outputMessage.put(ParserVersion, parser_version);
    if (parser_version == 0) {
      outputMessage.put(Tokens, tokens.length);
    }
  }
}
