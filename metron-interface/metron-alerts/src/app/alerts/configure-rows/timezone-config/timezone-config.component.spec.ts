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

import { TimezoneConfigComponent } from './timezone-config.component';
import { SwitchComponent } from 'app/shared/switch/switch.component';
import { TimezoneConfigService } from './timezone-config.service';
import { By } from '@angular/platform-browser';

class TimezoneConfigServiceStub {
  toggleUTCtoLocal(showLocal) {}
}

describe('TimezoneConfigComponent', () => {
  let component: TimezoneConfigComponent;
  let fixture: ComponentFixture<TimezoneConfigComponent>;
  let service: TimezoneConfigService;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        TimezoneConfigComponent,
        SwitchComponent,
      ],
      providers: [
        { provide: TimezoneConfigService, useClass: TimezoneConfigServiceStub },
      ]
    })
    .compileComponents();
    service = TestBed.get(TimezoneConfigService);
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TimezoneConfigComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should toggle between UTC and Local time when clicked', () => {
    spyOn(service, 'toggleUTCtoLocal');
    spyOn(component, 'toggleTimezoneConfig').and.callThrough();
    fixture.detectChanges();

    const timeToggle = fixture.debugElement.query(By.css('[data-qe-id="UTCtoLocalToggle"] input')).nativeElement;
    timeToggle.click();
    fixture.detectChanges();
    expect(timeToggle.checked).toBe(true);
    expect(component.toggleTimezoneConfig).toHaveBeenCalledWith(true);
    expect(service.toggleUTCtoLocal).toHaveBeenCalledWith(true);

    timeToggle.click();
    fixture.detectChanges();
    expect(timeToggle.checked).toBe(false);
    expect(component.toggleTimezoneConfig).toHaveBeenCalledWith(false);
    expect(component.timezoneConfigService.toggleUTCtoLocal).toHaveBeenCalledWith(false);
  });
});
