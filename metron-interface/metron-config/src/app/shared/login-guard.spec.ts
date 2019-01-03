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
import {AuthenticationService} from '../service/authentication.service';
import {Router} from '@angular/router';
import {LoginGuard} from './login-guard';
import {HttpClientTestingModule} from "@angular/common/http/testing";
import {AppConfigService} from '../service/app-config.service';
import {MockAppConfigService} from '../service/mock.app-config.service';

class MockAuthenticationService extends AuthenticationService {
  public logout(): void {}
}

class MockRouter {
  navigateByUrl(): any {}
}

describe('LoginGuard', () => {

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        LoginGuard,
        {provide: AuthenticationService, useClass: MockAuthenticationService},
        {provide: Router, useClass: MockRouter},
        { provide: AppConfigService, useClass: MockAppConfigService }
      ]
    })
      .compileComponents();

  }));

  it('can instantiate auth guard',
    inject([LoginGuard], (loginGaurd: LoginGuard) => {
      expect(loginGaurd instanceof LoginGuard).toBe(true);
  }));

  it('test when login is checked',
    inject([LoginGuard, AuthenticationService], (loginGuard: LoginGuard, authenticationService: MockAuthenticationService) => {

      spyOn(authenticationService, 'clearAuthentication');

      expect(loginGuard.canActivate(null, null)).toBe(true);

      expect(authenticationService.clearAuthentication).toHaveBeenCalled();

  }));

});
