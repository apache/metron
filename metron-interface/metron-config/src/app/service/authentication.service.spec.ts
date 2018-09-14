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
import {Router} from '@angular/router';
import {async, inject, TestBed} from '@angular/core/testing';
import {MockBackend, MockConnection} from '@angular/http/testing';
import {HttpModule, XHRBackend, Response, ResponseOptions, Http} from '@angular/http';
import '../rxjs-operators';
import {Observable} from 'rxjs/Observable';
import {AuthenticationService} from './authentication.service';
import {APP_CONFIG, METRON_REST_CONFIG} from '../app.config';
import {IAppConfig} from '../app.config.interface';
import {CookieService} from 'ng2-cookies';

class MockRouter {
    navigateByUrl(url: string) {
    }
}

describe('AuthenticationService', () => {
    beforeEach(async(() => {
        TestBed.configureTestingModule({
            imports: [HttpModule],
            providers: [
              AuthenticationService,
              {provide: XHRBackend, useClass: MockBackend},
              {provide: Router, useClass: MockRouter},
              {provide: APP_CONFIG, useValue: METRON_REST_CONFIG},
              {provide: CookieService}
            ]
        })
            .compileComponents();
    }));

    describe('when service functions', () => {
        it('can instantiate service when inject service',
            inject([AuthenticationService], (service: AuthenticationService) => {
                expect(service instanceof AuthenticationService).toBe(true);
        }));

    });

    describe('when service functions', () => {
        let authenticationService: AuthenticationService;
        let mockBackend: MockBackend;
        let userResponse: Response;
        let userName = 'test';
        let router: MockRouter;

        beforeEach(inject([Http, XHRBackend, Router, AuthenticationService, APP_CONFIG],
            (http: Http, be: MockBackend, mRouter: MockRouter, service: AuthenticationService, config: IAppConfig) => {
            mockBackend = be;
            router = mRouter;
            authenticationService = service;
            userResponse = new Response(new ResponseOptions({status: 200, body: userName}));
        }));


        it('getCurrentUser', async(inject([], () => {
            mockBackend.connections.subscribe((c: MockConnection) => userResponse);
            authenticationService.getCurrentUser(null).subscribe(
                result => {
                    expect(result).toEqual('');
                }, error => console.log(error));
        })));

    });
});