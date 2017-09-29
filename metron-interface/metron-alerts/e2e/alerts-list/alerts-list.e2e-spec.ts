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
import { loadTestData, deleteTestData } from "../utils/e2e_util";

describe('metron-alerts App', function() {
  let page: MetronAlertsPage;
  let loginPage: LoginPage;
  let columnNames = [ 'Score', 'id', 'timestamp', 'source:type', 'ip_src_addr', 'enrichm...:country',
                      'ip_dst_addr', 'host', 'alert_status', ''];
  let colNamesColumnConfig = [ 'score', 'id', 'timestamp', 'source:type', 'ip_src_addr', 'enrichments:geo:ip_dst_addr:country',
                                'ip_dst_addr', 'host', 'alert_status' ];

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

  it('should have all the UI elements', () => {
    page.navigateTo();
    page.clearLocalStorage();

    expect(page.isMetronLogoPresent()).toEqualBcoz(true, 'for Metron Logo');
    expect(page.isSavedSearchButtonPresent()).toEqualBcoz(true, 'for SavedSearch Button');
    expect(page.isClearSearchPresent()).toEqualBcoz(true, 'for Clear Search');
    expect(page.isSearchButtonPresent()).toEqualBcoz(true, 'for Search Button');
    expect(page.isSaveSearchButtonPresent()).toEqualBcoz(true, 'for Save Search Button');
    expect(page.isTableSettingsButtonPresent()).toEqualBcoz(true, 'for table settings button');
    expect(page.isPausePlayRefreshButtonPresent()).toEqualBcoz(true, 'for pause/play button');
    expect(page.isConfigureTableColumnsPresent()).toEqualBcoz(true, 'for alerts table column configure button');

    expect(page.getAlertTableTitle()).toEqualBcoz('Alerts (169)', 'for alerts title');
    expect(page.getActionDropdownItems()).toEqualBcoz([ 'Open', 'Dismiss', 'Escalate', 'Resolve' ], 'for default dropdown actions');
    expect(page.getTableColumnNames()).toEqualBcoz(columnNames, 'for default column names for alert list table');
  });

  it('should have all pagination controls and they should be working', () => {
    expect(page.isChevronLeftEnabled()).toEqualBcoz(false, 'for left chevron to be disabled for first page');
    expect(page.getPaginationText()).toEqualBcoz('1 - 25 of 169', 'for pagination text');
    expect(page.isChevronRightEnabled()).toEqualBcoz(true, 'for right chevron to be enabled for first page');

    page.clickChevronRight();

    expect(page.isChevronLeftEnabled()).toEqualBcoz(true, 'for left chevron to be enabled for second page');
    expect(page.getPaginationText()).toEqualBcoz('26 - 50 of 169', 'for pagination text');
    expect(page.isChevronRightEnabled()).toEqualBcoz(true, 'for right chevron to be enabled for second page');

    page.clickChevronRight();

    expect(page.isChevronLeftEnabled()).toEqualBcoz(true, 'for left chevron to be enabled for third page');
    expect(page.getPaginationText()).toEqualBcoz('51 - 75 of 169', 'for pagination text');
    expect(page.isChevronRightEnabled()).toEqualBcoz(true, 'for right chevron to be enabled for third page');

    page.clickChevronRight(4);

    expect(page.isChevronLeftEnabled()).toEqualBcoz(true, 'for left chevron to be enabled for last page');
    expect(page.getPaginationText()).toEqualBcoz('151 - 169 of 169', 'for pagination text');
    expect(page.isChevronRightEnabled()).toEqualBcoz(false, 'for right chevron to be disabled for last page');

    page.clickChevronLeft(7);

    expect(page.isChevronLeftEnabled()).toEqualBcoz(false, 'for left chevron to be disabled for first page again');
    expect(page.getPaginationText()).toEqualBcoz('1 - 25 of 169', 'for pagination text');
    expect(page.isChevronRightEnabled()).toEqualBcoz(true, 'for right chevron to be enabled for first page again');

  });

  it('should have all settings controls and they should be working', () => {
    let settingsPaneLbelNames = [ 'REFRESH RATE', 'ROWS PER PAGE', 'HIDE Resolved Alerts', 'HIDE Dismissed Alerts' ];
    let settingPaneRefreshIntervals = [ '5s', '10s', '15s', '30s', '1m', '10m', '1h' ];
    let settingsPanePageSize = [ '10', '25', '50', '100', '250', '500', '1000' ];

    page.clickSettings();

    expect(page.getSettingsLabels()).toEqualBcoz(settingsPaneLbelNames, 'for table settings labels');

    expect(page.getRefreshRateOptions()).toEqualBcoz(settingPaneRefreshIntervals, 'for table settings refresh rate labels');
    expect(page.getRefreshRateSelectedOption()).toEqualBcoz([ '1m' ], 'for table settings default refresh rate');

    page.clickRefreshInterval('10s');
    expect(page.getRefreshRateSelectedOption()).toEqualBcoz([ '10s' ], 'for refresh interval 10s');

    page.clickRefreshInterval('1h');
    expect(page.getRefreshRateSelectedOption()).toEqualBcoz([ '1h' ], 'for refresh interval 1h');

    expect(page.getPageSizeOptions()).toEqualBcoz(settingsPanePageSize, 'for table settings refresh rate labels');
    expect(page.getPageSizeSelectedOption()).toEqualBcoz([ '25' ], 'for table settings default page size');

    page.clickPageSize('10');
    expect(page.getPageSizeSelectedOption()).toEqualBcoz([ '10' ], 'for page size 10');

    page.clickPageSize('100');
    expect(page.getPageSizeSelectedOption()).toEqualBcoz([ '100' ], 'for page size 100');

    page.clickSettings();
  });

  it('play pause should start polling and stop polling ', () => {
    expect(page.getPlayPauseState()).toEqual('fa fa-pause', 'for default pause option');

    page.clickPlayPause();
    expect(page.getPlayPauseState()).toEqual('fa fa-play', 'for default pause option');

    page.clickPlayPause();
    expect(page.getPlayPauseState()).toEqual('fa fa-pause', 'for default pause option');
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

});
