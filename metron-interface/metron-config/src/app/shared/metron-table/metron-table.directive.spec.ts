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

import {Sort} from '../../util/enums';
import {MetronTableDirective, SortEvent} from './metron-table.directive';

describe('Directive: MetronTable', () => {

  it('should create an instance', () => {
    let directive = new MetronTableDirective();
    expect(directive).toBeTruthy();
  });

  it('should emmit listeners ', () => {
    let eventToTest: SortEvent = null;
    let directive = new MetronTableDirective();

    let event1 = { sortBy: 'col1', sortOrder: Sort.ASC };
    let event2 = { sortBy: 'col2', sortOrder: Sort.DSC };

    directive.onSort.subscribe((sortEvent: SortEvent) => {
        expect(sortEvent).toEqual(eventToTest);
    });
    directive.onSortColumnChange.subscribe((sortEvent: SortEvent) => {
        expect(sortEvent).toEqual(eventToTest);
    });

    eventToTest = event1;
    directive.setSort(eventToTest);

    eventToTest = event2;
    directive.setSort(eventToTest);

  });

});
