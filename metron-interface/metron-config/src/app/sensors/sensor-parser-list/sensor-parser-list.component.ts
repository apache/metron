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
import {ParserConfigModel} from '../models/parser-config.model';
import {SensorParserConfigService} from '../../service/sensor-parser-config.service';
import {MetronAlerts} from '../../shared/metron-alerts';
import {MetronDialogBox} from '../../shared/metron-dialog-box';
import {StormService} from '../../service/storm.service';
import {TopologyStatus} from '../../model/topology-status';
import {SensorParserConfigHistory} from '../../model/sensor-parser-config-history';
import { Subscription, Observable } from 'rxjs';
import { ParserMetaInfoModel } from '../models/parser-meta-info.model';
import { Store, select } from '@ngrx/store';
import { ParserGroupModel } from '../models/parser-group.model';
import * as ParsersActions from '../parser-configs.actions';
import * as parserSelectors from '../parser-configs.selectors';
import { SensorParserStatus } from '../../model/sensor-parser-status';
import { ParserState } from '../parser-configs.reducers';
import { SensorState } from '../reducers';
import { ParserModel } from '../models/parser.model';
import * as DragAndDropActions from '../parser-configs-dnd.actions';

@Component({
  selector: 'metron-config-sensor-parser-list',
  templateUrl: 'sensor-parser-list.component.html',
  styleUrls: ['sensor-parser-list.component.scss']
})
export class SensorParserListComponent implements OnInit, OnDestroy {

  componentName = 'Sensors';
  @ViewChild('table') table;

  count = 0;
  sensorsStatus: TopologyStatus[] = [];
  selectedSensor: ParserMetaInfoModel;
  selectedSensors: ParserMetaInfoModel[] = [];
  enableAutoRefresh = true;
  sensorsToRender: ParserMetaInfoModel[];

  private mergedConfigs$: Observable<ParserMetaInfoModel[]>;
  private isStatusPolling: boolean;
  private draggedElement: ParserMetaInfoModel;

  constructor(private sensorParserConfigService: SensorParserConfigService,
              private stormService: StormService,
              private router: Router,
              private metronAlerts:  MetronAlerts,
              private metronDialogBox: MetronDialogBox,
              private store: Store<SensorState>) {
    router.events.subscribe(event => {
      if (event instanceof NavigationStart && event.url === '/sensors') {
        this.onNavigationStart();
      }
    });

    this.mergedConfigs$ = store.pipe(select(parserSelectors.getMergedConfigs));
  }

  getParserType(sensor: ParserConfigModel): string {
    if (!sensor.parserClassName) {
      return '';
    }
    let items = sensor.parserClassName.split('.');
    return items[items.length - 1].replace('Basic', '').replace('Parser', '');
  }

  ngOnInit() {
    this.store.dispatch(new ParsersActions.LoadStart());

    this.sensorParserConfigService.dataChanged$.subscribe(
      data => {
        this.store.dispatch(new ParsersActions.LoadStart());
      }
    );

    this.mergedConfigs$.subscribe((mergedConfigs) => {
      this.sensorsToRender = mergedConfigs;
      this.count = this.sensorsToRender.length;

      if (!this.isStatusPolling) {
        this.isStatusPolling = true;
        this.store.dispatch(new ParsersActions.StartPolling());
      }
    });
  }

  addAddSensor() {
    this.router.navigateByUrl('/sensors(dialog:sensors-config/new)');
  }

  navigateToSensorEdit(selectedSensor: ParserMetaInfoModel, event) {
    this.selectedSensor = selectedSensor;
    this.router.navigateByUrl('/sensors(dialog:sensors-config/' + selectedSensor.getName() + ')');
    event.stopPropagation();
  }

  onRowSelected(parserConfig: ParserMetaInfoModel, $event) {
    if ($event.target.checked) {
      this.selectedSensors.push(parserConfig);
    } else {
      this.selectedSensors.splice(this.selectedSensors.indexOf(parserConfig), 1);
    }
  }

  onSelectDeselectAll($event) {
    let checkBoxes = this.table.nativeElement.querySelectorAll('tr td:last-child input[type="checkbox"]');

    for (let ele of checkBoxes) {
      ele.checked = $event.target.checked;
    }

    if ($event.target.checked) {
      this.selectedSensors = this.sensorsToRender.slice(0);
    } else {
      this.selectedSensors = [];
    }
  }

  onSensorRowSelect(sensor: ParserMetaInfoModel) {
    if (this.selectedSensor === sensor) {
      this.selectedSensor = null;
      this.router.navigateByUrl('/sensors');
      return;
    }
    this.selectedSensor = sensor;
    this.router.navigateByUrl('/sensors(dialog:sensors-readonly/' + sensor.getName() + ')');
  }

  showConfirm(message: string, callback: Function) {
    this.metronDialogBox.showConfirmationMessage(message).subscribe(callback);
  }

  onDeleteSelectedItems() {
    const names = this.selectedSensors.map(p => p.getName());
    this.showConfirm('Are you sure you want to delete ' + names.join(', ') + ' ?', (confirmed: boolean) => {
      if (confirmed) {
        this.store.dispatch(new ParsersActions.MarkAsDeleted({
          parserIds: names
        }));
      }
    });
  }

  onDeleteItem(item: ParserMetaInfoModel, e: Event) {
    this.showConfirm('Are you sure you want to delete ' + item.getName() + ' ?', (confirmed: boolean) => {
      if (confirmed) {
        this.store.dispatch(new ParsersActions.MarkAsDeleted({
          parserIds: [item.getName()]
        }));
      }
    });
    e.stopPropagation();
  }

  private batchUpdateResultHandler(deleteResult: {success: Array<string>, failure: Array<string>}) {
    if (deleteResult.success.length > 0) {
      this.metronAlerts.showSuccessMessage('Deleted sensors: ' + deleteResult.success.join(', '));
    }
    if (deleteResult.failure.length > 0) {
      this.metronAlerts.showErrorMessage('Unable to deleted sensors: ' + deleteResult.failure.join(', '));
    }
  }

  onStopSensors() {
    for (let sensor of this.selectedSensors) {
      if (sensor.getStatus().status === 'ACTIVE' || sensor.getStatus().status === 'INACTIVE') {
        this.onStopSensor(sensor, null);
      }
    }
  }

  onStopSensor(sensor: ParserMetaInfoModel, event) {
    this.toggleStartStopInProgress(sensor);

    this.stormService.stopParser(sensor.getName()).subscribe(result => {
        this.metronAlerts.showSuccessMessage('Stopped sensor ' + sensor.getName());
        this.toggleStartStopInProgress(sensor);
      },
      error => {
        this.metronAlerts.showErrorMessage('Unable to stop sensor ' + sensor.getName());
        this.toggleStartStopInProgress(sensor);
      });

    if (event !== null) {
      event.stopPropagation();
    }
  }

  onStartSensors() {
    for (let sensor of this.selectedSensors) {
      if (sensor.getStatus().status === 'KILLED') {
        this.onStartSensor(sensor, null);
      }
    }
  }

  onStartSensor(sensor: ParserMetaInfoModel, event) {
    this.toggleStartStopInProgress(sensor);

    this.stormService.startParser(sensor.getName()).subscribe(result => {
        if (result['status'] === 'ERROR') {
          this.metronAlerts.showErrorMessage('Unable to start sensor ' + sensor.getName() + ': ' + result['message']);
        } else {
          this.metronAlerts.showSuccessMessage('Started sensor ' + sensor.getName());
        }

        this.toggleStartStopInProgress(sensor);
      },
      error => {
        this.metronAlerts.showErrorMessage('Unable to start sensor ' + sensor.getName());
        this.toggleStartStopInProgress(sensor);
      });

    if (event !== null) {
      event.stopPropagation();
    }
  }

  onDisableSensors() {
    for (let sensor of this.selectedSensors) {
      if (sensor.getStatus().status === 'ACTIVE') {
        this.onDisableSensor(sensor, null);
      }
    }
  }

  onDisableSensor(sensor: ParserMetaInfoModel, event) {
    this.toggleStartStopInProgress(sensor);

    this.stormService.deactivateParser(sensor.getName()).subscribe(result => {
        this.metronAlerts.showSuccessMessage('Disabled sensor ' + sensor.getName());
        this.toggleStartStopInProgress(sensor);
      },
      error => {
        this.metronAlerts.showErrorMessage('Unable to disable sensor ' + sensor.getName());
        this.toggleStartStopInProgress(sensor);
      });

    if (event !== null) {
      event.stopPropagation();
    }
  }

  onEnableSensors() {
    for (let sensor of this.selectedSensors) {
      if (sensor.getStatus().status === 'INACTIVE') {
        this.onEnableSensor(sensor, null);
      }
    }
  }

  onEnableSensor(sensor: ParserMetaInfoModel, event) {
    this.toggleStartStopInProgress(sensor);

    this.stormService.activateParser(sensor.getName()).subscribe(result => {
        this.metronAlerts.showSuccessMessage('Enabled sensor ' + sensor.getName());
        this.toggleStartStopInProgress(sensor);
      },
      error => {
        this.metronAlerts.showErrorMessage('Unable to enabled sensor ' + sensor.getName());
        this.toggleStartStopInProgress(sensor);
      });

    if (event != null) {
      event.stopPropagation();
    }
  }

  toggleStartStopInProgress(sensor: ParserMetaInfoModel) {
    sensor.startStopInProgress = !sensor.startStopInProgress;
  }

  onNavigationStart() {
    this.selectedSensor = null;
    this.selectedSensors = [];
  }

  onDragStart(metaInfo: ParserMetaInfoModel, e: DragEvent) {
    this.draggedElement = metaInfo;
    e.dataTransfer.setDragImage((e.target as HTMLElement).parentElement, 10, 17);
    this.store.dispatch(new DragAndDropActions.SetDragged(metaInfo.getName()));
  }

  onDragOver(sensor, e: DragEvent) {
    const el = (e.currentTarget as HTMLElement);
    const rect = el.getBoundingClientRect();
    const mouseX = e.pageX;
    const mouseY = e.pageY;

    if (mouseX > rect.left + 8 && mouseY > rect.top + 8 && mouseX <= (rect.right - 8) && mouseY <= (rect.bottom - 8)) {
      this.store.dispatch(new ParsersActions.SetDraggedOver({
        id: sensor.getName(),
        value: true,
      }));
    } else {
      this.store.dispatch(new ParsersActions.SetDraggedOver({
        id: sensor.getName(),
        value: false,
      }));
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
      this.store.dispatch(new ParsersActions.SetHighlighted({
        id: groupName,
        value: true,
      }));
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
      this.store.dispatch(new ParsersActions.SetDraggedOver({
        id: sensor.getName(),
        value: false,
      }));

      const groupName = sensor.getGroup();
      if (!groupName) {
        return;
      }
      this.store.dispatch(new ParsersActions.SetHighlighted({
        id: groupName,
        value: false,
      }));
    }
  }

  onDrop(referenceMetaInfo: ParserMetaInfoModel, e: DragEvent) {
    this.store.dispatch(new ParsersActions.SetAllHighlighted(false));
    this.store.dispatch(new ParsersActions.SetAllDraggedOver(false));
    const el = e.currentTarget as HTMLElement;
    const dragged = this.draggedElement;
    if (dragged.getName() !== referenceMetaInfo.getName() && !referenceMetaInfo.isDeleted) {
      if (el.classList.contains('drop-before') || el.classList.contains('drop-after')) {
        if (referenceMetaInfo.getGroup() !== dragged.getGroup() || referenceMetaInfo.isGroup()) {
          this.store.dispatch(new ParsersActions.AddToGroup({
            groupName: referenceMetaInfo.hasGroup()
              ? referenceMetaInfo.getGroup()
              : referenceMetaInfo.isGroup()
                ? referenceMetaInfo.getName()
                : '',
            parserIds: [dragged.getName()]
          }));
        }
      }
      if (el.classList.contains('drop-before')) {
        this.store.dispatch(new ParsersActions.InjectBefore({
          reference: referenceMetaInfo.getName(),
          parserId: dragged.getName(),
        }));
      } else if (el.classList.contains('drop-after')) {
        this.store.dispatch(new ParsersActions.InjectAfter({
          reference: referenceMetaInfo.getName(),
          parserId: dragged.getName(),
        }));
      } else {
        if (referenceMetaInfo.isGroup() && !referenceMetaInfo.isDeleted) {
          this.store.dispatch(new ParsersActions.AddToGroup({
            groupName: referenceMetaInfo.getName(),
            parserIds: [dragged.getName()]
          }));
          this.store.dispatch(new ParsersActions.InjectAfter({
            reference: referenceMetaInfo.getName(),
            parserId: dragged.getName(),
          }));
        } else {
          this.store.dispatch(new DragAndDropActions.SetDropTarget(referenceMetaInfo.getName()));
          this.store.dispatch(new DragAndDropActions.SetTargetGroup(referenceMetaInfo.getConfig().group || ''));
          this.router.navigateByUrl('/sensors(dialog:sensor-aggregate)');
        }
      }
    }
    el.classList.remove('drop-before');
    el.classList.remove('drop-after');
  }

  ngOnDestroy() { }

  isSelected(sensor) {
    // return this.selectedSensors.indexOf(sensor.getConfig()) !== -1 || this.selectedSensor === sensor.getConfig();
    return this.selectedSensors.find(s => {
      if (s.getName() === sensor.getName()) {
        return true;
      } else {
        return false;
      }
    })
  }
}
