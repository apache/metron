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
import { SensorParserConfigService } from './sensor-parser-config.service';
import { SensorParserConfig } from '../model/sensor-parser-config';
import { ParseMessageRequest } from '../model/parse-message-request';
import { APP_CONFIG, METRON_REST_CONFIG } from '../app.config';
import {
  HttpClientTestingModule,
  HttpTestingController
} from '@angular/common/http/testing';
import {AppConfigService} from './app-config.service';
import {MockAppConfigService} from './mock.app-config.service';

describe('SensorParserConfigService', () => {
  let mockBackend: HttpTestingController;
  let sensorParserConfigService: SensorParserConfigService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        SensorParserConfigService,
        { provide: AppConfigService, useClass: MockAppConfigService }
      ]
    });
    mockBackend = TestBed.get(HttpTestingController);
    sensorParserConfigService = TestBed.get(SensorParserConfigService);
  });

  afterEach(() => {
    mockBackend.verify();
  });

  let sensorParserConfig = new SensorParserConfig();
  sensorParserConfig.sensorTopic = 'bro';
  sensorParserConfig.parserClassName = 'parserClass';
  sensorParserConfig.parserConfig = { field: 'value' };
  let availableParsers = [{ Grok: 'org.apache.metron.parsers.GrokParser' }];
  let parseMessageRequest = new ParseMessageRequest();
  parseMessageRequest.sensorParserConfig = new SensorParserConfig();
  parseMessageRequest.sensorParserConfig.sensorTopic = 'bro';
  parseMessageRequest.sampleData = 'sampleData';
  let parsedMessage = { field: 'value' };
  let sensorParserConfig1 = new SensorParserConfig();
  sensorParserConfig1.sensorTopic = 'bro1';
  let sensorParserConfig2 = new SensorParserConfig();
  sensorParserConfig2.sensorTopic = 'bro2';

  it('post', () => {
    sensorParserConfigService
      .post('bro', sensorParserConfig)
      .subscribe(result => {
        expect(result).toEqual(sensorParserConfig);
      });

    const req = mockBackend.expectOne('/api/v1/sensor/parser/config/bro');
    expect(req.request.method).toBe('POST');
    req.flush(sensorParserConfig);
  });

  it('get', () => {
    sensorParserConfigService.get('bro').subscribe(result => {
      expect(result).toEqual(sensorParserConfig);
    });
    const req = mockBackend.expectOne('/api/v1/sensor/parser/config/bro');
    expect(req.request.method).toBe('GET');
    req.flush(sensorParserConfig);
  });

  it('getAll', () => {
    sensorParserConfigService.getAll().subscribe(results => {
      expect(results).toEqual([sensorParserConfig]);
    });
    const req = mockBackend.expectOne('/api/v1/sensor/parser/config');
    expect(req.request.method).toBe('GET');
    req.flush([sensorParserConfig]);
  });

  it('getAvailableParsers', () => {
    sensorParserConfigService.getAvailableParsers().subscribe(results => {
      expect(results).toEqual(availableParsers);
    });
    const req = mockBackend.expectOne(
      '/api/v1/sensor/parser/config/list/available'
    );
    expect(req.request.method).toBe('GET');
    req.flush(availableParsers);
  });

  it('parseMessage', () => {
    sensorParserConfigService
      .parseMessage(parseMessageRequest)
      .subscribe(results => {
        expect(results).toEqual(parsedMessage);
      });
    const req = mockBackend.expectOne(
      '/api/v1/sensor/parser/config/parseMessage'
    );
    expect(req.request.method).toBe('POST');
    req.flush(parsedMessage);
  });

  it('deleteSensorParserConfigs', () => {
    let req = [];
    sensorParserConfigService
      .deleteSensorParserConfigs(['bro1', 'bro2'])
      .subscribe(result => {
        expect(result.success.length).toEqual(2);
      });
    req[0] = mockBackend.expectOne('/api/v1/sensor/parser/config/bro1');
    req[1] = mockBackend.expectOne('/api/v1/sensor/parser/config/bro2');
    req.map(r => {
      expect(r.request.method).toBe('DELETE');
      r.flush(parsedMessage);
    });
  });
});
