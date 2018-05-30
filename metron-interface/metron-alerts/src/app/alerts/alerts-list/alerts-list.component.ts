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
import {Component, OnInit, ViewChild, ElementRef, OnDestroy} from '@angular/core';
import {Router, NavigationStart} from '@angular/router';
import {Observable, Subscription} from 'rxjs/Rx';

import {Alert} from '../../model/alert';
import {SearchService} from '../../service/search.service';
import {UpdateService} from '../../service/update.service';
import {QueryBuilder} from './query-builder';
import {ConfigureTableService} from '../../service/configure-table.service';
import {AlertsService} from '../../service/alerts.service';
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
import {Filter} from '../../model/filter';
import {THREAT_SCORE_FIELD_NAME, TIMESTAMP_FIELD_NAME, ALL_TIME} from '../../utils/constants';
import {TableViewComponent} from './table-view/table-view.component';
import {Pagination} from '../../model/pagination';
import {META_ALERTS_SENSOR_TYPE, META_ALERTS_INDEX} from '../../utils/constants';
import {MetaAlertService} from '../../service/meta-alert.service';
import {Facets} from '../../model/facets';
import { GlobalConfigService } from '../../service/global-config.service';

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
  refreshTimer: Subscription;
  pauseRefresh = false;
  lastPauseRefreshValue = false;
  isMetaAlertPresentInSelectedAlerts = false;
  timeStampfilterPresent = false;
  selectedTimeRange = new Filter(TIMESTAMP_FIELD_NAME, ALL_TIME, false);
  threatScoreFieldName = THREAT_SCORE_FIELD_NAME;

  @ViewChild('table') table: ElementRef;
  @ViewChild('dataViewComponent') dataViewComponent: TableViewComponent;
  @ViewChild(AlertSearchDirective) alertSearchDirective: AlertSearchDirective;

  tableMetaData = new TableMetadata();
  queryBuilder: QueryBuilder = new QueryBuilder();
  pagination: Pagination = new Pagination();
  alertChangedSubscription: Subscription;
  groupFacets: Facets;
  globalConfig: {} = {};
  configSubscription: Subscription;

  constructor(private router: Router,
              private searchService: SearchService,
              private updateService: UpdateService,
              private configureTableService: ConfigureTableService,
              private alertsService: AlertsService,
              private clusterMetaDataService: ClusterMetaDataService,
              private saveSearchService: SaveSearchService,
              private metronDialogBox: MetronDialogBox,
              private metaAlertsService: MetaAlertService,
              private globalConfigService: GlobalConfigService) {
    router.events.subscribe(event => {
      if (event instanceof NavigationStart && event.url === '/alerts-list') {
        this.selectedAlerts = [];
        this.restoreRefreshState();
      }
    });
  }

  addAlertChangedListner() {
    this.metaAlertsService.alertChanged$.subscribe(metaAlertAddRemoveRequest => {
      this.updateAlert(META_ALERTS_SENSOR_TYPE, metaAlertAddRemoveRequest.metaAlertGuid, (metaAlertAddRemoveRequest.alerts === null));
    });

    this.alertChangedSubscription = this.updateService.alertChanged$.subscribe(patchRequest => {
      this.updateAlert(patchRequest.sensorType, patchRequest.guid, false);
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
      queryBuilder.setGroupby(this.queryBuilder.groupRequest.groups.map(group => group.field));
      queryBuilder.searchRequest = savedSearch.searchRequest;
      queryBuilder.filters = savedSearch.filters;
      this.queryBuilder = queryBuilder;
      this.setSelectedTimeRange(savedSearch.filters);
      this.prepareColumnData(savedSearch.tableColumns, []);
      this.timeStampfilterPresent = this.queryBuilder.isTimeStampFieldPresent();
      this.search(true, savedSearch);
    });
  }

  setSelectedTimeRange(filters: Filter[]) {
    filters.forEach(filter => {
      if (filter.field === TIMESTAMP_FIELD_NAME && filter.dateFilterValue) {
        this.selectedTimeRange = JSON.parse(JSON.stringify(filter));
      }
    });
  }

  calcColumnsToDisplay() {
    let availableWidth = document.documentElement.clientWidth - (200 + (15 + 15 + 25)); /* screenwidth - (navPaneWidth + (paddings))*/
    availableWidth = availableWidth - ((20 * 3) + 55 + 25); /* availableWidth - (score + colunSelectIcon +selectCheckbox )*/
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
    this.removeAlertChangedListner();
    this.configSubscription.unsubscribe();
  }

  ngOnInit() {
    this.configSubscription = this.globalConfigService.get().subscribe((config: {}) => {
      this.globalConfig = config;
      if (this.globalConfig['source.type.field']) {
        let filteredAlertsColumns = this.alertsColumns.filter(colName => colName.name !== 'source:type');
        if (filteredAlertsColumns.length < this.alertsColumns.length) {
          this.alertsColumns = filteredAlertsColumns;
          this.alertsColumns.splice(2, 0, new ColumnMetadata(this.globalConfig['source.type.field'], 'string'));
        }
      }
    });
    this.getAlertColumnNames(true);
    this.addAlertColChangedListner();
    this.addLoadSavedSearchListner();
    this.addAlertChangedListner();
  }

  onClear() {
    this.timeStampfilterPresent = false;
    this.queryBuilder.clearSearch();
    this.selectedTimeRange = new Filter(TIMESTAMP_FIELD_NAME, ALL_TIME, false);
    this.search();
  }

  onSearch($event) {
    this.queryBuilder.setSearch($event);
    this.timeStampfilterPresent = this.queryBuilder.isTimeStampFieldPresent();
    this.search();
    return false;
  }

  onAddFacetFilter($event) {
    this.onAddFilter(new Filter($event.name, $event.key));
  }

  onRefreshData($event) {
    this.search($event);
  }

  onSelectedAlertsChange(selectedAlerts) {
    this.selectedAlerts = selectedAlerts;
    this.isMetaAlertPresentInSelectedAlerts = this.selectedAlerts.some(alert => (alert.source.alert && alert.source.alert.length > 0));

    if (selectedAlerts.length > 0) {
      this.pause();
    } else {
      this.resume();
    }
  }

  onAddFilter(filter: Filter) {
    this.timeStampfilterPresent = (filter.field === TIMESTAMP_FIELD_NAME);
    this.queryBuilder.addOrUpdateFilter(filter);
    this.search();
  }

  onConfigRowsChange() {
    this.searchService.interval = this.refreshInterval;
    this.search();
  }

  onGroupsChange(groups) {
    this.queryBuilder.setGroupby(groups);
    this.search();
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

  onTimeRangeChange(filter: Filter) {
    if (filter.value === ALL_TIME) {
      this.queryBuilder.removeFilter(filter.field);
    } else {
      this.queryBuilder.addOrUpdateFilter(filter);
    }

    this.search();
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
    this.updateService.updateAlertState(this.selectedAlerts, 'ESCALATE', false).subscribe(results => {
      this.updateSelectedAlertStatus('ESCALATE');
    });
  }

  processDismiss() {
    this.updateService.updateAlertState(this.selectedAlerts, 'DISMISS', false).subscribe(results => {
      this.updateSelectedAlertStatus('DISMISS');
    });
  }

  processOpen() {
    this.updateService.updateAlertState(this.selectedAlerts, 'OPEN', false).subscribe(results => {
      this.updateSelectedAlertStatus('OPEN');
    });
  }

  processResolve() {
    this.updateService.updateAlertState(this.selectedAlerts, 'RESOLVE', false).subscribe(results => {
      this.updateSelectedAlertStatus('RESOLVE');
    });
  }

  processAddToAlert() {
    this.metaAlertsService.selectedAlerts = this.selectedAlerts;
    this.router.navigateByUrl('/alerts-list(dialog:add-to-meta-alert)');
  }

  removeFilter(field: string) {
    this.timeStampfilterPresent = (field === TIMESTAMP_FIELD_NAME) ? false : this.timeStampfilterPresent;
    this.queryBuilder.removeFilter(field);
    this.search();
  }

  restoreRefreshState() {
    this.pauseRefresh = this.lastPauseRefreshValue;
    this.tryStartPolling();
  }

  search(resetPaginationParams = true, savedSearch?: SaveSearch) {
    this.saveCurrentSearch(savedSearch);
    if (resetPaginationParams) {
      this.pagination.from = 0;
    }

    this.setSearchRequestSize();

    this.searchService.search(this.queryBuilder.searchRequest).subscribe(results => {
      this.setData(results);
    }, error => {
      this.setData(new SearchResponse());
      this.metronDialogBox.showConfirmationMessage(ElasticsearchUtils.extractESErrorMessage(error), DialogType.Error);
    });

    this.tryStartPolling();
  }

  setSearchRequestSize() {
    if (this.queryBuilder.groupRequest.groups.length === 0) {
      this.queryBuilder.searchRequest.from = this.pagination.from;
      if (this.tableMetaData.size) {
        this.pagination.size = this.tableMetaData.size;
      }
      this.queryBuilder.searchRequest.size = this.pagination.size;
    } else {
      this.queryBuilder.searchRequest.from = 0;
      this.queryBuilder.searchRequest.size = 0;
    }
  }

  saveCurrentSearch(savedSearch: SaveSearch) {
    if (this.queryBuilder.query !== '*') {
      if (!savedSearch) {
        savedSearch = new SaveSearch();
        savedSearch.searchRequest = this.queryBuilder.searchRequest;
        savedSearch.tableColumns = this.alertsColumns;
        savedSearch.filters = this.queryBuilder.filters;
        savedSearch.searchRequest.query = '';
        savedSearch.name = this.queryBuilder.generateNameForSearchRequest();
      }

      this.saveSearchService.saveAsRecentSearches(savedSearch).subscribe(() => {
      });
    }
  }

  setData(results: SearchResponse) {
    this.selectedAlerts = [];
    this.searchResponse = results;
    this.pagination.total = results.total;
    this.alerts = results.results ? results.results : [];
    this.setSelectedTimeRange(this.queryBuilder.filters);
    this.createGroupFacets(results);
  }

  private createGroupFacets(results: SearchResponse) {
    this.groupFacets = JSON.parse(JSON.stringify(results.facetCounts));
    if (this.groupFacets[this.globalConfig['source.type.field']]) {
      delete this.groupFacets[this.globalConfig['source.type.field']]['metaalert'];
    }
  }

  showConfigureTable() {
    this.saveRefreshState();
    this.router.navigateByUrl('/alerts-list(dialog:configure-table)');
  }

  showDetails(alert: Alert) {
    this.selectedAlerts = [];
    this.selectedAlerts = [alert];
    this.saveRefreshState();
    let sourceType = (alert.index === META_ALERTS_INDEX && !alert.source[this.globalConfig['source.type.field']])
        ? META_ALERTS_SENSOR_TYPE : alert.source[this.globalConfig['source.type.field']];
    let url = '/alerts-list(dialog:details/' + sourceType + '/' + alert.source.guid + '/' + alert.index + ')';
    this.router.navigateByUrl(url);
  }

  saveRefreshState() {
    this.lastPauseRefreshValue = this.pauseRefresh;
    this.tryStopPolling();
  }

  pause() {
    this.pauseRefresh = true;
    this.tryStopPolling();
  }

  resume() {
    this.pauseRefresh = false;
    this.tryStartPolling();
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
      this.refreshTimer = this.searchService.pollSearch(this.queryBuilder).subscribe(results => {
        this.setData(results);
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

  updateAlert(sensorType: string, guid: string, isDelete: boolean) {
    if (isDelete) {
      let alertIndex = -1;
      this.alerts.forEach((alert, index) => {
        alertIndex = (alert.source.guid === guid) ? index : alertIndex;
      });
      this.alerts.splice(alertIndex, 1);
      return;
    }

    this.searchService.getAlert(sensorType, guid).subscribe(alertSource => {
      this.alerts.filter(alert => alert.source.guid === guid)
      .map(alert => alert.source = alertSource);
    });
  }

  updateSelectedAlertStatus(status: string) {
    for (let selectedAlert of this.selectedAlerts) {
      selectedAlert.source['alert_status'] = status;
    }
    this.selectedAlerts = [];
    this.resume();
  }

  removeAlertChangedListner() {
    this.alertChangedSubscription.unsubscribe();
  }
}
