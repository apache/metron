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

export enum SortOrderOption {
  Lowest_Score, Highest_Score, Lowest_Name, Highest_Name
}

export enum ThreatTriageFilter {
  NONE, LOW, MEDIUM, HIGH
}

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

  sortOrderOption = SortOrderOption;
  sortOrder = SortOrderOption.Highest_Score;
  threatTriageFilter = ThreatTriageFilter;
  filter: ThreatTriageFilter = ThreatTriageFilter.NONE;

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
    this.updateBuckets();
    this.onSortOrderChange(null);
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

  updateBuckets() {
    this.lowAlerts = 0;
    this.mediumAlerts = 0;
    this.highAlerts = 0;
    for (let riskLevelRule of this.visibleRules) {
      if (riskLevelRule.score <= 20) {
        this.lowAlerts++;
      } else if (riskLevelRule.score >= 80) {
        this.highAlerts++;
      } else {
        this.mediumAlerts++;
      }
    }
  }

  getRuleColor(riskLevelRule: RiskLevelRule): string {
    let color: string;
    if (riskLevelRule.score <= 20) {
      color = 'khaki';
    } else if (riskLevelRule.score >= 80) {
      color = 'red';
    } else {
      color = 'orange';
    }
    return color;
  }

  onSortOrderChange(sortOrder: any) {
    if (sortOrder !== null) {
      this.sortOrder = sortOrder;
    }

    // all comparisons with enums must be == and not ===
    if (this.sortOrder == this.sortOrderOption.Highest_Score) {
      this.visibleRules.sort((a, b) => {
        return b.score - a.score;
      });
    } else if (this.sortOrder == SortOrderOption.Lowest_Score) {
      this.visibleRules.sort((a, b) => {
        return a.score - b.score;
      });
    } else if (this.sortOrder == SortOrderOption.Lowest_Name) {
      this.visibleRules.sort((a, b) => {
        let aName = a.name ? a.name : '';
        let bName = b.name ? b.name : '';
        if (aName.toLowerCase() >= bName.toLowerCase()) {
          return 1;
        } else if (aName.toLowerCase() < bName.toLowerCase()) {
          return -1;
        }
      });
    } else {
      this.visibleRules.sort((a, b) => {
        let aName = a.name ? a.name : '';
        let bName = b.name ? b.name : '';
        if (aName.toLowerCase() >= bName.toLowerCase()) {
          return -1;
        } else if (aName.toLowerCase() < bName.toLowerCase()) {
          return 1;
        }
      });
    }
  }

  onFilterChange(filter: ThreatTriageFilter) {
    if (filter === this.filter) {
      this.filter = ThreatTriageFilter.NONE;
    } else {
      this.filter = filter;
    }
    this.visibleRules = this.sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules.filter(riskLevelRule => {
      if (this.filter === ThreatTriageFilter.NONE) {
        return true;
      } else {
        if (this.filter === ThreatTriageFilter.HIGH) {
          return riskLevelRule.score >= 80;
        } else if (this.filter === ThreatTriageFilter.LOW) {
          return riskLevelRule.score <= 20;
        } else {
          return riskLevelRule.score < 80 && riskLevelRule.score > 20;
        }
      }
    });
    this.onSortOrderChange(null);
  }

  getDisplayName(riskLevelRule: RiskLevelRule): string {
    if (riskLevelRule.name) {
      return riskLevelRule.name;
    } else {
      return riskLevelRule.rule ? riskLevelRule.rule : '';
    }
  }

}
