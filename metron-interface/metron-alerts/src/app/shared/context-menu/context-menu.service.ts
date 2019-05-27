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
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { HttpUtil } from 'app/utils/httpUtil';
import { AppConfigService } from 'app/service/app-config.service';

export interface ContextMenuConfigModel {
  isEnabled: boolean,
  config: {}
}

@Injectable()
export class ContextMenuService {
  private cachedConfig$: BehaviorSubject<ContextMenuConfigModel>;

  constructor(
    private http: HttpClient,
    private appConfig: AppConfigService
    ) {}

  getConfig(): Observable<ContextMenuConfigModel> {
    if (!this.cachedConfig$) {
      this.cachedConfig$ = new BehaviorSubject(undefined);

      this.http.get(this.appConfig.getContextMenuConfigURL())
      .pipe(
        map(HttpUtil.extractData),
        catchError(HttpUtil.handleError)
      ).subscribe((result) => {
        if (this.validate(result)) {
          this.cachedConfig$.next(result);
        } else {
          console.error('Context menu configuration JSON is corrupt.');
        }
      });
    }

    return this.cachedConfig$;
  }

  private validate(jsonConfig: {}) {
    return jsonConfig.hasOwnProperty('isEnabled') && jsonConfig.hasOwnProperty('config');
  }
}
