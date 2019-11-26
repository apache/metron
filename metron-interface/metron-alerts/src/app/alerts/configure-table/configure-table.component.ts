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
import { Component, OnInit, ViewChild, ElementRef, AfterViewInit, ViewChildren, QueryList, ChangeDetectorRef } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { forkJoin as observableForkJoin, fromEvent, Observable, Subject } from 'rxjs';

import {ConfigureTableService} from '../../service/configure-table.service';
import {ClusterMetaDataService} from '../../service/cluster-metadata.service';
import {ColumnMetadata} from '../../model/column-metadata';
import {ColumnNamesService} from '../../service/column-names.service';
import {ColumnNames} from '../../model/column-names';
import {SearchService} from '../../service/search.service';
import { debounceTime } from 'rxjs/operators';
import { DragulaService } from 'ng2-dragula';

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
  @ViewChild('columnFilterInput') columnFilterInput: ElementRef;
  @ViewChildren('moveColUpBtn') moveColUpBtn: QueryList<ElementRef>;

  columnHeaders: string;
  allColumns$: Subject<ColumnMetadataWrapper[]> = new Subject<ColumnMetadataWrapper[]>();
  visibleColumns$: Observable<ColumnMetadataWrapper[]>;
  availableColumns$: Observable<ColumnMetadataWrapper[]>;
  visibleColumns: ColumnMetadataWrapper[] = [];
  availableColumns: ColumnMetadataWrapper[] = [];
  filteredColumns: ColumnMetadataWrapper[] = [];

  constructor(
    private router: Router, private activatedRoute: ActivatedRoute,
    private configureTableService: ConfigureTableService,
    private clusterMetaDataService: ClusterMetaDataService,
    private columnNamesService: ColumnNamesService,
    private searchService: SearchService,
    private dragulaService: DragulaService,
    private cdRef: ChangeDetectorRef
  ) {
      if (!dragulaService.find('configure-table')) {
        dragulaService.setOptions('configure-table', {
          /**
           * In the list of alerts there can be certain items which should not be allowed to be dragged.
           * This is a simple solution where you can prevent items from being dragged by adding the
           * out-of-dragula class on the list item in the html template.
           *
           * Reference: https://github.com/bevacqua/dragula#optionsmoves
           */
          moves(el: HTMLElement) {
            return !(el.classList.contains('out-of-dragula'));
          },
          /**
           * This is the same as above but it's about not allowing an element to be a drop target.
           *
           * Reference: https://github.com/bevacqua/dragula#optionsaccepts
           */
          accepts(el, target, source, sibling) {
            if (!sibling) {
              return true;
            }
            return !(sibling.classList.contains('out-of-dragula'));
          }
        });
      }

      /**
       *
       * I cannot rely on dragula's internal syncing mechanism because it doesn't force angular to re-render
       * the component. But it's vital here because the state of the list items changes after changing the order
       * (e.g the user is also able to reorder the list by clicking on the arrows on the right).
       *
       * That's why I'm subscribing the drop event here and rearrange the array manually.
       *
       * References:
       * https://github.com/bevacqua/dragula#drakeon-events
       *
       * params[0] {String} - groupName (the name of the dragula group)
       * params[1] {HTMLElement} - el (the dragged element)
       * params[2] {HTMLElement} - target (the target container)
       * params[3] {HTMLElement} - source (the source container)
       * params[4] {HTMLElement} - sibling (after dropping the dragged element, this is the following element)
       */
      dragulaService.drop.subscribe((params: any[]) => {
        const el = params[1] as HTMLElement;
        const elIndex = +el.dataset.index;
        const colToMove = this.visibleColumns[elIndex];
        const cols = this.visibleColumns.filter((item, i) => i !== elIndex);
        const sibling = params[4] as HTMLElement;

        /**
         * if there's no sibling, it means that the user is moving the item to the end of the list
         */
        if (!sibling) {
          this.visibleColumns = [
            ...cols,
            colToMove
          ];
        } else {
          const siblingIndex = +sibling.dataset.index;
          /**
           * if the index of the sibling is 0, it means that the user is moving the item to the
           * beginning of the list
           */
          if (siblingIndex === 0) {
            this.visibleColumns = [
              colToMove,
              ...cols
            ];
          } else {
            /**
             * Otherwise I'm putting the element in the appropriate place within the array
             * by applying a simple reduce function to rearrange the array items.
             */
            this.visibleColumns = cols.reduce((acc, item, i) => {
              if (elIndex < siblingIndex) { // if the dragged element took place before the new sibling originally
                if (i === siblingIndex - 1) {
                  acc.push(colToMove);
                }
              } else { // if the dragged element took place after the new sibling originally
                if (i === siblingIndex) {
                  acc.push(colToMove);
                }
              }
              acc.push(item);
              return acc;
            }, []);
          }
        }
      });
  }

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
      const allColumns = this.prepareData(response[0], response[1], response[2].tableColumns);

      this.visibleColumns = allColumns.filter(column => column.selected);
      this.availableColumns = allColumns.filter(column => !column.selected);
      this.filteredColumns = this.availableColumns;
    });
  }

  ngAfterViewInit() {
    fromEvent(this.columnFilterInput.nativeElement, 'keyup')
      .pipe(debounceTime(250))
      .subscribe(e => {
        this.filterColumns(e['target'].value);
      });
  }

  filterColumns(val: string) {
    const words = val.trim().split(' ');
    this.filteredColumns = this.availableColumns.filter(col => {
      return !this.isColMissingFilterKeyword(words, col);
    });
  }

  isColMissingFilterKeyword(words: string[], col: ColumnMetadataWrapper) {
    return !words.every(word => col.columnMetadata.name.toLowerCase().includes(word.toLowerCase()));
  }

  clearFilter() {
    this.columnFilterInput.nativeElement.value = '';
    this.filteredColumns = this.availableColumns;
  }

  /* Slight variation of insertion sort with bucketing the items in the display order*/
  prepareData(defaultColumns: ColumnMetadata[], allColumns: ColumnMetadata[], savedColumns: ColumnMetadata[]): ColumnMetadataWrapper[] {
    let configuredColumns: ColumnMetadata[] = (savedColumns && savedColumns.length > 0) ?  savedColumns : defaultColumns;
    let configuredColumnNames: string[] = configuredColumns.map((mData: ColumnMetadata) => mData.name);

    allColumns = allColumns.filter((mData: ColumnMetadata) => configuredColumnNames.indexOf(mData.name) === -1);
    allColumns = allColumns.sort(this.defaultColumnSorter);

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

    return allColumns.map(mData => {
      return new ColumnMetadataWrapper(mData, configuredColumnNames.indexOf(mData.name) > -1,
                                        ColumnNamesService.columnNameToDisplayValueMap[mData.name]);
      });
    this.filteredColumns = this.availableColumns;
  }

  private defaultColumnSorter(col1: ColumnMetadata, col2: ColumnMetadata): number {
    return col1.name.localeCompare(col2.name);
  }

  postSave() {
    this.configureTableService.fireTableChanged();
    this.goBack();
  }

  save() {
    this.configureTableService.saveColumnMetaData(
      this.visibleColumns.map(columnMetaWrapper => columnMetaWrapper.columnMetadata))
      .subscribe(() => {
        this.saveColumnNames();
      }, error => {
        console.log('Unable to save column preferences ...');
        this.saveColumnNames();
      });
  }

  saveColumnNames() {
    let columnNames = this.visibleColumns.map(mDataWrapper => {
      return new ColumnNames(mDataWrapper.columnMetadata.name, mDataWrapper.displayName);
    });

    this.columnNamesService.save(columnNames).subscribe(() => {
      this.postSave();
    }, error => {
      console.log('Unable to column names ...');
      this.postSave();
    });
  }

  onColumnAdded(column: ColumnMetadataWrapper) {
    this.markColumn(column);
    this.swapList(column, this.availableColumns, this.visibleColumns);
    this.filterColumns(this.columnFilterInput.nativeElement.value);
  }

  onColumnRemoved(column: ColumnMetadataWrapper) {
    this.markColumn(column);
    this.swapList(column, this.visibleColumns, this.availableColumns);
    this.filterColumns(this.columnFilterInput.nativeElement.value);
  }

  private markColumn(column: ColumnMetadataWrapper) {
    column.selected = !column.selected;
  }

  private swapList(column: ColumnMetadataWrapper,
    source: ColumnMetadataWrapper[],
    target: ColumnMetadataWrapper[]) {

    target.push(column);
    source.splice(source.indexOf(column), 1);

    this.availableColumns.sort((colWrapper1: ColumnMetadataWrapper, colWrapper2: ColumnMetadataWrapper) => {
      return this.defaultColumnSorter(colWrapper1.columnMetadata, colWrapper2.columnMetadata)
    });
  }

  swapUp(index: number, event: any) {
    const colUpButtons = this.moveColUpBtn.toArray();
    if (index > 0) {
      [this.visibleColumns[index], this.visibleColumns[index - 1]] = [this.visibleColumns[index - 1], this.visibleColumns[index]];
    }
    /**
    *  The default behavior of the browser causes the up arrow button to lose focus
    *  on enter or space keypress, which differs in behavior when compared to the down arrow button.
    *  This condition runs change detection (which removes the focus by applying default browser behavior)
    *  and then re-applies focus to the up arrow.
    */
    if (event.type === 'keyup') {
      this.cdRef.detectChanges();
      colUpButtons[index].nativeElement.focus();
    }
  }

  swapDown(index: number) {
    if (index + 1 < this.visibleColumns.length) {
      [this.visibleColumns[index], this.visibleColumns[index + 1]] = [this.visibleColumns[index + 1], this.visibleColumns[index]];
    }
  }
}


