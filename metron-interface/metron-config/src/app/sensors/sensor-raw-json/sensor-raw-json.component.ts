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
import {Component, Input, EventEmitter, Output, OnChanges, SimpleChanges} from '@angular/core';
import {SensorParserConfig} from '../../model/sensor-parser-config';
import {SensorEnrichmentConfig, EnrichmentConfig, ThreatIntelConfig} from '../../model/sensor-enrichment-config';
import {IndexingConfigurations, SensorIndexingConfig} from '../../model/sensor-indexing-config';

declare var ace: any;

@Component({
  selector: 'metron-config-sensor-raw-json',
  templateUrl: './sensor-raw-json.component.html',
  styleUrls: ['./sensor-raw-json.component.scss']
})

export class SensorRawJsonComponent implements OnChanges {

  @Input() showRawJson: boolean;
  @Input() sensorParserConfig: SensorParserConfig;
  @Input() sensorEnrichmentConfig: SensorEnrichmentConfig;
  @Input() indexingConfigurations: IndexingConfigurations;

  @Output() hideRawJson: EventEmitter<boolean> = new EventEmitter<boolean>();
  @Output() onRawJsonChanged: EventEmitter<boolean> = new EventEmitter<boolean>();

  newSensorParserConfig: string;
  newSensorEnrichmentConfig: string;
  newIndexingConfigurations: string;

  ngOnChanges(changes: SimpleChanges) {
    if (changes['showRawJson'] && changes['showRawJson'].currentValue) {
      this.init();
    }
  }

  init(): void {
    if (this.sensorParserConfig) {
      this.newSensorParserConfig = JSON.stringify(this.sensorParserConfig, null, '\t');
    }

    if (this.sensorEnrichmentConfig) {
      this.newSensorEnrichmentConfig = JSON.stringify(this.sensorEnrichmentConfig, null, '\t');
    }

    if (this.indexingConfigurations) {
      this.newIndexingConfigurations = JSON.stringify(this.indexingConfigurations, null, '\t');
    }
  }

  onSave() {
    let newParsedSensorParserConfig = JSON.parse(this.newSensorParserConfig);
    Object.keys(newParsedSensorParserConfig).filter(key => newParsedSensorParserConfig[key])
      .forEach(key => this.sensorParserConfig[key] = newParsedSensorParserConfig[key]);

    let newParsedSensorEnrichmentConfig = JSON.parse(this.newSensorEnrichmentConfig);
    this.sensorEnrichmentConfig.enrichment = Object.assign(new EnrichmentConfig(), newParsedSensorEnrichmentConfig.enrichment);
    this.sensorEnrichmentConfig.threatIntel = Object.assign(new ThreatIntelConfig(), newParsedSensorEnrichmentConfig.threatIntel);
    if (newParsedSensorEnrichmentConfig.configuration != null) {
      this.sensorEnrichmentConfig.configuration = newParsedSensorEnrichmentConfig.configuration;
    }

    let newParsedIndexingConfigurations = JSON.parse(this.newIndexingConfigurations);
    this.indexingConfigurations.hdfs = Object.assign(new SensorIndexingConfig(), newParsedIndexingConfigurations.hdfs);
    this.indexingConfigurations.elasticsearch = Object.assign(new SensorIndexingConfig(), newParsedIndexingConfigurations.elasticsearch);
    this.indexingConfigurations.solr = Object.assign(new SensorIndexingConfig(), newParsedIndexingConfigurations.solr);
    this.hideRawJson.emit(true);
    this.onRawJsonChanged.emit(true);
  }

  onCancel(): void {
    this.hideRawJson.emit(true);
  }
}
