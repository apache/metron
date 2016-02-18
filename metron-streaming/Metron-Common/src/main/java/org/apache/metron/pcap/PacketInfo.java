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
package org.apache.metron.pcap;

import java.text.MessageFormat;
import org.apache.log4j.Logger;

import org.krakenapps.pcap.decoder.ip.Ipv4Packet;
import org.krakenapps.pcap.decoder.tcp.TcpPacket;
import org.krakenapps.pcap.decoder.udp.UdpPacket;
import org.krakenapps.pcap.file.GlobalHeader;
import org.krakenapps.pcap.packet.PacketHeader;
import org.krakenapps.pcap.packet.PcapPacket;

import org.apache.metron.pcap.Constants;
import org.apache.metron.pcap.PcapUtils;

/**
 * The Class PacketInfo.
 * 
 * @author sheetal
 * @version $Revision: 1.0 $
 */
public class PacketInfo {

  /** The packetHeader. */
  private PacketHeader packetHeader = null;

  /** The packet. */
  private PcapPacket packet = null;

  /** The ipv4 packet. */
  private Ipv4Packet ipv4Packet = null;

  /** The tcp packet. */
  private TcpPacket tcpPacket = null;

  /** The udp packet. */
  private UdpPacket udpPacket = null;

  /** The global header. */
  private GlobalHeader globalHeader = null;

  /** The Constant globalHeaderJsonTemplateSB. */
  private static final StringBuffer globalHeaderJsonTemplateSB = new StringBuffer();

  /** The Constant ipv4HeaderJsonTemplateSB. */
  private static final StringBuffer ipv4HeaderJsonTemplateSB = new StringBuffer();

  /** The Constant tcpHeaderJsonTemplateSB. */
  private static final StringBuffer tcpHeaderJsonTemplateSB = new StringBuffer();

  /** The Constant udpHeaderJsonTemplateSB. */
  private static final StringBuffer udpHeaderJsonTemplateSB = new StringBuffer();

  /** The Constant LOG. */
  private static final Logger LOG = Logger.getLogger(PacketInfo.class);
  
  static {
    globalHeaderJsonTemplateSB.append("<\"global_header\":<\"pcap_id\":\"").append("{0}").append('"');
    globalHeaderJsonTemplateSB.append(",\"inc_len\":").append("{1}");
    globalHeaderJsonTemplateSB.append(",\"orig_len\":").append("{2}");
    globalHeaderJsonTemplateSB.append(",\"ts_sec\":").append("{3}");
    globalHeaderJsonTemplateSB.append(",\"ts_usec\":").append("{4}");
    globalHeaderJsonTemplateSB.append(">,"); // NOPMD by sheetal on 1/29/14 2:37
    // PM

    // ipv4 header

    ipv4HeaderJsonTemplateSB.append("\"ipv4_header\":");

    ipv4HeaderJsonTemplateSB.append("\"ip_dst\":").append("{0}");
    ipv4HeaderJsonTemplateSB.append(",\"ip_dst_addr\":\"").append("{1}");
    ipv4HeaderJsonTemplateSB.append("\",\"ip_flags\":").append("{2}");
    ipv4HeaderJsonTemplateSB.append(",\"ip_fragment_offset\":").append("{3}");
    ipv4HeaderJsonTemplateSB.append(",\"ip_header_checksum\":").append("{4}");
    ipv4HeaderJsonTemplateSB.append(",\"ip_id\":").append("{5}");
    ipv4HeaderJsonTemplateSB.append(",\"ip_header_length\":").append("{6}");
    ipv4HeaderJsonTemplateSB.append(",\"ip_protocol\":").append("{7}");
    ipv4HeaderJsonTemplateSB.append(",\"ip_src\":").append("{8}");
    ipv4HeaderJsonTemplateSB.append(",\"ip_src_addr\":\"").append("{9}");
    ipv4HeaderJsonTemplateSB.append("\",\"ip_tos\":").append("{10}");
    ipv4HeaderJsonTemplateSB.append(",\"ip_total_length\":").append("{11}");
    ipv4HeaderJsonTemplateSB.append(",\"ip_ttl\":").append("{12}");
    ipv4HeaderJsonTemplateSB.append(",\"ip_version\":").append("{13}");
    ipv4HeaderJsonTemplateSB.append('>');

    // tcp header
    tcpHeaderJsonTemplateSB.append(",\"tcp_header\":<\"ack\":").append("{0}");
    tcpHeaderJsonTemplateSB.append(",\"checksum\":").append("{1}");
    tcpHeaderJsonTemplateSB.append(",\"data_length\":").append("{2}");
    tcpHeaderJsonTemplateSB.append(",\"data_offset\":").append("{3}");
    tcpHeaderJsonTemplateSB.append(",\"dst_addr\":\"").append("{4}");
    tcpHeaderJsonTemplateSB.append("\",\"dst_port\":").append("{5}");
    tcpHeaderJsonTemplateSB.append(",\"direction\":").append("{6}");
    tcpHeaderJsonTemplateSB.append(",\"flags\":").append("{7}");
    tcpHeaderJsonTemplateSB.append(",\"reassembled_length \":").append("{8}");
    tcpHeaderJsonTemplateSB.append(",\"relative_ack\":").append("{9}");
    tcpHeaderJsonTemplateSB.append(",\"relative_seq\":").append("{10}");
    tcpHeaderJsonTemplateSB.append(",\"seq\":").append("{11}");
    tcpHeaderJsonTemplateSB.append(",\"session_key\":\"").append("{12}");
    tcpHeaderJsonTemplateSB.append("\",\"src_addr\":\"").append("{13}");
    tcpHeaderJsonTemplateSB.append("\",\"src_port\":").append("{14}");
    tcpHeaderJsonTemplateSB.append(",\"total_length\":").append("{15}");
    tcpHeaderJsonTemplateSB.append(",\"urgent_pointer\":").append("{16}");
    tcpHeaderJsonTemplateSB.append(",\"window\":").append("{17}");
    tcpHeaderJsonTemplateSB.append(">>");

    // udp headers
    udpHeaderJsonTemplateSB.append(",\"udp_header\":<\"checksum\":").append("{0}");
    udpHeaderJsonTemplateSB.append(",\"dst_port\":").append("{1}");
    udpHeaderJsonTemplateSB.append(",\"length\":").append("{2}");
    udpHeaderJsonTemplateSB.append(",\"src_port\":").append("{3}");
    udpHeaderJsonTemplateSB.append(",\"dst_addr\":\"").append("{4}");
    udpHeaderJsonTemplateSB.append("\",\"src_addr\":\"").append("{5}").append('"');
    tcpHeaderJsonTemplateSB.append(">>");

  }

  /** The Constant globalHeaderJsonTemplateString. */
  private static final String globalHeaderJsonTemplateString = globalHeaderJsonTemplateSB.toString();

  /** The Constant ipv4HeaderJsonTemplateString. */
  private static final String ipv4HeaderJsonTemplateString = ipv4HeaderJsonTemplateSB.toString();

  /** The Constant tcpHeaderJsonTemplateString. */
  private static final String tcpHeaderJsonTemplateString = tcpHeaderJsonTemplateSB.toString();

  /** The Constant udpHeaderJsonTemplateString. */
  private static final String udpHeaderJsonTemplateString = udpHeaderJsonTemplateSB.toString();

  /**
   * Instantiates a new packet info.
   * 
   * @param globalHeader
   *          the global header
   * @param packetHeader
   *          the packet header
   * @param packet
   *          the packet
   * @param ipv4Packet
   *          the ipv4 packet
   * @param tcpPacket
   *          the tcp packet
   * @param udpPacket
   *          the udp packet
   */
  public PacketInfo(GlobalHeader globalHeader, PacketHeader packetHeader, PcapPacket packet, Ipv4Packet ipv4Packet, TcpPacket tcpPacket,
      UdpPacket udpPacket) {
    this.packetHeader = packetHeader;
    this.packet = packet;
    this.ipv4Packet = ipv4Packet;
    this.tcpPacket = tcpPacket;
    this.udpPacket = udpPacket;
    this.globalHeader = globalHeader;
  }

  /**
   * Gets the global header.
   * 
   * @return the global header
   */
  public GlobalHeader getGlobalHeader() {
    return globalHeader;
  }

  /**
   * Gets the packet header.
   * 
   * 
   * @return the packet header
   */
  public PacketHeader getPacketHeader() {
    return packetHeader;
  }

  /**
   * Gets the packet.
   * 
   * 
   * @return the packet
   */
  public PcapPacket getPacket() {
    return packet;
  }

  /**
   * Gets the ipv4 packet.
   * 
   * 
   * @return the ipv4 packet
   */
  public Ipv4Packet getIpv4Packet() {
    return ipv4Packet;
  }

  /**
   * Gets the tcp packet.
   * 
   * 
   * @return the tcp packet
   */
  public TcpPacket getTcpPacket() {
    return tcpPacket;
  }

  /**
   * Gets the udp packet.
   * 
   * 
   * @return the udp packet
   */
  public UdpPacket getUdpPacket() {
    return udpPacket;
  }

  /**
   * Gets the key.
   * 
   * 
   * @return the key
   */
  public String getKey() {
    int sourcePort = 0;
    int destinationPort = 0;
    if (Constants.PROTOCOL_UDP == ipv4Packet.getProtocol()) {
      sourcePort = udpPacket.getSourcePort();

      destinationPort = udpPacket.getDestinationPort();

    } else if (Constants.PROTOCOL_TCP == ipv4Packet.getProtocol()) {
      sourcePort = tcpPacket.getSourcePort();

      destinationPort = tcpPacket.getDestinationPort();

    }

    return PcapUtils.getSessionKey(ipv4Packet.getSourceAddress().getHostAddress(), ipv4Packet.getDestinationAddress().getHostAddress(),
        ipv4Packet.getProtocol(), sourcePort, destinationPort, ipv4Packet.getId(), ipv4Packet.getFragmentOffset());

  }

  /**
   * Gets the short key
   * 
   * 
   * @return the short key
   */
  public String getShortKey() {
	int sourcePort = 0;
	int destinationPort = 0;
	if(Constants.PROTOCOL_UDP == ipv4Packet.getProtocol()) {
		sourcePort = udpPacket.getSourcePort();
		destinationPort = udpPacket.getDestinationPort();
	} else if (Constants.PROTOCOL_TCP == ipv4Packet.getProtocol()) {
		sourcePort = tcpPacket.getSourcePort();
		destinationPort = tcpPacket.getDestinationPort();
	}
	  
	return PcapUtils.getShortSessionKey(ipv4Packet.getSourceAddress().getHostAddress(), ipv4Packet.getDestinationAddress().getHostAddress(),
	    ipv4Packet.getProtocol(), sourcePort, destinationPort);
			 
  }
  
  /**
   * Gets the json doc.
   * 
   * 
   * @return the json doc
   */
  public String getJsonDoc() {

    return getJsonDocUsingSBAppend();
  }

  /**
   * Gets the json doc.
   * 
   * 
   * @return the json doc
   */
  public String getJsonIndexDoc() {

    return getJsonIndexDocUsingSBAppend();
  }

  /**
   * Gets the json doc using sb append.
   * 
   * @return the json doc using sb append
   */
  private String getJsonDocUsingSBAppend() {

	
    StringBuffer jsonSb = new StringBuffer(1024);

    // global header
    jsonSb.append("{\"global_header\":{\"pcap_id\":\"").append(getKey());
    jsonSb.append("\",\"inc_len\":").append(packetHeader.getInclLen());
    jsonSb.append(",\"orig_len\":").append(packetHeader.getOrigLen());
    jsonSb.append(",\"ts_sec\":").append(packetHeader.getTsSec());
    jsonSb.append(",\"ts_usec\":").append(packetHeader.getTsUsec());
    jsonSb.append("},"); // NOPMD by sheetal on 1/29/14 2:37 PM

    // ipv4 header

    jsonSb.append("\"ipv4_header\":{");

    jsonSb.append("\"ip_dst\":").append(ipv4Packet.getDestination());
    jsonSb.append(",\"ip_dst_addr\":\"").append(ipv4Packet.getDestinationAddress().getHostAddress());
    jsonSb.append("\",\"ip_flags\":").append(ipv4Packet.getFlags());
    jsonSb.append(",\"ip_fragment_offset\":").append(ipv4Packet.getFragmentOffset());
    jsonSb.append(",\"ip_header_checksum\":").append(ipv4Packet.getHeaderChecksum());
    jsonSb.append(",\"ip_id\":").append(ipv4Packet.getId());
    jsonSb.append(",\"ip_header_length\":").append(ipv4Packet.getIhl());
    jsonSb.append(",\"ip_protocol\":").append(ipv4Packet.getProtocol());
    jsonSb.append(",\"ip_src\":").append(ipv4Packet.getSource());
    jsonSb.append(",\"ip_src_addr\":\"").append(ipv4Packet.getSourceAddress().getHostAddress());
    jsonSb.append("\",\"ip_tos\":").append(ipv4Packet.getTos());
    jsonSb.append(",\"ip_total_length\":").append(ipv4Packet.getTotalLength());
    jsonSb.append(",\"ip_ttl\":").append(ipv4Packet.getTtl());
    jsonSb.append(",\"ip_version\":").append(ipv4Packet.getVersion());
    jsonSb.append('}');

    // tcp header
    if (tcpPacket != null) {
      jsonSb.append(",\"tcp_header\":{\"ack\":").append(tcpPacket.getAck());
      jsonSb.append(",\"checksum\":").append(tcpPacket.getChecksum());
      jsonSb.append(",\"data_length\":").append(tcpPacket.getDataLength());
      jsonSb.append(",\"data_offset\":").append(tcpPacket.getDataOffset());
      jsonSb.append(",\"dst_addr\":\"").append(tcpPacket.getDestinationAddress().getHostAddress());
      jsonSb.append("\",\"dst_port\":").append(tcpPacket.getDestinationPort());
      jsonSb.append(",\"direction\":").append(tcpPacket.getDirection());
      jsonSb.append(",\"flags\":").append(tcpPacket.getFlags());
      jsonSb.append(",\"reassembled_length \":").append(tcpPacket.getReassembledLength());
      jsonSb.append(",\"relative_ack\":").append(tcpPacket.getRelativeAck());
      jsonSb.append(",\"relative_seq\":").append(tcpPacket.getRelativeSeq());
      jsonSb.append(",\"seq\":").append(tcpPacket.getSeq());
      jsonSb.append(",\"session_key\":\"").append(tcpPacket.getSessionKey());
      jsonSb.append("\",\"src_addr\":\"").append(tcpPacket.getSourceAddress().getHostAddress());
      jsonSb.append("\",\"src_port\":").append(tcpPacket.getSourcePort());
      jsonSb.append(",\"total_length\":").append(tcpPacket.getTotalLength());
      jsonSb.append(",\"urgent_pointer\":").append(tcpPacket.getUrgentPointer());
      jsonSb.append(",\"window\":").append(tcpPacket.getWindow());
      jsonSb.append('}');
    }

    // udp headers
    if (udpPacket != null) {
      jsonSb.append(",\"udp_header\":{\"checksum\":").append(udpPacket.getChecksum());
      jsonSb.append(",\"dst_port\":").append(udpPacket.getDestinationPort());
      jsonSb.append(",\"length\":").append(udpPacket.getLength());
      jsonSb.append(",\"src_port\":").append(udpPacket.getSourcePort());
      jsonSb.append(",\"dst_addr\":\"").append(udpPacket.getDestination().getAddress().getHostAddress());
      jsonSb.append("\",\"src_addr\":\"").append(udpPacket.getSource().getAddress().getHostAddress());
      jsonSb.append("\"}");
    }

    jsonSb.append('}');

    return jsonSb.toString();
  }

  /**
   * Gets the json doc using message format.
   * 
   * @return the json doc using message format
   */
  private String getJsonDocUsingMessageFormat() {

    StringBuffer jsonSb = new StringBuffer(600);

    jsonSb.append(MessageFormat.format(globalHeaderJsonTemplateString, getKey(), packetHeader.getInclLen(), packetHeader.getOrigLen(),
        packetHeader.getTsSec(), packetHeader.getTsUsec()));

    jsonSb.append(MessageFormat.format(ipv4HeaderJsonTemplateString, ipv4Packet.getDestination(), ipv4Packet.getDestinationAddress()
        .getHostAddress(), ipv4Packet.getFlags(), ipv4Packet.getFragmentOffset(), ipv4Packet.getHeaderChecksum(), ipv4Packet.getId(),
        ipv4Packet.getIhl(), ipv4Packet.getProtocol(), ipv4Packet.getSource(), ipv4Packet.getSourceAddress().getHostAddress(), ipv4Packet
            .getTos(), ipv4Packet.getTotalLength(), ipv4Packet.getTtl(), ipv4Packet.getVersion()));

    // tcp header
    if (tcpPacket != null) {
      jsonSb.append(MessageFormat.format(tcpHeaderJsonTemplateString, tcpPacket.getAck(), tcpPacket.getChecksum(), tcpPacket
          .getDataLength(), tcpPacket.getDataOffset(), tcpPacket.getDestinationAddress().getHostAddress(), tcpPacket.getDestinationPort(),
          tcpPacket.getDirection(), tcpPacket.getFlags(), tcpPacket.getReassembledLength(), tcpPacket.getRelativeAck(), tcpPacket
              .getRelativeSeq(), tcpPacket.getSeq(), tcpPacket.getSessionKey(), tcpPacket.getSourceAddress().getHostAddress(), tcpPacket
              .getSourcePort(), tcpPacket.getTotalLength(), tcpPacket.getUrgentPointer(), tcpPacket.getWindow()));
    } else
    // udp headers
    if (udpPacket != null) {
      jsonSb.append(MessageFormat.format(udpHeaderJsonTemplateString, udpPacket.getChecksum(), udpPacket.getDestinationPort(),
          udpPacket.getLength(), udpPacket.getSourcePort(), udpPacket.getDestination().getAddress().getHostAddress(), udpPacket.getSource()
              .getAddress().getHostAddress()));

    } else {
      jsonSb.append('}');
    }
    return jsonSb.toString().replace('<', '{').replace('>', '}');
  }

  /**
   * Gets the json index doc using sb append.
   * 
   * @return the json index doc using sb append
   */
  private String getJsonIndexDocUsingSBAppend() {

	Long ts_micro = getPacketTimeInNanos() / 1000L;
	StringBuffer jsonSb = new StringBuffer(175);

	jsonSb.append("{\"pcap_id\":\"").append(getShortKey());
    jsonSb.append("\",\"ip_protocol\":").append(ipv4Packet.getProtocol());
    jsonSb.append(",\"ip_id\":").append(ipv4Packet.getId());
    jsonSb.append(",\"frag_offset\":").append(ipv4Packet.getFragmentOffset());
    jsonSb.append(",\"ts_micro\":").append(ts_micro);


    // tcp header
    if (tcpPacket != null) {
      jsonSb.append(",\"ip_src_addr\":\"").append(tcpPacket.getSourceAddress().getHostAddress());
      jsonSb.append("\",\"ip_src_port\":").append(tcpPacket.getSourcePort());
      jsonSb.append(",\"ip_dst_addr\":\"").append(tcpPacket.getDestinationAddress().getHostAddress());
      jsonSb.append("\",\"ip_dst_port\":").append(tcpPacket.getDestinationPort());
    }

    // udp headers
    if (udpPacket != null) {
      jsonSb.append(",\"ip_src_addr\":\"").append(udpPacket.getSource().getAddress().getHostAddress());
      jsonSb.append("\",\"ip_src_port\":").append(udpPacket.getSourcePort());
      jsonSb.append(",\"ip_dst_addr\":\"").append(udpPacket.getDestination().getAddress().getHostAddress());
      jsonSb.append("\",\"ip_dst_port\":").append(udpPacket.getDestinationPort());
    }

    jsonSb.append('}');

    return jsonSb.toString();
  }
  
  public long getPacketTimeInNanos()
  {
	  if ( getGlobalHeader().getMagicNumber() == 0xa1b2c3d4 || getGlobalHeader().getMagicNumber() == 0xd4c3b2a1 )
	  {
		  //Time is in micro assemble as nano
		  LOG.info("Times are in micro according to the magic number");
		  return getPacketHeader().getTsSec() * 1000000000L + getPacketHeader().getTsUsec() * 1000L ; 
	  }
	  else if ( getGlobalHeader().getMagicNumber() == 0xa1b23c4d || getGlobalHeader().getMagicNumber() == 0x4d3cb2a1 ) {
		//Time is in nano assemble as nano
		  LOG.info("Times are in nano according to the magic number");
		  return getPacketHeader().getTsSec() * 1000000000L + getPacketHeader().getTsUsec() ; 
	  }
	  //Default assume time is in micro assemble as nano
	  LOG.warn("Unknown magic number. Defaulting to micro");
	  return getPacketHeader().getTsSec() * 1000000000L + getPacketHeader().getTsUsec() * 1000L ;  
  }
}
