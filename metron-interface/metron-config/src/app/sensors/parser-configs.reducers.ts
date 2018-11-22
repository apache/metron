import { Action, State } from '@ngrx/store';
import { SensorParserConfigHistory } from 'app/model/sensor-parser-config-history';
import { SensorParserStatus } from '../model/sensor-parser-status';
import { TopologyStatus } from '../model/topology-status';
import { ParserGroupModel } from './models/parser-group.model';
import * as ParsersActions from './parser-configs.actions';
import { ParserMetaInfoModel } from './models/parser-meta-info.model';
import * as DragAndDropActions from './parser-configs-dnd.actions';
import { ParserConfigModel } from './models/parser-config.model';

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

interface DragNDropState {
  draggedId: string,
  dropTargetId: string,
  targetGroup: string,
}

export interface LayoutState {
  order: string[],
  dnd: DragNDropState
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

const initialLayoutState: LayoutState = {
  order: [],
  dnd: {
    draggedId: '',
    dropTargetId: '',
    targetGroup: ''
  }
}

export function parserConfigsReducer(state: ParserState = initialParserState, action: Action): ParserState {
  switch (action.type) {
    case ParsersActions.ParserConfigsActions.LoadSuccess:
      return {
        ...state,
        items: (action as ParsersActions.LoadSuccess).payload.parsers
      };

    case ParsersActions.ParserConfigsActions.AggregateParsers:
    case ParsersActions.ParserConfigsActions.AddToGroup: {
      const a = (action as ParsersActions.AggregateParsers);
      return {
        ...state,
        items: state.items.map(item => {
          if (a.payload.parserIds.includes(item.getName())) {
            item.getConfig().group = a.payload.groupName;
          }
          return item;
        })
      };
    }

    case ParsersActions.ParserConfigsActions.SetHighlighted: {
      const a = (action as ParsersActions.SetHighlighted);
      return {
        ...state,
        items: state.items.map(item => {
          if (a.payload.id === item.getName()) {
            item.setHighlighted(a.payload.value);
          }
          return item;
        })
      };
    }

    case ParsersActions.ParserConfigsActions.SetAllHighlighted: {
      const a = (action as ParsersActions.SetAllHighlighted);
      return {
        ...state,
        items: state.items.map(item => {
          item.setHighlighted(a.payload);
          return item;
        })
      };
    }

    case ParsersActions.ParserConfigsActions.SetDraggedOver: {
      const a = (action as ParsersActions.SetDraggedOver);
      return {
        ...state,
        items: state.items.map(item => {
          if (a.payload.id === item.getName()) {
            item.setDraggedOver(a.payload.value);
          }
          return item;
        })
      };
    }

    case ParsersActions.ParserConfigsActions.SetAllDraggedOver: {
      const a = (action as ParsersActions.SetAllDraggedOver);
      return {
        ...state,
        items: state.items.map(item => {
          item.setDraggedOver(a.payload);
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
    case ParsersActions.ParserConfigsActions.LoadSuccess:
      return {
        ...state,
        items: (action as ParsersActions.LoadSuccess).payload.groups
      }
    case ParsersActions.ParserConfigsActions.CreateGroup: {
      const a = (action as ParsersActions.CreateGroup);
      const group = new ParserMetaInfoModel(new ParserGroupModel({ name: a.payload }));
      group.setIsGroup(true);
      return {
        ...state,
        items: [
          ...state.items,
          group
        ]
      }
    }
    case ParsersActions.ParserConfigsActions.SetHighlighted: {
      const a = (action as ParsersActions.SetHighlighted);
      return {
        ...state,
        items: state.items.map(item => {
          if (a.payload.id === item.getName()) {
            item.setHighlighted(a.payload.value);
          }
          return item;
        })
      };
    }

    case ParsersActions.ParserConfigsActions.SetAllHighlighted: {
      const a = (action as ParsersActions.SetAllHighlighted);
      return {
        ...state,
        items: state.items.map(item => {
          item.setHighlighted(a.payload);
          return item;
        })
      };
    }

    case ParsersActions.ParserConfigsActions.SetDraggedOver: {
      const a = (action as ParsersActions.SetDraggedOver);
      return {
        ...state,
        items: state.items.map(item => {
          if (a.payload.id === item.getName()) {
            item.setDraggedOver(a.payload.value);
          }
          return item;
        })
      };
    }

    case ParsersActions.ParserConfigsActions.SetAllDraggedOver: {
      const a = (action as ParsersActions.SetAllDraggedOver);
      return {
        ...state,
        items: state.items.map(item => {
          item.setDraggedOver(a.payload);
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

export function layoutReducer(state: LayoutState = initialLayoutState, action: Action): LayoutState {
  switch (action.type) {
    case ParsersActions.ParserConfigsActions.LoadSuccess: {
      const payload = (action as ParsersActions.LoadSuccess).payload;
      const groups: ParserMetaInfoModel[] = payload.groups;
      const parsers: ParserMetaInfoModel[] = payload.parsers;
      let order: string[] = [];
      groups.forEach((group, i) => {
        order = order.concat(group.getName());
        const configsForGroup = parsers
          .filter(parser => parser.getConfig() && parser.getConfig().group === group.getName())
          .map(parser => parser.getName());
          order = order.concat(configsForGroup);
      });

      order = order.concat(
        parsers
          .filter(parser => !parser.getConfig().group)
          .map(parser => parser.getName())
        );

      return {
        ...state,
        order
      };
    }

    case DragAndDropActions.DragAndDropActionTypes.SetDragged: {

      return {
        ...state,
        dnd: {
          ...state.dnd,
          draggedId: (action as DragAndDropActions.SetDragged).payload
        }
      };
    }

    case DragAndDropActions.DragAndDropActionTypes.SetDropTarget: {

      return {
        ...state,
        dnd: {
          ...state.dnd,
          dropTargetId: (action as DragAndDropActions.SetDropTarget).payload
        }
      };
    }

    case DragAndDropActions.DragAndDropActionTypes.SetTargetGroup: {

      return {
        ...state,
        dnd: {
          ...state.dnd,
          targetGroup: (action as DragAndDropActions.SetTargetGroup).payload
        }
      };
    }

    case ParsersActions.ParserConfigsActions.CreateGroup: {
      const a = (action as ParsersActions.CreateGroup);
      return {
        ...state,
        order: [
          ...state.order,
          a.payload
        ]
      };
    }

    case ParsersActions.ParserConfigsActions.AggregateParsers: {
      let order = state.order.slice(0);
      const a = (action as ParsersActions.AggregateParsers);
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

    case ParsersActions.ParserConfigsActions.InjectAfter: {
      let order = state.order.slice(0);
      const a = (action as ParsersActions.InjectAfter);
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

    case ParsersActions.ParserConfigsActions.InjectBefore: {
      let order = state.order.slice(0);
      const a = (action as ParsersActions.InjectBefore);
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

