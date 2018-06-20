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
import {Subject}    from 'rxjs/Subject';
import {Observable} from 'rxjs/Rx';
import 'rxjs/add/observable/interval';
import 'rxjs/add/operator/switchMap';
import 'rxjs/add/operator/onErrorResumeNext';

import {HttpUtil} from '../utils/httpUtil';
import {Alert} from '../model/alert';
import {Http} from '@angular/http';
import {PatchRequest} from '../model/patch-request';
import {Utils} from '../utils/utils';
import {Patch} from '../model/patch';
import { GlobalConfigService } from './global-config.service';
import {CommentAddRemoveRequest} from "../model/comment-add-remove-request";

@Injectable()
export class UpdateService {

  defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};

  alertChangedSource = new Subject<PatchRequest>();
  alertChanged$ = this.alertChangedSource.asObservable();
  sourceType = 'source:type';
  alertCommentChangedSource = new Subject<CommentAddRemoveRequest>();
  alertCommentChanged$ = this.alertCommentChangedSource.asObservable();

  constructor(private http: Http, private globalConfigService: GlobalConfigService) {
    this.globalConfigService.get().subscribe((config: {}) => {
      this.sourceType = config['source.type.field'];
    });
  }

  public addComment(commentRequest: CommentAddRemoveRequest, fireChangeListener = true): Observable<{}> {
    let url = '/api/v1/update/add/comment';
    return this.http.post(url, commentRequest, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
    .catch(HttpUtil.handleError)
    .map(result => {
      if (fireChangeListener) {
        this.alertCommentChangedSource.next(commentRequest);
      }
      return result;
    });
  }

  public removeComment(commentRequest: CommentAddRemoveRequest, fireChangeListener = true): Observable<{}> {
    let url = '/api/v1/update/remove/comment';
    return this.http.post(url, commentRequest, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
    .catch(HttpUtil.handleError)
    .map(result => {
      if (fireChangeListener) {
        this.alertCommentChangedSource.next(commentRequest);
      }
      return result;
    });
  }

  public patch(patchRequest: PatchRequest, fireChangeListener = true): Observable<{}> {
    let url = '/api/v1/update/patch';
    return this.http.patch(url, patchRequest, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
    .catch(HttpUtil.handleError)
    .map(result => {
      if (fireChangeListener) {
        this.alertChangedSource.next(patchRequest);
      }
      return result;
    });
  }

  public updateAlertState(alerts: Alert[], state: string, fireChangeListener = true): Observable<{}> {
    let patchRequests: PatchRequest[] = alerts.map(alert => {
      let patchRequest = new PatchRequest();
      patchRequest.guid = alert.source.guid;
      patchRequest.sensorType = Utils.getAlertSensorType(alert, this.sourceType);
      patchRequest.patch = [new Patch('add', '/alert_status', state)];
      return patchRequest;
    });
    let patchObservables = [];
    for (let patchRequest of patchRequests) {
      patchObservables.push(this.patch(patchRequest, fireChangeListener));
    }
    return Observable.forkJoin(patchObservables);
  }
}
