import { Action } from '@ngrx/store';
import { SensorParserConfigHistory } from 'app/model/sensor-parser-config-history';
import { SensorParserStatus } from '../model/sensor-parser-status';

export enum ParserConfigsActions {
  LoadParsersSuccess = '[Parser Configs List] Loading parsers success',
  LoadParserFailed = '[Parser Configs List] Loading parsers success',
  LoadParserStart = '[Parser Config List] Load parsers',

  LoadGroupsSuccess = '[Parser Configs List] Loading groups success',
  LoadGroupsFailed = '[Parser Configs List] Loading groups success',
  LoadGroupsStart = '[Parser Config List] Load groups',

  LoadStatusStart = '[Parser Config List] Load parsers status info',
  LoadStatusSuccess = '[Parser Config List] Load parsers status success',
  LoadStatusFailed = '[Parser Config List] Load parsers status failed',
}

export class ParserLoadingStart implements Action {
  readonly type = ParserConfigsActions.LoadParserStart;
}
export class ParserLoadingFailed implements Action {
  readonly type = ParserConfigsActions.LoadParserFailed;
}
export class ParserLoadingSuccess implements Action {
  readonly type = ParserConfigsActions.LoadParsersSuccess;
  readonly parserConfigs: SensorParserConfigHistory[];

  constructor(readonly payload: SensorParserConfigHistory[]) {
    this.parserConfigs = payload;
  }
}

export class GroupLoadingStart implements Action {
  readonly type = ParserConfigsActions.LoadStatusStart;
}
export class GroupLoadingFailed implements Action {
  readonly type = ParserConfigsActions.LoadStatusFailed;
}
export class GroupLoadingSuccess implements Action {
  readonly type = ParserConfigsActions.LoadStatusSuccess;
  readonly groupConfigs: SensorParserConfigHistory[];

  constructor(readonly payload: SensorParserConfigHistory[]) {
    this.groupConfigs = payload;
  }
}

export class StatusLoadingStart implements Action {
  readonly type = ParserConfigsActions.LoadStatusStart;
}
export class StatusLoadingFailed implements Action {
  readonly type = ParserConfigsActions.LoadStatusFailed;
}
export class StatusLoadingSuccess implements Action {
  readonly type = ParserConfigsActions.LoadStatusSuccess;
  readonly parserStatus: SensorParserStatus[];

  constructor(readonly payload: SensorParserStatus[]) {
    this.parserStatus = payload;
  }
}
