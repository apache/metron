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
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import {MetronTableDirective, SortEvent} from '../metron-table.directive';
import {Sort} from '../../../utils/enums';

@Component({
  selector: 'metron-config-sorter',
  templateUrl: './metron-sorter.component.html',
  styleUrls: ['./metron-sorter.component.scss']
})
export class MetronSorterComponent implements OnChanges {

  @Input() sortOnCol: string;
  @Input() sortOrder: number;

  @Input() sortBy: string;
  @Input() type = 'string';

  sortAsc = false;
  sortDesc = false;

  constructor(private metronTable: MetronTableDirective ) {
    this.metronTable.onSortColumnChange.subscribe((event: SortEvent) => {
      this.sortAsc = (event.sortBy === this.sortBy && event.sortOrder === Sort.ASC);
      this.sortDesc = (event.sortBy === this.sortBy && event.sortOrder === Sort.DSC);
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['sortOnCol'] && changes['sortOnCol'].currentValue) {
      if (this.sortOnCol === this.sortBy ) {
        this.sortAsc = this.sortOrder === Sort.ASC;
        this.sortDesc = this.sortOrder === Sort.DSC;
      }
    }
  }

  sort() {
    let order = this.sortAsc ? Sort.DSC : Sort.ASC;
    this.metronTable.setSort({sortBy: this.sortBy, sortOrder: order, type: this.type});
  }
}
