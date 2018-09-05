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
import { Component, OnInit, OnDestroy, Input } from '@angular/core';

import { PcapService } from '../service/pcap.service';
import { PcapStatusResponse } from '../model/pcap-status-response';
import { PcapRequest } from '../model/pcap.request';
import { Pdml } from '../model/pdml';
import { Subscription } from 'rxjs';
import { PcapPagination } from '../model/pcap-pagination';
import { RestError } from '../../model/rest-error';

@Component({
  selector: 'app-pcap-panel',
  templateUrl: './pcap-panel.component.html',
  styleUrls: ['./pcap-panel.component.scss']
})
export class PcapPanelComponent implements OnInit, OnDestroy {

  @Input() pdml: Pdml = null;
  @Input() pcapRequest: PcapRequest;
  @Input() resetPaginationForSearch: boolean;

  statusSubscription: Subscription;
  cancelSubscription: Subscription;
  submitSubscription: Subscription;
  getSubscription: Subscription;
  queryRunning = false;
  queryId: string;
  progressWidth = 0;
  pagination: PcapPagination = new PcapPagination();
  savedPcapRequest: {};
  errorMsg: string;
  cancelConfirmMessage = 'Are you sure want to cancel the running query?';

  constructor(private pcapService: PcapService) { }

  ngOnInit() {
    this.pcapRequest = new PcapRequest();
    this.pcapService.getRunningJob().subscribe((statusResponses: PcapStatusResponse[]) => {
      if (statusResponses.length > 0) {
        // Assume the first job in the list is the running job
        this.queryRunning = true;
        let statusResponse = statusResponses[0];
        this.updateStatus(statusResponse);
        this.startPolling(statusResponse.jobId);
        this.pcapService.getPcapRequest(statusResponse.jobId).subscribe((pcapRequest: PcapRequest) => {
          this.pcapRequest = pcapRequest;
        });
      }
    });
  }

  changePage(page) {
    this.pagination.selectedPage = page;
    this.pcapService.getPackets(this.queryId, this.pagination.selectedPage).toPromise().then(pdml => {
      this.pdml = pdml;
    });
  }

  onSearch(pcapRequest) {
    this.queryRunning = true;
    this.queryId = '';
    this.savedPcapRequest = pcapRequest;
    this.pagination.selectedPage = 1;
    this.pdml = null;
    this.progressWidth = 0;
    this.errorMsg = null;
    this.submitSubscription = this.pcapService.submitRequest(pcapRequest).subscribe((submitResponse: PcapStatusResponse) => {
      let id = submitResponse.jobId;
      if (!id) {
        this.errorMsg = submitResponse.description;
        this.queryRunning = false;
      } else {
        this.startPolling(id);
      }
    }, (error: any) => {
      this.errorMsg = `Response message: ${error.message}. Something went wrong with your query submission!`;
    });
  }

  startPolling(id: string) {
    this.queryId = id;
    this.errorMsg = null;
    this.statusSubscription = this.pcapService.pollStatus(id).subscribe((statusResponse: PcapStatusResponse) => {
      this.updateStatus(statusResponse);
    }, (error: any) => {
      this.statusSubscription.unsubscribe();
      this.queryRunning = false;
      this.errorMsg = `Response message: ${error.message}. Something went wrong with your status request!`;
    });
  }

  updateStatus(statusResponse: PcapStatusResponse) {
    if ('SUCCEEDED' === statusResponse.jobStatus) {
      this.pagination.total = statusResponse.pageTotal;
      this.statusSubscription.unsubscribe();
      this.queryRunning = false;
      this.pcapService.getPackets(this.queryId, this.pagination.selectedPage).toPromise().then(pdml => {
        this.pdml = pdml;
      }, (error: RestError) => {
        if (error.status === 404) {
          this.errorMsg = 'No results returned';
        } else {
          this.errorMsg = `Response message: ${error.message}. Something went wrong retrieving pdml results!`;
        }
      });
    } else if ('FAILED' === statusResponse.jobStatus) {
      this.statusSubscription.unsubscribe();
      this.queryRunning = false;
      this.errorMsg = `Query status: ${statusResponse.jobStatus}. Check your filter criteria and try again!`;
    } else if (this.progressWidth < 100) {
      this.progressWidth = Math.trunc(statusResponse.percentComplete);
    }
  }

  getDownloadUrl() {
    return this.pcapService.getDownloadUrl(this.queryId, this.pagination.selectedPage);
  }

  unsubscribeAll() {
    if (this.cancelSubscription) {
      this.cancelSubscription.unsubscribe();
    }
    if (this.statusSubscription) {
      this.statusSubscription.unsubscribe();
    }
    if (this.submitSubscription) {
      this.submitSubscription.unsubscribe();
    }
  }

  cancelQuery() {
    this.cancelSubscription = this.pcapService.cancelQuery(this.queryId)
      .subscribe(() => {
        this.unsubscribeAll();
        this.queryId = '';
        this.queryRunning = false;
      }, (error: any) => {
        this.cancelSubscription.unsubscribe();
        this.queryId = '';
        this.errorMsg = `Response message: ${error.message}. Something went wrong with the cancellation!`;
        this.queryRunning = false;
      });
  }

  ngOnDestroy() {
    this.unsubscribeAll();
  }
}
