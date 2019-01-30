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
import { Injectable } from '@angular/core';
import { Observable, interval } from 'rxjs';
import { map, catchError, switchMap } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { HttpUtil } from '../../utils/httpUtil';


import { PcapRequest } from '../model/pcap.request';
import { Pdml } from '../model/pdml';
import { PcapStatusResponse } from '../model/pcap-status-response';
import { AppConfigService } from '../../service/app-config.service';

@Injectable()
export class PcapService {
  private statusInterval = 4;
  defaultHeaders = {
    'Content-Type': 'application/json',
    'X-Requested-With': 'XMLHttpRequest'
  };

  constructor(private http: HttpClient, private appConfigService: AppConfigService) {}

  public pollStatus(id: string): Observable<{}> {
    return interval(this.statusInterval * 1000).pipe(
      switchMap(() => {
        return this.getStatus(id);
      })
    );
  }

  public submitRequest(
    pcapRequest: PcapRequest
  ): Observable<PcapStatusResponse> {
    return this.http.post(this.appConfigService.getApiRoot() + '/pcap/fixed', pcapRequest).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public getStatus(id: string): Observable<PcapStatusResponse> {
    return this.http.get(this.appConfigService.getApiRoot() + `/pcap/${id}`).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public getRunningJob(): Observable<PcapStatusResponse[]> {
    return this.http.get(this.appConfigService.getApiRoot() + '/pcap?state=RUNNING').pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public getPackets(id: string, pageId: number): Observable<Pdml> {
    return this.http.get(this.appConfigService.getApiRoot() + `/pcap/${id}/pdml?page=${pageId}`).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public getPcapRequest(id: string): Observable<PcapRequest> {
    return this.http.get(this.appConfigService.getApiRoot() + `/pcap/${id}/config`).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public getDownloadUrl(id: string, pageId: number) {
    return this.appConfigService.getApiRoot() + `/pcap/${id}/raw?page=${pageId}`;
  }

  public cancelQuery(queryId: string) {
    return this.http
      .delete(this.appConfigService.getApiRoot() + `/pcap/kill/${queryId}`)
      .pipe(catchError(HttpUtil.handleError));
  }
}
