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
  let page: MetronAlertsPage;
  let loginPage: LoginPage;

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

  it('should display all the default values for saved searches', () => {
    page.clearLocalStorage();
    page.navigateTo();

    page.clickSavedSearch();
    expect(page.getSavedSearchTitle()).toEqualBcoz('Searches', 'for saved searches title');
    expect(page.getRecentSearchOptions()).toEqual({ 'Recent Searches': [ 'No Recent Searches yet' ] }, 'for recent search options');
    expect(page.getSavedSearchOptions()).toEqual({ 'Saved Searches': [ 'No Saved Searches yet' ] }, 'for saved search options');
    page.clickCloseSavedSearch();

  });

  it('should have all save search controls and they save search should be working', () => {
    page.saveSearch('e2e-1');
    page.clickSavedSearch();
    expect(page.getSavedSearchOptions()).toEqual({ 'Saved Searches': [ 'e2e-1' ] }, 'for saved search options e2e-1');
    page.clickCloseSavedSearch();
  });

  it('should populate search items when selected on table', () => {
    page.clickTableText('US');
    expect(page.getSearchText()).toEqual('enrichments:geo:ip_dst_addr:country:US', 'for search text ip_dst_addr_country US');
    page.clickClearSearch();
    expect(page.getSearchText()).toEqual('*', 'for clear search');
  });

  it('should delete search items from search box', () => {
    page.clickTableText('US');
    expect(page.getSearchText()).toEqual('enrichments:geo:ip_dst_addr:country:US', 'for search text ip_dst_addr_country US');
    page.clickRemoveSearchChip();
    expect(page.getSearchText()).toEqual('*', 'for search chip remove');
  });

  it('should delete first search items from search box having multiple search fields', () => {
    page.clickTableText('US');
    page.clickTableText('alerts_ui_e2e');
    expect(page.getSearchText()).toEqual('enrichments:geo:ip_dst_addr:country:US AND source:type:alerts_ui_e2e', 'for search text US and alerts_ui_e2e');
    page.clickRemoveSearchChip();
    expect(page.getSearchText()).toEqual('source:type:alerts_ui_e2e', 'for search text alerts_ui_e2e after US is removed');
    page.clickRemoveSearchChip();
    expect(page.getSearchText()).toEqual('*', 'for search chip remove for two search texts');
  });

  it('manually entering search queries to search box and pressing enter key should search', () => {
    page.setSearchText('enrichments:geo:ip_dst_addr:country:US');
    expect(page.getPaginationText()).toEqualBcoz('1 - 22 of 22',
                                                'for pagination text with search text enrichments:geo:ip_dst_addr:country:US');
    page.setSearchText('enrichments:geo:ip_dst_addr:country:RU');
    expect(page.getPaginationText()).toEqualBcoz('1 - 25 of 44',
                                                  'for pagination text with search text enrichments:geo:ip_dst_addr:country:RU as text');
  });

});
