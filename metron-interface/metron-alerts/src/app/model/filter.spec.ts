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
import { Filter } from './filter';
import { TIMESTAMP_FIELD_NAME } from 'app/utils/constants';
import { Utils } from 'app/utils/utils';

describe('model.Filter', () => {

  const expectedTimeRangeQueryString = '(timestamp:[1552863600000 TO 1552950000000] OR ' +
   'metron_alert.timestamp:[1552863600000 TO 1552950000000])';

  it('should have getQueryString function', () => {
    const filter = new Filter('testField', 'someValue', false);
    expect(typeof filter.getQueryString).toBe('function');
  });

  it('getQueryString for basic filter', () => {
    const filter = new Filter('testField', 'someValue', false);
    expect(filter.getQueryString()).toBe('(testField:someValue OR metron_alert.testField:someValue)');
  });

  it('getQueryString for guid filter', () => {
    const filter = new Filter('guid', 'someValue', false);
    expect(filter.getQueryString()).toBe('(guid:"someValue" OR metron_alert.guid:"someValue")');
  });

  it('getQueryString for time range filter with []', () => {
    const filter = new Filter(TIMESTAMP_FIELD_NAME, '[1552863600000 TO 1552950000000]', false);
    expect(filter.getQueryString()).toBe(expectedTimeRangeQueryString);
  });

  it('getQueryString for time range filter without []', () => {
    const filter = new Filter(TIMESTAMP_FIELD_NAME, '1552863600000 TO 1552950000000', false);
    expect(filter.getQueryString()).toBe(expectedTimeRangeQueryString);
  });

  it('getQueryString for time range filter for display', () => {
    const filter = new Filter(TIMESTAMP_FIELD_NAME, '[1552863600000 TO 1552950000000]', true);
    expect(filter.getQueryString()).toBe('(timestamp:\\[1552863600000\\ TO\\ 1552950000000\\] OR ' +
      'metron_alert.timestamp:\\[1552863600000\\ TO\\ 1552950000000\\])');
  });

  /**
   * Actual time range and quick range conversion tested in utils/utils.spec.ts
   * until further refactoring.
   */
  it('getQueryString for time range filter should call Utils.timeRangeToDateObj', () => {
    const timeRange = '1552863600000 TO 1552950000000';
    spyOn(Utils, 'timeRangeToDateObj').and.callThrough();
    const filter = new Filter(TIMESTAMP_FIELD_NAME, timeRange, false);

    filter.getQueryString();
    expect(Utils.timeRangeToDateObj).toHaveBeenCalledWith(timeRange);
  });
});
