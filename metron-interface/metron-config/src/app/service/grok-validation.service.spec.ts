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
import { async, inject, TestBed } from '@angular/core/testing';
import { GrokValidationService } from './grok-validation.service';
import { GrokValidation } from '../model/grok-validation';
import {
  HttpClientTestingModule,
  HttpTestingController
} from '@angular/common/http/testing';
import {AppConfigService} from './app-config.service';
import {MockAppConfigService} from './mock.app-config.service';

describe('GrokValidationService', () => {
  let grokValidationService: GrokValidationService;
  let mockBackend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        GrokValidationService,
        { provide: AppConfigService, useClass: MockAppConfigService }
      ]
    });
    grokValidationService = TestBed.get(GrokValidationService);
    mockBackend = TestBed.get(HttpTestingController);
  });

  afterEach(() => {
    mockBackend.verify();
  });

  describe('when service functions', () => {
    let grokValidation = new GrokValidation();
    grokValidation.statement = 'statement';
    grokValidation.sampleData = 'sampleData';
    grokValidation.results = { results: 'results' };
    let grokList = ['pattern'];
    let grokStatement = 'grok statement';

    it('validate', () => {
        grokValidationService.validate(grokValidation).subscribe(
          result => {
            expect(result).toEqual(grokValidation);
          },
          error => console.log(error)
        );
        const req = mockBackend.expectOne('/api/v1/grok/validate');
        expect(req.request.method).toBe('POST');
        req.flush(grokValidation);
      }
    );

    it('list', () => {
        grokValidationService.list().subscribe(
          results => {
            expect(results).toEqual(grokList);
          },
          error => console.log(error)
        );
        const req = mockBackend.expectOne('/api/v1/grok/list');
        expect(req.request.method).toBe('GET');
        req.flush(grokList);
      }
    );

    it('getStatement', () => {
        grokValidationService.getStatement('/path').subscribe(
          results => {
            expect(results).toEqual(grokStatement);
          },
          error => console.log(error)
        );
        const req = mockBackend.expectOne('/api/v1/grok/get/statement?path=/path');
        expect(req.request.method).toBe('GET');
        req.flush(grokStatement);
      }
    );
  });
});
