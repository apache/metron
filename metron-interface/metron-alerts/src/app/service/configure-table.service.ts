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
import {Injectable, Inject} from '@angular/core';
import {Observable} from 'rxjs/Rx';
import {Http} from '@angular/http';
import {Subject} from 'rxjs/Subject';

import {IAppConfig} from '../app.config.interface';
import {APP_CONFIG} from '../app.config';
import {ALERTS_TABLE_METADATA} from '../utils/constants';
import {ColumnMetadata} from '../model/column-metadata';
import {TableMetadata} from '../model/table-metadata';

@Injectable()
export class ConfigureTableService {

  private tableChangedSource = new Subject<string>();
  tableChanged$ = this.tableChangedSource.asObservable();
  defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};

  constructor(private http: Http, @Inject(APP_CONFIG) private config: IAppConfig) {
  }

  fireTableChanged() {
    this.tableChangedSource.next('table changed');
  }

  getTableMetadata(): Observable<TableMetadata> {
    return Observable.create(observer => {
      let tableMetadata: TableMetadata;
      try {
        tableMetadata = TableMetadata.fromJSON(JSON.parse(localStorage.getItem(ALERTS_TABLE_METADATA)));
      } catch (e) {}

      observer.next(tableMetadata);
      observer.complete();

    });
  }

  saveColumnMetaData(columns: ColumnMetadata[]): Observable<{}> {
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

  saveTableMetaData(tableMetadata: TableMetadata): Observable<TableMetadata> {
    return Observable.create(observer => {
      try {
        localStorage.setItem(ALERTS_TABLE_METADATA, JSON.stringify(tableMetadata));
      } catch (e) {}

      observer.next({});
      observer.complete();

    });
  }

}
