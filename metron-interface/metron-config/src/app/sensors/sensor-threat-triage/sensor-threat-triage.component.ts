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
import {Component, OnInit, Input, EventEmitter, Output, OnChanges, SimpleChanges, ViewChild} from '@angular/core';
import {SensorEnrichmentConfig, EnrichmentConfig, ThreatIntelConfig} from '../../model/sensor-enrichment-config';

export enum SortOrderOption {
  LOWEST_SCORE, HIGHEST_SCORE, LOWEST_NAME, HIGHEST_NAME
}

export enum ThreatTriageFilter {
  NONE, LOW, MEDIUM, HIGH
}

@Component({
  selector: 'metron-config-sensor-threat-triage',
  templateUrl: './sensor-threat-triage.component.html',
  styleUrls: ['./sensor-threat-triage.component.scss']
})

export class SensorThreatTriageComponent implements OnInit, OnChanges {

  @Input() showThreatTriage: boolean;
  @Input() sensorEnrichmentConfig: SensorEnrichmentConfig;

  @Output() hideThreatTriage: EventEmitter<boolean> = new EventEmitter<boolean>();
  
  availableAggregators = ['MAX', 'SUM'];

  showTextEditor = false;
  currentValue: string;
  textEditorValue: string;
  textEditorScore: number;

  rules = [];

  lowAlerts = 0;
  mediumAlerts = 0;
  highAlerts = 0;

  sortOrderOption = SortOrderOption;
  sortOrder = SortOrderOption.HIGHEST_SCORE;
  threatTriageFilter = ThreatTriageFilter;
  filter: ThreatTriageFilter = ThreatTriageFilter.NONE;

  constructor() { }

  ngOnInit() {

  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['showThreatTriage'] && changes['showThreatTriage'].currentValue) {
      this.init();
    }
  }

  init(): void {
    this.rules = Object.keys(this.sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules);
    this.updateBuckets();
    this.onSortOrderChange();
  }

  onClose(): void {
    this.hideThreatTriage.emit(true);
  }


  onSubmitTextEditor(rule: {}): void {
    let ruleValue = Object.keys(rule)[0];
    delete this.sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules[this.textEditorValue];
    this.sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules[ruleValue] = rule[ruleValue];
    this.showTextEditor = false;
    this.init();
  }
  
  onCancelTextEditor(): void {
    this.showTextEditor = false;
  }
  
  onEditRule(rule: string) {
    this.textEditorValue = rule;
    this.textEditorScore = this.sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules[rule];
    this.showTextEditor = true;
  }

  onDeleteRule(rule: string) {
    delete this.sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules[rule];
    this.init();
  }
  
  onNewRule(): void {
    this.textEditorValue = '';
    this.textEditorScore = 0;
    this.showTextEditor = true;
  }

  updateBuckets() {
    this.lowAlerts = 0;
    this.mediumAlerts = 0;
    this.highAlerts = 0;
    for(let rule of this.rules) {
      if (this.sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules[rule] <= 20) {
        this.lowAlerts++;
      } else if (this.sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules[rule] >= 80) {
        this.highAlerts++;
      } else {
        this.mediumAlerts++;
      }
    }
  }
  
  getRuleColor(rule: string): string {
    let color: string;
    if (this.sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules[rule] <= 20) {
      color = 'khaki';
    } else if (this.sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules[rule] >= 80) {
      color = 'red';
    } else {
      color = 'orange';
    }
    return color;
  }

  onSortOrderChange() {
    if (this.sortOrder == SortOrderOption.HIGHEST_SCORE) {
      this.rules.sort((a, b) => {
        let scoreA = this.sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules[a];
        let scoreB = this.sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules[b];
        return scoreB - scoreA;
      })
    } else if (this.sortOrder == SortOrderOption.LOWEST_SCORE) {
      this.rules.sort((a, b) => {
        let scoreA = this.sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules[a];
        let scoreB = this.sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules[b];
        return scoreA - scoreB;
      })
    } else if (this.sortOrder == SortOrderOption.LOWEST_NAME) {
      this.rules.sort((a, b) => {
        if (a.toLowerCase() > b.toLowerCase()) {
          return 1;
        } else if (a.toLowerCase() < b.toLowerCase()) {
          return -1;
        } else {
          return 0;
        }
      })
    } else {
      this.rules.sort((a, b) => {
        if (a.toLowerCase() > b.toLowerCase()) {
          return -1;
        } else if (a.toLowerCase() < b.toLowerCase()) {
          return 1;
        } else {
          return 0;
        }
      })
    }
  }

  onFilterChange(filter: ThreatTriageFilter) {
    if (filter == this.filter) {
      this.filter = ThreatTriageFilter.NONE;
    } else {
      this.filter = filter;
    }
    this.rules = Object.keys(this.sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules).filter(rule => {
      if (this.filter === ThreatTriageFilter.NONE) {
        return true;
      } else {
        let score = this.sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules[rule];
        if (this.filter === ThreatTriageFilter.HIGH) {
          return score >= 80;
        } else if (this.filter === ThreatTriageFilter.LOW) {
          return score <= 20;
        } else {
          return score < 80 && score > 20;
        }
      }
    });
    this.onSortOrderChange();
  }

}
