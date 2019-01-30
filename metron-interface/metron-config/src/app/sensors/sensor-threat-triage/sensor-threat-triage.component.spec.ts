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
import { SimpleChange, SimpleChanges } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { async, TestBed, ComponentFixture } from '@angular/core/testing';
import {
  SensorThreatTriageComponent,
  SortOrderOption,
  ThreatTriageFilter
} from './sensor-threat-triage.component';
import {
  SensorEnrichmentConfig,
  ThreatIntelConfig
} from '../../model/sensor-enrichment-config';
import { RiskLevelRule } from '../../model/risk-level-rule';
import { SensorEnrichmentConfigService } from '../../service/sensor-enrichment-config.service';
import { Observable } from 'rxjs';
import { SensorThreatTriageModule } from './sensor-threat-triage.module';

class MockSensorEnrichmentConfigService {
  public getAvailableThreatTriageAggregators(): Observable<string[]> {
    return Observable.create(observer => {
      observer.next(['MAX', 'MIN', 'SUM', 'MEAN', 'POSITIVE_MEAN']);
      observer.complete();
    });
  }
}

describe('Component: SensorThreatTriageComponent', () => {
  let component: SensorThreatTriageComponent;
  let fixture: ComponentFixture<SensorThreatTriageComponent>;
  let sensorEnrichmentConfigService: SensorEnrichmentConfigService;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [SensorThreatTriageModule],
      providers: [
        { provide: HttpClient },
        {
          provide: SensorEnrichmentConfigService,
          useClass: MockSensorEnrichmentConfigService
        }
      ]
    })
      .compileComponents()
      .then(() => {
        fixture = TestBed.createComponent(SensorThreatTriageComponent);
        component = fixture.componentInstance;
        sensorEnrichmentConfigService = fixture.debugElement.injector.get(
          SensorEnrichmentConfigService
        );
      });
  }));

  it('should create an instance', () => {
    expect(component).toBeDefined();
    fixture.destroy();
  });

  it('should create an instance', async(() => {
    spyOn(component, 'init');
    let changes: SimpleChanges = {
      showThreatTriage: new SimpleChange(false, true, true)
    };

    component.ngOnChanges(changes);
    expect(component.init).toHaveBeenCalled();

    changes = { showStellar: new SimpleChange(true, false, false) };
    component.ngOnChanges(changes);
    expect(component.init['calls'].count()).toEqual(1);

    fixture.destroy();
  }));

  it('should close panel', async(() => {
    let numClosed = 0;
    component.hideThreatTriage.subscribe((closed: boolean) => {
      numClosed++;
    });

    component.onClose();
    expect(numClosed).toEqual(1);

    fixture.destroy();
  }));

  it('should get color', async(() => {
    let sensorEnrichmentConfig = new SensorEnrichmentConfig();
    sensorEnrichmentConfig.threatIntel = Object.assign(
      new ThreatIntelConfig(),
      {
        triageConfig: {
          riskLevelRules: {
            ruleA: 15,
            ruleB: 95,
            ruleC: 50
          },
          aggregator: 'MAX',
          aggregationConfig: {}
        }
      }
    );
    component.sensorEnrichmentConfig = sensorEnrichmentConfig;

    let ruleA = { name: 'ruleA', rule: 'rule A', score: 15, comment: '' };
    let ruleB = { name: 'ruleB', rule: 'rule B', score: 95, comment: '' };
    let ruleC = { name: 'ruleC', rule: 'rule C', score: 50, comment: '' };

    expect(component.getRuleColor(ruleA)).toEqual('khaki');
    expect(component.getRuleColor(ruleB)).toEqual('red');
    expect(component.getRuleColor(ruleC)).toEqual('orange');

    fixture.destroy();
  }));

  it('should edit rules', async(() => {
    let ruleA = { name: 'ruleA', rule: 'rule A', score: 15, comment: '' };
    let ruleB = { name: 'ruleB', rule: 'rule B', score: 95, comment: '' };
    let ruleC = { name: 'ruleC', rule: 'rule C', score: 50, comment: '' };
    let ruleD = { name: 'ruleD', rule: 'rule D', score: 85, comment: '' };
    let ruleE = { name: 'ruleE', rule: 'rule E', score: 5, comment: '' };
    let ruleF = { name: 'ruleF', rule: 'rule F', score: 21, comment: '' };
    let ruleG = { name: 'ruleG', rule: 'rule G', score: 100, comment: '' };

    let sensorEnrichmentConfig = new SensorEnrichmentConfig();
    sensorEnrichmentConfig.threatIntel = Object.assign(
      new ThreatIntelConfig(),
      {
        triageConfig: {
          riskLevelRules: [ruleA, ruleB, ruleC, ruleD, ruleE],
          aggregator: 'MAX',
          aggregationConfig: {}
        }
      }
    );
    component.sensorEnrichmentConfig = sensorEnrichmentConfig;

    let changes: SimpleChanges = {
      showThreatTriage: new SimpleChange(false, true, true)
    };
    component.ngOnChanges(changes);

    // sorted by score high to low
    expect(component.visibleRules).toEqual([ruleB, ruleD, ruleC, ruleA, ruleE]);
    expect(component.lowAlerts).toEqual(2);
    expect(component.mediumAlerts).toEqual(1);
    expect(component.highAlerts).toEqual(2);

    // sorted by name high to low
    component.onSortOrderChange(SortOrderOption.Highest_Name);
    expect(component.visibleRules).toEqual([ruleE, ruleD, ruleC, ruleB, ruleA]);

    // sorted by score low to high
    component.onSortOrderChange(SortOrderOption.Lowest_Score);
    expect(component.visibleRules).toEqual([ruleE, ruleA, ruleC, ruleD, ruleB]);

    // sorted by name low to high
    component.onSortOrderChange(SortOrderOption.Lowest_Name);
    expect(component.visibleRules).toEqual([ruleA, ruleB, ruleC, ruleD, ruleE]);

    component.onNewRule();
    expect(component.currentRiskLevelRule.name).toEqual('');
    expect(component.currentRiskLevelRule.rule).toEqual('');
    expect(component.currentRiskLevelRule.score).toEqual(0);
    expect(component.showTextEditor).toEqual(true);

    component.currentRiskLevelRule = new RiskLevelRule();
    component.onCancelTextEditor();
    expect(component.showTextEditor).toEqual(false);
    expect(component.visibleRules).toEqual([ruleA, ruleB, ruleC, ruleD, ruleE]);

    component.sortOrder = SortOrderOption.Lowest_Score;
    component.onNewRule();
    component.currentRiskLevelRule = ruleF;
    expect(component.showTextEditor).toEqual(true);
    component.onSubmitTextEditor(ruleF);
    expect(component.visibleRules).toEqual([
      ruleE,
      ruleA,
      ruleF,
      ruleC,
      ruleD,
      ruleB
    ]);
    expect(component.lowAlerts).toEqual(2);
    expect(component.mediumAlerts).toEqual(2);
    expect(component.highAlerts).toEqual(2);
    expect(component.showTextEditor).toEqual(false);

    component.onDeleteRule(ruleE);
    expect(component.visibleRules).toEqual([ruleA, ruleF, ruleC, ruleD, ruleB]);
    expect(component.lowAlerts).toEqual(1);
    expect(component.mediumAlerts).toEqual(2);
    expect(component.highAlerts).toEqual(2);

    component.onFilterChange(ThreatTriageFilter.LOW);
    expect(component.visibleRules).toEqual([ruleA]);

    component.onFilterChange(ThreatTriageFilter.MEDIUM);
    expect(component.visibleRules).toEqual([ruleF, ruleC]);

    component.onFilterChange(ThreatTriageFilter.HIGH);
    expect(component.visibleRules).toEqual([ruleD, ruleB]);

    component.onFilterChange(ThreatTriageFilter.HIGH);
    expect(component.visibleRules).toEqual([ruleA, ruleF, ruleC, ruleD, ruleB]);

    component.onEditRule(ruleC);
    expect(component.currentRiskLevelRule).toEqual(ruleC);
    expect(component.showTextEditor).toEqual(true);
    component.onSubmitTextEditor(ruleG);
    expect(component.visibleRules).toEqual([ruleA, ruleF, ruleD, ruleB, ruleG]);
    expect(component.lowAlerts).toEqual(1);
    expect(component.mediumAlerts).toEqual(1);
    expect(component.highAlerts).toEqual(3);
    expect(component.showTextEditor).toEqual(false);

    fixture.destroy();
  }));
});
