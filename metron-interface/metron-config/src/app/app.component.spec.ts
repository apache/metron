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
import { Inject } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { AppComponent } from './app.component';
import { AuthenticationService } from './service/authentication.service';
import { AppModule } from './app.module';
import { HttpResponse, HttpClient } from '@angular/common/http';
import {AppConfigService} from './service/app-config.service';
import {MockAppConfigService} from './service/mock.app-config.service';

class MockAuthenticationService extends AuthenticationService {

  public checkAuthentication() {}

  public getCurrentUser(options): Observable<HttpResponse<{}>> {
    return Observable.create(observer => {
      observer.next(new HttpResponse({ body: 'test' }));
      observer.complete();
    });
  }
}

class MockRouter {
  navigateByUrl(url: string) {}
}

describe('App: Static', () => {
  let comp: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let authenticationService: AuthenticationService;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [AppModule],
      providers: [
        { provide: AuthenticationService, useClass: MockAuthenticationService },
        { provide: Router, useClass: MockRouter },
        { provide: AppConfigService, useClass: MockAppConfigService }
      ]
    });
    fixture = TestBed.createComponent(AppComponent);
    comp = fixture.componentInstance;
    authenticationService = TestBed.get(AuthenticationService);
  }));

  it('should create the app', () => {
    expect(comp).toBeTruthy();
  });

  it('should return true/false from loginevent and loggedIn should be set', () => {
    authenticationService.onLoginEvent.next(true);
    expect(comp.loggedIn).toEqual(true);
    authenticationService.onLoginEvent.next(false);
    expect(comp.loggedIn).toEqual(false);
  });
});
