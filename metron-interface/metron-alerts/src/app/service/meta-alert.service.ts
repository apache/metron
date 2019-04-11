
import {map, catchError} from 'rxjs/operators';
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
import {Injectable} from '@angular/core';
import {Subject}    from 'rxjs';
import {Observable} from 'rxjs';

import {HttpUtil} from '../utils/httpUtil';
import {Alert} from '../model/alert';
import { HttpClient } from '@angular/common/http';
import {MetaAlertCreateRequest} from '../model/meta-alert-create-request';
import {MetaAlertAddRemoveRequest} from '../model/meta-alert-add-remove-request';
import { AppConfigService } from './app-config.service';
import {AlertSource} from "../model/alert-source";

@Injectable()
export class MetaAlertService {
  private _selectedAlerts: Alert[];
  alertChangedSource = new Subject<AlertSource>();
  alertChanged$ = this.alertChangedSource.asObservable();

  constructor(private http: HttpClient, private appConfigService: AppConfigService) {
  }

  get selectedAlerts(): Alert[] {
    return this._selectedAlerts;
  }

  set selectedAlerts(value: Alert[]) {
    this._selectedAlerts = value;
  }

  public create(metaAlertCreateRequest: MetaAlertCreateRequest): Observable<{}> {
    let url = this.appConfigService.getApiRoot() + '/metaalert/create';
    return this.http.post(url, metaAlertCreateRequest).pipe(
    catchError(HttpUtil.handleError));
  }

  public addAlertsToMetaAlert(metaAlertAddRemoveRequest: MetaAlertAddRemoveRequest): Observable<AlertSource> {
    let url = this.appConfigService.getApiRoot() + '/metaalert/add/alert';
    return this.http.post(url, metaAlertAddRemoveRequest).pipe(
    catchError(HttpUtil.handleError),
    map(result => {
      let alertSource = result['document'];
      this.alertChangedSource.next(alertSource);
      return alertSource;
    }));
  }

  public  removeAlertsFromMetaAlert(metaAlertAddRemoveRequest: MetaAlertAddRemoveRequest): Observable<AlertSource> {
    let url = this.appConfigService.getApiRoot() + '/metaalert/remove/alert';
    return this.http.post(url, metaAlertAddRemoveRequest).pipe(
    catchError(HttpUtil.handleError),
    map(result => {
      let alertSource = result['document'];
      this.alertChangedSource.next(alertSource);
      return alertSource;
    }));
  }

  public updateMetaAlertStatus(guid: string, status: string) {
    let url = this.appConfigService.getApiRoot() + `/metaalert/update/status/${guid}/${status}`;
    return this.http.post(url, {}).pipe(
    catchError(HttpUtil.handleError),
    map(result => {
      let alertSource = result['document'];
      this.alertChangedSource.next(alertSource);
      return alertSource;
    }));
  }
}
