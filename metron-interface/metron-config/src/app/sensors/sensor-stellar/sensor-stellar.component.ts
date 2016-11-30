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
import {Component, OnInit, Input, EventEmitter, Output, OnChanges, SimpleChanges} from '@angular/core';
import {SensorParserConfig} from '../../model/sensor-parser-config';
import {SensorEnrichmentConfig} from '../../model/sensor-enrichment-config';

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

  transformationConfig: string;
  enrichmentConfig: string;
  triageConfig: string;

  constructor() { }

  ngOnInit() {
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['showStellar'] && changes['showStellar'].currentValue) {
      this.init();
    }
  }

  init(): void {
    if (this.sensorParserConfig.fieldTransformations) {
      this.transformationConfig = JSON.stringify(this.sensorParserConfig.fieldTransformations, null, '\t');
    }

    let enrichmentConfigObject = this.sensorEnrichmentConfig.enrichment.fieldMap['stellar'];
    if (enrichmentConfigObject) {
      this.enrichmentConfig = JSON.stringify(enrichmentConfigObject.config, null, '\t');
    } else {
      this.enrichmentConfig = '{}';
    }

    let triageConfigObject = this.sensorEnrichmentConfig.threatIntel.triageConfig;
    if (triageConfigObject) {
      this.triageConfig = JSON.stringify(triageConfigObject, null, '\t');
    }
  }

  onSave() {
    let transformationConfigObjects = JSON.parse(this.transformationConfig);
    this.sensorParserConfig.fieldTransformations = transformationConfigObjects;
    let enrichmentConfigObject = JSON.parse('{"config": ' + this.enrichmentConfig + '}');
    this.sensorEnrichmentConfig.enrichment.fieldMap['stellar'] = enrichmentConfigObject;
    let triageConfigObject = JSON.parse(this.triageConfig);
    this.sensorEnrichmentConfig.threatIntel.triageConfig = triageConfigObject;
    this.hideStellar.emit(true);
    this.onStellarChanged.emit(true);
  }

  onCancel(): void {
    this.init();
    this.hideStellar.emit(true);
  }

  onTransformationBlur(): void {
    try {
      this.transformationConfig = JSON.stringify(JSON.parse(this.transformationConfig), null, '\t');
    } catch (e) {
    }
  }

  onEnrichmentBlur(): void {
    try {
      this.enrichmentConfig = JSON.stringify(JSON.parse(this.enrichmentConfig), null, '\t');
    } catch (e) {
    }
  }

  onTriageBlur(): void {
    try {
      this.triageConfig = JSON.stringify(JSON.parse(this.triageConfig), null, '\t');
    } catch (e) {
    }
  }

}
