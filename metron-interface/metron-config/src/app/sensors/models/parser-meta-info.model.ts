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
import { ParserConfigModel } from './parser-config.model';
import { ParserGroupModel } from './parser-group.model'
import { Subject, Observable } from 'rxjs';
import { SensorParserConfigService } from 'app/service/sensor-parser-config.service';
import { ThrowStmt } from '@angular/compiler';
import { TopologyStatus } from '../../model/topology-status';
import { ParserModel } from './parser.model';

export class ParserMetaInfoModel {

  private config: ParserModel = null;

  _status: TopologyStatus = new TopologyStatus();

  _previousIndex = -1;

  _isGroup: boolean;

  changed$ = new Subject();

  _highlighted = false;
  _draggedOver = false;

  isPhantom = false;
  isDirty = false;
  isDeleted = false;

  startStopInProgress: boolean;
  modifiedByDate: string;
  modifiedBy: string;

  constructor(config: ParserModel) {
    this.config = config;
  }

  setStatus(status: TopologyStatus) {
    this._status = status;
  }

  getStatus(): TopologyStatus {
    return this._status;
  }

  getConfig(): ParserModel {
    return this.config;
  }

  setProps(props) {

    if (typeof props.groupName !== 'undefined') {
      this.config.group = props.groupName;
    }
  }

  hasGroup(): boolean {
    return !!this.config.group;
  }

  getGroup(): string {
    return this.config.group;
  }

  getName(): string {
    return this.config.getName();
  }

  setName(name: string) {
    this.config.setName(name);
  }

  setIsGroup(value: boolean) {
    this._isGroup = value;
  }

  isGroup() {
    return this._isGroup;
  }

  setHighlighted(value: boolean) {
    this._highlighted = value;
  }

  getHighlighted(): boolean {
    return this._highlighted;
  }

  setDraggedOver(value: boolean) {
    this._draggedOver = value;
  }

  getDraggedOver(): boolean {
    return this._draggedOver;
  }

  isChanged(): Observable<any> {
    return this.changed$.asObservable();
  }

  _next() {
    this.changed$.next();
  }

  isStartable() {
    return this.isRootElement() &&
      this.getStatus().status === 'KILLED' && this.getStatus().status !== 'INACTIVE'
      && !this.startStopInProgress;
  }

  isStopable() {
    return this.isRootElement() &&
      this.getStatus().status === 'ACTIVE' && this.getStatus().status !== 'INACTIVE'
      && !this.startStopInProgress;
  }

  isRootElement() {
    return this.isGroup() || !this.hasGroup();
  }
}
