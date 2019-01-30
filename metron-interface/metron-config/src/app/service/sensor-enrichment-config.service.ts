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
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { SensorEnrichmentConfig } from '../model/sensor-enrichment-config';
import { HttpUtil } from '../util/httpUtil';
import {AppConfigService} from './app-config.service';

@Injectable()
export class SensorEnrichmentConfigService {
  url = this.appConfigService.getApiRoot() + '/sensor/enrichment/config';

  constructor(
    private http: HttpClient,
    private appConfigService: AppConfigService
  ) {}

  public post(
    name: string,
    sensorEnrichmentConfig: SensorEnrichmentConfig
  ): Observable<SensorEnrichmentConfig> {
    return this.http
      .post(this.url + '/' + name, JSON.stringify(sensorEnrichmentConfig))
      .pipe(
        map(HttpUtil.extractData),
        catchError(HttpUtil.handleError)
      );
  }

  public get(name: string): Observable<SensorEnrichmentConfig> {
    return this.http.get(this.url + '/' + name).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public getAll(): Observable<SensorEnrichmentConfig[]> {
    return this.http.get(this.url).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public deleteSensorEnrichments(name: string) {
    return this.http
      .delete<Observable<{}>>(this.url + '/' + name)
      .pipe<HttpResponse<{}>>(catchError(HttpUtil.handleError));
  }

  public getAvailableEnrichments(): Observable<string[]> {
    return this.http.get(this.url + '/list/available/enrichments').pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public getAvailableThreatTriageAggregators(): Observable<string[]> {
    return this.http
      .get(this.url + '/list/available/threat/triage/aggregators')
      .pipe(
        map(HttpUtil.extractData),
        catchError(HttpUtil.handleError)
      );
  }
}
