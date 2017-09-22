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
import {Component, OnInit, ViewChild, ElementRef, OnDestroy, ChangeDetectorRef} from '@angular/core';
import {Router, NavigationStart} from '@angular/router';
import {Observable, Subscription} from 'rxjs/Rx';

import {Alert} from '../../model/alert';
import {SearchService} from '../../service/search.service';
import {QueryBuilder} from './query-builder';
import {ConfigureTableService} from '../../service/configure-table.service';
import {WorkflowService} from '../../service/workflow.service';
import {ClusterMetaDataService} from '../../service/cluster-metadata.service';
import {ColumnMetadata} from '../../model/column-metadata';
import {SaveSearchService} from '../../service/save-search.service';
import {RefreshInterval} from '../configure-rows/configure-rows-enums';
import {SaveSearch} from '../../model/save-search';
import {TableMetadata} from '../../model/table-metadata';
import {MetronDialogBox, DialogType} from '../../shared/metron-dialog-box';
import {AlertSearchDirective} from '../../shared/directives/alert-search.directive';
import {SearchResponse} from '../../model/search-response';
import {ElasticsearchUtils} from '../../utils/elasticsearch-utils';
import {TableViewComponent} from './table-view/table-view.component';
import {Filter} from '../../model/filter';

@Component({
  selector: 'app-alerts-list',
  templateUrl: './alerts-list.component.html',
  styleUrls: ['./alerts-list.component.scss']
})

export class AlertsListComponent implements OnInit, OnDestroy {

  alertsColumns: ColumnMetadata[] = [];
  alertsColumnsToDisplay: ColumnMetadata[] = [];
  selectedAlerts: Alert[] = [];
  alerts: Alert[] = [];
  searchResponse: SearchResponse = new SearchResponse();
  colNumberTimerId: number;
  refreshInterval = RefreshInterval.ONE_MIN;
  pauseRefresh = false;
  lastPauseRefreshValue = false;
  threatScoreFieldName = 'threat:triage:score';
  refreshTimer: Subscription;

  @ViewChild('table') table: ElementRef;
  @ViewChild('dataViewComponent') dataViewComponent: TableViewComponent;
  @ViewChild(AlertSearchDirective) alertSearchDirective: AlertSearchDirective;

  tableMetaData = new TableMetadata();
  queryBuilder: QueryBuilder = new QueryBuilder();

  constructor(private router: Router,
              private searchService: SearchService,
              private configureTableService: ConfigureTableService,
              private workflowService: WorkflowService,
              private clusterMetaDataService: ClusterMetaDataService,
              private saveSearchService: SaveSearchService,
              private metronDialogBox: MetronDialogBox,
              private changeDetector: ChangeDetectorRef) {
    router.events.subscribe(event => {
      if (event instanceof NavigationStart && event.url === '/alerts-list') {
        this.selectedAlerts = [];
        this.restoreRefreshState();
      }
    });
  }

  addAlertColChangedListner() {
    this.configureTableService.tableChanged$.subscribe(colChanged => {
      if (colChanged) {
        this.getAlertColumnNames(false);
      }
    });
  }

  addLoadSavedSearchListner() {
    this.saveSearchService.loadSavedSearch$.subscribe((savedSearch: SaveSearch) => {
      let queryBuilder = new QueryBuilder();
      queryBuilder.searchRequest = savedSearch.searchRequest;
      this.queryBuilder = queryBuilder;
      this.prepareColumnData(savedSearch.tableColumns, []);
      this.search(true, savedSearch);
    });
  }

  calcColumnsToDisplay() {
    let availableWidth = document.documentElement.clientWidth - (200 + (15 * 3)); /* screenwidth - (navPaneWidth + (paddings))*/
    availableWidth = availableWidth - (55 + 25 + 25); /* availableWidth - (score + colunSelectIcon +selectCheckbox )*/
    let tWidth = 0;
    this.alertsColumnsToDisplay =  this.alertsColumns.filter(colMetaData => {
      if (colMetaData.type.toUpperCase() === 'DATE') {
        tWidth += 140;
      } else if (colMetaData.type.toUpperCase() === 'IP') {
        tWidth += 120;
      } else if (colMetaData.type.toUpperCase() === 'BOOLEAN') {
        tWidth += 50;
      } else {
        tWidth += 130;
      }

      return tWidth < availableWidth;
    });
  }

  getAlertColumnNames(resetPaginationForSearch: boolean) {
    Observable.forkJoin(
      this.configureTableService.getTableMetadata(),
      this.clusterMetaDataService.getDefaultColumns()
    ).subscribe((response: any) => {
      this.prepareData(response[0], response[1], resetPaginationForSearch);
    });
  }

  getColumnNamesForQuery() {
    let fieldNames = this.alertsColumns.map(columnMetadata => columnMetadata.name);
    fieldNames = fieldNames.filter(name => !(name === 'id' || name === 'alert_status'));
    fieldNames.push(this.threatScoreFieldName);
    return fieldNames;
  }

  ngOnDestroy() {
    this.tryStopPolling();
  }

  ngOnInit() {
    this.getAlertColumnNames(true);
    this.addAlertColChangedListner();
    this.addLoadSavedSearchListner();
  }

  onClear() {
    this.queryBuilder.displayQuery = '';
    this.search();
  }

  onSearch($event) {
    this.queryBuilder.displayQuery = $event;
    this.search();

    return false;
  }

  onAddFilter(filter: Filter) {
    this.queryBuilder.addOrUpdateFilter(filter);
    this.search();
  }

  onConfigRowsChange() {
    this.searchService.interval = this.refreshInterval;
    this.search();
  }

  searchView(resetPaginationParams = true, pageSize: number = null) {
    this.changeDetector.detectChanges();
    this.dataViewComponent.search(resetPaginationParams, pageSize);
  }

  onPausePlay() {
    this.pauseRefresh = !this.pauseRefresh;
    if (this.pauseRefresh) {
      this.tryStopPolling();
    } else {
      this.search(false);
    }
  }

  onResize() {
    clearTimeout(this.colNumberTimerId);
    this.colNumberTimerId = setTimeout(() => { this.calcColumnsToDisplay(); }, 500);
  }

  prepareColumnData(configuredColumns: ColumnMetadata[], defaultColumns: ColumnMetadata[]) {
    this.alertsColumns = (configuredColumns && configuredColumns.length > 0) ? configuredColumns : defaultColumns;
    this.queryBuilder.setFields(this.getColumnNamesForQuery());
    this.calcColumnsToDisplay();
  }

  prepareData(tableMetaData: TableMetadata, defaultColumns: ColumnMetadata[], resetPagination: boolean) {
    this.tableMetaData = tableMetaData;
    this.refreshInterval = this.tableMetaData.refreshInterval;

    this.updateConfigRowsSettings();
    this.prepareColumnData(tableMetaData.tableColumns, defaultColumns);

    this.search(resetPagination);
  }

  processEscalate() {
    this.workflowService.start(this.selectedAlerts).subscribe(workflowId => {
      this.searchService.updateAlertState(this.selectedAlerts, 'ESCALATE', workflowId).subscribe(results => {
        this.updateSelectedAlertStatus('ESCALATE');
      });
    });
  }

  processDismiss() {
    this.searchService.updateAlertState(this.selectedAlerts, 'DISMISS', '').subscribe(results => {
      this.updateSelectedAlertStatus('DISMISS');
    });
  }

  processOpen() {
    this.searchService.updateAlertState(this.selectedAlerts, 'OPEN', '').subscribe(results => {
      this.updateSelectedAlertStatus('OPEN');
    });
  }

  processResolve() {
    this.searchService.updateAlertState(this.selectedAlerts, 'RESOLVE', '').subscribe(results => {
      this.updateSelectedAlertStatus('RESOLVE');
    });
  }

  removeFilter(field: string) {
    this.queryBuilder.removeFilter(field);
    this.search();
  }

  restoreRefreshState() {
    this.pauseRefresh = this.lastPauseRefreshValue;
    this.tryStartPolling();
  }

  search(resetPaginationParams = true, savedSearch?: SaveSearch) {
    this.selectedAlerts = [];

    this.saveCurrentSearch(savedSearch);

    this.queryBuilder.setFromAndSize(0, 0);
    this.searchService.search(this.queryBuilder.searchRequest).subscribe(results => {
      this.setData(results);
    }, error => {
      this.setData(new SearchResponse());
      this.metronDialogBox.showConfirmationMessage(ElasticsearchUtils.extractESErrorMessage(error), DialogType.Error);
    });

    this.searchView(resetPaginationParams, this.tableMetaData.size);

    this.tryStartPolling();
  }

  saveCurrentSearch(savedSearch: SaveSearch) {
    if (this.queryBuilder.query !== '*') {
      if (!savedSearch) {
        savedSearch = new SaveSearch();
        savedSearch.searchRequest = this.queryBuilder.searchRequest;
        savedSearch.tableColumns = this.alertsColumns;
        savedSearch.name = savedSearch.getDisplayString();
      }

      this.saveSearchService.saveAsRecentSearches(savedSearch).subscribe(() => {
      });
    }
  }

  setData(results: SearchResponse) {
    this.searchResponse = results;
    this.alerts = results.results ? results.results : [];
  }

  showConfigureTable() {
    this.saveRefreshState();
    this.router.navigateByUrl('/alerts-list(dialog:configure-table)');
  }

  showDetails(alert: Alert) {
    this.selectedAlerts = [];
    this.selectedAlerts = [alert];
    this.saveRefreshState();
    this.router.navigateByUrl('/alerts-list(dialog:details/' + alert.source['source:type'] + '/' + alert.source.guid + ')');
  }

  saveRefreshState() {
    this.lastPauseRefreshValue = this.pauseRefresh;
    this.tryStopPolling();
  }


  showSavedSearches() {
    this.saveRefreshState();
    this.router.navigateByUrl('/alerts-list(dialog:saved-searches)');
  }

  showSaveSearch() {
    this.saveRefreshState();
    this.saveSearchService.setCurrentQueryBuilderAndTableColumns(this.queryBuilder, this.alertsColumns);
    this.router.navigateByUrl('/alerts-list(dialog:save-search)');
  }

  tryStartPolling() {
    if (!this.pauseRefresh) {
      this.tryStopPolling();
      this.refreshTimer = this.searchService.pollSearch(this.queryBuilder.searchRequest).subscribe(results => {
        this.setData(results);
        this.searchView(false);
      });
    }
  }

  tryStopPolling() {
    if (this.refreshTimer && !this.refreshTimer.closed) {
      this.refreshTimer.unsubscribe();
    }
  }

  updateConfigRowsSettings() {
    this.searchService.interval = this.refreshInterval;
  }

  updateSelectedAlertStatus(status: string) {
    for (let alert of this.selectedAlerts) {
      alert.status = status;
    }
  }
}
