/// <reference path="../matchers/custom-matchers.d.ts"/>
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
import { MetronAlertsPage } from './alerts-list.po';
import { customMatchers } from  '../matchers/custom-matchers';
import { LoginPage } from '../login/login.po';
import { loadTestData, deleteTestData } from '../utils/e2e_util';
import {browser} from "protractor/built";

describe('Test spec for all ui elements & list view', function() {
  let page: MetronAlertsPage;
  let loginPage: LoginPage;
  let columnNames = [ '', 'Score', 'id', 'timestamp', 'source:type', 'ip_src_addr', 'enrichm...:country',
                      'ip_dst_addr', 'host', 'alert_status', '', '', ''];
  let colNamesColumnConfig = [ 'score', 'id', 'timestamp', 'source:type', 'ip_src_addr', 'enrichments:geo:ip_dst_addr:country',
                                'ip_dst_addr', 'host', 'alert_status' ];

  beforeAll(async function() : Promise<any> {
    loginPage = new LoginPage();
    loginPage.login();

    await loadTestData();
  });

  afterAll(async function() : Promise<any> {
    loginPage.logout();
    await deleteTestData();
  });

  beforeEach(() => {
    page = new MetronAlertsPage();
    jasmine.addMatchers(customMatchers);
  });

  it('should have all the UI elements', () => {
    page.navigateTo();
    page.clearLocalStorage();

    expect(page.getChangesAlertTableTitle('Alerts (0)')).toEqualBcoz('Alerts (169)', 'for alerts title');

    expect(page.isMetronLogoPresent()).toEqualBcoz(true, 'for Metron Logo');
    expect(page.isSavedSearchButtonPresent()).toEqualBcoz(true, 'for SavedSearch Button');
    expect(page.isClearSearchPresent()).toEqualBcoz(true, 'for Clear Search');
    expect(page.isSearchButtonPresent()).toEqualBcoz(true, 'for Search Button');
    expect(page.isSaveSearchButtonPresent()).toEqualBcoz(true, 'for Save Search Button');
    expect(page.isTableSettingsButtonPresent()).toEqualBcoz(true, 'for table settings button');
    expect(page.isPausePlayRefreshButtonPresent()).toEqualBcoz(true, 'for pause/play button');
    expect(page.isConfigureTableColumnsPresent()).toEqualBcoz(true, 'for alerts table column configure button');

    expect(page.getActionDropdownItems()).toEqualBcoz([ 'Open', 'Dismiss', 'Escalate', 'Resolve', 'Add to Alert' ],
                                                        'for default dropdown actions');
    expect(page.getTableColumnNames()).toEqualBcoz(columnNames, 'for default column names for alert list table');
  });

  it('should have all pagination controls and they should be working', async function() : Promise<any> {

    await page.clickSettings();
    await page.clickPageSize('100');

    expect(page.getChangedPaginationText('1 - 25 of 169')).toEqualBcoz('1 - 100 of 169', 'for pagination text');
    expect(page.isChevronLeftEnabled()).toEqualBcoz(false, 'for left chevron to be disabled for first page');
    expect(page.isChevronRightEnabled()).toEqualBcoz(true, 'for right chevron to be enabled for first page');

    await page.clickChevronRight();
    expect(page.getChangedPaginationText('1 - 100 of 169')).toEqualBcoz('101 - 169 of 169', 'for pagination text');
    expect(page.isChevronLeftEnabled()).toEqualBcoz(true, 'for left chevron to be disabled for first page');
    expect(page.isChevronRightEnabled()).toEqualBcoz(false, 'for right chevron to be enabled for first page');

    await page.clickChevronLeft();
    expect(page.getChangedPaginationText('101 - 169 of 169')).toEqualBcoz('1 - 100 of 169', 'for pagination text');
    expect(page.isChevronLeftEnabled()).toEqualBcoz(false, 'for left chevron to be disabled for first page');
    expect(page.isChevronRightEnabled()).toEqualBcoz(true, 'for right chevron to be enabled for first page');

    await page.clickSettings();
    await page.clickPageSize('25');
    expect(page.getChangedPaginationText('1 - 100 of 169')).toEqualBcoz('1 - 25 of 169', 'for pagination text');

    await page.clickSettings();
  });

  it('should have all settings controls and they should be working', async function() : Promise<any> {
    let settingsPaneLbelNames = [ 'REFRESH RATE', 'ROWS PER PAGE', 'HIDE Resolved Alerts', 'HIDE Dismissed Alerts' ];
    let settingPaneRefreshIntervals = [ '5s', '10s', '15s', '30s', '1m', '10m', '1h' ];
    let settingsPanePageSize = [ '10', '25', '50', '100', '250', '500', '1000' ];

    await page.clickSettings();

    expect(page.getSettingsLabels()).toEqualBcoz(settingsPaneLbelNames, 'for table settings labels');

    expect(page.getRefreshRateOptions()).toEqualBcoz(settingPaneRefreshIntervals, 'for table settings refresh rate labels');
    expect(page.getRefreshRateSelectedOption()).toEqualBcoz([ '1m' ], 'for table settings default refresh rate');

    await  page.clickRefreshInterval('10s');
    expect(page.getRefreshRateSelectedOption()).toEqualBcoz([ '10s' ], 'for refresh interval 10s');

    await page.clickRefreshInterval('1h');
    expect(page.getRefreshRateSelectedOption()).toEqualBcoz([ '1h' ], 'for refresh interval 1h');

    expect(page.getPageSizeOptions()).toEqualBcoz(settingsPanePageSize, 'for table settings refresh rate labels');
    expect(page.getPageSizeSelectedOption()).toEqualBcoz([ '25' ], 'for table settings default page size');

    await page.clickPageSize('50');
    expect(page.getPageSizeSelectedOption()).toEqualBcoz([ '50' ], 'for page size 50');

    await page.clickPageSize('100');
    expect(page.getPageSizeSelectedOption()).toEqualBcoz([ '100' ], 'for page size 100');

    await page.clickPageSize('25');
    expect(page.getPageSizeSelectedOption()).toEqualBcoz([ '25' ], 'for page size 100');

    await page.clickSettings();
  });

  it('play pause should start polling and stop polling ', async function() : Promise<any> {
    expect(page.getPlayPauseState()).toEqual('fa fa-play', 'for default pause option');

    await page.clickPlayPause('fa-pause');
    expect(page.getPlayPauseState()).toEqual('fa fa-pause', 'for default pause option');

    await page.clickPlayPause('fa-play');
    expect(page.getPlayPauseState()).toEqual('fa fa-play', 'for default pause option');
  });

  it('should select columns from table configuration', () => {
    let newColNamesColumnConfig = [ 'score', 'timestamp', 'source:type', 'ip_src_addr', 'enrichments:geo:ip_dst_addr:country',
      'ip_dst_addr', 'host', 'alert_status', 'guid' ];

    page.clickConfigureTable();
    expect(page.getSelectedColumnNames()).toEqual(colNamesColumnConfig, 'for default selected column names');
    page.toggleSelectCol('id');
    page.toggleSelectCol('guid', 'method');
    expect(page.getSelectedColumnNames()).toEqual(newColNamesColumnConfig, 'for guid added to selected column names');
    page.saveConfigureColumns();

  });

  it('should have all time-range controls', () => {
    let quickRanges = [
      'Last 7 days', 'Last 30 days', 'Last 60 days', 'Last 90 days', 'Last 6 months', 'Last 1 year', 'Last 2 years', 'Last 5 years',
      'Yesterday', 'Day before yesterday', 'This day last week', 'Previous week', 'Previous month', 'Previous year', 'All time',
      'Today', 'Today so far', 'This week', 'This week so far', 'This month', 'This year',
      'Last 5 minutes', 'Last 15 minutes', 'Last 30 minutes', 'Last 1 hour', 'Last 3 hours', 'Last 6 hours', 'Last 12 hours', 'Last 24 hours'
    ];

    page.clickDateSettings();
    expect(page.getTimeRangeTitles()).toEqual(['Time Range', 'Quick Ranges']);
    expect(page.getQuickTimeRanges()).toEqual(quickRanges);
    expect(page.getValueForManualTimeRange()).toEqual([ 'now', 'now' ]);
    expect(page.isManulaTimeRangeApplyButtonPresent()).toEqual(true);
    expect(page.getTimeRangeButtonText()).toEqual('All time');
    page.hideDateSettings();

  });

  it('should have all time range values populated - 1', () => {
    let secInADay = (24 * 60 * 60 * 1000);

    page.clickDateSettings();
    expect(page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 7 days')).toEqualBcoz(['Last 7 days', String(secInADay * 7)], 'for last 7 days');

    page.clickDateSettings();
    expect(page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 30 days')).toEqualBcoz(['Last 30 days', String(secInADay * 30)], 'for last 30 days');

    page.clickDateSettings();
    expect(page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 60 days')).toEqualBcoz(['Last 60 days', String(secInADay * 60)], 'for last 60 days');

    page.clickDateSettings();
    expect(page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 90 days')).toEqualBcoz(['Last 90 days', String(secInADay * 90)], 'for last 90 days');

    page.clickDateSettings();
    expect(page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 1 year')).toEqualBcoz(['Last 1 year', String(secInADay * 365)], 'for last 1 year');

    page.clickDateSettings();
    expect(page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 2 years')).toEqualBcoz(['Last 2 years', String((secInADay * 365 * 2) + secInADay)], 'for last 2 years');

    page.clickDateSettings();
    expect(page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 5 years')).toEqualBcoz(['Last 5 years', String((secInADay * 365 * 5) + secInADay)], 'for last 5 years');

    page.clickClearSearch();
  });

  it('should have all time range values populated - 2', () => {
    let secInADay = (24*60*60*1000);

    page.clickDateSettings();
    expect(page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Yesterday')).toEqualBcoz([ 'Yesterday', String(secInADay - 1000)], 'yesterday');

    page.clickDateSettings();
    expect(page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Day before yesterday')).toEqualBcoz([ 'Day before yesterday', String(secInADay - 1000)], 'day before yesterday');

    page.clickDateSettings();
    expect(page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('This day last week')).toEqualBcoz([ 'This day last week', String(secInADay - 1000)], 'this day last week');

    page.clickDateSettings();
    expect(page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Previous week')).toEqualBcoz([ 'Previous week', String((secInADay * 7) - (1000))], 'for previous week');

    page.clickClearSearch();
  });

  it('should have all time range values populated - 3', () => {
    let secInADay = (24*60*60*1000);

    page.clickDateSettings();
    expect(page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Today')).toEqualBcoz([ 'Today', String(secInADay - 1000)], 'for today');

    page.clickDateSettings();
    expect(page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('This week')).toEqualBcoz([ 'This week', String((secInADay*7) - 1000)], 'for this week');

    page.clickClearSearch();
  });

  it('should have all time range values populated - 4', () => {
    let secInADay = (24*60*60*1000);

    page.clickDateSettings();
    expect(page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 5 minutes')).toEqualBcoz([ 'Last 5 minutes', String(5 * 60 * 1000)], 'for last 5 minutes');

    page.clickDateSettings();
    expect(page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 15 minutes')).toEqualBcoz([ 'Last 15 minutes', String(15 * 60 * 1000)], 'for last 15 minutes');

    page.clickDateSettings();
    expect(page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 30 minutes')).toEqualBcoz([ 'Last 30 minutes', String(30 * 60 * 1000)], 'for last 30 minutes');

    page.clickDateSettings();
    expect(page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 1 hour')).toEqualBcoz([ 'Last 1 hour', String(60 * 60 * 1000)], 'for last 1 hour');

    page.clickDateSettings();
    expect(page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 3 hours')).toEqualBcoz([ 'Last 3 hours', String(3 * 60 * 60 * 1000)], 'for last 3 hours');

    page.clickDateSettings();
    expect(page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 6 hours')).toEqualBcoz([ 'Last 6 hours', String(6 * 60 * 60 * 1000)], 'for last 6 hours');

    page.clickDateSettings();
    expect(page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 12 hours')).toEqualBcoz([ 'Last 12 hours', String(12 * 60 * 60 * 1000)], 'for last 12 hours');

    page.clickDateSettings();
    expect(page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 24 hours')).toEqualBcoz([ 'Last 24 hours', String(24 * 60 * 60 * 1000)], 'for last 24 hours');

    page.clickClearSearch();
  });

  it('should disable date picker when timestamp is present in search', () => {
    page.setSearchText('timestamp:1505325740512');
    expect(page.getChangesAlertTableTitle('Alerts (169)')).toEqual('Alerts (1)');
    expect(page.isDateSeettingDisabled()).toEqual(true);

    page.clickClearSearch();
    expect(page.getChangesAlertTableTitle('Alerts (1)')).toEqual('Alerts (169)');
    expect(page.isDateSeettingDisabled()).toEqual(false);

    expect(page.clickTableTextAndGetSearchText('FR', 'enrichments:geo:ip_dst_addr:country:FR')).toEqual('enrichments:geo:ip_dst_addr:country:FR');
    expect(page.getChangesAlertTableTitle('Alerts (169)')).toEqual('Alerts (25)');
    expect(page.isDateSeettingDisabled()).toEqual(false);

    page.clickClearSearch();
    expect(page.getChangesAlertTableTitle('Alerts (25)')).toEqual('Alerts (169)');
  });

  it('should have now included when to date is empty', () => {
    page.clickDateSettings();
    page.setDate(0, '2017', 'September', '13', '23', '29', '35');
    page.selectTimeRangeApplyButton();
    expect(page.getTimeRangeButtonTextForNow()).toEqual([ 'Date Range', '2017-09-13 23:29:35 to now' ]);

    page.clickClearSearch();
  });
  
  it('should have all time-range included while searching', () => {
    let startDate = new Date(1505325575000);
    let endDate = new Date(1505325580000);

    page.clearLocalStorage();
    page.clickDateSettings();

    /* Select Last 5years for time range */
    expect(page.selectQuickTimeRangeAndGetTimeRangeText('Last 5 years')).toEqual('Last 5 years');

    /* Select custom date for time range */
    page.clickDateSettings();
    page.setDate(0, String(startDate.getFullYear()), startDate.toLocaleString('en-us', { month: "long" }), String(startDate.getDate()),
                String(startDate.getHours()), String(startDate.getMinutes()), String(startDate.getSeconds()));
    page.setDate(1, String(endDate.getFullYear()), endDate.toLocaleString('en-us', { month: "long" }), String(endDate.getDate()),
                String(endDate.getHours()), String(endDate.getMinutes()), String(endDate.getSeconds()));
    page.selectTimeRangeApplyButton();
    expect(page.getChangesAlertTableTitle('Alerts (169)')).toEqual('Alerts (5)');

    /* Save custom date in saved searches */
    page.saveSearch('e2e-2');
    page.clickSavedSearch();
    expect(page.getRecentSearchOptions()).toContain('timestamp:last-5-years', 'for recent search options');
    expect(page.getSavedSearchOptions()).toEqual(['e2e-2'],
                                                    'for saved search options');
    page.clickCloseSavedSearch();

    /* Clear Search should should show all rows */
    page.clickClearSearch();
    expect(page.getChangesAlertTableTitle('Alerts (5)')).toEqual('Alerts (169)');

    /* Load the saved search */
    page.clickSavedSearch();
    page.loadSavedSearch('e2e-2');
    expect(page.getChangesAlertTableTitle('Alerts (169)')).toEqual('Alerts (5)');

    /* Load recent search */
    page.clickSavedSearch();
    page.loadRecentSearch('last-5-years');
    expect(page.getChangesAlertTableTitle('Alerts (5)')).toEqual('Alerts (169)');

  });

});
