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
import {HttpUtil} from '../util/httpUtil';
import {IAppConfig} from '../app.config.interface';
import {SensorParserConfigHistory} from '../model/sensor-parser-config-history';
import {APP_CONFIG} from '../app.config';
import {SensorParserConfig} from '../model/sensor-parser-config';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/catch';

@Injectable()
export class SensorParserConfigHistoryService {
  url =  this.config.apiEndpoint + '/sensor/parser/config';
  defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};

  constructor(private http: Http, @Inject(APP_CONFIG) private config: IAppConfig) {

  }

  public get(name: string): Observable<SensorParserConfigHistory> {
    return this.http.get(this.url + '/' + name, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map((response: Response) => {
        let sensorParserConfigHistory = new SensorParserConfigHistory();
        sensorParserConfigHistory.config = response.json();
        return sensorParserConfigHistory;
      })
      .catch(HttpUtil.handleError);
  }

  public getAll(): Observable<SensorParserConfigHistory[]> {
    return this.http.get(this.url, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map((response: Response) => {
        let sensorParserConfigHistoryArray = [];
        let sensorParserConfigs: SensorParserConfig[] = response.json();
        for (let sensorParserConfig of sensorParserConfigs) {
          let sensorParserConfigHistory = new SensorParserConfigHistory();
          sensorParserConfigHistory.config = sensorParserConfig;
          sensorParserConfigHistoryArray.push(sensorParserConfigHistory);
        }
        return sensorParserConfigHistoryArray;
      })
      .catch(HttpUtil.handleError);
  }

}
