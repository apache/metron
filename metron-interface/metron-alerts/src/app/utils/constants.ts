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

import { environment } from '../../environments/environment';

export const META_ALERTS_SENSOR_TYPE = 'metaalert';

export const NUM_SAVED_SEARCH = 10;
export const ALERTS_RECENT_SEARCH = 'metron-alerts-recent-saved-search';
export const ALERTS_SAVED_SEARCH = 'metron-alerts-saved-search';
export const ALERTS_TABLE_METADATA = 'metron-alerts-table-metadata';
export const ALERTS_COLUMN_NAMES = 'metron-alerts-column-names';

export let TIMESTAMP_FIELD_NAME = 'timestamp';
export let ALL_TIME = 'all-time';

export let DEFAULT_TIMESTAMP_FORMAT = 'YYYY-MM-DD HH:mm:ss';
export let CUSTOMM_DATE_RANGE_LABEL = 'Date Range';

export let TREE_SUB_GROUP_SIZE = 5;
export let INDEXES =  environment.indices ? environment.indices.split(',') : [];
export let POLLING_DEFAULT_STATE = !environment.defaultPollingState;

export let MAX_ALERTS_IN_META_ALERTS = 350;

export const DEFAULT_END_TIME = new Date();
export const DEFAULT_START_TIME = new Date().setDate(DEFAULT_END_TIME.getDate() - 5);
