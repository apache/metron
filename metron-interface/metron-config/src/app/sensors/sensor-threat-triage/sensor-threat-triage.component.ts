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
/* tslint:disable:triple-equals */
import {Component, Input, EventEmitter, Output, OnChanges, SimpleChanges} from '@angular/core';
import {SensorEnrichmentConfig } from '../../model/sensor-enrichment-config';
import {RiskLevelRule} from '../../model/risk-level-rule';
import {SensorEnrichmentConfigService} from '../../service/sensor-enrichment-config.service';

@Component({
  selector: 'metron-config-sensor-threat-triage',
  templateUrl: './sensor-threat-triage.component.html',
  styleUrls: ['./sensor-threat-triage.component.scss']
})

export class SensorThreatTriageComponent implements OnChanges {

  @Input() showThreatTriage: boolean;
  @Input() sensorEnrichmentConfig: SensorEnrichmentConfig;
  @Output() hideThreatTriage: EventEmitter<boolean> = new EventEmitter<boolean>();

  availableAggregators = [];
  visibleRules: RiskLevelRule[] = [];
  showTextEditor = false;
  currentRiskLevelRule: RiskLevelRule;
  lowAlerts = 0;
  mediumAlerts = 0;
  highAlerts = 0;

  constructor(private sensorEnrichmentConfigService: SensorEnrichmentConfigService) { }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['showThreatTriage'] && changes['showThreatTriage'].currentValue) {
      this.init();
    }
  }

  init(): void {
    this.visibleRules = this.sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules;
    this.sensorEnrichmentConfigService.getAvailableThreatTriageAggregators().subscribe(results => {
      this.availableAggregators = results;
    });
  }

  onClose(): void {
    this.hideThreatTriage.emit(true);
  }


  onSubmitTextEditor(riskLevelRule: RiskLevelRule): void {
    this.deleteRule(this.currentRiskLevelRule);
    this.sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules.push(riskLevelRule);
    this.showTextEditor = false;
    this.init();
  }

  onCancelTextEditor(): void {
    this.showTextEditor = false;
  }

  onEditRule(riskLevelRule: RiskLevelRule) {
    this.currentRiskLevelRule = riskLevelRule;
    this.showTextEditor = true;
  }

  onDeleteRule(riskLevelRule: RiskLevelRule) {
    this.deleteRule(riskLevelRule);
    this.init();
  }

  onNewRule(): void {
    this.currentRiskLevelRule = new RiskLevelRule();
    this.showTextEditor = true;
  }

  deleteRule(riskLevelRule: RiskLevelRule) {
    let index = this.sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules.indexOf(riskLevelRule);
    if (index != -1) {
      this.sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules.splice(index, 1);
    }
  }

  getDisplayName(riskLevelRule: RiskLevelRule): string {
    if (riskLevelRule.name) {
      return riskLevelRule.name;
    } else {
      return riskLevelRule.rule ? riskLevelRule.rule : '';
    }
  }

}
