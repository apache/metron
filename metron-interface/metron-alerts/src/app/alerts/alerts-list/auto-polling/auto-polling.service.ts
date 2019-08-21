import { Injectable } from '@angular/core';
import { Subscription, Subject, Observable, interval } from 'rxjs';
import { SearchService } from 'app/service/search.service';
import { QueryBuilder } from '../query-builder';
import { SearchResponse } from 'app/model/search-response';
import { switchMap, filter, takeWhile, tap } from 'rxjs/operators';
import { POLLING_DEFAULT_STATE } from 'app/utils/constants';

@Injectable()
export class AutoPollingService {
  data = new Subject<SearchResponse>();

  private isCongestion = false;
  private interval = 10;
  private isPollingActive = POLLING_DEFAULT_STATE;
  private isPending = false;
  private isPollingSuppressed = false;
  private pollingIntervalSubs: Subscription;

  constructor(private searchService: SearchService,
              private queryBuilder: QueryBuilder) {}

  toggle() {
    if (!this.isPollingActive) {
      this.activate();
    }

    this.isPollingActive = !this.isPollingActive;
  }

  setSuppression(value: boolean) {
    this.isPollingSuppressed = value;
  }

  dropNextAndContinue() {
    this.reset();
  }

  setInterval(seconds: number) {
    this.interval = seconds;

    if (this.isPollingActive) {
      this.reset();
    }
  }

  getIsPollingActive() {
    return this.isPollingActive;
  }

  getIsCongestion() {
    return this.isCongestion
  }

  private reset() {
    if (this.pollingIntervalSubs) {
      this.pollingIntervalSubs.unsubscribe();
      this.isPending = false;
    }
    this.activate();
  }

  private activate() {
    this.pollingIntervalSubs = this.pollData().subscribe(results => {
      this.data.next(results);
      this.isPending = false;
    });
  }

  private pollData(): Observable<SearchResponse> {
    return interval(this.interval * 1000).pipe(
      tap(() => this.checkCongestionOnTick()),
      filter(() => !this.isPollingSuppressed && !this.isCongestion),
      takeWhile(() => this.isPollingActive),
      switchMap(() => {
        this.isPending = true;
        return this.searchService.search(this.queryBuilder.searchRequest);
      }));
  }

  private checkCongestionOnTick() {
    if (this.isPending) {
      this.isCongestion = true;
    } else {
      this.isCongestion = false;
    }
  }

  onDestroy() {
    this.isPollingActive = false;
    this.pollingIntervalSubs.unsubscribe();
  }
}
