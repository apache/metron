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
import { TestBed, async, ComponentFixture } from '@angular/core/testing';

import { AppComponent } from './app.component';
import { Component } from '@angular/core';
import { AuthenticationService } from './service/authentication.service';
import { of } from 'rxjs';
import { DialogService } from './service/dialog.service';
import { MetronDialogComponent } from './shared/metron-dialog/metron-dialog.component';

@Component({ selector: 'router-outlet', template: '' })
class RouterOutletStubComponent {}

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      providers: [
        DialogService,
        { provide: AuthenticationService, useValue: { onLoginEvent: of(true) } }
      ],
      declarations: [
        AppComponent,
        MetronDialogComponent,
        RouterOutletStubComponent,
      ],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
