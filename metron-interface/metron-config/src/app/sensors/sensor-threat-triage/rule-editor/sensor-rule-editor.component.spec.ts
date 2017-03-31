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
import {RiskLevelRule} from '../../../model/risk-level-rule';

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
        let numCancelled = 0;
        let savedRule = new RiskLevelRule();
        component.onCancelTextEditor.subscribe((cancelled: boolean) => {
          numCancelled++;
        });
        component.onSubmitTextEditor.subscribe((rule: RiskLevelRule) => {
          savedRule = rule;
        });

        component.riskLevelRule =  {name: 'rule1', rule: 'initial rule', score: 1, comment: ''};
        component.ngOnInit();
        component.onSave();
        let rule1 = Object.assign(new RiskLevelRule(), {name: 'rule1', rule: 'initial rule', score: 1, comment: ''});
        expect(savedRule).toEqual(rule1);

        component.riskLevelRule = {name: 'rule2', rule: 'new rule', score: 2, comment: ''};
        component.ngOnInit();
        component.onSave();
        let rule2 = Object.assign(new RiskLevelRule(), {name: 'rule2', rule: 'new rule', score: 2, comment: ''});
        expect(savedRule).toEqual(rule2);

        expect(numCancelled).toEqual(0);
        component.onCancel();
        expect(numCancelled).toEqual(1);
    }));
});
