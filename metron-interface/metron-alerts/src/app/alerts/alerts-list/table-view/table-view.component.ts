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

import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, OnInit, OnDestroy } from '@angular/core';
import {Router} from '@angular/router';
import { Subscription } from 'rxjs/Rx';

import {Pagination} from '../../../model/pagination';
import {SortEvent} from '../../../shared/metron-table/metron-table.directive';
import {ColumnMetadata} from '../../../model/column-metadata';
import {Alert} from '../../../model/alert';
import {SearchService} from '../../../service/search.service';
import {MetronDialogBox} from '../../../shared/metron-dialog-box';
import {QueryBuilder} from '../query-builder';
import {Sort} from '../../../utils/enums';
import {Filter} from '../../../model/filter';
import {AlertSource} from '../../../model/alert-source';
import {PatchRequest} from '../../../model/patch-request';
import {Patch} from '../../../model/patch';
import {UpdateService} from '../../../service/update.service';
import {META_ALERTS_INDEX} from '../../../utils/constants';
import {MetaAlertService} from '../../../service/meta-alert.service';
import {MetaAlertAddRemoveRequest} from '../../../model/meta-alert-add-remove-request';
import {GetRequest} from '../../../model/get-request';
import { GlobalConfigService } from '../../../service/global-config.service';

export enum MetronAlertDisplayState {
  COLLAPSE, EXPAND
}

@Component({
  selector: 'app-table-view',
  templateUrl: './table-view.component.html',
  styleUrls: ['./table-view.component.scss']
})

export class TableViewComponent implements OnInit, OnChanges, OnDestroy {

  threatScoreFieldName = 'threat:triage:score';

  router: Router;
  searchService: SearchService;
  updateService: UpdateService;
  isStatusFieldPresent = false;
  metronDialogBox: MetronDialogBox;
  metaAlertService: MetaAlertService;
  metaAlertsDisplayState: {[key: string]: MetronAlertDisplayState} = {};
  metronAlertDisplayState = MetronAlertDisplayState;
  globalConfig: {} = {};
  globalConfigService: GlobalConfigService;
  configSubscription: Subscription;

  @Input() alerts: Alert[] = [];
  @Input() queryBuilder: QueryBuilder;
  @Input() pagination: Pagination;
  @Input() alertsColumnsToDisplay: ColumnMetadata[] = [];
  @Input() selectedAlerts: Alert[] = [];

  @Output() onResize = new EventEmitter<void>();
  @Output() onAddFilter = new EventEmitter<Filter>();
  @Output() onRefreshData = new EventEmitter<boolean>();
  @Output() onShowDetails = new EventEmitter<Alert>();
  @Output() onShowConfigureTable = new EventEmitter<Alert>();
  @Output() onSelectedAlertsChange = new EventEmitter< Alert[]>();

  constructor(router: Router,
              searchService: SearchService,
              metronDialogBox: MetronDialogBox,
              updateService: UpdateService,
              metaAlertService: MetaAlertService,
              globalConfigService: GlobalConfigService) {
    this.router = router;
    this.searchService = searchService;
    this.metronDialogBox = metronDialogBox;
    this.updateService = updateService;
    this.metaAlertService = metaAlertService;
    this.globalConfigService = globalConfigService;
  }

  ngOnInit() {
    this.configSubscription = this.globalConfigService.get().subscribe((config: {}) => {
      this.globalConfig = config;
      if (this.globalConfig['source.type.field']) {
        let filteredAlertsColumnsToDisplay = this.alertsColumnsToDisplay.filter(colName => colName.name !== 'source:type');
        if (filteredAlertsColumnsToDisplay.length < this.alertsColumnsToDisplay.length) {
          this.alertsColumnsToDisplay = filteredAlertsColumnsToDisplay;
          this.alertsColumnsToDisplay.splice(2, 0, new ColumnMetadata(this.globalConfig['source.type.field'], 'string'));
        }
      }
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes && changes['alerts'] && changes['alerts'].currentValue) {
      let expandedMetaAlerts = this.getGUIDOfAllExpandedMetaAlerts();
      this.updateExpandedStateForChangedData(expandedMetaAlerts);
    }

    if (changes && changes['alertsColumnsToDisplay'] && changes['alertsColumnsToDisplay'].currentValue) {
      this.isStatusFieldPresent = this.alertsColumnsToDisplay.some(col => col.name === 'alert_status');
    }
  }

  ngOnDestroy() {
    this.configSubscription.unsubscribe();
  }

  updateExpandedStateForChangedData(expandedMetaAlerts: string[]) {
    this.alerts.forEach(alert => {
      if (alert.source.alert && alert.source.alert.length > 0) {
        this.metaAlertsDisplayState[alert.id] = expandedMetaAlerts.indexOf(alert.id) === -1 ?
                                                  MetronAlertDisplayState.COLLAPSE : MetronAlertDisplayState.EXPAND;
      }
    });
  }

  getGUIDOfAllExpandedMetaAlerts(): string[] {
    let expandedMetaAlerts = [];
    Object.keys(this.metaAlertsDisplayState).forEach(id => {
      if (this.metaAlertsDisplayState[id] === MetronAlertDisplayState.EXPAND) {
        expandedMetaAlerts.push(id);
      }
    });

    return expandedMetaAlerts;
  }

  onSort(sortEvent: SortEvent) {
    let sortOrder = (sortEvent.sortOrder === Sort.ASC ? 'asc' : 'desc');
    let sortBy = sortEvent.sortBy === 'id' ? 'guid' : sortEvent.sortBy;
    this.queryBuilder.setSort(sortBy, sortOrder);
    this.onRefreshData.emit(true);
  }

  getValue(alert: Alert, column: ColumnMetadata, formatData: boolean) {
    if (column.name === 'id') {
      return this.formatValue(column, alert[column.name]);
    }

    return this.getValueFromSource(alert.source, column, formatData);
  }

  getValueFromSource(alertSource: AlertSource, column: ColumnMetadata, formatData: boolean) {
    let returnValue = '';
    try {
      switch (column.name) {
        case 'id':
          returnValue = alertSource['guid'];
          break;
        case 'alert_status':
          returnValue = alertSource['alert_status'] ? alertSource['alert_status'] : 'NEW';
          break;
        default:
          returnValue = alertSource[column.name];
          break;
      }
    } catch (e) {
    }

    if (formatData) {
      returnValue = this.formatValue(column, returnValue);
    }
    return returnValue;
  }

  fireSelectedAlertsChanged() {
    this.onSelectedAlertsChange.emit(this.selectedAlerts);
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
    this.queryBuilder.setFromAndSize(this.pagination.from, this.pagination.size);
    this.onRefreshData.emit(false);
  }

  selectRow($event, alert: Alert) {
    if ($event.target.checked) {
      this.selectedAlerts.push(alert);
    } else {
      this.selectedAlerts.splice(this.selectedAlerts.indexOf(alert), 1);
    }
    this.fireSelectedAlertsChanged();
  }

  selectAllRows($event) {
    this.selectedAlerts = [];
    if ($event.target.checked) {
      this.selectedAlerts = this.alerts;
    }
    this.fireSelectedAlertsChanged();
  }

  resize() {
    this.onResize.emit();
  }

  addFilter(field: string, value: string) {
    field = (field === 'id') ? 'guid' : field;
    this.onAddFilter.emit(new Filter(field, value));
  }

  showMetaAlertDetails($event, alertSource: AlertSource) {
    let alert = new Alert();
    alert.source = alertSource;
    alert.index = META_ALERTS_INDEX;
    this.showDetails($event, alert);
  }

  showDetails($event, alert: Alert) {
    if ($event.target.parentElement.firstElementChild.type !== 'checkbox' && $event.target.nodeName !== 'A') {
      this.onShowDetails.emit(alert);
    }
  }

  showConfigureTable() {
    this.onShowConfigureTable.emit();
  }

  toggleExpandCollapse($event, alert: Alert) {
    if (this.metaAlertsDisplayState[alert.id] === MetronAlertDisplayState.COLLAPSE) {
      this.metaAlertsDisplayState[alert.id] = MetronAlertDisplayState.EXPAND;
    } else {
      this.metaAlertsDisplayState[alert.id] = MetronAlertDisplayState.COLLAPSE;
    }

    $event.stopPropagation();
    return false;
  }

  deleteOneAlertFromMetaAlert($event, alert: Alert, metaAlertIndex: number) {
    this.metronDialogBox.showConfirmationMessage('Do you wish to remove the alert from the meta alert?').subscribe(response => {
      if (response) {
        this.doDeleteOneAlertFromMetaAlert(alert, metaAlertIndex);
      }
    });
    $event.stopPropagation();
  }

  deleteMetaAlert($event, alert: Alert, index: number) {
    this.metronDialogBox.showConfirmationMessage('Do you wish to remove all the alerts from meta alert?').subscribe(response => {
      if (response) {
        this.doDeleteMetaAlert(alert, index);
      }
    });
    $event.stopPropagation();
  }

  doDeleteOneAlertFromMetaAlert(alert, metaAlertIndex) {
    let alertToRemove = alert.source.alert[metaAlertIndex];
    let metaAlertAddRemoveRequest = new MetaAlertAddRemoveRequest();
    metaAlertAddRemoveRequest.metaAlertGuid = alert.source.guid;
    metaAlertAddRemoveRequest.alerts = [new GetRequest(alertToRemove.guid, alertToRemove[this.globalConfig['source.type.field']], '')];

    this.metaAlertService.removeAlertsFromMetaAlert(metaAlertAddRemoveRequest).subscribe(() => {
    });
  }

  doDeleteMetaAlert(alert: Alert, index: number) {
    this.metaAlertService.updateMetaAlertStatus(alert.source.guid, 'inactive').subscribe(() => {
    });
  }
}
