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
import {PageSize, RefreshInterval} from '../alerts/configure-rows/configure-rows-enums';
import {ColumnMetadata} from './column-metadata';

export class TableMetadata {
  size = PageSize.TWENTY_FIVE;
  refreshInterval = RefreshInterval.TEN_MIN;
  hideResolvedAlerts = true;
  hideDismissedAlerts = true;
  tableColumns: ColumnMetadata[];

  static fromJSON(obj: any): TableMetadata {
    let tableMetadata = new TableMetadata();
    if (obj) {
      tableMetadata.size = obj.size;
      tableMetadata.refreshInterval = obj.refreshInterval;
      tableMetadata.hideResolvedAlerts = obj.hideResolvedAlerts;
      tableMetadata.hideDismissedAlerts = obj.hideDismissedAlerts;
      tableMetadata.tableColumns = (typeof (obj.tableColumns) === 'string') ? JSON.parse(obj.tableColumns) : obj.tableColumns;
    }

    return tableMetadata;
  }
}
