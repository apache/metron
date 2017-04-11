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
import {HdfsService} from './hdfs.service';

describe('HdfsService', () => {

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [HttpModule],
      providers: [
        HdfsService,
        {provide: XHRBackend, useClass: MockBackend},
        {provide: APP_CONFIG, useValue: METRON_REST_CONFIG}
      ]
    })
      .compileComponents();
  }));

  it('can instantiate service when inject service',
    inject([HdfsService], (service: HdfsService) => {
      expect(service instanceof HdfsService).toBe(true);
    }));

  it('can instantiate service with "new"', inject([Http, APP_CONFIG], (http: Http, config: IAppConfig) => {
    expect(http).not.toBeNull('http should be provided');
    let service = new HdfsService(http, config);
    expect(service instanceof HdfsService).toBe(true, 'new service should be ok');
  }));


  it('can provide the mockBackend as XHRBackend',
    inject([XHRBackend], (backend: MockBackend) => {
      expect(backend).not.toBeNull('backend should be provided');
    }));

  describe('when service functions', () => {
    let hdfsService: HdfsService;
    let mockBackend: MockBackend;
    let fileList = ['file1', 'file2'];
    let contents = 'file contents';
    let listResponse: Response;
    let readResponse: Response;
    let postResponse: Response;
    let deleteResponse: Response;

    beforeEach(inject([Http, XHRBackend, APP_CONFIG], (http: Http, be: MockBackend, config: IAppConfig) => {
      mockBackend = be;
      hdfsService = new HdfsService(http, config);
      listResponse = new Response(new ResponseOptions({status: 200, body: fileList}));
      readResponse = new Response(new ResponseOptions({status: 200, body: contents}));
      postResponse = new Response(new ResponseOptions({status: 200}));
      deleteResponse = new Response(new ResponseOptions({status: 200}));
    }));

    it('list', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(listResponse));
      hdfsService.list('/path').subscribe(
        result => {
          expect(result).toEqual(fileList);
        }, error => console.log(error));
    })));

    it('read', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(readResponse));
      hdfsService.read('/path').subscribe(
        result => {
          expect(result).toEqual(contents);
        }, error => console.log(error));
    })));

    it('post', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(postResponse));
      hdfsService.post('/path', contents).subscribe(
          result => {
            expect(result.status).toEqual(200);
          }, error => console.log(error));
    })));

    it('deleteFile', async(inject([], () => {
      mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(deleteResponse));
      hdfsService.deleteFile('/path').subscribe(
          result => {
            expect(result.status).toEqual(200);
          }, error => console.log(error));
    })));
  });


});
