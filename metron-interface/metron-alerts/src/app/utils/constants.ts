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
export const NUM_SAVED_SEARCH = 10;
export const ALERTS_RECENT_SEARCH = 'metron-alerts-recent-saved-search';
export const ALERTS_SAVED_SEARCH = 'metron-alerts-saved-search';
export const ALERTS_TABLE_METADATA = 'metron-alerts-table-metadata';
export const ALERTS_COLUMN_NAMES = 'metron-alerts-column-names';

export let TREE_SUB_GROUP_SIZE = 5;
export let DEFAULT_FACETS = ['source:type', 'ip_src_addr', 'ip_dst_addr', 'host', 'enrichments:geo:ip_dst_addr:country'];
export let DEFAULT_GROUPS = ['source:type', 'ip_src_addr', 'ip_dst_addr', 'host', 'enrichments:geo:ip_dst_addr:country'];
export let INDEXES = ['websphere', 'snort', 'asa', 'bro', 'yaf'];
