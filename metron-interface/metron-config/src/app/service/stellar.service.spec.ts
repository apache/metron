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
import { StellarService } from './stellar.service';
import { SensorParserContext } from '../model/sensor-parser-context';
import { SensorParserConfig } from '../model/sensor-parser-config';
import {
  HttpClientTestingModule,
  HttpTestingController
} from '@angular/common/http/testing';
import { StellarFunctionDescription } from '../model/stellar-function-description';
import {AppConfigService} from './app-config.service';
import {MockAppConfigService} from './mock.app-config.service';

describe('StellarService', () => {
  let mockBackend: HttpTestingController;
  let transformationValidationService: StellarService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        StellarService,
        { provide: AppConfigService, useClass: MockAppConfigService }
      ]
    });
    mockBackend = TestBed.get(HttpTestingController);
    transformationValidationService = TestBed.get(StellarService);
  });

  describe('when service functions', () => {
    let transformationRules = ['rule1', 'rule2'];
    let transformationRulesValidation = { rule1: true, rule2: false };
    let transformationValidation = new SensorParserContext();
    transformationValidation.sampleData = { data: 'data' };
    transformationValidation.sensorParserConfig = new SensorParserConfig();
    transformationValidation.sensorParserConfig.sensorTopic = 'test';
    let transformations = ['STELLAR', 'REMOVE'];
    let transformFunctions: StellarFunctionDescription[] = [
      { name: 'function1', description: 'desc1', params: [], returns: '' },
      { name: 'function2', description: 'desc2', params: [], returns: '' }
    ];
    let simpleTransformFunctions = Object.assign([], transformFunctions);

    it('validateRules', () => {
      transformationValidationService
        .validateRules(transformationRules)
        .subscribe(
          result => {
            expect(result).toEqual(transformationRulesValidation);
          },
          error => console.log(error)
        );
      const req = mockBackend.expectOne('/api/v1/stellar/validate/rules');
      expect(req.request.method).toBe('POST');
      req.flush(transformationRulesValidation);
    });

    it('validate', () => {
      transformationValidationService
        .validate(transformationValidation)
        .subscribe(
          result => {
            expect(result).toEqual(transformationValidation);
          },
          error => console.log(error)
        );
      const req = mockBackend.expectOne('/api/v1/stellar/validate');
      expect(req.request.method).toBe('POST');
      req.flush(transformationValidation);
    });

    it('list', () => {
      transformationValidationService.list().subscribe(
        result => {
          expect(result).toEqual(transformations);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/stellar/list');
      expect(req.request.method).toBe('GET');
      req.flush(transformations);
    });

    it('listFunctions', () => {
      transformationValidationService.listFunctions().subscribe(
        result => {
          expect(result).toEqual(transformFunctions);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne('/api/v1/stellar/list/functions');
      expect(req.request.method).toBe('GET');
      req.flush(transformFunctions);
    });

    it('listSimpleFunctions', () => {
      transformationValidationService.listSimpleFunctions().subscribe(
        result => {
          expect(result).toEqual(simpleTransformFunctions);
        },
        error => console.log(error)
      );
      const req = mockBackend.expectOne(
        '/api/v1/stellar/list/simple/functions'
      );
      expect(req.request.method).toBe('GET');
      req.flush(simpleTransformFunctions);
    });
  });
});
