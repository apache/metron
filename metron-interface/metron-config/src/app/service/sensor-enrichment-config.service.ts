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
import {Http, Headers, RequestOptions, Response} from '@angular/http';
import {Observable} from 'rxjs/Observable';
import {SensorEnrichmentConfig} from '../model/sensor-enrichment-config';
import {HttpUtil} from '../util/httpUtil';
import {IAppConfig} from '../app.config.interface';
import {APP_CONFIG} from '../app.config';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/catch';

@Injectable()
export class SensorEnrichmentConfigService {
  url = this.config.apiEndpoint + '/sensor/enrichment/config';
  defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};

  constructor(private http: Http, @Inject(APP_CONFIG) private config: IAppConfig) {
  }

  public post(name: string, sensorEnrichmentConfig: SensorEnrichmentConfig): Observable<SensorEnrichmentConfig> {
    return this.http.post(this.url + '/' + name, JSON.stringify(sensorEnrichmentConfig),
                          new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .catch(HttpUtil.handleError);
  }

  public get(name: string): Observable<SensorEnrichmentConfig> {
    return this.http.get(this.url + '/' + name, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .catch(HttpUtil.handleError);
  }

  public getAll(): Observable<SensorEnrichmentConfig[]> {
    return this.http.get(this.url, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .catch(HttpUtil.handleError);
  }

  public deleteSensorEnrichments(name: string): Observable<Response> {
    return this.http.delete(this.url + '/' + name, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .catch(HttpUtil.handleError);
  }

  public getAvailableEnrichments(): Observable<string[]> {
    return this.http.get(this.url + '/list/available/enrichments', new RequestOptions({headers: new Headers(this.defaultHeaders)}))
        .map(HttpUtil.extractData)
        .catch(HttpUtil.handleError);
  }

  public getAvailableThreatTriageAggregators(): Observable<string[]> {
    return this.http.get(this.url + '/list/available/threat/triage/aggregators',
        new RequestOptions({headers: new Headers(this.defaultHeaders)}))
        .map(HttpUtil.extractData)
        .catch(HttpUtil.handleError);
  }

}
