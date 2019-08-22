import { Component, OnInit } from '@angular/core';
import { TimezoneConfigService } from './timezone-config.service';

@Component({
  selector: 'app-timezone-config',
  template: `
    <app-switch
      data-qe-id="UTCtoLocalToggle"
      [text]="'Convert timestamps to local time'"
      (onChange)="toggleTimezoneConfig($event)"
      [selected]="timezoneConfigService.showLocal"
    ></app-switch>
  `,
})
export class TimezoneConfigComponent {
  constructor(public timezoneConfigService: TimezoneConfigService) {}

  toggleTimezoneConfig(isLocal) {
    this.timezoneConfigService.toggleUTCtoLocal(isLocal);
  }
}
