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
import { Utils } from './utils';
import { DateFilterValue } from 'app/model/date-filter-value';

describe('utils.Utils', () => {

  it('Converting time range based on From/To', () => {
    expect(Utils.timeRangeToDateObj('949273200000 TO 951692400000')).toEqual(new DateFilterValue(949273200000, 951692400000));
  });

  describe('Converting quick range to date object', () => {
    const getTestObj = (label, rangeId, from, to) => {
      return {
        label,
        rangeId,
        from: new Date(from).getTime(),
        to: new Date(to).getTime(),
      }
    }

    [
      getTestObj('Last 7 days', 'last-7-days', '1999-12-25T12:00:00', '2000-01-01T12:00:00'),
      getTestObj('Last 30 days', 'last-30-days', '1999-12-02T12:00:00', '2000-01-01T12:00:00'),
      getTestObj('Last 60 days', 'last-60-days', '1999-11-02T12:00:00', '2000-01-01T12:00:00'),
      getTestObj('Last 90 days', 'last-90-days', '1999-10-03T12:00:00', '2000-01-01T12:00:00'),
      getTestObj('Last 6 months', 'last-6-months', '1999-07-01T12:00:00', '2000-01-01T12:00:00'),
      getTestObj('Last 1 year', 'last-1-year', '1999-01-01T12:00:00', '2000-01-01T12:00:00'),
      getTestObj('Last 2 years', 'last-2-years', '1998-01-01T12:00:00', '2000-01-01T12:00:00'),
      getTestObj('Last 5 years', 'last-5-years', '1995-01-01T12:00:00', '2000-01-01T12:00:00'),
      getTestObj('Yesterday', 'yesterday', '1999-12-31T00:00:00', '1999-12-31T23:59:59'),
      getTestObj('Day before yesterday', 'day-before-yesterday', '1999-12-30T00:00:00', '1999-12-30T23:59:59'),
      getTestObj('This day last week', 'this-day-last-week', '1999-12-25T00:00:00', '1999-12-25T23:59:59'),
      getTestObj('Previous week', 'previous-week', '1999-12-19T00:00:00', '1999-12-25T23:59:59'),
      getTestObj('Previous month', 'previous-month', '1999-12-01T00:00:00', '1999-12-31T23:59:59'),
      getTestObj('Previous year', 'previous-year', '1999-01-01T00:00:00', '1999-12-31T23:59:59'),
      getTestObj('All time', 'all-time', '1970-01-01T05:30:00+05:30', '2100-01-01T05:30:00+05:30'),
      getTestObj('Today', 'today', '2000-01-01T00:00:00', '2000-01-01T23:59:59'),
      getTestObj('Today so far', 'today-so-far', '2000-01-01T00:00:00', '2000-01-01T12:00:00'),
      getTestObj('This week', 'this-week', '1999-12-26T00:00:00', '2000-01-01T23:59:59'),
      getTestObj('This week so far', 'this-week-so-far', '1999-12-26T00:00:00', '2000-01-01T12:00:00'),
      getTestObj('This month', 'this-month', '2000-01-01T00:00:00', '2000-01-31T23:59:59'),
      getTestObj('This year', 'this-year', '2000-01-01T00:00:00', '2000-12-31T23:59:59'),
      getTestObj('Last 5 minutes', 'last-5-minutes', '2000-01-01T11:55:00', '2000-01-01T12:00:00'),
      getTestObj('Last 15 minutes', 'last-15-minutes', '2000-01-01T11:45:00', '2000-01-01T12:00:00'),
      getTestObj('Last 30 minutes', 'last-30-minutes', '2000-01-01T11:30:00', '2000-01-01T12:00:00'),
      getTestObj('Last 1 hour', 'last-1-hour', '2000-01-01T11:00:00', '2000-01-01T12:00:00'),
      getTestObj('Last 3 hours', 'last-3-hours', '2000-01-01T09:00:00', '2000-01-01T12:00:00'),
      getTestObj('Last 6 hours', 'last-6-hours', '2000-01-01T06:00:00', '2000-01-01T12:00:00'),
      getTestObj('Last 12 hours', 'last-12-hours', '2000-01-01T00:00:00', '2000-01-01T12:00:00'),
      getTestObj('Last 24 hours', 'last-24-hours', '1999-12-31T12:00:00', '2000-01-01T12:00:00'),
    ].forEach((test) => {
      it(`Converting ${test.label} to DateFilterValue`, () => {
        jasmine.clock().mockDate(new Date('2000-01-01T12:00:00'));

        expect(Utils.timeRangeToDateObj(test.rangeId)).toEqual(new DateFilterValue(test.from, test.to));
      })
    });

  });
});
