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
import {SensorParserConfig} from '../model/sensor-parser-config';
import {HttpModule, XHRBackend, Response, ResponseOptions, Http} from '@angular/http';
import '../rxjs-operators';
import {METRON_REST_CONFIG, APP_CONFIG} from '../app.config';
import {SensorParserConfigHistoryService} from './sensor-parser-config-history.service';
import {IAppConfig} from '../app.config.interface';
import {SensorParserConfigHistory} from '../model/sensor-parser-config-history';

describe('SensorParserConfigHistoryService', () => {

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [HttpModule],
      providers: [
        SensorParserConfigHistoryService,
        {provide: XHRBackend, useClass: MockBackend},
        {provide: APP_CONFIG, useValue: METRON_REST_CONFIG}
      ]
    })
        .compileComponents();
  }));

  it('can instantiate service when inject service',
      inject([SensorParserConfigHistoryService], (service: SensorParserConfigHistoryService) => {
        expect(service instanceof SensorParserConfigHistoryService).toBe(true);
      }));

  it('can instantiate service with "new"', inject([Http, APP_CONFIG], (http: Http, config: IAppConfig) => {
    expect(http).not.toBeNull('http should be provided');
    let service = new SensorParserConfigHistoryService(http, config);
    expect(service instanceof SensorParserConfigHistoryService).toBe(true, 'new service should be ok');
  }));


  it('can provide the mockBackend as XHRBackend',
      inject([XHRBackend], (backend: MockBackend) => {
        expect(backend).not.toBeNull('backend should be provided');
      }));

  describe('when service functions', () => {
    let sensorParserConfigHistoryService: SensorParserConfigHistoryService;
    let mockBackend: MockBackend;
    let sensorParserConfigHistory = new SensorParserConfigHistory();
    let sensorParserConfig = new SensorParserConfig();
    sensorParserConfig.sensorTopic = 'bro';
    sensorParserConfigHistory.config = sensorParserConfig;
    let sensorParserConfigHistoryResponse: Response;
    let allSensorParserConfigHistoryResponse: Response;

    beforeEach(inject([Http, XHRBackend, APP_CONFIG], (http: Http, be: MockBackend, config: IAppConfig) => {
      mockBackend = be;
      sensorParserConfigHistoryService = new SensorParserConfigHistoryService(http, config);
      sensorParserConfigHistoryResponse = new Response(new ResponseOptions({status: 200, body: sensorParserConfig}));
      allSensorParserConfigHistoryResponse = new Response(new ResponseOptions({status: 200, body: [sensorParserConfig]}));
    }));

    it('get', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(sensorParserConfigHistoryResponse));

      sensorParserConfigHistoryService.get('bro').subscribe(
          result => {
            expect(result).toEqual(sensorParserConfigHistory);
          }, error => console.log(error));
    })));

    it('getAll', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(allSensorParserConfigHistoryResponse));

      sensorParserConfigHistoryService.getAll().subscribe(
          result => {
            expect(result).toEqual([sensorParserConfigHistory]);
          }, error => console.log(error));
    })));
  });

});


