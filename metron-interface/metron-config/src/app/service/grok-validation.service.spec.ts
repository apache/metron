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
import {GrokValidationService} from './grok-validation.service';
import {GrokValidation} from '../model/grok-validation';
import {HttpModule, XHRBackend, Response, ResponseOptions, Http} from '@angular/http';
import '../rxjs-operators';
import {APP_CONFIG, METRON_REST_CONFIG} from '../app.config';
import {IAppConfig} from '../app.config.interface';

describe('GrokValidationService', () => {

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [HttpModule],
      providers: [
        GrokValidationService,
        {provide: XHRBackend, useClass: MockBackend},
        {provide: APP_CONFIG, useValue: METRON_REST_CONFIG}
      ]
    })
      .compileComponents();
  }));

  it('can instantiate service when inject service',
    inject([GrokValidationService], (service: GrokValidationService) => {
      expect(service instanceof GrokValidationService).toBe(true);
    }));

  it('can instantiate service with "new"', inject([Http, APP_CONFIG], (http: Http, config: IAppConfig) => {
    expect(http).not.toBeNull('http should be provided');
    let service = new GrokValidationService(http, config);
    expect(service instanceof GrokValidationService).toBe(true, 'new service should be ok');
  }));


  it('can provide the mockBackend as XHRBackend',
    inject([XHRBackend], (backend: MockBackend) => {
      expect(backend).not.toBeNull('backend should be provided');
    }));

  describe('when service functions', () => {
    let grokValidationService: GrokValidationService;
    let mockBackend: MockBackend;
    let grokValidation = new GrokValidation();
    grokValidation.statement = 'statement';
    grokValidation.sampleData = 'sampleData';
    grokValidation.results = {'results': 'results'};
    let grokList = ['pattern'];
    let grokStatement = 'grok statement';
    let grokValidationResponse: Response;
    let grokListResponse: Response;
    let grokGetStatementResponse: Response;

    beforeEach(inject([Http, XHRBackend, APP_CONFIG], (http: Http, be: MockBackend, config: IAppConfig) => {
      mockBackend = be;
      grokValidationService = new GrokValidationService(http, config);
      grokValidationResponse = new Response(new ResponseOptions({status: 200, body: grokValidation}));
      grokListResponse = new Response(new ResponseOptions({status: 200, body: grokList}));
      grokGetStatementResponse = new Response(new ResponseOptions({status: 200, body: grokStatement}));
    }));

    it('validate', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(grokValidationResponse));

      grokValidationService.validate(grokValidation).subscribe(
        result => {
          expect(result).toEqual(grokValidation);
        }, error => console.log(error));
    })));

    it('list', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(grokListResponse));
      grokValidationService.list().subscribe(
        results => {
          expect(results).toEqual(grokList);
        }, error => console.log(error));
    })));

    it('getStatement', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(grokGetStatementResponse));
      grokValidationService.getStatement('/path').subscribe(
          results => {
            expect(results).toEqual(grokStatement);
          }, error => console.log(error));
    })));
  });


});
