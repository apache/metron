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
import {ALERTS_COLUMN_NAMES} from '../utils/constants';
import {ColumnNames} from '../model/column-names';

@Injectable()
export class ColumnNamesService {

  static columnNameToDisplayValueMap = {};
  static columnDisplayValueToNameMap = {};

  constructor(private http: Http, @Inject(APP_CONFIG) private config: IAppConfig) {
  }

  list(): Promise<ColumnNames> {
    return Observable.create(observer => {
      let columnNames: ColumnNames[];
      try {
        columnNames = JSON.parse(localStorage.getItem(ALERTS_COLUMN_NAMES));
        ColumnNamesService.toMap(columnNames);
      } catch (e) {}

      columnNames = columnNames || [];

      observer.next(columnNames);
      observer.complete();

    }).toPromise();
  }

  save(columns: ColumnNames[]): Observable<{}> {
    return Observable.create(observer => {
      try {
        localStorage.setItem(ALERTS_COLUMN_NAMES, JSON.stringify(columns));
      } catch (e) {}
      ColumnNamesService.toMap(columns);
      observer.next({});
      observer.complete();

    });
  }

  public static getColumnDisplayValue(key: string) {
    if (!key) {
      return '';
    }

    let displayValue = ColumnNamesService.columnNameToDisplayValueMap[key];

    return displayValue ? displayValue : key;
  }

  public static getColumnDisplayKey(key: string) {
    if (!key) {
      return key;
    }

    let name = ColumnNamesService.columnDisplayValueToNameMap[key];

    return (!name || name.length === 0) ? key : name;
  }

  private static toMap(columnNames:ColumnNames[]) {
    ColumnNamesService.columnNameToDisplayValueMap = {};
    ColumnNamesService.columnDisplayValueToNameMap = {};

    columnNames.forEach(columnName => {
      ColumnNamesService.columnNameToDisplayValueMap[columnName.key] = columnName.displayValue;
      ColumnNamesService.columnDisplayValueToNameMap[columnName.displayValue] = columnName.key;
    });
  }
}
