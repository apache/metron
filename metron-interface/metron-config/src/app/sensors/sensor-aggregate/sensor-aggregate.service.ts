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
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { SensorParserConfigHistoryUndoable } from './sensor-parser-config-history-undoable';

@Injectable ()
export class SensorAggregateService {

  _sensorsToBeMerged: SensorParserConfigHistoryUndoable[] = [];

  executeMerge$ = new Subject();

  constructor(private router: Router) {}

  getSensorsToBeMerged() {
    return [
      ...this._sensorsToBeMerged
    ];
  }

  markSensorToBeMerged(sensor: SensorParserConfigHistoryUndoable, index: number) {
    this._sensorsToBeMerged[index] = sensor;
  }

  unmarkAllSensorsToBeMerged() {
    this._sensorsToBeMerged.length = 0;
  }

  close() {
    this.unmarkAllSensorsToBeMerged();
    this.router.navigateByUrl('/sensors');
  }

  save(groupName: string, description: string) {
    this.executeMerge$.next({
      groupName,
      description,
      sensors: [
        ...this._sensorsToBeMerged
      ]
    });

    this.close();
  }
}
