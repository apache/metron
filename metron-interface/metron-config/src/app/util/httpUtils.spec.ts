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
import {Response} from '@angular/http';
import {Observable} from 'rxjs/Observable';
import {RestError} from "../model/rest-error";

describe('HttpUtil', () => {

  it('should create an instance', () => {
    expect(HttpUtil.handleError).toBeTruthy();
    expect(HttpUtil.extractString).toBeTruthy();
    expect(HttpUtil.extractData).toBeTruthy();
    expect(HttpUtil.getErrorMessageFromBody).toBeTruthy();
  });

  // it('should handleError', () => {
  //   spyOn(console, 'error');
  //   let error: RestError = {message: 'This is error', responseCode: 500, fullMessage: 'This is error'};
  //   let response = {'_body': JSON.stringify({'message': 'This is error'})};
  //   expect(HttpUtil.handleError(response)).toEqual(Observable.throw(error));
  //   expect(console.error).toHaveBeenCalledWith('This is error');
  //
  //   expect(HttpUtil.handleError({})).toEqual(Observable.throw({}));
  //   expect(console.error).toHaveBeenCalledWith('Server error');
  // });
  //
  // it('should getErrorMessageFromBody', () => {
  //   let error = {'_body': JSON.stringify({'message': 'This is error'})};
  //   expect(HttpUtil.getErrorMessageFromBody(error)).toEqual('This is error');
  //
  //   error = {'_body': 'abc'};
  //   expect(HttpUtil.getErrorMessageFromBody(error)).toEqual({ _body: 'abc' });
  // });

});
