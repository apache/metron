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
import { Component, OnInit } from '@angular/core';
import { GlobalConfigService } from '../service/global-config.service';
import { MetronAlerts } from '../shared/metron-alerts';
import { MetronDialogBox } from '../shared/metron-dialog-box';

@Component({
  selector: 'metron-config-general-settings',
  templateUrl: './general-settings.component.html',
  styleUrls: ['./general-settings.component.scss']
})
export class GeneralSettingsComponent implements OnInit {
  globalConfig: {} = {
    'es.date.format': '-'
  };

  fieldValidations: string;

  constructor(
    private globalConfigService: GlobalConfigService,
    private metronAlerts: MetronAlerts,
    private metronDialog: MetronDialogBox
  ) {}

  ngOnInit() {
    this.globalConfigService.get().subscribe((config: {}) => {
      this.globalConfig = config;
      if (!this.globalConfig['es.date.format']) {
        this.globalConfig['es.date.format'] = '-';
      }
      if (this.globalConfig['fieldValidations']) {
        this.fieldValidations = JSON.stringify(
          this.globalConfig['fieldValidations'],
          null,
          '\t'
        );
      }
    });
  }

  onSave() {
    if (this.fieldValidations && this.fieldValidations.length > 0) {
      this.globalConfig['fieldValidations'] = JSON.parse(this.fieldValidations);
    }
    this.globalConfigService.post(this.globalConfig).subscribe(
      () => {
        this.metronAlerts.showSuccessMessage('Saved Global Settings');
      },
      error => {
        this.metronAlerts.showErrorMessage(
          'Unable to save Global Settings: ' + error
        );
      }
    );
  }

  onCancel() {
    let confirmationMsg =
      'Cancelling will revert all the changes made to the form. Do you wish to continue ?';
    this.metronDialog
      .showConfirmationMessage(confirmationMsg)
      .subscribe((result: boolean) => {
        if (result) {
          this.ngOnInit();
        }
      });
  }
}
