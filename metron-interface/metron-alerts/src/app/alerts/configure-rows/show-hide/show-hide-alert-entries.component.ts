import { Component } from '@angular/core';

@Component({
  selector: 'app-show-hide-alert-entries',
  template: `
    <app-switch [text]="'HIDE Resolved Alerts'" (onChange)="onVisibilityChanged('RESOLVE', $event)"> </app-switch>
    <app-switch [text]="'HIDE Dismissed Alerts'" (onChange)="onVisibilityChanged('DISMISS', $event)"> </app-switch>
  `,
  styles: ['']
})
export class ShowHideAlertEntriesComponent {

  constructor() {}

  onVisibilityChanged(alertStatus, isExcluded) {
    console.log(alertStatus, isExcluded);
  }

}
