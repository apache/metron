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

import {QueryBuilder} from '../alerts/alerts-list/query-builder';
import {ColumnMetadata} from './column-metadata';
import {SearchRequest} from './search-request';
import {Filter} from './filter';

export class SaveSearch {
  name  = '';
  lastAccessed = 0;
  searchRequest: SearchRequest;
  tableColumns: ColumnMetadata[];
  filters: Filter[];

  public static fromJSON(obj: SaveSearch): SaveSearch {
    let saveSearch = new SaveSearch();
    saveSearch.name = obj.name;
    saveSearch.lastAccessed = obj.lastAccessed;
    saveSearch.searchRequest = obj.searchRequest;
    saveSearch.filters = Filter.fromJSON(obj.filters);
    saveSearch.tableColumns = ColumnMetadata.fromJSON(obj.tableColumns);

    return saveSearch;
  }

  getDisplayString() {
    if (this.name && this.name.length > 0) {
      return this.name;
    }

    let queryBuilder = new QueryBuilder();
    queryBuilder.searchRequest = this.searchRequest;
    return queryBuilder.generateNameForSearchRequest();
  }
}
