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
import { Component, OnInit } from '@angular/core';
import {Router} from '@angular/router';

import {MetaAlertService} from '../../service/meta-alert.service';
import {UpdateService} from '../../service/update.service';
import {SearchRequest} from '../../model/search-request';
import {SearchService} from '../../service/search.service';
import {SearchResponse} from '../../model/search-response';
import {SortField} from '../../model/sort-field';
import {META_ALERTS_INDEX, META_ALERTS_SENSOR_TYPE} from '../../utils/constants';
import {AlertSource} from '../../model/alert-source';
import {PatchRequest} from '../../model/patch-request';
import {Patch} from '../../model/patch';

@Component({
  selector: 'app-meta-alerts',
  templateUrl: './meta-alerts.component.html',
  styleUrls: ['./meta-alerts.component.scss']
})
export class MetaAlertsComponent implements OnInit {

  selectedMetaAlert = '';
  searchResponse: SearchResponse = new SearchResponse();

  constructor(private router: Router,
              private metaAlertService: MetaAlertService,
              private updateService: UpdateService,
              private searchService: SearchService) {
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
    //searchRequest.fields = [];

    this.searchService.search(searchRequest).subscribe(resp => this.searchResponse = resp);
  }

  doAddAlertToMetaAlert(alertSources: AlertSource[]) {
    let patchRequest = new PatchRequest();
    patchRequest.guid = this.selectedMetaAlert;
    patchRequest.sensorType = 'metaalert';
    patchRequest.index = META_ALERTS_INDEX;
    patchRequest.patch = [new Patch('replace', 'alert', alertSources)];

    this.updateService.patch(patchRequest).subscribe(rep => {
      console.log('Meta alert saved');
      this.goBack();
    });
  }

  addAlertToMetaAlert() {
    let searchRequest = new SearchRequest();
    searchRequest.query = 'guid:"' + this.selectedMetaAlert + '"';
    searchRequest.from = 0;
    searchRequest.size = 1;
    searchRequest.facetFields = [];
    searchRequest.indices =  [META_ALERTS_SENSOR_TYPE];
    searchRequest.sort = [];
    searchRequest.fields = [];

    this.searchService.search(searchRequest).subscribe((searchResponse: SearchResponse) => {
      if (searchResponse.results.length === 1) {
        let allAlertsInMetaAlerts = [...searchResponse.results[0].source.alert,
                                                  ...this.metaAlertService.selectedAlerts.map(alert => alert.source)];
        this.doAddAlertToMetaAlert(allAlertsInMetaAlerts);
      } else {
        console.log('Unable to get a single meta alert');
      }
    });
    console.log(this.selectedMetaAlert);
  }

}
