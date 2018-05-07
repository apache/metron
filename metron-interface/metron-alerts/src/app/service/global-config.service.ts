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
import {Http, Headers, RequestOptions, Response, ResponseOptions} from '@angular/http';
import {Observable} from 'rxjs/Observable';
import {HttpUtil} from '../utils/httpUtil';

@Injectable()
export class GlobalConfigService {
  url = 'api/v1/global/config';
  defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};

  private globalConfig = {};

  constructor(private http: Http) {}

  public get(): Observable<{}> {
    return this.http.get(this.url , new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map((res: Response): any => {
        let body = res.json();
        this.setDefaultSourceType(body);
        return body || {};
      })
      .catch(HttpUtil.handleError);
  }

  private setDefaultSourceType(globalConfig) {
    let sourceType: {} = {};
    if(!globalConfig['source.type.field']) {
      sourceType = Object.assign({}, globalConfig, {'source.type.field': 'source:type'});
      return sourceType;
    }
  }

}
