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
import {Router, NavigationStart} from '@angular/router';
import {SensorParserConfig} from '../../model/sensor-parser-config';
import {SensorParserConfigService} from '../../service/sensor-parser-config.service';
import {MetronAlerts} from '../../shared/metron-alerts';
import {MetronDialogBox} from '../../shared/metron-dialog-box';
import {Sort} from '../../util/enums';
import {SortEvent} from '../../shared/metron-table/metron-table.directive';
import {StormService} from '../../service/storm.service';
import {TopologyStatus} from '../../model/topology-status';
import {SensorParserConfigHistory} from '../../model/sensor-parser-config-history';
import {SensorParserConfigHistoryService} from '../../service/sensor-parser-config-history.service';

@Component({
  selector: 'metron-config-sensor-parser-list',
  templateUrl: 'sensor-parser-list.component.html',
  styleUrls: ['sensor-parser-list.component.scss']
})
export class SensorParserListComponent implements OnInit {

  componentName: string = 'Sensors';
  @ViewChild('table') table;

  count: number = 0;
  sensors: SensorParserConfigHistory[] = [];
  sensorsStatus: TopologyStatus[] = [];
  selectedSensor: SensorParserConfigHistory;
  selectedSensors: SensorParserConfigHistory[] = [];
  enableAutoRefresh: boolean = true;

  constructor(private sensorParserConfigService: SensorParserConfigService,
              private sensorParserConfigHistoryService: SensorParserConfigHistoryService,
              private stormService: StormService,
              private router: Router,
              private metronAlerts:  MetronAlerts,
              private metronDialogBox: MetronDialogBox) {
    router.events.subscribe(event => {
      if (event instanceof NavigationStart && event.url === '/sensors') {
        this.onNavigationStart();
      }
    });
  }

  getSensors(justOnce: boolean) {
    this.sensorParserConfigService.getAll().subscribe(
      (results: {string: SensorParserConfig}) => {
        this.sensors = [];
        for (let sensorName of Object.keys(results)) {
          let sensorParserConfigHistory = new SensorParserConfigHistory();
          sensorParserConfigHistory.sensorName = sensorName;
          sensorParserConfigHistory.config = results[sensorName];
          this.sensors.push(sensorParserConfigHistory);
        }
        this.selectedSensors = [];
        this.count = this.sensors.length;
        if (!justOnce) {
          this.getStatus();
          this.pollStatus();
        } else {
          this.getStatus();
        }

      }
    );
  }

  onSort($event: SortEvent) {
    switch ($event.sortBy) {
      case 'sensorName':
        this.sensors.sort((obj1: SensorParserConfigHistory, obj2: SensorParserConfigHistory) => {
          if ($event.sortOrder === Sort.ASC) {
            return obj1.sensorName.localeCompare(obj2.sensorName);
          }
          return obj2.sensorName.localeCompare(obj1.sensorName);
        });
        break;
      case 'parserClassName':
        this.sensors.sort((obj1: SensorParserConfigHistory, obj2: SensorParserConfigHistory) => {
          if ($event.sortOrder === Sort.ASC) {
            return this.getParserType(obj1.config).localeCompare(this.getParserType(obj2.config));
          }
          return this.getParserType(obj2.config).localeCompare(this.getParserType(obj1.config));
        });
        break;
      case 'status':
      case 'modifiedBy':
      case 'modifiedByDate':
      case 'latency':
      case 'throughput':
        this.sensors.sort((obj1: SensorParserConfigHistory, obj2: SensorParserConfigHistory) => {
          if (!obj1[$event.sortBy] || !obj1[$event.sortBy]) {
            return 0;
          }
          if ($event.sortOrder === Sort.ASC) {
            return obj1[$event.sortBy].localeCompare(obj2[$event.sortBy]);
          }

          return obj2[$event.sortBy].localeCompare(obj1[$event.sortBy]);
        });
        break;
    }
  }

  private pollStatus() {
    this.stormService.pollGetAll().subscribe(
        (results: TopologyStatus[]) => {
          this.sensorsStatus = results;
          this.updateSensorStatus();
        },
        error => {
          this.updateSensorStatus();
        }
    );
  }

  private getStatus() {
    this.stormService.getAll().subscribe(
      (results: TopologyStatus[]) => {
        this.sensorsStatus = results;
        this.updateSensorStatus();
      },
      error => {
        this.updateSensorStatus();
      }
    );
  }

  updateSensorStatus() {
      for (let sensor of this.sensors) {

        let status: TopologyStatus = this.sensorsStatus.find(status => {
          return status.name === sensor.sensorName;
        });

        if (status) {
          if (status.status === 'ACTIVE') {
            sensor['status'] = 'Running';
          }
          if (status.status === 'KILLED') {
            sensor['status'] = 'Stopped';
          }
          if (status.status === 'INACTIVE') {
            sensor['status'] = 'Disabled';
          }
        } else {
          sensor['status'] = 'Stopped';
        }

        sensor['latency'] = status && status.status === 'ACTIVE' ? (status.latency + 'ms') : '-';
        sensor['throughput'] = status && status.status === 'ACTIVE' ? (Math.round(status.throughput * 100) / 100) + 'kb/s' : '-';
      }
  }

  getParserType(sensor: SensorParserConfig): string {
    let items = sensor.parserClassName.split('.');
    return items[items.length - 1].replace('Basic', '').replace('Parser', '');
  }

  ngOnInit() {
    this.getSensors(false);
    this.sensorParserConfigService.dataChanged$.subscribe(
      data => {
        this.getSensors(true);
      }
    );
  }

  addAddSensor() {
    this.router.navigateByUrl('/sensors(dialog:sensors-config/new)');
  }

  navigateToSensorEdit(selectedSensor: SensorParserConfigHistory, event) {
    this.selectedSensor = selectedSensor;
    this.router.navigateByUrl('/sensors(dialog:sensors-config/' + selectedSensor.sensorName + ')');
    event.stopPropagation();
  }

  onRowSelected(sensor: SensorParserConfigHistory, $event) {
    if ($event.target.checked) {
      this.selectedSensors.push(sensor);
    } else {
      this.selectedSensors.splice(this.selectedSensors.indexOf(sensor), 1);
    }
  }

  onSelectDeselectAll($event) {
    let checkBoxes = this.table.nativeElement.querySelectorAll('tr td:last-child input[type="checkbox"]');

    for (let ele of checkBoxes) {
      ele.checked = $event.target.checked;
    }

    if ($event.target.checked) {
      this.selectedSensors = this.sensors.slice(0).map((sensorParserInfo: SensorParserConfigHistory) => sensorParserInfo);
    } else {
      this.selectedSensors = [];
    }
  }

  onSensorRowSelect(sensor: SensorParserConfigHistory, $event) {
    if ($event.target.type !== 'checkbox' && $event.target.parentElement.firstChild.type !== 'checkbox') {

      if (this.selectedSensor === sensor) {
        this.selectedSensor = null;
        this.router.navigateByUrl('/sensors');
        return;
      }
      this.selectedSensor = sensor;
      this.router.navigateByUrl('/sensors(dialog:sensors-readonly/' + sensor.sensorName + ')');
    }
  }

  deleteSensor($event, selectedSensorsToDelete: SensorParserConfigHistory[]) {
    if ($event) {
      $event.stopPropagation();
    }

    let sensorNames = selectedSensorsToDelete.map(sensor => { return sensor.sensorName; });
    let confirmationsMsg = 'Are you sure you want to delete sensor(s) ' + sensorNames.join(', ') + ' ?';

    this.metronDialogBox.showConfirmationMessage(confirmationsMsg).subscribe(result => {
      if (result) {
        this.sensorParserConfigService.deleteSensorParserConfigs(sensorNames)
            .subscribe((deleteResult: {success: Array<string>, failure: Array<string>}) => {
          if (deleteResult.success.length > 0) {
            this.metronAlerts.showSuccessMessage('Deleted sensors: ' + deleteResult.success.join(', '));
          }

          if (deleteResult.failure.length > 0) {
            this.metronAlerts.showErrorMessage('Unable to deleted sensors: ' + deleteResult.failure.join(', '));
          }
        });
      }
    });
  }

  onDeleteSensor() {
    this.deleteSensor(null, this.selectedSensors);
  }

  onStopSensors() {
    for (let sensor of this.selectedSensors) {
      if (sensor['status'] === 'Running' || sensor['status'] === 'Disabled') {
        this.onStopSensor(sensor, null);
      }
    }
  }

  onStopSensor(sensor: SensorParserConfigHistory, event) {
    this.toggleStartStopInProgress(sensor);

    this.stormService.stopParser(sensor.sensorName).subscribe(result => {
        this.metronAlerts.showSuccessMessage('Stopped sensor ' + sensor.sensorName);
        this.toggleStartStopInProgress(sensor);
      },
      error => {
        this.metronAlerts.showErrorMessage('Unable to stop sensor ' + sensor.sensorName);
        this.toggleStartStopInProgress(sensor);
      });

    if (event !== null) {
      event.stopPropagation();
    }
  }

  onStartSensors() {
    for (let sensor of this.selectedSensors) {
      if (sensor['status'] === 'Stopped') {
        this.onStartSensor(sensor, null);
      }
    }
  }

  onStartSensor(sensor: SensorParserConfigHistory, event) {
    this.toggleStartStopInProgress(sensor);

    this.stormService.startParser(sensor.sensorName).subscribe(result => {
        if (result['status'] === 'ERROR') {
          this.metronAlerts.showErrorMessage('Unable to start sensor ' + sensor.sensorName + ': ' + result['message']);
        } else {
          this.metronAlerts.showSuccessMessage('Started sensor ' + sensor.sensorName);
        }

        this.toggleStartStopInProgress(sensor);
      },
      error => {
        this.metronAlerts.showErrorMessage('Unable to start sensor ' + sensor.sensorName);
        this.toggleStartStopInProgress(sensor);
      });

    if (event !== null) {
      event.stopPropagation();
    }
  }

  onDisableSensors() {
    for (let sensor of this.selectedSensors) {
      if (sensor['status'] === 'Running') {
        this.onDisableSensor(sensor, null);
      }
    }
  }

  onDisableSensor(sensor: SensorParserConfigHistory, event) {
    this.toggleStartStopInProgress(sensor);

    this.stormService.deactivateParser(sensor.sensorName).subscribe(result => {
        this.metronAlerts.showSuccessMessage('Disabled sensor ' + sensor.sensorName);
        this.toggleStartStopInProgress(sensor);
      },
      error => {
        this.metronAlerts.showErrorMessage('Unable to disable sensor ' + sensor.sensorName);
        this.toggleStartStopInProgress(sensor);
      });

    if (event !== null) {
      event.stopPropagation();
    }
  }

  onEnableSensors() {
    for (let sensor of this.selectedSensors) {
      if (sensor['status'] === 'Disabled') {
        this.onEnableSensor(sensor, null);
      }
    }
  }

  onEnableSensor(sensor: SensorParserConfigHistory, event) {
    this.toggleStartStopInProgress(sensor);

    this.stormService.activateParser(sensor.sensorName).subscribe(result => {
        this.metronAlerts.showSuccessMessage('Enabled sensor ' + sensor.sensorName);
        this.toggleStartStopInProgress(sensor);
      },
      error => {
        this.metronAlerts.showErrorMessage('Unable to enabled sensor ' + sensor.sensorName);
        this.toggleStartStopInProgress(sensor);
      });

    if (event != null) {
      event.stopPropagation();
    }
  }

  toggleStartStopInProgress(sensor: SensorParserConfigHistory) {
    sensor.config['startStopInProgress'] = !sensor.config['startStopInProgress'];
  }

  onNavigationStart() {
    this.selectedSensor = null;
    this.selectedSensors = [];
  }
}
