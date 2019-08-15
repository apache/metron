import { Injectable, NgZone } from '@angular/core';
import { Subscription, Subject, Observable, interval } from 'rxjs';
import { SearchService } from 'app/service/search.service';
import { QueryBuilder } from '../query-builder';
import { SearchResponse } from 'app/model/search-response';
import { SearchRequest } from 'app/model/search-request';
import { switchMap, filter } from 'rxjs/operators';

@Injectable()
export class AutoPollingService {
  data = new Subject<SearchResponse>();

  interval = 8;

  refreshTimer: Subscription;
  isRefreshPaused = true; // TODO rename this to isPollingActive, be avare of the logic turned

  private isPollingSupressed = false;

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

  silentPause() {
    this.isPollingSupressed = true;
  }

  silentResume() {
    this.isPollingSupressed = false;
  }

  tryStartPolling() {
    if (!this.isRefreshPaused) {
      this.tryStopPolling();
      // TODO rename refreshTimer bc it's not a timer
      this.refreshTimer = this.pollSearch(this.queryBuilder.searchRequest).subscribe(results => {
        this.data.next(results);
      });
    }
  }

  pollSearch(searchRequest: SearchRequest): Observable<SearchResponse> {

    return interval(this.interval * 1000).pipe(
      filter(() => !this.isPollingSupressed),
      switchMap(() => {
        return this.searchService.search(this.queryBuilder.searchRequest);
      }));

    // TODO choose which way to go
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
