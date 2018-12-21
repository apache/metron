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
import { APP_CONFIG, METRON_REST_CONFIG } from '../app.config';
import { GlobalConfigService } from './global-config.service';
import {
  HttpTestingController,
  HttpClientTestingModule
} from '@angular/common/http/testing';
import {AppConfigService} from './app-config.service';
import {MockAppConfigService} from './mock.app-config.service';

describe('GlobalConfigService', () => {
  let mockBackend: HttpTestingController;
  let globalConfigService: GlobalConfigService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        GlobalConfigService,
        { provide: AppConfigService, useClass: MockAppConfigService }
      ]
    });
    mockBackend = TestBed.get(HttpTestingController);
    globalConfigService = TestBed.get(GlobalConfigService);
  });

  afterEach(() => {
    mockBackend.verify();
  });

  describe('when service functions', () => {
    let globalConfig = { field: 'value' };

    it('post', () => {
      globalConfigService.post(globalConfig).subscribe(
        result => {
          expect(result).toEqual(globalConfig);
        },
        error => console.log(error)
      );

      const req = mockBackend.expectOne('/api/v1/global/config');
      expect(req.request.method).toBe('POST');
      req.flush(globalConfig);
    });

    it('get', () => {
      globalConfigService.get().subscribe(
        result => {
          expect(result).toEqual(globalConfig);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/global/config');
      expect(req.request.method).toBe('GET');
      req.flush(globalConfig);
    });

    it('deleteSensorParserConfigs', () => {
      globalConfigService.delete().subscribe(result => {
        expect(result.status).toEqual(200);
      });
    });
  });
});
