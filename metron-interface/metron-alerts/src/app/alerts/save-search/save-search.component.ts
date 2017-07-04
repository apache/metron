import { Component, OnInit } from '@angular/core';
import {Router} from '@angular/router';

import {SaveSearchService} from '../../service/save-search.service';
import {SaveSearch} from '../../model/save-search';
import {MetronDialogBox} from '../../shared/metron-dialog-box';

@Component({
  selector: 'app-save-search',
  templateUrl: './save-search.component.html',
  styleUrls: ['./save-search.component.scss']
})
export class SaveSearchComponent implements OnInit {

  saveSearch = new SaveSearch();

  constructor(private router: Router,
              private saveSearchService: SaveSearchService,
              private metronDialogBox: MetronDialogBox) {
  }

  goBack() {
    this.router.navigateByUrl('/alerts-list');
    return false;
  }

  ngOnInit() {
  }

  save() {
    this.saveSearch.searchRequest = this.saveSearchService.queryBuilder.searchRequest;
    this.saveSearch.tableColumns = this.saveSearchService.tableColumns;

    this.saveSearchService.saveSearch(this.saveSearch).subscribe(() => {
      this.goBack();
    }, error => {
    });
  }

  trySave() {
    this.saveSearchService.listSavedSearches().subscribe((savedSearches: SaveSearch[]) => {
      if (savedSearches && savedSearches.length > 0 && savedSearches.find(savedSearch => savedSearch.name === this.saveSearch.name)) {
        this.update();
      } else {
        this.save();
      }
    });
  }

  update() {
    let message = 'A Search with the name \'' + this.saveSearch.name + '\' already exist do you wish to override it?';
    this.metronDialogBox.showConfirmationMessage(message).subscribe(result => {
      if (result) {
        this.saveSearch.searchRequest = this.saveSearchService.queryBuilder.searchRequest;
        this.saveSearchService.updateSearch(this.saveSearch).subscribe(() => { this.goBack(); }, error => {});
      }
    });
  }

}
