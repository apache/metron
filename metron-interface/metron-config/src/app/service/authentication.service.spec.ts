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
import { Router } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import {
  HttpClientTestingModule,
  HttpTestingController
} from '@angular/common/http/testing';
import { AuthenticationService } from './authentication.service';
import {AppConfigService} from "./app-config.service";
import {MockAppConfigService} from './mock.app-config.service';
import {HttpUtil} from '../util/httpUtil';

class MockRouter {
  navigateByUrl(url: string) {}
}

describe('AuthenticationService', () => {
  let authenticationService: AuthenticationService;
  let mockBackend: HttpTestingController;
  let router: MockRouter;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthenticationService,
        { provide: Router, useClass: MockRouter },
        { provide: AppConfigService, useClass: MockAppConfigService }
      ]
    });
    authenticationService = TestBed.get(AuthenticationService);
    mockBackend = TestBed.get(HttpTestingController);
    router = TestBed.get(Router);
  });

  afterEach(() => {
    mockBackend.verify();
  });

  describe('when service functions', () => {
    it('init', () => {
      authenticationService.init();
      authenticationService.onLoginEvent.getValue();

      const req = mockBackend.match('/api/v1/user');
      req[1].flush('user');
      expect(req[1].request.method).toBe('GET');
      expect(authenticationService.onLoginEvent.getValue()).toEqual(true);
    });

    it('login', () => {
      let errorObj = new HttpErrorResponse({
        status: 404,
        statusText: 'Not Found'
      });
      spyOn(router, 'navigateByUrl');

      authenticationService.login('test', 'test', error => {});
      let errorSpy = jasmine.createSpy('error');
      authenticationService.login('test', 'test', errorSpy);

      const req = mockBackend.match('/api/v1/user');
      req[1].flush('test');
      expect(req[1].request.method).toBe('GET');

      expect(router.navigateByUrl).toHaveBeenCalledWith('/sensors');
      expect(authenticationService.onLoginEvent.getValue()).toEqual(true);
      req[2].flush('Error', errorObj);
      expect(req[2].request.method).toBe('GET');
      expect(errorSpy).toHaveBeenCalled();
    });

    it('logout', () => {
      spyOn(HttpUtil, 'navigateToLogin');
      spyOn(authenticationService.onLoginEvent, 'next');

      authenticationService.logout();
      const req = mockBackend.match('/api/v1/logout');
      const req2 = mockBackend.expectOne('/api/v1/user');
      req.map(r => r.flush(''));
      expect(req[0].request.method).toBe('POST');

      expect(HttpUtil.navigateToLogin).toHaveBeenCalledWith();
      expect(authenticationService.onLoginEvent.getValue()).toEqual(false);
    });

    it('checkAuthentication', () => {
      let isAuthenticated = false;
      spyOn(HttpUtil, 'navigateToLogin');
      spyOn(authenticationService, 'isAuthenticated').and.callFake(function() {
        return isAuthenticated;
      });

      authenticationService.checkAuthentication();
      expect(HttpUtil.navigateToLogin).toHaveBeenCalledWith();
      isAuthenticated = true;
      authenticationService.checkAuthentication();
      expect(HttpUtil.navigateToLogin['calls'].count()).toEqual(1);
      const req = mockBackend.expectOne('/api/v1/user');
    });

    it('getCurrentUser', () => {
      authenticationService.getCurrentUser().subscribe(
        result => {
          expect(result).toEqual('');
        },
        error => console.log(error)
      );
      const req = mockBackend.match('/api/v1/user');
      req.map(r => {
        expect(r.request.method).toBe('GET');
        r.flush('');
      });
    });

    it('isAuthenticationChecked', () => {
      expect(authenticationService.isAuthenticationChecked()).toEqual(false);

      authenticationService.login('test', 'test', null);
      const req = mockBackend.match('/api/v1/user');
      req[1].flush('user');

      expect(authenticationService.isAuthenticationChecked()).toEqual(true);
    });

    it('isAuthenticated', () => {
      expect(authenticationService.isAuthenticated()).toEqual(false);

      authenticationService.login('test', 'test', null);
      authenticationService.login('test', 'test', null);
      const req = mockBackend.match('/api/v1/user');
      req[1].flush('user');

      expect(authenticationService.isAuthenticated()).toEqual(true);
    });
  });
});
