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
import {Observable} from 'rxjs/Rx';
import {Headers, RequestOptions} from '@angular/http';
import { Injectable } from '@angular/core';
import {HttpUtil} from '../utils/httpUtil';
import {DataSource} from './data-source';
import {ColumnMetadata} from '../model/column-metadata';
import {ElasticsearchUtils} from '../utils/elasticsearch-utils';
import {
  ALERTS_COLUMN_NAMES, ALERTS_TABLE_METADATA, ALERTS_RECENT_SEARCH,
  ALERTS_SAVED_SEARCH, NUM_SAVED_SEARCH
} from '../utils/constants';
import {ColumnNames} from '../model/column-names';
import {ColumnNamesService} from './column-names.service';
import {TableMetadata} from '../model/table-metadata';
import {SaveSearch} from '../model/save-search';
import {SearchResponse} from '../model/search-response';
import {SearchRequest} from '../model/search-request';
import {AlertSource} from '../model/alert-source';

@Injectable()
export class ElasticSearchLocalstorageImpl extends DataSource {

  globalConfig: {} = {};
  sourceType: 'source:type';

  private defaultColumnMetadata = [
    new ColumnMetadata('id', 'string'),
    new ColumnMetadata('timestamp', 'date'),
    new ColumnMetadata('source:type', 'string'),
    new ColumnMetadata('ip_src_addr', 'ip'),
    new ColumnMetadata('enrichments:geo:ip_dst_addr:country', 'string'),
    new ColumnMetadata('ip_dst_addr', 'ip'),
    new ColumnMetadata('host', 'string'),
    new ColumnMetadata('alert_status', 'string')
  ];

  getAlerts(searchRequest: SearchRequest): Observable<SearchResponse> {
    let url = '/search/*' + ElasticsearchUtils.excludeIndexName + '/_search';
    let request: any  = JSON.parse(JSON.stringify(searchRequest));
    request.query = { query_string: { query: searchRequest.query } };

    return this.http.post(url, request, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .map(ElasticsearchUtils.extractAlertsData)
      .catch(HttpUtil.handleError)
      .onErrorResumeNext();
  }

  getAlert(sourceType: string, alertId: string): Observable<AlertSource> {
    return Observable.throw('Method not implemented in ElasticSearchLocalstorageImpl');
  }

  updateAlertState(request: any): Observable<{}> {
    return this.http.post('/search/_bulk', request, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .catch(HttpUtil.handleError);
  }

  getDefaultAlertTableColumnNames(): Observable<ColumnMetadata[]> {
    return Observable.create(observer => {
      observer.next(JSON.parse(JSON.stringify(this.defaultColumnMetadata)));
      observer.complete();
    });
  }

  getAllFieldNames(): Observable<ColumnMetadata[]> {
    let url = '_cluster/state';
    return this.http.get(url, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .map(ElasticsearchUtils.extractColumnNameData)
      .catch(HttpUtil.handleError);
  }

  getAlertTableColumnNames(): Observable<ColumnNames[]> {
    return Observable.create(observer => {
      let columnNames: ColumnNames[];
      try {
        columnNames = JSON.parse(localStorage.getItem(ALERTS_COLUMN_NAMES));
        ColumnNamesService.toMap(columnNames);
      } catch (e) {}

      columnNames = columnNames || [];

      observer.next(columnNames);
      observer.complete();

    });
  }

  saveAlertTableColumnNames(columns: ColumnNames[]): Observable<{}> {
    return Observable.create(observer => {
      try {
        localStorage.setItem(ALERTS_COLUMN_NAMES, JSON.stringify(columns));
      } catch (e) {}
      ColumnNamesService.toMap(columns);
      observer.next({});
      observer.complete();

    });
  }

  getAlertTableSettings(): Observable<TableMetadata> {
    return Observable.create(observer => {
      let tableMetadata: TableMetadata;
      try {
        tableMetadata = TableMetadata.fromJSON(JSON.parse(localStorage.getItem(ALERTS_TABLE_METADATA)));
      } catch (e) {}

      observer.next(tableMetadata);
      observer.complete();

    });
  }

  saveColumnMetaDataInAlertTableSettings(columns: ColumnMetadata[]): Observable<{}> {
    return Observable.create(observer => {
      try {
        let  tableMetadata = TableMetadata.fromJSON(JSON.parse(localStorage.getItem(ALERTS_TABLE_METADATA)));
        tableMetadata.tableColumns = columns;
        localStorage.setItem(ALERTS_TABLE_METADATA, JSON.stringify(tableMetadata));
      } catch (e) {}

      observer.next({});
      observer.complete();

    });
  }

  saveAlertTableSettings(tableMetadata): Observable<TableMetadata> {
    return Observable.create(observer => {
      try {
        localStorage.setItem(ALERTS_TABLE_METADATA, JSON.stringify(tableMetadata));
      } catch (e) {}

      observer.next({});
      observer.complete();

    });
  }

  deleteRecentSearch(saveSearch: SaveSearch): Observable<{}> {
    return Observable.create(observer => {
      let recentSearches: SaveSearch[] = [];
      try {
        recentSearches = JSON.parse(localStorage.getItem(ALERTS_RECENT_SEARCH));
        recentSearches = recentSearches.filter(search => search.name !== saveSearch.name);
      } catch (e) {}

      localStorage.setItem(ALERTS_RECENT_SEARCH, JSON.stringify(recentSearches));

      observer.next({});
      observer.complete();

    });
  }

  deleteSavedSearch(saveSearch: SaveSearch): Observable<{}> {
    return Observable.create(observer => {
      let savedSearches: SaveSearch[] = [];
      try {
        savedSearches = JSON.parse(localStorage.getItem(ALERTS_SAVED_SEARCH));
        savedSearches = savedSearches.filter(search => search.name !== saveSearch.name);
      } catch (e) {}

      localStorage.setItem(ALERTS_SAVED_SEARCH, JSON.stringify(savedSearches));

      observer.next({});
      observer.complete();

    });
  }

  listRecentSearches(): Observable<SaveSearch[]> {
    return Observable.create(observer => {
      let savedSearches: SaveSearch[] = [];
      try {
        savedSearches = JSON.parse(localStorage.getItem(ALERTS_RECENT_SEARCH));
      } catch (e) {}

      savedSearches = savedSearches || [];
      savedSearches = savedSearches.map(tSaveSeacrh => SaveSearch.fromJSON(tSaveSeacrh));

      observer.next(savedSearches);
      observer.complete();

    });
  }

  listSavedSearches(): Observable<SaveSearch[]> {
    return Observable.create(observer => {
      let savedSearches: SaveSearch[] = [];
      try {
        savedSearches = JSON.parse(localStorage.getItem(ALERTS_SAVED_SEARCH));
      } catch (e) {}

      savedSearches = savedSearches || [];
      savedSearches = savedSearches.map(tSaveSeacrh => SaveSearch.fromJSON(tSaveSeacrh));

      observer.next(savedSearches);
      observer.complete();

    });
  }

  saveRecentSearch(saveSearch: SaveSearch): Observable<{}> {
    return Observable.create(observer => {
      let savedSearches: SaveSearch[] = [];
      saveSearch.lastAccessed = new Date().getTime();

      try {
        savedSearches = JSON.parse(localStorage.getItem(ALERTS_RECENT_SEARCH));
      } catch (e) {}

      savedSearches = savedSearches || [];
      savedSearches = savedSearches.map(tSaveSeacrh => SaveSearch.fromJSON(tSaveSeacrh));

      if (savedSearches.length  === 0) {
        savedSearches.push(saveSearch);
      } else {
        let found = false;
        for ( let tSaveSearch of savedSearches) {
          if (saveSearch.name === tSaveSearch.name) {
            tSaveSearch.lastAccessed = new Date().getTime();
            found = true;
            break;
          }
        }
        if (!found ) {
          if (savedSearches.length < NUM_SAVED_SEARCH) {
            savedSearches.push(saveSearch);
          } else {
            savedSearches.sort((s1, s2) => s1.lastAccessed - s2.lastAccessed).shift();
            savedSearches.push(saveSearch);
          }
        }
      }

      localStorage.setItem(ALERTS_RECENT_SEARCH, JSON.stringify(savedSearches));

      observer.next({});
      observer.complete();

    });
  }

  saveSearch(saveSearch: SaveSearch): Observable<{}> {
    return Observable.create(observer => {
      let savedSearches: SaveSearch[] = [];
      try {
        savedSearches = JSON.parse(localStorage.getItem(ALERTS_SAVED_SEARCH));
      } catch (e) {}

      savedSearches = savedSearches || [];
      savedSearches.push(saveSearch);
      localStorage.setItem(ALERTS_SAVED_SEARCH, JSON.stringify(savedSearches));

      observer.next({});
      observer.complete();

    });
  }

  updateSearch(saveSearch: SaveSearch): Observable<{}> {
    return Observable.create(observer => {
      let savedSearches: SaveSearch[] = [];
      try {
        savedSearches = JSON.parse(localStorage.getItem(ALERTS_SAVED_SEARCH));
        let savedItem = savedSearches.find(search => search.name === saveSearch.name);
        savedItem.lastAccessed = saveSearch.lastAccessed;
        savedItem.searchRequest = saveSearch.searchRequest;
      } catch (e) {}

      localStorage.setItem(ALERTS_SAVED_SEARCH, JSON.stringify(savedSearches));

      observer.next({});
      observer.complete();

    });
  }
}
