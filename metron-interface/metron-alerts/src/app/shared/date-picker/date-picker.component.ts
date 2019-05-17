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
import { Component, ViewChild, ElementRef, OnChanges, SimpleChanges, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import * as moment from 'moment/moment';
import * as Pikaday from 'pikaday-time';

@Component({
  selector: 'app-date-picker',
  templateUrl: './date-picker.component.html',
  providers : [{
    provide : NG_VALUE_ACCESSOR,
    useExisting: DatePickerComponent,
    multi: true,
  }],
  styleUrls: ['./date-picker.component.scss']
})
export class DatePickerComponent implements OnInit, OnChanges, ControlValueAccessor {
  defaultDateStr = 'now';
  picker: Pikaday;
  dateStr = this.defaultDateStr;

  private onChange: Function;
  private onTouched: Function;
  private isManualMode = false;

  @Input() date = '';
  @Input() minDate = '';
  @Output() dateChange = new EventEmitter<string>();
  @ViewChild('inputText') inputText: ElementRef;
  @ViewChild('calendarIcon') calendarIcon: ElementRef;

  constructor(private elementRef: ElementRef) {}

  ngOnInit() {
    let _datePickerComponent = this;
    let pikadayConfig = {
      trigger: this.calendarIcon.nativeElement,
      field: this.elementRef.nativeElement,
      showSeconds: true,
      use24hour: true,
      onSelect: function() {
        if (_datePickerComponent.isManualMode) {
          return;
        }
        _datePickerComponent.performChange.call(_datePickerComponent, this.getMoment().format('YYYY-MM-DD HH:mm:ss'));
      }
    };
    this.picker = new Pikaday(pikadayConfig);
    this.setDate();
  }

  performChange(date: string) {
    this.dateStr = date;
      setTimeout(() => {
        this.dateChange.emit(this.dateStr);
        if (this.onChange) {
          this.onChange(this.dateStr);
        }
      }, 0);
  }

  onKeyup(e) {
    if (e.code === 'Enter' && new Date(e.target.value).toString() !== 'Invalid Date') {
      this.performChange(e.target.value);
      this.picker.hide();
    }
  }

  onFocus() {
    this.isManualMode = true;
  }

  onBlur(e) {
    if (new Date(e.target.value).toString() !== 'Invalid Date') {
      this.performChange(e.target.value);
    }
    this.isManualMode = false;
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes && changes['minDate'] && this.picker) {
      this.setMinDate();
    }

    if (changes && changes['date'] && this.picker) {
      this.setDate();
    }
  }

  writeValue(value) {
    this.date = value;
    this.setDate();
  }

  registerOnChange(fn) {
    this.onChange = fn;
  }

  registerOnTouched(fn) {
    this.onTouched = fn;
  }

  setDate() {
    if (this.date === '') {
      this.dateStr = this.defaultDateStr;
    } else {
      this.dateStr = this.date;
      this.picker.setDate(this.dateStr);
    }
  }

  setMinDate() {
    let currentDate = new Date(this.dateStr).getTime();
    let currentMinDate = new Date(this.minDate).getTime();
    if (currentMinDate > currentDate) {
      this.dateStr = this.defaultDateStr;
    }
    this.picker.setMinDate(new Date(this.minDate));
    this.picker.setDate(moment(this.minDate).endOf('day').format('YYYY-MM-DD HH:mm:ss'));
  }
}
