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
import {GroupRequest} from '../../model/group-request';
import {Group} from '../../model/group';

export class QueryBuilder {
  private _searchRequest = new SearchRequest();
  private _groupRequest = new GroupRequest();
  private _query = '*';
  private _displayQuery = this._query;
  private _filters: Filter[] = [];

  set query(value: string) {
    value = value.replace(/\\:/g, ':');
    this._query = value;
    this.updateFilters(this._query, false);
    this.onSearchChange();
  }

  get query(): string {
    return this._query;
  }

  set displayQuery(value: string) {
    this._displayQuery = value;
    this.updateFilters(this._displayQuery, true);
    this.onSearchChange();
  }

  get displayQuery(): string {
    return this._displayQuery;
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
    this.query = this._searchRequest.query;
  }

  get groupRequest(): GroupRequest {
    this._groupRequest.query = this.generateSelect();
    return this._groupRequest;
  }

  addOrUpdateFilter(filter: Filter) {
    let existingFilter = this._filters.find(tFilter => tFilter.field === filter.field);
    if (existingFilter) {
      existingFilter.value = filter.value;
    } else {
      this._filters.push(filter);
    }

    this.onSearchChange();
  }

  generateSelect() {
    let select = this._filters.map(filter => {
      return filter.field.replace(/:/g, '\\:') +
              ':' +
        String(filter.value)
          .replace(/[\*\+\-=~><\"\?^\${}\(\)\:\!\/[\]\\\s]/g, '\\$&') // replace single  special characters
          .replace(/\|\|/g, '\\||') // replace ||
          .replace(/\&\&/g, '\\&&'); // replace &&
    }).join(' AND ');
    return (select.length === 0) ? '*' : select;
  }

  generateSelectForDisplay() {
    let select = this._filters.map(filter => ColumnNamesService.getColumnDisplayValue(filter.field) + ':' + filter.value).join(' AND ');
    return (select.length === 0) ? '*' : select;
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
    this.groupRequest.groups = groups.map(groupName => new Group(groupName));
  }

  setSort(sortBy: string, order: string) {
    let sortField = new SortField();
    sortField.field = sortBy;
    sortField.sortOrder = order;

    this.searchRequest.sort = [sortField];
  }

  private updateFilters(tQuery: string, updateNameTransform = false) {
    let query = tQuery;
    this._filters = [];

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
}
