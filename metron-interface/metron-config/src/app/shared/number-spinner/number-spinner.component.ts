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
import { Component, Input, forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
  selector: 'metron-config-number-spinner',
  templateUrl: './number-spinner.component.html',
  styleUrls: ['./number-spinner.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => NumberSpinnerComponent),
      multi: true
    }
  ]
})
export class NumberSpinnerComponent implements ControlValueAccessor {

  @Input() min: number;
  @Input() max: number;

  innerValue: number = 0;

  private onTouchedCallback ;
  private onChangeCallback ;

  writeValue(val: any) {
    this.innerValue = val;
  }

  registerOnChange(fn: any) {
    this.onChangeCallback = fn;
  }

  registerOnTouched(fn: any) {
    this.onTouchedCallback = fn;
  }

  get value(): any {
    if (typeof this.innerValue === 'string') {
      this.innerValue = parseInt(this.innerValue, 10);
    }
    return this.innerValue;
  };

  set value(v: any) {
    v = Number(v);
    if (!isNaN(v) && v !== this.innerValue) {
      this.innerValue = v;
      this.onChangeCallback(v);
    }
  }

}
