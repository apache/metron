import { Injectable, NgZone } from '@angular/core';
import { Subscription, Subject, Observable, interval } from 'rxjs';
import { SearchService } from 'app/service/search.service';
import { QueryBuilder } from '../query-builder';
import { SearchResponse } from 'app/model/search-response';
import { SearchRequest } from 'app/model/search-request';
import { switchMap } from 'rxjs/operators';

@Injectable()
export class AutoPollingService {

  data = new Subject<SearchResponse>();

  interval = 8;

  refreshTimer: Subscription;
  isRefreshPaused = true;

  constructor(private ngZone: NgZone,
    private searchService: SearchService,
    private queryBuilder: QueryBuilder) {}

  toggle() {
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
        this.data.next(results);
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
