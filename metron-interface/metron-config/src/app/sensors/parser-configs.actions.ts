import { Action } from '@ngrx/store';
import { SensorParserConfigHistory } from 'app/model/sensor-parser-config-history';
import { SensorParserStatus } from '../model/sensor-parser-status';
import { ParserGroupModel } from '../model/parser-group';
import { TopologyStatus } from '../model/topology-status';

export enum ParserConfigsActions {
  LoadStart = '[Parser Configs List] Loading parsers start',
  LoadSuccess = '[Parser Configs List] Loading parsers success',
  StartPolling = '[Parser Configs List] Start polling',
  PollStatusSuccess = '[Parser Configs List] Poll status success',
}

export class LoadStart implements Action {
  readonly type = ParserConfigsActions.LoadStart;
}

export interface LoadSuccesActionPayload {
  parsers: SensorParserConfigHistory[],
  groups: ParserGroupModel[],
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
