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
import { Component, Output, EventEmitter } from '@angular/core';
import { ShowHideService } from './show-hide.service';
import { FilteringMode } from 'app/alerts/alerts-list/query-builder';

export interface ShowHideStateModel {
  hideResolved: boolean,
  hideDismissed: boolean,
}

@Component({
  selector: 'app-show-hide-alert-entries',
  template: `
    <div *ngIf="!isAvailable()" class="warning">Hide toggles are not available in manual filtering mode.</div>
    <app-switch [text]="'HIDE Resolved Alerts'" data-qe-id="hideResolvedAlertsToggle" [selected]="showHideService.hideResolved"
      (onChange)="onVisibilityChanged('RESOLVE', $event)"
      [disabled]="!isAvailable()"> </app-switch>
    <app-switch [text]="'HIDE Dismissed Alerts'" data-qe-id="hideDismissedAlertsToggle" [selected]="showHideService.hideDismissed"
      (onChange)="onVisibilityChanged('DISMISS', $event)"
      [disabled]="!isAvailable()"> </app-switch>
  `,
  styles: [
    '.warning { font-size: 0.8rem; padding: 0 0.4rem; color: darkorange; }',
  ]
})
export class ShowHideAlertEntriesComponent {

  @Output() changed = new EventEmitter<ShowHideStateModel>();

  constructor(public showHideService: ShowHideService) {}

  onVisibilityChanged(alertStatus: string, isHide: boolean): void {
    this.showHideService.setFilterFor(alertStatus, isHide);
    this.changed.emit({
      hideResolved: this.showHideService.hideResolved,
      hideDismissed: this.showHideService.hideDismissed,
    });
  }

  isAvailable() {
    return this.showHideService.queryBuilder.getFilteringMode() !== FilteringMode.MANUAL;
  }

}
