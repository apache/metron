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
import {Component, OnInit, ViewChild, OnDestroy} from '@angular/core';
import {Router, NavigationStart} from '@angular/router';
import {SensorParserConfig} from '../../model/sensor-parser-config';
import {SensorParserConfigService} from '../../service/sensor-parser-config.service';
import {MetronAlerts} from '../../shared/metron-alerts';
import {MetronDialogBox} from '../../shared/metron-dialog-box';
import {StormService} from '../../service/storm.service';
import {TopologyStatus} from '../../model/topology-status';
import {SensorParserConfigHistory} from '../../model/sensor-parser-config-history';
import { SensorAggregateService } from '../sensor-aggregate/sensor-aggregate.service';
import { Subscription, Observable } from 'rxjs';
import { SensorParserConfigHistoryListController } from '../sensor-aggregate/sensor-parser-config-history-list.controller';
import { MetaParserConfigItem } from '../sensor-aggregate/meta-parser-config-item';
import { Store, select } from '@ngrx/store';
import { ParserGroupModel } from 'app/model/parser-group';
import * as ParsersActions from '../parser-configs.actions';
import * as parserSelectors from '../parser-configs.selectors';
import { SensorParserStatus } from '../../model/sensor-parser-status';
import { ParserState } from '../parser-configs.reducers';
import { SensorState } from '../reducers';

@Component({
  selector: 'metron-config-sensor-parser-list',
  templateUrl: 'sensor-parser-list.component.html',
  styleUrls: ['sensor-parser-list.component.scss']
})
export class SensorParserListComponent implements OnInit, OnDestroy {

  componentName = 'Sensors';
  @ViewChild('table') table;

  count = 0;
  sensors: SensorParserConfigHistory[] = [];
  sensorsStatus: TopologyStatus[] = [];
  selectedSensor: SensorParserConfigHistory;
  selectedSensors: SensorParserConfigHistory[] = [];
  enableAutoRefresh = true;
  _executeMergeSubscription: Subscription;
  sensorsToRender: MetaParserConfigItem[];

  private parserConfigs$: Observable<SensorState>;
  private mergedConfigs$: Observable<MetaParserConfigItem[]>;

  private isStatusPolling: boolean;

  constructor(private sensorParserConfigService: SensorParserConfigService,
              private stormService: StormService,
              private router: Router,
              private metronAlerts:  MetronAlerts,
              private metronDialogBox: MetronDialogBox,
              private sensorAggregateService: SensorAggregateService,
              private sensorParserConfigHistoryListController: SensorParserConfigHistoryListController,
              private store: Store<SensorState>) {
    router.events.subscribe(event => {
      if (event instanceof NavigationStart && event.url === '/sensors') {
        this.onNavigationStart();
      }
    });

    this.parserConfigs$ = store.select('sensors');
    this.mergedConfigs$ = store.pipe(select(parserSelectors.getMergedConfigs));
  }

  getParserType(sensor: SensorParserConfig): string {
    if (!sensor.parserClassName) {
      return '';
    }
    let items = sensor.parserClassName.split('.');
    return items[items.length - 1].replace('Basic', '').replace('Parser', '');
  }

  ngOnInit() {
    this.parserConfigs$.subscribe((state: SensorState) => {
      this.sensors = state.parsers.items;
      this.selectedSensors = [];
      this.count = this.sensors.length;
    })

    this.store.dispatch(new ParsersActions.LoadStart());

    this.sensorParserConfigService.dataChanged$.subscribe(
      data => {
        this.store.dispatch(new ParsersActions.LoadStart());
      }
    );

    this.mergedConfigs$.subscribe((mergedConfigs) => {
      this.sensorsToRender = mergedConfigs;

      if (!this.isStatusPolling) {
        this.isStatusPolling = true;
        this.store.dispatch(new ParsersActions.StartPolling());
      }
    });

    // FIXME: this codepart is not responsible for the merging of the list of groups and configs
    // but the actual creation of new groups
    this._executeMergeSubscription = this.sensorAggregateService.executeMerge$
      .subscribe((value: {
        groupName: string,
        sensors: MetaParserConfigItem[]
      }) => {
        if (!value.groupName || !value.sensors.length) {
          // there's nothing to group
          return;
        }
        this.addSensorsToGroup(value.groupName, value.sensors);
      });

    // TODO: unsubscribe
    // this.sensorParserConfigHistoryListController.isChanged().subscribe(
    //   (sensors: MetaParserConfigItem[]) => {
    //     this.sensorsToRender = sensors;
    //   }
    // );
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

  getDeleteFunc(sensor: MetaParserConfigItem) {
    return sensor.isGroup() ? this.deleteGroup.bind(this) : this.deleteSensor.bind(this);
  }

  // FIXME: template calls sensor.getSensor() wich return with a SensorParserConfigHistory
  deleteGroup(items: SensorParserConfigHistory[] | SensorParserConfigHistory, $event) {
    if ($event) {
      $event.stopPropagation();
    }

    this.deleteParserConfigListItems((itemNames) => {
      this.sensors.filter((item: SensorParserConfigHistory) => {
        return item.config.group && itemNames.includes(item.config.group);
      }).map((item: SensorParserConfigHistory) => {
        delete item.config.group;
        return item;
      }).forEach((item: SensorParserConfigHistory) => {
        this.sensorParserConfigService.saveConfig(item.sensorName, item.config)
          .subscribe();
      });

      return this.sensorParserConfigService.deleteGroups(itemNames);
    }, items);
  }

  // FIXME it could be a group as well, deleteSensor is not apropiate name anymore but it used other places
  // so I leave it as it is for now and create getDeleteFunc to manage group deletion.
  deleteSensor(items: SensorParserConfigHistory[] | SensorParserConfigHistory, $event: Event | null) {
    if ($event) {
      $event.stopPropagation();
    }
    this.deleteParserConfigListItems(this.sensorParserConfigService.deleteConfigs, items);
  }

  private deleteParserConfigListItems(
    typeSpecificDeleteFn: Function,
    items: SensorParserConfigHistory[] | SensorParserConfigHistory
    ) {
      const itemNames = this.getListOfItemNames(items);
      const confirmationsMsg = 'Are you sure you want to delete sensor(s) ' + itemNames.join(', ') + ' ?';

      this.metronDialogBox.showConfirmationMessage(confirmationsMsg).subscribe(result => {
        if (result) {
          typeSpecificDeleteFn.call(this.sensorParserConfigService, itemNames)
            .subscribe(this.batchUpdateResultHandler.bind(this));
        }
      });
  }

  private getListOfItemNames(items: SensorParserConfigHistory[] | SensorParserConfigHistory) {
    let itemsArr = [];
      if (Array.isArray(items)) {
        itemsArr = items;
      } else {
        itemsArr = [items];
      }

    return itemsArr.map(item => { return item.sensorName; });
  }

  private batchUpdateResultHandler(deleteResult: {success: Array<string>, failure: Array<string>}) {
    if (deleteResult.success.length > 0) {
      this.metronAlerts.showSuccessMessage('Deleted sensors: ' + deleteResult.success.join(', '));
    }
    if (deleteResult.failure.length > 0) {
      this.metronAlerts.showErrorMessage('Unable to deleted sensors: ' + deleteResult.failure.join(', '));
    }
  }

  onDeleteSensor() {
    this.deleteSensor(this.selectedSensors, null);
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
    sensor.config.startStopInProgress = !sensor.config.startStopInProgress;
  }

  onNavigationStart() {
    this.selectedSensor = null;
    this.selectedSensors = [];
  }

  onDragStart(sensor: MetaParserConfigItem, e: DragEvent) {
    this.sensorAggregateService.markSensorToBeMerged(sensor, 0);
    e.dataTransfer.setDragImage((e.target as HTMLElement).parentElement, 10, 17);
  }

  onDragOver(sensor, e: DragEvent) {
    const el = (e.currentTarget as HTMLElement);
    const rect = el.getBoundingClientRect();
    const mouseX = e.pageX;
    const mouseY = e.pageY;


    if (mouseX > rect.left + 8 && mouseY > rect.top + 8 && mouseX <= (rect.right - 8) && mouseY <= (rect.bottom - 8)) {
      sensor.setDraggedOver(true);
    } else {
      sensor.setDraggedOver(false);
    }

    if (mouseY > rect.top && mouseY < (rect.top + 8)) {
      el.classList.add('drop-before');
    } else {
      el.classList.remove('drop-before');
    }
    if (mouseY > rect.top && mouseY > (rect.bottom - 8) && mouseY <= rect.bottom) {
      el.classList.add('drop-after');
    } else {
      el.classList.remove('drop-after');
    }

    e.preventDefault();
  }

  onDragEnter(sensor, e) {

    const groupName = sensor.getGroup();
    if (!groupName) {
      return;
    }
    setTimeout(() => {
      const group = this.sensorParserConfigHistoryListController.getByName(groupName);
      group.setHighlighted(true);
    });
  }

  onDragLeave(sensor, e: DragEvent) {
    const el = e.currentTarget as HTMLElement;
    const rect = el.getBoundingClientRect();
    const mouseX = e.pageX;
    const mouseY = e.pageY;

    if (mouseX < rect.left || mouseY < rect.top || mouseX >= rect.right || mouseY >= rect.bottom) {
      el.classList.remove('drop-before');
      el.classList.remove('drop-after');
      sensor.setDraggedOver(false);

      const groupName = sensor.getGroup();
      if (!groupName) {
        return;
      }
      const group = this.sensorParserConfigHistoryListController.getByName(groupName);
      group.setHighlighted(false);
    }
  }

  onDrop(referenceSensor: MetaParserConfigItem, e: DragEvent) {

    this.sensorParserConfigHistoryListController.setAllHighlighted(false);
    this.sensorParserConfigHistoryListController.setAllDraggedOver(false);

    const el = e.currentTarget as HTMLElement;
    const draggedSensor = this.sensorAggregateService.getSensorsToBeMerged()[0];

    if (draggedSensor !== referenceSensor) {
      if (el.classList.contains('drop-before')) {
        this.sensorParserConfigHistoryListController.insertBefore(referenceSensor, draggedSensor);
      } else if (el.classList.contains('drop-after')) {
        this.sensorParserConfigHistoryListController.insertAfter(referenceSensor, draggedSensor);
      } else {
        if (referenceSensor.isGroup()) {

          this.sensorParserConfigHistoryListController
            .addToGroup(
              referenceSensor.getName(),
              draggedSensor,
              { startTimer: true });
        } else {
          this.sensorAggregateService.markSensorToBeMerged(referenceSensor, 1);
          this.router.navigateByUrl('/sensors(dialog:sensor-aggregate)');
        }
      }
    }

    el.classList.remove('drop-before');
    el.classList.remove('drop-after');
  }

  addSensorsToGroup(groupName: string, sensors: MetaParserConfigItem[]) {
    sensors.reverse().forEach(sensor => {
      if (sensor.getGroup() !== groupName) {
        this.sensorParserConfigHistoryListController.addToGroup(groupName, sensor, { startTimer: true });
      }
    });
  }

  undo(sensor: MetaParserConfigItem, e) {
    e.stopPropagation();
    this.sensorParserConfigHistoryListController.restorePreviousState(sensor);
  }

  ngOnDestroy() {
    this._executeMergeSubscription.unsubscribe();
    this.sensorParserConfigHistoryListController.tearDown();
  }
}
