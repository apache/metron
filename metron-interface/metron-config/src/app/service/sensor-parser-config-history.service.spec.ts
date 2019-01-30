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
import { SensorParserConfig } from '../model/sensor-parser-config';
import { SensorParserConfigHistoryService } from './sensor-parser-config-history.service';
import { SensorParserConfigHistory } from '../model/sensor-parser-config-history';
import {
  HttpTestingController,
  HttpClientTestingModule
} from '@angular/common/http/testing';
import {AppConfigService} from './app-config.service';
import {MockAppConfigService} from './mock.app-config.service';

describe('SensorParserConfigHistoryService', () => {
  let mockBackend: HttpTestingController;
  let sensorParserConfigHistoryService: SensorParserConfigHistoryService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        SensorParserConfigHistoryService,
        { provide: AppConfigService, useClass: MockAppConfigService }
      ]
    });
    mockBackend = TestBed.get(HttpTestingController);
    sensorParserConfigHistoryService = TestBed.get(
      SensorParserConfigHistoryService
    );
  });

  afterAll(() => {
    mockBackend.verify();
  });

  describe('when service functions', () => {
    let sensorParserConfigHistory = new SensorParserConfigHistory();
    sensorParserConfigHistory.config = new SensorParserConfig();

    it('get', () => {
      sensorParserConfigHistoryService.get('bro').subscribe(
        result => {
          expect(result).toEqual(sensorParserConfigHistory);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/sensor/parser/config/bro');
      expect(req.request.method).toBe('GET');
      req.flush(sensorParserConfigHistory.config);
    });

    it('getAll', () => {
      sensorParserConfigHistoryService.getAll().subscribe(
        result => {
          expect(result).toEqual([sensorParserConfigHistory]);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/sensor/parser/config');
      expect(req.request.method).toBe('GET');
      req.flush([sensorParserConfigHistory.config]);
    });
  });
});
