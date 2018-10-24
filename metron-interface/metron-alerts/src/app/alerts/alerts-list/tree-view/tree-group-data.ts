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
import {Subscription} from 'rxjs';
import {SearchResponse} from '../../../model/search-response';
import {Pagination} from '../../../model/pagination';
import {TREE_SUB_GROUP_SIZE} from '../../../utils/constants';
import {SortField} from '../../../model/sort-field';
import {SortEvent} from '../../../shared/metron-table/metron-table.directive';
import {Sort} from '../../../utils/enums';

export class TreeGroupData {
  key: string;
  total: number;
  level: number;
  show: boolean;
  expand = false;
  score: number;
  isLeafNode = false;

  // Used by only Dashrow
  sortField: SortField;
  sortEvent: SortEvent = { sortBy: '', type: '', sortOrder: Sort.ASC};
  treeSubGroups: TreeGroupData[] = [];

  // Used by only Leafnodes
  groupQueryMap = null;
  response: SearchResponse = new SearchResponse();
  pagingData: Pagination = new Pagination();


  constructor(key: string, total: number, score: number, level: number, expand: boolean) {
    this.key = key;
    this.total = total;
    this.score = score;
    this.level = level;
    this.show = expand;

    this.pagingData.size = TREE_SUB_GROUP_SIZE;
  }
}


export class TreeAlertsSubscription {
  refreshTimer: Subscription;
  group: TreeGroupData;

  constructor(refreshTimer: Subscription, group: TreeGroupData) {
    this.refreshTimer = refreshTimer;
    this.group = group;
  }
}
