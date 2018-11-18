import { Action } from '@ngrx/store';
import { ParserConfigsActions, ParserLoadingSuccess } from './parser-configs.actions';
import { SensorParserConfigHistory } from 'app/model/sensor-parser-config-history';

export const initialState: SensorParserConfigHistory[] = [];

export function parserConfigsReducer(state: SensorParserConfigHistory[] = initialState, action: Action) {
  switch (action.type) {
    case ParserConfigsActions.LoadParsersSuccess:
      return (action as ParserLoadingSuccess).payload;

    case ParserConfigsActions.LoadParserFailed:
      return [];

    default:
      return state;
  }
}
