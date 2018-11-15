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
import { Component, OnInit } from '@angular/core';
import { KafkaService } from '../../service/kafka.service';
import { Router, ActivatedRoute } from '@angular/router';
import { KafkaTopic } from '../../model/kafka-topic';
import { MetronAlerts } from '../../shared/metron-alerts';
import { SensorParserConfigService } from '../../service/sensor-parser-config.service';
import { StormService } from '../../service/storm.service';
import { TopologyStatus } from '../../model/topology-status';
import { SensorParserConfigHistoryService } from '../../service/sensor-parser-config-history.service';
import { SensorParserConfigHistory } from '../../model/sensor-parser-config-history';
import { SensorEnrichmentConfigService } from '../../service/sensor-enrichment-config.service';
import { SensorEnrichmentConfig } from '../../model/sensor-enrichment-config';
import { RiskLevelRule } from '../../model/risk-level-rule';
import { HdfsService } from '../../service/hdfs.service';
import { RestError } from '../../model/rest-error';
import { GrokValidationService } from '../../service/grok-validation.service';
import { SensorParserConfig } from '../../model/sensor-parser-config';

@Component({
  selector: 'metron-config-sensor-parser-readonly',
  templateUrl: 'sensor-parser-config-readonly.component.html',
  styleUrls: ['sensor-parser-config-readonly.component.scss']
})
export class SensorParserConfigReadonlyComponent implements OnInit {
  selectedSensorName: string;
  startStopInProgress: boolean = false;
  kafkaTopic: KafkaTopic = new KafkaTopic();
  sensorParserConfigHistory: SensorParserConfigHistory = new SensorParserConfigHistory();
  sensorParserConfig: SensorParserConfig = new SensorParserConfig();
  topologyStatus: TopologyStatus = new TopologyStatus();
  sensorEnrichmentConfig: SensorEnrichmentConfig = new SensorEnrichmentConfig();
  grokStatement = {};
  transformsConfigKeys: string[] = [];
  transformsConfigMap: {} = {};
  rules: RiskLevelRule[] = [];
  transformLinkText = 'show more';
  threatTriageLinkText = 'show more';

  editViewMetaData: {
    label?: string;
    value?: string;
    type?: string;
    model?: string;
    boldTitle?: boolean;
  }[] = [
    { type: 'SEPARATOR', model: '', value: '' },
    {
      label: 'PARSER',
      model: 'sensorParserConfigHistory',
      value: 'parserClassName'
    },
    {
      label: 'LAST UPDATED',
      model: 'sensorParserConfigHistory',
      value: 'modifiedByDate'
    },
    {
      label: 'LAST EDITOR',
      model: 'sensorParserConfigHistory',
      value: 'modifiedBy'
    },
    { label: 'STATE', model: 'topologyStatus', value: 'sensorStatus' },
    {
      label: 'ORIGINATOR',
      model: 'sensorParserConfigHistory',
      value: 'createdBy'
    },
    {
      label: 'CREATION DATE',
      model: 'sensorParserConfigHistory',
      value: 'createdDate'
    },

    { type: 'SPACER', model: '', value: '' },

    {
      label: 'STORM',
      model: 'topologyStatus',
      value: 'status',
      boldTitle: true
    },
    { label: 'LATENCY', model: 'topologyStatus', value: 'latency' },
    { label: 'THROUGHPUT', model: 'topologyStatus', value: 'throughput' },
    { label: 'EMITTED(10 MIN)', model: 'topologyStatus', value: 'emitted' },
    { label: 'ACKED(10 MIN)', model: 'topologyStatus', value: 'acked' },
    { label: 'NUM WORKERS', model: 'sensorParserConfig', value: 'numWorkers' },
    { label: 'NUM ACKERS', model: 'sensorParserConfig', value: 'numAckers' },
    {
      label: 'SPOUT PARALLELISM',
      model: 'sensorParserConfig',
      value: 'spoutParallelism'
    },
    {
      label: 'SPOUT NUM TASKS',
      model: 'sensorParserConfig',
      value: 'spoutNumTasks'
    },
    {
      label: 'PARSER PARALLELISM',
      model: 'sensorParserConfig',
      value: 'parserParallelism'
    },
    {
      label: 'PARSER NUM TASKS',
      model: 'sensorParserConfig',
      value: 'parserNumTasks'
    },
    {
      label: 'ERROR WRITER PARALLELISM',
      model: 'sensorParserConfig',
      value: 'errorWriterParallelism'
    },
    {
      label: 'ERROR NUM TASKS',
      model: 'sensorParserConfig',
      value: 'errorWriterNumTasks'
    },

    { type: 'SPACER', model: '', value: '' },

    {
      label: 'KAFKA',
      model: 'kafkaTopic',
      value: 'currentKafkaStatus',
      boldTitle: true
    },
    { label: 'PARTITONS', model: 'kafkaTopic', value: 'numPartitions' },
    {
      label: 'REPLICATION FACTOR',
      model: 'kafkaTopic',
      value: 'replicationFactor'
    },
    { type: 'SEPARATOR', model: '', value: '' },

    { label: '', model: 'grokStatement', value: 'grokPattern' },

    { type: 'TITLE', model: '', value: 'Schema' },
    { label: '', model: 'transforms', value: '' },
    { type: 'SEPARATOR', model: '', value: '' },

    { type: 'TITLE', model: '', value: 'Threat Triage Rules' },
    { label: '', model: 'threatTriageRules', value: '' }
  ];

  constructor(
    private sensorParserConfigHistoryService: SensorParserConfigHistoryService,
    private sensorParserConfigService: SensorParserConfigService,
    private sensorEnrichmentService: SensorEnrichmentConfigService,
    private stormService: StormService,
    private kafkaService: KafkaService,
    private hdfsService: HdfsService,
    private grokValidationService: GrokValidationService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private metronAlerts: MetronAlerts
  ) {}

  getSensorInfo(): void {
    this.sensorParserConfigHistoryService
      .get(this.selectedSensorName)
      .subscribe((results: SensorParserConfigHistory) => {
        this.sensorParserConfigHistory = results;
        this.sensorParserConfig = this.sensorParserConfigHistory.config;
        this.setGrokStatement();
        this.setTransformsConfigKeys();

        let items = this.sensorParserConfigHistory.config.parserClassName.split(
          '.'
        );
        this.sensorParserConfigHistory['parserClassName'] = items[
          items.length - 1
        ]
          .replace('Basic', '')
          .replace('Parser', '');
      });
  }

  getSensorStatusService() {
    this.stormService.getStatus(this.selectedSensorName).subscribe(
      (results: TopologyStatus) => {
        this.topologyStatus = results;
      },
      error => {
        this.topologyStatus.status = 'Stopped';
      }
    );
  }

  getKafkaData(): void {
    this.kafkaService.get(this.selectedSensorName).subscribe(
      (results: KafkaTopic) => {
        this.kafkaTopic = results;
        this.kafkaService.sample(this.selectedSensorName).subscribe(
          (sampleData: string) => {
            this.kafkaTopic['currentKafkaStatus'] =
              sampleData && sampleData.length > 0 ? 'Emitting' : 'Not Emitting';
          },
          error => {
            this.kafkaTopic['currentKafkaStatus'] = 'Not Emitting';
          }
        );
      },
      error => {
        this.kafkaTopic['currentKafkaStatus'] = 'No Kafka Topic';
      }
    );
  }

  getEnrichmentData() {
    this.sensorEnrichmentService
      .get(this.selectedSensorName)
      .subscribe(sensorEnrichmentConfig => {
        this.sensorEnrichmentConfig = sensorEnrichmentConfig;
        this.rules =
          sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules;
      });
  }

  getTopologyStatus(key: string): string {
    if (key === 'latency') {
      return this.topologyStatus.latency >= 0
        ? this.topologyStatus.latency + 'ms'
        : '-';
    } else if (key === 'throughput') {
      return this.topologyStatus.throughput >= 0
        ? Math.round(this.topologyStatus.throughput * 100) / 100 + 'kb/s'
        : '-';
    } else if (key === 'emitted') {
      return this.topologyStatus.emitted >= 0
        ? this.topologyStatus.emitted + ''
        : '-';
    } else if (key === 'acked') {
      return this.topologyStatus.acked >= 0
        ? this.topologyStatus.acked + ''
        : '-';
    } else if (key === 'sensorStatus') {
      if (this.topologyStatus.status === 'ACTIVE') {
        return 'Enabled';
      } else if (this.topologyStatus.status === 'INACTIVE') {
        return 'Disabled';
      } else {
        return '-';
      }
    } else if (key === 'status') {
      if (this.topologyStatus.status === 'ACTIVE') {
        return 'Running';
      } else if (this.topologyStatus.status === 'INACTIVE') {
        return 'Disabled';
      } else {
        return 'Stopped';
      }
    }

    return this.topologyStatus[key] ? this.topologyStatus[key] : '-';
  }

  ngOnInit() {
    this.activatedRoute.params.subscribe(params => {
      this.selectedSensorName = params['id'];
      this.getData();
    });
  }

  getData() {
    this.startStopInProgress = false;

    this.getSensorInfo();
    this.getSensorStatusService();
    this.getKafkaData();
    this.getEnrichmentData();
  }

  setGrokStatement() {
    if (
      this.sensorParserConfigHistory.config &&
      this.sensorParserConfigHistory.config.parserConfig
    ) {
      let path = this.sensorParserConfigHistory.config.parserConfig['grokPath'];
      if (path) {
        this.hdfsService.read(path).subscribe(
          contents => {
            this.grokStatement = contents;
          },
          (hdfsError: RestError) => {
            this.grokValidationService.getStatement(path).subscribe(
              contents => {
                this.grokStatement = contents;
              },
              (grokError: RestError) => {
                this.metronAlerts.showErrorMessage(
                  'Could not find grok statement in HDFS or classpath at ' +
                    path
                );
              }
            );
          }
        );
      }
    }
  }

  setTransformsConfigKeys() {
    if (
      this.sensorParserConfigHistory.config &&
      this.sensorParserConfigHistory.config.fieldTransformations &&
      this.sensorParserConfigHistory.config.fieldTransformations.length > 0
    ) {
      this.transformsConfigKeys = [];
      for (let transforms of this.sensorParserConfigHistory.config
        .fieldTransformations) {
        if (transforms.config) {
          for (let key of Object.keys(transforms.config)) {
            if (this.transformsConfigKeys.indexOf(key) === -1) {
              this.transformsConfigMap[key] = [];
              this.transformsConfigKeys.push(key);
            }
            this.transformsConfigMap[key].push(transforms.config[key]);
          }
        }
      }
      this.transformsConfigKeys = this.transformsConfigKeys.sort();
    }
  }

  getTransformsOutput(): string {
    if (
      this.sensorParserConfigHistory.config &&
      this.sensorParserConfigHistory.config.fieldTransformations &&
      this.sensorParserConfigHistory.config.fieldTransformations.length > 0
    ) {
      let output = [];
      for (let transforms of this.sensorParserConfigHistory.config
        .fieldTransformations) {
        if (transforms.output) {
          output = output.concat(transforms.output);
        }
      }
      output = output.sort().filter(function(item, pos, self) {
        return self.indexOf(item) === pos;
      });

      return output.join(', ');
    }

    return '-';
  }

  goBack() {
    this.router.navigateByUrl('/sensors');
  }

  onEditSensor() {
    this.router.navigateByUrl(
      '/sensors(dialog:sensors-config/' + this.selectedSensorName + ')'
    );
  }

  onStartSensor() {
    this.toggleStartStopInProgress();
    let name = this.selectedSensorName;

    this.stormService.startParser(name).subscribe(
      result => {
        this.metronAlerts.showSuccessMessage('Started sensor ' + name);
        this.toggleStartStopInProgress();
        this.getData();
      },
      error => {
        this.metronAlerts.showErrorMessage('Unable to start sensor ' + name);
        this.toggleStartStopInProgress();
      }
    );
  }

  onStopSensor() {
    this.toggleStartStopInProgress();

    let name = this.selectedSensorName;
    this.stormService.stopParser(name).subscribe(
      result => {
        this.metronAlerts.showSuccessMessage('Stopped sensor ' + name);
        this.toggleStartStopInProgress();
        this.getData();
      },
      error => {
        this.metronAlerts.showErrorMessage('Unable to stop sensor ' + name);
        this.toggleStartStopInProgress();
      }
    );
  }

  onEnableSensor() {
    this.toggleStartStopInProgress();

    let name = this.selectedSensorName;
    this.stormService.activateParser(name).subscribe(
      result => {
        this.metronAlerts.showSuccessMessage('Enabled sensor ' + name);
        this.toggleStartStopInProgress();
        this.getData();
      },
      error => {
        this.metronAlerts.showErrorMessage('Unable to enabled sensor ' + name);
        this.toggleStartStopInProgress();
      }
    );
  }

  onDisableSensor() {
    this.toggleStartStopInProgress();

    let name = this.selectedSensorName;
    this.stormService.deactivateParser(name).subscribe(
      result => {
        this.metronAlerts.showSuccessMessage('Disabled sensor ' + name);
        this.toggleStartStopInProgress();
        this.getData();
      },
      error => {
        this.metronAlerts.showErrorMessage('Unable to disable sensor ' + name);
        this.toggleStartStopInProgress();
      }
    );
  }

  onDeleteSensor() {
    this.toggleStartStopInProgress();

    let name = this.selectedSensorName;
    this.sensorParserConfigService.deleteSensorParserConfig(name).subscribe(
      result => {
        this.metronAlerts.showSuccessMessage('Deleted sensor ' + name);
        this.toggleStartStopInProgress();
        this.sensorParserConfigService.dataChangedSource.next([name]);
        this.goBack();
      },
      error => {
        this.metronAlerts.showErrorMessage('Unable to delete sensor ' + name);
        this.toggleStartStopInProgress();
      }
    );
  }

  toggleStartStopInProgress() {
    this.startStopInProgress = !this.startStopInProgress;
  }

  getRuleDisplayName(): string {
    return this.rules.map(x => this.getDisplayName(x)).join(', ');
  }

  getDisplayName(riskLevelRule: RiskLevelRule): string {
    if (riskLevelRule.name) {
      return riskLevelRule.name;
    } else {
      return riskLevelRule.rule ? riskLevelRule.rule : '';
    }
  }

  toggleTransformLink() {
    return (this.transformLinkText =
      this.transformLinkText === 'show more' ? 'show less' : 'show more');
  }

  toggleThreatTriageLink() {
    return (this.threatTriageLinkText =
      this.threatTriageLinkText === 'show more' ? 'show less' : 'show more');
  }

  isStartHidden() {
    return (
      this.topologyStatus.status === 'ACTIVE' ||
      this.topologyStatus.status === 'INACTIVE'
    );
  }

  isStopHidden() {
    return (
      this.topologyStatus.status === 'KILLED' ||
      this.topologyStatus.status === 'Stopped'
    );
  }

  isEnableHidden() {
    return (
      this.topologyStatus.status === 'ACTIVE' ||
      this.topologyStatus.status === 'KILLED' ||
      this.topologyStatus.status === 'Stopped'
    );
  }

  isDisableHidden() {
    return (
      this.topologyStatus.status === 'INACTIVE' ||
      this.topologyStatus.status === 'KILLED' ||
      this.topologyStatus.status === 'Stopped'
    );
  }
}
