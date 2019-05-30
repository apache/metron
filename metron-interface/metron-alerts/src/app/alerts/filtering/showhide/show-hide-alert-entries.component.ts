import { Component } from '@angular/core';
import { ImplicitFilteringService } from '../service/ImplicitFilteringService';

@Component({
  selector: 'app-show-hide-alert-entries',
  template: `
    <app-switch [text]="'HIDE Resolved Alerts'" (onChange)="onVisibilityChanged('RESOLVE', $event)"> </app-switch>
    <app-switch [text]="'HIDE Dismissed Alerts'" (onChange)="onVisibilityChanged('DISMISS', $event)"> </app-switch>
  `,
  styles: ['']
})
export class ShowHideAlertEntriesComponent {

  constructor(private implicitFilteringService: ImplicitFilteringService) {}

  onVisibilityChanged(alertStatus, isExcluded) {
    console.log(alertStatus, isExcluded);
  }

}
