import { Component, OnInit, Input } from '@angular/core';

import { PcapService } from '../service/pcap.service'
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

  private statusSubscription: Subscription;
  private queryRunning: boolean = false;
  private progressWidth: number = 0;
  
  constructor(private pcapService: PcapService ) { }

  ngOnInit() {
  }

  onSearch(pcapRequest) {
    console.log(pcapRequest);
    this.pdml = null;
    this.progressWidth = 0;
    this.pcapService.submitRequest(pcapRequest).subscribe(id => {
      this.queryRunning = true;
      this.statusSubscription = this.pcapService.pollStatus(id).subscribe(status => {
        //console.log(this.statusSubscription.closed);
        if (this.progressWidth == 100) {
          //this.progressWidth = 0;
        } else {
          this.progressWidth += 5;
        }
        if ('Finished' === status) {
          this.statusSubscription.unsubscribe();
          console.log(this.statusSubscription.closed);
          this.queryRunning = false;
          this.pcapService.getPackets(id).subscribe(pdml => {
            this.pdml = pdml;
          })
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
