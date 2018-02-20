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
import {Component, OnInit, OnChanges, Input} from '@angular/core';
import {FormGroup, Validators, FormControl} from '@angular/forms';

@Component({
  selector: 'metron-config-advanced-form',
  templateUrl: 'advanced-config-form.component.html',
  styleUrls: ['advanced-config-form.component.scss']
})
export class AdvancedConfigFormComponent implements OnInit, OnChanges {

  @Input() config: {};
  configKeys: string[] = [];
  newConfigKey: string = 'enter field';
  newConfigValue: string = 'enter value';
  configForm: FormGroup;

  constructor() {

  }

  ngOnInit() {
    this.configKeys = Object.keys(this.config);
    this.configForm = this.createForm();
  }

  ngOnChanges(changes: {[propertyName: string]: any}) {
    for (let propName of Object.keys(changes)) {
      let chng = changes[propName];
      let cur = JSON.stringify(chng.currentValue);
      let prev = JSON.stringify(chng.previousValue);
      if (cur !== prev) {
        this.configKeys = Object.keys(this.config);
        this.configForm = this.createForm();
      }
    }
  }

  createForm(): FormGroup {
    let group: any = {};
    for (let key of this.configKeys) {
      group[key] = new FormControl(this.config[key], Validators.required);
    }
    group['newConfigKey'] = new FormControl(this.newConfigKey, Validators.required);
    group['newConfigValue'] = new FormControl(this.newConfigValue, Validators.required);
    return new FormGroup(group);
  }

  clearKeyPlaceholder() {
    if (this.newConfigKey === 'enter field') {
      this.newConfigKey = '';
    }
  }

  clearValuePlaceholder() {
    if (this.newConfigValue === 'enter value') {
      this.newConfigValue = '';
    }
  }

  saveNewConfig() {
    if (this.newConfigKey === '') {
      this.newConfigKey = 'enter field';
    }
    if (this.newConfigValue === '') {
      this.newConfigValue = 'enter value';
    }
    if (this.newConfigKey !== 'enter field' && this.newConfigValue !== 'enter value') {
      let keyExists = this.config[this.newConfigKey] !== undefined;
      this.saveValue(this.newConfigKey, this.newConfigValue);
      if (keyExists) {
        this.newConfigKey = 'enter field';
        this.newConfigValue = 'enter value';
      }

    }
  }

  addConfig() {
    if (this.newConfigKey !== 'enter field' && this.newConfigValue !== 'enter value') {
      this.configKeys.push(this.newConfigKey);
      this.configForm.addControl(this.newConfigKey, new FormControl(this.newConfigValue, Validators.required));
      this.newConfigKey = 'enter field';
      this.newConfigValue = 'enter value';
    }
  }

  removeConfig(key: string) {
    delete this.config[key];
    this.configKeys = Object.keys(this.config);
    this.configForm.removeControl(key);
  }

  displayValue(key: string): string {
    let value = this.config[key];
    if (Array.isArray(value) || value instanceof Object) {
      return JSON.stringify(value);
    } else {
      return value;
    }
  }

  saveValue(key: string, value: string) {
    try {
        this.config[key] = JSON.parse(value);
    } catch (err) {
        this.config[key] = value;
    }

  }

}
