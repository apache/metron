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

import { AutoPollingComponent } from './auto-polling.component';
import { AutoPollingService } from './auto-polling.service';

describe('AutoPollingComponent', () => {
  let component: AutoPollingComponent;
  let fixture: ComponentFixture<AutoPollingComponent>;
  let autoPollingSvc: AutoPollingService;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AutoPollingComponent ],
      providers: [
        { provide: AutoPollingService, useClass: () => { return {
          getIsPollingActive: () => {},
          start: () => {},
          stop: () => {},
        } } },
      ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AutoPollingComponent);
    component = fixture.componentInstance;

    autoPollingSvc = TestBed.get(AutoPollingService);

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have auto polling service injected', () => {
    expect(component.autoPollingSvc).toBeTruthy();
  });

  it('toggle should call stop on svc when polling is active', () => {
    spyOn(autoPollingSvc, 'getIsPollingActive').and.returnValue(true);
    spyOn(autoPollingSvc, 'stop');
    spyOn(autoPollingSvc, 'start');

    component.onToggle();

    expect(autoPollingSvc.start).not.toHaveBeenCalled();
    expect(autoPollingSvc.stop).toHaveBeenCalled();
  });

  it('toggle should call start on svc when polling is inactive', () => {
    spyOn(autoPollingSvc, 'getIsPollingActive').and.returnValue(false);
    spyOn(autoPollingSvc, 'stop');
    spyOn(autoPollingSvc, 'start');

    component.onToggle();

    expect(autoPollingSvc.start).toHaveBeenCalled();
    expect(autoPollingSvc.stop).not.toHaveBeenCalled();
  });
});
