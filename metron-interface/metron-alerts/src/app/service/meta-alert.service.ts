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
import {Headers, RequestOptions} from '@angular/http';
import {Observable} from 'rxjs/Rx';
import 'rxjs/add/observable/interval';
import 'rxjs/add/operator/switchMap';
import 'rxjs/add/operator/onErrorResumeNext';

import {HttpUtil} from '../utils/httpUtil';
import {Alert} from '../model/alert';
import {Http} from '@angular/http';
import {MetaAlertCreateRequest} from '../model/meta-alert-create-request';

@Injectable()
export class MetaAlertService {
  private _selectedAlerts: Alert[];
  defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};

  constructor(private http: Http) {}

  get selectedAlerts(): Alert[] {
    return this._selectedAlerts;
  }

  set selectedAlerts(value: Alert[]) {
    this._selectedAlerts = value;
  }

  public create(metaAlertCreateRequest: MetaAlertCreateRequest): Observable<{}> {
    let url = '/api/v1/metaalert/create';
    return this.http.post(url, metaAlertCreateRequest, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
    .catch(HttpUtil.handleError);
  }
}
