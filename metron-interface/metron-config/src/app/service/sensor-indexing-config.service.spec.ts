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
import {HttpModule, XHRBackend, Response, ResponseOptions, Http} from '@angular/http';
import '../rxjs-operators';
import {METRON_REST_CONFIG, APP_CONFIG} from '../app.config';
import {IAppConfig} from '../app.config.interface';
import {SensorIndexingConfigService} from './sensor-indexing-config.service';
import {SensorIndexingConfig} from '../model/sensor-indexing-config';

describe('SensorIndexingConfigService', () => {

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [HttpModule],
      providers: [
        SensorIndexingConfigService,
        {provide: XHRBackend, useClass: MockBackend},
        {provide: APP_CONFIG, useValue: METRON_REST_CONFIG}
      ]
    })
        .compileComponents();
  }));

  it('can instantiate service when inject service',
      inject([SensorIndexingConfigService], (service: SensorIndexingConfigService) => {
        expect(service instanceof SensorIndexingConfigService).toBe(true);
      }));

  it('can instantiate service with "new"', inject([Http, APP_CONFIG], (http: Http, config: IAppConfig) => {
    expect(http).not.toBeNull('http should be provided');
    let service = new SensorIndexingConfigService(http, config);
    expect(service instanceof SensorIndexingConfigService).toBe(true, 'new service should be ok');
  }));


  it('can provide the mockBackend as XHRBackend',
      inject([XHRBackend], (backend: MockBackend) => {
        expect(backend).not.toBeNull('backend should be provided');
      }));

  describe('when service functions', () => {
    let sensorIndexingConfigService: SensorIndexingConfigService;
    let mockBackend: MockBackend;
    let sensorIndexingConfig1 = new SensorIndexingConfig();
    sensorIndexingConfig1.index = 'squid';
    sensorIndexingConfig1.batchSize = 1;
    let sensorIndexingConfig2 = new SensorIndexingConfig();
    sensorIndexingConfig2.index = 'yaf';
    sensorIndexingConfig2.batchSize = 2;
    let sensorIndexingConfigResponse: Response;
    let sensorIndexingConfigsResponse: Response;
    let deleteResponse: Response;

    beforeEach(inject([Http, XHRBackend, APP_CONFIG], (http: Http, be: MockBackend, config: IAppConfig) => {
      mockBackend = be;
      sensorIndexingConfigService = new SensorIndexingConfigService(http, config);
      sensorIndexingConfigResponse = new Response(new ResponseOptions({status: 200, body: sensorIndexingConfig1}));
      sensorIndexingConfigsResponse = new Response(new ResponseOptions({status: 200, body: [sensorIndexingConfig1,
        sensorIndexingConfig2]}));
      deleteResponse = new Response(new ResponseOptions({status: 200}));
    }));

    it('post', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(sensorIndexingConfigResponse));

      sensorIndexingConfigService.post('squid', sensorIndexingConfig1).subscribe(
          result => {
            expect(result).toEqual(sensorIndexingConfig1);
          }, error => console.log(error));
    })));

    it('get', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(sensorIndexingConfigResponse));

      sensorIndexingConfigService.get('squid').subscribe(
          result => {
            expect(result).toEqual(sensorIndexingConfig1);
          }, error => console.log(error));
    })));

    it('getAll', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(sensorIndexingConfigsResponse));

      sensorIndexingConfigService.getAll().subscribe(
          results => {
            expect(results).toEqual([sensorIndexingConfig1, sensorIndexingConfig2]);
          }, error => console.log(error));
    })));

    it('deleteSensorEnrichments', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(deleteResponse));

      sensorIndexingConfigService.deleteSensorIndexingConfig('squid').subscribe(result => {
        expect(result.status).toEqual(200);
      });
    })));
  });

});


