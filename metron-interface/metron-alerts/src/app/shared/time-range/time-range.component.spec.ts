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

import { TimeRangeComponent } from './time-range.component';
import { DatePickerComponent } from '../date-picker/date-picker.component';
import { MapKeysPipe } from '../pipes/map-keys.pipe';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { Filter } from 'app/model/filter';
import { TIMESTAMP_FIELD_NAME } from 'app/utils/constants';
import { DateFilterValue } from 'app/model/date-filter-value';

describe('TimeRangeComponent', () => {
  let component: TimeRangeComponent;
  let fixture: ComponentFixture<TimeRangeComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule
      ],
      declarations: [
        TimeRangeComponent,
        DatePickerComponent,
        MapKeysPipe
     ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TimeRangeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  describe('From/To time ranges', () => {

    it('should use date pickers to set range', () => {
      component.datePickerFromDate = '2000-01-31 00:00:00';
      component.datePickerToDate = '2000-02-28 00:00:00';

      const fromTS = new Date(component.datePickerFromDate).getTime();
      const toTS = new Date(component.datePickerToDate).getTime();

      spyOn(component.timeRangeChange, 'emit');
      component.applyCustomDate();

      const filter = new Filter(TIMESTAMP_FIELD_NAME, `[${fromTS} TO ${toTS}]`, false);
      filter.dateFilterValue = new DateFilterValue(fromTS, toTS);

      expect(component.timeRangeChange.emit).toHaveBeenCalledWith(filter);
    });

    it('should apply current date and time if To field empty', () => {
      jasmine.clock().mockDate(new Date('2000-02-01T12:00:01'));

      component.datePickerFromDate = '2000-01-31 00:00:00';
      component.datePickerToDate = '';

      const fromTS = new Date(component.datePickerFromDate).getTime();
      const currentTs = new Date().getTime();

      spyOn(component.timeRangeChange, 'emit');
      component.applyCustomDate();

      const filter = new Filter(TIMESTAMP_FIELD_NAME, `[${fromTS} TO ${currentTs}]`, false);
      filter.dateFilterValue = new DateFilterValue(fromTS, currentTs);

      expect(component.timeRangeChange.emit).toHaveBeenCalledWith(filter);
    });
  });

  describe('Quick Ranges', () => {
    [
      { label: 'Last 7 days', rangeId: 'last-7-days' },
      { label: 'Last 30 days', rangeId: 'last-30-days' },
      { label: 'Last 60 days', rangeId: 'last-60-days' },
      { label: 'Last 90 days', rangeId: 'last-90-days' },
      { label: 'Last 6 months', rangeId: 'last-6-months' },
      { label: 'Last 1 year', rangeId: 'last-1-year' },
      { label: 'Last 2 years', rangeId: 'last-2-years' },
      { label: 'Last 5 years', rangeId: 'last-5-years' },
      { label: 'Yesterday', rangeId: 'yesterday' },
      { label: 'Day before yesterday', rangeId: 'day-before-yesterday' },
      { label: 'This day last week', rangeId: 'this-day-last-week' },
      { label: 'Previous week', rangeId: 'previous-week' },
      { label: 'Previous month', rangeId: 'previous-month' },
      { label: 'Previous year', rangeId: 'previous-year' },
      { label: 'All time', rangeId: 'all-time' },
      { label: 'Today', rangeId: 'today' },
      { label: 'Today so far', rangeId: 'today-so-far' },
      { label: 'This week', rangeId: 'this-week' },
      { label: 'This week so far', rangeId: 'this-week-so-far' },
      { label: 'This month', rangeId: 'this-month' },
      { label: 'This year', rangeId: 'this-year' },
      { label: 'Last 5 minutes', rangeId: 'last-5-minutes' },
      { label: 'Last 15 minutes', rangeId: 'last-15-minutes' },
      { label: 'Last 30 minutes', rangeId: 'last-30-minutes' },
      { label: 'Last 1 hour', rangeId: 'last-1-hour' },
      { label: 'Last 3 hours', rangeId: 'last-3-hours' },
      { label: 'Last 6 hours', rangeId: 'last-6-hours' },
      { label: 'Last 12 hours', rangeId: 'last-12-hours' },
      { label: 'Last 24 hours', rangeId: 'last-24-hours' },
    ].forEach((test) => {
      it(`Clicking on ${test.label} should emit timeRangeChange event with range identifier: "${test.rangeId}"`, () => {
        spyOn(component.timeRangeChange, 'emit');
        fixture.debugElement.query(By.css(`span[id="${test.rangeId}"]`)).nativeElement.click();

        expect(component.timeRangeChange.emit).toHaveBeenCalledWith(new Filter(TIMESTAMP_FIELD_NAME, test.rangeId, false));
      })
    });
  });
});
