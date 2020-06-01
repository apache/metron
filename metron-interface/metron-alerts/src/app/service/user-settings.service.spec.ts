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
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AppConfigService } from './app-config.service';
import { UserSettingsService } from './user-settings.service';

describe('UserSettingsService', () => {
  let service: UserSettingsService;
  let httpTestingController: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ HttpClientTestingModule ],
      providers: [
        UserSettingsService,
        { provide: AppConfigService, useClass: () => {
          return {
            getApiRoot: () => '/api',
          }
        } },
      ]
    });
    service = TestBed.get(UserSettingsService);
    httpTestingController = TestBed.get(HttpTestingController);
  });

  it('should load from server', (done) => {
    service.load()
      .then(() => {
        service.get('foo').subscribe((value) => {
          expect(value).toBe('bar');
          done();
        });
      });

    const req = httpTestingController.expectOne('/api/hdfs?path=user-settings');
    expect(req.request.method).toEqual('GET');
    req.flush({
      foo: 'bar'
    });

    httpTestingController.verify();
  });

  it('should save properly', () => {
    service.save({
      foo: 'bar'
    }).subscribe();

    service.get('foo').subscribe((value) => {
      expect(value).toBe('bar');
    });

    const req = httpTestingController.expectOne('/api/hdfs?path=user-settings');
    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toEqual({
      foo: 'bar'
    });
    req.flush({});

    httpTestingController.verify();
  });
});
