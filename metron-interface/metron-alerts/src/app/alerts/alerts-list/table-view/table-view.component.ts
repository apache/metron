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

import { Component, Input, Output, EventEmitter } from '@angular/core';
import {Router} from '@angular/router';

import {Pagination} from '../../../model/pagination';
import {SortEvent} from '../../../shared/metron-table/metron-table.directive';
import {ColumnMetadata} from '../../../model/column-metadata';
import {Alert} from '../../../model/alert';
import {SearchResponse} from '../../../model/search-response';
import {SearchService} from '../../../service/search.service';
import {MetronDialogBox, DialogType} from '../../../shared/metron-dialog-box';
import {ElasticsearchUtils} from '../../../utils/elasticsearch-utils';
import {QueryBuilder} from '../query-builder';
import {Sort} from '../../../utils/enums';
import {Filter} from '../../../model/filter';

@Component({
  selector: 'app-table-view',
  templateUrl: './table-view.component.html',
  styleUrls: ['./table-view.component.scss']
})

export class TableViewComponent {

  alerts: Alert[] = [];
  threatScoreFieldName = 'threat:triage:score';

  router: Router;
  searchService: SearchService;
  metronDialogBox: MetronDialogBox;
  pagingData = new Pagination();
  searchResponse: SearchResponse = new SearchResponse();

  @Input() queryBuilder: QueryBuilder;
  @Input() alertsColumnsToDisplay: ColumnMetadata[] = [];
  @Input() selectedAlerts: Alert[] = [];

  @Output() onResize = new EventEmitter<void>();
  @Output() onAddFilter = new EventEmitter<Filter>();
  @Output() onShowDetails = new EventEmitter<Alert>();
  @Output() selectedAlertsChange = new EventEmitter< Alert[]>();

  constructor(router: Router,
              searchService: SearchService,
              metronDialogBox: MetronDialogBox) {
    this.router = router;
    this.searchService = searchService;
    this.metronDialogBox = metronDialogBox;
  }

  search(resetPaginationParams = true, pageSize: number = null) {
    if (resetPaginationParams) {
      this.pagingData.from = 0;
    }

    this.pagingData.size = pageSize === null ? this.pagingData.size : pageSize;
    this.queryBuilder.setFromAndSize(this.pagingData.from, this.pagingData.size);

    this.searchService.search(this.queryBuilder.searchRequest).subscribe(results => {
      this.setAlertData(results);
    }, error => {
      this.setAlertData(new SearchResponse());
      this.metronDialogBox.showConfirmationMessage(ElasticsearchUtils.extractESErrorMessage(error), DialogType.Error);
    });
  }

  setAlertData(results: SearchResponse) {
    this.searchResponse = results;
    this.pagingData.total = results.total;
    this.alerts = this.searchResponse.results ? this.searchResponse.results : [];
  }

  onSort(sortEvent: SortEvent) {
    let sortOrder = (sortEvent.sortOrder === Sort.ASC ? 'asc' : 'desc');
    let sortBy = sortEvent.sortBy === 'id' ? '_uid' : sortEvent.sortBy;
    this.queryBuilder.setSort(sortBy, sortOrder);
    this.search();
  }

  getValue(alert: Alert, column: ColumnMetadata, formatData: boolean) {
    let returnValue = '';
    try {
      switch (column.name) {
        case 'id':
          returnValue = alert[column.name];
          break;
        case 'alert_status':
          returnValue = 'NEW';
          break;
        default:
          returnValue = alert.source[column.name];
          break;
      }
    } catch (e) {}

    if (formatData) {
      returnValue = this.formatValue(column, returnValue);
    }

    return returnValue;
  }

  formatValue(column: ColumnMetadata, returnValue: string) {
    try {
      if (column.name.endsWith(':ts') || column.name.endsWith('timestamp')) {
        returnValue = new Date(parseInt(returnValue, 10)).toISOString().replace('T', ' ').slice(0, 19);
      }
    } catch (e) {}

    return returnValue;
  }

  onPageChange() {
    this.search(false);
  }

  selectRow($event, alert: Alert) {
    if ($event.target.checked) {
      this.selectedAlerts.push(alert);
    } else {
      this.selectedAlerts.splice(this.selectedAlerts.indexOf(alert), 1);
    }

    this.selectedAlertsChange.emit(this.selectedAlerts);
  }

  selectAllRows($event) {
    this.selectedAlerts = [];
    if ($event.target.checked) {
      this.selectedAlerts = this.alerts;
    }

    this.selectedAlertsChange.emit(this.selectedAlerts);
  }

  resize() {
    this.onResize.emit();
  }

  addFilter(field: string, value: string) {
    field = (field === 'id') ? '_uid' : field;
    this.onAddFilter.emit(new Filter(field, value));
  }

  showDetails($event, alert: Alert) {
    if ($event.target.parentElement.firstElementChild.type !== 'checkbox' && $event.target.nodeName !== 'A') {
      this.onShowDetails.emit(alert);
    }
  }
}
