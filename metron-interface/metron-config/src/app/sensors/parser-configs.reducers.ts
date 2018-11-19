import { Action, State } from '@ngrx/store';
import { ParserConfigsActions, ParserLoadingSuccess, StatusLoadingSuccess, GroupLoadingSuccess } from './parser-configs.actions';
import { SensorParserConfigHistory } from 'app/model/sensor-parser-config-history';
import { SensorParserStatus } from '../model/sensor-parser-status';
import { TopologyStatus } from '../model/topology-status';
import { ParserGroupModel } from '../model/parser-group';
import { ParserState, AppState } from 'app/app.state';

export const initialParser: SensorParserConfigHistory[] = [];
export const initialGroup: ParserGroupModel[] = [];
export const initialStatus: TopologyStatus[] = [];

const initialParserState: ParserState = {
  parserConfigs: [],
  groupConfigs: [],
  parserStatus: []
}

export function parserReducer(parserState: ParserState = initialParserState, action: Action): ParserState {
  switch (action.type) {
    case ParserConfigsActions.LoadParsersSuccess:
      return {
        ...parserState,
        parserConfigs: parserConfigsReducer(parserState.parserConfigs, action)
      }
    case ParserConfigsActions.LoadGroupsSuccess:
      return {
        ...parserState,
        groupConfigs: groupConfigsReducer(parserState.groupConfigs, action)
      }
    case ParserConfigsActions.LoadStatusSuccess:
      return {
        ...parserState,
        parserStatus: parserStatusReducer(parserState.parserStatus, action)
      }
    default:
      return parserState;
  }
}

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

