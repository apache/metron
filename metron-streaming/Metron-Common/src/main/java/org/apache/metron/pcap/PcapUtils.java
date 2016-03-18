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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.json.simple.JSONObject;

/**
 * The Class PcapUtils.
 */
public class PcapUtils {

  /** The Constant SESSION_KEY_SEPERATOR. */
  private static final char SESSION_KEY_SEPERATOR = '-';

  /** The Constant protocolIdToNameMap. */
  private static final BiMap<Integer, String> protocolIdToNameMap = HashBiMap
      .create();

  // private static final Map<Integer, String> protocolIdToNameMap = new
  // HashMap();

  static {

    protocolIdToNameMap.put(0, "HOPOPT");
    protocolIdToNameMap.put(1, "ICMP");
    protocolIdToNameMap.put(2, "IGMP");
    protocolIdToNameMap.put(3, "GGP");
    protocolIdToNameMap.put(4, "IPV4");
    protocolIdToNameMap.put(5, "ST");
    protocolIdToNameMap.put(6, "TCP");
    protocolIdToNameMap.put(7, "CBT");
    protocolIdToNameMap.put(8, "EGP");
    protocolIdToNameMap.put(9, "IGP");
    protocolIdToNameMap.put(10, "BBN-RCC-MON");
    protocolIdToNameMap.put(11, "NVP-II");
    protocolIdToNameMap.put(12, "PUP");
    protocolIdToNameMap.put(13, "ARGUS");
    protocolIdToNameMap.put(14, "EMCON");
    protocolIdToNameMap.put(15, "XNET");
    protocolIdToNameMap.put(16, "CHAOS");
    protocolIdToNameMap.put(17, "UDP");
    protocolIdToNameMap.put(18, "MUX");
    protocolIdToNameMap.put(19, "DCN-MEAS");
    protocolIdToNameMap.put(20, "HMP");
    protocolIdToNameMap.put(21, "PRM");
    protocolIdToNameMap.put(22, "XNS-IDP");
    protocolIdToNameMap.put(23, "TRUNK-1");
    protocolIdToNameMap.put(24, "TRUNK-2");
    protocolIdToNameMap.put(25, "LEAF-1");
    protocolIdToNameMap.put(26, "LEAF-2");
    protocolIdToNameMap.put(27, "RDP");
    protocolIdToNameMap.put(28, "IRTP");
    protocolIdToNameMap.put(29, "ISO-TP4");
    protocolIdToNameMap.put(30, "NETBLT");
    protocolIdToNameMap.put(31, "MFE-NSP");
    protocolIdToNameMap.put(32, "MERIT-INP");
    protocolIdToNameMap.put(33, "DCCP");
    protocolIdToNameMap.put(34, "3PC");
    protocolIdToNameMap.put(35, "IDPR");
    protocolIdToNameMap.put(36, "XTP");
    protocolIdToNameMap.put(37, "DDP");
    protocolIdToNameMap.put(38, "IDPR-CMTP");
    protocolIdToNameMap.put(39, "TP++");
    protocolIdToNameMap.put(40, "IL");
    protocolIdToNameMap.put(41, "IPV6");
    protocolIdToNameMap.put(42, "SDRP");
    protocolIdToNameMap.put(43, "IPV6-ROUTE");
    protocolIdToNameMap.put(44, "IPV6-FRAG");
    protocolIdToNameMap.put(45, "IDRP");
    protocolIdToNameMap.put(46, "RSVP");
    protocolIdToNameMap.put(47, "GRE");
    protocolIdToNameMap.put(48, "DSR");
    protocolIdToNameMap.put(49, "BNA");
    protocolIdToNameMap.put(50, "ESP");
    protocolIdToNameMap.put(51, "AH");
    protocolIdToNameMap.put(52, "I-NLSP");
    protocolIdToNameMap.put(53, "SWIPE");
    protocolIdToNameMap.put(54, "NARP");
    protocolIdToNameMap.put(55, "MOBILE");
    protocolIdToNameMap.put(56, "TLSP");
    protocolIdToNameMap.put(57, "SKIP");
    protocolIdToNameMap.put(58, "IPV6-ICMP");
    protocolIdToNameMap.put(59, "IPV6-NONXT");
    protocolIdToNameMap.put(60, "IPV6-OPTS");
    protocolIdToNameMap.put(62, "CFTP");
    protocolIdToNameMap.put(64, "SAT-EXPAK");
    protocolIdToNameMap.put(65, "KRYPTOLAN");
    protocolIdToNameMap.put(66, "RVD");
    protocolIdToNameMap.put(67, "IPPC");
    protocolIdToNameMap.put(69, "SAT-MON");
    protocolIdToNameMap.put(70, "VISA");
    protocolIdToNameMap.put(71, "IPCV");
    protocolIdToNameMap.put(72, "CPNX");
    protocolIdToNameMap.put(73, "CPHB");
    protocolIdToNameMap.put(74, "WSN");
    protocolIdToNameMap.put(75, "PVP");
    protocolIdToNameMap.put(76, "BR-SAT-MON");
    protocolIdToNameMap.put(77, "SUN-ND");
    protocolIdToNameMap.put(78, "WB-MON");
    protocolIdToNameMap.put(79, "WB-EXPAK");
    protocolIdToNameMap.put(80, "ISO-IP");
    protocolIdToNameMap.put(81, "VMTP");
    protocolIdToNameMap.put(82, "SECURE-VMTP");
    protocolIdToNameMap.put(83, "VINES");
    protocolIdToNameMap.put(84, "TTP");
    protocolIdToNameMap.put(85, "NSFNET-IGP");
    protocolIdToNameMap.put(86, "DGP");
    protocolIdToNameMap.put(87, "TCF");
    protocolIdToNameMap.put(88, "EIGRP");
    protocolIdToNameMap.put(89, "OSPFIGP");
    protocolIdToNameMap.put(90, "SPRITE-RPC");
    protocolIdToNameMap.put(91, "LARP");
    protocolIdToNameMap.put(92, "MTP");
    protocolIdToNameMap.put(93, "AX.25");
    protocolIdToNameMap.put(94, "IPIP");
    protocolIdToNameMap.put(95, "MICP");
    protocolIdToNameMap.put(96, "SCC-SP");
    protocolIdToNameMap.put(97, "ETHERIP");
    protocolIdToNameMap.put(98, "ENCAP");
    protocolIdToNameMap.put(100, "GMTP");
    protocolIdToNameMap.put(101, "IFMP");
    protocolIdToNameMap.put(102, "PNNI");
    protocolIdToNameMap.put(103, "PIM");
    protocolIdToNameMap.put(104, "ARIS");
    protocolIdToNameMap.put(105, "SCPS");
    protocolIdToNameMap.put(106, "QNX");
    protocolIdToNameMap.put(107, "A/N");
    protocolIdToNameMap.put(108, "IPCOMP");
    protocolIdToNameMap.put(109, "SNP");
    protocolIdToNameMap.put(110, "COMPAQ-PEER");
    protocolIdToNameMap.put(111, "IPX-IN-IP");
    protocolIdToNameMap.put(112, "VRRP");
    protocolIdToNameMap.put(113, "PGM");
    protocolIdToNameMap.put(115, "L2TP");
    protocolIdToNameMap.put(116, "DDX");
    protocolIdToNameMap.put(117, "IATP");
    protocolIdToNameMap.put(118, "STP");
    protocolIdToNameMap.put(119, "SRP");
    protocolIdToNameMap.put(120, "UTI");
    protocolIdToNameMap.put(121, "SMP");
    protocolIdToNameMap.put(122, "SM");
    protocolIdToNameMap.put(123, "PTP");
    protocolIdToNameMap.put(124, "ISIS OVER IPV4");
    protocolIdToNameMap.put(125, "FIRE");
    protocolIdToNameMap.put(126, "CRTP");
    protocolIdToNameMap.put(127, "CRUDP");
    protocolIdToNameMap.put(128, "SSCOPMCE");
    protocolIdToNameMap.put(129, "IPLT");
    protocolIdToNameMap.put(130, "SPS");
    protocolIdToNameMap.put(131, "PIPE");
    protocolIdToNameMap.put(132, "SCTP");
    protocolIdToNameMap.put(133, "FC");
    protocolIdToNameMap.put(134, "RSVP-E2E-IGNORE");
    protocolIdToNameMap.put(135, "MOBILITY HEADER");
    protocolIdToNameMap.put(136, "UDPLITE");
    protocolIdToNameMap.put(137, "MPLS-IN-IP");
    protocolIdToNameMap.put(138, "MANET");
    protocolIdToNameMap.put(139, "HIP");
    protocolIdToNameMap.put(140, "SHIM6");
    protocolIdToNameMap.put(141, "WESP");
    protocolIdToNameMap.put(142, "ROHC");
  }

  /** The Constant protocolNameToIdMap. */
  private static final BiMap<String, Integer> protocolNameToIdMap = protocolIdToNameMap
      .inverse();

  // private static final Map<String, Integer> protocolNameToIdMap =
  // invertMap(protocolIdToNameMap);

  /**
   * Convert ipv4 ip to hex.
   * 
   * @param ipAddress
   *          the ip address
   * @return the string
   */
  public static String convertIpv4IpToHex(String ipAddress) {
    StringBuffer hexIp = new StringBuffer(64);
    String[] ipSegments = ipAddress.split("\\.");

    for (String ipSegment : ipSegments) {
      hexIp.append(convertIpSegmentToHex(ipSegment));
    }

    return hexIp.toString();

  }

  /**
   * Gets the session key.
   * 
   * @param srcIp
   *          the src ip
   * @param dstIp
   *          the dst ip
   * @param protocol
   *          the protocol
   * @param srcPort
   *          the src port
   * @param dstPort
   *          the dst port
   * @return the session key
   */
  public static String getSessionKey(String srcIp, String dstIp,
      String protocol, String srcPort, String dstPort) {
    return getSessionKey(srcIp, dstIp, protocol, srcPort, dstPort, null, null);
  }

  /**
   * Gets the session key.
   * 
   * @param srcIp
   *          the src ip
   * @param dstIp
   *          the dst ip
   * @param protocol
   *          the protocol
   * @param srcPort
   *          the src port
   * @param dstPort
   *          the dst port
   * @param ipId
   *          the ip id
   * @param fragmentOffset
   *          the fragment offset
   * @return the session key
   */
  public static String getSessionKey(String srcIp, String dstIp,
      String protocol, String srcPort, String dstPort, String ipId,
      String fragmentOffset) {

    StringBuffer sb = new StringBuffer(40);
    sb.append(convertIpv4IpToHex(srcIp)).append(SESSION_KEY_SEPERATOR)
        .append(convertIpv4IpToHex(dstIp)).append(SESSION_KEY_SEPERATOR)
        .append(protocol == null ? "0" : protocol)
        .append(SESSION_KEY_SEPERATOR).append(srcPort == null ? "0" : srcPort)
        .append(SESSION_KEY_SEPERATOR).append(dstPort == null ? "0" : dstPort)
        .append(SESSION_KEY_SEPERATOR).append(ipId == null ? "0" : ipId)
        .append(SESSION_KEY_SEPERATOR)
        .append(fragmentOffset == null ? "0" : fragmentOffset);

    return sb.toString();
  }

  public static String getSessionKey(JSONObject message) {
    String srcIp = (String) message.get("ip_src_addr");
    String dstIp = (String) message.get("ip_dst_addr");
    Long protocol = (Long) message.get("ip_protocol");
    Long srcPort = (Long) message.get("ip_src_port");
    Long dstPort = (Long) message.get("ip_dst_port");
    Long ipId = (Long) message.get("ip_id");
    String ipIdString = ipId == null ? null : ipId.toString();
    Long fragmentOffset = (Long) message.get("frag_offset");
    String fragmentOffsetString = fragmentOffset == null ? null : fragmentOffset.toString();
    return PcapUtils.getSessionKey(srcIp, dstIp, protocol.toString(), srcPort.toString(), dstPort.toString(), ipIdString, fragmentOffsetString);
  }

  public static String getPartialSessionKey(String srcIp, String dstIp,
                                            String protocol, String srcPort, String dstPort) {
    StringBuffer sb = new StringBuffer(40);
    sb.append(convertIpv4IpToHex(srcIp)).append(SESSION_KEY_SEPERATOR)
            .append(convertIpv4IpToHex(dstIp)).append(SESSION_KEY_SEPERATOR)
            .append(protocol == null ? "0" : protocol)
            .append(SESSION_KEY_SEPERATOR).append(srcPort == null ? "0" : srcPort)
            .append(SESSION_KEY_SEPERATOR).append(dstPort == null ? "0" : dstPort);
    return sb.toString();
  }

  /**
   * Gets the session key.
   * 
   * @param srcIp
   *          the src ip
   * @param dstIp
   *          the dst ip
   * @param protocol
   *          the protocol
   * @param srcPort
   *          the src port
   * @param dstPort
   *          the dst port
   * @param ipId
   *          the ip id
   * @param fragmentOffset
   *          the fragment offset
   * @return the session key
   */
  public static String getSessionKey(String srcIp, String dstIp, int protocol,
      int srcPort, int dstPort, int ipId, int fragmentOffset) {
    String keySeperator = "-";
    StringBuffer sb = new StringBuffer(40);
    sb.append(convertIpv4IpToHex(srcIp)).append(keySeperator)
        .append(convertIpv4IpToHex(dstIp)).append(keySeperator)
        .append(protocol).append(keySeperator).append(srcPort)
        .append(keySeperator).append(dstPort).append(keySeperator).append(ipId)
        .append(keySeperator).append(fragmentOffset);

    return sb.toString();
  }

  /**
   * Gets the short session key. (5-tuple only)
   * 
   * @param srcIp
   *          the src ip
   * @param dstIp
   *          the dst ip
   * @param protocol
   *          the protocol
   * @param srcPort
   *          the src port
   * @param dstPort
   *          the dst port
   * @return the session key
   */
  public static String getShortSessionKey(String srcIp, String dstIp, int protocol,
      int srcPort, int dstPort) {
    String keySeperator = "-";
    StringBuffer sb = new StringBuffer(40);
    sb.append(convertIpv4IpToHex(srcIp)).append(keySeperator)
        .append(convertIpv4IpToHex(dstIp)).append(keySeperator)
        .append(protocol).append(keySeperator).append(srcPort)
        .append(keySeperator).append(dstPort);

    return sb.toString();
  }
  
  // public static String convertPortToHex(String portNumber) {
  // return convertPortToHex(Integer.valueOf(portNumber));
  //
  // }
  //
  // public static String convertPortToHex(int portNumber) {
  // return convertToHex(portNumber, 4);
  //
  // }
  //
  // public static String convertProtocolToHex(String protocol) {
  // return convertProtocolToHex(Integer.valueOf(protocol));
  //
  // }
  //
  // public static String convertProtocolToHex(int protocol) {
  // return convertToHex(protocol, 2);
  // }

  /**
   * Convert ip segment to hex.
   * 
   * @param ipSegment
   *          the ip segment
   * @return the string
   */
  public static String convertIpSegmentToHex(String ipSegment) {
    return convertIpSegmentToHex(Integer.valueOf(ipSegment));

  }

  /**
   * Convert ip segment to hex.
   * 
   * @param ipSegment
   *          the ip segment
   * @return the string
   */
  public static String convertIpSegmentToHex(int ipSegment) {
    return convertToHex(ipSegment, 2);

  }

  /**
   * Convert to hex.
   * 
   * @param number
   *          the number
   * @param length
   *          the length
   * @return the string
   */
  public static String convertToHex(int number, int length) {
    return StringUtils.leftPad(Integer.toHexString(number), length, '0');

  }

  /**
   * Gets the protocol name.
   * 
   * @param protocolNumber
   *          the protocol number
   * 
   * @return the protocol name
   */
  public static String getProtocolNameFromId(int protocolNumber) {
    String protocolName = protocolIdToNameMap.get(protocolNumber);

    if (protocolName == null) {
      protocolName = String.valueOf(protocolNumber);
    }
    return protocolName;
  }

  /**
   * Gets the protocol id from name.
   * 
   * @param protocolName
   *          the protocol name
   * @return the protocol id from name
   */
  public static int getProtocolIdFromName(String protocolName) {
    Integer protocolNumber = protocolNameToIdMap
        .get(protocolName.toUpperCase());

    if (protocolNumber == null) {
      protocolNumber = -1;
    }
    return protocolNumber;
  }

  /**
   * Invert map.
   * 
   * @param <V>
   *          the value type
   * @param <K>
   *          the key type
   * @param map
   *          the map
   * @return the map
   */
  private static <V, K> Map<V, K> invertMap(Map<K, V> map) {

    Map<V, K> inv = new HashMap<V, K>();

    for (Entry<K, V> entry : map.entrySet())
      inv.put(entry.getValue(), entry.getKey());

    return inv;
  }
}
