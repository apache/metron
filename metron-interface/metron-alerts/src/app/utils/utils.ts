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
import * as moment from 'moment/moment';

import { DEFAULT_START_TIME, DEFAULT_END_TIME, DEFAULT_TIMESTAMP_FORMAT, META_ALERTS_SENSOR_TYPE } from './constants';
import { Alert } from '../model/alert';
import { DateFilterValue } from '../model/date-filter-value';
import { PcapRequest } from '../pcap/model/pcap.request';
import { PcapFilterFormValue } from '../pcap/pcap-filters/pcap-filters.component';
import { FormGroup } from '@angular/forms';

export class Utils {

  public static escapeESField(field: string): string {
    return field.replace(/:/g, '\\:');
  }

  public static escapeESValue(value: string): string {
    return String(value)
    .replace(/[\*\+\-=~><\"\?^\${}\(\)\:\!\/[\]\\\s]/g, '\\$&') // replace single  special characters
    .replace(/\|\|/g, '\\||') // replace ||
    .replace(/\&\&/g, '\\&&'); // replace &&
  }

  public static getAlertSensorType(alert: Alert, sourceType: string): string {
    if (alert.source[sourceType] && alert.source[sourceType].length > 0) {
      return alert.source[sourceType];
    } else {
      return META_ALERTS_SENSOR_TYPE;
    }
  }

  public static timeRangeToDateObj(range: string): DateFilterValue {
    let timeRangeToDisplayStr = Utils.timeRangeToDisplayStr(range);
    if (timeRangeToDisplayStr != null) {
      let toDate = new Date((timeRangeToDisplayStr.toDate)).getTime();
      let fromDate = new Date((timeRangeToDisplayStr.fromDate)).getTime();

      return new DateFilterValue(fromDate, toDate);
    }
    let timeRangeToEpoc = Utils.parseTimeRange(range);
    if (timeRangeToEpoc !== null) {
      return new DateFilterValue(timeRangeToEpoc.fromDate, timeRangeToEpoc.toDate);
    }
    return null;
  }

  public static parseTimeRange(range: string): { toDate: number, fromDate: number } {
    let parsed = range.replace(/^\[/, '')
    .replace(/]$/, '')
    .split('TO');
    if (parsed.length === 2 && !isNaN(Number(parsed[0])) && !isNaN(Number(parsed[1]))) {
      return {toDate: Number(parsed[1]), fromDate: Number(parsed[0])};
    }
    if (parsed.length === 1 && !isNaN(Number(parsed[0]))) {
      return {toDate: null, fromDate: Number(parsed[0])};
    }

    return null;
  }

  public static timeRangeToDisplayStr(range: string): { toDate: string, fromDate: string } {
    let toDate = '';
    let fromDate = '';

    switch (range) {
      case 'last-7-days':
        fromDate = moment().subtract(7, 'days').local().format();
        toDate = moment().local().format();
        break;
      case 'last-30-days':
        fromDate = moment().subtract(30, 'days').local().format();
        toDate = moment().local().format();
        break;
      case 'last-60-days':
        fromDate = moment().subtract(60, 'days').local().format();
        toDate = moment().local().format();
        break;
      case 'last-90-days':
        fromDate = moment().subtract(90, 'days').local().format();
        toDate = moment().local().format();
        break;
      case 'last-6-months':
        fromDate = moment().subtract(6, 'months').local().format();
        toDate = moment().local().format();
        break;
      case 'last-1-year':
        fromDate = moment().subtract(1, 'year').local().format();
        toDate = moment().local().format();
        break;
      case 'last-2-years':
        fromDate = moment().subtract(2, 'years').local().format();
        toDate = moment().local().format();
        break;
      case 'last-5-years':
        fromDate = moment().subtract(5, 'years').local().format();
        toDate = moment().local().format();
        break;
      case 'all-time':
        fromDate = '1970-01-01T05:30:00+05:30';
        toDate = '2100-01-01T05:30:00+05:30';
        break;
      case 'yesterday':
        fromDate = moment().subtract(1, 'days').startOf('day').local().format();
        toDate = moment().subtract(1, 'days').endOf('day').local().format();
        break;
      case 'day-before-yesterday':
        fromDate = moment().subtract(2, 'days').startOf('day').local().format();
        toDate = moment().subtract(2, 'days').endOf('day').local().format();
        break;
      case 'this-day-last-week':
        fromDate = moment().subtract(7, 'days').startOf('day').local().format();
        toDate = moment().subtract(7, 'days').endOf('day').local().format();
        break;
      case 'previous-week':
        fromDate = moment().subtract(1, 'weeks').startOf('week').local().format();
        toDate = moment().subtract(1, 'weeks').endOf('week').local().format();
        break;
      case 'previous-month':
        fromDate = moment().subtract(1, 'months').startOf('month').local().format();
        toDate = moment().subtract(1, 'months').endOf('month').local().format();
        break;
      case 'previous-year':
        fromDate = moment().subtract(1, 'years').startOf('year').local().format();
        toDate = moment().subtract(1, 'years').endOf('year').local().format();
        break;
      case 'today':
        fromDate = moment().startOf('day').local().format();
        toDate = moment().endOf('day').local().format();
        break;
      case 'today-so-far':
        fromDate = moment().startOf('day').local().format();
        toDate = moment().local().format();
        break;
      case 'this-week':
        fromDate = moment().startOf('week').local().format();
        toDate = moment().endOf('week').local().format();
        break;
      case 'this-week-so-far':
        fromDate = moment().startOf('week').local().format();
        toDate = moment().local().format();
        break;
      case 'this-month':
        fromDate = moment().startOf('month').local().format();
        toDate = moment().endOf('month').local().format();
        break;
      case 'this-year':
        fromDate = moment().startOf('year').local().format();
        toDate = moment().endOf('year').local().format();
        break;
      case 'last-5-minutes':
        fromDate = moment().subtract(5, 'minutes').local().format();
        toDate = moment().local().format();
        break;
      case 'last-15-minutes':
        fromDate = moment().subtract(15, 'minutes').local().format();
        toDate = moment().local().format();
        break;
      case 'last-30-minutes':
        fromDate = moment().subtract(30, 'minutes').local().format();
        toDate = moment().local().format();
        break;
      case 'last-1-hour':
        fromDate = moment().subtract(60, 'minutes').local().format();
        toDate = moment().local().format();
        break;
      case 'last-3-hours':
        fromDate = moment().subtract(3, 'hours').local().format();
        toDate = moment().local().format();
        break;
      case 'last-6-hours':
        fromDate = moment().subtract(6, 'hours').local().format();
        toDate = moment().local().format();
        break;
      case 'last-12-hours':
        fromDate = moment().subtract(12, 'hours').local().format();
        toDate = moment().local().format();
        break;
      case 'last-24-hours':
        fromDate = moment().subtract(24, 'hours').local().format();
        toDate = moment().local().format();
        break;
      default:
        return null;
    }

    toDate = moment(toDate).format(DEFAULT_TIMESTAMP_FORMAT);
    fromDate = moment(fromDate).format(DEFAULT_TIMESTAMP_FORMAT);

    return {toDate: toDate, fromDate: fromDate};
  }
}
