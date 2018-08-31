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
import {Injectable, } from '@angular/core';
import {Observable} from 'rxjs';
import { Subject } from 'rxjs';
import {QueryBuilder} from '../alerts/alerts-list/query-builder';
import {SaveSearch} from '../model/save-search';
import {ColumnMetadata} from '../model/column-metadata';
import {DataSource} from './data-source';

@Injectable()
export class SaveSearchService {

  queryBuilder: QueryBuilder;
  tableColumns: ColumnMetadata[];

  private loadSavedSearch = new Subject<SaveSearch>();
  loadSavedSearch$ = this.loadSavedSearch.asObservable();

  constructor(private dataSource: DataSource) {}

  deleteRecentSearch(saveSearch: SaveSearch): Observable<{}> {
    return this.dataSource.deleteRecentSearch(saveSearch);
  }

  deleteSavedSearch(saveSearch: SaveSearch): Observable<{}> {
    return this.dataSource.deleteSavedSearch(saveSearch);
  }

  fireLoadSavedSearch(savedSearch: SaveSearch) {
    this.loadSavedSearch.next(savedSearch);
  }

  listRecentSearches(): Observable<SaveSearch[]> {
    return this.dataSource.listRecentSearches();
  }

  listSavedSearches(): Observable<SaveSearch[]> {
    return this.dataSource.listSavedSearches();
  }

  saveAsRecentSearches(saveSearch: SaveSearch): Observable<{}> {
    return this.dataSource.saveRecentSearch(saveSearch);
  }

  saveSearch(saveSearch: SaveSearch): Observable<{}> {
    return this.dataSource.saveSearch(saveSearch);
  }

  setCurrentQueryBuilderAndTableColumns(queryBuilder: QueryBuilder, tableColumns: ColumnMetadata[]) {
    this.queryBuilder = queryBuilder;
    this.tableColumns = tableColumns;
  }

  updateSearch(saveSearch: SaveSearch): Observable<{}> {
    return this.dataSource.updateSearch(saveSearch);
  }
}
