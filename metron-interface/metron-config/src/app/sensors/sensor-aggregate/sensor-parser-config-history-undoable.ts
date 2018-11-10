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
import { SensorParserConfigHistory } from '../../model/sensor-parser-config-history';
import { Subject, Observable } from 'rxjs';

const DEFAULT_UNDO_TIMEOUT = 60000;

export class SensorParserConfigHistoryUndoable {

  _sensor: SensorParserConfigHistory = null;

  _cache: any = null;
  _previousIndex = -1;

  _isParent: boolean;
  _timer = -1;

  changed$ = new Subject();

  constructor(sensor: SensorParserConfigHistory) {
    this._sensor = sensor;
  }

  getSensor(): SensorParserConfigHistory {
    return this._sensor;
  }

  setProps(props) {

    if (typeof props.groupName !== 'undefined') {
      this._sensor.group = props.groupName;
    }

    if (typeof props.status !== 'undefined') {
      this._sensor.status = props.status;
    }

    if (this._timer) {
      this._stopTimer();
    }
    this._startTimer(() => {
      this._next();
      this._stopTimer();
      // call persist !
    }, 10000);
  }

  setName(name: string) {
    this._sensor.sensorName = name;
  }

  setIsParent(value: boolean) {
    this._isParent = value;
  }

  isParent() {
    return this._isParent;
  }

  setStatus(status: string) {
    this._sensor.status = status;
  }

  _startTimer(fn, delay = DEFAULT_UNDO_TIMEOUT) {
    this._timer = setTimeout(fn, delay);
  }

  _stopTimer() {
    clearTimeout(this._timer);
    this._timer = -1;
  }

  canUndo() {
    return this._timer > -1;
  }

  isChanged(): Observable<any> {
    return this.changed$.asObservable();
  }

  _next() {
    this.changed$.next();
  }

  destroy() {
    if (this._timer) {
      this._stopTimer();
    }
    this._sensor = null;
  }

  restorePreviousState() {
    this._sensor = this._cache;

    this._cache = null;
    this._stopTimer();

    // this._next();
  }

  storePreviousState() {
    this._cache = this._sensor.clone();
  }

  getPreviousState() {
    return this._cache;
  }
}
