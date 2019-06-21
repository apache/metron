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
import { Utils } from '../utils/utils';
import { TIMESTAMP_FIELD_NAME, GIUD_FIELD_NAME } from '../utils/constants';
import { DateFilterValue } from './date-filter-value';

export class Filter {
  value: string;
  display: boolean;
  dateFilterValue: DateFilterValue;

  private readonly excludeOperatorRxp = /^-/;
  private readonly excludeOperator = '-';
  private isExcluding = false;

  private clearedField: string;

  get field(): string {
    return this.operatorToAdd() + this.clearedField;
  }

  static fromJSON(objs: Filter[]): Filter[] {
    let filters = [];
    if (objs) {
      for (let obj of objs) {
        filters.push(new Filter(obj.field, obj.value, obj.display));
      }
    }
    return filters;
  }

  toJSON() {
    return { field: this.field, value: this.value, display: this.display };
  }

  constructor(field: string, value: string, display = true) {
    const { clearedField, isExcluding } = this.parseAndClearField(field);

    this.value = value;
    this.display = display;

    this.clearedField = clearedField;
    this.isExcluding = isExcluding;
  }

  private parseAndClearField(field: string): { clearedField: string, isExcluding: boolean } {
    field = field.replace(/\*/, ''); // removing wildcard caracter
    field = field.trim(); // removing whitespaces
    const isExcluding = this.excludeOperatorRxp.test(field); // looking for excluding operator
    const clearedField = field.replace(this.excludeOperatorRxp, ''); // removing exlude operator

    return { clearedField, isExcluding };
  }

  getQueryString(): string {
    if (this.clearedField === GIUD_FIELD_NAME) {
      let valueWithQuote = '\"' + this.value + '\"';
      return this.createNestedQuery(this.clearedField, valueWithQuote);
    }

    if (this.clearedField === TIMESTAMP_FIELD_NAME && !this.display) {
      this.dateFilterValue = Utils.timeRangeToDateObj(this.value);
      if (this.dateFilterValue !== null && this.dateFilterValue.toDate !== null) {
        return this.createNestedQuery(this.clearedField,
            '[' + this.dateFilterValue.fromDate + ' TO ' + this.dateFilterValue.toDate + ']');
      } else {
        return this.createNestedQuery(this.clearedField,  this.value);
      }
    }

    return this.createNestedQuery(this.clearedField, this.value);
  }

  private createNestedQuery(field: string, value: string): string {
    field = this.escapingESSpearators(field);

    return this.operatorToAdd() + '(' + field + ':' +  value  + ' OR ' +
      this.addElasticSearchPrefix('metron_alert', field) + ':' + value + ')';
  }

  private escapingESSpearators(field: string): string {
    return field.replace(/\:/g, '\\:');
  }

  private operatorToAdd() {
    return this.isExcluding ? this.excludeOperator : '';
  }

  private addElasticSearchPrefix(prefix: string, field: string): string {
    return prefix + '.' + field;
  }

  equals(filter: Filter): boolean {
    return this.field === filter.field && this.value === filter.value;
  }
}
