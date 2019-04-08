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
import {
  Component,
  ViewChild,
  ElementRef,
  HostListener,
  EventEmitter,
  Output,
  Input,
  OnChanges,
  SimpleChanges
} from '@angular/core';
import * as moment from 'moment/moment';

import { Filter } from '../../model/filter';
import {
    DEFAULT_TIMESTAMP_FORMAT, CUSTOMM_DATE_RANGE_LABEL,
    TIMESTAMP_FIELD_NAME, ALL_TIME
} from '../../utils/constants';
import { DateFilterValue } from '../../model/date-filter-value';

@Component({
  selector: 'app-time-range',
  templateUrl: './time-range.component.html',
  styleUrls: ['./time-range.component.scss']
})
export class TimeRangeComponent implements OnChanges {
  toDateStr = '';
  fromDateStr = '';
  datePickerFromDate = '';
  datePickerToDate = '';
  selectedTimeRangeValue = 'Last 15 minutes';

  @Input() disabled = false;
  @Input() selectedTimeRange: Filter;
  @ViewChild('datePicker') datePicker: ElementRef;
  @Output() timeRangeChange = new EventEmitter<Filter>();

  readonly timeRangeMappingCol1 = {
    'Last 7 days':            'last-7-days',
    'Last 30 days':           'last-30-days',
    'Last 60 days':           'last-60-days',
    'Last 90 days':           'last-90-days',
    'Last 6 months':          'last-6-months',
    'Last 1 year':            'last-1-year',
    'Last 2 years':           'last-2-years',
    'Last 5 years':           'last-5-years'
  };
  readonly timeRangeMappingCol2 = {
    'Yesterday':              'yesterday',
    'Day before yesterday':   'day-before-yesterday',
    'This day last week':     'this-day-last-week',
    'Previous week':          'previous-week',
    'Previous month':         'previous-month',
    'Previous year':          'previous-year',
    'All time':               ALL_TIME
  };
  readonly timeRangeMappingCol3 = {
    'Today':                  'today',
    'Today so far':           'today-so-far',
    'This week':              'this-week',
    'This week so far':       'this-week-so-far',
    'This month':             'this-month',
    'This year':              'this-year'
  };
  readonly timeRangeMappingCol4 = {
    'Last 5 minutes':         'last-5-minutes',
    'Last 15 minutes':        'last-15-minutes',
    'Last 30 minutes':        'last-30-minutes',
    'Last 1 hour':            'last-1-hour',
    'Last 3 hours':           'last-3-hours',
    'Last 6 hours':           'last-6-hours',
    'Last 12 hours':          'last-12-hours',
    'Last 24 hours':          'last-24-hours'
  };

  constructor() { }

  ngOnChanges(changes: SimpleChanges) {
    if (changes && changes['selectedTimeRange']) {
      this.onSelectedTimeRangeChange();
    }
  }

  onSelectedTimeRangeChange() {
    let foundQuickRange = false;
    const merged = Object.assign(
      {},
      this.timeRangeMappingCol1,
      this.timeRangeMappingCol2,
      this.timeRangeMappingCol3,
      this.timeRangeMappingCol4
    );

    Object.keys(merged).forEach(key => {
      if (this.selectedTimeRange.value === merged[key]) {
        foundQuickRange = true;
        this.selectedTimeRangeValue = key;
        if (this.selectedTimeRange.dateFilterValue) {
          this.toDateStr = moment(this.selectedTimeRange.dateFilterValue.toDate).format(DEFAULT_TIMESTAMP_FORMAT);
          this.fromDateStr = moment(this.selectedTimeRange.dateFilterValue.fromDate).format(DEFAULT_TIMESTAMP_FORMAT);

          this.datePickerFromDate = '';
          this.datePickerToDate = '';
        }
      }
    });

    if (!foundQuickRange) {
      this.selectedTimeRangeValue = CUSTOMM_DATE_RANGE_LABEL;
      this.toDateStr = this.selectedTimeRange.dateFilterValue.toDate !== null ?
                        moment(this.selectedTimeRange.dateFilterValue.toDate).format(DEFAULT_TIMESTAMP_FORMAT) :
                        'now';
      this.fromDateStr = moment(this.selectedTimeRange.dateFilterValue.fromDate).format(DEFAULT_TIMESTAMP_FORMAT);

      this.datePickerFromDate = this.fromDateStr;
      this.datePickerToDate = this.selectedTimeRange.dateFilterValue.toDate !== null ? this.toDateStr : '';
    }
  }

  getTimeRangeStr() {
    let mappingVal = this.timeRangeMappingCol1[this.selectedTimeRangeValue];
    if (!mappingVal) {
      mappingVal = this.timeRangeMappingCol2[this.selectedTimeRangeValue];
    }
    if (!mappingVal) {
      mappingVal = this.timeRangeMappingCol3[this.selectedTimeRangeValue];
    }
    if (!mappingVal) {
      mappingVal = this.timeRangeMappingCol4[this.selectedTimeRangeValue];
    }
    return mappingVal;
  }

  selectTimeRange($event, range: string) {
    this.hideDatePicker();
    this.selectedTimeRangeValue = $event.target.textContent.trim();
    this.datePickerFromDate = '';
    this.datePickerToDate = '';
    this.timeRangeChange.emit(new Filter(TIMESTAMP_FIELD_NAME, range, false));
  }

  hideDatePicker() {
    this.datePicker.nativeElement.classList.remove('show');
  }

  applyCustomDate() {
    this.hideDatePicker();
    this.selectedTimeRangeValue = CUSTOMM_DATE_RANGE_LABEL;
    this.toDateStr = this.datePickerToDate.length > 0  ? moment(this.datePickerToDate).format(DEFAULT_TIMESTAMP_FORMAT) : 'now';
    this.fromDateStr = moment(this.datePickerFromDate).format(DEFAULT_TIMESTAMP_FORMAT);

    let toDate = this.datePickerToDate.length > 0 ? new Date(this.toDateStr).getTime() : new Date().getTime();
    let fromDate = new Date(this.fromDateStr).getTime();
    let toDateExpression = ' TO ' + toDate;

    let value = '[' + fromDate + toDateExpression + ']';
    let filter = new Filter(TIMESTAMP_FIELD_NAME, value, false);
    filter.dateFilterValue = new DateFilterValue(fromDate, toDate);
    this.timeRangeChange.emit(filter);
  }

  isPikaSelectElement(targetElement: HTMLElement): boolean {
    while (targetElement) {
      if (targetElement.classList.toString().startsWith('pika')){
        return true;
      }
      targetElement = targetElement.parentElement;
    }

    return false;
  }

  @HostListener('document:click', ['$event', '$event.target'])
  onClick(event: MouseEvent, targetElement: HTMLElement): void {
    if (!targetElement) {
      return;
    }

    if (this.isPikaSelectElement(targetElement)) {
      return;
    }

    const clickedInside = this.datePicker.nativeElement.contains(targetElement);
    if (!clickedInside) {
      this.hideDatePicker();
    }
  }

}
