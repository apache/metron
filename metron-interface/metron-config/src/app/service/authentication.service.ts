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
import {Injectable, EventEmitter, Inject}     from '@angular/core';
import {Http, Headers, RequestOptions, Response} from '@angular/http';
import {Router} from '@angular/router';
import {Observable}     from 'rxjs/Observable';
import {IAppConfig} from '../app.config.interface';
import {APP_CONFIG} from '../app.config';

@Injectable()
export class AuthenticationService {

  private static USER_NOT_VERIFIED: string = 'USER-NOT-VERIFIED';
  private currentUser: string = AuthenticationService.USER_NOT_VERIFIED;
  loginUrl: string = this.config.apiEndpoint + '/user';
  logoutUrl: string = '/logout';
  defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};
  onLoginEvent: EventEmitter<boolean> = new EventEmitter<boolean>();

  constructor(private http: Http, private router: Router, @Inject(APP_CONFIG) private config: IAppConfig) {
    this.init();
  }

  public init() {
      this.getCurrentUser(new RequestOptions({headers: new Headers(this.defaultHeaders)})).subscribe((response: Response) => {
        this.currentUser = response.text();
        if (this.currentUser) {
          this.onLoginEvent.emit(true);
        }
      }, error => {
        this.onLoginEvent.emit(false);
      });
  }

  public login(username: string, password: string, onError): void {
    let loginHeaders: Headers = new Headers(this.defaultHeaders);
    loginHeaders.append('authorization', 'Basic ' + btoa(username + ':' + password));
    let loginOptions: RequestOptions = new RequestOptions({headers: loginHeaders});
    this.getCurrentUser(loginOptions).subscribe((response: Response) => {
        this.currentUser = response.text();
        this.router.navigateByUrl('/sensors');
        this.onLoginEvent.emit(true);
      },
      error => {
        onError(error);
      });
  }

  public logout(): void {
    this.http.post(this.logoutUrl, {}, new RequestOptions({headers: new Headers(this.defaultHeaders)})).subscribe(response => {
        this.currentUser = AuthenticationService.USER_NOT_VERIFIED;
        this.onLoginEvent.emit(false);
        this.router.navigateByUrl('/login');
      },
      error => {
        console.log(error);
      });
  }

  public checkAuthentication() {
    if (!this.isAuthenticated()) {
      this.router.navigateByUrl('/login');
    }
  }

  public getCurrentUser(options: RequestOptions): Observable<Response> {
    return this.http.get(this.loginUrl, options);
  }

  public isAuthenticationChecked(): boolean {
    return this.currentUser !== AuthenticationService.USER_NOT_VERIFIED;
  }

  public isAuthenticated(): boolean {
    return this.currentUser !== AuthenticationService.USER_NOT_VERIFIED && this.currentUser != null;
  }
}
