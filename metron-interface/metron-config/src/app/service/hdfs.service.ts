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
import {HttpClient, HttpParams} from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { HttpUtil } from '../util/httpUtil';
import {AppConfigService} from './app-config.service';

@Injectable()
export class HdfsService {
  url = this.appConfigService.getApiRoot() + '/hdfs';

  constructor(
    private http: HttpClient,
    private appConfigService: AppConfigService
  ) {}

  public list(path: string): Observable<string[]> {
    const options: HttpParams = new HttpParams().set('path', path);
    return this.http.get(this.url + '/list', { params: options }).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public read(path: string): Observable<Object> {
    const options: HttpParams = new HttpParams().set('path', path);
    return this.http.get(this.url, { params: options, responseType: 'text' }).pipe(
      map(HttpUtil.extractString),
      catchError(HttpUtil.handleError)
    );
  }

  public post(path: string, contents: string): any {
    const options: HttpParams = new HttpParams().set('path', path);
    return this.http
      .post(this.url, contents, { params: options })
      .pipe(catchError(HttpUtil.handleError));
  }

  public deleteFile(path: string): any {
    const options: HttpParams = new HttpParams().set('path', path);
    return this.http
      .delete(this.url, { params: options })
      .pipe(catchError(HttpUtil.handleError));
  }
}
