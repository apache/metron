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
import {Injectable, Inject}     from '@angular/core';
import {Http, Headers, RequestOptions, Response} from '@angular/http';
import {Observable}     from 'rxjs/Observable';
import {SensorParserConfig} from '../model/sensor-parser-config';
import {HttpUtil} from '../util/httpUtil';
import {Subject}    from 'rxjs/Subject';
import {ParseMessageRequest} from '../model/parse-message-request';
import {IAppConfig} from '../app.config.interface';
import {APP_CONFIG} from '../app.config';

@Injectable()
export class SensorParserConfigService {
  url = this.config.apiEndpoint + '/sensor/parser/config';
  defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};
  selectedSensorParserConfig: SensorParserConfig;

  dataChangedSource = new Subject<SensorParserConfig[]>();
  dataChanged$ = this.dataChangedSource.asObservable();

  constructor(private http: Http, @Inject(APP_CONFIG) private config: IAppConfig) {

  }

  public post(sensorParserConfig: SensorParserConfig): Observable<SensorParserConfig> {
    return this.http.post(this.url, JSON.stringify(sensorParserConfig), new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .catch(HttpUtil.handleError);
  }

  public get(name: string): Observable<SensorParserConfig> {
    return this.http.get(this.url + '/' + name, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .catch(HttpUtil.handleError);
  }

  public getAll(): Observable<SensorParserConfig[]> {
    return this.http.get(this.url, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .catch(HttpUtil.handleError);
  }

  public deleteSensorParserConfig(name: string): Observable<Response> {
    return this.http.delete(this.url + '/' + name, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .catch(HttpUtil.handleError);
  }

  public getAvailableParsers(): Observable<{}> {
    return this.http.get(this.url + '/list/available', new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .catch(HttpUtil.handleError);
  }

  public parseMessage(parseMessageRequest: ParseMessageRequest): Observable<{}> {
    return this.http.post(this.url + '/parseMessage', parseMessageRequest, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .catch(HttpUtil.handleError);
  }

  public deleteSensorParserConfigs(sensors: SensorParserConfig[]): Observable<{success: Array<string>, failure: Array<string>}> {
    let result: {success: Array<string>, failure: Array<string>} = {success: [], failure: []};
    let observable = Observable.create((observer => {

      let completed = () => {
        if (observer) {
          observer.next(result);
          observer.complete();
        }

        this.dataChangedSource.next(sensors);
      };

      for (let i = 0; i < sensors.length; i++) {
        this.deleteSensorParserConfig(sensors[i].sensorTopic).subscribe(results => {
          result.success.push(sensors[i].sensorTopic);
          if (result.success.length + result.failure.length === sensors.length) {
            completed();
          }
        }, error => {
          result.failure.push(sensors[i].sensorTopic);
          if (result.success.length + result.failure.length === sensors.length) {
            completed();
          }
        });
      }

    }));

    return observable;
  }

  public setSeletedSensor(sensor: SensorParserConfig): void {
    this.selectedSensorParserConfig = sensor;
  }

  public getSelectedSensor(): SensorParserConfig {
    return this.selectedSensorParserConfig;
  }

}
