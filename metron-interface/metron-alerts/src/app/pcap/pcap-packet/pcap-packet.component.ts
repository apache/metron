import { Component, OnInit, Input } from '@angular/core';

import { PdmlPacket } from '../model/pdml'

@Component({
  selector: 'app-pcap-packet',
  templateUrl: './pcap-packet.component.html',
  styleUrls: ['./pcap-packet.component.scss']
})
export class PcapPacketComponent implements OnInit {
  @Input() packet: PdmlPacket 

  constructor() { }

  ngOnInit() {
  }

  toggle() {
    this.packet.expanded = !this.packet.expanded
  }

}
