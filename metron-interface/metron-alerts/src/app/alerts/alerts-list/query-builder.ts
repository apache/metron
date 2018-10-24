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
import {Filter} from '../../model/filter';
import {ColumnNamesService} from '../../service/column-names.service';
import {SearchRequest} from '../../model/search-request';
import {SortField} from '../../model/sort-field';
import {TIMESTAMP_FIELD_NAME} from '../../utils/constants';
import {GroupRequest} from '../../model/group-request';
import {Group} from '../../model/group';

export class QueryBuilder {
  private _searchRequest = new SearchRequest();
  private _groupRequest = new GroupRequest();
  private _query = '*';
  private _displayQuery = this._query;
  private _filters: Filter[] = [];

  get query(): string {
    return this._query;
  }

  get displayQuery(): string {
    return this._displayQuery;
  }

  set filters(filters: Filter[]) {
    filters.forEach(filter =>  {
      this.addOrUpdateFilter(filter)
    });
  }

  get filters(): Filter[] {
    return this._filters;
  }


  get searchRequest(): SearchRequest {
    this._searchRequest.query = this.generateSelect();
    return this._searchRequest;
  }

  set searchRequest(value: SearchRequest) {
    this._searchRequest = value;
    this.setSearch(this._searchRequest.query);
  }

  groupRequest(scoreField): GroupRequest {
    this._groupRequest.query = this.generateSelect();
    this._groupRequest.scoreField = scoreField;
    return this._groupRequest;
  }

  setSearch(query: string) {
    this.updateFilters(query, true);
    this.onSearchChange();
  }

  clearSearch() {
    this._filters = [];
    this.onSearchChange();
  }

  addOrUpdateFilter(filter: Filter) {
    let existingFilterIndex = -1;
    let existingFilter = this._filters.find((tFilter, index) => {
      if (tFilter.field === filter.field) {
        existingFilterIndex = index;
        return true;
      }
      return false;
    });

    if (existingFilter) {
      this._filters.splice(existingFilterIndex, 1, filter);
    } else {
      this._filters.push(filter);
    }

    this.onSearchChange();
  }

  generateSelect() {
    let select = this._filters.map(filter => filter.getQueryString()).join(' AND ');
    return (select.length === 0) ? '*' : select;
  }

  generateNameForSearchRequest() {
    let select = this._filters.map(filter => ColumnNamesService.getColumnDisplayValue(filter.field) + ':' + filter.value).join(' AND ');
    return (select.length === 0) ? '*' : select;
  }

  generateSelectForDisplay() {
    let appliedFilters = [];
    this._filters.reduce((appliedFilters, filter) => {
      if (filter.display) {
        appliedFilters.push(ColumnNamesService.getColumnDisplayValue(filter.field) + ':' + filter.value);
      }

      return appliedFilters;
    }, appliedFilters);

    let select = appliedFilters.join(' AND ');
    return (select.length === 0) ? '*' : select;
  }

  isTimeStampFieldPresent(): boolean {
    return this._filters.some(filter => (filter.field === TIMESTAMP_FIELD_NAME &&  !isNaN(Number(filter.value))));
  }

  onSearchChange() {
    this._query = this.generateSelect();
    this._displayQuery = this.generateSelectForDisplay();
  }

  removeFilter(field: string) {
    let filter = this._filters.find(tFilter => tFilter.field === field);
    this._filters.splice(this._filters.indexOf(filter), 1);

    this.onSearchChange();
  }

  setFields(fieldNames: string[]) {
      // this.searchRequest._source = fieldNames;
  }

  setFromAndSize(from: number, size: number) {
    this.searchRequest.from = from;
    this.searchRequest.size = size;
  }

  setGroupby(groups: string[]) {
    this._groupRequest.groups = groups.map(groupName => new Group(groupName));
  }

  setSort(sortBy: string, order: string) {
    let sortField = new SortField(sortBy, order);
    this.searchRequest.sort = [sortField];
  }

  private updateFilters(tQuery: string, updateNameTransform = false) {
    let query = tQuery;
    this.removeDisplayedFilters();

    if (query && query !== '' && query !== '*') {
      let terms = query.split(' AND ');
      for (let term of terms) {
        let separatorPos = term.lastIndexOf(':');
        let field = term.substring(0, separatorPos).replace('\\', '');
        field = updateNameTransform ? ColumnNamesService.getColumnDisplayKey(field) : field;
        let value = term.substring(separatorPos + 1, term.length);
        this.addOrUpdateFilter(new Filter(field, value));
      }
    }
  }

  private removeDisplayedFilters() {
    for (let i = this._filters.length-1; i >= 0; i--) {
      if (this._filters[i].display) {
        this._filters.splice(i, 1);
      }
    }
  }
}
