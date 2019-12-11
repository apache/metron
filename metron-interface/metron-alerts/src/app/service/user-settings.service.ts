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
import {Injectable} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AppConfigService } from './app-config.service';
import { Observable } from 'rxjs';

let settings = {};

@Injectable()
export class UserSettingsService {

  constructor(
    private http: HttpClient,
    private appConfigService: AppConfigService
  ) {}

  load(): Promise<any> {
    return this.http.get(this.appConfigService.getApiRoot() + '/hdfs?path=user-settings')
      .toPromise()
      .then((result) => {
        settings = result;
      })
      .catch(() => ({}));
  }

  save(data: any): Observable<any> {
    const newSettings = {
      ...settings,
      ...data
    };
    settings = newSettings;
    return this.http.post(this.appConfigService.getApiRoot() + '/hdfs?path=user-settings', newSettings);
  }

  get(key: string): Observable<any> {
    return new Observable((observer) => {
      observer.next(settings[key]);
      observer.complete();
    });

  }
}
