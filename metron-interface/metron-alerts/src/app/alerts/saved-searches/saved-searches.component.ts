import { Component, OnInit } from '@angular/core';
import {Router} from '@angular/router';
import {Observable} from 'rxjs/Rx';

import {SaveSearchService} from '../../service/save-search.service';
import {SaveSearch} from '../../model/save-search';
import {MetronDialogBox} from '../../shared/metron-dialog-box';
import {NUM_SAVED_SEARCH} from '../../utils/constants';
import {CollapseComponentData, CollapseComponentDataItems} from '../../shared/collapse/collapse-component-data';

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
              private metronDialog: MetronDialogBox) {
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
    let selectedSearch = this.recentSearcheObj.find(savedSearch => savedSearch.name === $event.key);
    this.metronDialog.showConfirmationMessage('Do you wish to delete recent search ' + selectedSearch.name).subscribe((result: boolean) => {
      if (result) {
        this.doDeleteRecentSearch(selectedSearch);
      }
    });
  }

  deleteSearch($event) {
    let selectedSearch = this.searches.find(savedSearch => savedSearch.name === $event.key);
    this.metronDialog.showConfirmationMessage('Do you wish to delete saved search ' + selectedSearch.name).subscribe((result: boolean) => {
      if (result) {
        this.doDeleteSearch(selectedSearch);
      }
    });
  }

  ngOnInit() {
    Observable.forkJoin(
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
