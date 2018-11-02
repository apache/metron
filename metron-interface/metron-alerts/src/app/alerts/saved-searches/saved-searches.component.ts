
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
import { Component, OnInit } from '@angular/core';
import {Router} from '@angular/router';
import {forkJoin as observableForkJoin} from 'rxjs';

import {SaveSearchService} from '../../service/save-search.service';
import {SaveSearch} from '../../model/save-search';
import {NUM_SAVED_SEARCH} from '../../utils/constants';
import {CollapseComponentData, CollapseComponentDataItems} from '../../shared/collapse/collapse-component-data';
import { DialogService } from 'app/service/dialog.service';
import { ConfirmationType } from 'app/model/confirmation-type';

@Component({
  selector: 'app-saved-searches',
  templateUrl: './saved-searches.component.html',
  styleUrls: ['./saved-searches.component.scss']
})
export class SavedSearchesComponent implements OnInit {

  searches: SaveSearch[];
  recentSearcheObj: SaveSearch[];
  savedSearches: CollapseComponentData = new CollapseComponentData();
  recentSearches: CollapseComponentData = new CollapseComponentData();
  constructor(private router: Router,
              private saveSearchService: SaveSearchService,
              private dialogService: DialogService) {
  }

  doDeleteRecentSearch(selectedSearch: SaveSearch) {
    this.saveSearchService.deleteRecentSearch(selectedSearch).subscribe(() => {
        this.ngOnInit();
      },
      error => {
        this.ngOnInit();
      });
  }
  doDeleteSearch(selectedSearch: SaveSearch) {
    this.saveSearchService.deleteSavedSearch(selectedSearch).subscribe(() => {
      this.doDeleteRecentSearch(selectedSearch);
      this.ngOnInit();
    },
    error => {
      this.ngOnInit();
    });
  }

  deleteRecentSearch($event) {
    let selectedSearch = this.recentSearcheObj.find(
      savedSearch => savedSearch.name === $event.key
    );
    const confirmedSubscription = this.dialogService
      .launchDialog(
        'Do you wish to delete recent search ' + selectedSearch.name
      )
      .subscribe(action => {
        if (action === ConfirmationType.Confirmed) {
          this.doDeleteRecentSearch(selectedSearch);
        }
        confirmedSubscription.unsubscribe();
      });
  }

  deleteSearch($event) {
    let selectedSearch = this.searches.find(
      savedSearch => savedSearch.name === $event.key
    );
    const confirmedSubscription = this.dialogService
      .launchDialog('Do you wish to delete saved search ' + selectedSearch.name)
      .subscribe(action => {
        if (action === ConfirmationType.Confirmed) {
          this.doDeleteSearch(selectedSearch);
        }
        confirmedSubscription.unsubscribe();
      });
  }

  ngOnInit() {
    observableForkJoin(
      this.saveSearchService.listSavedSearches(),
      this.saveSearchService.listRecentSearches()
    ).subscribe((response: any) => {
      this.prepareData(response[0]);
      this.preparedRecentlyAccessedSearches(response[1]);
    });
  }

  prepareData(savedSearches: SaveSearch[]) {
    savedSearches = savedSearches || [];
    this.preparedSavedSearches(savedSearches);
    this.searches = savedSearches;
  }

  preparedRecentlyAccessedSearches(recentSearches: SaveSearch[]) {
    this.recentSearcheObj = recentSearches ? recentSearches : [];
    let recentSearchNames = this.recentSearcheObj.sort((s1, s2) => { return s2.lastAccessed - s1.lastAccessed; }).slice(0, NUM_SAVED_SEARCH)
                          .map(search => {
                            return  new CollapseComponentDataItems(search.getDisplayString());
                          });

    this.recentSearches.groupName =  'Recent Searches';
    this.recentSearches.groupItems = recentSearchNames;
  }

  preparedSavedSearches(savedSearches: SaveSearch[]) {
    let savedSearchNames = savedSearches.map(savedSearch => { return new CollapseComponentDataItems(savedSearch.name); });

    this.savedSearches.groupName =  'Saved Searches';
    this.savedSearches.groupItems = savedSearchNames;
  }

  goBack() {
    this.router.navigateByUrl('/alerts-list');
    return false;
  }

  onSaveRecentSearchSelect($event) {
    let selectedSearch = this.recentSearcheObj.find(savedSearch =>  savedSearch.name === $event.key);
    this.saveSearchService.fireLoadSavedSearch(SaveSearch.fromJSON(selectedSearch));
    this.goBack();
  }

  onSaveSearchSelect($event) {
    let selectedSearch = this.searches.find(savedSearch => savedSearch.name === $event.key);
    this.saveSearchService.fireLoadSavedSearch(SaveSearch.fromJSON(selectedSearch));
    this.goBack();
  }
}
