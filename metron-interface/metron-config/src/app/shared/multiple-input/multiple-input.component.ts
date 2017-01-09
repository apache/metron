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
import { Component, Input, Output, EventEmitter } from '@angular/core';
import {AutocompleteOption} from '../../model/autocomplete-option';

@Component({
  selector: 'metron-config-multiple-input',
  templateUrl: './multiple-input.component.html',
  styleUrls: ['./multiple-input.component.scss']
})
export class MultipleInputComponent {

  @Input() type: string = 'text';
  @Input() allowDuplicates: boolean = true;
  @Input() configuredItems: AutocompleteOption[] = [];
  @Input() availableItems: AutocompleteOption[] = [];

  @Output() onConfigChange: EventEmitter<void> = new EventEmitter<void>();

  showAddNew: boolean = false;

  constructor() { }

  getAvailableFunctions(): AutocompleteOption[] {
    if (this.allowDuplicates) {
      return this.availableItems;
    }

    let tAvailable: AutocompleteOption[] = [];
    let configuredFunctionNames: string[] = [];
    for (let item of this.configuredItems) {
      configuredFunctionNames.push(item.name);
    }
    for (let item of this.availableItems) {
      if (configuredFunctionNames.indexOf(item.name) === -1) {
        tAvailable.push(item);
      }
    }

    return tAvailable;
  }

  getAvailableItemByName(name: string): AutocompleteOption {
    for (let item of this.availableItems) {
      if (name === item.name) {
        return item;
      }
    }

    return new AutocompleteOption();
  }

  onAdd(select: any) {
    this.configuredItems.push(this.getAvailableItemByName(select.value));
    select.value = '';
    this.onConfigChange.emit();
  }

  onRemove(configuredItem: AutocompleteOption) {
    this.configuredItems.splice(this.configuredItems.indexOf(configuredItem), 1);
    this.onConfigChange.emit();
  }

  onUpdate(configuredItem: AutocompleteOption, value: string) {
    this.configuredItems.splice(this.configuredItems.indexOf(configuredItem), 1, this.getAvailableItemByName(value));
    this.onConfigChange.emit();
  }
}
