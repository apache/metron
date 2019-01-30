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
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthenticationService } from '../service/authentication.service';
import { LoginComponent } from './login.component';
import { Observable } from 'rxjs';
import { ActivatedRoute, Params } from '@angular/router';
import { LoginModule } from './login.module';
import { APP_CONFIG, METRON_REST_CONFIG } from '../app.config';

class MockAuthenticationService {
  public login(username: string, password: string, onError): void {
    if (username === 'success') {
      onError({ status: 200 });
    }

    if (username === 'failure') {
      onError({ status: 401 });
    }
  }
}

class MockActivatedRoute {
  queryParams: Observable<Params> = Observable.create(observer => {
    observer.complete();
  });

  setSessionExpired() {
    this.queryParams = Observable.create(observer => {
      observer.next({ sessionExpired: 'true' });
      observer.complete();
    });
  }
}

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let activatedRoute: MockActivatedRoute;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [LoginModule],
      providers: [
        { provide: ActivatedRoute, useClass: MockActivatedRoute },
        { provide: AuthenticationService, useClass: MockAuthenticationService },
        { provide: APP_CONFIG, useValue: METRON_REST_CONFIG }
      ]
    });
    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    activatedRoute = TestBed.get(ActivatedRoute);
  }));

  it('should create an instance', async(() => {
    expect(component).toBeDefined();
  }));

  it('can instantiate login component', async(() => {
    component.user = 'success';
    component.password = 'success';
    component.login();
    expect(component.loginFailure).toEqual('');

    component.user = 'failure';
    component.password = 'failure';
    component.login();
    expect(component.loginFailure).toEqual('Login failed for failure');
  }));

  it('can handle sessionExpired', async(() => {
    activatedRoute.setSessionExpired();
    let sessionExpiredComponent = TestBed.createComponent(LoginComponent)
      .componentInstance;

    expect(sessionExpiredComponent.loginFailure).toEqual('Session has expired');
  }));
});
