/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Action, createSelector, createFeatureSelector } from '@ngrx/store';
import { TopologyStatus } from '../../model/topology-status';
import { ParserGroupModel } from '../models/parser-group.model';
import { ParserMetaInfoModel } from '../models/parser-meta-info.model';
import * as fromActions from '../actions';
import { State, SensorState } from './';
import { ParserConfigModel } from '../models/parser-config.model';

export interface ParserState {
  items: ParserMetaInfoModel[];
}

export interface GroupState {
  items: ParserMetaInfoModel[];
}

export interface StatusState {
  items: TopologyStatus[];
}

export interface DragNDropState {
  draggedId?: string,
  dropTargetId?: string,
  targetGroup?: string,
}

export interface LayoutState {
  order: string[],
  dnd: DragNDropState
}

export const initialParserState: ParserState = {
  items: []
}

export const initialGroupState: GroupState = {
  items: []
}

export const initialStatusState: StatusState = {
  items: []
}

export const initialLayoutState: LayoutState = {
  order: [],
  dnd: {
    draggedId: '',
    dropTargetId: '',
    targetGroup: ''
  }
}

export function parserConfigsReducer(state: ParserState = initialParserState, action: Action): ParserState {
  switch (action.type) {
    case fromActions.SensorsActionTypes.LoadSuccess:
      return {
        ...state,
        items: (action as fromActions.LoadSuccess).payload.parsers
      };

    case fromActions.SensorsActionTypes.AggregateParsers:
    case fromActions.SensorsActionTypes.AddToGroup: {
      const a = (action as fromActions.AggregateParsers);
      return {
        ...state,
        items: state.items.map(item => {
          if (a.payload.parserIds.includes(item.config.getName())) {
            if (item.config.group !== a.payload.groupName) {
              const config = (item.config as ParserConfigModel).clone();
              config.group = a.payload.groupName;
              return {
                ...item,
                isDirty: true,
                config,
              };
            }
          }
          return item;
        })
      };
    }

    case fromActions.SensorsActionTypes.MarkAsDeleted: {
      const a = (action as fromActions.MarkAsDeleted);
      return {
        ...state,
        items: state.items.map(item => {
          if (a.payload.parserIds.includes(item.config.getName())) {
            item = {
              ...item,
              isDeleted: true
            };
          }
          if (a.payload.parserIds.includes(item.config.group)) {
            if (item.config.group) {
              const config = (item.config as ParserConfigModel).clone();
              config.group = '';
              item = {
                ...item,
                isDirty: true,
                config,
              };
            }
          }
          return item;
        })
      }
    }

    case fromActions.SensorsActionTypes.StartSensor:
    case fromActions.SensorsActionTypes.StopSensor:
    case fromActions.SensorsActionTypes.EnableSensor:
    case fromActions.SensorsActionTypes.DisableSensor: {
      const a = action as fromActions.SensorControlAction;
      return {
        ...state,
        items: state.items.map((item) => {
          if (a.payload.parser.config.getName() === item.config.getName()) {
            return {
              ...item,
              startStopInProgress: true
            };
          }
          return item;
        })
      };
    }

    case fromActions.SensorsActionTypes.StartSensorSuccess:
    case fromActions.SensorsActionTypes.StartSensorFailure:
    case fromActions.SensorsActionTypes.StopSensorSuccess:
    case fromActions.SensorsActionTypes.StopSensorFailure:
    case fromActions.SensorsActionTypes.EnableSensorSuccess:
    case fromActions.SensorsActionTypes.EnableSensorFailure:
    case fromActions.SensorsActionTypes.DisableSensorSuccess:
    case fromActions.SensorsActionTypes.DisableSensorFailure: {
      const a = action as fromActions.SensorControlResponseAction;
      return {
        ...state,
        items: state.items.map((item) => {
          if (a.payload.parser.config.getName() === item.config.getName()) {
            return {
              ...item,
              startStopInProgress: false
            };
          }
          return item;
        })
      };
    }

    default:
      return state;
  }
}

export function groupConfigsReducer(state: GroupState = initialGroupState, action: Action): GroupState {
  switch (action.type) {
    case fromActions.SensorsActionTypes.LoadSuccess:
      return {
        ...state,
        items: (action as fromActions.LoadSuccess).payload.groups
      }
    case fromActions.SensorsActionTypes.CreateGroup: {
      const a = (action as fromActions.CreateGroup);
      const group = {
        config: new ParserGroupModel({ name: a.payload.name, description: a.payload.description }),
        isGroup: true,
        isPhantom: true,
      };
      return {
        ...state,
        items: [
          ...state.items,
          group
        ]
      }
    }
    case fromActions.SensorsActionTypes.UpdateGroupDescription: {
      const a = (action as fromActions.UpdateGroupDescription);
      return {
        ...state,
        items: state.items.map(item => {
          if (a.payload.name === item.config.getName()) {
            const config = (item.config as ParserGroupModel).clone(a.payload);
            config.setDescription(a.payload.description);
            return {
              ...item,
              config,
              isDirty: true
            }
          }
          return item;
        })
      }
    }
    case fromActions.SensorsActionTypes.MarkAsDeleted: {
      const a = (action as fromActions.MarkAsDeleted);
      return {
        ...state,
        items: state.items.map(item => {
          if (a.payload.parserIds.includes(item.config.getName())) {
            return {
              ...item,
              isDeleted: true
            };
          }
          return item;
        })
      }
    }
    case fromActions.SensorsActionTypes.StartSensor:
    case fromActions.SensorsActionTypes.StopSensor:
    case fromActions.SensorsActionTypes.EnableSensor:
    case fromActions.SensorsActionTypes.DisableSensor: {
      const a = action as fromActions.SensorControlAction;
      return {
        ...state,
        items: state.items.map((item) => {
          if (a.payload.parser.config.getName() === item.config.getName()) {
            return {
              ...item,
              startStopInProgress: true
            };
          }
          return item;
        })
      };
    }

    case fromActions.SensorsActionTypes.StartSensorSuccess:
    case fromActions.SensorsActionTypes.StartSensorFailure:
    case fromActions.SensorsActionTypes.StopSensorSuccess:
    case fromActions.SensorsActionTypes.StopSensorFailure:
    case fromActions.SensorsActionTypes.EnableSensorSuccess:
    case fromActions.SensorsActionTypes.EnableSensorFailure:
    case fromActions.SensorsActionTypes.DisableSensorSuccess:
    case fromActions.SensorsActionTypes.DisableSensorFailure: {
      const a = action as fromActions.SensorControlResponseAction;
      return {
        ...state,
        items: state.items.map((item) => {
          if (a.payload.parser.config.getName() === item.config.getName()) {
            return {
              ...item,
              startStopInProgress: false
            };
          }
          return item;
        })
      };
    }

    default:
      return state;
  }
}

export function parserStatusReducer(state: StatusState = initialStatusState, action: Action): StatusState {
  switch (action.type) {
    case fromActions.SensorsActionTypes.LoadSuccess:
    case fromActions.SensorsActionTypes.PollStatusSuccess: {
      return {
        ...state,
        items: (action as fromActions.LoadSuccess).payload.statuses
      }
    }

    default:
      return state;
  }
}

export function layoutReducer(state: LayoutState = initialLayoutState, action: Action): LayoutState {
  switch (action.type) {
    case fromActions.SensorsActionTypes.LoadSuccess: {
      const payload = (action as fromActions.LoadSuccess).payload;
      const groups: ParserMetaInfoModel[] = payload.groups;
      const parsers: ParserMetaInfoModel[] = payload.parsers;
      let order: string[] = [];
      groups.forEach((group) => {
        order = order.concat(group.config.getName());
        const configsForGroup = parsers
          .filter(parser => parser.config && parser.config.group === group.config.getName())
          .map(parser => parser.config.getName());
          order = order.concat(configsForGroup);
      });

      order = order.concat(
        parsers
          .filter(parser => !parser.config.group)
          .map(parser => parser.config.getName())
        );

      return {
        ...state,
        order
      };
    }

    case fromActions.SensorsActionTypes.SetDragged: {

      return {
        ...state,
        dnd: {
          ...state.dnd,
          draggedId: (action as fromActions.SetDragged).payload
        }
      };
    }

    case fromActions.SensorsActionTypes.SetDropTarget: {

      return {
        ...state,
        dnd: {
          ...state.dnd,
          dropTargetId: (action as fromActions.SetDropTarget).payload
        }
      };
    }

    case fromActions.SensorsActionTypes.SetTargetGroup: {

      return {
        ...state,
        dnd: {
          ...state.dnd,
          targetGroup: (action as fromActions.SetTargetGroup).payload
        }
      };
    }

    case fromActions.SensorsActionTypes.CreateGroup: {
      const a = (action as fromActions.CreateGroup);
      let placeholder = state;
      return {
        ...state,
        order: [
          ...state.order,
          a.payload.name
        ]
      };
    }

    case fromActions.SensorsActionTypes.AggregateParsers: {
      let order = state.order.slice(0);
      const a = (action as fromActions.AggregateParsers);
      const reference: string = a.payload.parserIds[0];
      const referenceIndex = order.indexOf(reference);
      const dragged: string = a.payload.parserIds[1];

      order = order.map(id => {
        if (id === a.payload.groupName || id === dragged) {
          return null;
        }
        return id;
      });
      order.splice(referenceIndex, 0, a.payload.groupName);
      order.splice(referenceIndex + 1, 0, dragged);

      order = order.filter(Boolean);

      return {
        ...state,
        order,
      }
    }

    case fromActions.SensorsActionTypes.InjectAfter: {
      let order = state.order.slice(0);
      const a = (action as fromActions.InjectAfter);
      const referenceIndex = order.indexOf(a.payload.reference);

      order = order.map(id => {
        if (id === a.payload.parserId) {
          return null;
        }
        return id;
      });

      order.splice(referenceIndex + 1, 0, a.payload.parserId);

      order = order.filter(Boolean);

      return {
        ...state,
        order
      };
    }

    case fromActions.SensorsActionTypes.InjectBefore: {
      let order = state.order.slice(0);
      const a = (action as fromActions.InjectBefore);
      const referenceIndex = order.indexOf(a.payload.reference);

      order = order.map(id => {
        if (id === a.payload.parserId) {
          return null;
        }
        return id;
      });

      order.splice(referenceIndex, 0, a.payload.parserId);

      order = order.filter(Boolean);

      return {
        ...state,
        order
      };
    }

    default:
      return state;
  }
}

/**
 * Selectors
 */

 export const getSensorsState = createFeatureSelector<State, SensorState>('sensors');

export const getGroups = createSelector(
  getSensorsState,
  (state: SensorState): ParserMetaInfoModel[] => {
    return state.groups.items;
  }
);

export const getParsers = createSelector(
  getSensorsState,
  (state: SensorState): ParserMetaInfoModel[] => {
    return state.parsers.items;
  }
);

export const getStatuses = createSelector(
  getSensorsState,
  (state: SensorState): TopologyStatus[] => {
    return state.statuses.items;
  }
);

export const getLayoutOrder = createSelector(
  getSensorsState,
  (state: SensorState): string[] => {
    return state.layout.order;
  }
);

export const getMergedConfigs = createSelector(
  getGroups,
  getParsers,
  getStatuses,
  getLayoutOrder,
  (
    groups: ParserMetaInfoModel[],
    parsers: ParserMetaInfoModel[],
    statuses: TopologyStatus[],
    order: string[]
  ): ParserMetaInfoModel[] => {
    let result: ParserMetaInfoModel[] = [];
    result = order.map((id: string) => {
      const group = groups.find(g => g.config.getName() === id);
      if (group) {
        return group;
      }
      const parserConfig = parsers.find(p => p.config.getName() === id);
      if (parserConfig) {
        return parserConfig;
      }
      return null;
    }).filter(Boolean);

    result = result.map((item) => {
      let status: TopologyStatus = statuses.find(stat => {
        return stat.name === item.config.getName();
      });
      return {
        ...item,
        status: status ? new TopologyStatus(status) : new TopologyStatus(),
      };
    });

    return result;
  }
);

export const isDirty = createSelector(
  getGroups,
  getParsers,
  (groups: ParserMetaInfoModel[], parsers: ParserMetaInfoModel[]): boolean => {
    const isChanged = (item) => item.isDeleted || item.isDirty || item.isPhantom;
    return groups.some(isChanged) || parsers.some(isChanged)
  }
);

