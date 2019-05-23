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
    req.flush({ menuKey: [] });

    expect(req.request.method).toEqual('GET');
    mockBackend.verify();
  });

  it('getConfig() should return with the result of config svc', () => {
    contextMenuSvc.getConfig().subscribe(); // returns the default value first

    const req = mockBackend.expectOne(FAKE_CONFIG_SVC_URL);
    req.flush({ menuKey: [] });

    contextMenuSvc.getConfig().subscribe((result) => {
      expect(result).toEqual({ menuKey: [] });
    });
  })

  it('should cache the first response', () => {
    contextMenuSvc.getConfig().subscribe(); // returns the default value first

    const req = mockBackend.expectOne(FAKE_CONFIG_SVC_URL);
    req.flush({ menuKey: [] });

    contextMenuSvc.getConfig().subscribe((first) => {
      contextMenuSvc.getConfig().subscribe((second) => {
        expect(first).toBe(second);
      });
    });
  });
});
