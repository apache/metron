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
import {Observable} from 'rxjs';
import { catchError } from 'rxjs/operators';
import {Alert} from '../model/alert';
import { HttpClient } from '@angular/common/http';
import {HttpUtil} from '../utils/httpUtil';
import { RestError } from '../model/rest-error';
import {AppConfigService} from './app-config.service';

@Injectable()
export class AlertsService {

  constructor(private http: HttpClient, private appConfigService: AppConfigService) {}

  public escalate(alerts: Alert[]): Observable<Object | RestError> {
    return this.http.post(this.appConfigService.getApiRoot() + '/alerts/ui/escalate', alerts).pipe(
    catchError(HttpUtil.handleError));
  }
}
