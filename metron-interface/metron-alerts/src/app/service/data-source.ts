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
import {Observable} from 'rxjs';
import {Injectable} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {ColumnMetadata} from '../model/column-metadata';
import {ColumnNames} from '../model/column-names';
import {TableMetadata} from '../model/table-metadata';
import {SaveSearch} from '../model/save-search';

@Injectable()
export abstract class DataSource {

  constructor(protected http: HttpClient) {}

  // Calls to fetch default alert table column names and all the field names across all indexes
  abstract getDefaultAlertTableColumnNames(): Observable<ColumnMetadata[]>

  // Calls to rename field names and to fetch the renamed field names
  abstract getAlertTableColumnNames(): Observable<ColumnNames[]>
  abstract saveAlertTableColumnNames(columns: ColumnNames[]): Observable<{}>

  // Calls to fetch and save alerts table settings like refresh interval, page size, default selected table column names
  abstract getAlertTableSettings(): Observable<TableMetadata>
  abstract saveColumnMetaDataInAlertTableSettings(columns: ColumnMetadata[]): Observable<{}>
  abstract saveAlertTableSettings(tableMetadata): Observable<TableMetadata>

  // Calls to save search, last 10 searches, saved searches
  abstract deleteRecentSearch(saveSearch: SaveSearch): Observable<{}>
  abstract deleteSavedSearch(saveSearch: SaveSearch): Observable<{}>
  abstract listRecentSearches(): Observable<SaveSearch[]>
  abstract listSavedSearches(): Observable<SaveSearch[]>
  abstract saveRecentSearch(saveSearch: SaveSearch): Observable<{}>
  abstract saveSearch(saveSearch: SaveSearch): Observable<{}>
  abstract updateSearch(saveSearch: SaveSearch): Observable<{}>
}
