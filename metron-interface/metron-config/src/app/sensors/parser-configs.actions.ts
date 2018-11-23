import { Action } from '@ngrx/store';
import { SensorParserConfigHistory } from 'app/model/sensor-parser-config-history';
import { SensorParserStatus } from '../model/sensor-parser-status';
import { ParserGroupModel } from './models/parser-group.model';
import { TopologyStatus } from '../model/topology-status';
import { ParserMetaInfoModel } from './models/parser-meta-info.model';

export enum ParserConfigsActions {
  LoadStart = '[Parser Configs List] Loading parsers start',
  LoadSuccess = '[Parser Configs List] Loading parsers success',
  StartPolling = '[Parser Configs List] Start polling',
  PollStatusSuccess = '[Parser Configs List] Poll status success',
  AggregateParsers = '[Parser Configs List] Aggregate parsers',
  CreateGroup = '[Parser Configs List] Create group',
  AddToGroup = '[Parser Configs List] Add to group',
  InjectBefore = '[Parser Configs List] Inject before',
  InjectAfter = '[Parser Configs List] Inject after',
  SetHighlighted = '[Parser Configs List] Set highlighted',
  SetDraggedOver = '[Parser Configs List] Set dragged over',
  SetAllHighlighted = '[Parser Configs List] Set all highlighted',
  SetAllDraggedOver = '[Parser Configs List] Set all dragged over',
  MarkAsDeleted = '[Parser Configs List] Mark as deleted',
}

export class LoadStart implements Action {
  readonly type = ParserConfigsActions.LoadStart;
}

export interface LoadSuccesActionPayload {
  parsers: ParserMetaInfoModel[],
  groups: ParserMetaInfoModel[],
  statuses: TopologyStatus[],
}

export class LoadSuccess implements Action {
  readonly type = ParserConfigsActions.LoadSuccess;
  constructor(readonly payload: LoadSuccesActionPayload) {}
}

export class StartPolling implements Action {
  readonly type = ParserConfigsActions.StartPolling;
}

export class PollStatusSuccess implements Action {
  readonly type = ParserConfigsActions.PollStatusSuccess;
  constructor(readonly payload: { statuses: TopologyStatus[] }) {}
}

export class AggregateParsers implements Action {
  readonly type = ParserConfigsActions.AggregateParsers;
  constructor(readonly payload: {
    groupName: string,
    parserIds: string[],
  }) {}
}

export class CreateGroup implements Action {
  readonly type = ParserConfigsActions.CreateGroup;
  constructor(readonly payload: string) {}
}

export class AddToGroup implements Action {
  readonly type = ParserConfigsActions.AddToGroup;
  constructor(readonly payload: {
    groupName: string,
    parserIds: string[],
  }) {}
}

export class InjectBefore implements Action {
  readonly type = ParserConfigsActions.InjectBefore;
  constructor(readonly payload: {
    reference: string,
    parserId: string,
  }) {}
}

export class InjectAfter implements Action {
  readonly type = ParserConfigsActions.InjectAfter;
  constructor(readonly payload: {
    reference: string,
    parserId: string,
  }) {}
}

export class SetHighlighted implements Action {
  readonly type = ParserConfigsActions.SetHighlighted;
  constructor(readonly payload: {
    value: boolean,
    id: string,
  }) {}
}

export class SetDraggedOver implements Action {
  readonly type = ParserConfigsActions.SetDraggedOver;
  constructor(readonly payload: {
    value: boolean,
    id: string,
  }) {}
}

export class SetAllHighlighted implements Action {
  readonly type = ParserConfigsActions.SetAllHighlighted;
  constructor(readonly payload: boolean) {}
}

export class SetAllDraggedOver implements Action {
  readonly type = ParserConfigsActions.SetAllDraggedOver;
  constructor(readonly payload: boolean) {}
}

export class MarkAsDeleted implements Action {
  readonly type = ParserConfigsActions.MarkAsDeleted;
  constructor(readonly payload: {
    parserIds: string[]
  }) {}
}
