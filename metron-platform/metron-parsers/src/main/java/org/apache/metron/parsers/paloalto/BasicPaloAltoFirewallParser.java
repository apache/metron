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

  private static final Logger _LOG = LoggerFactory.getLogger
          (BasicPaloAltoFirewallParser.class);

  private static final long serialVersionUID = 3147090149725343999L;
  public static final String PaloAltoDomain = "palo_alto_domain";
  public static final String ReceiveTime = "receive_time";
  public static final String SerialNum = "serial";
  public static final String Type = "type";
  public static final String ThreatContentType = "subtype";
  public static final String ConfigVersion = "config_version";
  public static final String GenerateTime = "time_generated";
  public static final String SourceAddress = "src";
  public static final String DestinationAddress = "dst";
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
  public static final String SourcePort = "sport";
  public static final String DestinationPort = "dport";
  public static final String NATSourcePort = "natsport";
  public static final String NATDestinationPort = "natdport";
  public static final String Flags = "flags";
  public static final String IPProtocol = "proto";
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
  public static final String UserAgent= "user_agent";
  public static final String WFFileType = "filetype";
  public static final String XForwardedFor = "xff";
  public static final String Referer = "referer";
  public static final String WFSender = "sender";
  public static final String WFSubject = "subject";
  public static final String WFRecipient = "recipient";
  public static final String WFReportID = "reportid";

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
      _LOG.debug("Received message: " + toParse);


      parseMessage(toParse, outputMessage);
      long timestamp = System.currentTimeMillis();
      outputMessage.put("timestamp", System.currentTimeMillis());
      outputMessage.put("ip_src_addr", outputMessage.remove("src"));
      outputMessage.put("ip_src_port", outputMessage.remove("sport"));
      outputMessage.put("ip_dst_addr", outputMessage.remove("dst"));
      outputMessage.put("ip_dst_port", outputMessage.remove("dport"));
      outputMessage.put("protocol", outputMessage.remove("proto"));

      outputMessage.put("original_string", toParse);
      messages.add(outputMessage);
      return messages;
    } catch (Exception e) {
      e.printStackTrace();
      _LOG.error("Failed to parse: " + toParse);
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private void parseMessage(String message, JSONObject outputMessage) {

    //String[] tokens = message.split(",");
    String[] tokens = Iterables.toArray(Splitter.on(Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")).split(message), String.class);

    String type = tokens[3].trim();

    //populate common objects
    outputMessage.put(PaloAltoDomain, tokens[0].trim());
    outputMessage.put(ReceiveTime, tokens[1].trim());
    outputMessage.put(SerialNum, tokens[2].trim());
    outputMessage.put(Type, type);
    outputMessage.put(ThreatContentType, tokens[4].trim());
    outputMessage.put(ConfigVersion, tokens[5].trim());
    outputMessage.put(GenerateTime, tokens[6].trim());
    outputMessage.put(SourceAddress, tokens[7].trim());
    outputMessage.put(DestinationAddress, tokens[8].trim());
    outputMessage.put(NATSourceIP, tokens[9].trim());
    outputMessage.put(NATDestinationIP, tokens[10].trim());
    outputMessage.put(Rule, tokens[11].trim());
    outputMessage.put(SourceUser, tokens[12].trim());
    outputMessage.put(DestinationUser, tokens[13].trim());
    outputMessage.put(Application, tokens[14].trim());
    outputMessage.put(VirtualSystem, tokens[15].trim());
    outputMessage.put(SourceZone, tokens[16].trim());
    outputMessage.put(DestinationZone, tokens[17].trim());
    outputMessage.put(InboundInterface, tokens[18].trim());
    outputMessage.put(OutboundInterface, tokens[19].trim());
    outputMessage.put(LogAction, tokens[20].trim());
    outputMessage.put(TimeLogged, tokens[21].trim());
    outputMessage.put(SessionID, tokens[22].trim());
    outputMessage.put(RepeatCount, tokens[23].trim());
    outputMessage.put(SourcePort, tokens[24].trim());
    outputMessage.put(DestinationPort, tokens[25].trim());
    outputMessage.put(NATSourcePort, tokens[26].trim());
    outputMessage.put(NATDestinationPort, tokens[27].trim());
    outputMessage.put(Flags, tokens[28].trim());
    outputMessage.put(IPProtocol, tokens[29].trim());
    outputMessage.put(Action, tokens[30].trim());


    if ("THREAT".equals(type.toUpperCase())) {
      outputMessage.put(URL, tokens[31].trim());
      try {
        URL url = new URL(tokens[31].trim());
        outputMessage.put(HOST, url.getHost());
      } catch (MalformedURLException e) {
      }
      outputMessage.put(ThreatID, tokens[32].trim());
      outputMessage.put(Category, tokens[33].trim());
      outputMessage.put(Severity, tokens[34].trim());
      outputMessage.put(Direction, tokens[35].trim());
      outputMessage.put(Seqno, tokens[36].trim());
      outputMessage.put(ActionFlags, tokens[37].trim());
      outputMessage.put(SourceLocation, tokens[38].trim());
      outputMessage.put(DestinationLocation, tokens[39].trim());
      outputMessage.put(ContentType, tokens[40].trim());
      outputMessage.put(PCAPID, tokens[41].trim());
      outputMessage.put(WFFileDigest, tokens[42].trim());
      outputMessage.put(WFCloud, tokens[43].trim());
      outputMessage.put(UserAgent, tokens[44].trim());
      outputMessage.put(WFFileType, tokens[45].trim());
      outputMessage.put(XForwardedFor, tokens[46].trim());
      outputMessage.put(Referer, tokens[47].trim());
      outputMessage.put(WFSender, tokens[48].trim());
      outputMessage.put(WFSubject, tokens[49].trim());
      outputMessage.put(WFRecipient, tokens[50].trim());
      outputMessage.put(WFReportID, tokens[51].trim());
      if (tokens.length > 52) { 
        outputMessage.put(DGH1, tokens[52].trim());
        outputMessage.put(DGH2, tokens[53].trim());
        outputMessage.put(DGH3, tokens[54].trim());
        outputMessage.put(DGH4, tokens[55].trim());
        outputMessage.put(VSYSName, tokens[56].trim());
        outputMessage.put(DeviceName, tokens[57].trim());
        outputMessage.put(ActionSource, tokens[58].trim());
      }


    } else if ("TRAFFIC".equals(type.toUpperCase())) {
      outputMessage.put(Bytes, tokens[31].trim());
      outputMessage.put(BytesSent, tokens[32].trim());
      outputMessage.put(BytesReceived, tokens[33].trim());
      outputMessage.put(Packets, tokens[34].trim());
      outputMessage.put(StartTime, tokens[35].trim());
      outputMessage.put(ElapsedTimeInSec, tokens[36].trim());
      outputMessage.put(Category, tokens[37].trim());
      outputMessage.put(Seqno, tokens[39].trim());
      outputMessage.put(ActionFlags, tokens[40].trim());
      outputMessage.put(SourceLocation, tokens[41].trim());
      outputMessage.put(DestinationLocation, tokens[42].trim());
      outputMessage.put(PktsSent, tokens[44].trim());
      outputMessage.put(PktsReceived, tokens[45].trim());
      outputMessage.put(EndReason, tokens[46].trim());
      if (tokens.length > 47) {
        outputMessage.put(DGH1, tokens[47].trim());
        outputMessage.put(DGH2, tokens[48].trim());
        outputMessage.put(DGH3, tokens[49].trim());
        outputMessage.put(DGH4, tokens[50].trim());
        outputMessage.put(VSYSName, tokens[51].trim());
        outputMessage.put(DeviceName, tokens[52].trim());
        outputMessage.put(ActionSource, tokens[53].trim());
      }
    }

  }


}
