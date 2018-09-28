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
import { DEFAULT_START_TIME, DEFAULT_END_TIME, DEFAULT_TIMESTAMP_FORMAT, META_ALERTS_SENSOR_TYPE } from './constants';
import { Alert } from '../model/alert';
import { DateFilterValue } from '../model/date-filter-value';
import { PcapRequest } from '../pcap/model/pcap.request';
import { PcapFilterFormValue } from '../pcap/pcap-filters/pcap-filters.component';
import { FormGroup } from '@angular/forms';
import {
  subMinutes,
  subHours,
  subDays,
  subWeeks,
  subMonths,
  subYears,
  startOfDay,
  startOfWeek,
  startOfMonth,
  startOfYear,
  endOfDay,
  endOfWeek,
  endOfMonth,
  endOfYear,
  format
} from 'date-fns';

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

  public static timeRangeToDateObj(range:string) {
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

  public static parseTimeRange(range:string) {
    let parsed = range.replace(/^\(>=/, '')
    .replace(/\)$/, '')
    .replace(/<=/, '').split('AND');
    if (parsed.length === 2 && !isNaN(Number(parsed[0])) && !isNaN(Number(parsed[1]))) {
      return {toDate: Number(parsed[1]), fromDate: Number(parsed[0])};
    }
    if (parsed.length === 1 && !isNaN(Number(parsed[0]))) {
      return {toDate: null, fromDate: Number(parsed[0])};
    }

    return null;
  }

  public static timeRangeToDisplayStr(range:string) {
    let toDate;
    let fromDate;

    switch (range) {
      case 'last-7-days':
        fromDate = subDays(Date.now(), 7);
        toDate = Date.now();
        break;
      case 'last-30-days':
        fromDate = subDays(Date.now(), 30);
        toDate = Date.now();
        break;
      case 'last-60-days':
        fromDate = subDays(Date.now(), 60);
        toDate = Date.now();
        break;
      case 'last-90-days':
        fromDate = subDays(Date.now(), 90);
        toDate = Date.now();
        break;
      case 'last-6-months':
        fromDate = subMonths(Date.now(), 6);
        toDate = Date.now();
        break;
      case 'last-1-year':
        fromDate = subYears(Date.now(), 1);
        toDate = Date.now();
        break;
      case 'last-2-years':
        fromDate = subYears(Date.now(), 2);
        toDate = Date.now();
        break;
      case 'last-5-years':
        fromDate = subYears(Date.now(), 5);
        toDate = Date.now();
        break;
      case 'all-time':
        fromDate = '1970-01-01';
        toDate = '2100-01-01';
        break;
      case 'yesterday':
        fromDate = startOfDay(subDays(Date.now(), 1));
        toDate = endOfDay(subDays(Date.now(), 1));
        break;
      case 'day-before-yesterday':
        fromDate = startOfDay(subDays(Date.now(), 2));
        toDate = endOfDay(subDays(Date.now(), 2));
        break;
      case 'this-day-last-week':
        fromDate = startOfDay(subDays(Date.now(), 7));
        toDate = endOfDay(subDays(Date.now(), 7));
        break;
      case 'previous-week':
        fromDate = startOfWeek(subWeeks(Date.now(), 1));
        toDate = endOfWeek(subWeeks(Date.now(), 1));
        break;
      case 'previous-month':
        fromDate = startOfMonth(subMonths(Date.now(), 1));
        toDate = endOfMonth(subMonths(Date.now(), 1));
        break;
      case 'previous-year':
        fromDate = startOfYear(subYears(Date.now(), 1));
        toDate = endOfYear(subYears(Date.now(), 1));
        break;
      case 'today':
        fromDate = startOfDay(Date.now());
        toDate = endOfDay(Date.now());
        break;
      case 'today-so-far':
        fromDate = startOfDay(Date.now());
        toDate = Date.now();
        break;
      case 'this-week':
        fromDate = startOfWeek(Date.now());
        toDate = endOfWeek(Date.now());
        break;
      case 'this-week-so-far':
        fromDate = startOfWeek(Date.now());
        toDate = Date.now();
        break;
      case 'this-month':
        fromDate = startOfMonth(Date.now());
        toDate = endOfMonth(Date.now());
        break;
      case 'this-year':
        fromDate = startOfYear(Date.now());
        toDate = endOfYear(Date.now());
        break;
      case 'last-5-minutes':
        fromDate = subMinutes(Date.now(), 5);
        toDate = Date.now();
        break;
      case 'last-15-minutes':
        fromDate = subMinutes(Date.now(), 15);
        toDate = Date.now();
        break;
      case 'last-30-minutes':
        fromDate = subMinutes(Date.now(), 30);
        toDate = Date.now();
        break;
      case 'last-1-hour':
        fromDate = subMinutes(Date.now(), 60);
        toDate = Date.now();
        break;
      case 'last-3-hours':
        fromDate = subHours(Date.now(), 3);
        toDate = Date.now();
        break;
      case 'last-6-hours':
        fromDate = subHours(Date.now(), 6);
        toDate = Date.now();
        break;
      case 'last-12-hours':
        fromDate = subHours(Date.now(), 12);
        toDate = Date.now();
        break;
      case 'last-24-hours':
        fromDate = subHours(Date.now(), 24);
        toDate = Date.now();
        break;
      default:
        return null;
    }

    toDate = format(toDate, DEFAULT_TIMESTAMP_FORMAT);
    fromDate = format(fromDate, DEFAULT_TIMESTAMP_FORMAT);

    return {
      fromDate,
      toDate
    };
  }
}
