/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * 'License'); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Component, OnInit, OnDestroy } from '@angular/core';
import {Router} from '@angular/router';
import { Subscription } from 'rxjs/Rx';

import {MetaAlertService} from '../../service/meta-alert.service';
import {UpdateService} from '../../service/update.service';
import {SearchRequest} from '../../model/search-request';
import {SearchService} from '../../service/search.service';
import {SearchResponse} from '../../model/search-response';
import {SortField} from '../../model/sort-field';
import { META_ALERTS_SENSOR_TYPE } from '../../utils/constants';
import {MetronDialogBox} from '../../shared/metron-dialog-box';
import {MetaAlertAddRemoveRequest} from '../../model/meta-alert-add-remove-request';
import {GetRequest} from '../../model/get-request';
import { GlobalConfigService } from '../../service/global-config.service';

@Component({
  selector: 'app-meta-alerts',
  templateUrl: './meta-alerts.component.html',
  styleUrls: ['./meta-alerts.component.scss']
})
export class MetaAlertsComponent implements OnInit, OnDestroy {

  selectedMetaAlert = '';
  searchResponse: SearchResponse = new SearchResponse();
  globalConfig: {} = {};
  configSubscription: Subscription;

  constructor(private router: Router,
              private metaAlertService: MetaAlertService,
              private updateService: UpdateService,
              private searchService: SearchService,
              private metronDialogBox: MetronDialogBox,
              private globalConfigService: GlobalConfigService) {
  }

  goBack() {
    this.router.navigateByUrl('/alerts-list');
    return false;
  }

  ngOnInit() {
    let searchRequest = new SearchRequest();
    searchRequest.query = '*';
    searchRequest.from = 0;
    searchRequest.size = 999;
    searchRequest.facetFields = [];
    searchRequest.indices =  [META_ALERTS_SENSOR_TYPE];
    searchRequest.sort = [new SortField('threat:triage:score', 'desc')];

    this.searchService.search(searchRequest).subscribe(resp => this.searchResponse = resp);
    this.configSubscription = this.globalConfigService.get().subscribe((config: {}) => {
      this.globalConfig = config;
    });
  }

  ngOnDestroy() {
    this.configSubscription.unsubscribe();
  }

  addAlertToMetaAlert() {
    let getRequest = this.metaAlertService.selectedAlerts.map(alert =>
          new GetRequest(alert.source.guid, alert.source[this.globalConfig['source.type.field']], alert.index));
    let metaAlertAddRemoveRequest = new MetaAlertAddRemoveRequest();
    metaAlertAddRemoveRequest.metaAlertGuid = this.selectedMetaAlert;
    metaAlertAddRemoveRequest.alerts = getRequest;

    this.metaAlertService.addAlertsToMetaAlert(metaAlertAddRemoveRequest).subscribe(() => {
      console.log('Meta alert saved');
      this.goBack();
    });
  }

}
