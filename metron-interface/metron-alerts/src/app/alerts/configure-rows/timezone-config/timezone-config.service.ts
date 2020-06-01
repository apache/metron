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
import { UserSettingsService } from 'app/service/user-settings.service';

@Injectable({
  providedIn: 'root'
})
export class TimezoneConfigService {

  public readonly CONVERT_UTC_TO_LOCAL_KEY = 'convertUTCtoLocal';

  showLocal = false;

  constructor(private userSettingsService: UserSettingsService) {
    this.userSettingsService.get(this.CONVERT_UTC_TO_LOCAL_KEY)
      .subscribe((showLocal) => {
        this.showLocal = !!showLocal;
      });
  }

  toggleUTCtoLocal(isLocal: boolean) {
    this.showLocal = isLocal;
    this.userSettingsService.save({
      [this.CONVERT_UTC_TO_LOCAL_KEY]: isLocal
    }).subscribe();
  }

  getTimezoneConfig() {
    return this.showLocal;
  }
}
