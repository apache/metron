import { Injectable } from '@angular/core';
import { QueryBuilder } from 'app/alerts/alerts-list/query-builder';
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
}
