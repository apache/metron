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


interface SensorReducers {
  parsers: ParserState;
  groups: GroupState;
  status: StatusState;
}

export const reducers: ActionReducerMap<SensorReducers, any> = {
  parsers: parserConfigsReducer,
  groups: groupConfigsReducer,
  status: parserStatusReducer,
}
