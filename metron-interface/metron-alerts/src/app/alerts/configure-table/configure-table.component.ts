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
import { Component, OnInit, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';
import {forkJoin as observableForkJoin, fromEvent} from 'rxjs';

import {ConfigureTableService} from '../../service/configure-table.service';
import {ClusterMetaDataService} from '../../service/cluster-metadata.service';
import {ColumnMetadata} from '../../model/column-metadata';
import {ColumnNamesService} from '../../service/column-names.service';
import {ColumnNames} from '../../model/column-names';
import {SearchService} from '../../service/search.service';
import { debounceTime } from 'rxjs/operators';

export enum AlertState {
  NEW, OPEN, ESCALATE, DISMISS, RESOLVE
}

export class ColumnMetadataWrapper {
  columnMetadata: ColumnMetadata;
  displayName: string;
  selected: boolean;

  constructor(columnMetadata: ColumnMetadata, selected: boolean, displayName: string) {
    this.columnMetadata = columnMetadata;
    this.selected = selected;
    this.displayName = displayName;
  }
}

@Component({
  selector: 'app-configure-table',
  templateUrl: './configure-table.component.html',
  styleUrls: ['./configure-table.component.scss']
})

export class ConfigureTableComponent implements OnInit, AfterViewInit {
  @ViewChild('filterColResults') filterColResults: ElementRef;

  allColumns: ColumnMetadataWrapper[] = [];
  filteredColumns: ColumnMetadataWrapper[] = [];
  columnHeaders: string;

  constructor(private router: Router, private activatedRoute: ActivatedRoute,
              private configureTableService: ConfigureTableService,
              private clusterMetaDataService: ClusterMetaDataService,
              private columnNamesService: ColumnNamesService,
              private searchService: SearchService) { }

  goBack() {
    this.router.navigateByUrl('/alerts-list');
    return false;
  }

  indexOf(columnMetadata: ColumnMetadata, configuredColumns: ColumnMetadata[]): number {
    for (let i = 0; i < configuredColumns.length; i++) {
      if (configuredColumns[i].name === columnMetadata.name) {
        return i;
      }
    }
  }

  indexToInsert(columnMetadata: ColumnMetadata, allColumns: ColumnMetadata[], configuredColumnNames: string[]): number {
    let i = 0;
    for ( ; i < allColumns.length; i++) {
      if (configuredColumnNames.indexOf(allColumns[i].name) === -1 && columnMetadata.name.localeCompare(allColumns[i].name) === -1 ) {
        break;
      }
    }
    return i;
  }

  ngOnInit() {
    observableForkJoin(
      this.clusterMetaDataService.getDefaultColumns(),
      this.searchService.getColumnMetaData(),
      this.configureTableService.getTableMetadata()
    ).subscribe((response: any) => {
      this.prepareData(response[0], response[1], response[2].tableColumns);
    });
  }

  ngAfterViewInit() {
    fromEvent(this.filterColResults.nativeElement, 'keyup')
      .pipe(debounceTime(250))
      .subscribe(e => {
        this.filterColumns(e['target'].value);
      });
  }

  filterColumns(val: string) {
    const words = val.trim().split(' ');
    this.filteredColumns = this.allColumns.filter(col => {
      return !this.isColMissingFilterKeyword(words, col, col.displayName);
    });
  }

  isColMissingFilterKeyword(words: string[], col: ColumnMetadataWrapper, displayName?: string) {
    if (displayName) {
      return !words.every(word => col.displayName.toLowerCase().includes(word.toLowerCase()));
    } else {
      return !words.every(word => col.columnMetadata.name.toLowerCase().includes(word.toLowerCase()));
    }
  }

  clearFilter() {
    this.filterColResults.nativeElement.value = '';
    this.filteredColumns = this.allColumns;
  }

  onSelectDeselectAll($event) {
    let checked = $event.target.checked;
    this.allColumns.forEach(colMetaData => colMetaData.selected = checked);
  }

  /* Slight variation of insertion sort with bucketing the items in the display order*/
  prepareData(defaultColumns: ColumnMetadata[], allColumns: ColumnMetadata[], savedColumns: ColumnMetadata[]) {
    let configuredColumns: ColumnMetadata[] = (savedColumns && savedColumns.length > 0) ?  savedColumns : defaultColumns;
    let configuredColumnNames: string[] = configuredColumns.map((mData: ColumnMetadata) => mData.name);

    allColumns = allColumns.filter((mData: ColumnMetadata) => configuredColumnNames.indexOf(mData.name) === -1);
    allColumns = allColumns.sort((mData1: ColumnMetadata, mData2: ColumnMetadata) => { return mData1.name.localeCompare(mData2.name); });

    let sortedConfiguredColumns = JSON.parse(JSON.stringify(configuredColumns));
    sortedConfiguredColumns = sortedConfiguredColumns.sort((mData1: ColumnMetadata, mData2: ColumnMetadata) => {
                                                                return mData1.name.localeCompare(mData2.name);
                                                          });

    while (configuredColumns.length > 0 ) {
      let columnMetadata = sortedConfiguredColumns.shift();

      let index = this.indexOf(columnMetadata, configuredColumns);
      let itemsToInsert: any[] = configuredColumns.splice(0, index + 1);


      let indexInAll = this.indexToInsert(columnMetadata, allColumns, configuredColumnNames);
      allColumns.splice.apply(allColumns, [indexInAll, 0].concat(itemsToInsert));
    }

    this.allColumns = allColumns.map(mData => {
      return new ColumnMetadataWrapper(mData, configuredColumnNames.indexOf(mData.name) > -1,
                                        ColumnNamesService.columnNameToDisplayValueMap[mData.name]);
      });
    this.filteredColumns = this.allColumns;
  }

  postSave() {
    this.configureTableService.fireTableChanged();
    this.goBack();
  }

  save() {
    let selectedColumns = this.allColumns.filter((mDataWrapper: ColumnMetadataWrapper) => mDataWrapper.selected)
                          .map((mDataWrapper: ColumnMetadataWrapper) => mDataWrapper.columnMetadata);

    this.configureTableService.saveColumnMetaData(selectedColumns).subscribe(() => {
      this.saveColumnNames();
    }, error => {
      console.log('Unable to save column preferences ...');
      this.saveColumnNames();
    });


  }

  saveColumnNames() {
    let columnNames = this.allColumns.map(mDataWrapper => {
      return new ColumnNames(mDataWrapper.columnMetadata.name, mDataWrapper.displayName);
    });

    this.columnNamesService.save(columnNames).subscribe(() => {
      this.postSave();
    }, error => {
      console.log('Unable to column names ...');
      this.postSave();
    });
  }

  selectColumn(columns: ColumnMetadataWrapper) {
    columns.selected = !columns.selected;
  }

  swapUp(index: number) {
    if (index > 0) {
      [this.allColumns[index], this.allColumns[index - 1]] = [this.allColumns[index - 1], this.allColumns[index]];
    }
  }

  swapDown(index: number) {
    if (index + 1 < this.allColumns.length) {
      [this.allColumns[index], this.allColumns[index + 1]] = [this.allColumns[index + 1], this.allColumns[index]];
    }
  }
}


