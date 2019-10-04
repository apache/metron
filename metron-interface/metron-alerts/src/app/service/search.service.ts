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
import { HttpClient } from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import { map, onErrorResumeNext, catchError } from 'rxjs/operators';
import {HttpUtil} from '../utils/httpUtil';
import {SearchResponse} from '../model/search-response';
import {SearchRequest} from '../model/search-request';
import {AlertSource} from '../model/alert-source';
import {GroupRequest} from '../model/group-request';
import {GroupResult} from '../model/group-result';
import { RestError } from '../model/rest-error';
import {INDEXES} from '../utils/constants';
import {ColumnMetadata} from '../model/column-metadata';
import { AppConfigService } from './app-config.service';

@Injectable()
export class SearchService {

  private static extractColumnNameDataFromRestApi(res): ColumnMetadata[] {
    let response: any = res || {};
    let processedKeys: string[] = [];
    let columnMetadatas: ColumnMetadata[] = [];

    for (let key of Object.keys(response)) {
      if (processedKeys.indexOf(key) === -1) {
        processedKeys.push(key);
        columnMetadatas.push(new ColumnMetadata(key, response[key]));
      }
    }

    return columnMetadatas;
  }

  constructor(private http: HttpClient,
              private appConfigService: AppConfigService) { }

  groups(groupRequest: GroupRequest): Observable<GroupResult> {
    let url = this.appConfigService.getApiRoot() + '/search/group';
    return this.http.post(url, groupRequest).pipe(
    map(HttpUtil.extractData),
    catchError(HttpUtil.handleError),
    onErrorResumeNext());
  }

  public getAlert(sourceType: string, alertId: string): Observable<AlertSource> {
    let url = this.appConfigService.getApiRoot() + '/search/findOne';
    let requestSchema = { guid: alertId, sensorType: sourceType};
    return this.http.post(url, requestSchema).pipe(
    map(HttpUtil.extractData),
    catchError(HttpUtil.handleError),
    onErrorResumeNext());
  }

  public getColumnMetaData(): Observable<RestError | ColumnMetadata[]> {
    let url = this.appConfigService.getApiRoot() + '/search/column/metadata';
    return this.http.post(url, INDEXES).pipe(
    map(HttpUtil.extractData),
    map(SearchService.extractColumnNameDataFromRestApi),
    catchError(HttpUtil.handleError));
  }

  public search(searchRequest: SearchRequest): Observable<SearchResponse> {
    let url = this.appConfigService.getApiRoot() + '/search/search';

    return this.http.post(url, searchRequest).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.sessionExpiration),
    );
  }
}
