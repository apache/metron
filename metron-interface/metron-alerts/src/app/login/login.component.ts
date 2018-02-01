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
import { Component } from '@angular/core';
import { AuthenticationService } from '../service/authentication.service';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'metron-alerts-login',
  templateUrl: 'login.component.html',
  styleUrls: ['login.component.scss'],
})
export class LoginComponent {

  user: string;
  password: string;
  loginFailure = '';

  constructor(private authenticationService: AuthenticationService, private activatedRoute: ActivatedRoute) {
    this.activatedRoute.queryParams.subscribe(params => {
      if (params['sessionExpired'] === 'true') {
        this.loginFailure = 'Session has expired';
      }
    });
  }

  login(): void {
    this.authenticationService.login(this.user, this.password, error => {
      if (error.status === 401) {
        this.loginFailure = 'Login failed for ' + this.user;
      }
    });
  }

}
