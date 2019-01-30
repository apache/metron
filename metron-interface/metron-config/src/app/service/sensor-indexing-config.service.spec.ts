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
import { HttpResponse } from '@angular/common/http';
import { SensorIndexingConfigService } from './sensor-indexing-config.service';
import { IndexingConfigurations } from '../model/sensor-indexing-config';
import {
  HttpClientTestingModule,
  HttpTestingController
} from '@angular/common/http/testing';
import {AppConfigService} from './app-config.service';
import {MockAppConfigService} from './mock.app-config.service';

describe('SensorIndexingConfigService', () => {
  let mockBackend: HttpTestingController;
  let sensorIndexingConfigService: SensorIndexingConfigService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        SensorIndexingConfigService,
        { provide: AppConfigService, useClass: MockAppConfigService }
      ]
    });
    mockBackend = TestBed.get(HttpTestingController);
    sensorIndexingConfigService = TestBed.get(SensorIndexingConfigService);
  });

  afterEach(() => {
    mockBackend.verify();
  });

  describe('when service functions', () => {
    let sensorIndexingConfig1 = new IndexingConfigurations();
    sensorIndexingConfig1.hdfs.index = 'squid';
    sensorIndexingConfig1.hdfs.batchSize = 1;
    let sensorIndexingConfig2 = new IndexingConfigurations();
    sensorIndexingConfig2.hdfs.index = 'yaf';
    sensorIndexingConfig2.hdfs.batchSize = 2;
    let deleteResponse: HttpResponse<{}>;

    beforeEach(() => {
      deleteResponse = new HttpResponse({ status: 200 });
    });

    it('post', () => {
      sensorIndexingConfigService
        .post('squid', sensorIndexingConfig1)
        .subscribe(
          result => {
            expect(result).toEqual(sensorIndexingConfig1);
          },
          error => console.log(error)
        );
      const req = mockBackend.expectOne('/api/v1/sensor/indexing/config/squid');
      expect(req.request.method).toBe('POST');
      req.flush(sensorIndexingConfig1);
    });

    it('get', () => {
      sensorIndexingConfigService.get('squid').subscribe(
        result => {
          expect(result).toEqual(sensorIndexingConfig1);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/sensor/indexing/config/squid');
      expect(req.request.method).toBe('GET');
      req.flush(sensorIndexingConfig1);
    });

    it('getAll', () => {
      sensorIndexingConfigService.getAll().subscribe(
        results => {
          expect(results).toEqual([
            sensorIndexingConfig1,
            sensorIndexingConfig2
          ]);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/sensor/indexing/config');
      expect(req.request.method).toBe('GET');
      req.flush([sensorIndexingConfig1, sensorIndexingConfig2]);
    });

    it('deleteSensorEnrichments', () => {
      sensorIndexingConfigService.deleteSensorIndexingConfig('squid').subscribe(
        result => {
          expect(result.status).toEqual(200);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/sensor/indexing/config/squid');
      expect(req.request.method).toBe('DELETE');
      req.flush(deleteResponse);
    });
  });
});
