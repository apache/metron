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
/* tslint:disable:no-unused-variable */
// directiveSelectorNameRule

import {MetronSorterComponent} from './metron-sorter.component';
import {MetronTableDirective} from '../metron-table.directive';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import {Component, DebugElement} from '@angular/core';

@Component({
  template: `<table class="table table-sm" metron-config-table #table></table>`
})
class TestHoverFocusComponent {
}

describe('Component: MetronSorter', () => {
  let metronTable: ComponentFixture<MetronSorterComponent>;
  let sorter1: MetronSorterComponent;
  let sorter2: MetronSorterComponent;
  let sorter3: MetronSorterComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [MetronTableDirective]
    });
    metronTable = TestBed.createComponent(MetronSorterComponent);
    sorter1 = metronTable.componentInstance;
    sorter2 = metronTable.componentInstance;
    sorter3 = metronTable.componentInstance;
  });

  // it('should create an instance', () => {
  //   let metronTable = new MetronTableDirective();
  //   let component = new MetronSorterComponent(metronTable);
  //   expect(component).toBeTruthy();
  // });

  it('should set the variables according to sorter', () => {
    // let metronTable = new MetronTableDirective();
    // let sorter1 = new MetronSorterComponent(metronTable);
    // let sorter2 = new MetronSorterComponent(metronTable);
    // let sorter3 = new MetronSorterComponent(metronTable);

    sorter1.sortBy = 'col1';
    sorter2.sortBy = 'col2';
    sorter3.sortBy = 'col3';

    sorter1.sort();
    expect(sorter1.sortAsc).toEqual(true);
    expect(sorter1.sortDesc).toEqual(false);
    expect(sorter2.sortAsc).toEqual(false);
    expect(sorter2.sortDesc).toEqual(false);
    expect(sorter3.sortAsc).toEqual(false);
    expect(sorter3.sortDesc).toEqual(false);

    sorter1.sort();
    expect(sorter1.sortAsc).toEqual(false);
    expect(sorter1.sortDesc).toEqual(true);
    expect(sorter2.sortAsc).toEqual(false);
    expect(sorter2.sortDesc).toEqual(false);
    expect(sorter3.sortAsc).toEqual(false);
    expect(sorter3.sortDesc).toEqual(false);

    sorter2.sort();
    expect(sorter1.sortAsc).toEqual(false);
    expect(sorter1.sortDesc).toEqual(false);
    expect(sorter2.sortAsc).toEqual(true);
    expect(sorter2.sortDesc).toEqual(false);
    expect(sorter3.sortAsc).toEqual(false);
    expect(sorter3.sortDesc).toEqual(false);

    sorter2.sort();
    expect(sorter1.sortAsc).toEqual(false);
    expect(sorter1.sortDesc).toEqual(false);
    expect(sorter2.sortAsc).toEqual(false);
    expect(sorter2.sortDesc).toEqual(true);
    expect(sorter3.sortAsc).toEqual(false);
    expect(sorter3.sortDesc).toEqual(false);

    sorter3.sort();
    expect(sorter1.sortAsc).toEqual(false);
    expect(sorter1.sortDesc).toEqual(false);
    expect(sorter2.sortAsc).toEqual(false);
    expect(sorter2.sortDesc).toEqual(false);
    expect(sorter3.sortAsc).toEqual(true);
    expect(sorter3.sortDesc).toEqual(false);

    sorter3.sort();
    expect(sorter1.sortAsc).toEqual(false);
    expect(sorter1.sortDesc).toEqual(false);
    expect(sorter2.sortAsc).toEqual(false);
    expect(sorter2.sortDesc).toEqual(false);
    expect(sorter3.sortAsc).toEqual(false);
    expect(sorter3.sortDesc).toEqual(true);

  });

});
