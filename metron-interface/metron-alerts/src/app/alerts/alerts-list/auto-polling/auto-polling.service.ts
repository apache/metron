import { Injectable } from '@angular/core';
import { Subscription, Subject, Observable, interval } from 'rxjs';
import { SearchService } from 'app/service/search.service';
import { QueryBuilder } from '../query-builder';
import { SearchResponse } from 'app/model/search-response';
import { SearchRequest } from 'app/model/search-request';
import { switchMap, filter, takeWhile } from 'rxjs/operators';

@Injectable()
export class AutoPollingService {
  isPollingActive = false;
  data = new Subject<SearchResponse>();

  private interval = 8;
  private isPollingSuppressed = false;
  private pollingIntervalSubs: Subscription;

  // TODO implement congestion handling
  private isCongestion = false;

  constructor(private searchService: SearchService,
              private queryBuilder: QueryBuilder) {}

  toggle() {
    if (!this.isPollingActive) {
      this.activate();
    }

    this.isPollingActive = !this.isPollingActive;
  }

  setSuppression(value: boolean) {
    this.isPollingSuppressed = true;
  }

  setInterval(seconds: number) {
    this.interval = seconds;

    if (this.isPollingActive) {
      this.reset();
    }
  }

  private reset() {
    this.pollingIntervalSubs.unsubscribe();
    this.activate();
  }

  private activate() {
    this.pollingIntervalSubs = this.pollData(this.queryBuilder.searchRequest).subscribe(results => {
      this.data.next(results);
    });
  }

  private pollData(searchRequest: SearchRequest): Observable<SearchResponse> {
    return interval(this.interval * 1000).pipe(
      filter(() => !this.isPollingSuppressed),
      takeWhile(() => this.isPollingActive),
      switchMap(() => {
        return this.searchService.search(this.queryBuilder.searchRequest);
      }));
  }

  onDestroy() {
    this.isPollingActive = false;
    this.pollingIntervalSubs.unsubscribe();
  }
}
