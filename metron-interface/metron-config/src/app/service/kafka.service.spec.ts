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
import {KafkaService} from './kafka.service';
import {KafkaTopic} from '../model/kafka-topic';
import {HttpModule, XHRBackend, Response, ResponseOptions, Http} from '@angular/http';
import '../rxjs-operators';
import {APP_CONFIG, METRON_REST_CONFIG} from '../app.config';
import {IAppConfig} from '../app.config.interface';

describe('KafkaService', () => {

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [HttpModule],
      providers: [
        KafkaService,
        {provide: XHRBackend, useClass: MockBackend},
        {provide: APP_CONFIG, useValue: METRON_REST_CONFIG}
      ]
    })
      .compileComponents();
  }));

  it('can instantiate service when inject service',
    inject([KafkaService], (service: KafkaService) => {
      expect(service instanceof KafkaService).toBe(true);
    }));

  it('can instantiate service with "new"', inject([Http, APP_CONFIG], (http: Http, config: IAppConfig) => {
    expect(http).not.toBeNull('http should be provided');
    let service = new KafkaService(http, config);
    expect(service instanceof KafkaService).toBe(true, 'new service should be ok');
  }));

  it('can provide the mockBackend as XHRBackend',
    inject([XHRBackend], (backend: MockBackend) => {
      expect(backend).not.toBeNull('backend should be provided');
    }));

  describe('when service functions', () => {
    let kafkaService: KafkaService;
    let mockBackend: MockBackend;
    let kafkaTopic = new KafkaTopic();
    kafkaTopic.name = 'bro';
    kafkaTopic.numPartitions = 1;
    kafkaTopic.replicationFactor = 1;
    let sampleMessage = 'sample message';
    let kafkaResponse: Response;
    let kafkaListResponse: Response;
    let sampleMessageResponse: Response;

    beforeEach(inject([Http, XHRBackend, APP_CONFIG], (http: Http, be: MockBackend, config: IAppConfig) => {
      mockBackend = be;
      kafkaService = new KafkaService(http, config);
      kafkaResponse = new Response(new ResponseOptions({status: 200, body: kafkaTopic}));
      kafkaListResponse = new Response(new ResponseOptions({status: 200, body: [kafkaTopic]}));
      sampleMessageResponse = new Response(new ResponseOptions({status: 200, body: sampleMessage}));
    }));

    it('post', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(kafkaResponse));

      kafkaService.post(kafkaTopic).subscribe(
        result => {
          expect(result).toEqual(kafkaTopic);
        }, error => console.log(error));
    })));

    it('get', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(kafkaResponse));

      kafkaService.get('bro').subscribe(
        result => {
          expect(result).toEqual(kafkaTopic);
        }, error => console.log(error));
    })));

    it('list', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(kafkaListResponse));

      kafkaService.list().subscribe(
        result => {
          expect(result).toEqual([kafkaTopic]);
        }, error => console.log(error));
    })));

    it('sample', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(sampleMessageResponse));
      kafkaService.sample('bro').subscribe(
        result => {
          expect(result).toEqual(sampleMessage);
        }, error => console.log(error));
    })));
  });

});

