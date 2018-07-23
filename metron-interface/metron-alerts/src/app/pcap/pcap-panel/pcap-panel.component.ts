import { Component, OnInit, Input } from '@angular/core';

import { PcapService, PcapStatusResponse } from '../service/pcap.service'
import { PcapRequest } from '../model/pcap.request'
import { Pdml } from '../model/pdml'
import {Subscription} from "rxjs/Rx";

@Component({
  selector: 'app-pcap-panel',
  templateUrl: './pcap-panel.component.html',
  styleUrls: ['./pcap-panel.component.scss']
})
export class PcapPanelComponent implements OnInit {

  @Input() pdml: Pdml = null;

  @Input() pcapRequest: PcapRequest;

  statusSubscription: Subscription;
  queryRunning: boolean = false;
  progressWidth: number = 0;
  selectedPage: number = 1;

  constructor(private pcapService: PcapService ) { }

  ngOnInit() {
  }

  onSearch(pcapRequest) {
    console.log(pcapRequest);
    this.pdml = null;
    this.progressWidth = 0;
    this.pcapService.submitRequest(pcapRequest).subscribe(id => {
      this.queryRunning = true;
      this.statusSubscription = this.pcapService.pollStatus(id).subscribe((statusResponse: PcapStatusResponse) => {
        if ('SUCCEEDED' === statusResponse.jobStatus) {
          this.statusSubscription.unsubscribe();
          this.queryRunning = false;
          this.pcapService.getPackets(id, this.selectedPage).toPromise().then(pdml => {
            this.pdml = pdml;
          });
        } else if (this.progressWidth < 100) {
          this.progressWidth = Math.trunc(statusResponse.percentComplete);
        }
      });
    });

    // this.pcapService.getTestPackets(this.pcapRequest).subscribe(response => {
    //   this.pdml = response
    // });
  }

  test(pcapRequest) {
    console.log(pcapRequest);
    this.pcapService.getTestPackets(this.pcapRequest).subscribe(response => {
      this.pdml = response
    })
  }


}
