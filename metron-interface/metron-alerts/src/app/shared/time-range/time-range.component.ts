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
import { Component, OnInit, ViewChild, ElementRef, HostListener, EventEmitter, Output, Input, OnChanges, SimpleChanges} from '@angular/core';
import * as moment from 'moment/moment';
import {Filter, RangeFilter} from '../../model/filter';
import {DEFAULT_TIMESTAMP_FORMAT, CUSTOMM_DATE_RANGE_LABEL} from '../../utils/constants';

@Component({
  selector: 'app-time-range',
  templateUrl: './time-range.component.html',
  styleUrls: ['./time-range.component.scss']
})
export class TimeRangeComponent implements OnInit, OnChanges {
  toDateStr = '';
  fromDateStr = '';
  datePickerFromDate = '';
  datePickerToDate = '';
  selectedTimeRangeValue = 'All time';

  @Input() disabled = false;
  @ViewChild('datePicker') datePicker: ElementRef;
  @Output() timeRangeChange = new EventEmitter<Filter>();

  timeRangeMappingCol1 = {
    'Last 7 days':            'last-7-days',
    'Last 30 days':           'last-30-days',
    'Last 60 days':           'last-60-days',
    'Last 90 days':           'last-90-days',
    'Last 6 months':          'last-6-months',
    'Last 1 year':            'last-1-year',
    'Last 2 years':           'last-2-years',
    'Last 5 years':           'last-5-years'
  };
  timeRangeMappingCol2 = {
    'Yesterday':              'yesterday',
    'Day before yesterday':   'day-before-yesterday',
    'This day last week':     'this-day-last-week',
    'Previous week':          'previous-week',
    'Previous month':         'previous-month',
    'Previous year':          'previous-year',
    'All time':               'all-time'
  };
  timeRangeMappingCol3 = {
    'Today':                  'today',
    'Today so far':           'today-so-far',
    'This week':              'this-week',
    'This week so far':       'this-week-so-far',
    'This month':             'this-month',
    'This year':              'this-year'
  };
  timeRangeMappingCol4 = {
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
    if (changes && !changes['disabled'].currentValue){
      this.setDate(this.getTimeRangeStr());
    }
  }

  ngOnInit() {
    this.setDate(this.getTimeRangeStr());
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
    this.setDate(range);
  }

  hideDatePicker() {
    this.datePicker.nativeElement.classList.remove('show');
  }

  setDate(range:string) {
    let toDate = '';
    let fromDate = '';
    
    switch (range) {
      case 'last-7-days':
        fromDate = moment().subtract(7, 'days').local().format();
        toDate = moment().local().format();
        break;
      case 'last-30-days':
        fromDate = moment().subtract(30, 'days').local().format();
        toDate = moment().local().format();
        break;
      case 'last-60-days':
        fromDate = moment().subtract(60, 'days').local().format();
        toDate = moment().local().format();
        break;
      case 'last-90-days':
        fromDate = moment().subtract(90, 'days').local().format();
        toDate = moment().local().format();
        break;
      case 'last-6-months':
        fromDate = moment().subtract(6, 'months').local().format();
        toDate = moment().local().format();
        break;
      case 'last-1-year':
        fromDate = moment().subtract(1, 'year').local().format();
        toDate = moment().local().format();
        break;
      case 'last-2-years':
        fromDate = moment().subtract(2, 'years').local().format();
        toDate = moment().local().format();
        break;
      case 'last-5-years':
        fromDate = moment().subtract(5, 'years').local().format();
        toDate = moment().local().format();
        break;
      case 'all-time':
        fromDate = '1970-01-01T05:30:00+05:30';
        toDate = moment().local().format();
        break;
      case 'yesterday':
        fromDate = moment().subtract(1, 'days').startOf('day').local().format();
        toDate = moment().subtract(1, 'days').endOf('day').local().format();
        break;
      case 'day-before-yesterday':
        fromDate = moment().subtract(2, 'days').startOf('day').local().format();
        toDate = moment().subtract(2, 'days').endOf('day').local().format();
        break;
      case 'this-day-last-week':
        fromDate = moment().subtract(7, 'days').startOf('day').local().format();
        toDate = moment().subtract(7, 'days').endOf('day').local().format();
        break;
      case 'previous-week':
        fromDate = moment().subtract(1, 'weeks').startOf('week').local().format();
        toDate = moment().subtract(1, 'weeks').endOf('week').local().format();
        break;
      case 'previous-month':
        fromDate = moment().subtract(1, 'months').startOf('month').local().format();
        toDate = moment().subtract(1, 'months').endOf('month').local().format();
        break;
      case 'previous-year':
        fromDate = moment().subtract(1, 'years').startOf('year').local().format();
        toDate = moment().subtract(1, 'years').endOf('year').local().format();
        break;
      case 'today':
        fromDate = moment().startOf('day').local().format();
        toDate = moment().endOf('day').local().format();
        break;
      case 'today-so-far':
        fromDate = moment().startOf('day').local().format();
        toDate = moment().local().format();
        break;
      case 'this-week':
        fromDate = moment().startOf('week').local().format();
        toDate = moment().endOf('week').local().format();
        break;
      case 'this-week-so-far':
        fromDate = moment().startOf('week').local().format();
        toDate = moment().local().format();
        break;
      case 'this-month':
        fromDate = moment().startOf('month').local().format();
        toDate = moment().endOf('month').local().format();
        break;
      case 'this-year':
        fromDate = moment().startOf('year').local().format();
        toDate = moment().endOf('year').local().format();
        break;
      case 'last-5-minutes':
        fromDate = moment().subtract(5, 'minutes').local().format();
        toDate = moment().local().format();
        break;
      case 'last-15-minutes':
        fromDate = moment().subtract(15, 'minutes').local().format();
        toDate = moment().local().format();
        break;
      case 'last-30-minutes':
        fromDate = moment().subtract(30, 'minutes').local().format();
        toDate = moment().local().format();
        break;
      case 'last-1-hour':
        fromDate = moment().subtract(60, 'minutes').local().format();
        toDate = moment().local().format();
        break;
      case 'last-3-hours':
        fromDate = moment().subtract(3, 'hours').local().format();
        toDate = moment().local().format();
        break;
      case 'last-6-hours':
        fromDate = moment().subtract(6, 'hours').local().format();
        toDate = moment().local().format();
        break;
      case 'last-12-hours':
        fromDate = moment().subtract(12, 'hours').local().format();
        toDate = moment().local().format();
        break;
      case 'last-24-hours':
        fromDate = moment().subtract(24, 'hours').local().format();
        toDate = moment().local().format();
        break;
    }

    this.applyRange(toDate, fromDate);
  }

  applyRange(toDate:string, fromDate:string) {
    this.toDateStr = moment(toDate).format(DEFAULT_TIMESTAMP_FORMAT);
    this.fromDateStr = moment(fromDate).format(DEFAULT_TIMESTAMP_FORMAT);
    this.timeRangeChange.emit(new RangeFilter('timestamp', new Date((fromDate)).getTime(), new Date((toDate)).getTime(), false));
  }

  applyCustomDate() {
    this.applyRange(this.datePickerToDate, this.datePickerFromDate);
    this.selectedTimeRangeValue = CUSTOMM_DATE_RANGE_LABEL;
    this.hideDatePicker();
  }

  isPikaSelectElement(targetElement: HTMLElement): boolean {
    while(targetElement) {
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

    if(this.isPikaSelectElement(targetElement)) {
      return;
    }

    const clickedInside = this.datePicker.nativeElement.contains(targetElement);
    if (!clickedInside) {
      this.hideDatePicker();
    }
  }

}
