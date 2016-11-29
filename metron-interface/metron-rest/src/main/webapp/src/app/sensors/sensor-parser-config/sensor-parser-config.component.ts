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
import {Component, OnInit, ViewChild} from '@angular/core';
import {FormGroup, Validators, FormControl} from '@angular/forms';
import {SensorParserConfig} from '../../model/sensor-parser-config';
import {SensorParserConfigService} from '../../service/sensor-parser-config.service';
import {Router, ActivatedRoute} from '@angular/router';
import {MetronAlerts} from '../../shared/metron-alerts';
import {TransformationValidation} from '../../model/transformation-validation';
import {SensorEnrichmentConfigService} from '../../service/sensor-enrichment-config.service';
import {SensorEnrichmentConfig} from '../../model/sensor-enrichment-config';
import {SensorFieldSchemaComponent} from '../sensor-field-schema/sensor-field-schema.component';
import {SensorStellarComponent} from '../sensor-stellar/sensor-stellar.component';
import {HttpUtil} from '../../util/httpUtil';

export enum Pane {
  GROK, STELLAR, FIELDSCHEMA
}

@Component({
  selector: 'metron-config-sensor',
  templateUrl: 'sensor-parser-config.component.html',
  styleUrls: ['sensor-parser-config.component.scss']
})

export class SensorParserConfigComponent implements OnInit {

  sensorConfigForm: FormGroup;
  transformsValidationForm: FormGroup;

  sensorParserConfig: SensorParserConfig = new SensorParserConfig();
  sensorEnrichmentConfig: SensorEnrichmentConfig = new SensorEnrichmentConfig();

  showGrokValidator: boolean = false;
  showTransformsValidator: boolean = false;
  showAdvancedParserConfiguration: boolean = false;
  showStellar: boolean = false;
  showFieldSchema: boolean = false;

  availableParsers = {};
  availableParserNames = [];

  editMode: boolean = false;

  topicExists: boolean = false;

  transformsValidationResult: {map: any, keys: string[]} = {map: {}, keys: []};
  transformsValidation: TransformationValidation = new TransformationValidation();

  pane = Pane;
  openPane: Pane = null;

  @ViewChild(SensorFieldSchemaComponent) sensorFieldSchema: SensorFieldSchemaComponent;
  @ViewChild(SensorStellarComponent) sensorStellar: SensorStellarComponent;

  constructor(private sensorParserConfigService: SensorParserConfigService, private metronAlerts: MetronAlerts,
              private sensorEnrichmentConfigService: SensorEnrichmentConfigService, private route: ActivatedRoute,
              private router: Router) {
    this.sensorParserConfig.parserConfig = {};
  }


  init(id: string): void {
    if (id !== 'new') {
      this.editMode = true;

      this.sensorParserConfigService.get(id).subscribe(
        (results: SensorParserConfig) => {
          this.sensorParserConfig = results;
          if (Object.keys(this.sensorParserConfig.parserConfig).length > 0) {
            this.showAdvancedParserConfiguration = true;
          }
        });

      this.sensorEnrichmentConfigService.get(id).subscribe((result: SensorEnrichmentConfig) => {
        this.sensorEnrichmentConfig = result;
      },
      error => {
        this.sensorEnrichmentConfig = new SensorEnrichmentConfig();
        this.sensorEnrichmentConfig.index = id;
      });

    } else {
      this.sensorParserConfig = new SensorParserConfig();
    }
  }

  ngOnInit() {
    this.route.params.subscribe(params => {
      let id = params['id'];
      this.init(id);
    });
    this.createForms();
    this.getAvailableParsers();
  }

  createSensorConfig(): FormGroup {
    let group: any = {};

    group['sensorTopic'] = new FormControl(this.sensorParserConfig.sensorTopic, Validators.required);
    group['parserClassName'] = new FormControl(this.sensorParserConfig.parserClassName, Validators.required);
    group['grokStatement'] = new FormControl(this.sensorParserConfig.parserConfig['grokStatement']);
    group['transforms'] = new FormControl(this.sensorParserConfig['transforms']);
    group['stellar'] = new FormControl(this.sensorParserConfig, Validators.required);
    group['index'] = new FormControl(this.sensorEnrichmentConfig.index, Validators.required);
    group['batchSize'] = new FormControl(this.sensorEnrichmentConfig.batchSize, Validators.required);

    return new FormGroup(group);
  }

  createTransformsValidationForm(): FormGroup {
    let group: any = {};

    group['sampleData'] = new FormControl(this.transformsValidation.sampleData, Validators.required);
    group['sensorParserConfig'] = new FormControl(this.transformsValidation.sensorParserConfig, Validators.required);

    return new FormGroup(group);
  }

  createForms() {
    this.sensorConfigForm = this.createSensorConfig();
    this.transformsValidationForm = this.createTransformsValidationForm();
    if (Object.keys(this.sensorParserConfig.parserConfig).length > 0) {
      this.showAdvancedParserConfiguration = true;
    }
  }

  private getAvailableParsers() {
    this.sensorParserConfigService.getAvailableParsers().subscribe(
      availableParsers => {
        this.availableParsers = availableParsers;
        this.availableParserNames = Object.keys(availableParsers);
      }
    );
  }

  private getMessagePrefix(): string {
    return this.editMode ? 'Modified' : 'Created';
  }

  onSetSensorName(): void {
    if (!this.sensorEnrichmentConfig.index) {
      this.sensorEnrichmentConfig.index = this.sensorParserConfig.sensorTopic;
    }
  }

  getTransforms(): string {
    let count = 0;
    if (this.sensorParserConfig.fieldTransformations) {
      for (let tranforms of this.sensorParserConfig.fieldTransformations) {
        if (tranforms.output) {
          count += tranforms.output.length;
        }
      }
    }

    return count + ' Transformations Applied';
  }

  getStellar(): string {
    let count = 0;
    let transformConfigObject = this.sensorParserConfig.fieldTransformations.filter(fieldTransformer =>
      fieldTransformer.transformation === 'STELLAR');
    if (transformConfigObject.length > 0 && transformConfigObject[0].config &&
        Object.keys(transformConfigObject[0].config).length > 0) {
      count++;
    }
    let enrichmentConfig = this.sensorEnrichmentConfig.enrichment.fieldMap['stellar'];
    if (enrichmentConfig && enrichmentConfig.config && Object.keys(enrichmentConfig.config).length > 0) {
      count++;
    }
    let triageConfigObject = this.sensorEnrichmentConfig.threatIntel.triageConfig;
    if (triageConfigObject && triageConfigObject.riskLevelRules && Object.keys(triageConfigObject.riskLevelRules).length > 0) {
      count++;
    }
    return count + ' Stellar Configs Applied';
  }

  goBack() {
    this.router.navigateByUrl('/sensors');
  }

  onSave() {
    let sensorParserConfigSave: SensorParserConfig = new SensorParserConfig();
    sensorParserConfigSave.parserConfig = {};
    sensorParserConfigSave.sensorTopic = this.sensorParserConfig.sensorTopic;
    sensorParserConfigSave.parserClassName = this.sensorParserConfig.parserClassName;
    sensorParserConfigSave.parserConfig = this.sensorParserConfig.parserConfig;
    if (this.isGrokParser()) {
      sensorParserConfigSave.parserConfig['grokStatement'] = this.sensorParserConfig.parserConfig['grokStatement'];
    }
    sensorParserConfigSave.fieldTransformations = this.sensorParserConfig.fieldTransformations;

    this.sensorParserConfigService.post(sensorParserConfigSave).subscribe(
      sensorParserConfig => {
        this.sensorEnrichmentConfigService.post(sensorParserConfig.sensorTopic, this.sensorEnrichmentConfig).subscribe(
            (sensorEnrichmentConfig: SensorEnrichmentConfig) => {
              this.metronAlerts.showSuccessMessage(this.getMessagePrefix() + ' Sensor ' + sensorParserConfig.sensorTopic);
              this.sensorParserConfigService.dataChangedSource.next([sensorParserConfigSave]);
              this.goBack();
        },
        error => {
            let msg = ' Sensor parser config but unable to save enrichment configuration: ';
            this.metronAlerts.showErrorMessage(this.getMessagePrefix() + msg + HttpUtil.getErrorMessageFromBody(error));
        });
      },
      error => {
        this.metronAlerts.showErrorMessage(this.getMessagePrefix() + ' Sensor parser config: ' + HttpUtil.getErrorMessageFromBody(error));
      });
  }

  isGrokParser(): boolean {
    return this.sensorParserConfig.parserClassName === 'org.apache.metron.parsers.GrokParser';
  }

  getTransformationCount(): number {
    let stellarTransformations = this.sensorParserConfig.fieldTransformations.filter(fieldTransformer =>
      fieldTransformer.transformation === 'STELLAR');
    if (stellarTransformations.length > 0 && stellarTransformations[0].config) {
      return Object.keys(stellarTransformations[0].config).length;
    } else {
      return 0;
    }
  }

  getEnrichmentCount(): number {
    let count = 0;
    if (this.sensorEnrichmentConfig.enrichment.fieldMap) {
      for (let enrichment in this.sensorEnrichmentConfig.enrichment.fieldMap) {
        if (enrichment !== 'hbaseEnrichment' && enrichment !== 'stellar') {
          count += this.sensorEnrichmentConfig.enrichment.fieldMap[enrichment].length;
        }
      }
    }
    if (this.sensorEnrichmentConfig.enrichment.fieldToTypeMap) {
      for (let fieldName in this.sensorEnrichmentConfig.enrichment.fieldToTypeMap) {
        if (this.sensorEnrichmentConfig.enrichment.fieldToTypeMap.hasOwnProperty(fieldName)) {
          count += this.sensorEnrichmentConfig.enrichment.fieldToTypeMap[fieldName].length;
        }
      }
    }
    return count;
  }

  getThreatIntelCount(): number {
    let count = 0;
    if (this.sensorEnrichmentConfig.threatIntel.fieldToTypeMap) {
      for (let fieldName in this.sensorEnrichmentConfig.threatIntel.fieldToTypeMap) {
        if (this.sensorEnrichmentConfig.threatIntel.fieldToTypeMap.hasOwnProperty(fieldName)) {
          count += this.sensorEnrichmentConfig.threatIntel.fieldToTypeMap[fieldName].length;
        }
      }
    }
    return count;
  }

  showPane(pane: Pane) {
    this.setPaneVisibility(pane, true);
  }

  hidePane(pane: Pane) {
    this.setPaneVisibility(pane, false);
  }

  setPaneVisibility(pane: Pane, visibilty: boolean) {
      this.showGrokValidator = (pane === Pane.GROK) ? visibilty : false;
      this.showFieldSchema = (pane === Pane.FIELDSCHEMA) ? visibilty : false;
      this.showStellar = (pane ===  Pane.STELLAR) ? visibilty : false;
  }

  onFieldSchemaChanged(): void {
    this.sensorStellar.init();
  }

  onStellarChanged(): void {
    this.sensorFieldSchema.createFieldSchemaRows();
  }

  onAdvancedConfigFormClose(): void {
    this.showAdvancedParserConfiguration = false;
  }
}
