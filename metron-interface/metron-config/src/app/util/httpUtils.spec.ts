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
import {HttpUtil} from './httpUtil';
import {Response, ResponseOptions, ResponseType} from '@angular/http';
import {Observable} from 'rxjs/Observable';
import {RestError} from '../model/rest-error';

describe('HttpUtil', () => {

  it('should create an instance', () => {
    expect(HttpUtil.handleError).toBeTruthy();
    expect(HttpUtil.extractString).toBeTruthy();
    expect(HttpUtil.extractData).toBeTruthy();
  });

  it('should handleError', () => {
    let error500: RestError = {message: 'This is error', responseCode: 500, fullMessage: 'This is error'};
    let responseOptions = new ResponseOptions();
    responseOptions.body = error500;
    let response = new Response(responseOptions);
    response.type = ResponseType.Basic;
    expect(HttpUtil.handleError(response)).toEqual(Observable.throw(error500));

    let error404 = new RestError();
    error404.responseCode = 404;
    response = new Response(new ResponseOptions());
    response.type = ResponseType.Basic;
    response.status = 404;
    expect(HttpUtil.handleError(response)).toEqual(Observable.throw(error404));
  });

});
