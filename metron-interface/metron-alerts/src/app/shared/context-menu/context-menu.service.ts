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
import { DynamicMenuItem } from './dynamic-item.model';

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
      const defaultConfig = { isEnabled: false, config: {} };

      this.cachedConfig$ = new BehaviorSubject(undefined);

      this.http.get(this.appConfig.getContextMenuConfigURL())
      .pipe(
        map(HttpUtil.extractData),
        catchError(HttpUtil.handleError)
      ).subscribe((result) => {
        if (this.validate(result)) {
          this.cachedConfig$.next(result);
        } else {
          this.cachedConfig$.next(defaultConfig);
        }
      });
    }

    return this.cachedConfig$;
  }

  private validate(configJson: ContextMenuConfigModel) {

    if (!configJson.hasOwnProperty('isEnabled') || !configJson.hasOwnProperty('config')) {
      console.error('[Context Menu] CONFIG: isEnabled and/or config entries are missing.')
      return false;
    }

    if (configJson.isEnabled !== true && configJson.isEnabled !== false) {
      console.error('[Context Menu] CONFIG: isEnabled has to be a boolean. Defaulting to false.');
      return false;
    }

    if (typeof configJson.config !== 'object' || Array.isArray(configJson.config)) {
      console.error('[Context Menu] CONFIG: Config entry has to be an object. Defaulting to {}.');
      return false;
    }

    return Object.keys(configJson.config).every((key) => {
      if (!Array.isArray(configJson.config[key])) {
        console.error('[Context Menu] CONFIG: Each item in config object has to be an array.')
        return false;
      }

      return configJson.config[key].every((menuItem) => {
        if (!DynamicMenuItem.isConfigValid(menuItem)) {
          console.error(`[Context Menu] CONFIG: Entry is invalid: ${JSON.stringify(menuItem)}`);
          return false;
        }
        return true;
      });
    })
  }
}
