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

  grokMainStatement: string = '';
  grokFunctionStatement: string = '';

  editViewMetaData: {label?: string, value?: string, type?: string, model?: string}[] = [
    {type: 'SEPARATOR', model: '', value: ''},
    {label: 'PARSER', model: 'sensorParserConfigHistory', value: 'parserName'},
    {label: 'STATUS', model: 'topologyStatus', value: 'status'},
    {label: 'LATENCY', model: 'topologyStatus', value: 'latency'},
    {label: 'THROUGHPUT', model: 'topologyStatus', value: 'throughput'},
    {label: 'CREATION DATE', model: 'sensorParserConfigHistory', value: 'createdDate'},
    {label: 'ORIGINATOR', model: 'sensorParserConfigHistory', value: 'createdBy'},
    {label: 'LAST EDITED', model: 'sensorParserConfigHistory', value: 'modifiedByDate'},
    {label: 'LAST EDITED BY', model: 'sensorParserConfigHistory', value: 'modifiedBy'},

    {type: 'SEPARATOR', model: '', value: ''},

    {type: 'TITLE', model: '', value: 'Kafka Topic'},
    {label: 'PARTITONS', model: 'kafka', value: 'numPartitions'},
    {label: 'REPLICATION FACTOR', model: 'kafka', value: 'replicationFactor'},
    {type: 'SEPARATOR', model: '', value: ''},

    {type: 'TITLE', model: '', value: 'Grok Statement'},
    {label: '', model: 'grokStatement', value: 'grokPattern'},
    {type: 'SEPARATOR', model: '', value: ''},

    {type: 'TITLE', model: '', value: 'Transforms'},
    {label: '', model: 'transforms', value: ''}

  ];

  constructor(private sensorParserConfigHistoryService: SensorParserConfigHistoryService,
              private sensorParserConfigService: SensorParserConfigService,
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
      });
  }

  getSensorStatusService() {
    this.stormService.getStatus(this.selectedSensorName).subscribe(
      (results: TopologyStatus) => {
        this.topologyStatus = results;

        this.topologyStatus.latency = (this.topologyStatus.latency ? this.topologyStatus.latency : '0') + 's';
        this.topologyStatus.throughput = (this.topologyStatus.throughput ? this.topologyStatus.throughput : '0') + 'kb/s';

        if (this.topologyStatus.status === 'ACTIVE') {
          this.topologyStatus.status = 'Running';
        } else if (this.topologyStatus.status === 'KILLED') {
          this.topologyStatus.status = 'Stopped';
        } else if (this.topologyStatus.status === 'INACTIVE') {
          this.topologyStatus.status = 'Disabled';
        } else {
          this.topologyStatus.status = 'Stopped';
          }
      },
      error => {
        this.topologyStatus.status = 'Stopped';
      });
  }

  getKafkaData(): void {
    this.kafkaService.get(this.selectedSensorName).subscribe(
      (results: KafkaTopic) => {
        this.kafkaTopic = results;
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
  }

  setGrokStatement() {
    if (this.sensorParserConfigHistory.config && this.sensorParserConfigHistory.config.parserConfig &&
        this.sensorParserConfigHistory.config.parserConfig['grokStatement']) {
      let statement = this.sensorParserConfigHistory.config.parserConfig['grokStatement'];
      if ((statement.match(/,/g) || []).length > 1) {
        statement = statement.replace(/\n/g, ' <br> ');
        let lastIndex = statement.lastIndexOf(' <br> ');
        this.grokMainStatement = statement.substr(lastIndex + 5);
        this.grokFunctionStatement = statement.substr(0, lastIndex);
      } else {
        this.grokMainStatement = statement;
      }
    }
  }

  getTransformsConfigKeys(): string[] {
    if (this.sensorParserConfigHistory.config && this.sensorParserConfigHistory.config.fieldTransformations &&
        this.sensorParserConfigHistory.config.fieldTransformations.length > 0) {
      return Object.keys(this.sensorParserConfigHistory.config.fieldTransformations[0].config);
      }

    return [];
  }

  getTransformsOutput(): string {
    if (this.sensorParserConfigHistory.config && this.sensorParserConfigHistory.config.fieldTransformations &&
        this.sensorParserConfigHistory.config.fieldTransformations.length > 0) {
      return this.sensorParserConfigHistory.config.fieldTransformations[0].output.join(', ');
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
