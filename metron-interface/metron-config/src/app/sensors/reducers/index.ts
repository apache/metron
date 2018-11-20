import { ActionReducerMap, Action } from '@ngrx/store';
import {
  parserConfigsReducer,
  groupConfigsReducer,
  parserStatusReducer,
  ParserState,
  GroupState,
  StatusState
} from '../parser-configs.reducers';
import { ParserConfigsActions } from '../parser-configs.actions';


export interface SensorState {
  parsers: ParserState;
  groups: GroupState;
  statuses: StatusState;
}

export const reducers: ActionReducerMap<SensorState, any> = {
  parsers: parserConfigsReducer,
  groups: groupConfigsReducer,
  statuses: parserStatusReducer,
}
