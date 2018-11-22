import { Action } from '@ngrx/store';

export enum DragAndDropActionTypes {
  SetDragged = '[Drag and Drop] Set dragged element',
  SetDropTarget = '[Drag and Drop] Set drop target',
  SetTargetGroup = '[Drag and Drop] Set target group'
}

export class SetDragged implements Action {
  readonly type = DragAndDropActionTypes.SetDragged;
  constructor(readonly payload: string) {}
}

export class SetDropTarget implements Action {
  readonly type = DragAndDropActionTypes.SetDropTarget;
  constructor(readonly payload: string) {}
}

export class SetTargetGroup implements Action {
  readonly type = DragAndDropActionTypes.SetTargetGroup;
  constructor(readonly payload: string) {}
}
