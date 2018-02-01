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
    this.saveSearch.filters = this.saveSearchService.queryBuilder.filters;
    this.saveSearch.searchRequest.query = '';
    
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
