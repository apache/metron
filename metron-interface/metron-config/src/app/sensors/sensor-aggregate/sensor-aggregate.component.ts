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
import { Component } from '@angular/core';
import { SensorAggregateService } from './sensor-aggregate.service';
import { Router } from '@angular/router';

@Component({
  selector: 'metron-config-sensor-aggregate',
  templateUrl: './sensor-aggregate.component.html',
  styleUrls: ['./sensor-aggregate.component.scss']
})
export class SensorAggregateComponent {

  allowMerge = true;

  _forceCreate = false;

  constructor(
    private aggregateService: SensorAggregateService,
    private router: Router
    ) {}

  close() {
    this._forceCreate = false;
    this.aggregateService.close();
  }

  createNew(groupName: string, description: string) {
    this.aggregateService.save(groupName, description);
  }

  mergeOrCreate() {
    if (this.allowMerge) {
      this.addToExisting();
    } else {
      this._forceCreate = true;
    }
  }

  addToExisting() {
    this.aggregateService.save(
      this.aggregateService.getTargetSensor().getGroup(),
      ''
    );
  }

  showCreateForm(): boolean {
    return !this.aggregateService.doesTargetSensorHaveGroup() || this._forceCreate;
  }
}
