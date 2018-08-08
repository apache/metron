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
import { Component, OnInit, ViewChild, ElementRef, OnChanges, SimpleChanges, Input, Output, EventEmitter } from '@angular/core';
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

  @Input() date = '';
  @Input() minDate = '';
  @Output() dateChange = new EventEmitter<string>();
  @ViewChild('inputText') inputText: ElementRef;

  constructor(private elementRef: ElementRef) {}

  ngOnInit() {
    let _datePickerComponent = this;
    let pikadayConfig = {
      field: this.elementRef.nativeElement,
      showSeconds: true,
      use24hour: true,
      onSelect: function() {
        _datePickerComponent.dateStr = this.getMoment().format('YYYY-MM-DD HH:mm:ss');
        setTimeout(() => {
          _datePickerComponent.dateChange.emit(_datePickerComponent.dateStr);
          if (_datePickerComponent.onChange) {
            _datePickerComponent.onChange(_datePickerComponent.dateStr);
          }
        }, 0);
      }
    };
    this.picker = new Pikaday(pikadayConfig);
    this.setDate();
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

  toggleDatePicker($event) {
    if (this.picker) {
      if (this.picker.isVisible()) {
        this.picker.hide();
      } else {
        this.picker.show();
      }

      $event.stopPropagation();
    }
  }
}
