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
import {CookieService} from 'ng2-cookies';

@Injectable()
export class AuthenticationService {

  private currentUser: Observable<string>;
  userUrl: string = this.config.apiEndpoint + '/user';
  ssoCookie: string = 'hadoop-jwt';
  defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};
  onLoginEvent: EventEmitter<boolean> = new EventEmitter<boolean>();

  constructor(private http: Http,
              private router: Router,
              @Inject(APP_CONFIG) private config: IAppConfig,
              private cookieService: CookieService) {
     this.init();
  }

  public init() {
      this.currentUser = this.getCurrentUser(new RequestOptions({headers: new Headers(this.defaultHeaders)}))
        .map(result => {
          this.onLoginEvent.emit(true);
          return result.text();
        }, error => {
          this.onLoginEvent.emit(false);
        });
  }

  public getCurrentUser(options: RequestOptions): Observable<Response> {
    return this.http.get(this.userUrl, options);
  }

  public getCurrentUserName(): Observable<string> {
    return this.currentUser;
  }

  private logoutUrl(originalUrl: string): string {
    return `/logout?originalUrl=${originalUrl}`;
  }

  public logout() {
    // clear the authentication cookie
    this.cookieService.delete(this.ssoCookie);
    window.location.href = this.logoutUrl(window.location.href);
  }
}
