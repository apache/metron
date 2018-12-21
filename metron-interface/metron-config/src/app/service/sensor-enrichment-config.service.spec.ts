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
import { SensorEnrichmentConfigService } from './sensor-enrichment-config.service';
import {
  SensorEnrichmentConfig,
  EnrichmentConfig
} from '../model/sensor-enrichment-config';
import { HttpResponse } from '@angular/common/http';
import { METRON_REST_CONFIG, APP_CONFIG } from '../app.config';
import {
  HttpClientTestingModule,
  HttpTestingController
} from '@angular/common/http/testing';
import {AppConfigService} from './app-config.service';
import {MockAppConfigService} from './mock.app-config.service';

describe('SensorEnrichmentConfigService', () => {
  let mockBackend: HttpTestingController;
  let sensorEnrichmentConfigService: SensorEnrichmentConfigService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        SensorEnrichmentConfigService,
        { provide: AppConfigService, useClass: MockAppConfigService }
      ]
    });
    mockBackend = TestBed.get(HttpTestingController);
    sensorEnrichmentConfigService = TestBed.get(SensorEnrichmentConfigService);
  });

  afterEach(() => {
    mockBackend.verify();
  });

  describe('when service functions', () => {
    let sensorEnrichmentConfig1 = new SensorEnrichmentConfig();
    let enrichmentConfig1 = new EnrichmentConfig();
    enrichmentConfig1.fieldMap = {
      geo: ['ip_dst_addr'],
      host: ['ip_dst_addr']
    };
    sensorEnrichmentConfig1.enrichment.fieldMap = enrichmentConfig1;
    let sensorEnrichmentConfig2 = new SensorEnrichmentConfig();
    let enrichmentConfig2 = new EnrichmentConfig();
    enrichmentConfig1.fieldMap = {
      whois: ['ip_dst_addr'],
      host: ['ip_src_addr']
    };
    sensorEnrichmentConfig2.enrichment = enrichmentConfig2;
    let availableEnrichments: string[] = ['geo', 'host', 'whois'];
    let availableThreatTriageAggregators: string[] = [
      'MAX',
      'MIN',
      'SUM',
      'MEAN',
      'POSITIVE_MEAN'
    ];
    let sensorEnrichmentConfigResponse: HttpResponse<{}>;
    let sensorEnrichmentConfigsResponse: HttpResponse<{}>;
    let availableEnrichmentsResponse: HttpResponse<{}>;
    let availableThreatTriageAggregatorsResponse: HttpResponse<{}>;
    let deleteResponse: HttpResponse<{}>;

    beforeEach(() => {
      sensorEnrichmentConfigResponse = new HttpResponse({
        status: 200,
        body: sensorEnrichmentConfig1
      });
      sensorEnrichmentConfigsResponse = new HttpResponse({
        status: 200,
        body: [sensorEnrichmentConfig1, sensorEnrichmentConfig2]
      });
      availableEnrichmentsResponse = new HttpResponse({
        status: 200,
        body: availableEnrichments
      });
      availableThreatTriageAggregatorsResponse = new HttpResponse({
        status: 200,
        body: availableThreatTriageAggregators
      });
      deleteResponse = new HttpResponse({ status: 200 });
    });

    it('post', () => {
      sensorEnrichmentConfigService
        .post('bro', sensorEnrichmentConfig1)
        .subscribe(
          result => {
            expect(result).toEqual(sensorEnrichmentConfig1);
          },
          error => console.log(error)
        );
      const req = mockBackend.expectOne('/api/v1/sensor/enrichment/config/bro');
      expect(req.request.method).toBe('POST');
      req.flush(sensorEnrichmentConfig1);
    });

    it('get', () => {
      sensorEnrichmentConfigService.get('bro').subscribe(
        result => {
          expect(result).toEqual(sensorEnrichmentConfig1);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/sensor/enrichment/config/bro');
      expect(req.request.method).toBe('GET');
      req.flush(sensorEnrichmentConfig1);
    });

    it('getAll', () => {
      sensorEnrichmentConfigService.getAll().subscribe(
        results => {
          expect(results).toEqual([
            sensorEnrichmentConfig1,
            sensorEnrichmentConfig2
          ]);
        },
        error => console.log(error)
      );

      const req = mockBackend.expectOne('/api/v1/sensor/enrichment/config');
      expect(req.request.method).toBe('GET');
      req.flush([sensorEnrichmentConfig1, sensorEnrichmentConfig2]);
    });

    it('getAvailableEnrichments', () => {
      sensorEnrichmentConfigService.getAvailableEnrichments().subscribe(
        results => {
          expect(results).toEqual(availableEnrichments);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne(
        '/api/v1/sensor/enrichment/config/list/available/enrichments'
      );
      expect(req.request.method).toBe('GET');
      req.flush(availableEnrichments);
    });

    it('getAvailableThreatTriageAggregators', () => {
      sensorEnrichmentConfigService
        .getAvailableThreatTriageAggregators()
        .subscribe(
          results => {
            expect(results).toEqual(availableThreatTriageAggregators);
          },
          error => console.log(error)
        );
      const req = mockBackend.expectOne(
        '/api/v1/sensor/enrichment/config/list/available/threat/triage/aggregators'
      );
      expect(req.request.method).toBe('GET');
      req.flush(availableThreatTriageAggregators);
    });

    it('deleteSensorEnrichments', () => {
      sensorEnrichmentConfigService
        .deleteSensorEnrichments('bro')
        .subscribe(result => {
          expect(result.status).toEqual(200);
        });

      const req = mockBackend.expectOne('/api/v1/sensor/enrichment/config/bro');
      expect(req.request.method).toBe('DELETE');
      req.flush(deleteResponse);
    });
  });
});
