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

import { Component, OnInit, OnChanges, SimpleChanges, OnDestroy, Input, EventEmitter, Output } from '@angular/core';
import {Subscription, Observable} from 'rxjs';

import {TableViewComponent} from '../table-view/table-view.component';
import {SearchResponse} from '../../../model/search-response';
import {SearchService} from '../../../service/search.service';
import {TreeGroupData, TreeAlertsSubscription} from './tree-group-data';
import {GroupResponse} from '../../../model/group-response';
import {GroupResult} from '../../../model/group-result';
import {SortField} from '../../../model/sort-field';
import {Sort} from '../../../utils/enums';
import {ElasticsearchUtils} from '../../../utils/elasticsearch-utils';
import {SearchRequest} from '../../../model/search-request';
import {MetaAlertCreateRequest} from '../../../model/meta-alert-create-request';
import {MetaAlertService} from '../../../service/meta-alert.service';
import {INDEXES, MAX_ALERTS_IN_META_ALERTS} from '../../../utils/constants';
import {UpdateService} from '../../../service/update.service';
import {GetRequest} from '../../../model/get-request';
import { GlobalConfigService } from '../../../service/global-config.service';
import { DialogService } from '../../../service/dialog.service';
import { DialogType } from 'app/model/dialog-type';
import { ConfirmationType } from 'app/model/confirmation-type';
import { AlertSource } from '../../../model/alert-source';
import { QueryBuilder } from '../query-builder';
import { GroupRequest } from 'app/model/group-request';
import { Group } from 'app/model/group';
import { TimezoneConfigService } from 'app/alerts/configure-rows/timezone-config/timezone-config.service';

@Component({
  selector: 'app-tree-view',
  templateUrl: './tree-view.component.html',
  styleUrls: ['./tree-view.component.scss']
})

export class TreeViewComponent extends TableViewComponent implements OnInit, OnChanges, OnDestroy {

  @Input() globalConfig: {} = {};
  @Input() query = '';
  @Input() groups: string[] = [];

  @Output() onMetaAlertCreated = new EventEmitter<boolean>();

  @Output() treeViewChange = new EventEmitter<number>();
  groupByFields: string[] = [];
  topGroups: TreeGroupData[] = [];
  groupResponse: GroupResponse = new GroupResponse();
  treeGroupSubscriptionMap: {[key: string]: TreeAlertsSubscription } = {};
  alertsChangedSubscription: Subscription;
  configSubscription: Subscription;
  dialogService: DialogService;
  subgroupTotalAlerts = 0;

  constructor(searchService: SearchService,
              updateService: UpdateService,
              metaAlertService: MetaAlertService,
              globalConfigService: GlobalConfigService,
              dialogService: DialogService,
              timezoneConfigService: TimezoneConfigService, ) {
    super(searchService, updateService, metaAlertService, globalConfigService, dialogService, timezoneConfigService);
  }

  addAlertChangedListner() {
    this.alertsChangedSubscription = this.updateService.alertChanged$.subscribe(alertSource => {
      this.updateAlert(alertSource);
    });
  }

  removeAlertChangedLister() {
    this.alertsChangedSubscription.unsubscribe();
  }

  collapseGroup(groupArray: TreeGroupData[], level: number, index: number) {
    for (let i = index + 1; i < groupArray.length; i++) {
      if (groupArray[i].level > (level)) {
        groupArray[i].show = false;
        groupArray[i].expand = false;
      } else {
        break;
      }
    }
  }

  createQuery(selectedGroup: TreeGroupData) {
    let searchQuery = this.query;
    let groupQery = Object.keys(selectedGroup.groupQueryMap).map(key => {
      return key.replace(/:/g, '\\:') +
          ':' +
          String(selectedGroup.groupQueryMap[key])
          .replace(/[\*\+\-=~><\"\?^\${}\(\)\:\!\/[\]\\\s]/g, '\\$&') // replace single  special characters
          .replace(/\|\|/g, '\\||') // replace ||
          .replace(/\&\&/g, '\\&&'); // replace &&
    }).join(' AND ');

    groupQery += searchQuery === '*' ? '' : (' AND ' + searchQuery);
    return groupQery;
  }

  expandGroup(groupArray: TreeGroupData[], level: number, index: number) {
    for (let i = index + 1; i < groupArray.length; i++) {
      if (groupArray[i].level === (level + 1)) {
        groupArray[i].show = true;
      } else {
        break;
      }
    }
  }

  getAlerts(selectedGroup: TreeGroupData): Subscription {
    let searchRequest = new SearchRequest();
    searchRequest.query = this.createQuery(selectedGroup);
    searchRequest.from = selectedGroup.pagingData.from;
    searchRequest.size = selectedGroup.pagingData.size;
    searchRequest.sort = selectedGroup.sortField ? [selectedGroup.sortField] : [];

    return this.searchGroup(selectedGroup, searchRequest);
  }

  getGroups() {
    this.searchService.groups(this.getGroupRequest()).subscribe(groupResponse => {
      this.updateGroupData(groupResponse);
    });
  }

  updateGroupData(groupResponse) {
    this.selectedAlerts = [];
    this.groupResponse = groupResponse;
    this.parseTopLevelGroup();
  }

  groupPageChange(group: TreeGroupData) {
    this.getAlerts(group);
  }

  createTopGroups(groupByFields: string[]) {
    this.topGroups = [];
    this.treeGroupSubscriptionMap = {};

    this.groupResponse.groupResults.forEach((groupResult: GroupResult) => {
      let treeGroupData = new TreeGroupData(groupResult.key, groupResult.total, groupResult.score, 0, false);
      treeGroupData.isLeafNode = (groupByFields.length === 1);

      if (groupByFields.length === 1) {
        treeGroupData.groupQueryMap  = this.createTopGroupQueryMap(groupByFields[0], groupResult);
      }

      this.topGroups.push(treeGroupData);
    });
  }

  createTopGroupQueryMap(groupByFields: string, groupResult: GroupResult) {
    let groupQueryMap = {};
    groupQueryMap[groupByFields] = groupResult.key;
    return groupQueryMap;
  }

  initTopGroups() {
    let groupByFields =  this.groups;
    let currentTopGroupKeys = this.groupResponse.groupResults.map(groupResult => groupResult.key);
    let previousTopGroupKeys = this.topGroups.map(group => group.key);

    if (this.topGroups.length === 0 || JSON.stringify(this.groupByFields) !== JSON.stringify(groupByFields) ||
    JSON.stringify(currentTopGroupKeys) !== JSON.stringify(previousTopGroupKeys)) {
      this.createTopGroups(groupByFields);
    }

    this.subgroupTotalAlerts = this.topGroups.reduce((accumulator, currentValue) => {
      return accumulator + currentValue.total;
    }, 0);
    this.treeViewChange.next(this.subgroupTotalAlerts);
    this.groupByFields = groupByFields;
  }

  search(resetPaginationParams = true, pageSize: number = null) {
    this.getGroups();
  }

  ngOnChanges(changes: SimpleChanges) {
    if ((changes['alerts'] && changes['alerts'].currentValue)) {
      this.search();
    }
  }

  ngOnInit() {
    this.addAlertChangedListner();
  }

  ngOnDestroy(): void {
    this.removeAlertChangedLister();
    this.treeViewChange.next(0);
  }

  searchGroup(selectedGroup: TreeGroupData, searchRequest: SearchRequest): Subscription {
    return this.searchService.search(searchRequest).subscribe(results => {
      this.setData(selectedGroup, results);
    }, error => {
      this.dialogService.launchDialog(ElasticsearchUtils.extractESErrorMessage(error), DialogType.Error);
    });
  }

  setData(selectedGroup: TreeGroupData, results: SearchResponse) {
    selectedGroup.response.results = results.results;
    selectedGroup.pagingData.total = results.total;
    selectedGroup.total = results.total;

    this.topGroups.map(topGroup => {
      if (topGroup.treeSubGroups.length > 0) {
        topGroup.total = topGroup.treeSubGroups.reduce((total, subGroup) => { return total + subGroup.total; }, 0);
      }
    });
  }

  checkAndToSubscription(group: TreeGroupData) {
    if (group.isLeafNode) {
      let key = JSON.stringify(group.groupQueryMap);
      if (this.treeGroupSubscriptionMap[key]) {
        this.removeFromSubscription(group);
      }

      let subscription = this.getAlerts(group);
      this.treeGroupSubscriptionMap[key] = new TreeAlertsSubscription(subscription, group);
    }
  }

  removeFromSubscription(group: TreeGroupData) {
    if (group.isLeafNode) {
      let key = JSON.stringify(group.groupQueryMap);
      let subscription = this.treeGroupSubscriptionMap[key].refreshTimer;
      if (subscription && !subscription.closed) {
        subscription.unsubscribe();
      }
      delete this.treeGroupSubscriptionMap[key];
    }
  }

  toggleSubGroups(topLevelGroup: TreeGroupData, selectedGroup: TreeGroupData, index: number) {
    selectedGroup.expand = !selectedGroup.expand;

    if (selectedGroup.expand) {
      this.expandGroup(topLevelGroup.treeSubGroups, selectedGroup.level, index);
      this.checkAndToSubscription(selectedGroup);
    } else {
      this.collapseGroup(topLevelGroup.treeSubGroups, selectedGroup.level, index);
      this.removeFromSubscription(selectedGroup);
    }
  }

  toggleTopLevelGroup(group: TreeGroupData) {
    group.expand = !group.expand;
    group.show = !group.show;

    if (group.expand) {
      this.checkAndToSubscription(group);
    } else {
      this.removeFromSubscription(group);
    }
  }

  parseSubGroups(group: GroupResult, groupAsArray: TreeGroupData[],
                 parentQueryMap: {[key: string]: string}, currentGroupKey: string, level: number, index: number): number {
    index++;

    let currentTreeNodeData = (groupAsArray.length > 0) ? groupAsArray[index] : null;

    if (currentTreeNodeData && (currentTreeNodeData.key === group.key) && (currentTreeNodeData.level === level)) {
      currentTreeNodeData.total = group.total;
    } else {
      let newTreeNodeData = new TreeGroupData(group.key, group.total, group.score, level, level === 1);
      if (!currentTreeNodeData) {
        groupAsArray.push(newTreeNodeData);
      } else {
        groupAsArray.splice(index, 1, newTreeNodeData);
      }
    }

    groupAsArray[index].isLeafNode = false;
    groupAsArray[index].groupQueryMap = JSON.parse(JSON.stringify(parentQueryMap));
    groupAsArray[index].groupQueryMap[currentGroupKey] = group.key;

    if (!group.groupResults) {
      groupAsArray[index].isLeafNode = true;
      if (groupAsArray[index].expand && groupAsArray[index].show && groupAsArray[index].groupQueryMap) {
        this.checkAndToSubscription(groupAsArray[index]);
      }
      return index;
    }

    group.groupResults.forEach(subGroup => {
      index = this.parseSubGroups(subGroup, groupAsArray, groupAsArray[index].groupQueryMap, group.groupedBy, level + 1, index);
    });

    return index;
  }

  parseTopLevelGroup() {
    let groupedBy = this.groupResponse.groupedBy;

    this.initTopGroups();

    for (let i = 0; i < this.groupResponse.groupResults.length; i++) {
      let index = -1;
      let topGroup = this.topGroups[i];
      let resultGroup = this.groupResponse.groupResults[i];

      topGroup.total = resultGroup.total;
      topGroup.groupQueryMap = this.createTopGroupQueryMap(groupedBy, resultGroup);

      if (resultGroup.groupResults) {
        resultGroup.groupResults.forEach(subGroup => {
          index = this.parseSubGroups(subGroup, topGroup.treeSubGroups, topGroup.groupQueryMap, resultGroup.groupedBy, 1, index);
        });

        topGroup.treeSubGroups.splice(index + 1);
      }
    }

    if (this.groupByFields.length === 1) {
      this.refreshAllExpandedGroups();
    }
  }

  sortTreeSubGroup($event, treeGroup: TreeGroupData) {
    let sortBy = $event.sortBy === 'id' ? '_uid' : $event.sortBy;
    let sortOrder = $event.sortOrder === Sort.ASC ? 'asc' : 'desc';
    let sortField = new SortField(sortBy, sortOrder);

    treeGroup.sortEvent = $event;
    treeGroup.sortField = sortField;
    treeGroup.treeSubGroups.forEach(treeSubGroup => treeSubGroup.sortField = sortField);

    this.refreshAllExpandedGroups();
  }

  selectAllGroupRows($event, group: TreeGroupData) {
    this.selectedAlerts = [];

    if ($event.target.checked) {
      if (group.expand && group.show && group.response) {
        this.selectedAlerts = group.response.results;
      }

      group.treeSubGroups.forEach(subGroup => {
        if (subGroup.expand && subGroup.show && subGroup.response) {
          this.selectedAlerts = this.selectedAlerts.concat(subGroup.response.results);
        }
      });
    }

    this.onSelectedAlertsChange.emit(this.selectedAlerts);
  }

  refreshAllExpandedGroups() {
    Object.keys(this.treeGroupSubscriptionMap).forEach(key => {
      this.getAlerts(this.treeGroupSubscriptionMap[key].group);
    });
  }

  canCreateMetaAlert(count: number) {
    if (count > MAX_ALERTS_IN_META_ALERTS) {
      let errorMessage = 'Meta Alert cannot have more than ' + MAX_ALERTS_IN_META_ALERTS + ' alerts within it';
      this.dialogService.launchDialog(errorMessage, DialogType.Error);
      return false;
    }
    return true;
  }

  createGetRequestArray(searchResponse: SearchResponse): any {
    return searchResponse.results.map(alert =>
      new GetRequest(alert.source.guid, alert.source[this.globalConfig['source.type.field']], alert.index));
  }

  getAllAlertsForSlectedGroup(group: TreeGroupData): Observable<SearchResponse> {
    let searchRequest = new SearchRequest();
    searchRequest.fields = ['guid', this.globalConfig['source.type.field']];
    searchRequest.from = 0;
    searchRequest.indices = INDEXES;
    searchRequest.query = this.createQuery(group);
    searchRequest.size =  MAX_ALERTS_IN_META_ALERTS;
    searchRequest.facetFields =  [];
    return this.searchService.search(searchRequest);
  }

  doCreateMetaAlert(group: TreeGroupData, index: number) {
    this.getAllAlertsForSlectedGroup(group).subscribe((searchResponse: SearchResponse) => {
      if (this.canCreateMetaAlert(searchResponse.total)) {
        let metaAlert = new MetaAlertCreateRequest();
        metaAlert.alerts = this.createGetRequestArray(searchResponse);
        metaAlert.groups = this.groups;

        this.metaAlertService.create(metaAlert).subscribe(() => {
          setTimeout(() => this.onMetaAlertCreated.emit(true), 1000);
          console.log('Meta alert created successfully');
        });
      }
    });
  }

  getGroupRequest(): GroupRequest {
    const req = new GroupRequest();
    req.groups = this.groups.map(groupName => new Group(groupName));
    req.query = this.query;
    req.scoreField = this.threatScoreFieldName();
    return req;
  }

  createMetaAlert($event, group: TreeGroupData, index: number) {
    if (this.canCreateMetaAlert(group.total)) {
      let confirmationMsg = 'Do you wish to create a meta alert with ' +
                            (group.total === 1 ? ' alert' : group.total + ' selected alerts') + '?';
      const confirmedSubscription = this.dialogService.launchDialog(confirmationMsg).subscribe(action => {
        if (action === ConfirmationType.Confirmed) {
          this.doCreateMetaAlert(group, index);
        }
        confirmedSubscription.unsubscribe();
      });
    }

    $event.stopPropagation();
    return false;
  }

  updateAlert(alertSource: AlertSource) {
    Object.keys(this.treeGroupSubscriptionMap).forEach(key => {
      let group = this.treeGroupSubscriptionMap[key].group;
      if (group.response && group.response.results && group.response.results.length > 0) {
        group.response.results.filter(alert => alert.source.guid === alertSource.guid)
        .forEach(alert => alert.source = alertSource);
      }
    });
  }
}
