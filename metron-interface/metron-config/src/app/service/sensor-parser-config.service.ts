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
import { Observable, Subject } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { SensorParserConfig } from '../model/sensor-parser-config';
import { HttpUtil } from '../util/httpUtil';
import { ParseMessageRequest } from '../model/parse-message-request';
import { RestError } from '../model/rest-error';
import { IAppConfig } from '../app.config.interface';
import { APP_CONFIG } from '../app.config';
import { ParserGroupModel } from 'app/model/parser-group';
import { SensorParserConfigHistory } from 'app/model/sensor-parser-config-history';

@Injectable()
export class SensorParserConfigService {
  parserConfigEndpoint = this.config.apiEndpoint + '/sensor/parser/config';
  parserGroupEndpoint = this.config.apiEndpoint + '/sensor/parser/config';
  selectedSensorParserConfig: SensorParserConfig;

  dataChangedSource = new Subject<string[]>();
  dataChanged$ = this.dataChangedSource.asObservable();

  private groups: Observable<ParserGroupModel[]>;
  private parsers: Observable<SensorParserConfigHistory[]>;
  public groupsAndParsers: Observable<{}>;

  constructor(
    private http: HttpClient,
    @Inject(APP_CONFIG) private config: IAppConfig
  ) {
    this.getParserGroups();
    this.groups.subscribe();
  }

  private getParserGroups() {
    function extractParserGroups(rawJsonArray) {
      return rawJsonArray.map(group => new ParserGroupModel(group));
    }

    this.groups = this.http.get(this.parserGroupEndpoint).pipe(
      map(extractParserGroups),
      catchError(HttpUtil.handleError)
    );
  }

  public post(
    name: string,
    sensorParserConfig: SensorParserConfig
  ): Observable<SensorParserConfig> {
    return this.http
      .post(this.parserConfigEndpoint + '/' + name, JSON.stringify(sensorParserConfig))
      .pipe(
        map(HttpUtil.extractData),
        catchError(HttpUtil.handleError)
      );
  }

  public get(name: string): Observable<SensorParserConfig> {
    return this.http.get(this.parserConfigEndpoint + '/' + name).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public getAll(): Observable<{}> {
    return this.http.get(this.parserConfigEndpoint).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public deleteSensorParserConfig(
    name: string
  ): Observable<Object | RestError> {
    return this.http
      .delete(this.parserConfigEndpoint + '/' + name)
      .pipe(catchError(HttpUtil.handleError));
  }

  public getAvailableParsers(): Observable<{}> {
    return this.http.get(this.parserConfigEndpoint + '/list/available').pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public parseMessage(
    parseMessageRequest: ParseMessageRequest
  ): Observable<{}> {
    return this.http.post(this.parserConfigEndpoint + '/parseMessage', parseMessageRequest).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public deleteSensorParserConfigs(
    sensorNames: string[]
  ): Observable<{ success: Array<string>; failure: Array<string> }> {
    let result: { success: Array<string>; failure: Array<string> } = {
      success: [],
      failure: []
    };
    let observable = Observable.create(observer => {
      let completed = () => {
        if (observer) {
          observer.next(result);
          observer.complete();
        }

        this.dataChangedSource.next(sensorNames);
      };
      for (let i = 0; i < sensorNames.length; i++) {
        this.deleteSensorParserConfig(sensorNames[i]).subscribe(
          results => {
            result.success.push(sensorNames[i]);
            if (
              result.success.length + result.failure.length ===
              sensorNames.length
            ) {
              completed();
            }
          },
          error => {
            result.failure.push(sensorNames[i]);
            if (
              result.success.length + result.failure.length ===
              sensorNames.length
            ) {
              completed();
            }
          }
        );
      }
    });

    return observable;
  }
}
