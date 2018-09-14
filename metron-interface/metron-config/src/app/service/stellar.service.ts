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
import {Observable} from 'rxjs/Observable';
import {SensorParserContext} from '../model/sensor-parser-context';
import {HttpUtil} from '../util/httpUtil';
import {StellarFunctionDescription} from '../model/stellar-function-description';
import {IAppConfig} from '../app.config.interface';
import {APP_CONFIG} from '../app.config';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/catch';

@Injectable()
export class StellarService {
  url = this.config.apiEndpoint + '/stellar';
  defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};

  constructor(private http: Http, @Inject(APP_CONFIG) private config: IAppConfig) {

  }

  public validateRules(rules: string[]): Observable<{}> {
    return this.http.post(this.url + '/validate/rules', JSON.stringify(rules),
      new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .catch(HttpUtil.handleError);
  }

  public validate(transformationValidation: SensorParserContext): Observable<{}> {
    return this.http.post(this.url + '/validate', JSON.stringify(transformationValidation),
      new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .catch(HttpUtil.handleError);
  }

  public list(): Observable<string[]> {
    return this.http.get(this.url + '/list', new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .catch(HttpUtil.handleError);
  }

  public listFunctions(): Observable<StellarFunctionDescription[]> {
    return this.http.get(this.url + '/list/functions', new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .catch(HttpUtil.handleError);
  }

  public listSimpleFunctions(): Observable<StellarFunctionDescription[]> {
    return this.http.get(this.url + '/list/simple/functions', new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .catch(HttpUtil.handleError);
  }

}
