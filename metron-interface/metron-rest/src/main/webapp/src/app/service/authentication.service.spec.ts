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
              {provide: APP_CONFIG, useValue: METRON_REST_CONFIG}
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

        it('init', async(inject([], () => {
            let userResponsesuccess = true;
            spyOn(authenticationService.onLoginEvent, 'emit');
            spyOn(authenticationService, 'getCurrentUser').and.callFake(function() {
                if (userResponsesuccess) {
                    return Observable.create(observer => {
                        observer.next(userResponse);
                        observer.complete();
                    });
                }

                return Observable.throw('Error');
            });

            authenticationService.init();
            expect(authenticationService.onLoginEvent.emit).toHaveBeenCalledWith(true);

            userResponsesuccess = false;
            authenticationService.init();
            expect(authenticationService.onLoginEvent.emit['calls'].count()).toEqual(2);

        })));

        it('login', async(inject([], () => {
            let responseMessageSuccess = true;
            mockBackend.connections.subscribe((c: MockConnection) => {
                if (responseMessageSuccess) {
                    c.mockRespond(userResponse);
                } else {
                    c.mockError(new Error('login failed'));
                }
            });

            spyOn(router, 'navigateByUrl');
            spyOn(authenticationService.onLoginEvent, 'emit');
            authenticationService.login('test', 'test', error => {
            });

            expect(router.navigateByUrl).toHaveBeenCalledWith('/sensors');
            expect(authenticationService.onLoginEvent.emit).toHaveBeenCalled();

            responseMessageSuccess = false;
            let errorSpy = jasmine.createSpy('error');
            authenticationService.login('test', 'test', errorSpy);
            expect(errorSpy).toHaveBeenCalledWith(new Error('login failed'));

        })));

        it('logout', async(inject([], () => {
            let responseMessageSuccess = true;
            mockBackend.connections.subscribe((c: MockConnection) => {
                if (responseMessageSuccess) {
                    c.mockRespond(userResponse);
                } else {
                    c.mockError(new Error('login failed'));
                }
            });

            spyOn(router, 'navigateByUrl');
            spyOn(authenticationService.onLoginEvent, 'emit');
            authenticationService.logout();

            expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
            expect(authenticationService.onLoginEvent.emit).toHaveBeenCalled();

            responseMessageSuccess = false;
            spyOn(console, 'log');
            authenticationService.logout();
            expect(console.log).toHaveBeenCalled();

        })));

        it('checkAuthentication', async(inject([], () => {
            let isAuthenticated = false;
            spyOn(router, 'navigateByUrl');
            spyOn(authenticationService, 'isAuthenticated').and.callFake(function() {
                return isAuthenticated;
            });

            authenticationService.checkAuthentication();
            expect(router.navigateByUrl).toHaveBeenCalledWith('/login');

            isAuthenticated = true;
            authenticationService.checkAuthentication();
            expect(router.navigateByUrl['calls'].count()).toEqual(1);
        })));

        it('getCurrentUser', async(inject([], () => {
            mockBackend.connections.subscribe((c: MockConnection) => userResponse);
            authenticationService.getCurrentUser(null).subscribe(
                result => {
                    expect(result).toEqual('');
                }, error => console.log(error));
        })));

        it('isAuthenticationChecked', async(inject([], () => {
            mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(userResponse));

            expect(authenticationService.isAuthenticationChecked()).toEqual(false);

            authenticationService.login('test', 'test', null);
            expect(authenticationService.isAuthenticationChecked()).toEqual(true);

        })));

        it('isAuthenticated', async(inject([], () => {
            mockBackend.connections.subscribe((c: MockConnection) => c.mockRespond(userResponse));

            expect(authenticationService.isAuthenticated()).toEqual(false);

            authenticationService.login('test', 'test', null);
            expect(authenticationService.isAuthenticated()).toEqual(true);

        })));
    });


});
