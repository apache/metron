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
import 'rxjs/add/observable/interval';
import 'rxjs/add/operator/switchMap';
import 'rxjs/add/operator/onErrorResumeNext';

import {Alert} from '../model/alert';
import {Http} from '@angular/http';
import {DataSource} from './data-source';
import {AlertsSearchResponse} from '../model/alerts-search-response';
import {SearchRequest} from '../model/search-request';

@Injectable()
export class AlertService {

  interval = 80000;
  defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};

  constructor(private http: Http,
              private dataSource: DataSource,
              private ngZone: NgZone) { }

  public search(searchRequest: SearchRequest): Observable<AlertsSearchResponse> {
    return this.dataSource.getAlerts(searchRequest);
  }

  public pollSearch(searchRequest: SearchRequest): Observable<AlertsSearchResponse> {
    return this.ngZone.runOutsideAngular(() => {
      return Observable.interval(this.interval * 1000).switchMap(() => {
        return this.dataSource.getAlerts(searchRequest);
      });
    });
  }

  public getAlert(index: string, type: string, alertId: string): Observable<Alert> {
    return this.dataSource.getAlert(index, type, alertId);
  }

  public updateAlertState(alerts: Alert[], state: string, workflowId: string) {
    let request = '';
    for (let alert of alerts) {
      request += '{ "update" : { "_index" : "' + alert._index + '", "_type" : "' + alert._type + '", "_id" : "' + alert._id + '" } }\n' +
                  '{ "doc": { "alert_status": "' + state + '"';
      if (workflowId) {
        request += ', "workflow_id": "' + workflowId + '"';
      }
      request += ' }}\n';
    }

    return this.dataSource.updateAlertState(request);
  }
}
