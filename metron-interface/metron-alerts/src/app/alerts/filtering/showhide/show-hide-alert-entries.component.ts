import { Component } from '@angular/core';
import { ImplicitFiltersService } from '../ImplicitFiltersService';

@Component({
  selector: 'app-show-hide-alert-entries',
  template: `
    <app-switch [text]="'HIDE Resolved Alerts'"> </app-switch>
    <app-switch [text]="'HIDE Dismissed Alerts'"> </app-switch>
  `,
  styles: ['']
})
export class ShowHideAlertEntriesComponent {

  constructor(private implicitFiltersService: ImplicitFiltersService) {}

}
