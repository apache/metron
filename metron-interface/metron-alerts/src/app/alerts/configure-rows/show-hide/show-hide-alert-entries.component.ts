import { Component, OnInit } from '@angular/core';
import { QueryBuilder } from 'app/alerts/alerts-list/query-builder';
import { Filter } from 'app/model/filter';

@Component({
  selector: 'app-show-hide-alert-entries',
  template: `
    <app-switch [text]="'HIDE Resolved Alerts'" [selected]="hideResolved"
      (onChange)="onVisibilityChanged('RESOLVE', $event)"> </app-switch>
    <app-switch [text]="'HIDE Dismissed Alerts'" [selected]="hideDismissed"
      (onChange)="onVisibilityChanged('DISMISS', $event)"> </app-switch>
  `,
  styles: ['']
})
export class ShowHideAlertEntriesComponent implements OnInit {

  private readonly FIELD = '-alert_status';
  private readonly RESOLVE = 'RESOLVE';
  private readonly DISMISS = 'DISMISS';

  private readonly HIDE_RESOLVE_STORAGE_KEY = 'hideResolvedAlertItems';
  private readonly HIDE_DISMISS_STORAGE_KEY = 'hideDismissAlertItems';

  private readonly resolveFilter = new Filter(this.FIELD, this.RESOLVE, false);
  private readonly dismissFilter = new Filter(this.FIELD, this.DISMISS, false);

  hideResolved = false;
  hideDismissed = false;

  constructor(private queryBuilder: QueryBuilder) {}

  ngOnInit() {
    this.hideDismissed = localStorage.getItem(this.HIDE_DISMISS_STORAGE_KEY) === 'true';
    this.onVisibilityChanged(this.DISMISS, this.hideDismissed);

    this.hideResolved = localStorage.getItem(this.HIDE_RESOLVE_STORAGE_KEY) === 'true';
    this.onVisibilityChanged(this.RESOLVE, this.hideResolved);
  }

  onVisibilityChanged(alertStatus, isHide) {
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
