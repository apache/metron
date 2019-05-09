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
import { TopologyStatus } from '../model/topology-status';
import { TopologyResponse } from '../model/topology-response';
import { StormService } from './storm.service';
import {
  HttpTestingController,
  HttpClientTestingModule
} from '@angular/common/http/testing';
import {AppConfigService} from './app-config.service';
import {MockAppConfigService} from './mock.app-config.service';

describe('StormService', () => {
  let mockBackend: HttpTestingController;
  let stormService: StormService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        StormService,
        { provide: AppConfigService, useClass: MockAppConfigService }
      ]
    });
    mockBackend = TestBed.get(HttpTestingController);
    stormService = TestBed.get(StormService);
  });

  describe('when service functions', () => {
    let allStatuses: TopologyStatus[] = [];
    let broStatus = new TopologyStatus();
    broStatus.name = 'bro';
    broStatus.id = 'broid';
    broStatus.status = 'ACTIVE';
    allStatuses.push(broStatus);
    let enrichmentStatus = new TopologyStatus();
    enrichmentStatus.name = 'enrichment';
    enrichmentStatus.id = 'enrichmentid';
    enrichmentStatus.status = 'ACTIVE';
    allStatuses.push(enrichmentStatus);
    let indexingStatus = new TopologyStatus();
    indexingStatus.name = 'indexing';
    indexingStatus.id = 'indexingid';
    indexingStatus.status = 'ACTIVE';
    allStatuses.push(indexingStatus);
    let startMessage: TopologyResponse = {
      status: 'success',
      message: 'STARTED'
    };
    let stopMessage: TopologyResponse = {
      status: 'success',
      message: 'STOPPED'
    };
    let activateMessage: TopologyResponse = {
      status: 'success',
      message: 'ACTIVE'
    };
    let deactivateMessage: TopologyResponse = {
      status: 'success',
      message: 'INACTIVE'
    };

    it('getAll', () => {
      stormService.getAll().subscribe(
        result => {
          expect(result).toEqual(allStatuses);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/storm');
      expect(req.request.method).toBe('GET');
      req.flush(allStatuses);
    });

    it('getEnrichmentStatus', () => {
      stormService.getEnrichmentStatus().subscribe(
        result => {
          expect(result).toEqual(enrichmentStatus);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/storm/enrichment');
      expect(req.request.method).toBe('GET');
      req.flush(enrichmentStatus);
    });

    it('activateEnrichment', () => {
      stormService.activateEnrichment().subscribe(
        result => {
          expect(result).toEqual(activateMessage);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/storm/enrichment/activate');
      expect(req.request.method).toBe('GET');
      req.flush(activateMessage);
    });

    it('deactivateEnrichment', () => {
      stormService.deactivateEnrichment().subscribe(
        result => {
          expect(result).toEqual(deactivateMessage);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/storm/enrichment/deactivate');
      expect(req.request.method).toBe('GET');
      req.flush(deactivateMessage);
    });

    it('startEnrichment', () => {
      stormService.startEnrichment().subscribe(
        result => {
          expect(result).toEqual(startMessage);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/storm/enrichment/start');
      expect(req.request.method).toBe('GET');
      req.flush(startMessage);
    });

    it('stopEnrichment', () => {
      stormService.stopEnrichment().subscribe(
        result => {
          expect(result).toEqual(stopMessage);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/storm/enrichment/stop');
      expect(req.request.method).toBe('GET');
      req.flush(stopMessage);
    });

    it('getIndexingStatus', () => {
      stormService.getIndexingStatus().subscribe(
        result => {
          expect(result).toEqual(indexingStatus);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/storm/indexing');
      expect(req.request.method).toBe('GET');
      req.flush(indexingStatus);
    });

    it('activateIndexing', () => {
      stormService.activateIndexing().subscribe(
        result => {
          expect(result).toEqual(activateMessage);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/storm/indexing/activate');
      expect(req.request.method).toBe('GET');
      req.flush(activateMessage);
    });

    it('deactivateIndexing', () => {
      stormService.deactivateIndexing().subscribe(
        result => {
          expect(result).toEqual(deactivateMessage);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/storm/indexing/deactivate');
      expect(req.request.method).toBe('GET');
      req.flush(deactivateMessage);
    });

    it('startIndexing', () => {
      stormService.startIndexing().subscribe(
        result => {
          expect(result).toEqual(startMessage);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/storm/indexing/start');
      expect(req.request.method).toBe('GET');
      req.flush(startMessage);
    });

    it('stopIndexing', () => {
      stormService.stopIndexing().subscribe(
        result => {
          expect(result).toEqual(stopMessage);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/storm/indexing/stop');
      expect(req.request.method).toBe('GET');
      req.flush(stopMessage);
    });

    it('getStatus', () => {
      stormService.getStatus('bro').subscribe(
        result => {
          expect(result).toEqual(broStatus);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/storm/bro');
      expect(req.request.method).toBe('GET');
      req.flush(broStatus);
    });

    it('activateParser', () => {
      stormService.activateParser('bro').subscribe(
        result => {
          expect(result).toEqual(activateMessage);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/storm/parser/activate/bro');
      expect(req.request.method).toBe('GET');
      req.flush(activateMessage);
    });

    it('deactivateParser', () => {
      stormService.deactivateParser('bro').subscribe(
        result => {
          expect(result).toEqual(deactivateMessage);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/storm/parser/deactivate/bro');
      expect(req.request.method).toBe('GET');
      req.flush(deactivateMessage);
    });

    it('startParser', () => {
      stormService.startParser('bro').subscribe(
        result => {
          expect(result).toEqual(startMessage);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/storm/parser/start/bro');
      expect(req.request.method).toBe('GET');
      req.flush(startMessage);
    });

    it('stopParser', () => {
      stormService.stopParser('bro').subscribe(
        result => {
          expect(result).toEqual(stopMessage);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/storm/parser/stop/bro');
      expect(req.request.method).toBe('GET');
      req.flush(stopMessage);
    });
  });
});
