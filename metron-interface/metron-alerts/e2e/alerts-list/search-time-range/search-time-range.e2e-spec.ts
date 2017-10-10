/// <reference path="../../matchers/custom-matchers.d.ts"/>
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
import { customMatchers } from  '../../matchers/custom-matchers';
import {MetronAlertsPage} from '../alerts-list.po';
import {LoginPage} from '../../login/login.po';
import {loadTestData, deleteTestData} from '../../utils/e2e_util';

describe('metron-alerts Search', function() {
  let page:MetronAlertsPage;
  let loginPage:LoginPage;

  beforeAll(() => {
    loadTestData();
    loginPage = new LoginPage();
    loginPage.login();
  });

  afterAll(() => {
    loginPage.logout();
    deleteTestData();
  });

  beforeEach(() => {
    page = new MetronAlertsPage();
    jasmine.addMatchers(customMatchers);
  });

  it('sould have all time-range controls', () => {
    let quickRanges = [
      'Last 7 days', 'Last 30 days', 'Last 60 days', 'Last 90 days', 'Last 6 months', 'Last 1 year', 'Last 2 years', 'Last 5 years',
      'Yesterday', 'Day before yesterday', 'This day last week', 'Previous week', 'Previous month', 'Previous year', 'All time',
      'Today', 'Today so far', 'This week', 'This week so far', 'This month', 'This year',
      'Last 5 minutes', 'Last 15 minutes', 'Last 30 minutes', 'Last 1 hour', 'Last 3 hours', 'Last 6 hours', 'Last 12 hours', 'Last 24 hours'
    ];

    page.clickDateSettings();
    expect(page.getTimeRangeTitles()).toEqual(['Time Range', 'Quick Ranges']);
    expect(page.getQuickTimeRanges()).toEqual(quickRanges);
    expect(page.getValueForManualTimeRange()).toEqual([ 'now/d', 'now/d' ]);
    expect(page.isManulaTimeRangeApplyButtonPresent()).toEqual(true);
    expect(page.getTimeRangeButtonText()).toEqual('All time');

  });

  it('sould have all time-range included while searching', () => {
    page.selectQuickTimeRange('Last 5 years');
    expect(page.getTimeRangeButtonText()).toEqual('Last 5 years');

    page.clickDateSettings();
    page.setDate(0, '2017', 'September', '13', '23', '29', '35');
    page.setDate(1, '2017', 'September', '13', '23', '29', '40');
    page.selectTimeRangeApplyButton();
    
    expect(page.getChangesAlertTableTitle('Alerts (169)')).toEqual('Alerts (5)');
  });

});