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
import {SimpleChange, SimpleChanges} from '@angular/core';
import {async, TestBed, ComponentFixture} from '@angular/core/testing';
import {SensorThreatTriageComponent, SortOrderOption, ThreatTriageFilter} from './sensor-threat-triage.component';
import {SharedModule} from '../../shared/shared.module';
import {SensorEnrichmentConfig, ThreatIntelConfig} from '../../model/sensor-enrichment-config';
import {SensorRuleEditorComponent} from './rule-editor/sensor-rule-editor.component';
import {NumberSpinnerComponent} from '../../shared/number-spinner/number-spinner.component';

describe('Component: SensorThreatTriageComponent', () => {

  let fixture: ComponentFixture<SensorThreatTriageComponent>;
  let component: SensorThreatTriageComponent;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [SharedModule
      ],
      declarations: [SensorThreatTriageComponent, SensorRuleEditorComponent, NumberSpinnerComponent],
      providers: [
        SensorThreatTriageComponent
      ]
    });

    fixture = TestBed.createComponent(SensorThreatTriageComponent);
    component = fixture.componentInstance;
  }));

  it('should create an instance', () => {
    expect(component).toBeDefined();
  });

  it('should create an instance', () => {
    spyOn(component, 'init');
    let changes: SimpleChanges = {'showThreatTriage': new SimpleChange(false, true)};

    component.ngOnChanges(changes);
    expect(component.init).toHaveBeenCalled();

    changes = {'showStellar': new SimpleChange(true, false)};
    component.ngOnChanges(changes);
    expect(component.init['calls'].count()).toEqual(1);

    fixture.destroy();
  });

  it('should close panel', () => {
    let numClosed = 0;
    component.hideThreatTriage.subscribe((closed: boolean) => {
      numClosed++;
    });

    component.onClose();
    expect(numClosed).toEqual(1);

    fixture.destroy();
  });

  it('should get color', () => {
    let sensorEnrichmentConfig = new SensorEnrichmentConfig();
    sensorEnrichmentConfig.threatIntel = Object.assign(new ThreatIntelConfig(), {
      'triageConfig': {
        'riskLevelRules': {
          'ruleA': 15,
          'ruleB': 95,
          'ruleC': 50
        },
        'aggregator': 'MAX',
        'aggregationConfig': {}
      }
    });
    component.sensorEnrichmentConfig = sensorEnrichmentConfig;

    expect(component.getRuleColor('ruleA')).toEqual('khaki');
    expect(component.getRuleColor('ruleB')).toEqual('red');
    expect(component.getRuleColor('ruleC')).toEqual('orange');

    fixture.destroy();
  });

  it('should edit rules', () => {
    let sensorEnrichmentConfig = new SensorEnrichmentConfig();
    sensorEnrichmentConfig.threatIntel = Object.assign(new ThreatIntelConfig(), {
      'triageConfig': {
        'riskLevelRules': {
          'ruleA': 15,
          'ruleB': 95,
          'ruleC': 50,
          'ruleD': 85,
          'ruleE': 5
        },
        'aggregator': 'MAX',
        'aggregationConfig': {}
      }
    });
    component.sensorEnrichmentConfig = sensorEnrichmentConfig;


    let changes: SimpleChanges = {'showThreatTriage': new SimpleChange(false, true)};
    component.ngOnChanges(changes);

    // sorted by score high to low
    expect(component.rules).toEqual(['ruleB', 'ruleD', 'ruleC', 'ruleA', 'ruleE']);
    expect(component.lowAlerts).toEqual(2);
    expect(component.mediumAlerts).toEqual(1);
    expect(component.highAlerts).toEqual(2);

    // sorted by name high to low
    component.sortOrder = SortOrderOption.HIGHEST_NAME;
    component.onSortOrderChange();
    expect(component.rules).toEqual(['ruleE', 'ruleD', 'ruleC', 'ruleB', 'ruleA']);

    // sorted by score low to high
    component.sortOrder = SortOrderOption.LOWEST_SCORE;
    component.onSortOrderChange();
    expect(component.rules).toEqual(['ruleE', 'ruleA', 'ruleC', 'ruleD', 'ruleB']);

    // sorted by name low to high
    component.sortOrder = SortOrderOption.LOWEST_NAME;
    component.onSortOrderChange();
    expect(component.rules).toEqual(['ruleA', 'ruleB', 'ruleC', 'ruleD', 'ruleE']);

    component.onNewRule();
    expect(component.textEditorValue).toEqual('');
    expect(component.textEditorScore).toEqual(0);
    expect(component.showTextEditor).toEqual(true);

    component.textEditorValue = 'ruleF';
    component.textEditorScore = 21;
    component.onCancelTextEditor();
    expect(component.showTextEditor).toEqual(false);
    expect(component.rules).toEqual(['ruleA', 'ruleB', 'ruleC', 'ruleD', 'ruleE']);

    component.sortOrder = SortOrderOption.LOWEST_SCORE;
    component.onNewRule();
    component.textEditorValue = 'ruleF';
    component.textEditorScore = 21;
    expect(component.showTextEditor).toEqual(true);
    component.onSubmitTextEditor({'ruleF': 21});
    expect(component.rules).toEqual(['ruleE', 'ruleA', 'ruleF', 'ruleC', 'ruleD', 'ruleB']);
    expect(component.lowAlerts).toEqual(2);
    expect(component.mediumAlerts).toEqual(2);
    expect(component.highAlerts).toEqual(2);
    expect(component.showTextEditor).toEqual(false);

    component.onDeleteRule('ruleE');
    expect(component.rules).toEqual(['ruleA', 'ruleF', 'ruleC', 'ruleD', 'ruleB']);
    expect(component.lowAlerts).toEqual(1);
    expect(component.mediumAlerts).toEqual(2);
    expect(component.highAlerts).toEqual(2);

    component.onFilterChange(ThreatTriageFilter.LOW);
    expect(component.rules).toEqual(['ruleA']);

    component.onFilterChange(ThreatTriageFilter.MEDIUM);
    expect(component.rules).toEqual(['ruleF', 'ruleC']);

    component.onFilterChange(ThreatTriageFilter.HIGH);
    expect(component.rules).toEqual(['ruleD', 'ruleB']);

    component.onFilterChange(ThreatTriageFilter.HIGH);
    expect(component.rules).toEqual(['ruleA', 'ruleF', 'ruleC', 'ruleD', 'ruleB']);

    component.onEditRule('ruleC');
    expect(component.textEditorValue).toEqual('ruleC');
    expect(component.textEditorScore).toEqual(50);
    expect(component.showTextEditor).toEqual(true);
    component.onSubmitTextEditor({'ruleG': 100});
    expect(component.rules).toEqual(['ruleA', 'ruleF', 'ruleD', 'ruleB', 'ruleG']);
    expect(component.lowAlerts).toEqual(1);
    expect(component.mediumAlerts).toEqual(1);
    expect(component.highAlerts).toEqual(3);
    expect(component.showTextEditor).toEqual(false);

    fixture.destroy();
  });


});
