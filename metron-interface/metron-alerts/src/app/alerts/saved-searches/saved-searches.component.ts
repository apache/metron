import { Component, OnInit } from '@angular/core';
import {Router} from '@angular/router';
import {Observable} from 'rxjs/Rx';

import {SaveSearchService} from '../../service/save-search.service';
import {SaveSearch} from '../../model/save-search';
import {MetronDialogBox} from '../../shared/metron-dialog-box';

@Component({
  selector: 'app-saved-searches',
  templateUrl: './saved-searches.component.html',
  styleUrls: ['./saved-searches.component.scss']
})
export class SavedSearchesComponent implements OnInit {

  searches: SaveSearch[];
  recentSearcheObj: SaveSearch[];
  savedSearches: any = {};
  recentSearches: any = {};
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
    let recentSearchNames = this.recentSearcheObj.sort((s1, s2) => { return s2.lastAccessed - s1.lastAccessed; }).slice(0, 5)
                          .map(search => {
                            return {key: search.getDisplayString()};
                          });

    this.recentSearches = {
      getName: () => {
        return 'Recent Searches';
      },
      getData: () => {
        return recentSearchNames;
      },
    };
  }

  preparedSavedSearches(savedSearches: SaveSearch[]) {
    let savedSearchNames = savedSearches.map(savedSearch => { return {key: savedSearch.name}; });

    this.savedSearches = {
      getName: () => {
        return 'Saved Searches';
      },
      getData: () => {
        return savedSearchNames;
      },
    };
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
