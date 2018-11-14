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

describe('Test spec for all ui elements & list view', function() {
  let page: MetronAlertsPage;
  let loginPage: LoginPage;
  let columnNames = [ '', 'Score', 'guid', 'timestamp', 'source:type', 'ip_src_addr', 'enrichm...:country',
                      'ip_dst_addr', 'host', 'alert_status', '', '', ''];
  let colNamesColumnConfig = [ 'score', 'guid', 'timestamp', 'source:type', 'ip_src_addr', 'enrichments:geo:ip_dst_addr:country',
                                'ip_dst_addr', 'host', 'alert_status' ];

  beforeAll(async function() : Promise<any> {
    loginPage = new LoginPage();

    await loadTestData();
    await loginPage.login();
  });

  afterAll(async function() : Promise<any> {
    await loginPage.logout();
    await deleteTestData();
  });

  beforeEach(() => {
    page = new MetronAlertsPage();
    jasmine.addMatchers(customMatchers);
  });

  it('should have all the UI elements', async function() : Promise<any> {
    await page.navigateTo();
    await page.clearLocalStorage();

    expect(await page.getChangesAlertTableTitle('Alerts (0)')).toEqualBcoz('Alerts (169)', 'for alerts title');

    expect(await page.isMetronLogoPresent()).toEqualBcoz(true, 'for Metron Logo');
    expect(await page.isSavedSearchButtonPresent()).toEqualBcoz(true, 'for SavedSearch Button');
    expect(await page.isClearSearchPresent()).toEqualBcoz(true, 'for Clear Search');
    expect(await page.isSearchButtonPresent()).toEqualBcoz(true, 'for Search Button');
    expect(await page.isSaveSearchButtonPresent()).toEqualBcoz(true, 'for Save Search Button');
    expect(await page.isTableSettingsButtonPresent()).toEqualBcoz(true, 'for table settings button');
    expect(await page.isPausePlayRefreshButtonPresent()).toEqualBcoz(true, 'for pause/play button');
    expect(await page.isConfigureTableColumnsPresent()).toEqualBcoz(true, 'for alerts table column configure button');

    expect(await page.getActionDropdownItems()).toEqualBcoz([ 'Open', 'Dismiss', 'Escalate', 'Resolve', 'Add to Alert' ],
                                                        'for default dropdown actions');
    expect(await page.getTableColumnNames()).toEqualBcoz(columnNames, 'for default column names for alert list table');
  });

  it('should have all pagination controls and they should be working', async function() : Promise<any> {

    await page.clickSettings();
    await page.clickPageSize('100');

    expect(await page.getChangedPaginationText('1 - 25 of 169')).toEqualBcoz('1 - 100 of 169', 'for pagination text');
    expect(await page.isChevronLeftEnabled()).toEqualBcoz(false, 'for left chevron to be disabled for first page');
    expect(await page.isChevronRightEnabled()).toEqualBcoz(true, 'for right chevron to be enabled for first page');

    await page.clickChevronRight();
    expect(await page.getChangedPaginationText('1 - 100 of 169')).toEqualBcoz('101 - 169 of 169', 'for pagination text');
    expect(await page.isChevronLeftEnabled()).toEqualBcoz(true, 'for left chevron to be disabled for first page');
    expect(await page.isChevronRightEnabled()).toEqualBcoz(false, 'for right chevron to be enabled for first page');

    await page.clickChevronLeft();
    expect(await page.getChangedPaginationText('101 - 169 of 169')).toEqualBcoz('1 - 100 of 169', 'for pagination text');
    expect(await page.isChevronLeftEnabled()).toEqualBcoz(false, 'for left chevron to be disabled for first page');
    expect(await page.isChevronRightEnabled()).toEqualBcoz(true, 'for right chevron to be enabled for first page');

    await page.clickSettings();
    await page.clickPageSize('25');
    expect(await page.getChangedPaginationText('1 - 100 of 169')).toEqualBcoz('1 - 25 of 169', 'for pagination text');

    await page.clickSettings();
  });

  it('should have all settings controls and they should be working', async function() : Promise<any> {
    let settingsPaneLbelNames = [ 'REFRESH RATE', 'ROWS PER PAGE', 'HIDE Resolved Alerts', 'HIDE Dismissed Alerts' ];
    let settingPaneRefreshIntervals = [ '5s', '10s', '15s', '30s', '1m', '10m', '1h' ];
    let settingsPanePageSize = [ '10', '25', '50', '100', '250', '500', '1000' ];

    await page.clickSettings();

    expect(await page.getSettingsLabels()).toEqualBcoz(settingsPaneLbelNames, 'for table settings labels');

    expect(await page.getRefreshRateOptions()).toEqualBcoz(settingPaneRefreshIntervals, 'for table settings refresh rate labels');
    expect(await page.getRefreshRateSelectedOption()).toEqualBcoz([ '1m' ], 'for table settings default refresh rate');

    await  page.clickRefreshInterval('10s');
    expect(await page.getRefreshRateSelectedOption()).toEqualBcoz([ '10s' ], 'for refresh interval 10s');

    await page.clickRefreshInterval('1h');
    expect(await page.getRefreshRateSelectedOption()).toEqualBcoz([ '1h' ], 'for refresh interval 1h');

    expect(await page.getPageSizeOptions()).toEqualBcoz(settingsPanePageSize, 'for table settings refresh rate labels');
    expect(await page.getPageSizeSelectedOption()).toEqualBcoz([ '25' ], 'for table settings default page size');

    await page.clickPageSize('50');
    expect(await page.getPageSizeSelectedOption()).toEqualBcoz([ '50' ], 'for page size 50');

    await page.clickPageSize('100');
    expect(await page.getPageSizeSelectedOption()).toEqualBcoz([ '100' ], 'for page size 100');

    await page.clickPageSize('25');
    expect(await page.getPageSizeSelectedOption()).toEqualBcoz([ '25' ], 'for page size 100');

    await page.clickSettings();
  });

  it('play pause should start polling and stop polling ', async function() : Promise<any> {
    expect(await page.getPlayPauseState()).toEqual('fa fa-play', 'for default pause option');

    await page.clickPlayPause('fa-pause');
    expect(await page.getPlayPauseState()).toEqual('fa fa-pause', 'for default pause option');

    await page.clickPlayPause('fa-play');
    expect(await page.getPlayPauseState()).toEqual('fa fa-play', 'for default pause option');
  });

  it('should select columns from table configuration', async function() : Promise<any> {
    await page.clickConfigureTable();
    expect(await page.getSelectedColumnNames()).toEqual(colNamesColumnConfig, 'expect default selected column names');

    // remove the 'guid' column and add the 'id' column
    await page.toggleSelectCol('guid');
    await page.toggleSelectCol('id');

    let expectedColumns = [ 'score', 'timestamp', 'source:type', 'ip_src_addr', 'enrichments:geo:ip_dst_addr:country',
      'ip_dst_addr', 'host', 'alert_status', 'id' ];
    expect(await page.getSelectedColumnNames()).toEqual(expectedColumns, 'expect "id" field added and "guid" field removed from visible columns');
    await page.saveConfigureColumns();
  });

  it('should have all time-range controls', async function() : Promise<any> {
    let quickRanges = [
      'Last 7 days', 'Last 30 days', 'Last 60 days', 'Last 90 days', 'Last 6 months', 'Last 1 year', 'Last 2 years', 'Last 5 years',
      'Yesterday', 'Day before yesterday', 'This day last week', 'Previous week', 'Previous month', 'Previous year', 'All time',
      'Today', 'Today so far', 'This week', 'This week so far', 'This month', 'This year',
      'Last 5 minutes', 'Last 15 minutes', 'Last 30 minutes', 'Last 1 hour', 'Last 3 hours', 'Last 6 hours', 'Last 12 hours', 'Last 24 hours'
    ];

    await page.clickDateSettings();
    expect(await page.getTimeRangeTitles()).toEqual(['Time Range', 'Quick Ranges']);
    expect(await page.getQuickTimeRanges()).toEqual(quickRanges);
    expect(await page.getValueForManualTimeRange()).toEqual([ 'now', 'now' ]);
    expect(await page.isManulaTimeRangeApplyButtonPresent()).toEqual(true);
    expect(await page.getTimeRangeButtonText()).toEqual('All time');
    await page.hideDateSettings();

  });

  it('should have all time range values populated - 1', async function() : Promise<any> {
    let secInADay = (24 * 60 * 60 * 1000);

    await page.clickDateSettings();
    expect(await page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 7 days').catch(e => console.log(e))).toEqualBcoz(['Last 7 days', String(secInADay * 7)], 'for last 7 days');

    await page.clickDateSettings();
    expect(await page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 30 days').catch(e => console.log(e))).toEqualBcoz(['Last 30 days', String(secInADay * 30)], 'for last 30 days');

    await page.clickDateSettings();
    expect(await page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 60 days').catch(e => console.log(e))).toEqualBcoz(['Last 60 days', String(secInADay * 60)], 'for last 60 days');

    await page.clickDateSettings();
    expect(await page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 90 days').catch(e => console.log(e))).toEqualBcoz(['Last 90 days', String(secInADay * 90)], 'for last 90 days');

    await page.clickDateSettings();
    expect(await page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 1 year').catch(e => console.log(e))).toEqualBcoz(['Last 1 year', String(secInADay * 365)], 'for last 1 year');

    await page.clickDateSettings();
    expect(await page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 2 years').catch(e => console.log(e))).toEqualBcoz(['Last 2 years', String((secInADay * 365 * 2))], 'for last 2 years');

    await page.clickDateSettings();
    expect(await page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 5 years').catch(e => console.log(e))).toEqualBcoz(['Last 5 years', String((secInADay * 365 * 5) + secInADay)], 'for last 5 years');

  });

  it('should have all time range values populated - 2', async function() : Promise<any> {
    let secInADay = (24*60*60*1000);

    await page.clickDateSettings();
    expect(await page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Yesterday').catch(e => console.log(e))).toEqualBcoz([ 'Yesterday', String(secInADay - 1000)], 'yesterday');

    await page.clickDateSettings();
    expect(await page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Day before yesterday').catch(e => console.log(e))).toEqualBcoz([ 'Day before yesterday', String(secInADay - 1000)], 'day before yesterday');

    await page.clickDateSettings();
    expect(await page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('This day last week').catch(e => console.log(e))).toEqualBcoz([ 'This day last week', String(secInADay - 1000)], 'this day last week');

    await page.clickDateSettings();
    expect(await page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Previous week').catch(e => console.log(e))).toEqualBcoz([ 'Previous week', String((secInADay * 7) - (1000))], 'for previous week');

  });

  it('should have all time range values populated - 3', async function() : Promise<any> {
    let secInADay = (24*60*60*1000);

    await page.clickDateSettings();
    expect(await page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Today').catch(e => console.log(e))).toEqualBcoz([ 'Today', String(secInADay - 1000)], 'for today');

    await page.clickDateSettings();
    expect(await page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('This week').catch(e => console.log(e))).toEqualBcoz([ 'This week', String((secInADay*7) - 1000)], 'for this week');

  });

  it('should have all time range values populated - 4', async function() : Promise<any> {

    await page.clickDateSettings();
    expect(await page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 5 minutes').catch(e => console.log(e))).toEqualBcoz([ 'Last 5 minutes', String(5 * 60 * 1000)], 'for last 5 minutes');

    await page.clickDateSettings();
    expect(await page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 15 minutes').catch(e => console.log(e))).toEqualBcoz([ 'Last 15 minutes', String(15 * 60 * 1000)], 'for last 15 minutes');

    await page.clickDateSettings();
    expect(await page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 30 minutes').catch(e => console.log(e))).toEqualBcoz([ 'Last 30 minutes', String(30 * 60 * 1000)], 'for last 30 minutes');

    await page.clickDateSettings();
    expect(await page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 1 hour').catch(e => console.log(e))).toEqualBcoz([ 'Last 1 hour', String(60 * 60 * 1000)], 'for last 1 hour');

    await page.clickDateSettings();
    expect(await page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 3 hours').catch(e => console.log(e))).toEqualBcoz([ 'Last 3 hours', String(3 * 60 * 60 * 1000)], 'for last 3 hours');

    await page.clickDateSettings();
    expect(await page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 6 hours').catch(e => console.log(e))).toEqualBcoz([ 'Last 6 hours', String(6 * 60 * 60 * 1000)], 'for last 6 hours');

    await page.clickDateSettings();
    expect(await page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 12 hours').catch(e => console.log(e))).toEqualBcoz([ 'Last 12 hours', String(12 * 60 * 60 * 1000)], 'for last 12 hours');

    await page.clickDateSettings();
    expect(await page.selectQuickTimeRangeAndGetTimeRangeAndTimeText('Last 24 hours').catch(e => console.log(e))).toEqualBcoz([ 'Last 24 hours', String(24 * 60 * 60 * 1000)], 'for last 24 hours');

  });

  it('should disable date picker when timestamp is present in search', async function() : Promise<any> {
    await page.setSearchText('timestamp:1505325740512');
    expect(await page.getChangesAlertTableTitle('Alerts (169)')).toEqual('Alerts (1)');
    expect(await page.isDateSeettingDisabled()).toEqual(true);

    await page.clickClearSearch();
    expect(await page.getChangesAlertTableTitle('Alerts (1)')).toEqual('Alerts (169)');
    expect(await page.isDateSeettingDisabled()).toEqual(false);

    await expect(page.clickTableTextAndGetSearchText('FR', 'enrichments:geo:ip_dst_addr:country:FR')).toEqual('enrichments:geo:ip_dst_addr:country:FR');
    expect(await page.getChangesAlertTableTitle('Alerts (169)')).toEqual('Alerts (25)');
    expect(await page.isDateSeettingDisabled()).toEqual(false);

    await page.clickClearSearch();
    expect(await page.getChangesAlertTableTitle('Alerts (25)')).toEqual('Alerts (169)');
  });

  it('should have now included when to date is empty', async function() : Promise<any> {
    await page.clickDateSettings();
    await page.setDate(0, '2017', 'September', '13', '23', '29', '35');
    await page.selectTimeRangeApplyButton();
    expect(await page.getTimeRangeButtonTextForNow()).toEqual([ 'Date Range', '2017-09-13 23:29:35 to now' ]);

    await page.clickClearSearch();
  });

  it('should have all time-range included while searching', async function() : Promise<any> {
    let startDate = new Date(1505325575000);
    let endDate = new Date(1505325580000);

    await page.clearLocalStorage();
    await page.clickDateSettings();

    /* Select Last 5years for time range */
    expect(await page.selectQuickTimeRangeAndGetTimeRangeText('Last 5 years')).toEqual('Last 5 years');

    /* Select custom date for time range */
    await page.clickDateSettings();
    await page.setDate(0, String(startDate.getFullYear()), startDate.toLocaleString('en-us', { month: "long" }), String(startDate.getDate()),
                String(startDate.getHours()), String(startDate.getMinutes()), String(startDate.getSeconds()));
    await page.setDate(1, String(endDate.getFullYear()), endDate.toLocaleString('en-us', { month: "long" }), String(endDate.getDate()),
                String(endDate.getHours()), String(endDate.getMinutes()), String(endDate.getSeconds()));
    await page.selectTimeRangeApplyButton();
    expect(await page.getChangesAlertTableTitle('Alerts (169)')).toEqual('Alerts (5)');

    /* Save custom date in saved searches */
    await page.saveSearch('e2e-2');
    await page.clickSavedSearch();

    // The below expect statement will fail until this issue is resolved in Protractor: https://github.com/angular/protractor/issues/4693
    // This is because the connection resets before deleting the test comment, which causes the assertion to be false

    // expect(await page.getRecentSearchOptions()).toContain('timestamp:last-5-years', 'for recent search options');
    expect(await page.getSavedSearchOptions()).toEqual(['e2e-2'],
                                                    'for saved search options');
    await page.clickCloseSavedSearch();

    /* Clear Search should should show all rows */
    await page.clickClearSearch();
    expect(await page.getChangesAlertTableTitle('Alerts (5)')).toEqual('Alerts (169)');

    /* Load the saved search */
    await page.clickSavedSearch();
    await page.loadSavedSearch('e2e-2');
    expect(await page.getChangesAlertTableTitle('Alerts (169)')).toEqual('Alerts (5)');

    /* Load recent search */
    await page.clickSavedSearch();
    await page.loadRecentSearch('last-5-years');
    expect(await page.getChangesAlertTableTitle('Alerts (5)')).toEqual('Alerts (169)');

  });

});
