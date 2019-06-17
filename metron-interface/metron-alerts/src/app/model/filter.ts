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
import {Utils} from '../utils/utils';
import {TIMESTAMP_FIELD_NAME} from '../utils/constants';
import {DateFilterValue} from './date-filter-value';

export class Filter {
  field: string;
  value: string;
  display: boolean;
  dateFilterValue: DateFilterValue;

  static fromJSON(objs: Filter[]): Filter[] {
    let filters = [];
    if (objs) {
      for (let obj of objs) {
        filters.push(new Filter(obj.field, obj.value, obj.display));
      }
    }
    return filters;
  }

  constructor(field: string, value: string, display = true) {
    this.field = field;
    this.value = value;
    this.display = display;
  }

  getQueryString(): string {
    if (this.field === 'guid') {
      let valueWithQuote = '\"' + this.value + '\"';
      return this.createNestedQuery(this.field, valueWithQuote);
    }

    if (this.field === TIMESTAMP_FIELD_NAME && !this.display) {
      this.dateFilterValue = Utils.timeRangeToDateObj(this.value);
      if (this.dateFilterValue !== null && this.dateFilterValue.toDate !== null) {
        return this.createNestedQuery(this.field,
            '[' + this.dateFilterValue.fromDate + ' TO ' + this.dateFilterValue.toDate + ']');
      } else {
        return this.createNestedQuery(this.field,  this.value);
      }
    }

    return this.createNestedQuery(this.field, this.value);
  }

  private createNestedQuery(field: string, value: string): string {
    return '(' + field + ':' +  value  + ' OR ' + this.addPrefix('metron_alert', field) + ':' + value + ')';
  }

  private addPrefix(prefix: string, field: string): string {
    const excludeOperatorRxp = /^-/;
    const isExcluding = excludeOperatorRxp.test(field);
    const operatorToAdd = isExcluding ? '-' : '';
    const clearedField = field.replace(excludeOperatorRxp, '');

    return operatorToAdd + prefix + '.' + clearedField;
  }

  equals(filter: Filter): boolean {
    return this.field === filter.field && this.value === filter.value;
  }
}
