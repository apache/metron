import { Action } from '@ngrx/store';
import { ParserConfigsActions, ParserLoadingSuccess, StatusLoadingSuccess, GroupLoadingSuccess } from './parser-configs.actions';
import { SensorParserConfigHistory } from 'app/model/sensor-parser-config-history';
import { SensorParserStatus } from '../model/sensor-parser-status';

export const initialParser: SensorParserConfigHistory[] = [];
export const initialGroup: SensorParserConfigHistory[] = [];
export const initialStatus: SensorParserStatus[] = [];

export function parserConfigsReducer(state: SensorParserConfigHistory[] = initialParser, action: Action) {
  switch (action.type) {
    case ParserConfigsActions.LoadParsersSuccess:
      return (action as ParserLoadingSuccess).payload;

    case ParserConfigsActions.LoadParserFailed:
      return [];

    default:
      return initialParser;
  }
}

export function groupConfigsReducer(state: SensorParserConfigHistory[] = initialGroup, action: Action) {
  switch (action.type) {
    case ParserConfigsActions.LoadGroupsSuccess:
      return (action as GroupLoadingSuccess).payload;

    default:
      return initialGroup;
  }
}

export function parserStatusReducer(state: SensorParserStatus[] = initialStatus, action: Action) {
  switch (action.type) {
    case ParserConfigsActions.LoadStatusSuccess:
      return (action as StatusLoadingSuccess).payload;

    default:
      return initialStatus;
  }
}

