import { Component, OnInit, Input } from '@angular/core';
import { PdmlPacket, PdmlProto, PdmlField } from '../model/pdml'

@Component({
  selector: '[app-pcap-packet-line]',
  templateUrl: './pcap-packet-line.component.html',
  styleUrls: ['./pcap-packet-line.component.scss']
})
export class PcapPacketLineComponent implements OnInit {

  @Input() packet: PdmlPacket

  ip: { 
    timestamp: PdmlField,
    ip_src_addr: PdmlField, ip_src_port: PdmlField, 
    ip_dest_addr: PdmlField, ip_dest_port: PdmlField, 
    protocol: PdmlField
  }
  
  constructor() { }

  ngOnInit() {
    let gen_proto: PdmlProto = this.packet.proto.filter(p => p['$'].name == "geninfo")[0]
    let ip_proto: PdmlProto = this.packet.proto.filter(p => p['$'].name == "ip")[0]
    let tcp_proto: PdmlProto = this.packet.proto.filter(p => p['$'].name == "tcp")[0]
    
    this.ip = {
      timestamp: PdmlProto.findField(gen_proto,'timestamp'), 
      ip_src_addr: PdmlProto.findField(ip_proto,'ip.src'), 
      ip_src_port: PdmlProto.findField(tcp_proto,'tcp.srcport'), 
      ip_dest_addr: PdmlProto.findField(ip_proto,'ip.dst'), 
      ip_dest_port: PdmlProto.findField(tcp_proto,'tcp.dstport'), 
      protocol: PdmlProto.findField(ip_proto,'ip.proto')
    };
  }
  

}
