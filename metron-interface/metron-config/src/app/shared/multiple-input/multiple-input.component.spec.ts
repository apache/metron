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
/* tslint:disable:no-unused-variable */

import {async, TestBed, ComponentFixture} from '@angular/core/testing';
import { MultipleInputComponent } from './multiple-input.component';
import {SharedModule} from '../shared.module';
import {AutocompleteOption} from '../../model/autocomplete-option';

describe('Component: MultipleInput', () => {

  let fixture: ComponentFixture<MultipleInputComponent>;
  let component: MultipleInputComponent;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [SharedModule],
      declarations: [ MultipleInputComponent ],
      providers: [
        MultipleInputComponent
      ]
    });

    fixture = TestBed.createComponent(MultipleInputComponent);
    component = fixture.componentInstance;

  }));

  it('should create an instance', () => {
    expect(component).toBeDefined();
  });

  it('should get AvailableFunctions', () => {
    let availableItems: AutocompleteOption[] = [];
    availableItems.push(new AutocompleteOption('option1'));
    availableItems.push(new AutocompleteOption('option2'));
    availableItems.push(new AutocompleteOption('option3'));
    availableItems.push(new AutocompleteOption('option4'));

    let configuredItems: AutocompleteOption[] = [];
    configuredItems.push(new AutocompleteOption('option1'));
    configuredItems.push(new AutocompleteOption('option3'));

    component.allowDuplicates = false;

    component.availableItems = availableItems;
    expect(component.getAvailableFunctions()).toEqual(availableItems);

    component.configuredItems = configuredItems;
    expect(component.getAvailableFunctions()).toEqual([new AutocompleteOption('option2'), new AutocompleteOption('option4')]);

    component.allowDuplicates = true;
    expect(component.getAvailableFunctions()).toEqual(availableItems);

    fixture.destroy();
  });

  it('should get item by name', () => {
    let availableItems: AutocompleteOption[] = [];
    availableItems.push(new AutocompleteOption('option1'));
    availableItems.push(new AutocompleteOption('option2'));
    availableItems.push(new AutocompleteOption('option3'));

    component.availableItems = availableItems;
    expect(component.getAvailableItemByName('option1')).toEqual(new AutocompleteOption('option1'));
    expect(component.getAvailableItemByName('option4')).toEqual(new AutocompleteOption());

    fixture.destroy();
  });

  it('should add', () => {
    spyOn(component.onConfigChange, 'emit');

    let availableItems: AutocompleteOption[] = [];
    availableItems.push(new AutocompleteOption('option1'));
    availableItems.push(new AutocompleteOption('option2'));
    availableItems.push(new AutocompleteOption('option3'));

    let select = {'value': 'option1'};
    component.availableItems = availableItems;
    component.onAdd(select);
    expect(component.configuredItems.length).toEqual(1);
    expect(component.configuredItems[0].name).toEqual('option1');
    expect(component.showAddNew).toEqual(false);
    expect(component.onConfigChange.emit).toHaveBeenCalled();
    expect(select.value).toEqual('');

    fixture.destroy();
  });

  it('should remove', () => {
    spyOn(component.onConfigChange, 'emit');

    let configuredItems: AutocompleteOption[] = [];
    configuredItems.push(new AutocompleteOption('option1'));
    configuredItems.push(new AutocompleteOption('option2'));
    configuredItems.push(new AutocompleteOption('option3'));

    component.configuredItems = configuredItems;
    component.onRemove(new AutocompleteOption('option1'));
    expect(component.configuredItems.length).toEqual(2);
    expect(component.onConfigChange.emit).toHaveBeenCalled();

    fixture.destroy();
  });

  it('should update', () => {
    spyOn(component.onConfigChange, 'emit');

    let availableItems: AutocompleteOption[] = [];
    availableItems.push(new AutocompleteOption('option1'));
    availableItems.push(new AutocompleteOption('option2'));
    availableItems.push(new AutocompleteOption('option3'));
    availableItems.push(new AutocompleteOption('option4'));

    let configuredItems: AutocompleteOption[] = [];
    let option1 = new AutocompleteOption('option1');
    configuredItems.push(option1);
    configuredItems.push(new AutocompleteOption('option3'));

    component.availableItems = availableItems;
    component.configuredItems = configuredItems;

    component.onUpdate(option1, 'option4');
    expect(component.configuredItems.length).toEqual(2);
    expect(component.configuredItems[0].name).toEqual('option4');
    expect(component.onConfigChange.emit).toHaveBeenCalled();

    fixture.destroy();
  });

});
