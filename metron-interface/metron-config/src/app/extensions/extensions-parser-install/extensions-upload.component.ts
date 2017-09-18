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

import { Component, ElementRef, Input, ViewChild } from '@angular/core';
import {ParserExtensionService} from "../../service/parser-extension.service";
import {MetronAlerts} from "../../shared/metron-alerts";
import {RestError} from "../../model/rest-error";
import {Router} from "@angular/router";

@Component({
  selector: 'extensions-upload',
  template: '<input type="file"#fileInput>'
})
export class ExtensionsUploadComponent {
  @ViewChild('fileInput') inputEl: ElementRef;

  constructor(private parserExtensionService: ParserExtensionService, private metronAlerts:  MetronAlerts, private router: Router) {}

  goBack() {
    this.router.navigateByUrl('/extensions');
    return false;
  }

  upload() {
    let inputEl: HTMLInputElement = this.inputEl.nativeElement;
    let fileCount: number = inputEl.files.length;
    let formData = new FormData();
    if (fileCount > 0) {
      formData.append('extensionTgz', inputEl.files.item(0));
      this.parserExtensionService.post(inputEl.files.item(0).name, formData).subscribe(
          result => {
            if (result.status == 201) {
              this.metronAlerts.showSuccessMessage('Installed Parser Extension: ' + inputEl.files[0].name);
              (this).parserExtensionService.dataChangedSource.next();
              this.goBack();
            } else {
              this.metronAlerts.showErrorMessage('Unable to Install Parser Extension: ' + inputEl.files[0].name);
            }
          }, (error: RestError) => {
            let msg = ' Unable to Install Parser Extension: ';
            this.metronAlerts.showErrorMessage(msg + error.message);
          });
    }
  }
}