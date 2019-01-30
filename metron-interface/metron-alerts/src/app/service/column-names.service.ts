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
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';

import {ColumnNames} from '../model/column-names';
import {DataSource} from './data-source';

@Injectable()
export class ColumnNamesService {

  static columnNameToDisplayValueMap = {};
  static columnDisplayValueToNameMap = {};

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

  public static toMap(columnNames: ColumnNames[]) {
    ColumnNamesService.columnNameToDisplayValueMap = {};
    ColumnNamesService.columnDisplayValueToNameMap = {};

    columnNames.forEach(columnName => {
      ColumnNamesService.columnNameToDisplayValueMap[columnName.key] = columnName.displayValue;
      ColumnNamesService.columnDisplayValueToNameMap[columnName.displayValue] = columnName.key;
    });
  }

  constructor(private dataSource: DataSource) {}

  save(columns: ColumnNames[]): Observable<{}> {
    return this.dataSource.saveAlertTableColumnNames(columns);
  }
}
