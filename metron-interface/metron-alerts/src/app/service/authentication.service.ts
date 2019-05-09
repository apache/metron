
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
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import {Router} from '@angular/router';
import { ReplaySubject } from 'rxjs';
import { GlobalConfigService } from './global-config.service';
import { DataSource } from './data-source';
import { AppConfigService } from './app-config.service';
import {HttpUtil} from "../utils/httpUtil";

@Injectable()
export class AuthenticationService {

  private static USER_NOT_VERIFIED = 'USER-NOT-VERIFIED';
  private currentUser: string = AuthenticationService.USER_NOT_VERIFIED;
  userUrl = this.appConfigService.getApiRoot() + '/user';
  logoutUrl = this.appConfigService.getApiRoot() + '/logout';
  onLoginEvent: ReplaySubject<boolean> = new ReplaySubject<boolean>();
  $onLoginEvent = this.onLoginEvent.asObservable();

  constructor(private http: HttpClient,
              private router: Router,
              private globalConfigService: GlobalConfigService,
              private dataSource: DataSource,
              private appConfigService: AppConfigService) {
    this.init();
  }

  public init() {
      this.getCurrentUser({ headers: new HttpHeaders({'Accept': 'text/plain'}), responseType: 'text'}).subscribe((response) => {
        this.currentUser = response.toString();
        if (this.currentUser) {
          this.onLoginEvent.next(true);
          this.dataSource.getDefaultAlertTableColumnNames();
        }
      }, error => {
        this.onLoginEvent.next(false);
      });
  }

  public login(username: string, password: string, onError): void {
    let credentials = btoa(username + ':' + password);
    this.getCurrentUser({ headers: new HttpHeaders({'Authorization': `Basic ${credentials}`, 'Accept': 'text/plain'}), responseType: 'text' })
        .subscribe((response) => {
          this.currentUser = response.toString();
          this.router.navigateByUrl('/alerts-list');
          this.onLoginEvent.next(true);
          this.globalConfigService.get();
          this.dataSource.getDefaultAlertTableColumnNames();
      },
      error => {
        onError(error);
      });
  }

  public logout(): void {
    this.http.post(this.logoutUrl, {}).subscribe(response => {
        this.clearAuthentication();
        HttpUtil.navigateToLogin();
      },
      error => {
        console.log('Logout failed:', error);
        HttpUtil.navigateToLogin();
      });
  }

  public checkAuthentication() {
    if (!this.isAuthenticated()) {
      HttpUtil.navigateToLogin();
    }
  }

  public getCurrentUser(options?: {}) {
    return this.http.get(this.userUrl, options);
  }

  public getCurrentUserName(): string {
    return this.currentUser;
  }

  public isAuthenticationChecked(): boolean {
    return this.currentUser !== AuthenticationService.USER_NOT_VERIFIED;
  }

  public isAuthenticated(): boolean {
    return this.currentUser !== AuthenticationService.USER_NOT_VERIFIED && this.currentUser != null;
  }

  public clearAuthentication(): void {
    this.currentUser = AuthenticationService.USER_NOT_VERIFIED;
    this.onLoginEvent.next(false);
  }
}
