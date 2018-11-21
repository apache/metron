import { Action, State } from '@ngrx/store';
import { SensorParserConfigHistory } from 'app/model/sensor-parser-config-history';
import { SensorParserStatus } from '../model/sensor-parser-status';
import { TopologyStatus } from '../model/topology-status';
import { ParserGroupModel } from './models/parser-group.model';
import * as ParsersActions from './parser-configs.actions';
import { ParserMetaInfoModel } from './models/parser-meta-info.model';

export const initialParser: SensorParserConfigHistory[] = [];
export const initialGroup: ParserGroupModel[] = [];
export const initialStatus: TopologyStatus[] = [];


export interface ParserState {
  items: ParserMetaInfoModel[];
}

export interface GroupState {
  items: ParserMetaInfoModel[];
}

export interface StatusState {
  items: TopologyStatus[];
}

const initialParserState: ParserState = {
  items: []
}

const initialGroupState: GroupState = {
  items: []
}

const initialStatusState: StatusState = {
  items: []
}

export function parserConfigsReducer(state: ParserState = initialParserState, action: Action): ParserState {
  switch (action.type) {
    case ParsersActions.ParserConfigsActions.LoadSuccess:
      return {
        ...state,
        items: (action as ParsersActions.LoadSuccess).payload.parsers
      };

    default:
      return state;
  }
}

export function groupConfigsReducer(state: GroupState = initialGroupState, action: Action): GroupState {
  switch (action.type) {
    case ParsersActions.ParserConfigsActions.LoadSuccess:
      return {
        ...state,
        items: (action as ParsersActions.LoadSuccess).payload.groups
      }

    default:
      return state;
  }
}

export function parserStatusReducer(state: StatusState = initialStatusState, action: Action): StatusState {
  switch (action.type) {
    case ParsersActions.ParserConfigsActions.LoadSuccess:
    case ParsersActions.ParserConfigsActions.PollStatusSuccess:
      return {
        ...state,
        items: (action as ParsersActions.LoadSuccess).payload.statuses
      }

    default:
      return state;
  }
}

