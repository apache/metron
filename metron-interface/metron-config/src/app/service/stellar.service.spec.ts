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
import {StellarService} from './stellar.service';
import {SensorParserContext} from '../model/sensor-parser-context';
import {SensorParserConfig} from '../model/sensor-parser-config';
import {HttpModule, XHRBackend, Response, ResponseOptions, Http} from '@angular/http';
import '../rxjs-operators';
import {APP_CONFIG, METRON_REST_CONFIG} from '../app.config';
import {IAppConfig} from '../app.config.interface';

describe('StellarService', () => {

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [HttpModule],
      providers: [
        StellarService,
        {provide: XHRBackend, useClass: MockBackend},
        {provide: APP_CONFIG, useValue: METRON_REST_CONFIG}
      ]
    })
      .compileComponents();
  }));

  it('can instantiate service when inject service',
    inject([StellarService], (service: StellarService) => {
      expect(service instanceof StellarService).toBe(true);
    }));

  it('can instantiate service with "new"', inject([Http, APP_CONFIG], (http: Http, config: IAppConfig) => {
    expect(http).not.toBeNull('http should be provided');
    let service = new StellarService(http, config);
    expect(service instanceof StellarService).toBe(true, 'new service should be ok');
  }));


  it('can provide the mockBackend as XHRBackend',
    inject([XHRBackend], (backend: MockBackend) => {
      expect(backend).not.toBeNull('backend should be provided');
    }));

  describe('when service functions', () => {
    let transformationValidationService: StellarService;
    let mockBackend: MockBackend;
    let transformationRules = ['rule1', 'rule2'];
    let transformationRulesValidation = {rule1: true, rule2: false};
    let transformationValidation = new SensorParserContext();
    transformationValidation.sampleData = {'data': 'data'};
    transformationValidation.sensorParserConfig = new SensorParserConfig();
    transformationValidation.sensorParserConfig.sensorTopic = 'test';
    let transformations = ['STELLAR', 'REMOVE'];
    let transformFunctions = [{'function1': 'desc1'}, {'function2': 'desc2'}];
    let simpleTransformFunctions = [{'simplefunction1': 'simpledesc1'}, {'simplefunction2': 'simpledesc2'}];
    let transformationRulesValidationResponse: Response;
    let transformationValidationResponse: Response;
    let transformationListResponse: Response;
    let transformationListFunctionsResponse: Response;
    let transformationListSimpleFunctionsResponse: Response;

    beforeEach(inject([Http, XHRBackend, APP_CONFIG], (http: Http, be: MockBackend, config: IAppConfig) => {
      mockBackend = be;
      transformationValidationService = new StellarService(http, config);
      transformationRulesValidationResponse = new Response(new ResponseOptions({
        status: 200,
        body: transformationRulesValidation
      }));
      transformationValidationResponse = new Response(new ResponseOptions({
        status: 200,
        body: transformationValidation
      }));
      transformationListResponse = new Response(new ResponseOptions({status: 200, body: transformations}));
      transformationListFunctionsResponse = new Response(new ResponseOptions({status: 200, body: transformFunctions}));
      transformationListSimpleFunctionsResponse = new Response(new ResponseOptions({status: 200, body: simpleTransformFunctions}));
    }));

    it('validateRules', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(transformationRulesValidationResponse));

      transformationValidationService.validateRules(transformationRules).subscribe(
        result => {
          expect(result).toEqual(transformationRulesValidation);
        }, error => console.log(error));
    })));

    it('validate', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(transformationValidationResponse));

      transformationValidationService.validate(transformationValidation).subscribe(
        result => {
          expect(result).toEqual(transformationValidation);
        }, error => console.log(error));
    })));

    it('list', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(transformationListResponse));

      transformationValidationService.list().subscribe(
        result => {
          expect(result).toEqual(transformations);
        }, error => console.log(error));
    })));

    it('listFunctions', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(transformationListFunctionsResponse));

      transformationValidationService.listFunctions().subscribe(
        result => {
          expect(result).toEqual(transformFunctions);
        }, error => console.log(error));
    })));

    it('listSimpleFunctions', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(transformationListSimpleFunctionsResponse));

      transformationValidationService.listSimpleFunctions().subscribe(
          result => {
            expect(result).toEqual(simpleTransformFunctions);
          }, error => console.log(error));
    })));
  });

});



