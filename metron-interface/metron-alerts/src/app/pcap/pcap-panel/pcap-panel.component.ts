import { Component, OnInit, Input } from '@angular/core';

import { PcapService } from '../service/pcap.service'
import { PcapRequest } from '../model/pcap.request'
import { Pdml } from '../model/pdml'

@Component({
  selector: 'app-pcap-panel',
  templateUrl: './pcap-panel.component.html',
  styleUrls: ['./pcap-panel.component.scss']
})
export class PcapPanelComponent implements OnInit {

  @Input() pdml: Pdml = null
  
  @Input() search: PcapRequest
  
  constructor(private pcap: PcapService ) { }

  ngOnInit() {
  }
  
  onSearch(search) {
    this.pcap.getPackets(search).subscribe(response => this.pdml = response)
  }
  
  test() {
    console.log('test')
    this.pcap.getTestPackets(this.search).subscribe(response => {
      this.pdml = response
    })
  }
  

}
