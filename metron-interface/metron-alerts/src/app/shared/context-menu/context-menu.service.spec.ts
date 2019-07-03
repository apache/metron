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
import {
  HttpClientTestingModule,
  HttpTestingController,
  TestRequest
} from '@angular/common/http/testing';
import { ContextMenuService } from './context-menu.service';
import { AppConfigService } from 'app/service/app-config.service';
import { Injectable } from '@angular/core';
import { filter } from 'rxjs/operators';
import { Spy } from 'jasmine-core';

const FAKE_CONFIG_SVC_URL = '/test/config/menu/url';

@Injectable()
class FakeAppConfigService {
  constructor() {}

  getContextMenuConfigURL() {
    return FAKE_CONFIG_SVC_URL;
  }
}

describe('ContextMenuService', () => {

  let contextMenuSvc: ContextMenuService;
  let mockBackend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ HttpClientTestingModule ],
      providers: [
        ContextMenuService,
        { provide: AppConfigService, useClass: FakeAppConfigService }
      ]
    }).compileComponents();

    contextMenuSvc = TestBed.get(ContextMenuService);
    mockBackend = TestBed.get(HttpTestingController);
  });

  it('should be created', () => {
    expect(contextMenuSvc).toBeTruthy();
  });

  it('should invoke context menu endpoint only once', () => {
    contextMenuSvc.getConfig().subscribe();

    const req: TestRequest = mockBackend.expectOne(FAKE_CONFIG_SVC_URL);
    req.flush({ isEnabled: true, config: { menuKey: [] }});

    expect(req.request.method).toEqual('GET');
    mockBackend.verify();
  });

  it('getConfig() should return with the result of config svc', () => {
    contextMenuSvc.getConfig()
      .pipe(filter(value => !!value)) // first emitted default value is undefined
      .subscribe((result) => {
        expect(result).toEqual({ isEnabled: true, config: { menuKey: [] }});
      });

    const req = mockBackend.expectOne(FAKE_CONFIG_SVC_URL);
    req.flush({ isEnabled: true, config: { menuKey: [] }});
  })

  it('should cache the first response', () => {
    contextMenuSvc.getConfig().subscribe(); // returns the default value first

    const req = mockBackend.expectOne(FAKE_CONFIG_SVC_URL);
    req.flush({ isEnabled: true, config: { menuKey: [] }});

    contextMenuSvc.getConfig().subscribe((first) => {
      contextMenuSvc.getConfig().subscribe((second) => {
        expect(first).toBe(second);
      });
    });
  });

  it('should show console error if isEnabled flag is missing', () => {
    spyOn(console, 'error');
    contextMenuSvc.getConfig().subscribe(); // returns the default value first

    const req = mockBackend.expectOne(FAKE_CONFIG_SVC_URL);
    req.flush({ config: { menuKey: [] }});

    expect(console.error).toHaveBeenCalledWith('[Context Menu] CONFIG: isEnabled and/or config entries are missing.');
  });

  it('should show console error if isEnabled value is invalid', () => {
    spyOn(console, 'error');
    contextMenuSvc.getConfig().subscribe(); // returns the default value first

    const req = mockBackend.expectOne(FAKE_CONFIG_SVC_URL);
    req.flush({ isEnabled: 'false', config: { menuKey: [] }});

    expect(console.error).toHaveBeenCalledWith('[Context Menu] CONFIG: isEnabled has to be a boolean. Defaulting to false.');
  });

  it('should default to false if isEnabled value is invalid', () => {
    contextMenuSvc.getConfig()
      .pipe(filter(value => !!value)) // first emitted default value is undefined
      .subscribe((result) => {
        expect(result).toEqual({ isEnabled: false, config: {}});
      });

    const req = mockBackend.expectOne(FAKE_CONFIG_SVC_URL);
    req.flush({ isEnabledMisspelled: true, config: { menuKey: [] }});
  });

  it('should show console error if config is missing', () => {
    spyOn(console, 'error');
    contextMenuSvc.getConfig().subscribe(); // returns the default value first

    const req = mockBackend.expectOne(FAKE_CONFIG_SVC_URL);
    req.flush({ isEnabled: true });

    expect(console.error).toHaveBeenCalledWith('[Context Menu] CONFIG: isEnabled and/or config entries are missing.');
  });

  it('should show console error if config is not an object', () => {
    spyOn(console, 'error');
    contextMenuSvc.getConfig().subscribe(); // returns the default value first

    const req = mockBackend.expectOne(FAKE_CONFIG_SVC_URL);
    req.flush({ isEnabled: true, config: '' });

    expect(console.error).toHaveBeenCalledWith('[Context Menu] CONFIG: Config entry has to be an object. Defaulting to {}.');
  });

  it('should show console error if config is an array', () => {
    spyOn(console, 'error');
    contextMenuSvc.getConfig().subscribe(); // returns the default value first

    const req = mockBackend.expectOne(FAKE_CONFIG_SVC_URL);
    req.flush({ isEnabled: true, config: [] });

    expect(console.error).toHaveBeenCalledWith('[Context Menu] CONFIG: Config entry has to be an object. Defaulting to {}.');
  });

  it('should show console error if a config entry is not an array', () => {
    spyOn(console, 'error');
    contextMenuSvc.getConfig().subscribe(); // returns the default value first

    const req = mockBackend.expectOne(FAKE_CONFIG_SVC_URL);
    req.flush({ isEnabled: true, config: { menuKey: '' } });

    expect(console.error).toHaveBeenCalledWith('[Context Menu] CONFIG: Each item in config object has to be an array.');
  });

  it('should show console error if a config entry is corrupt', () => {
    spyOn(console, 'error');
    contextMenuSvc.getConfig().subscribe(); // returns the default value first

    const req = mockBackend.expectOne(FAKE_CONFIG_SVC_URL);
    req.flush({ isEnabled: true, config: { menuKey: [ { labelMisspelled: 'menu item label', urlPattern: '/some/url/pattern/{}' } ] } });

    expect(console.error).toHaveBeenCalledTimes(2);
    expect((console.error as Spy).calls.argsFor(0)[0]).toBe(
      '[Context Menu] CONFIG: Entry is invalid. Missing field: label'
    );
    expect((console.error as Spy).calls.argsFor(1)[0]).toBe(
      '[Context Menu] CONFIG: Entry is invalid: ' +
      '{"labelMisspelled":"menu item label","urlPattern":"/some/url/pattern/{}"}'
    );
  });

  it('should default to { isEnabled: false, config: {}} if a config entry is corrupt', () => {
    contextMenuSvc.getConfig()
      .pipe(filter(value => !!value)) // first emitted default value is undefined
      .subscribe((result) => {
        expect(result).toEqual({ isEnabled: false, config: {}});
      });

    const req = mockBackend.expectOne(FAKE_CONFIG_SVC_URL);
    req.flush({ isEnabled: true, configMisspelled: { menuKey: [] }});
  });

  it('should show no error if config is valid', () => {
    spyOn(console, 'error');
    contextMenuSvc.getConfig().subscribe(); // returns the default value first

    const req = mockBackend.expectOne(FAKE_CONFIG_SVC_URL);
    req.flush({
      isEnabled: true,
      config: {
        menuKey: [
          {
            label: 'menu item label',
            urlPattern: '/some/url/pattern/{}'
          },
          {
            label: 'menu item label 2',
            urlPattern: '/some/url/pattern/2/{}'
          }
        ],
        menuKey2: [
          {
            label: 'menu item label',
            urlPattern: '/some/url/pattern/{}'
          },
          {
            label: 'menu item label 2',
            urlPattern: '/some/url/pattern/2/{}'
          }
        ]
      }
    });

    expect(console.error).toHaveBeenCalledTimes(0);
  });
});
