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

    /**
     * Initially, the list handled by this controller doesn't include the group (root)
     * elements. We need to create and add them to the list and "attach" all the parsers
     * that belong to the newly created group element.
     */
    const collectGroups = () => {
      this._sensors.forEach(sensor => {
        if (sensor.getGroup()) {
          this.addToGroup(sensor.getGroup(), sensor, { silent: true });
        }
      });
    }

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

    collectGroups();

    this._next(this._sensors);
  }

  getSensors(): SensorParserConfigHistoryUndoable[] {
    return [
      ...(this._sensors || [])
    ];
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
      if (this._sensors[i].getGroup() === groupName) {
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

  /**
   * @param groupName - create a new group with this group name
   * @param at - The array index where you want to inject the group after creation
   */
  createGroup(groupName: string, at?: number): SensorParserConfigHistoryUndoable {
    const group = new SensorParserConfigHistoryUndoable(
      new SensorParserConfigHistory()
    );

    group.setName(groupName);
    group.setIsParent(true);

    if (typeof at === 'undefined') {
      this._sensors.push(group);
    } else {
      this._sensors.splice(at, 0, group);
    }

    return group;
  }

  /**
   * @param groupName
   * @param sensor
   * @param options.startTimer - whether we should start a timer on the parser (undoable)
   * @param options.silent - If it's true, it won't call next on the changed$ observer
   */
  addToGroup(groupName: string, sensor: SensorParserConfigHistoryUndoable, options: any = {}) {

    let group = this.getGroup(groupName);
    if (!group) {
      group = this.createGroup(groupName, this._sensors.indexOf(sensor));
    }

    if (options.startTimer) {
      // when we merge to parsers together, their status is "stopping"
      // until the timer expires.
      group.setStatus('Stopping');

      // basically you can undo the this action until the time expires.
      // when you undo this action you want the previous state back therefore we store it.
      sensor.storePreviousState();
      sensor.startTimer();
    }

    // update the sensor
    sensor.setProps({
      groupName
    });

    // reposition the sensor in the array
    this._sensors = this._sensors.filter(s => s !== sensor);
    this._sensors.splice(this.findLastItemIndexInGroup(groupName) + 1, 0, sensor);

    if (!options.silent) {
      this._next(this._sensors);
    }
  }

  /**
   * @param sensor
   * @param options.silent - If it's true, it won't call next on the changed$ observer
   */
  restorePreviousState(sensor: SensorParserConfigHistoryUndoable, options: any = {}) {

    const previous = sensor.getPreviousState();
    const previousGroup = previous.config.group;
    const groupName = sensor.getGroup();

    sensor.restorePreviousState();

    // reposition the sensor in the array
    this._sensors = this._sensors.filter(s => s !== sensor);
    this._sensors.splice(this.findLastItemIndexInGroup(previousGroup || groupName) + 1, 0, sensor);

    if (!options.silent) {
      this._next(this._sensors);
    }
  }

  getByName(name: string): SensorParserConfigHistoryUndoable {
    return this._sensors.find(sensor => sensor.getName() === name);
  }

  setAllHighlighted(value: boolean) {
    this._sensors.forEach(sensor => {
      sensor.setHighlighted(value);
    })
  }

  setAllDraggedOver(value: boolean) {
    this._sensors.forEach(sensor => {
      sensor.setDraggedOver(value);
    })
  }
}
