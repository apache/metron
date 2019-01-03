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
import {
  CanActivate,
  Router,
  ActivatedRouteSnapshot,
  RouterStateSnapshot
} from '@angular/router';
import { Observable } from 'rxjs';
import { AuthenticationService } from '../service/authentication.service';
import {AppConfigService} from "../service/app-config.service";
import {HttpUtil} from "../utils/httpUtil";

@Injectable()
export class AuthGuard implements CanActivate {

  constructor(private authService: AuthenticationService, private router: Router, private appConfigService: AppConfigService) {}

  canActivate(next: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
    if (!this.authService.isAuthenticationChecked()) {
      return Observable.create(observer => {
        this.authService.onLoginEvent.subscribe(isLoggedIn => {
          if (isLoggedIn) {
            observer.next(true);
            observer.complete();
          } else {
            observer.next(false);
            observer.complete();
            HttpUtil.navigateToLogin();
          }
        });
      });
    }
    return this.authService.isAuthenticated();
  }
}
