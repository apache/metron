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
import { ParserMetaInfoModel } from '../models/parser-meta-info.model';
import { Subject, Observable, Subscription } from 'rxjs';
import { SensorParserConfigService } from '../../service/sensor-parser-config.service';
import { ParserGroupModel } from '../models/parser-group.model';
import { ParserConfigModel } from '../models/parser-config.model';

@Injectable ()
export class SensorParserConfigHistoryListController {

  _sensors: ParserMetaInfoModel[];
  _subscriptions: Subscription[] = [];
  changed$ = new Subject();

  constructor(
    private sensorParserConfigService: SensorParserConfigService
  ) {}

  setSensors(sensors: ParserConfigModel[]) {

    this._next([]);

    this._sensors = sensors.map((sensor, i) => {
      const sensorUndoable = new ParserMetaInfoModel(sensor);

      this._subscriptions.push(
        sensorUndoable.isChanged().subscribe(() => this._next(this._sensors)),
      );
      return sensorUndoable;
    });

    this.sensorParserConfigService.getAllGroups().subscribe((groups) => {
      this._combineGroupsAndSensors(groups, this._sensors);
      this._next(this._sensors);
    });
  }

  _combineGroupsAndSensors(groups: ParserGroupModel[], sensors: ParserMetaInfoModel[]) {
    groups.forEach((group, i) => {
      this.createGroup(group.name, i);
    });
    const grouppedSensors = this._sensors.filter(s => s.hasGroup());
    this._sensors = this._sensors.filter(s => !s.hasGroup());
    grouppedSensors.forEach(s => {
      this.addToGroup(s.getGroup(), s, { silent: true });
    });
  }

  getSensors(): ParserMetaInfoModel[] {
    return [
      ...(this._sensors || [])
    ];
  }

  isChanged(): Observable<ParserMetaInfoModel[]> {
    return this.changed$.asObservable() as Observable<ParserMetaInfoModel[]>;
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

  getGroup(groupName: string): ParserMetaInfoModel | null {
    let i = 0, len = this._sensors.length;
    for (; i < len; i++) {
      if (this._sensors[i].getConfig().getName() === groupName) {
        return this._sensors[i];
      }
    }
    return null;
  }

  /**
   * @param groupName - create a new group with this group name
   * @param at - The array index where you want to inject the group after creation
   */
  createGroup(groupName: string, at?: number): ParserMetaInfoModel {
    const group = new ParserMetaInfoModel(new ParserGroupModel({
      name: groupName
    }));

    group.setName(groupName);
    group.setIsGroup(true);

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
  addToGroup(groupName: string, sensor: ParserMetaInfoModel, options: any = {}) {

    let group = this.getGroup(groupName);
    if (!group) {
      group = this.createGroup(groupName, this._sensors.indexOf(sensor));
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

  getByName(name: string): ParserMetaInfoModel {
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

  insertBefore(target: ParserMetaInfoModel, sensor: ParserMetaInfoModel) {

    if ((target.hasGroup() || sensor.hasGroup()) && target.getGroup() !== sensor.getGroup()) {
      sensor.setProps({
        groupName: (sensor.hasGroup() && !target.hasGroup()) ? '' : target.getGroup()
      });
    }

    // reposition the sensor in the array
    this._sensors = this._sensors.filter(s => s !== sensor);
    const targetIndex = this._sensors.indexOf(target);
    this._sensors.splice(targetIndex, 0, sensor);

    this._next(this._sensors);
  }

  insertAfter(target: ParserMetaInfoModel, sensor: ParserMetaInfoModel) {

    let newGroup;
    if (target.isGroup()) {
      newGroup = target.getName();
    } else {
      newGroup = target.getGroup() || '';
    }
    if (newGroup !== sensor.getGroup()) {
      sensor.setProps({
        groupName: newGroup
      });
    }

    // reposition the sensor in the array
    this._sensors = this._sensors.filter(s => s !== sensor);
    const targetIndex = this._sensors.indexOf(target);
    this._sensors.splice(targetIndex + 1, 0, sensor);

    this._next(this._sensors);
  }
}
