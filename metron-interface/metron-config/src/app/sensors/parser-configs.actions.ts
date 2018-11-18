import { Action } from '@ngrx/store';
import { SensorParserConfigHistory } from 'app/model/sensor-parser-config-history';

export enum ParserConfigsActions {
  LoadParsersSuccess = '[Parser Configs List] Loading parsers success',
  LoadParserFailed = '[Parser Configs List] Loading parsers success',
  LoadParserStart = '[Parser Config List] Load parsers',
}

export class ParserLoadingStart implements Action {
  readonly type = ParserConfigsActions.LoadParserStart;
}

export class ParserLoadingSuccess implements Action {
  readonly type = ParserConfigsActions.LoadParsersSuccess;
  readonly parserConfigs: SensorParserConfigHistory[];

  constructor(readonly payload: SensorParserConfigHistory[]) {
    this.parserConfigs = payload;
  }
}

export class ParserLoadingFailed implements Action {
  readonly type = ParserConfigsActions.LoadParserFailed;
}
