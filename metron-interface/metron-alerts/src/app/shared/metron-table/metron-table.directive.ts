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
/* tslint:disable:directive-selector-name */
import {Directive, Output, Input, EventEmitter, ElementRef, AfterViewInit} from '@angular/core';
import {Sort} from '../../utils/enums';

export interface SortEvent {
  sortBy: string;
  type: string;
  sortOrder: Sort;
}

@Directive({
  selector: '[metron-config-table]'
})

export class MetronTableDirective implements AfterViewInit {

  @Output() onSort = new EventEmitter<SortEvent>();
  @Input() data: any[] = [];
  @Input() cellSelectable = false;
  rowhighlightColor = '#333333';
  highlightColor = '#0F4450';
  border = '1px solid #1B596C';

  onSortColumnChange = new EventEmitter<SortEvent>();

  constructor(private element: ElementRef) { }

  private getParentTR(parent: any) {
    while (true) {
      if (parent == null) {
        return;
      }
      if (parent.nodeName === 'TR') {
        return parent;
      }
      parent = parent.parentNode;
    }
  }

  mouseover($event) {
    if ($event.target.nodeName === 'TH') {
      return;
    }

    if (this.cellSelectable && $event.target.nodeName === 'A') {
        $event.target.style.backgroundColor = this.highlightColor;
        $event.target.style.border = this.border;

    } else {
        let parent = this.getParentTR($event.target);
        if (!parent.classList.contains('no-hover')) {
          parent.style.backgroundColor = this.rowhighlightColor;
        }
    }
  }

  mouseleave($event) {
    if ($event.target.nodeName === 'TH') {
      return;
    }

    if (this.cellSelectable && $event.target.nodeName === 'A') {
      $event.target.style.border = '';
      $event.target.style.backgroundColor = '';
    } else {
      let parent = this.getParentTR($event.target);
      parent.style.backgroundColor = '';
    }
  }

  ngAfterViewInit() {
    this.element.nativeElement.querySelector('tbody').addEventListener('mouseover', this.mouseover.bind(this));
    this.element.nativeElement.querySelector('tbody').addEventListener('mouseout', this.mouseleave.bind(this));
  }


  public setSort(sortEvent: SortEvent): void {
    this.onSortColumnChange.emit(sortEvent);
    if (this.onSort.observers.length === 0 ) {
      this.sort(sortEvent);
    } else {
      this.onSort.emit(sortEvent);
    }
  }

  private sort($event) {
    this.data.sort((obj1: any, obj2: any) => {
      if ($event.sortOrder === Sort.ASC) {
        if ($event.type === 'string') {
          return obj1[$event.sortBy].localeCompare(obj2[$event.sortBy]);
        }
        if ($event.type === 'number') {
          return obj1[$event.sortBy] - obj2[$event.sortBy];
        }
      }

      if ($event.type === 'string') {
        return obj2[$event.sortBy].localeCompare(obj1[$event.sortBy]);
      }
      if ($event.type === 'number') {
        return obj2[$event.sortBy] - obj1[$event.sortBy];
      }
    });
  }
}
