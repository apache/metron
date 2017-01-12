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
import {Component, OnInit, Input, EventEmitter, Output, OnChanges, SimpleChanges,
    AfterViewInit, ViewChild, ElementRef} from '@angular/core';
import {SensorParserConfig} from '../../model/sensor-parser-config';
import {SensorEnrichmentConfig, EnrichmentConfig, ThreatIntelConfig} from '../../model/sensor-enrichment-config';

declare var ace: any;

@Component({
  selector: 'metron-config-sensor-stellar',
  templateUrl: './sensor-stellar.component.html',
  styleUrls: ['./sensor-stellar.component.scss']
})

export class SensorStellarComponent implements OnInit, OnChanges {

  @Input() showStellar: boolean;
  @Input() sensorParserConfig: SensorParserConfig;
  @Input() sensorEnrichmentConfig: SensorEnrichmentConfig;

  @Output() hideStellar: EventEmitter<boolean> = new EventEmitter<boolean>();
  @Output() onStellarChanged: EventEmitter<boolean> = new EventEmitter<boolean>();

  newSensorParserConfig: string;
  newSensorEnrichmentConfig: string;

  constructor() { }

  ngOnInit() {
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['showStellar'] && changes['showStellar'].currentValue) {
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
  }

  onSave() {
    let newParsedSensorParserConfig = JSON.parse(this.newSensorParserConfig);
    this.sensorParserConfig.sensorTopic = newParsedSensorParserConfig.sensorTopic;
    this.sensorParserConfig.parserClassName = newParsedSensorParserConfig.parserClassName;
    if (newParsedSensorParserConfig.writerClassName != null) {
      this.sensorParserConfig.writerClassName = newParsedSensorParserConfig.writerClassName;
    }
    if (newParsedSensorParserConfig.errorWriterClassName != null) {
      this.sensorParserConfig.errorWriterClassName = newParsedSensorParserConfig.errorWriterClassName;
    }
    if (newParsedSensorParserConfig.filterClassName != null) {
      this.sensorParserConfig.filterClassName = newParsedSensorParserConfig.filterClassName;
    }
    if (newParsedSensorParserConfig.invalidWriterClassName != null) {
      this.sensorParserConfig.invalidWriterClassName = newParsedSensorParserConfig.invalidWriterClassName;
    }
    this.sensorParserConfig.parserConfig = newParsedSensorParserConfig.parserConfig;
    this.sensorParserConfig.fieldTransformations = newParsedSensorParserConfig.fieldTransformations;
    let newParsedSensorEnrichmentConfig = JSON.parse(this.newSensorEnrichmentConfig);
    this.sensorEnrichmentConfig.batchSize = newParsedSensorEnrichmentConfig.batchSize;
    if (newParsedSensorEnrichmentConfig.configuration != null) {
      this.sensorEnrichmentConfig.configuration = newParsedSensorEnrichmentConfig.configuration;
    }
    this.sensorEnrichmentConfig.enrichment = Object.assign(new EnrichmentConfig(), newParsedSensorEnrichmentConfig.enrichment);
    this.sensorEnrichmentConfig.index = newParsedSensorEnrichmentConfig.index;
    this.sensorEnrichmentConfig.threatIntel = Object.assign(new ThreatIntelConfig(), newParsedSensorEnrichmentConfig.threatIntel);
    this.hideStellar.emit(true);
    this.onStellarChanged.emit(true);
  }

  onCancel(): void {
    // this.init();
    this.hideStellar.emit(true);
  }
}
