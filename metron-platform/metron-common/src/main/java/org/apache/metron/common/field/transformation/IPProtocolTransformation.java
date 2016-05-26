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

package org.apache.metron.common.field.transformation;


import java.util.HashMap;
import java.util.Map;

public class IPProtocolTransformation extends SimpleFieldTransformation {

  private final static Map<Integer, String> PROTOCOLS = new HashMap<>();

  static {
    PROTOCOLS.put(1, "ICMP");
    PROTOCOLS.put(2, "IGMP");
    PROTOCOLS.put(3, "GGP");
    PROTOCOLS.put(4, "IP-in-IP");
    PROTOCOLS.put(5, "ST");
    PROTOCOLS.put(6, "TCP");
    PROTOCOLS.put(7, "CBT");
    PROTOCOLS.put(8, "EGP");
    PROTOCOLS.put(9, "IGP");
    PROTOCOLS.put(10, "BBN-RCC-MON");
    PROTOCOLS.put(11, "NVP-II");
    PROTOCOLS.put(12, "PUP");
    PROTOCOLS.put(13, "ARGUS");
    PROTOCOLS.put(14, "EMCON");
    PROTOCOLS.put(15, "XNET");
    PROTOCOLS.put(16, "CHAOS");
    PROTOCOLS.put(17, "UDP");
    PROTOCOLS.put(18, "MUX");
    PROTOCOLS.put(19, "DCN-MEAS");
    PROTOCOLS.put(20, "HMP");
    PROTOCOLS.put(21, "PRM");
    PROTOCOLS.put(22, "XNS-IDP");
    PROTOCOLS.put(23, "TRUNK-1");
    PROTOCOLS.put(24, "TRUNK-2");
    PROTOCOLS.put(25, "LEAF-1");
    PROTOCOLS.put(26, "LEAF-2");
    PROTOCOLS.put(27, "RDP");
    PROTOCOLS.put(28, "IRTP");
    PROTOCOLS.put(29, "ISO-TP4");
    PROTOCOLS.put(30, "NETBLT");
    PROTOCOLS.put(31, "MFE-NSP");
    PROTOCOLS.put(32, "MERIT-INP");
    PROTOCOLS.put(33, "DCCP");
    PROTOCOLS.put(34, "3PC");
    PROTOCOLS.put(35, "IDPR");
    PROTOCOLS.put(36, "XTP");
    PROTOCOLS.put(37, "DDP");
    PROTOCOLS.put(38, "IDPR-CMTP");
    PROTOCOLS.put(39, "TP++");
    PROTOCOLS.put(40, "IL");
    PROTOCOLS.put(41, "IPv6");
    PROTOCOLS.put(42, "SDRP");
    PROTOCOLS.put(43, "IPv6-Route");
    PROTOCOLS.put(44, "IPv6-Frag");
    PROTOCOLS.put(45, "IDRP");
    PROTOCOLS.put(46, "RSVP");
    PROTOCOLS.put(47, "GRE");
    PROTOCOLS.put(48, "MHRP");
    PROTOCOLS.put(49, "BNA");
    PROTOCOLS.put(50, "ESP");
    PROTOCOLS.put(51, "AH");
    PROTOCOLS.put(52, "I-NLSP");
    PROTOCOLS.put(53, "SWIPE");
    PROTOCOLS.put(54, "NARP");
    PROTOCOLS.put(55, "MOBILE");
    PROTOCOLS.put(56, "TLSP");
    PROTOCOLS.put(57, "SKIP");
    PROTOCOLS.put(58, "IPv6-ICMP");
    PROTOCOLS.put(59, "IPv6-NoNxt");
    PROTOCOLS.put(60, "IPv6-Opts");
    PROTOCOLS.put(62, "CFTP");
    PROTOCOLS.put(64, "SAT-EXPAK");
    PROTOCOLS.put(65, "KRYPTOLAN");
    PROTOCOLS.put(66, "RVD");
    PROTOCOLS.put(67, "IPPC");
    PROTOCOLS.put(68, "SAT-MON");
    PROTOCOLS.put(70, "VISA");
    PROTOCOLS.put(71, "IPCU");
    PROTOCOLS.put(72, "CPNX");
    PROTOCOLS.put(73, "CPHB");
    PROTOCOLS.put(74, "WSN");
    PROTOCOLS.put(75, "PVP");
    PROTOCOLS.put(76, "BR-SAT-MON");
    PROTOCOLS.put(77, "SUN-ND");
    PROTOCOLS.put(78, "WB-MON");
    PROTOCOLS.put(79, "WB-EXPAK");
    PROTOCOLS.put(80, "ISO-IP");
    PROTOCOLS.put(81, "VMTP");
    PROTOCOLS.put(82, "SECURE-VMTP");
    PROTOCOLS.put(83, "VINES");
    PROTOCOLS.put(84, "TTP");
    PROTOCOLS.put(85, "NSFNET-IGP");
    PROTOCOLS.put(86, "DGP");
    PROTOCOLS.put(87, "TCF");
    PROTOCOLS.put(88, "");
    PROTOCOLS.put(89, "OSPF");
    PROTOCOLS.put(90, "Sprite-RP");
    PROTOCOLS.put(91, "LARP");
    PROTOCOLS.put(92, "MTP");
    PROTOCOLS.put(93, "AX.25");
    PROTOCOLS.put(94, "IPIP");
    PROTOCOLS.put(95, "MICP");
    PROTOCOLS.put(96, "SCC-SP");
    PROTOCOLS.put(97, "ETHERIP");
    PROTOCOLS.put(98, "ENCAP");
    PROTOCOLS.put(100, "GMTP");
    PROTOCOLS.put(101, "IFMP");
    PROTOCOLS.put(102, "PNNI");
    PROTOCOLS.put(103, "PIM");
    PROTOCOLS.put(104, "ARIS");
    PROTOCOLS.put(105, "SCPS");
    PROTOCOLS.put(106, "QNX");
    PROTOCOLS.put(107, "A/N");
    PROTOCOLS.put(108, "IPComp");
    PROTOCOLS.put(109, "SNP");
    PROTOCOLS.put(110, "Compaq-Peer");
    PROTOCOLS.put(111, "IPX-in-IP");
    PROTOCOLS.put(112, "VRRP");
    PROTOCOLS.put(113, "PGM");
    PROTOCOLS.put(115, "L2TP");
    PROTOCOLS.put(116, "DDX");
    PROTOCOLS.put(117, "IATP");
    PROTOCOLS.put(118, "STP");
    PROTOCOLS.put(119, "SRP");
    PROTOCOLS.put(120, "UTI");
    PROTOCOLS.put(121, "SMP");
    PROTOCOLS.put(122, "SM");
    PROTOCOLS.put(123, "PTP");
    PROTOCOLS.put(124, "IS-IS");
    PROTOCOLS.put(125, "FIRE");
    PROTOCOLS.put(126, "CRTP");
    PROTOCOLS.put(127, "CRUDP");
    PROTOCOLS.put(128, "SSCOPMCE");
    PROTOCOLS.put(129, "IPLT");
    PROTOCOLS.put(130, "SPS");
    PROTOCOLS.put(131, "PIPE");
    PROTOCOLS.put(132, "SCTP");
    PROTOCOLS.put(133, "FC");
    PROTOCOLS.put(134, "RSVP-E2E-IGNORE");
    PROTOCOLS.put(135, "Mobility Header");
    PROTOCOLS.put(136, "UDPLite");
    PROTOCOLS.put(137, "MPLS-in-IP");
    PROTOCOLS.put(138, "manet");
    PROTOCOLS.put(139, "HIP");
    PROTOCOLS.put(140, "Shim6");
    PROTOCOLS.put(141, "WESP");
    PROTOCOLS.put(142, "ROHC");
  }

  @Override
  public Map<String, Object> map(Object value, String outputField) {
    Map<String, Object> ret = new HashMap<>();
    if(value != null && value instanceof Number) {
      int protocolNum = ((Number)value).intValue();
      ret.put(outputField, PROTOCOLS.get(protocolNum));
    }
    return ret;
  }
}
