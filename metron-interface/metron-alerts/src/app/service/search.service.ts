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
import {Headers, RequestOptions} from '@angular/http';
import {Observable} from 'rxjs/Rx';
import 'rxjs/add/observable/interval';
import 'rxjs/add/operator/switchMap';
import 'rxjs/add/operator/onErrorResumeNext';

import {HttpUtil} from '../utils/httpUtil';
import {Http} from '@angular/http';
import {SearchResponse} from '../model/search-response';
import {SearchRequest} from '../model/search-request';
import {AlertSource} from '../model/alert-source';
import {INDEXES} from '../utils/constants';
import {ColumnMetadata} from '../model/column-metadata';

@Injectable()
export class SearchService {

  interval = 80000;
  defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};

  private static extractColumnNameDataFromRestApi(res: Response): ColumnMetadata[] {
    let response: any = res || {};
    let processedKeys: string[] = [];
    let columnMetadatas: ColumnMetadata[] = [];

    for (let index of Object.keys(response)) {
      let indexMetaData = response[index];
      for (let key of Object.keys(indexMetaData)) {
        if (processedKeys.indexOf(key) === -1) {
          processedKeys.push(key);
          columnMetadatas.push(new ColumnMetadata(key, indexMetaData[key]));
        }
      }
    }

    return columnMetadatas;
  }

  constructor(private http: Http,
              private ngZone: NgZone) { }

  public getAlert(sourceType: string, alertId: string): Observable<AlertSource> {
    let url = '/api/v1/search/findOne';
    let requestSchema = { guid: alertId, sensorType: sourceType};

    return this.http.post(url, requestSchema, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
    .map(HttpUtil.extractData)
    .catch(HttpUtil.handleError)
    .onErrorResumeNext();
  }

  public getColumnMetaData(): Observable<ColumnMetadata[]> {
    let url = '/api/v1/search/column/metadata';
    return this.http.post(url, INDEXES, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
    .map(HttpUtil.extractData)
    .map(SearchService.extractColumnNameDataFromRestApi)
    .catch(HttpUtil.handleError);
  }

  public pollSearch(searchRequest: SearchRequest): Observable<SearchResponse> {
    return this.ngZone.runOutsideAngular(() => {
      return this.ngZone.run(() => {
        return Observable.interval(this.interval * 1000).switchMap(() => {
          return this.search(searchRequest);
        });
      });
    });
  }

  public search(searchRequest: SearchRequest): Observable<SearchResponse> {
    let url = '/api/v1/search/search';
    return this.http.post(url, searchRequest, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
    .map(HttpUtil.extractData)
    .catch(HttpUtil.handleError)
    .onErrorResumeNext();
  }
}
