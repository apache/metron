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
import {Injectable, Inject} from '@angular/core';
import {Http, Headers, RequestOptions} from '@angular/http';
import {HttpUtil} from '../util/httpUtil';
import {TopologyStatus} from '../model/topology-status';
import {TopologyResponse} from '../model/topology-response';
import {APP_CONFIG} from '../app.config';
import {IAppConfig} from '../app.config.interface';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/observable/interval';
import 'rxjs/add/operator/switchMap';
import 'rxjs/add/operator/onErrorResumeNext';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/catch';

@Injectable()
export class StormService {
  url = this.config.apiEndpoint + '/storm';
  defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};

  constructor(private http: Http, @Inject(APP_CONFIG) private config: IAppConfig) {

  }

  public pollGetAll(): Observable<TopologyStatus[]> {
    return Observable.interval(8000).switchMap(() => {
      return this.http.get(this.url, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
          .map(HttpUtil.extractData)
          .catch(HttpUtil.handleError)
          .onErrorResumeNext();
    });
  }

  public getAll(): Observable<TopologyStatus[]> {
    return this.http.get(this.url, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
        .map(HttpUtil.extractData)
        .catch(HttpUtil.handleError);
  }

  public getEnrichmentStatus(): Observable<TopologyStatus> {
    return this.http.get(this.url + '/enrichment', new RequestOptions({headers: new Headers(this.defaultHeaders)}))
        .map(HttpUtil.extractData)
        .catch(HttpUtil.handleError);
  }

  public activateEnrichment(): Observable<TopologyResponse> {
    return this.http.get(this.url + '/enrichment/activate', new RequestOptions({headers: new Headers(this.defaultHeaders)}))
        .map(HttpUtil.extractData)
        .catch(HttpUtil.handleError);
  }

  public deactivateEnrichment(): Observable<TopologyResponse> {
    return this.http.get(this.url + '/enrichment/deactivate', new RequestOptions({headers: new Headers(this.defaultHeaders)}))
        .map(HttpUtil.extractData)
        .catch(HttpUtil.handleError);
  }

  public startEnrichment(): Observable<TopologyResponse> {
    return this.http.get(this.url + '/enrichment/start', new RequestOptions({headers: new Headers(this.defaultHeaders)}))
        .map(HttpUtil.extractData)
        .catch(HttpUtil.handleError);
  }

  public stopEnrichment(): Observable<TopologyResponse> {
    return this.http.get(this.url + '/enrichment/stop', new RequestOptions({headers: new Headers(this.defaultHeaders)}))
        .map(HttpUtil.extractData)
        .catch(HttpUtil.handleError);
  }

  public getIndexingStatus(): Observable<TopologyStatus> {
    return this.http.get(this.url + '/indexing', new RequestOptions({headers: new Headers(this.defaultHeaders)}))
        .map(HttpUtil.extractData)
        .catch(HttpUtil.handleError);
  }

  public activateIndexing(): Observable<TopologyResponse> {
    return this.http.get(this.url + '/indexing/activate', new RequestOptions({headers: new Headers(this.defaultHeaders)}))
        .map(HttpUtil.extractData)
        .catch(HttpUtil.handleError);
  }

  public deactivateIndexing(): Observable<TopologyResponse> {
    return this.http.get(this.url + '/indexing/deactivate', new RequestOptions({headers: new Headers(this.defaultHeaders)}))
        .map(HttpUtil.extractData)
        .catch(HttpUtil.handleError);
  }

  public startIndexing(): Observable<TopologyResponse> {
    return this.http.get(this.url + '/indexing/start', new RequestOptions({headers: new Headers(this.defaultHeaders)}))
        .map(HttpUtil.extractData)
        .catch(HttpUtil.handleError);
  }

  public stopIndexing(): Observable<TopologyResponse> {
    return this.http.get(this.url + '/indexing/stop', new RequestOptions({headers: new Headers(this.defaultHeaders)}))
        .map(HttpUtil.extractData)
        .catch(HttpUtil.handleError);
  }

  public getStatus(name: string): Observable<TopologyStatus> {
    return this.http.get(this.url + '/' + name, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
        .map(HttpUtil.extractData)
        .catch(HttpUtil.handleError);
  }

  public activateParser(name: string): Observable<TopologyResponse> {
    return this.http.get(this.url + '/parser/activate/' + name, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
        .map(HttpUtil.extractData)
        .catch(HttpUtil.handleError);
  }

  public deactivateParser(name: string): Observable<TopologyResponse> {
    return this.http.get(this.url + '/parser/deactivate/' + name, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
        .map(HttpUtil.extractData)
        .catch(HttpUtil.handleError);
  }

  public startParser(name: string): Observable<TopologyResponse> {
    return this.http.get(this.url + '/parser/start/' + name, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
        .map(HttpUtil.extractData)
        .catch(HttpUtil.handleError);
  }

  public stopParser(name: string): Observable<TopologyResponse> {
    return this.http.get(this.url + '/parser/stop/' + name, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
        .map(HttpUtil.extractData)
        .catch(HttpUtil.handleError);
  }

}
