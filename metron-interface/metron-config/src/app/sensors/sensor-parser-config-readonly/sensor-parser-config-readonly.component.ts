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
import {Component, OnInit} from '@angular/core';
import {KafkaService} from '../../service/kafka.service';
import {Router, ActivatedRoute} from '@angular/router';
import {KafkaTopic} from '../../model/kafka-topic';
import {MetronAlerts} from '../../shared/metron-alerts';
import {SensorParserConfigService} from '../../service/sensor-parser-config.service';
import {StormService} from '../../service/storm.service';
import {TopologyStatus} from '../../model/topology-status';
import {SensorParserConfigHistoryService} from '../../service/sensor-parser-config-history.service';
import {SensorParserConfigHistory} from '../../model/sensor-parser-config-history';
import {SensorEnrichmentConfigService} from '../../service/sensor-enrichment-config.service';
import {SensorEnrichmentConfig} from '../../model/sensor-enrichment-config';

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
  topologyStatus: TopologyStatus = new TopologyStatus();
  sensorEnrichmentConfig: SensorEnrichmentConfig = new SensorEnrichmentConfig();
  grokStatement: string = '';
  transformsConfigKeys: string[] = [];
  transformsConfigMap: {} = {};
  aggregationConfigKeys: string[] = [];

  editViewMetaData: {label?: string, value?: string, type?: string, model?: string, boldTitle?: boolean}[] = [
    {type: 'SEPARATOR', model: '', value: ''},
    {label: 'PARSERS', model: 'sensorParserConfigHistory', value: 'parserName'},
    {label: 'LAST UPDATED', model: 'sensorParserConfigHistory', value: 'modifiedByDate'},
    {label: 'LAST EDITOR', model: 'sensorParserConfigHistory', value: 'modifiedBy'},
    {label: 'STATE', model: 'topologyStatus', value: 'sensorStatus'},
    {label: 'ORIGINATOR', model: 'sensorParserConfigHistory', value: 'createdBy'},
    {label: 'CREATION DATE', model: 'sensorParserConfigHistory', value: 'createdDate'},

    {type: 'SPACER', model: '', value: ''},

    {label: 'STORM', model: 'topologyStatus', value: 'status', boldTitle: true},
    {label: 'LATENCY', model: 'topologyStatus', value: 'latency'},
    {label: 'THROUGHPUT', model: 'topologyStatus', value: 'throughput'},
    {label: 'EMITTED(10 MIN)', model: 'topologyStatus', value: 'emitted'},
    {label: 'ACKED(10 MIN)', model: 'topologyStatus', value: 'acked'},

    {type: 'SPACER', model: '', value: ''},

    {label: 'KAFKA', model: 'kafkaTopic', value: 'currentKafkaStatus', boldTitle: true},
    {label: 'PARTITONS', model: 'kafkaTopic', value: 'numPartitions'},
    {label: 'REPLICATION FACTOR', model: 'kafkaTopic', value: 'replicationFactor'},
    {type: 'SEPARATOR', model: '', value: ''},

    {type: 'TITLE', model: '', value: 'Grok Statement'},
    {label: '', model: 'grokStatement', value: 'grokPattern'},
    {type: 'SEPARATOR', model: '', value: ''},

    {type: 'TITLE', model: '', value: 'Schema'},
    {label: '', model: 'transforms', value: ''},
    {type: 'SEPARATOR', model: '', value: ''},

    {type: 'TITLE', model: '', value: 'Threat Triage Rules'},
    {label: '', model: 'threatTriageRules', value: ''}

  ];

  constructor(private sensorParserConfigHistoryService: SensorParserConfigHistoryService,
              private sensorParserConfigService: SensorParserConfigService,
              private sensorEnrichmentService: SensorEnrichmentConfigService,
              private stormService: StormService,
              private kafkaService: KafkaService,
              private activatedRoute: ActivatedRoute, private router: Router,
              private metronAlerts: MetronAlerts) {
  }

  getSensorInfo(): void {
    this.sensorParserConfigHistoryService.get(this.selectedSensorName).subscribe(
      (results: SensorParserConfigHistory) => {
        this.sensorParserConfigHistory = results;

        this.sensorParserConfigHistory['parserName'] =
            (this.sensorParserConfigHistory.config.parserClassName === 'org.apache.metron.parsers.GrokParser') ? 'Grok' : 'Java';
        this.setGrokStatement();
        this.setTransformsConfigKeys();
      });
  }

  getSensorStatusService() {
    this.stormService.getStatus(this.selectedSensorName).subscribe(
      (results: TopologyStatus) => {
        this.topologyStatus = results;
        this.topologyStatus.latency = (this.topologyStatus.latency ? this.topologyStatus.latency : '0') + 's';
        this.topologyStatus.throughput = (this.topologyStatus.throughput ? (Math.round(parseFloat(this.topologyStatus.throughput) * 100) / 100) : '0') + 'kb/s';


        this.topologyStatus['sensorStatus'] = '-';

        if (this.topologyStatus.status === 'ACTIVE') {
          this.topologyStatus.status = 'Running';
          this.topologyStatus['sensorStatus'] = 'Enabled';
        } else if (this.topologyStatus.status === 'KILLED') {
          this.topologyStatus.status = 'Stopped';
        } else if (this.topologyStatus.status === 'INACTIVE') {
          this.topologyStatus.status = 'Disabled';
          this.topologyStatus['sensorStatus'] = 'Disabled';
        } else {
          this.topologyStatus.status = 'Stopped';
          }
      },
      error => {
        this.topologyStatus.status = 'Stopped';
        this.topologyStatus['sensorStatus'] = '-';
      });
  }

  getKafkaData(): void {
    this.kafkaService.get(this.selectedSensorName).subscribe(
      (results: KafkaTopic) => {
        this.kafkaTopic = results;
        this.kafkaService.sample(this.selectedSensorName).subscribe((sampleData: string) => {
              this.kafkaTopic['currentKafkaStatus'] = (sampleData && sampleData.length > 0) ? 'Emitting' : 'Not Emitting';
            },
            error => {
              this.kafkaTopic['currentKafkaStatus'] = 'Not Emitting';
            });
      }, error => {
          this.kafkaTopic['currentKafkaStatus'] = 'No Kafka Topic';
      });
  }

  getEnrichmentData() {
    this.sensorEnrichmentService.get(this.selectedSensorName).subscribe((sensorEnrichmentConfig) => {
      this.sensorEnrichmentConfig = sensorEnrichmentConfig;
      this.aggregationConfigKeys = Object.keys(sensorEnrichmentConfig.threatIntel.triageConfig.riskLevelRules);
    });
  }

  ngOnInit() {
    this.activatedRoute.params.subscribe(params => {
      this.selectedSensorName = params['id'];
      this.getData();
    });
  }

  getData() {
    this.getSensorInfo();
    this.getSensorStatusService();
    this.getKafkaData();
    this.getEnrichmentData();
  }

  setGrokStatement() {
    if (this.sensorParserConfigHistory.config && this.sensorParserConfigHistory.config.parserConfig &&
        this.sensorParserConfigHistory.config.parserConfig['grokStatement']) {
      this.grokStatement = this.sensorParserConfigHistory.config.parserConfig['grokStatement'];
    }
  }

  setTransformsConfigKeys() {
    if (this.sensorParserConfigHistory.config && this.sensorParserConfigHistory.config.fieldTransformations &&
        this.sensorParserConfigHistory.config.fieldTransformations.length > 0) {
      this.transformsConfigKeys = [];
      for (let transforms of this.sensorParserConfigHistory.config.fieldTransformations) {
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
    if (this.sensorParserConfigHistory.config && this.sensorParserConfigHistory.config.fieldTransformations &&
        this.sensorParserConfigHistory.config.fieldTransformations.length > 0) {
      let output = [];
      for (let transforms of this.sensorParserConfigHistory.config.fieldTransformations) {
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
    this.router.navigateByUrl('/sensors(dialog:sensors-config/' + this.selectedSensorName + ')');
  }

  onStartSensor() {
    this.toggleStartStopInProgress();

    this.stormService.startParser(this.selectedSensorName).subscribe(result => {
        this.metronAlerts.showSuccessMessage('Started sensor ' + this.selectedSensorName);
        this.toggleStartStopInProgress();
        this.getData();
      },
      error => {
        this.metronAlerts.showErrorMessage('Unable to start sensor ' + this.selectedSensorName);
        this.toggleStartStopInProgress();
      });
  }

  onStopSensor() {
    this.toggleStartStopInProgress();

    this.stormService.stopParser(this.selectedSensorName).subscribe(result => {
        this.metronAlerts.showSuccessMessage('Stopped sensor ' + this.selectedSensorName);
        this.toggleStartStopInProgress();
        this.getData();
      },
      error => {
        this.metronAlerts.showErrorMessage('Unable to stop sensor ' + this.selectedSensorName);
        this.toggleStartStopInProgress();
      });
  }

  onEnableSensor() {
    this.toggleStartStopInProgress();

    this.stormService.activateParser(this.selectedSensorName).subscribe(result => {
        this.metronAlerts.showSuccessMessage('Enabled sensor ' + this.selectedSensorName);
        this.toggleStartStopInProgress();
        this.getData();
      },
      error => {
        this.metronAlerts.showErrorMessage('Unable to enabled sensor ' + this.selectedSensorName);
        this.toggleStartStopInProgress();
      });
  }

  onDisableSensor() {
    this.toggleStartStopInProgress();

    this.stormService.deactivateParser(this.selectedSensorName).subscribe(result => {
        this.metronAlerts.showSuccessMessage('Disabled sensor ' + this.selectedSensorName);
        this.toggleStartStopInProgress();
        this.getData();
      },
      error => {
        this.metronAlerts.showErrorMessage('Unable to disable sensor ' + this.selectedSensorName);
        this.toggleStartStopInProgress();
      });
  }

  onDeleteSensor() {
    this.toggleStartStopInProgress();

    this.sensorParserConfigService.deleteSensorParserConfig(this.selectedSensorName).subscribe(result => {
        this.metronAlerts.showSuccessMessage('Deleted sensor ' + this.selectedSensorName);
        this.toggleStartStopInProgress();
        this.sensorParserConfigService.dataChangedSource.next([this.sensorParserConfigHistory.config]);
        this.goBack();
      },
      error => {
        this.metronAlerts.showErrorMessage('Unable to delete sensor ' + this.selectedSensorName);
        this.toggleStartStopInProgress();
      });
  }

  toggleStartStopInProgress() {
    this.startStopInProgress = !this.startStopInProgress;
  }
}
