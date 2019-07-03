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

import { DatePickerComponent } from './date-picker.component';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

describe('DatePickerComponent', () => {
  let component: DatePickerComponent;
  let fixture: ComponentFixture<DatePickerComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule
      ],
      declarations: [ DatePickerComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DatePickerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should set the date on blur', () => {
    const input = fixture.debugElement.query(By.css('input')).nativeElement;
    input.value = '2019-04-30';
    expect(component.dateStr).toBe('now');
    input.dispatchEvent(new Event('blur'));
    expect(component.dateStr).toBe('2019-04-30');
  });

  it('should not set the date on blur if value is invalid', () => {
    const input = fixture.debugElement.query(By.css('input')).nativeElement;
    input.value = 'invalid date';
    expect(component.dateStr).toBe('now');
    input.dispatchEvent(new Event('blur'));
    expect(component.dateStr).toBe('now');
  });

  it('should set the date on enter', () => {
    const input = fixture.debugElement.query(By.css('input')).nativeElement;
    input.value = '2019-04-30';
    expect(component.dateStr).toBe('now');
    const e = new KeyboardEvent('keyup', {
      code: 'Enter',
    });
    input.dispatchEvent(e);
    expect(component.dateStr).toBe('2019-04-30');
  });

  it('should not set the date on enter if value is invalid', () => {
    const input = fixture.debugElement.query(By.css('input')).nativeElement;
    input.value = 'invalid date';
    expect(component.dateStr).toBe('now');
    const e = new KeyboardEvent('keyup', {
      code: 'Enter',
    });
    input.dispatchEvent(e);
    expect(component.dateStr).toBe('now');
  });
});
