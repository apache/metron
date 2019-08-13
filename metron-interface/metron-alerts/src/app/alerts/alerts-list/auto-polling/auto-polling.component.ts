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
import { Component, NgZone, Output } from '@angular/core';
import { SearchService } from 'app/service/search.service';
import { QueryBuilder } from '../query-builder';
import { Subscription, Observable, interval } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { SearchRequest } from 'app/model/search-request';
import { SearchResponse } from 'app/model/search-response';
import { EventEmitter } from '@angular/core';

@Component({
  selector: 'app-auto-polling',
  templateUrl: './auto-polling.component.html',
  styleUrls: ['./auto-polling.component.scss']
})
export class AutoPollingComponent {
  @Output() onRefresh = new EventEmitter<SearchResponse>();

  showNotification = false;

  interval = 8;

  refreshTimer: Subscription;
  isRefreshPaused = true;

  constructor(private ngZone: NgZone,
              private searchService: SearchService,
              private queryBuilder: QueryBuilder) {}

  onPausePlay() {
    this.isRefreshPaused = !this.isRefreshPaused;

    if (this.isRefreshPaused) {
      this.tryStopPolling();
    } else {
      // this.search(false);
      this.tryStartPolling();
    }
  }

  pause() {
    this.isRefreshPaused = true;
    this.tryStopPolling();
  }

  resume() {
    this.isRefreshPaused = false;
    this.tryStartPolling();
  }

  tryStartPolling() {
    if (!this.isRefreshPaused) {
      this.tryStopPolling();
      this.refreshTimer = this.pollSearch(this.queryBuilder.searchRequest).subscribe(results => {
        // this.setData(results);
        this.onRefresh.emit(results);
      });
    }
  }

  pollSearch(searchRequest: SearchRequest): Observable<SearchResponse> {

    return interval(this.interval * 1000).pipe(switchMap(() => {
      return this.searchService.search(searchRequest);
    }));

    // return this.ngZone.runOutsideAngular(() => {
    //   return this.ngZone.run(() => {
    //     return interval(this.interval * 1000).pipe(switchMap(() => {
    //       return this.searchService.search(searchRequest);
    //     }));
    //   });
    // });
  }

  tryStopPolling() {
    if (this.refreshTimer && !this.refreshTimer.closed) {
      this.refreshTimer.unsubscribe();
    }
  }
}
