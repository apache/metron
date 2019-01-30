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
import { KafkaService } from './kafka.service';
import { KafkaTopic } from '../model/kafka-topic';
import {
  HttpClientTestingModule,
  HttpTestingController
} from '@angular/common/http/testing';
import {AppConfigService} from './app-config.service';
import {MockAppConfigService} from './mock.app-config.service';

describe('KafkaService', () => {
  let mockBackend: HttpTestingController;
  let kafkaService: KafkaService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        KafkaService,
        { provide: AppConfigService, useClass: MockAppConfigService }
      ]
    });
    mockBackend = TestBed.get(HttpTestingController);
    kafkaService = TestBed.get(KafkaService);
  });

  afterEach(() => {
    mockBackend.verify();
  });

  describe('when service functions', () => {
    let kafkaTopic = new KafkaTopic();
    kafkaTopic.name = 'bro';
    kafkaTopic.numPartitions = 1;
    kafkaTopic.replicationFactor = 1;
    let sampleMessage = 'sample message';

    it('post', () => {
      kafkaService.post(kafkaTopic).subscribe(
        result => {
          expect(result).toEqual(kafkaTopic);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/kafka/topic');
      expect(req.request.method).toBe('POST');
      req.flush(kafkaTopic);
    });

    it('get', () => {
      kafkaService.get('bro').subscribe(
        result => {
          expect(result).toEqual(kafkaTopic);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/kafka/topic/bro');
      expect(req.request.method).toBe('GET');
      req.flush(kafkaTopic);
    });

    it('list', () => {
      kafkaService.list().subscribe(
        result => {
          expect(result).toEqual([kafkaTopic]);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/kafka/topic');
      expect(req.request.method).toBe('GET');
      req.flush([kafkaTopic]);
    });

    it('sample', () => {
      kafkaService.sample('bro').subscribe(
        result => {
          expect(result).toEqual(sampleMessage);
        },
        error => console.log(error)
      );

      const req = mockBackend.expectOne('/api/v1/kafka/topic/bro/sample');
      expect(req.request.method).toBe('GET');
      req.flush(sampleMessage);
    });
  });
});
