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
import { TestBed } from '@angular/core/testing';

import { TimezoneConfigService } from './timezone-config.service';
import { SwitchComponent } from 'app/shared/switch/switch.component';
import { UserSettingsService } from 'app/service/user-settings.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { AppConfigService } from 'app/service/app-config.service';

describe('TimezoneConfigService', () => {
  let service: TimezoneConfigService;
  let userSettingsService: UserSettingsService;

  beforeEach(() => {

    TestBed.configureTestingModule({
      imports: [ HttpClientTestingModule ],
      declarations: [ SwitchComponent ],
      providers: [
        {
          provide: AppConfigService,
          useValue: {
            getApiRoot() { return ''; }
          }
        },
        UserSettingsService,
        TimezoneConfigService
      ]
    });

    spyOn(TimezoneConfigService.prototype, 'toggleUTCtoLocal').and.callThrough();
    spyOn(UserSettingsService.prototype, 'get').and.callThrough();

    service = TestBed.get(TimezoneConfigService);
    userSettingsService = TestBed.get(UserSettingsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get persisted state', () => {
    expect(userSettingsService.get).toHaveBeenCalledWith(service.CONVERT_UTC_TO_LOCAL_KEY);
  });

  it('should return the current timezone configuration with getTimezoneConfig()', () => {
    expect(service.getTimezoneConfig()).toBe(false);
    service.showLocal = true;
    expect(service.getTimezoneConfig()).toBe(true);
  });
});
