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
import {ParserExtensionService} from './parser-extension.service';
import {ParserExtensionConfig} from '../model/parser-extension-config';
import {HttpModule, XHRBackend, Response, ResponseOptions, Http} from '@angular/http';
import '../rxjs-operators';
import {APP_CONFIG, METRON_REST_CONFIG} from '../app.config';
import {IAppConfig} from '../app.config.interface';

describe('ParserExtensionService', () => {

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [HttpModule],
      providers: [
        ParserExtensionService,
        {provide: XHRBackend, useClass: MockBackend},
        {provide: APP_CONFIG, useValue: METRON_REST_CONFIG}
      ]
    })
      .compileComponents();
  }));

  it('can instantiate service when inject service',
    inject([ParserExtensionService], (service: ParserExtensionService) => {
      expect(service instanceof ParserExtensionService).toBe(true);
    }));

  it('can instantiate service with "new"', inject([Http, APP_CONFIG], (http: Http, config: IAppConfig) => {
    expect(http).not.toBeNull('http should be provided');
    let service = new ParserExtensionService(http, config);
    expect(service instanceof ParserExtensionService).toBe(true, 'new service should be ok');
  }));


  it('can provide the mockBackend as XHRBackend',
    inject([XHRBackend], (backend: MockBackend) => {
      expect(backend).not.toBeNull('backend should be provided');
    }));

  describe('when service functions', () => {
    let parserExtensionService: ParserExtensionService;
    let mockBackend: MockBackend;
    let parserExtensionConfig = new ParserExtensionConfig();
    let extensionName = 'extension-name';
    let contents = new FormData();
    contents.append("file[]","content");
    parserExtensionConfig.extensionAssemblyName = 'extension-name';
    parserExtensionConfig.extensionBundleName = 'extension.bundle';
    parserExtensionConfig.extensionIdentifier = 'extension.identifier';
    parserExtensionConfig.extensionBundleID = 'org.apache.extension.name';
    parserExtensionConfig.extensionBundleVersion = "1.0.0";
    let parserExtensionResponse: Response;
    let parserExtensionsResponse: Response;
    let deleteResponse: Response;
    let postResponse: Response;

    beforeEach(inject([Http, XHRBackend, APP_CONFIG], (http: Http, be: MockBackend, config: IAppConfig) => {
      mockBackend = be;
      parserExtensionService = new ParserExtensionService(http, config);
      parserExtensionResponse = new Response(new ResponseOptions({status: 200, body: parserExtensionConfig}));
      parserExtensionsResponse = new Response(new ResponseOptions({status: 200, body: {'extension-name' : parserExtensionConfig}}));
      deleteResponse = new Response(new ResponseOptions({status: 200}));
      postResponse = new Response(new ResponseOptions({status: 201}));
    }));

    it('post', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(postResponse));

      parserExtensionService.post('data', contents).subscribe(
      result => {
        expect(result.status).toEqual(201);
      }, error => console.log(error));
    })));

    it('get', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(parserExtensionResponse));

      parserExtensionService.get('extension-name').subscribe(
        result => {
          expect(result).toEqual(parserExtensionConfig);
        }, error => console.log(error));
    })));

    it('getAll', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(parserExtensionsResponse));

      parserExtensionService.getAll().subscribe(
        results => {
          expect(results).toEqual({'extension-name' : parserExtensionConfig});
        }, error => console.log(error));
    })));

    it('deleteParserExtension', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(deleteResponse));

      parserExtensionService.delete(extensionName).subscribe(
          result => {
            expect(result.status).toEqual(200);
          }, error => console.log(error));
    })));
  });

});


