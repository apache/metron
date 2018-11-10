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
import { Injectable } from '@angular/core';
import { SensorParserConfigHistory } from '../../model/sensor-parser-config-history';
import { SensorParserConfigHistoryUndoable } from './sensor-parser-config-history-undoable';
import { Subject, Observable, Subscription } from 'rxjs';

@Injectable ()
export class SensorParserConfigHistoryListController {

  _sensors: SensorParserConfigHistoryUndoable[];
  _subscriptions: Subscription[] = [];
  changed$ = new Subject();

  setSensors(sensors: SensorParserConfigHistory[]) {

    if (this._sensors && this._sensors.length) {
      this._sensors.forEach(sensor => {
        sensor.destroy();
      });
    }

    this._sensors = sensors.map((sensor, i) => {
      const sensorUndoable = new SensorParserConfigHistoryUndoable(sensor);

      this._subscriptions.push(
        sensorUndoable.isChanged().subscribe(() => this._next(this._sensors)),
      );
      return sensorUndoable;
    });

    this._next(this._sensors);
  }

  getSensors(): SensorParserConfigHistoryUndoable[] {
    return [
      ...(this._sensors || [])
    ];
  }

  // groupSensors(groupName: string, ...sensors: SensorParserConfigHistoryUndoable[]) {
  //   const sensor1 = sensors[0];
  //   const sensor2 = sensors[1];

  //   const group = new SensorParserConfigHistoryUndoable(
  //     new SensorParserConfigHistory()
  //   );

  //   group.setName(groupName);
  //   group.setIsParent(true);

  //   sensor1.setProps({ groupName });
  //   sensor2.setProps({ groupName });

  //   this._injectItemsAt([group, sensor1, sensor2], this._sensors.findIndex(sensor => sensor === sensor2) - 1);

  //   this._next(this._sensors);
  // }

  _removeItems(items) {
    this._sensors = this._sensors.filter(sensor => !items.includes(sensor));
  }

  _injectItemsAt(items: SensorParserConfigHistoryUndoable[], index: number, storePrevious = true) {

    this._removeItems(items);
    this._sensors.splice(index, 0, ...items)
  }

  isChanged(): Observable<SensorParserConfigHistoryUndoable[]> {
    return this.changed$.asObservable() as Observable<SensorParserConfigHistoryUndoable[]>;
  }

  _next(sensors) {
    this.changed$.next([
      ...sensors
    ]);
  }

  tearDown() {

    this._subscriptions.forEach(subscription => {
      subscription.unsubscribe();
    });

    this._subscriptions.length = 0;

    this._sensors.forEach(sensor => {
      sensor.destroy();
    });

    this._sensors.length = 0;
  }

  findLastItemIndexInGroup(groupName): number {
    let lastIndex = -1;

    let i = 0, len = this._sensors.length;
    for (; i < len; i++) {
      if (this._sensors[i].getSensor().group === groupName) {
        lastIndex = i;
      }
    }

    if (lastIndex < 0) {
      const group = this.getGroup(groupName);
      return this._sensors.indexOf(group);
    }
    return lastIndex;
  }

  getGroup(groupName: string): SensorParserConfigHistoryUndoable | null {
    let i = 0, len = this._sensors.length;
    for (; i < len; i++) {
      if (this._sensors[i].getSensor().sensorName === groupName) {
        return this._sensors[i];
      }
    }
    return null;
  }

  createGroup(groupName: string, at?: number): SensorParserConfigHistoryUndoable {
    const group = new SensorParserConfigHistoryUndoable(
      new SensorParserConfigHistory()
    );

    group.setName(groupName);
    group.setStatus('Stopping');
    group.setIsParent(true);

    if (typeof at === 'undefined') {
      this._sensors.push(group);
    } else {
      this._sensors.splice(at, 0, group);
    }

    return group;
  }

  addToGroup(groupName: string, sensor: SensorParserConfigHistoryUndoable) {

    let group = this.getGroup(groupName);
    if (!group) {
      group = this.createGroup(groupName, this._sensors.indexOf(sensor));
    }

    sensor.storePreviousState();

    sensor.setProps({
      groupName
    });

    // reposition the sensor in the array
    this._sensors = this._sensors.filter(s => s !== sensor);
    this._sensors.splice(this.findLastItemIndexInGroup(groupName) + 1, 0, sensor);

    this._next(this._sensors);
  }

  restorePreviousState(sensor: SensorParserConfigHistoryUndoable) {

    const previous = sensor.getPreviousState();
    const previousGroup = previous.group;
    const groupName = sensor.getSensor().group;

    sensor.restorePreviousState();

    // reposition the sensor in the array
    this._sensors = this._sensors.filter(s => s !== sensor);
    this._sensors.splice(this.findLastItemIndexInGroup(previousGroup || groupName) + 1, 0, sensor);

    this._next(this._sensors);
  }
}
