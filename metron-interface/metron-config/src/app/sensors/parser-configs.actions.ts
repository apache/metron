import { Action } from '@ngrx/store';
import { SensorParserConfigHistory } from 'app/model/sensor-parser-config-history';

export enum ParserConfigsActions {
  LoadParsersSuccess = '[Parser Configs] Loading parsers success',
  LoadParserFailed = '[Parser Configs] Loading parsers success',
}

export class ParserLoadSuccess implements Action {
  readonly type = ParserConfigsActions.LoadParsersSuccess;
  readonly parserConfigs: SensorParserConfigHistory[];

  constructor(readonly payload: SensorParserConfigHistory[]) {
    this.parserConfigs = payload;
  }
}

export class ParserLoadFailed implements Action {
  readonly type = ParserConfigsActions.LoadParserFailed;
}
