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
import {Response} from '@angular/http';
import {Observable}     from 'rxjs/Observable';
import {RestError} from '../model/rest-error';

export class HttpUtil {

  public static extractString(res: Response): string {
    let text: string = res.text();
    return text || '';
  }

  public static extractData(res: Response): any {
    let body = res.json();
    return body || {};
  }

  public static handleError(res: Response): Observable<RestError> {
    // In a real world app, we might use a remote logging infrastructure
    // We'd also dig deeper into the error to get a better message
    let restError: RestError;
    if (res.status === 401) {
      window.location.assign('/login?sessionExpired=true');
    } else if (res.status !== 404) {
      restError = res.json();
    } else {
      restError = new RestError();
      restError.responseCode = 404;
    }
    return Observable.throw(restError);
  }
}
