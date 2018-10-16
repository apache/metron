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
import {Utils} from './utils'

describe('timeRangeToDisplayStr', () => {

  beforeAll(() => {
    jasmine.clock().mockDate(new Date('1998-06-14 16:00:00'))
  })

  afterAll(() => {
    jasmine.clock().uninstall()
  })

  const tests = [{
    input: 'last-7-days',
    expected: {
      fromDate: '1998-06-07 16:00:00',
      toDate: '1998-06-14 16:00:00'
    }
  }, {
    input: 'last-30-days',
    expected: {
      fromDate: '1998-05-15 16:00:00',
      toDate: '1998-06-14 16:00:00'
    }
  }, {
    input: 'last-60-days',
    expected: {
      fromDate: '1998-04-15 16:00:00',
      toDate: '1998-06-14 16:00:00'
    }
  }, {
    input: 'last-90-days',
    expected: {
      fromDate: '1998-03-16 16:00:00',
      toDate: '1998-06-14 16:00:00'
    }
  }, {
    input: 'last-6-months',
    expected: {
      fromDate: '1997-12-14 16:00:00',
      toDate: '1998-06-14 16:00:00'
    }
  }, {
    input: 'last-1-year',
    expected: {
      fromDate: '1997-06-14 16:00:00',
      toDate: '1998-06-14 16:00:00'
    }
  }, {
    input: 'last-2-years',
    expected: {
      fromDate: '1996-06-14 16:00:00',
      toDate: '1998-06-14 16:00:00'
    }
  }, {
    input: 'last-5-years',
    expected: {
      fromDate: '1993-06-14 16:00:00',
      toDate: '1998-06-14 16:00:00'
    }
  }, {
    input: 'all-time',
    expected: {
      fromDate: '1970-01-01 00:00:00',
      toDate: '2100-01-01 00:00:00'
    }
  }, {
    input: 'yesterday',
    expected: {
      fromDate: '1998-06-13 00:00:00',
      toDate: '1998-06-13 23:59:59'
    }
  }, {
    input: 'day-before-yesterday',
    expected: {
      fromDate: '1998-06-12 00:00:00',
      toDate: '1998-06-12 23:59:59'
    }
  }, {
    input: 'this-day-last-week',
    expected: {
      fromDate: '1998-06-07 00:00:00',
      toDate: '1998-06-07 23:59:59'
    }
  }, {
    input: 'previous-week',
    expected: {
      fromDate: '1998-06-07 00:00:00',
      toDate: '1998-06-13 23:59:59'
    }
  }, {
    input: 'previous-month',
    expected: {
      fromDate: '1998-05-01 00:00:00',
      toDate: '1998-05-31 23:59:59'
    }
  }, {
    input: 'previous-year',
    expected: {
      fromDate: '1997-01-01 00:00:00',
      toDate: '1997-12-31 23:59:59'
    }
  }, {
    input: 'today',
    expected: {
      fromDate: '1998-06-14 00:00:00',
      toDate: '1998-06-14 23:59:59'
    }
  }, {
    input: 'today-so-far',
    expected: {
      fromDate: '1998-06-14 00:00:00',
      toDate: '1998-06-14 16:00:00'
    }
  }, {
    input: 'this-week',
    expected: {
      fromDate: '1998-06-14 00:00:00',
      toDate: '1998-06-20 23:59:59'
    }
  }, {
    input: 'this-week-so-far',
    expected: {
      fromDate: '1998-06-14 00:00:00',
      toDate: '1998-06-14 16:00:00'
    }
  }, {
    input: 'this-month',
    expected: {
      fromDate: '1998-06-01 00:00:00',
      toDate: '1998-06-30 23:59:59'
    }
  }, {
    input: 'this-year',
    expected: {
      fromDate: '1998-01-01 00:00:00',
      toDate: '1998-12-31 23:59:59'
    }
  }, {
    input: 'last-5-minutes',
    expected: {
      fromDate: '1998-06-14 15:55:00',
      toDate: '1998-06-14 16:00:00'
    }
  }, {
    input: 'last-15-minutes',
    expected: {
      fromDate: '1998-06-14 15:45:00',
      toDate: '1998-06-14 16:00:00'
    }
  }, {
    input: 'last-30-minutes',
    expected: {
      fromDate: '1998-06-14 15:30:00',
      toDate: '1998-06-14 16:00:00'
    }
  }, {
    input: 'last-1-hour',
    expected: {
      fromDate: '1998-06-14 15:00:00',
      toDate: '1998-06-14 16:00:00'
    }
  }, {
    input: 'last-3-hours',
    expected: {
      fromDate: '1998-06-14 13:00:00',
      toDate: '1998-06-14 16:00:00'
    }
  }, {
    input: 'last-6-hours',
    expected: {
      fromDate: '1998-06-14 10:00:00',
      toDate: '1998-06-14 16:00:00'
    }
  }, {
    input: 'last-12-hours',
    expected: {
      fromDate: '1998-06-14 04:00:00',
      toDate: '1998-06-14 16:00:00'
    }
  }, {
    input: 'last-24-hours',
    expected: {
      fromDate: '1998-06-13 16:00:00',
      toDate: '1998-06-14 16:00:00'
    }
  }]

  tests.forEach((test) => {
    it('should work with: ' + test.input, () => {
      const actual = Utils.timeRangeToDisplayStr(test.input)
      const expected = test.expected

      expect(actual.fromDate).toBe(expected.fromDate)
      expect(actual.toDate).toBe(expected.toDate)
    })
  })
})