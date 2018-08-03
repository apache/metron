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
import { Component, Input } from '@angular/core';

import { PcapService, PcapStatusResponse } from '../service/pcap.service';
import { PcapRequest } from '../model/pcap.request';
import { Pdml } from '../model/pdml';
import { Subscription } from 'rxjs/Rx';

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
  selectedPage: number = 1;
  errorMsg: string;

  constructor(private pcapService: PcapService) {}

  onSearch(pcapRequest) {
    console.log(pcapRequest);
    this.pdml = null;
    this.progressWidth = 0;
    this.pcapService.submitRequest(pcapRequest).subscribe((submitResponse: PcapStatusResponse) => {
      this.queryId = submitResponse.jobId;
      this.queryRunning = true;
      this.errorMsg = null;
      this.statusSubscription = this.pcapService.pollStatus(submitResponse.jobId).subscribe((statusResponse: PcapStatusResponse) => {
        if ('SUCCEEDED' === statusResponse.jobStatus) {
          this.statusSubscription.unsubscribe();
          this.queryRunning = false;
          this.pcapService.getPackets(submitResponse.jobId, this.selectedPage).toPromise().then(pdml => {
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
        this.errorMsg = `Response status: ${error.responseCode}. Something went wrong with your status request!`;
      });
    });
  }

  getDownloadUrl() {
    return this.pcapService.getDownloadUrl(this.queryId, this.selectedPage);
  }
}
