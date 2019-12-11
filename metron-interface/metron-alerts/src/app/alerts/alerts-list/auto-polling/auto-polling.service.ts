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
import { Injectable } from '@angular/core';
import { Subscription, Subject, Observable, interval, timer } from 'rxjs';
import { SearchService } from 'app/service/search.service';
import { QueryBuilder } from '../query-builder';
import { SearchResponse } from 'app/model/search-response';
import { switchMap, filter, takeWhile, tap } from 'rxjs/operators';
import { POLLING_DEFAULT_STATE } from 'app/utils/constants';
import { RestError } from 'app/model/rest-error';
import { DialogType } from 'app/shared/metron-dialog/metron-dialog.component';
import { DialogService } from 'app/service/dialog.service';
import { RefreshInterval } from '../../configure-rows/configure-rows-enums';
import { UserSettingsService } from 'app/service/user-settings.service';

interface AutoPollingStateModel {
  isActive: boolean,
  refreshInterval: number,
}

@Injectable()
export class AutoPollingService {
  private isCongestion = false;
  private refreshInterval = RefreshInterval.TEN_MIN;
  private isPollingActive = POLLING_DEFAULT_STATE;
  private isPending = false;
  private isPollingSuppressed = false;
  private pollingIntervalSubs: Subscription;

  readonly AUTO_START_DELAY = 500;
  data = new Subject<SearchResponse>();

  public readonly AUTO_POLLING_STORAGE_KEY = 'autoPolling';

  constructor(private searchService: SearchService,
              private queryBuilder: QueryBuilder,
              private dialogService: DialogService,
              private userSettingsService: UserSettingsService
              ) {
                this.restoreState();
              }

  start() {
    if (!this.isPollingActive) {
      this.sendInitial();
      this.activate();
    }
    this.isPollingActive = true;
    this.persistState();
  }

  stop(persist = true) {
    this.isPollingActive = false;
    if (this.pollingIntervalSubs) {
      this.pollingIntervalSubs.unsubscribe();
      this.pollingIntervalSubs = null;
    }

    if (persist) {
      this.persistState();
    }
  }

  setSuppression(value: boolean) {
    this.isPollingSuppressed = value;
  }

  dropNextAndContinue() {
    this.reset();
  }

  setInterval(seconds: number) {
    this.refreshInterval = seconds;
    if (this.isPollingActive) {
      this.reset();
    }
    this.persistState();
  }

  getInterval(): number {
    return this.refreshInterval;
  }

  getIsPollingActive() {
    return this.isPollingActive;
  }

  getIsCongestion() {
    return this.isCongestion
  }

  private sendInitial() {
    this.isPending = true;
    this.searchService.search(this.queryBuilder.searchRequest).subscribe(this.onResult.bind(this));
  }

  private persistState(key = this.AUTO_POLLING_STORAGE_KEY): void {
    this.userSettingsService.save({
      [key]: JSON.stringify(this.getStateModel())
    }).subscribe();
  }

  private restoreState(key = this.AUTO_POLLING_STORAGE_KEY): void {
    this.userSettingsService.get(key)
      .subscribe((autoPollingState) => {
        const persistedState = autoPollingState ? JSON.parse(autoPollingState) as AutoPollingStateModel : null;

        if (persistedState) {
          this.refreshInterval = persistedState.refreshInterval;

          if (persistedState.isActive) {
            timer(this.AUTO_START_DELAY).subscribe(this.start.bind(this));
          }
        }
      });
  }

  private getStateModel(): AutoPollingStateModel {
    return {
      isActive: this.isPollingActive,
      refreshInterval: this.refreshInterval,
    }
  }

  private reset() {
    if (this.pollingIntervalSubs) {
      this.pollingIntervalSubs.unsubscribe();
      this.isPending = false;
    }
    this.activate();
  }

  private activate() {
    this.pollingIntervalSubs = this.startPolling()
      .subscribe(
        this.onResult.bind(this),
        this.onError.bind(this),
      );
  }

  private onError(error: RestError) {
    this.stop();
    this.dialogService.launchDialog(
      'Server were unable to apply query string. ' +
      'Evaluate query string and restart polling.'
      , DialogType.Error);
  }

  private onResult(result: SearchResponse) {
    this.data.next(result);
    this.isPending = false;
  }

  private startPolling(): Observable<SearchResponse> {
    return interval(this.refreshInterval * 1000).pipe(
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
    if (this.getIsPollingActive()) {
      this.stop(false);
    }
  }
}
