import { Component, OnInit, Input } from '@angular/core';
import { Pdml,PdmlPacket } from '../model/pdml'

@Component({
  selector: 'app-pcap-list',
  templateUrl: './pcap-list.component.html',
  styleUrls: ['./pcap-list.component.scss']
})
export class PcapListComponent implements OnInit {

  @Input() packets: PdmlPacket[]

  constructor() { }

  ngOnInit() {
  }
  
  toggle(packet) { 
    packet.expanded= !packet.expanded
  }

}
