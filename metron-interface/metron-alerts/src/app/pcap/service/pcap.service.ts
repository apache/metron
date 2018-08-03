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
import {Injectable, NgZone} from '@angular/core';
import {Observable} from 'rxjs/Rx';
import {Http, Headers, RequestOptions} from '@angular/http';
import {HttpUtil} from '../../utils/httpUtil';

import 'rxjs/add/operator/map';

import {PcapRequest} from '../model/pcap.request';
import {Pdml} from '../model/pdml';

export class PcapStatusResponse {
  jobId: string;
  jobStatus: string;
  percentComplete: number;
  totalPages: number;
}

@Injectable()
export class PcapService {

    private statusInterval = 4;
    defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};

    constructor(private http: Http, private ngZone: NgZone) {
    }

    public pollStatus(id: string): Observable<{}> {
      return Observable.interval(this.statusInterval * 1000).switchMap(() => {
        return this.getStatus(id);
      });
    }

    public submitRequest(pcapRequest: PcapRequest): Observable<PcapStatusResponse> {
      return this.http.post('/api/v1/pcap/fixed', pcapRequest, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
          .map(result => result.json() as PcapStatusResponse)
          .catch(HttpUtil.handleError)
          .onErrorResumeNext();
    }

    public getStatus(id: string): Observable<PcapStatusResponse> {
      return this.http.get(`/api/v1/pcap/${id}`,
          new RequestOptions({headers: new Headers(this.defaultHeaders)}))
          .map(HttpUtil.extractData)
          .catch(HttpUtil.handleError);
  }
    public getPackets(id: string, pageId: number): Observable<Pdml> {
        return this.http.get(`/api/v1/pcap/${id}/pdml?page=${pageId}`, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
            .map(HttpUtil.extractData)
            .catch(HttpUtil.handleError)
            .onErrorResumeNext();
    }

    public getDownloadUrl(id: string, pageNo: number) {
      return `/api/v1/pcap/${id}/raw?page=${pageNo}`;
    }
}
