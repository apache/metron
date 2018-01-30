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
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {AdvancedConfigFormComponent} from './advanced-config-form.component';

describe('Component: AdvancedConfigFormComponent', () => {

  let comp: AdvancedConfigFormComponent;
  let fixture: ComponentFixture<AdvancedConfigFormComponent>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      imports: [FormsModule, ReactiveFormsModule],
      declarations: [AdvancedConfigFormComponent]
    }).compileComponents()
      .then(() => {
        fixture = TestBed.createComponent(AdvancedConfigFormComponent);
        comp = fixture.componentInstance;
      });

  }));

  it('should create new forms for AdvancedConfigFormComponent',  async(() => {
      let component: AdvancedConfigFormComponent =  fixture.componentInstance;
      component.config = {'field1': 'value1', 'field2': 'value2'};
      component.ngOnInit();

      expect(Object.keys(component.configForm.controls).length).toEqual(4);
      expect(component.configForm.controls['newConfigKey'].value).toEqual('enter field');
      expect(component.configForm.controls['newConfigValue'].value).toEqual('enter value');
      expect(component.configForm.controls['field1'].value).toEqual('value1');
      expect(component.configForm.controls['field2'].value).toEqual('value2');
  }));

  it('OnChanges should recreate the form',  async(() => {
    let component: AdvancedConfigFormComponent =  fixture.componentInstance;
    component.config = {'field1': 'value1', 'field2': 'value2'};
    let changes = {'field1': {'currentValue' : 'value1', 'previousValue': 'value1'}};
    spyOn(component, 'createForm');

    component.ngOnChanges(changes);

    expect(component.createForm).not.toHaveBeenCalled();
    expect(component.configKeys).toEqual([]);

    changes = {'field1': {'currentValue' : 'value1', 'previousValue': 'value-1-1'}};
    component.ngOnChanges(changes);

    expect(component.createForm).toHaveBeenCalled();
    expect(component.configKeys).toEqual(['field1', 'field2']);

  }));

  it('verify form interactions AdvancedConfigFormComponent',  async(() => {
      let component: AdvancedConfigFormComponent =  fixture.componentInstance;
      component.config = {'field1': 'value1', 'field2': 'value2'};
      component.ngOnInit();


      expect(component.newConfigKey).toEqual('enter field');
      expect(component.newConfigValue).toEqual('enter value');
      expect(component.configKeys).toEqual(['field1', 'field2']);

      component.clearKeyPlaceholder();
      expect(component.newConfigKey).toEqual('');
      component.clearValuePlaceholder();
      expect(component.newConfigValue).toEqual('');

      component.newConfigKey = '';
      component.newConfigValue = '';
      component.saveNewConfig();
      expect(Object.keys(component.config).length).toEqual(2);
      expect(component.config['field1']).toEqual('value1');
      expect(component.config['field2']).toEqual('value2');
      expect(component.newConfigKey).toEqual('enter field');
      expect(component.newConfigValue).toEqual('enter value');
      component.addConfig();
      expect(component.configKeys).toEqual(['field1', 'field2']);
      expect(Object.keys(component.configForm.controls).length).toEqual(4);
      expect(component.configForm.controls['newConfigKey'].value).toEqual('enter field');
      expect(component.configForm.controls['newConfigValue'].value).toEqual('enter value');
      expect(component.configForm.controls['field1'].value).toEqual('value1');
      expect(component.configForm.controls['field2'].value).toEqual('value2');


      component.newConfigKey = 'field3';
      component.newConfigValue = 'value3';
      component.saveNewConfig();
      expect(Object.keys(component.config).length).toEqual(3);
      expect(component.config['field1']).toEqual('value1');
      expect(component.config['field2']).toEqual('value2');
      expect(component.config['field3']).toEqual('value3');
      expect(component.newConfigKey).toEqual('field3');
      expect(component.newConfigValue).toEqual('value3');
      component.addConfig();
      expect(component.configKeys).toEqual(['field1', 'field2', 'field3']);
      expect(Object.keys(component.configForm.controls).length).toEqual(5);
      expect(component.configForm.controls['newConfigKey'].value).toEqual('enter field');
      expect(component.configForm.controls['newConfigValue'].value).toEqual('enter value');
      expect(component.configForm.controls['field1'].value).toEqual('value1');
      expect(component.configForm.controls['field2'].value).toEqual('value2');
      expect(component.configForm.controls['field3'].value).toEqual('value3');

      component.newConfigKey = 'field1';
      component.newConfigValue = 'newValue1';
      component.saveNewConfig();
      expect(Object.keys(component.config).length).toEqual(3);
      expect(component.config['field1']).toEqual('newValue1');
      expect(component.config['field2']).toEqual('value2');
      expect(component.config['field3']).toEqual('value3');
      expect(component.newConfigKey).toEqual('enter field');
      expect(component.newConfigValue).toEqual('enter value');
      component.addConfig();
      expect(component.configKeys).toEqual(['field1', 'field2', 'field3']);
      expect(Object.keys(component.configForm.controls).length).toEqual(5);
      expect(component.configForm.controls['newConfigKey'].value).toEqual('enter field');
      expect(component.configForm.controls['newConfigValue'].value).toEqual('enter value');
      expect(component.configForm.controls['field1'].value).toEqual('value1');
      expect(component.configForm.controls['field2'].value).toEqual('value2');
      expect(component.configForm.controls['field3'].value).toEqual('value3');

      component.newConfigKey = 'field1';
      component.newConfigValue = '["newValue1"]';
      component.saveNewConfig();
      expect(Object.keys(component.config).length).toEqual(3);
      expect(component.config['field1']).toEqual(['newValue1']);

      component.newConfigKey = 'field1';
      component.newConfigValue = '{"key":"newValue1"}';
      component.saveNewConfig();
      expect(Object.keys(component.config).length).toEqual(3);
      expect(component.config['field1']).toEqual({key: 'newValue1'});

      component.removeConfig('field1');
      expect(Object.keys(component.config).length).toEqual(2);
      expect(component.config['field2']).toEqual('value2');
      expect(component.config['field3']).toEqual('value3');
      expect(component.configKeys).toEqual(['field2', 'field3']);
      expect(Object.keys(component.configForm.controls).length).toEqual(4);
      expect(component.configForm.controls['newConfigKey'].value).toEqual('enter field');
      expect(component.configForm.controls['newConfigValue'].value).toEqual('enter value');
      expect(component.configForm.controls['field2'].value).toEqual('value2');
      expect(component.configForm.controls['field3'].value).toEqual('value3');


  }));

    it('verify display and save values',  async(() => {
        let component: AdvancedConfigFormComponent =  fixture.componentInstance;
        component.config = {'field1': 'value1', 'field2': 'value2'};
        component.ngOnInit();

        expect(component.displayValue('field1')).toEqual('value1');

        component.saveValue('field1', '["value1","value2"]');
        expect(component.config['field1']).toEqual(['value1', 'value2']);
        expect(component.displayValue('field1')).toEqual('["value1","value2"]');

        component.saveValue('field1', '["value1","value2"');
        expect(component.config['field1']).toEqual('["value1","value2"');
        expect(component.displayValue('field1')).toEqual('["value1","value2"');

        component.saveValue('field1', '{"key1":"value1"}');
        expect(component.config['field1']).toEqual({'key1': 'value1'});
        expect(component.displayValue('field1')).toEqual('{"key1":"value1"}');
    }));

});
