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
import { QueryBuilder, FilteringMode } from 'app/alerts/alerts-list/query-builder';
import { Filter } from 'app/model/filter';

@Injectable({
  providedIn: 'root'
})
export class ShowHideService {

  private readonly FIELD = '-alert_status';
  private readonly RESOLVE = 'RESOLVE';
  private readonly DISMISS = 'DISMISS';

  public readonly HIDE_RESOLVE_STORAGE_KEY = 'hideResolvedAlertItems';
  public readonly HIDE_DISMISS_STORAGE_KEY = 'hideDismissAlertItems';

  private readonly resolveFilter = new Filter(this.FIELD, this.RESOLVE, false);
  private readonly dismissFilter = new Filter(this.FIELD, this.DISMISS, false);

  hideResolved = false;
  hideDismissed = false;

  constructor(public queryBuilder: QueryBuilder) {
    this.hideResolved = localStorage.getItem(this.HIDE_RESOLVE_STORAGE_KEY) === 'true';
    this.setFilterFor(this.RESOLVE, this.hideResolved);

    this.hideDismissed = localStorage.getItem(this.HIDE_DISMISS_STORAGE_KEY) === 'true';
    this.setFilterFor(this.DISMISS, this.hideDismissed);
  }

  setFilterFor(alertStatus, isHide) {
    const filterOperation = ((isFilterToAdd) => {
      if (isFilterToAdd) {
        return this.queryBuilder.addOrUpdateFilter.bind(this.queryBuilder);
      } else {
        return this.queryBuilder.removeFilter.bind(this.queryBuilder);
      }
    })(isHide);

    switch (alertStatus) {
      case this.DISMISS:
        filterOperation(this.dismissFilter);
        this.hideDismissed = isHide;
        localStorage.setItem(this.HIDE_DISMISS_STORAGE_KEY, isHide);
        break;
      case this.RESOLVE:
        filterOperation(this.resolveFilter);
        this.hideResolved = isHide;
        localStorage.setItem(this.HIDE_RESOLVE_STORAGE_KEY, isHide);
        break;
    }
  }

  isAvailable() {
    return this.queryBuilder.getFilteringMode() === FilteringMode.BUILDER;
  }
}
