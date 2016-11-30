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
import {async, inject, TestBed} from '@angular/core/testing';
import {MockBackend, MockConnection} from '@angular/http/testing';
import {TopologyStatus} from '../model/topology-status';
import {TopologyResponse} from '../model/topology-response';
import {HttpModule, XHRBackend, Response, ResponseOptions, Http} from '@angular/http';
import '../rxjs-operators';
import {APP_CONFIG, METRON_REST_CONFIG} from '../app.config';
import {IAppConfig} from '../app.config.interface';
import {StormService} from './storm.service';

describe('StormService', () => {

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [HttpModule],
      providers: [
        StormService,
        {provide: XHRBackend, useClass: MockBackend},
        {provide: APP_CONFIG, useValue: METRON_REST_CONFIG}
      ]
    })
        .compileComponents();
  }));

  it('can instantiate service when inject service',
      inject([StormService], (service: StormService) => {
        expect(service instanceof StormService).toBe(true);
      }));

  it('can instantiate service with "new"', inject([Http, APP_CONFIG], (http: Http, config: IAppConfig) => {
    expect(http).not.toBeNull('http should be provided');
    let service = new StormService(http, config);
    expect(service instanceof StormService).toBe(true, 'new service should be ok');
  }));


  it('can provide the mockBackend as XHRBackend',
      inject([XHRBackend], (backend: MockBackend) => {
        expect(backend).not.toBeNull('backend should be provided');
      }));

  describe('when service functions', () => {
    let stormService: StormService;
    let mockBackend: MockBackend;
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
    let startMessage: TopologyResponse = {status: 'success', message: 'STARTED'};
    let stopMessage: TopologyResponse = {status: 'success', message: 'STOPPED'};
    let activateMessage: TopologyResponse = {status: 'success', message: 'ACTIVE'};
    let deactivateMessage: TopologyResponse = {status: 'success', message: 'INACTIVE'};
    let allStatusesResponse: Response;
    let enrichmentStatusResponse: Response;
    let indexingStatusResponse: Response;
    let broStatusResponse: Response;
    let startResponse: Response;
    let stopResponse: Response;
    let activateResponse: Response;
    let deactivateResponse: Response;

    beforeEach(inject([Http, XHRBackend, APP_CONFIG], (http: Http, be: MockBackend, config: IAppConfig) => {
      mockBackend = be;
      stormService = new StormService(http, config);
      allStatusesResponse = new Response(new ResponseOptions({status: 200, body: allStatuses}));
      enrichmentStatusResponse = new Response(new ResponseOptions({status: 200, body: enrichmentStatus}));
      indexingStatusResponse = new Response(new ResponseOptions({status: 200, body: indexingStatus}));
      broStatusResponse = new Response(new ResponseOptions({status: 200, body: broStatus}));
      startResponse = new Response(new ResponseOptions({status: 200, body: startMessage}));
      stopResponse = new Response(new ResponseOptions({status: 200, body: stopMessage}));
      activateResponse = new Response(new ResponseOptions({status: 200, body: activateMessage}));
      deactivateResponse = new Response(new ResponseOptions({status: 200, body: deactivateMessage}));
    }));

    it('getAll', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(allStatusesResponse));

      stormService.getAll().subscribe(
          result => {
            expect(result).toEqual(allStatuses);
          }, error => console.log(error));
    })));

    it('getEnrichmentStatus', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(enrichmentStatusResponse));

      stormService.getEnrichmentStatus().subscribe(
          result => {
            expect(result).toEqual(enrichmentStatus);
          }, error => console.log(error));
    })));

    it('activateEnrichment', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(activateResponse));

      stormService.activateEnrichment().subscribe(
          result => {
            expect(result).toEqual(activateMessage);
          }, error => console.log(error));
    })));

    it('deactivateEnrichment', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(deactivateResponse));

      stormService.deactivateEnrichment().subscribe(
          result => {
            expect(result).toEqual(deactivateMessage);
          }, error => console.log(error));
    })));

    it('startEnrichment', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(startResponse));

      stormService.startEnrichment().subscribe(
          result => {
            expect(result).toEqual(startMessage);
          }, error => console.log(error));
    })));

    it('stopEnrichment', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(stopResponse));

      stormService.stopEnrichment().subscribe(
          result => {
            expect(result).toEqual(stopMessage);
          }, error => console.log(error));
    })));

    it('getIndexingStatus', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(indexingStatusResponse));

      stormService.getIndexingStatus().subscribe(
          result => {
            expect(result).toEqual(indexingStatus);
          }, error => console.log(error));
    })));

    it('activateIndexing', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(activateResponse));

      stormService.activateIndexing().subscribe(
          result => {
            expect(result).toEqual(activateMessage);
          }, error => console.log(error));
    })));

    it('deactivateIndexing', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(deactivateResponse));

      stormService.deactivateIndexing().subscribe(
          result => {
            expect(result).toEqual(deactivateMessage);
          }, error => console.log(error));
    })));

    it('startIndexing', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(startResponse));

      stormService.startIndexing().subscribe(
          result => {
            expect(result).toEqual(startMessage);
          }, error => console.log(error));
    })));

    it('stopIndexing', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(stopResponse));

      stormService.stopIndexing().subscribe(
          result => {
            expect(result).toEqual(stopMessage);
          }, error => console.log(error));
    })));

    it('getStatus', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(broStatusResponse));

      stormService.getStatus('bro').subscribe(
          result => {
            expect(result).toEqual(broStatus);
          }, error => console.log(error));
    })));

    it('activateParser', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(activateResponse));

      stormService.activateParser('bro').subscribe(
          result => {
            expect(result).toEqual(activateMessage);
          }, error => console.log(error));
    })));

    it('deactivateParser', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(deactivateResponse));

      stormService.deactivateParser('bro').subscribe(
          result => {
            expect(result).toEqual(deactivateMessage);
          }, error => console.log(error));
    })));

    it('startParser', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(startResponse));

      stormService.startParser('bro').subscribe(
          result => {
            expect(result).toEqual(startMessage);
          }, error => console.log(error));
    })));

    it('stopParser', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(stopResponse));

      stormService.stopParser('bro').subscribe(
          result => {
            expect(result).toEqual(stopMessage);
          }, error => console.log(error));
    })));



  });

});
