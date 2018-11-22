import { ActionReducerMap, Action } from '@ngrx/store';
import {
  parserConfigsReducer,
  groupConfigsReducer,
  parserStatusReducer,
  layoutReducer,
  ParserState,
  GroupState,
  StatusState,
  LayoutState,
} from '../parser-configs.reducers';
import { ParserConfigsActions } from '../parser-configs.actions';


export interface SensorState {
  parsers: ParserState;
  groups: GroupState;
  statuses: StatusState;
  layout: LayoutState
}

export const reducers: ActionReducerMap<SensorState, any> = {
  parsers: parserConfigsReducer,
  groups: groupConfigsReducer,
  statuses: parserStatusReducer,
  layout: layoutReducer
}
