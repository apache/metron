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
import { ActionReducerMap } from '@ngrx/store';
import * as fromSensors from './sensors.reducers';

export * from './sensors.reducers';

export interface State {
  sensors: SensorState
}

export interface SensorState {
  parsers: fromSensors.ParserState;
  groups: fromSensors.GroupState;
  statuses: fromSensors.StatusState;
  layout: fromSensors.LayoutState
}

export const reducers: ActionReducerMap<SensorState> = {
  parsers: fromSensors.parserConfigsReducer,
  groups: fromSensors.groupConfigsReducer,
  statuses: fromSensors.parserStatusReducer,
  layout: fromSensors.layoutReducer
}
