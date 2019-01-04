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
import { Injectable, Inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { GrokValidation } from '../model/grok-validation';
import { HttpUtil } from '../util/httpUtil';
import {AppConfigService} from './app-config.service';

@Injectable()
export class GrokValidationService {
  url = this.appConfigService.getApiRoot() + '/grok';

  constructor(
    private http: HttpClient,
    private appConfigService: AppConfigService
  ) {}

  public validate(grokValidation: GrokValidation): Observable<GrokValidation> {
    return this.http
      .post(this.url + '/validate', JSON.stringify(grokValidation))
      .pipe(
        map(HttpUtil.extractData),
        catchError(HttpUtil.handleError)
      );
  }

  public list(): Observable<string[]> {
    return this.http.get(this.url + '/list').pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public getStatement(path: string): Observable<Object> {
    const options: HttpParams = new HttpParams().set('path', path);
    return this.http.get(this.url + '/get/statement', { params: options }).pipe(
      map(HttpUtil.extractString),
      catchError(HttpUtil.handleError)
    );
  }
}
