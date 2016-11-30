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
import {HttpModule, XHRBackend, Response, ResponseOptions, Http} from '@angular/http';
import '../rxjs-operators';
import {APP_CONFIG, METRON_REST_CONFIG} from '../app.config';
import {IAppConfig} from '../app.config.interface';
import {GlobalConfigService} from './global-config.service';

describe('GlobalConfigService', () => {

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [HttpModule],
      providers: [
        GlobalConfigService,
        {provide: XHRBackend, useClass: MockBackend},
        {provide: APP_CONFIG, useValue: METRON_REST_CONFIG}
      ]
    })
      .compileComponents();
  }));

  it('can instantiate service when inject service',
    inject([GlobalConfigService], (service: GlobalConfigService) => {
      expect(service instanceof GlobalConfigService).toBe(true);
    }));

  it('can instantiate service with "new"', inject([Http, APP_CONFIG], (http: Http, config: IAppConfig) => {
    expect(http).not.toBeNull('http should be provided');
    let service = new GlobalConfigService(http, config);
    expect(service instanceof GlobalConfigService).toBe(true, 'new service should be ok');
  }));


  it('can provide the mockBackend as XHRBackend',
    inject([XHRBackend], (backend: MockBackend) => {
      expect(backend).not.toBeNull('backend should be provided');
    }));

  describe('when service functions', () => {
    let globalConfigService: GlobalConfigService;
    let mockBackend: MockBackend;
    let globalConfig = {'field': 'value'};
    let globalConfigResponse: Response;
    let deleteResponse: Response;

    beforeEach(inject([Http, XHRBackend, APP_CONFIG], (http: Http, be: MockBackend, config: IAppConfig) => {
      mockBackend = be;
      globalConfigService = new GlobalConfigService(http, config);
      globalConfigResponse = new Response(new ResponseOptions({status: 200, body: globalConfig}));
    }));

    it('post', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(globalConfigResponse));

      globalConfigService.post(globalConfig).subscribe(
      result => {
        expect(result).toEqual(globalConfig);
      }, error => console.log(error));
    })));

    it('get', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(globalConfigResponse));

      globalConfigService.get().subscribe(
        result => {
          expect(result).toEqual(globalConfig);
        }, error => console.log(error));
    })));

    it('deleteSensorParserConfigs', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(deleteResponse));

      globalConfigService.delete().subscribe(result => {
        expect(result.status).toEqual(200);
      });
    })));
  });

});


