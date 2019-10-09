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

import {async, TestBed, ComponentFixture, inject} from '@angular/core/testing';
import {SensorRuleEditorComponent} from './sensor-rule-editor.component';
import {SharedModule} from '../../../shared/shared.module';
import {NumberSpinnerComponent} from '../../../shared/number-spinner/number-spinner.component';
import {RiskLevelRule} from '../../../model/risk-level-rule';
import {AceEditorModule} from '../../../shared/ace-editor/ace-editor.module';
import {StellarService} from '../../../service/stellar.service';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AppConfigService } from 'app/service/app-config.service';
import { By } from '@angular/platform-browser';
import { Observable } from 'rxjs';

describe('Component: SensorRuleEditorComponent', () => {

    let fixture: ComponentFixture<SensorRuleEditorComponent>;
    let component: SensorRuleEditorComponent;
    let stellarService: StellarService;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            imports: [SharedModule, AceEditorModule, HttpClientTestingModule],
            declarations: [ SensorRuleEditorComponent, NumberSpinnerComponent ],
            providers: [
              SensorRuleEditorComponent,
              StellarService,
              {
                provide: AppConfigService,
                useValue: {
                  appConfigStatic: {},
                  getApiRoot: () => '/api/v1'
                }
            }]
        });

        fixture = TestBed.createComponent(SensorRuleEditorComponent);
        component = fixture.componentInstance;
        stellarService = TestBed.get(StellarService);
        fixture.detectChanges();
    }));

    it('should create an instance', () => {
        expect(component).toBeDefined();
    });

    it('should edit rules', async(() => {
        let numCancelled = 0;
        let savedRule = new RiskLevelRule();

        stellarService.validateRules = (expressions: string[]) => {
          return new Observable((observer) => {
            const response = {
              [expressions[0]]: true,
              [expressions[1]]: true,
            };
            observer.next(response);
          });
        }

        component.onCancelTextEditor.subscribe((cancelled: boolean) => {
          numCancelled++;
        });
        component.onSubmitTextEditor.subscribe((rule: RiskLevelRule) => {
          savedRule = rule;
        });

        component.riskLevelRule =  {name: 'rule1', rule: 'initial rule', score: '1', comment: ''};
        component.ngOnInit();
        component.onSave();
        fixture.detectChanges();
        let rule1 = Object.assign(new RiskLevelRule(), {name: 'rule1', rule: 'initial rule', score: '1', comment: ''});
        expect(savedRule).toEqual(rule1);

        component.riskLevelRule = {name: 'rule2', rule: 'new rule', score: '2', comment: ''};
        component.ngOnInit();
        component.onSave();
        fixture.detectChanges();
        let rule2 = Object.assign(new RiskLevelRule(), {name: 'rule2', rule: 'new rule', score: '2', comment: ''});
        expect(savedRule).toEqual(rule2);

        expect(numCancelled).toEqual(0);
        component.onCancel();
        fixture.detectChanges();
        expect(numCancelled).toEqual(1);
    }));

    it('should warn if either the rule or the score is invalid', inject(
      [HttpTestingController],
      (httpMock: HttpTestingController) => {
        const saveButton = fixture.debugElement.query(By.css('[data-qe-id="save-score"]'));

        component.newRiskLevelRule.rule = 'value > 10';
        component.newRiskLevelRule.score = 'value &&&&';
        fixture.detectChanges();
        saveButton.nativeElement.click();
        let validateRequest = httpMock.expectOne('/api/v1/stellar/validate/rules');
        validateRequest.flush({
          [component.newRiskLevelRule.rule]: true,
          [component.newRiskLevelRule.score]: false
        });
        fixture.detectChanges();
        let warning = fixture.debugElement.queryAll(By.css('.warning-text'));
        expect(warning.length).toBe(1);

        component.newRiskLevelRule.rule = 'value > 10';
        component.newRiskLevelRule.score = 'value * 10';
        fixture.detectChanges();
        saveButton.nativeElement.click();
        validateRequest = httpMock.expectOne('/api/v1/stellar/validate/rules');
        validateRequest.flush({
          [component.newRiskLevelRule.score]: true,
          [component.newRiskLevelRule.rule]: true
        });
        fixture.detectChanges();
        warning = fixture.debugElement.queryAll(By.css('.warning-text'));
        expect(warning.length).toBe(0);

        component.newRiskLevelRule.rule = 'value &&&&';
        component.newRiskLevelRule.score = 'value &&& &&&&';
        fixture.detectChanges();
        saveButton.nativeElement.click();
        validateRequest = httpMock.expectOne('/api/v1/stellar/validate/rules');
        validateRequest.flush({
          [component.newRiskLevelRule.score]: false,
          [component.newRiskLevelRule.rule]: false
        });
        fixture.detectChanges();
        warning = fixture.debugElement.queryAll(By.css('.warning-text'));
        expect(warning.length).toBe(2);
      }
    ));
});
