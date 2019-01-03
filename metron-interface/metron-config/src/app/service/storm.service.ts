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
import { HttpClient } from '@angular/common/http';
import { HttpUtil } from '../util/httpUtil';
import { TopologyStatus } from '../model/topology-status';
import { TopologyResponse } from '../model/topology-response';
import { Observable, interval } from 'rxjs';
import { map, catchError, switchMap, onErrorResumeNext } from 'rxjs/operators';
import {AppConfigService} from './app-config.service';

@Injectable()
export class StormService {
  url = this.appConfigService.getApiRoot() + '/storm';

  constructor(
    private http: HttpClient,
    private appConfigService: AppConfigService
  ) {}

  public pollGetAll(): Observable<TopologyStatus[]> {
    return interval(8000).pipe(
      switchMap(() => {
        return this.http.get(this.url).pipe(
          map(HttpUtil.extractData),
          catchError(HttpUtil.handleError),
          onErrorResumeNext()
        );
      })
    );
  }

  public getAll(): Observable<TopologyStatus[]> {
    return this.http.get(this.url).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public getEnrichmentStatus(): Observable<TopologyStatus> {
    return this.http.get(this.url + '/enrichment').pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public activateEnrichment(): Observable<TopologyResponse> {
    return this.http.get(this.url + '/enrichment/activate').pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public deactivateEnrichment(): Observable<TopologyResponse> {
    return this.http.get(this.url + '/enrichment/deactivate').pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public startEnrichment(): Observable<TopologyResponse> {
    return this.http.get(this.url + '/enrichment/start').pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public stopEnrichment(): Observable<TopologyResponse> {
    return this.http.get(this.url + '/enrichment/stop').pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public getIndexingStatus(): Observable<TopologyStatus> {
    return this.http.get(this.url + '/indexing').pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public activateIndexing(): Observable<TopologyResponse> {
    return this.http.get(this.url + '/indexing/activate').pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public deactivateIndexing(): Observable<TopologyResponse> {
    return this.http.get(this.url + '/indexing/deactivate').pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public startIndexing(): Observable<TopologyResponse> {
    return this.http.get(this.url + '/indexing/start').pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public stopIndexing(): Observable<TopologyResponse> {
    return this.http.get(this.url + '/indexing/stop').pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public getStatus(name: string): Observable<TopologyStatus> {
    return this.http.get(this.url + '/' + name).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public activateParser(name: string): Observable<TopologyResponse> {
    return this.http.get(this.url + '/parser/activate/' + name).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public deactivateParser(name: string): Observable<TopologyResponse> {
    return this.http.get(this.url + '/parser/deactivate/' + name).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public startParser(name: string): Observable<TopologyResponse> {
    return this.http.get(this.url + '/parser/start/' + name).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public stopParser(name: string): Observable<TopologyResponse> {
    return this.http.get(this.url + '/parser/stop/' + name).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }
}
