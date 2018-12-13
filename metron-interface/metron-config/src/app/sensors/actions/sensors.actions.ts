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
import { Action } from '@ngrx/store';
import { TopologyStatus } from '../../model/topology-status';
import { ParserMetaInfoModel } from '../models/parser-meta-info.model';
import { ParserConfigModel } from '../models/parser-config.model';

export enum SensorsActionTypes {
  LoadStart = '[Sensors] Load sensors',
  LoadSuccess = '[Sensors] Load sensors success',
  UpdateParserConfig = '[Sensors] update parser config',
  AddParserConfig = '[Sensors] Add parser config',
  StartPolling = '[Sensors] Start polling topology statuses',
  PollStatusSuccess = '[Sensors] Poll topology statuses success',
  AggregateParsers = '[Sensors] Aggregate parser configs',
  CreateGroup = '[Sensors] Create a group',
  AddToGroup = '[Sensors] Add to a group',
  InjectBefore = '[Sensors] Inject before',
  InjectAfter = '[Sensors] Inject after',
  MarkAsDeleted = '[Sensors] Mark as deleted',
  ApplyChanges = '[Sensors] Apply changes',
  ApplyChangesSuccess = '[Sensors] Apply changes success',
  SetDragged = '[Sensors] Set dragged element',
  SetDropTarget = '[Sensors] Set drop target',
  SetTargetGroup = '[Sensors] Set target group',
  StartSensor = '[Sensors] Start sensor',
  StartSensorSuccess = '[Sensors] Start sensor success',
  StartSensorFailure = '[Sensors] Start sensor failure',
  StopSensor = '[Sensors] Stop sensor',
  StopSensorSuccess = '[Sensors] Stop sensor success',
  StopSensorFailure = '[Sensors] Stop sensor failure',
  EnableSensor = '[Sensors] Enable sensor',
  EnableSensorSuccess = '[Sensors] Enable sensor success',
  EnableSensorFailure = '[Sensors] Enable sensor failure',
  DisableSensor = '[Sensors] Disable sensor',
  DisableSensorSuccess = '[Sensors] Disable sensor success',
  DisableSensorFailure = '[Sensors] Disable sensor failure',
  UpdateGroupDescription = '[Sensors] Update group description',
}

export class LoadStart implements Action {
  readonly type = SensorsActionTypes.LoadStart;
}

export interface LoadSuccesActionPayload {
  parsers?: ParserMetaInfoModel[],
  groups?: ParserMetaInfoModel[],
  statuses?: TopologyStatus[],
}

export class LoadSuccess implements Action {
  readonly type = SensorsActionTypes.LoadSuccess;
  constructor(readonly payload: LoadSuccesActionPayload) {}
}

export class UpdateParserConfig implements Action {
  readonly type = SensorsActionTypes.UpdateParserConfig;
  constructor(readonly payload: ParserConfigModel) {}
}

export class AddParserConfig implements Action {
  readonly type = SensorsActionTypes.AddParserConfig;
  constructor(readonly payload: ParserConfigModel) {}
}

export class StartPolling implements Action {
  readonly type = SensorsActionTypes.StartPolling;
}

export class PollStatusSuccess implements Action {
  readonly type = SensorsActionTypes.PollStatusSuccess;
  constructor(readonly payload: { statuses: TopologyStatus[] }) {}
}

export class AggregateParsers implements Action {
  readonly type = SensorsActionTypes.AggregateParsers;
  constructor(readonly payload: {
    groupName: string,
    parserIds: string[],
  }) {}
}

export class CreateGroup implements Action {
  readonly type = SensorsActionTypes.CreateGroup;
  constructor(readonly payload: {
    name: string,
    description: string,
  }) {}
}

export class UpdateGroupDescription implements Action {
  readonly type = SensorsActionTypes.UpdateGroupDescription;
  constructor(readonly payload: {
    name: string,
    description: string,
  }) {}
}

export class AddToGroup implements Action {
  readonly type = SensorsActionTypes.AddToGroup;
  constructor(readonly payload: {
    groupName: string,
    parserIds: string[],
  }) {}
}

export class InjectBefore implements Action {
  readonly type = SensorsActionTypes.InjectBefore;
  constructor(readonly payload: {
    reference: string,
    parserId: string,
  }) {}
}

export class InjectAfter implements Action {
  readonly type = SensorsActionTypes.InjectAfter;
  constructor(readonly payload: {
    reference: string,
    parserId: string,
  }) {}
}

export class MarkAsDeleted implements Action {
  readonly type = SensorsActionTypes.MarkAsDeleted;
  constructor(readonly payload: {
    parserIds: string[]
  }) {}
}

export class ApplyChanges implements Action {
  readonly type = SensorsActionTypes.ApplyChanges;
}

export class ApplyChangesSuccess implements Action {
  readonly type = SensorsActionTypes.ApplyChangesSuccess;
}

export class SetDragged implements Action {
  readonly type = SensorsActionTypes.SetDragged;
  constructor(readonly payload: string) {}
}

export class SetDropTarget implements Action {
  readonly type = SensorsActionTypes.SetDropTarget;
  constructor(readonly payload: string) {}
}

export class SetTargetGroup implements Action {
  readonly type = SensorsActionTypes.SetTargetGroup;
  constructor(readonly payload: string) {}
}

export class StartSensor implements Action {
  readonly type = SensorsActionTypes.StartSensor;
  constructor(readonly payload: { parser: ParserMetaInfoModel }) {}
}

export class StartSensorSuccess implements Action {
  readonly type = SensorsActionTypes.StartSensorSuccess;
  constructor(readonly payload: {
    status: string,
    parser: ParserMetaInfoModel,
  }) {}
}

export class StartSensorFailure implements Action {
  readonly type = SensorsActionTypes.StartSensorFailure;
  constructor(readonly payload: {
    status: string,
    parser: ParserMetaInfoModel,
  }) {}
}

export class StopSensor implements Action {
  readonly type = SensorsActionTypes.StopSensor;
  constructor(readonly payload: { parser: ParserMetaInfoModel }) {}
}

export class StopSensorSuccess implements Action {
  readonly type = SensorsActionTypes.StopSensorSuccess;
  constructor(readonly payload: {
    status: string,
    parser: ParserMetaInfoModel,
  }) {}
}

export class StopSensorFailure implements Action {
  readonly type = SensorsActionTypes.StopSensorFailure;
  constructor(readonly payload: {
    status: string,
    parser: ParserMetaInfoModel,
  }) {}
}

export class EnableSensor implements Action {
  readonly type = SensorsActionTypes.EnableSensor;
  constructor(readonly payload: { parser: ParserMetaInfoModel }) {}
}

export class EnableSensorSuccess implements Action {
  readonly type = SensorsActionTypes.EnableSensorSuccess;
  constructor(readonly payload: {
    status: string,
    parser: ParserMetaInfoModel,
  }) {}
}

export class EnableSensorFailure implements Action {
  readonly type = SensorsActionTypes.EnableSensorFailure;
  constructor(readonly payload: {
    status: string,
    parser: ParserMetaInfoModel,
  }) {}
}

export class DisableSensor implements Action {
  readonly type = SensorsActionTypes.DisableSensor;
  constructor(readonly payload: { parser: ParserMetaInfoModel }) {}
}

export class DisableSensorSuccess implements Action {
  readonly type = SensorsActionTypes.DisableSensorSuccess;
  constructor(readonly payload: {
    status: string,
    parser: ParserMetaInfoModel,
  }) {}
}

export class DisableSensorFailure implements Action {
  readonly type = SensorsActionTypes.DisableSensorFailure;
  constructor(readonly payload: {
    status: string,
    parser: ParserMetaInfoModel,
  }) {}
}

export type SensorControlAction = StartSensor | StopSensor | EnableSensor | DisableSensor;
export type SensorControlResponseAction = StartSensorSuccess
  | StartSensorFailure
  | StopSensorSuccess
  | StopSensorFailure
  | EnableSensorSuccess
  | EnableSensorFailure
  | DisableSensorSuccess
  | DisableSensorFailure;
