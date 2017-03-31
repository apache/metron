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
import {SensorEnrichmentConfigService} from './sensor-enrichment-config.service';
import {SensorEnrichmentConfig, EnrichmentConfig} from '../model/sensor-enrichment-config';
import {HttpModule, XHRBackend, Response, ResponseOptions, Http} from '@angular/http';
import '../rxjs-operators';
import {METRON_REST_CONFIG, APP_CONFIG} from '../app.config';
import {IAppConfig} from '../app.config.interface';

describe('SensorEnrichmentConfigService', () => {

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [HttpModule],
      providers: [
        SensorEnrichmentConfigService,
        {provide: XHRBackend, useClass: MockBackend},
        {provide: APP_CONFIG, useValue: METRON_REST_CONFIG}
      ]
    })
        .compileComponents();
  }));

  it('can instantiate service when inject service',
      inject([SensorEnrichmentConfigService], (service: SensorEnrichmentConfigService) => {
        expect(service instanceof SensorEnrichmentConfigService).toBe(true);
      }));

  it('can instantiate service with "new"', inject([Http, APP_CONFIG], (http: Http, config: IAppConfig) => {
    expect(http).not.toBeNull('http should be provided');
    let service = new SensorEnrichmentConfigService(http, config);
    expect(service instanceof SensorEnrichmentConfigService).toBe(true, 'new service should be ok');
  }));


  it('can provide the mockBackend as XHRBackend',
      inject([XHRBackend], (backend: MockBackend) => {
        expect(backend).not.toBeNull('backend should be provided');
      }));

  describe('when service functions', () => {
    let sensorEnrichmentConfigService: SensorEnrichmentConfigService;
    let mockBackend: MockBackend;
    let sensorEnrichmentConfig1 = new SensorEnrichmentConfig();
    let enrichmentConfig1 = new EnrichmentConfig();
    enrichmentConfig1.fieldMap = {'geo': ['ip_dst_addr'], 'host': ['ip_dst_addr']};
    sensorEnrichmentConfig1.enrichment.fieldMap = enrichmentConfig1;
    let sensorEnrichmentConfig2 = new SensorEnrichmentConfig();
    let enrichmentConfig2 = new EnrichmentConfig();
    enrichmentConfig1.fieldMap = {'whois': ['ip_dst_addr'], 'host': ['ip_src_addr']};
    sensorEnrichmentConfig2.enrichment = enrichmentConfig2;
    let availableEnrichments: string[] = ['geo', 'host', 'whois'];
    let sensorEnrichmentConfigResponse: Response;
    let sensorEnrichmentConfigsResponse: Response;
    let availableEnrichmentsResponse: Response;
    let deleteResponse: Response;

    beforeEach(inject([Http, XHRBackend, APP_CONFIG], (http: Http, be: MockBackend, config: IAppConfig) => {
      mockBackend = be;
      sensorEnrichmentConfigService = new SensorEnrichmentConfigService(http, config);
      sensorEnrichmentConfigResponse = new Response(new ResponseOptions({status: 200, body: sensorEnrichmentConfig1}));
      sensorEnrichmentConfigsResponse = new Response(new ResponseOptions({status: 200, body: [sensorEnrichmentConfig1,
        sensorEnrichmentConfig2]}));
      availableEnrichmentsResponse = new Response(new ResponseOptions({status: 200, body: availableEnrichments}));
      deleteResponse = new Response(new ResponseOptions({status: 200}));
    }));

    it('post', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(sensorEnrichmentConfigResponse));

      sensorEnrichmentConfigService.post('bro', sensorEnrichmentConfig1).subscribe(
          result => {
            expect(result).toEqual(sensorEnrichmentConfig1);
          }, error => console.log(error));
    })));

    it('get', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(sensorEnrichmentConfigResponse));

      sensorEnrichmentConfigService.get('bro').subscribe(
          result => {
            expect(result).toEqual(sensorEnrichmentConfig1);
          }, error => console.log(error));
    })));

    it('getAll', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(sensorEnrichmentConfigsResponse));

      sensorEnrichmentConfigService.getAll().subscribe(
          results => {
            expect(results).toEqual([sensorEnrichmentConfig1, sensorEnrichmentConfig2]);
          }, error => console.log(error));
    })));

    it('getAvailable', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(availableEnrichmentsResponse));

      sensorEnrichmentConfigService.getAvailable().subscribe(
          results => {
            expect(results).toEqual(availableEnrichments);
          }, error => console.log(error));
    })));

    it('deleteSensorEnrichments', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(deleteResponse));

      sensorEnrichmentConfigService.deleteSensorEnrichments('bro').subscribe(result => {
        expect(result.status).toEqual(200);
      });
    })));
  });

});


