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
import { HttpUtil } from './httpUtil';
import { RestError } from '../model/rest-error';
import { HttpErrorResponse } from '@angular/common/http';
import { noop } from 'rxjs';

describe('HttpUtil', () => {
  it('should create an instance', () => {
    expect(HttpUtil.handleError).toBeTruthy();
    expect(HttpUtil.extractString).toBeTruthy();
    expect(HttpUtil.extractData).toBeTruthy();
  });

  it('should handleError 500', () => {
    let error500: RestError = {
      message: 'This is error',
      status: 500,
      error: 'This is error'
    };
    let response = new HttpErrorResponse(error500);
    let httpUtilSub = HttpUtil.handleError(response).subscribe(
      noop,
      e => {
        expect(e.status).toBe(500);
        expect(e.message).toBe(
          'Http failure response for (unknown url): 500 undefined'
        );
        expect(e.error).toBe('This is error');
      },
      noop
    );
    httpUtilSub.unsubscribe();
  });

  it('should handleError 404', () => {
    const error404 = new RestError();
    error404.status = 404;
    const response = new HttpErrorResponse(error404);
    const httpUtilSub = HttpUtil.handleError(response).subscribe(
      noop,
      e => {
        expect(e.status).toBe(404);
        expect(e.message).toBe(undefined);
        expect(e.error).toBe(undefined);
      },
      noop
    );
    httpUtilSub.unsubscribe();
  });
});
