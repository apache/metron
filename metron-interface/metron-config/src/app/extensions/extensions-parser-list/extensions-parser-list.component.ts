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
import {Component, OnInit, ViewChild} from '@angular/core';
import {Router, NavigationStart} from '@angular/router';
import {MetronAlerts} from '../../shared/metron-alerts';
import {MetronDialogBox} from '../../shared/metron-dialog-box';
import {Sort} from '../../util/enums';
import {SortEvent} from '../../shared/metron-table/metron-table.directive';
import {ParserExtensionConfig} from "../../model/parser-extension-config";
import {ParserExtensionService} from "../../service/parser-extension.service";

@Component({
  selector: 'metron-config-extensions-parser-list',
  templateUrl: 'extensions-parser-list.component.html',
  styleUrls: ['extensions-parser-list.component.scss']
})
export class ExtensionsParserListComponent implements OnInit {

  componentName: string = 'Parser Extensions';
  @ViewChild('table') table;

  count: number = 0;
  parserExtensions: ParserExtensionConfig[] = [];
  selectedExtensions: ParserExtensionConfig[] = [];
  enableAutoRefresh: boolean = true;

  constructor(private parserExtensionService: ParserExtensionService,
              private router: Router,
              private metronAlerts:  MetronAlerts,
              private metronDialogBox: MetronDialogBox) {
    router.events.subscribe(event => {
      if (event instanceof NavigationStart && event.url === '/extensions') {
        this.onNavigationStart();
      }
    });
  }

  getParserExtensions(justOnce: boolean) {
    this.parserExtensionService.getAll().subscribe(
      (results: ParserExtensionConfig[]) => {
        this.parserExtensions = results;
        this.selectedExtensions = [];
        this.count = this.parserExtensions.length;
      }
    );
  }

  onSort($event: SortEvent) {
    switch ($event.sortBy) {
      case 'extensionIdentifier':
      case 'extensionAssemblyName':
      case 'extensionBundleName':
      case 'extensionBundleID':
      case 'extensionBundleVersion':
        this.parserExtensions.sort((obj1: ParserExtensionConfig, obj2: ParserExtensionConfig) => {
          if (!obj1[$event.sortBy] || !obj1[$event.sortBy]) {
            return 0;
          }
          if ($event.sortOrder === Sort.ASC) {
            return obj1[$event.sortBy].localeCompare(obj2[$event.sortBy]);
          }

          return obj2[$event.sortBy].localeCompare(obj1[$event.sortBy]);
        });
        break;
    }
  }

  getParserTypes(parserExtensionConfig: ParserExtensionConfig): string[] {
    let items = parserExtensionConfig.parserExtensionParserNames.map(info=>{
      return info.replace('Basic','').replace('Parser','');
    });
    return items;
  }

  ngOnInit() {
    this.getParserExtensions(false);
    this.parserExtensionService.dataChanged$.subscribe(
      data => {
        this.getParserExtensions(true);
      }
    );
  }

  addAddParserExtension() {
    this.router.navigateByUrl('/extensions(dialog:extensions-install)');
  }


  onRowSelected(parserExtension: ParserExtensionConfig, $event) {
    if ($event.target.checked) {
      this.selectedExtensions.push(parserExtension);
    } else {
      this.selectedExtensions.splice(this.selectedExtensions.indexOf(parserExtension), 1);
    }
  }

  onSelectDeselectAll($event) {
    let checkBoxes = this.table.nativeElement.querySelectorAll('tr td:last-child input[type="checkbox"]');

    for (let ele of checkBoxes) {
      ele.checked = $event.target.checked;
    }

    if ($event.target.checked) {
      this.selectedExtensions = this.parserExtensions.slice(0).map((parserExtensionInfo: ParserExtensionConfig) => parserExtensionInfo);
    } else {
      this.selectedExtensions = [];
    }
  }

  onParserExtensionRowSelect(parserExtension: ParserExtensionConfig, $event) {
    if ($event.target.type !== 'checkbox' && $event.target.parentElement.firstChild.type !== 'checkbox') {

      if (this.parserExtensionService.getSelectedExtension() === parserExtension) {
        this.parserExtensionService.setSeletedExtension(null);
        this.router.navigateByUrl('/extensions');
        return;
      }

      this.parserExtensionService.setSeletedExtension(parserExtension);
    }
  }

  deleteParserExtension($event, selectedParserExtensionsToDelete: ParserExtensionConfig[]) {
    if ($event) {
      $event.stopPropagation();
    }

    let sensorNames = selectedParserExtensionsToDelete.map(sensor => { return sensor.extensionIdentifier; });
    let confirmationsMsg = 'Are you sure you want to delete extension(s) ' + sensorNames.join(', ') + ' ?';

    this.metronDialogBox.showConfirmationMessage(confirmationsMsg).subscribe(result => {
      if (result) {
        this.parserExtensionService.deleteMany(selectedParserExtensionsToDelete)
            .subscribe((deleteResult: {success: Array<string>, failure: Array<string>}) => {
          if (deleteResult.success.length > 0) {
            this.metronAlerts.showSuccessMessage('Deleted extensions: ' + deleteResult.success.join(', '));
          }

          if (deleteResult.failure.length > 0) {
            this.metronAlerts.showErrorMessage('Unable to deleted extensions: ' + deleteResult.failure.join(', '));
          }
        });
      }
    });
  }

  onDeleteParserExtension() {
    let selectedExtensionsToDelete = this.selectedExtensions.map(info => {
      return info;
    });
    this.deleteParserExtension(null, selectedExtensionsToDelete);
  }

  onNavigationStart() {
    this.parserExtensionService.setSeletedExtension(null);
    this.selectedExtensions = [];
  }
}
