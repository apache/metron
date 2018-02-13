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
import {SensorParserConfigService} from './sensor-parser-config.service';
import {SensorParserConfig} from '../model/sensor-parser-config';
import {ParseMessageRequest} from '../model/parse-message-request';
import {HttpModule, XHRBackend, Response, ResponseOptions, Http} from '@angular/http';
import '../rxjs-operators';
import {APP_CONFIG, METRON_REST_CONFIG} from '../app.config';
import {IAppConfig} from '../app.config.interface';

describe('SensorParserConfigService', () => {

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [HttpModule],
      providers: [
        SensorParserConfigService,
        {provide: XHRBackend, useClass: MockBackend},
        {provide: APP_CONFIG, useValue: METRON_REST_CONFIG}
      ]
    })
      .compileComponents();
  }));

  it('can instantiate service when inject service',
    inject([SensorParserConfigService], (service: SensorParserConfigService) => {
      expect(service instanceof SensorParserConfigService).toBe(true);
    }));

  it('can instantiate service with "new"', inject([Http, APP_CONFIG], (http: Http, config: IAppConfig) => {
    expect(http).not.toBeNull('http should be provided');
    let service = new SensorParserConfigService(http, config);
    expect(service instanceof SensorParserConfigService).toBe(true, 'new service should be ok');
  }));


  it('can provide the mockBackend as XHRBackend',
    inject([XHRBackend], (backend: MockBackend) => {
      expect(backend).not.toBeNull('backend should be provided');
    }));

  describe('when service functions', () => {
    let sensorParserConfigService: SensorParserConfigService;
    let mockBackend: MockBackend;
    let sensorParserConfig = new SensorParserConfig();
    sensorParserConfig.sensorTopic = 'bro';
    sensorParserConfig.parserClassName = 'parserClass';
    sensorParserConfig.parserConfig = {field: 'value'};
    let availableParsers = [{ 'Grok': 'org.apache.metron.parsers.GrokParser'}];
    let parseMessageRequest = new ParseMessageRequest();
    parseMessageRequest.sensorParserConfig = new SensorParserConfig();
    parseMessageRequest.sensorParserConfig.sensorTopic = 'bro';
    parseMessageRequest.sampleData = 'sampleData';
    let parsedMessage = { 'field': 'value'};
    let sensorParserConfig1 = new SensorParserConfig();
    sensorParserConfig1.sensorTopic = 'bro1';
    let sensorParserConfig2 = new SensorParserConfig();
    sensorParserConfig2.sensorTopic = 'bro2';
    let deleteResult = {success: ['bro1', 'bro2']};
    let sensorParserConfigResponse: Response;
    let sensorParserConfigsResponse: Response;
    let availableParserResponse: Response;
    let parseMessageResponse: Response;
    let deleteResponse: Response;

    beforeEach(inject([Http, XHRBackend, APP_CONFIG], (http: Http, be: MockBackend, config: IAppConfig) => {
      mockBackend = be;
      sensorParserConfigService = new SensorParserConfigService(http, config);
      sensorParserConfigResponse = new Response(new ResponseOptions({status: 200, body: sensorParserConfig}));
      sensorParserConfigsResponse = new Response(new ResponseOptions({status: 200, body: [sensorParserConfig]}));
      availableParserResponse = new Response(new ResponseOptions({status: 200, body: availableParsers}));
      parseMessageResponse = new Response(new ResponseOptions({status: 200, body: parsedMessage}));
      deleteResponse = new Response(new ResponseOptions({status: 200, body: deleteResult}));
    }));

    it('post', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(sensorParserConfigResponse));

      sensorParserConfigService.post('bro', sensorParserConfig).subscribe(
      result => {
        expect(result).toEqual(sensorParserConfig);
      }, error => console.log(error));
    })));

    it('get', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(sensorParserConfigResponse));

      sensorParserConfigService.get('bro').subscribe(
        result => {
          expect(result).toEqual(sensorParserConfig);
        }, error => console.log(error));
    })));

    it('getAll', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(sensorParserConfigsResponse));

      sensorParserConfigService.getAll().subscribe(
        results => {
          expect(results).toEqual([sensorParserConfig]);
        }, error => console.log(error));
    })));

    it('getAvailableParsers', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(availableParserResponse));

      sensorParserConfigService.getAvailableParsers().subscribe(
        results => {
          expect(results).toEqual(availableParsers);
        }, error => console.log(error));
    })));

    it('parseMessage', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(parseMessageResponse));

      sensorParserConfigService.parseMessage(parseMessageRequest).subscribe(
        results => {
          expect(results).toEqual(parsedMessage);
        }, error => console.log(error));
    })));

    it('deleteSensorParserConfigs', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(deleteResponse));

      sensorParserConfigService.deleteSensorParserConfigs(['bro1', 'bro2']).subscribe(result => {
        expect(result.success.length).toEqual(2);
      });
    })));
  });

});


