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
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Subject } from 'rxjs';
import { QueryBuilder } from '../alerts/alerts-list/query-builder';
import { SaveSearch } from '../model/save-search';
import { ColumnMetadata } from '../model/column-metadata';
import { UserSettingsService } from './user-settings.service';
import { ALERTS_RECENT_SEARCH, ALERTS_SAVED_SEARCH, NUM_SAVED_SEARCH } from '../utils/constants';

@Injectable()
export class SaveSearchService {

  queryBuilder: QueryBuilder;
  tableColumns: ColumnMetadata[];

  private loadSavedSearch = new Subject<SaveSearch>();
  loadSavedSearch$ = this.loadSavedSearch.asObservable();

  constructor(
    private userSettingsService: UserSettingsService
  ) {}

  deleteSavedSearch(saveSearch: SaveSearch): Observable<{}> {
    return new Observable((observer) => {
      this.userSettingsService.get(ALERTS_SAVED_SEARCH)
        .subscribe((savedSearches) => {
          this.userSettingsService.get(ALERTS_RECENT_SEARCH)
            .subscribe((recentSearches) => {
              if (recentSearches) {
                recentSearches = recentSearches.filter(search => search.name !== saveSearch.name);
              }
              savedSearches = savedSearches.filter(search => search.name !== saveSearch.name);
              this.userSettingsService.save({
                [ALERTS_RECENT_SEARCH]: recentSearches,
                [ALERTS_SAVED_SEARCH]: savedSearches
              }).subscribe();
              observer.next({});
              observer.complete();
            });
        });
    });
  }

  fireLoadSavedSearch(savedSearch: SaveSearch) {
    this.loadSavedSearch.next(savedSearch);
  }

  listRecentSearches(): Observable<SaveSearch[]> {
    return new Observable((observer) => {
      this.userSettingsService.get(ALERTS_RECENT_SEARCH)
        .subscribe((recentSearches) => {
          recentSearches = recentSearches || [];
          recentSearches = recentSearches.map(search => SaveSearch.fromJSON(search));
          observer.next(recentSearches);
          observer.complete();
        });
    });
  }

  listSavedSearches(): Observable<SaveSearch[]> {
    return new Observable((observer) => {
      this.userSettingsService.get(ALERTS_SAVED_SEARCH)
        .subscribe((savedSearches) => {
          savedSearches = savedSearches || [];
          savedSearches = savedSearches.map(search => SaveSearch.fromJSON(search));
          observer.next(savedSearches);
          observer.complete();
        });
    });
  }

  saveAsRecentSearches(saveSearch: SaveSearch): Observable<{}> {
    return new Observable((observer) => {
      saveSearch.lastAccessed = new Date().getTime();
      this.userSettingsService.get(ALERTS_RECENT_SEARCH)
        .subscribe((recentSearches) => {
          if (!recentSearches) {
            recentSearches = [];
          }
          if (recentSearches.length === 0) {
            recentSearches.push(saveSearch);
          } else {
            let found = false;
            for (let recentSearch of recentSearches) {
              if (saveSearch.name === recentSearch.name) {
                recentSearch.lastAccessed = new Date().getTime();
                found = true;
                break;
              }
            }
            if (!found) {
              if (recentSearches.length < NUM_SAVED_SEARCH) {
                recentSearches.push(saveSearch);
              } else {
                recentSearches.sort((s1, s2) => s1.lastAccessed - s2.lastAccessed).shift();
                recentSearches.push(saveSearch);
              }
            }
          }

          this.userSettingsService.save({
            [ALERTS_RECENT_SEARCH]: recentSearches
          }).subscribe();
          observer.next({});
          observer.complete();
        });
    });
  }

  saveSearch(saveSearch: SaveSearch): Observable<{}> {
    return new Observable((observer) => {
      this.userSettingsService.get(ALERTS_SAVED_SEARCH)
        .subscribe((savedSearches) => {
          if (!savedSearches) {
            savedSearches = [];
          }
          savedSearches.push(saveSearch);
          this.userSettingsService.save({
            [ALERTS_SAVED_SEARCH]: savedSearches
          }).subscribe();
          observer.next({});
          observer.complete();
        });
    });
  }

  setCurrentQueryBuilderAndTableColumns(queryBuilder: QueryBuilder, tableColumns: ColumnMetadata[]) {
    this.queryBuilder = queryBuilder;
    this.tableColumns = tableColumns;
  }

  updateSearch(saveSearch: SaveSearch): Observable<{}> {
    return new Observable((observer) => {
      this.userSettingsService.get(ALERTS_SAVED_SEARCH)
        .subscribe((savedSearches) => {
          debugger;
          if (!savedSearches) {
            savedSearches = [];
          }
          savedSearches = savedSearches.map(search => {
            if (search.name === saveSearch.name) {
              return {
                ...search,
                lastAccessed: saveSearch.lastAccessed,
                searchRequest: saveSearch.searchRequest
              };
            }
            return search;
          });
          this.userSettingsService.save({
            [ALERTS_SAVED_SEARCH]: savedSearches
          }).subscribe(() => {
            observer.next({});
            observer.complete();
          });
        });
    });
  }
}
