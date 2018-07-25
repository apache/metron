import { Component, OnInit, Input } from '@angular/core';

import { PcapService, PcapStatusResponse } from '../service/pcap.service';
import { PcapRequest } from '../model/pcap.request';
import { Pdml } from '../model/pdml';
import {Subscription} from 'rxjs/Rx';
import { PcapPagination } from '../model/pcap-pagination';

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
  @Input() resetPaginationForSearch: boolean;

  statusSubscription: Subscription;
  queryRunning: boolean = false;
  queryId: string;
  progressWidth: number = 0;
  pagination: PcapPagination = new PcapPagination();
  savedPcapRequest: {};
  errorMsg: string;

  constructor(private pcapService: PcapService ) { }

  changePage(page) {
    this.pagination.selectedPage = page;
    this.pcapService.getPackets(this.queryId, this.pagination.selectedPage).toPromise().then(pdml => {
      this.pdml = pdml;
    });
  }

  onSearch(pcapRequest) {
    this.savedPcapRequest = pcapRequest;
    this.pagination.selectedPage = 1;
    this.pdml = null;
    this.progressWidth = 0;
    this.errorMsg = null;
    this.pcapService.submitRequest(pcapRequest).subscribe((statusResponse: PcapStatusResponse) => {
      let id = statusResponse.jobId;
      if (!id) {
        this.errorMsg = statusResponse.description;
      } else {
        this.queryId = id;
        this.queryRunning = true;
        this.errorMsg = null;
        this.statusSubscription = this.pcapService.pollStatus(id).subscribe((statusResponse: PcapStatusResponse) => {
          if ('SUCCEEDED' === statusResponse.jobStatus) {
            this.pagination.total = statusResponse.pageTotal;
            this.statusSubscription.unsubscribe();
            this.queryRunning = false;
            this.pcapService.getPackets(id, this.pagination.selectedPage).toPromise().then(pdml => {
              this.pdml = pdml;
            });
          } else if ('FAILED' === statusResponse.jobStatus) {
            this.statusSubscription.unsubscribe();
            this.queryRunning = false;
            this.errorMsg = `Query status: ${statusResponse.jobStatus}. Check your filter criteria and try again!`;
          } else if (this.progressWidth < 100) {
            this.progressWidth = Math.trunc(statusResponse.percentComplete);
          }
        }, (error: any) => {
          this.statusSubscription.unsubscribe();
          this.queryRunning = false;
          this.errorMsg = `Response message: ${error.message}. Something went wrong with your status request!`;
        });
      }
    }, (error: any) => {
      this.errorMsg = `Response message: ${error.message}. Something went wrong with your query submission!`;
    });
  }

  getDownloadUrl() {
    return this.pcapService.getDownloadUrl(this.queryId, this.pagination.selectedPage);
  }
}