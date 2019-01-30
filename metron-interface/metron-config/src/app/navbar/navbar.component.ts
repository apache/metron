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
import { Component, OnInit, OnDestroy } from '@angular/core';
import { AuthenticationService } from '../service/authentication.service';
import { Subscription } from 'rxjs';
import {HttpHeaders} from "@angular/common/http";

@Component({
  selector: 'metron-config-navbar',
  templateUrl: 'navbar.html',
  styleUrls: ['navbar.component.scss']
})
export class NavbarComponent implements OnInit, OnDestroy {
  currentUser;
  authService: Subscription;

  constructor(private authenticationService: AuthenticationService) {}

  ngOnInit() {
    this.authService = this.authenticationService
      .getCurrentUser({ headers: new HttpHeaders({'Accept': 'text/plain'}), responseType: 'text'})
      .subscribe(r => {
        this.currentUser = r;
      });
  }

  logout() {
    this.authenticationService.logout();
  }

  ngOnDestroy() {
    this.authService.unsubscribe();
  }
}
