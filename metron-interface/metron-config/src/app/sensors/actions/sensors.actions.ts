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

export enum SensorsActionTypes {
  LoadStart = '[Sensors] Load sensors',
  LoadSuccess = '[Sensors] Load sensors success',
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
  SetTargetGroup = '[Sensors] Set target group'
}

export class LoadStart implements Action {
  readonly type = SensorsActionTypes.LoadStart;
}

export interface LoadSuccesActionPayload {
  parsers: ParserMetaInfoModel[],
  groups: ParserMetaInfoModel[],
  statuses: TopologyStatus[],
}

export class LoadSuccess implements Action {
  readonly type = SensorsActionTypes.LoadSuccess;
  constructor(readonly payload: LoadSuccesActionPayload) {}
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
  constructor(readonly payload: string) {}
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
