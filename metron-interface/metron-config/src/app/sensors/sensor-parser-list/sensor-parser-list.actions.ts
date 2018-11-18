import { Action } from '@ngrx/store';

export enum ParserConfigsListActions {
  Initialized = '[Parser Config List] Component Initialized',
}

export class ParserListInitialized implements Action {
  readonly type = ParserConfigsListActions.Initialized;
}
