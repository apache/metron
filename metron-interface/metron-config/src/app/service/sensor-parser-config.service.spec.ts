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
import { ParserConfigModel } from '../sensors/models/parser-config.model';
import { ParseMessageRequest } from '../model/parse-message-request';
import { APP_CONFIG, METRON_REST_CONFIG } from '../app.config';
import {
  HttpClientTestingModule,
  HttpTestingController,
  TestRequest
} from '@angular/common/http/testing';
import { ParserGroupModel } from '../sensors/models/parser-group.model';
import { ParserMetaInfoModel } from '../sensors/models/parser-meta-info.model';
import { catchError } from 'rxjs/operators';
import { RestError } from '../model/rest-error';
import { noop } from 'rxjs';

describe('SensorParserConfigService', () => {
  let mockBackend: HttpTestingController;
  let sensorParserConfigService: SensorParserConfigService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        SensorParserConfigService,
        { provide: APP_CONFIG, useValue: METRON_REST_CONFIG }
      ]
    });
    mockBackend = TestBed.get(HttpTestingController);
    sensorParserConfigService = TestBed.get(SensorParserConfigService);
  });

  afterEach(() => {
    mockBackend.verify();
  });

  let sensorParserConfig = new ParserConfigModel('TestConfigId01');
  sensorParserConfig.sensorTopic = 'bro';
  sensorParserConfig.parserClassName = 'parserClass';
  sensorParserConfig.parserConfig = { field: 'value' };
  let availableParsers = [{ Grok: 'org.apache.metron.parsers.GrokParser' }];
  let parseMessageRequest = new ParseMessageRequest();
  parseMessageRequest.sensorParserConfig = new ParserConfigModel('TestConfigId02');
  parseMessageRequest.sensorParserConfig.sensorTopic = 'bro';
  parseMessageRequest.sampleData = 'sampleData';
  let parsedMessage = { field: 'value' };
  let sensorParserConfig1 = new ParserConfigModel('TestConfigId03');
  sensorParserConfig1.sensorTopic = 'bro1';
  let sensorParserConfig2 = new ParserConfigModel('TestConfigId04');
  sensorParserConfig2.sensorTopic = 'bro2';

  it('post', () => {
    sensorParserConfigService
      .saveConfig('bro', sensorParserConfig)
      .subscribe(result => {
        expect(result).toEqual(sensorParserConfig);
      });

    const req = mockBackend.expectOne('/api/v1/sensor/parser/config/bro');
    expect(req.request.method).toBe('POST');
    req.flush(sensorParserConfig);
  });

  it('get', () => {
    sensorParserConfigService.getConfig('bro').subscribe(result => {
      expect(result).toEqual(sensorParserConfig);
    });
    const req = mockBackend.expectOne('/api/v1/sensor/parser/config/bro');
    expect(req.request.method).toBe('GET');
    req.flush(sensorParserConfig);
  });

  it('getAll', () => {
    sensorParserConfigService.getAllConfig().subscribe(results => {
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
      .deleteConfigs(['bro1', 'bro2'])
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

  describe('REST Calls for Parser Grouping', () => {

    it('getting list of parser groups', () => {
      sensorParserConfigService.getAllGroups().subscribe((result: ParserGroupModel[]) => {
        expect(result.length).toBe(2);
        expect(result[0].name).toBe('TestGroupName1');
        expect(result[0].description).toBe('TestDesc1');
      });

      const request = mockBackend.expectOne('/api/v1/sensor/parser/group');
      request.flush([
        {
          name: 'TestGroupName1',
          description: 'TestDesc1'
        },
        {
          name: 'TestGroupName2',
          description: 'TestDesc2'
        }
      ]);
    });

    it('getting single parser group by name', () => {
      sensorParserConfigService.getGroup('TestGroup').subscribe((result: ParserGroupModel) => {
        expect(result.name).toBe('TestGroupName1');
        expect(result.description).toBe('TestDesc1');
      });

      const request = mockBackend.expectOne('/api/v1/sensor/parser/group/TestGroup');
      request.flush({
          name: 'TestGroupName1',
          description: 'TestDesc1'
        });
    });

    it('creating/editing single parser group by name', () => {
      sensorParserConfigService.saveGroup('TestGroup', new ParserGroupModel({
        name: 'TestGroupName1',
        description: 'TestDesc1'
      })).subscribe();

      const request = mockBackend.expectOne('/api/v1/sensor/parser/group/TestGroup');
      expect(request.request.method).toEqual('POST');
      expect(request.request.body.name).toBe('TestGroupName1');
    });

    it('deleting single parser group by name', () => {
      sensorParserConfigService.deleteGroup('TestGroup').subscribe();

      const request = mockBackend.expectOne('/api/v1/sensor/parser/group/TestGroup');
      expect(request.request.method).toEqual('DELETE');
    });

    it('deleting multiple parser groups by name', () => {
      sensorParserConfigService.deleteGroups(['TestGroup1', 'TestGroup2', 'TestGroup3'])
      .subscribe((result) => {
        expect(result.success.length).toBe(2);
        expect(result.failure.length).toBe(1);
      });

      const request: Array<TestRequest> = [];
      request.push(mockBackend.expectOne('/api/v1/sensor/parser/group/TestGroup1'));
      request.push(mockBackend.expectOne('/api/v1/sensor/parser/group/TestGroup2'));
      request.push(mockBackend.expectOne('/api/v1/sensor/parser/group/TestGroup3'));

      expect(request[0].request.method).toEqual('DELETE');
      expect(request[1].request.method).toEqual('DELETE');
      expect(request[2].request.method).toEqual('DELETE');

      request[0].flush({});
      request[1].flush('Invalid request parameters', { status: 404, statusText: 'Bad Request' });
      request[2].flush({});
    });

    function getTestGroups() {
      return [
        { config: new ParserGroupModel({ name: 'TestGroup01', description: '' }) },
        { config: new ParserGroupModel({ name: 'TestGroup02', description: '' }) },
        { config: new ParserGroupModel({ name: 'TestGroup03', description: '' }) },
        { config: new ParserGroupModel({ name: 'TestGroup04', description: '' }) },
      ];
    }

    function getTestConfigs() {
      return [
        { config: new ParserConfigModel('Parser_Config_ID_01', { sensorTopic: 'Kafka/Sensor Topic ID 01' }) },
        { config: new ParserConfigModel('Parser_Config_ID_02', { sensorTopic: 'Kafka/Sensor Topic ID 02' }) },
        { config: new ParserConfigModel('Parser_Config_ID_03', { sensorTopic: 'Kafka/Sensor Topic ID 03' }) },
        { config: new ParserConfigModel('Parser_Config_ID_04', { sensorTopic: 'Kafka/Sensor Topic ID 04' }) },
      ];
    }

    function markElementOnIndexAs(testData: ParserMetaInfoModel[], indexes: number[], flag: string) {
      indexes.forEach((index) => {
        testData[index][flag] = true;
      })
    }

    class DirtyFlags {
      static NEW = 'isPhantom';
      static CHANGED = 'isDirty';
      static DELETED = 'isDeleted';
    }

    it('syncronizing list of parser GROUPS with the backend - SINGLE DELETE', () => {
      const testData = getTestGroups();

      markElementOnIndexAs(testData, [1], DirtyFlags.DELETED);

      sensorParserConfigService.syncGroups(testData).subscribe();

      const request = mockBackend.expectOne('/api/v1/sensor/parser/group/TestGroup02');
      expect(request.request.method).toEqual('DELETE');
    });

    it('syncronizing list of parser GROUPS with the backend - MULTIPLE DELETE', () => {
      const testData = getTestGroups();

      markElementOnIndexAs(testData, [0, 2, 3], DirtyFlags.DELETED);

      sensorParserConfigService.syncGroups(testData).subscribe();

      const requests = [];
      requests.push(mockBackend.expectOne('/api/v1/sensor/parser/group/TestGroup01'));
      requests.push(mockBackend.expectOne('/api/v1/sensor/parser/group/TestGroup04'));
      requests.push(mockBackend.expectOne('/api/v1/sensor/parser/group/TestGroup03'));
      expect(requests[0].request.method).toEqual('DELETE');
      expect(requests[1].request.method).toEqual('DELETE');
      expect(requests[2].request.method).toEqual('DELETE');
    });

    it('syncronizing list of parser GROUPS with the backend - SINGLE NEW', () => {
      const testData = getTestGroups();

      markElementOnIndexAs(testData, [0], DirtyFlags.NEW);

      sensorParserConfigService.syncGroups(testData).subscribe();

      const request = mockBackend.expectOne('/api/v1/sensor/parser/group/TestGroup01');
      expect(request.request.method).toEqual('POST');
      expect(request.request.body.name).toEqual('TestGroup01');
      expect(request.request.body.description).toEqual('');
    });

    it('syncronizing list of parser GROUPS with the backend - MULTIPLE NEW', () => {
      const testData = getTestGroups();

      markElementOnIndexAs(testData, [0, 2], DirtyFlags.NEW);

      sensorParserConfigService.syncGroups(testData).subscribe();

      const requests = [];
      requests.push(mockBackend.expectOne('/api/v1/sensor/parser/group/TestGroup01'));
      requests.push(mockBackend.expectOne('/api/v1/sensor/parser/group/TestGroup03'));
      expect(requests[0].request.method).toEqual('POST');
      expect(requests[1].request.method).toEqual('POST');

      expect(requests[0].request.body.name).toEqual('TestGroup01');
      expect(requests[1].request.body.name).toEqual('TestGroup03');
    });

    it('syncronizing list of parser GROUPS with the backend - SINGLE CHANGED', () => {
      const testData = getTestGroups();

      markElementOnIndexAs(testData, [3], DirtyFlags.CHANGED);

      sensorParserConfigService.syncGroups(testData).subscribe();

      const request = mockBackend.expectOne('/api/v1/sensor/parser/group/TestGroup04');
      expect(request.request.method).toEqual('POST');
      expect(request.request.body.name).toEqual('TestGroup04');
      expect(request.request.body.description).toEqual('');
    });

    it('syncronizing list of parser GROUPS with the backend - MULTIPLE CHANGED', () => {
      const testData = getTestGroups();

      markElementOnIndexAs(testData, [0, 2], DirtyFlags.CHANGED);

      sensorParserConfigService.syncGroups(testData).subscribe();

      const requests = [];
      requests.push(mockBackend.expectOne('/api/v1/sensor/parser/group/TestGroup01'));
      requests.push(mockBackend.expectOne('/api/v1/sensor/parser/group/TestGroup03'));
      expect(requests[0].request.method).toEqual('POST');
      expect(requests[1].request.method).toEqual('POST');

      expect(requests[0].request.body.name).toEqual('TestGroup01');
      expect(requests[1].request.body.name).toEqual('TestGroup03');
    });

    it('syncronizing list of PARSER CONFIGS with the backend - SINGLE DELETE', () => {
      const testData = getTestConfigs();

      markElementOnIndexAs(testData, [1], DirtyFlags.DELETED);

      sensorParserConfigService.syncConfigs(testData).subscribe();

      const request = mockBackend.expectOne('/api/v1/sensor/parser/config/Parser_Config_ID_02');
      expect(request.request.method).toEqual('DELETE');
    });

    it('syncronizing list of PARSER CONFIGS with the backend - MULTIPLE DELETE', () => {
      const testData = getTestConfigs();

      markElementOnIndexAs(testData, [0, 2, 3], DirtyFlags.DELETED);

      sensorParserConfigService.syncConfigs(testData).subscribe();

      const requests = [];
      requests.push(mockBackend.expectOne('/api/v1/sensor/parser/config/Parser_Config_ID_01'));
      requests.push(mockBackend.expectOne('/api/v1/sensor/parser/config/Parser_Config_ID_03'));
      requests.push(mockBackend.expectOne('/api/v1/sensor/parser/config/Parser_Config_ID_04'));
      expect(requests[0].request.method).toEqual('DELETE');
      expect(requests[1].request.method).toEqual('DELETE');
      expect(requests[2].request.method).toEqual('DELETE');
    });

    it('syncronizing list of PARSER CONFIGS with the backend - SINGLE NEW', () => {
      const testData = getTestConfigs();

      markElementOnIndexAs(testData, [0], DirtyFlags.NEW);

      sensorParserConfigService.syncConfigs(testData).subscribe();

      const request = mockBackend.expectOne('/api/v1/sensor/parser/config/Parser_Config_ID_01');
      expect(request.request.method).toEqual('POST');
      expect(JSON.parse(request.request.body).sensorTopic).toEqual('Kafka/Sensor Topic ID 01');
    });

    it('syncronizing list of PARSER CONFIGS with the backend - MULTIPLE NEW', () => {
      const testData = getTestConfigs();

      markElementOnIndexAs(testData, [0, 2], DirtyFlags.NEW);

      sensorParserConfigService.syncConfigs(testData).subscribe();

      const requests = [];
      requests.push(mockBackend.expectOne('/api/v1/sensor/parser/config/Parser_Config_ID_01'));
      requests.push(mockBackend.expectOne('/api/v1/sensor/parser/config/Parser_Config_ID_03'));
      expect(requests[0].request.method).toEqual('POST');
      expect(requests[1].request.method).toEqual('POST');

      expect(JSON.parse(requests[0].request.body).sensorTopic).toEqual('Kafka/Sensor Topic ID 01');
      expect(JSON.parse(requests[1].request.body).sensorTopic).toEqual('Kafka/Sensor Topic ID 03');
    });

    it('syncronizing list of PARSER CONFIGS with the backend - SINGLE CHANGED', () => {
      const testData = getTestConfigs();

      markElementOnIndexAs(testData, [3], DirtyFlags.CHANGED);

      sensorParserConfigService.syncConfigs(testData).subscribe();

      const request = mockBackend.expectOne('/api/v1/sensor/parser/config/Parser_Config_ID_04');
      expect(request.request.method).toEqual('POST');
      expect(JSON.parse(request.request.body).sensorTopic).toEqual('Kafka/Sensor Topic ID 04');
    });

    it('syncronizing list of PARSER CONFIGS with the backend - MULTIPLE CHANGED', () => {
      const testData = getTestConfigs();

      markElementOnIndexAs(testData, [0, 2], DirtyFlags.CHANGED);

      sensorParserConfigService.syncConfigs(testData).subscribe();

      const requests = [];
      requests.push(mockBackend.expectOne('/api/v1/sensor/parser/config/Parser_Config_ID_01'));
      requests.push(mockBackend.expectOne('/api/v1/sensor/parser/config/Parser_Config_ID_03'));
      expect(requests[0].request.method).toEqual('POST');
      expect(requests[1].request.method).toEqual('POST');

      expect(JSON.parse(requests[0].request.body).sensorTopic).toEqual('Kafka/Sensor Topic ID 01');
      expect(JSON.parse(requests[1].request.body).sensorTopic).toEqual('Kafka/Sensor Topic ID 03');
    });

    it('syncronization of PARSER CONFIGS should return with an Observable array of successful/unsuccessful requests', () => {
      const testData = getTestConfigs();

      markElementOnIndexAs(testData, [0, 2], DirtyFlags.CHANGED);

      sensorParserConfigService.syncConfigs(testData)
        .subscribe((syncResults) => {
          expect(syncResults.length === 2);
        });

        const requests = [];
        requests.push(mockBackend.expectOne('/api/v1/sensor/parser/config/Parser_Config_ID_01'));
        requests.push(mockBackend.expectOne('/api/v1/sensor/parser/config/Parser_Config_ID_03'));
        requests[0].flush(requests[0].request.body);
        requests[1].flush(requests[1].request.body);
    });

    it('error throwing in syncConfigs()', () => {
      const testData = getTestConfigs();

      markElementOnIndexAs(testData, [2], DirtyFlags.CHANGED);

      sensorParserConfigService.syncConfigs(testData)
        .subscribe(
          noop,
          (error) => {
            expect(error).toBeDefined();
          }
        );

        const request = mockBackend.expectOne('/api/v1/sensor/parser/config/Parser_Config_ID_03');
        request.flush('Invalid request parameters', { status: 404, statusText: 'Bad Request' });
    });

    it('error throwing in syncConfigs()', () => {
      const testData = getTestGroups();

      markElementOnIndexAs(testData, [1], DirtyFlags.CHANGED);

      sensorParserConfigService.syncGroups(testData)
        .subscribe(
          noop,
          (error) => {
            expect(error).toBeDefined();
          }
        );

        const request = mockBackend.expectOne('/api/v1/sensor/parser/group/TestGroup02');
        request.flush('Invalid request parameters', { status: 404, statusText: 'Bad Request' });
    });
  })
});
