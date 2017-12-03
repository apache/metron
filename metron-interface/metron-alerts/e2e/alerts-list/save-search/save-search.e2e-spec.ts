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

describe('Test spec for search and save search', function() {
  let page: MetronAlertsPage;
  let loginPage: LoginPage;

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

  it('should display all the default values for saved searches', () => {
    page.clearLocalStorage();
    page.navigateTo();

    page.clickSavedSearch();
    expect(page.getSavedSearchTitle()).toEqualBcoz('Searches', 'for saved searches title');
    expect(page.getRecentSearchOptions()).toEqualBcoz([], 'for recent search options');
    expect(page.getSavedSearchOptions()).toEqualBcoz([], 'for saved search options');
    expect(page.getDefaultRecentSearchValue()).toEqualBcoz([ 'No Recent Searches' ], 'for recent search default value');
    expect(page.getDefaultSavedSearchValue()).toEqualBcoz([ 'No Saved Searches' ], 'for saved search default value');
    page.clickCloseSavedSearch();

  });

  it('should have all save search controls and they save search should be working', async function() : Promise<any> {
    page.saveSearch('e2e-1');
    await page.clickSavedSearch();
    expect(page.getSavedSearchOptions()).toEqualBcoz([ 'e2e-1' ], 'for saved search options e2e-1');
    page.clickCloseSavedSearch();
  });

  it('should delete search items from search box', () => {
    page.clickClearSearch();
    expect(page.getSearchText()).toEqual('*');
    expect(page.getChangesAlertTableTitle('')).toEqual('Alerts (169)');

    expect(page.clickTableTextAndGetSearchText('FR', 'enrichments:geo:ip_dst_addr:country:FR')).toEqualBcoz('enrichments:geo:ip_dst_addr:country:FR', 'for search text ip_dst_addr_country FR');
    expect(page.clickRemoveSearchChipAndGetSearchText('*')).toEqualBcoz('*', 'for search chip remove');
  });

  it('should delete first search items from search box having multiple search fields', () => {
    page.clickClearSearch();
    expect(page.getSearchText()).toEqual('*');
    expect(page.getChangesAlertTableTitle('')).toEqual('Alerts (169)');

    expect(page.clickTableTextAndGetSearchText('FR', 'enrichments:geo:ip_dst_addr:country:FR')).toEqual('enrichments:geo:ip_dst_addr:country:FR');
    expect(page.clickTableTextAndGetSearchText('alerts_ui_e2e', 'enrichments:geo:ip_dst_addr:country:FR AND source:type:alerts_ui_e2e')).toEqual('enrichments:geo:ip_dst_addr:country:FR AND source:type:alerts_ui_e2e');

    expect(page.clickRemoveSearchChipAndGetSearchText('source:type:alerts_ui_e2e')).toEqual('source:type:alerts_ui_e2e');
    expect(page.clickRemoveSearchChipAndGetSearchText('*')).toEqual('*');
  });

  it('manually entering search queries to search box and pressing enter key should search', () => {
    page.setSearchText('enrichments:geo:ip_dst_addr:country:US');
    expect(page.getChangesAlertTableTitle('Alerts (169)')).toEqual('Alerts (22)');
    expect(page.getPaginationText()).toEqualBcoz('1 - 22 of 22',
                                                'for pagination text with search text enrichments:geo:ip_dst_addr:country:US');

    page.setSearchText('enrichments:geo:ip_dst_addr:country:RU');
    expect(page.getChangesAlertTableTitle('Alerts (169)')).toEqual('Alerts (44)');
    expect(page.getPaginationText()).toEqualBcoz('1 - 25 of 44',
                                                  'for pagination text with search text enrichments:geo:ip_dst_addr:country:RU as text');
  });

});
