import { Component, Output, EventEmitter } from '@angular/core';
import { ShowHideService } from './show-hide.service';

export class ShowHideChanged {
  value: string;
  isHide: boolean;

  constructor(value: string, isHide: boolean) {
    this.value = value;
    this.isHide = isHide;
  }
}

@Component({
  selector: 'app-show-hide-alert-entries',
  template: `
    <app-switch [text]="'HIDE Resolved Alerts'" data-qe-id="hideResolvedAlertsToggle" [selected]="showHideService.hideResolved"
      (onChange)="onVisibilityChanged('RESOLVE', $event)"> </app-switch>
    <app-switch [text]="'HIDE Dismissed Alerts'" data-qe-id="hideDismissedAlertsToggle" [selected]="showHideService.hideDismissed"
      (onChange)="onVisibilityChanged('DISMISS', $event)"> </app-switch>
  `
})
export class ShowHideAlertEntriesComponent {

  @Output() changed = new EventEmitter<ShowHideChanged>();

  constructor(public showHideService: ShowHideService) {}

  onVisibilityChanged(alertStatus, isHide) {
    this.showHideService.setFilterFor(alertStatus, isHide);
    this.changed.emit(new ShowHideChanged(alertStatus, isHide));
  }

}
