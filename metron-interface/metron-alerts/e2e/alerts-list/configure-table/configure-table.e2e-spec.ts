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
import { MetronAlertsPage } from '../alerts-list.po';
import {customMatchers} from '../../matchers/custom-matchers';
import {LoginPage} from '../../login/login.po';
import {loadTestData, deleteTestData} from '../../utils/e2e_util';

describe('Test spec for table column configuration', function() {
  let page: MetronAlertsPage;
  let loginPage: LoginPage;
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

  it('should select columns from table configuration', async function() : Promise<any> {
    await page.clearLocalStorage();
    await page.navigateTo();
    await page.clickConfigureTable();
    expect(await page.getSelectedColumnNames()).toEqualBcoz(colNamesColumnConfig, 'for default selected column names');

    // remove the 'guid' column and add the 'id' column
    await page.toggleSelectCol('guid');
    await page.toggleSelectCol('id');

    let expectedColumns = [ 'score', 'timestamp', 'source:type', 'ip_src_addr', 'enrichments:geo:ip_dst_addr:country',
      'ip_dst_addr', 'host', 'alert_status', 'id' ];
    expect(await page.getSelectedColumnNames()).toEqualBcoz(expectedColumns, 'expect "id" field added and "guid" field removed from visible columns');
    await page.saveConfigureColumns();
  });

  it('should rename columns from table configuration', async function() : Promise<any> {
    await page.clearLocalStorage();
    await page.navigateTo();

    await page.clickConfigureTable();
    await page.renameColumn('enrichments:geo:ip_dst_addr:country', 'Country');
    await page.saveConfigureColumns();

    expect(await page.clickTableTextAndGetSearchText('FR', 'Country:FR')).toEqual('Country:FR');
    expect(await page.getChangesAlertTableTitle('Alerts (169)')).toEqual('Alerts (25)');
    await page.clickClearSearch();

    expect(await page.getChangesAlertTableTitle('Alerts (25)')).toEqual('Alerts (169)');
    await page.setSearchText('Country:FR');
    expect(await page.getChangesAlertTableTitle('Alerts (169)')).toEqual('Alerts (25)');
    await page.clickClearSearch();

    expect(await page.getTableColumnNames()).toContain('Country', 'for renamed column names for alert list table');

  });

});
