import { Action } from '@ngrx/store';
import { ParserConfigsActions, ParserLoadingSuccess, StatusLoadingSuccess, GroupLoadingSuccess } from './parser-configs.actions';
import { SensorParserConfigHistory } from 'app/model/sensor-parser-config-history';
import { SensorParserStatus } from '../model/sensor-parser-status';
import { TopologyStatus } from '../model/topology-status';
import { ParserGroupModel } from '../model/parser-group';

export const initialParser: SensorParserConfigHistory[] = [];
export const initialGroup: ParserGroupModel[] = [];
export const initialStatus: TopologyStatus[] = [];

export function parserConfigsReducer(state: SensorParserConfigHistory[] = initialParser, action: Action) {
  switch (action.type) {
    case ParserConfigsActions.LoadParsersSuccess:
      return (action as ParserLoadingSuccess).payload;

    default:
      return state;
  }
}

export function groupConfigsReducer(state: ParserGroupModel[] = initialGroup, action: Action) {
  switch (action.type) {
    case ParserConfigsActions.LoadGroupsSuccess:
      return (action as GroupLoadingSuccess).payload;

    default:
      return state;
  }
}

export function parserStatusReducer(state: TopologyStatus[] = initialStatus, action: Action) {
  switch (action.type) {
    case ParserConfigsActions.LoadStatusSuccess:
      return (action as StatusLoadingSuccess).payload;

    default:
      return state;
  }
}

