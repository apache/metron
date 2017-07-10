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
import {Inject} from '@angular/core';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ResponseOptions, RequestOptions, Response, Http} from '@angular/http';
import {Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {AppComponent} from './app.component';
import {AuthenticationService} from './service/authentication.service';
import {AppModule} from './app.module';
import {APP_CONFIG, METRON_REST_CONFIG} from './app.config';
import {IAppConfig} from './app.config.interface';

class MockAuthenticationService extends AuthenticationService {

  constructor(private http2: Http, private router2: Router, @Inject(APP_CONFIG) private config2: IAppConfig) {
    super(http2, router2, config2);
  }

  public checkAuthentication() {
  }

  public getCurrentUser(options: RequestOptions): Observable<Response> {
    return Observable.create(observer => {
      observer.next(new Response(new ResponseOptions({body: 'test'})));
      observer.complete();
    });
  }
}

class MockRouter {
  navigateByUrl(url: string) {
  }
}

describe('App: Static', () => {

  let comp: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let authenticationService: AuthenticationService;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      imports: [AppModule],
      providers: [
        {provide: Http},
        {provide: AuthenticationService, useClass: MockAuthenticationService},
        {provide: Router, useClass: MockRouter},
        {provide: APP_CONFIG, useValue: METRON_REST_CONFIG}
      ]
    }).compileComponents()
      .then(() => {
        fixture = TestBed.createComponent(AppComponent);
        comp = fixture.componentInstance;
        authenticationService = fixture.debugElement.injector.get(AuthenticationService);
      });
  }));

  it('should create the app', () => {
    expect(comp).toBeTruthy();
  });

  it('should return true/false from loginevent and loggedIn should be set', () => {

    expect(comp.loggedIn).toEqual(false);
    authenticationService.onLoginEvent.emit(true);
    expect(comp.loggedIn).toEqual(true);
    authenticationService.onLoginEvent.emit(false);
    expect(comp.loggedIn).toEqual(false);

  });

});
