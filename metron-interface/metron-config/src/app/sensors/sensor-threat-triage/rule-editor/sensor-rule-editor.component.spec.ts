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

import {async, TestBed, ComponentFixture} from '@angular/core/testing';
import {SensorRuleEditorComponent} from './sensor-rule-editor.component';
import {SharedModule} from '../../../shared/shared.module';
import {NumberSpinnerComponent} from '../../../shared/number-spinner/number-spinner.component';

describe('Component: SensorRuleEditorComponent', () => {

    let fixture: ComponentFixture<SensorRuleEditorComponent>;
    let component: SensorRuleEditorComponent;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            imports: [SharedModule
            ],
            declarations: [ SensorRuleEditorComponent, NumberSpinnerComponent ],
            providers: [
              SensorRuleEditorComponent
            ]
        });

        fixture = TestBed.createComponent(SensorRuleEditorComponent);
        component = fixture.componentInstance;
    }));

    it('should create an instance', () => {
        expect(component).toBeDefined();
    });

    it('should edit rules', async(() => {
        component.value =  'initial rule';
        component.score = 1;
        let numCancelled = 0;
        let savedRule = {};
        component.onCancelTextEditor.subscribe((cancelled: boolean) => {
          numCancelled++;
        });
        component.onSubmitTextEditor.subscribe((rule: {string: number}) => {
          savedRule = rule;
        });

        component.onSave();
        expect(savedRule).toEqual({'initial rule': 1});

        component.value =  'new rule';
        component.score = 2;
        component.onSave();
        expect(savedRule).toEqual({'new rule': 2});

        expect(numCancelled).toEqual(0);
        component.onCancel();
        expect(numCancelled).toEqual(1);
    }));
});
