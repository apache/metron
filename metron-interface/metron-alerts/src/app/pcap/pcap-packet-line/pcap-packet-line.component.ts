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
import { Component, OnInit, Input } from '@angular/core';
import { PdmlPacket, PdmlProto, PdmlField } from '../model/pdml';

@Component({
  selector: '[app-pcap-packet-line]',
  templateUrl: './pcap-packet-line.component.html',
  styleUrls: ['./pcap-packet-line.component.scss']
})
export class PcapPacketLineComponent implements OnInit {

  @Input() packet: PdmlPacket;

  ip: {
    timestamp: PdmlField,
    ipSrcAddr: PdmlField, ipSrcPort: PdmlField,
    ipDestAddr: PdmlField, ipDestPort: PdmlField,
    protocol: PdmlField
  };

  constructor() { }

  ngOnInit() {
    const genProto: PdmlProto = this.packet.protos.filter(p => p.name === 'geninfo')[0];
    const ipProto: PdmlProto = this.packet.protos.filter(p => p.name === 'ip')[0];
    const tcpProto: PdmlProto = this.packet.protos.filter(p => p.name === 'tcp')[0];
    const udpProto: PdmlProto = this.packet.protos.filter(p => p.name === 'udp')[0];

    this.ip = {
      timestamp: PdmlProto.findField(genProto, 'timestamp'),
      ipSrcAddr: PdmlProto.findField(ipProto, 'ip.src'),
      ipSrcPort: tcpProto ? PdmlProto.findField(tcpProto, 'tcp.srcport') : PdmlProto.findField(udpProto, 'udp.srcport'),
      ipDestAddr: PdmlProto.findField(ipProto, 'ip.dst'),
      ipDestPort: tcpProto ? PdmlProto.findField(tcpProto, 'tcp.dstport') : PdmlProto.findField(udpProto, 'udp.dstport'),
      protocol: PdmlProto.findField(ipProto, 'ip.proto')
    };
  }


}
