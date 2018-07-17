import { Component, OnInit, Input } from '@angular/core';

import { PcapService, PcapStatusRespons } from '../service/pcap.service'
import { PcapRequest } from '../model/pcap.request'
import { Pdml } from '../model/pdml'
import {Subscription} from "rxjs/Rx";

class Query {
  id: String
}

@Component({
  selector: 'app-pcap-panel',
  templateUrl: './pcap-panel.component.html',
  styleUrls: ['./pcap-panel.component.scss']
})
export class PcapPanelComponent {

  @Input() pdml: Pdml = null;
  @Input() pcapRequest: PcapRequest;

  statusSubscription: Subscription;
  queryRunning: boolean = false;
  queryId: string;
  progressWidth: number = 0;
  
  constructor(private pcapService: PcapService ) {}

  onSearch(pcapRequest) {
    console.log(pcapRequest);
    this.pdml = null;
    this.progressWidth = 0;
    this.pcapService.submitRequest(pcapRequest).subscribe(id => {
      this.queryId = id;
      this.queryRunning = true;
      this.statusSubscription = this.pcapService.pollStatus(id).subscribe((statusResponse: PcapStatusRespons) => {
        //console.log(this.statusSubscription.closed);
        if (this.progressWidth == 100) {
          //this.progressWidth = 0;
        } else {
          this.progressWidth += 5;
        }
        if ('Finished' === statusResponse.status) {
          this.statusSubscription.unsubscribe();
          console.log(this.statusSubscription.closed);
          this.queryRunning = false;
          this.pcapService.getPackets(id).subscribe(pdml => {
            this.pdml = pdml;
          })
        }
      });
    });
  }

  getDownloadUrl() {
    return this.pcapService.getDownloadUrl(this.queryId);
  }
}
