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
import { ColumnMetadata } from '../model/column-metadata';
import { TableMetadata } from '../model/table-metadata';
import { UserSettingsService } from './user-settings.service';
import { ALERTS_TABLE_METADATA } from '../utils/constants';


@Injectable()
export class ConfigureTableService {

  private tableChangedSource = new Subject<string>();
  tableChanged$ = this.tableChangedSource.asObservable();

  constructor(
    private userSettingsService: UserSettingsService
  ) {}

  fireTableChanged() {
    this.tableChangedSource.next('table changed');
  }

  getTableMetadata(): Observable<{}> {
    return new Observable((observer) => {
      this.userSettingsService.get(ALERTS_TABLE_METADATA)
        .subscribe((tableMetadata) => {
          tableMetadata = TableMetadata.fromJSON(tableMetadata);
          observer.next(tableMetadata);
          observer.complete();
        });
    });
  }

  saveColumnMetaData(columns: ColumnMetadata[]): Observable<{}> {
    return new Observable((observer) => {
      this.userSettingsService.get(ALERTS_TABLE_METADATA)
        .subscribe((tableMetadata) => {
          tableMetadata = TableMetadata.fromJSON(tableMetadata);
          tableMetadata.tableColumns = columns;
          this.userSettingsService.save({
            [ALERTS_TABLE_METADATA]: tableMetadata
          }).subscribe(() => {
            observer.next({});
            observer.complete();
          });
        });
    });
  }

  saveTableMetaData(tableMetadata): Observable<{}> {
    return new Observable((observer) => {
      this.userSettingsService.save({
        [ALERTS_TABLE_METADATA]: tableMetadata
      }).subscribe(() => {
        observer.next({});
        observer.complete();
      });
    });
  }
}
