import { Action, State } from '@ngrx/store';
import { ParserConfigsActions, ParserLoadingSuccess, StatusLoadingSuccess, GroupLoadingSuccess } from './parser-configs.actions';
import { SensorParserConfigHistory } from 'app/model/sensor-parser-config-history';
import { SensorParserStatus } from '../model/sensor-parser-status';
import { TopologyStatus } from '../model/topology-status';
import { ParserGroupModel } from '../model/parser-group';

export const initialParser: SensorParserConfigHistory[] = [];
export const initialGroup: ParserGroupModel[] = [];
export const initialStatus: TopologyStatus[] = [];

export interface ParserState {
  parserConfigs: SensorParserConfigHistory[]
}

export interface GroupState {
  groupConfigs: ParserGroupModel[]
}

export interface StatusState {
  parserStatus: TopologyStatus[]
}

const initialParserState: ParserState = {
  parserConfigs: []
}

const initialGroupState: GroupState = {
  groupConfigs: []
}

const initialStatusState: StatusState = {
  parserStatus: []
}

// export function parserReducer(parserState: ParserState = initialParserState, action: Action): ParserState {
//   switch (action.type) {
//     case ParserConfigsActions.LoadParsersSuccess:
//       return {
//         ...parserState,
//         parserConfigs: parserConfigsReducer(parserState.parserConfigs, action)
//       }
//     case ParserConfigsActions.LoadGroupsSuccess:
//       return {
//         ...parserState,
//         groupConfigs: groupConfigsReducer(parserState.groupConfigs, action)
//       }
//     case ParserConfigsActions.LoadStatusSuccess:
//       return {
//         ...parserState,
//         parserStatus: parserStatusReducer(parserState.parserStatus, action)
//       }
//     default:
//       return parserState;
//   }
// }

export function parserConfigsReducer(state: ParserState = initialParserState, action: Action): ParserState {
  switch (action.type) {
    case ParserConfigsActions.LoadParsersSuccess:
      return {
        parserConfigs: (action as ParserLoadingSuccess).payload
      };

    default:
      return state;
  }
}

export function groupConfigsReducer(state: GroupState = initialGroupState, action: Action): GroupState {
  switch (action.type) {
    case ParserConfigsActions.LoadGroupsSuccess:
      return {
        groupConfigs: (action as GroupLoadingSuccess).payload
      }

    default:
      return state;
  }
}

export function parserStatusReducer(state: StatusState = initialStatusState, action: Action): StatusState {
  switch (action.type) {
    case ParserConfigsActions.LoadStatusSuccess:
      return {
        parserStatus: (action as StatusLoadingSuccess).payload
      }

    default:
      return state;
  }
}

