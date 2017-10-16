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
import {customMatchers} from  '../../matchers/custom-matchers';
import {LoginPage} from '../../login/login.po';
import {TreeViewPage} from './tree-view.po';
import {loadTestData, deleteTestData} from '../../utils/e2e_util';
import {MetronAlertsPage} from '../alerts-list.po';

describe('metron-alerts tree view', function () {
  let page: TreeViewPage;
  let listPage: MetronAlertsPage;
  let loginPage: LoginPage;

  beforeAll(() => {
    loadTestData();
    loginPage = new LoginPage();
    page = new TreeViewPage();
    listPage = new MetronAlertsPage();
    loginPage.login();
    page.navigateToAlertsList();
  });

  afterAll(() => {
    loginPage.logout();
    deleteTestData();
  });

  beforeEach(() => {
    jasmine.addMatchers(customMatchers);
  });

  it('should have all group by elements', () => {
    let groupByItems = {
      'source:type': '1',
      'ip_dst_addr': '8',
      'host': '9',
      'enrichm...:country': '3',
      'ip_src_addr': '2'
    };
    expect(page.getGroupByCount()).toEqualBcoz(Object.keys(groupByItems).length, '5 Group By Elements should be present');
    expect(page.getGroupByItemNames()).toEqualBcoz(Object.keys(groupByItems), 'Group By Elements names should be present');
    expect(page.getGroupByItemCounts()).toEqualBcoz(Object.keys(groupByItems).map(key => groupByItems[key]),
                                                    '5 Group By Elements values should be present');
  });

  it('drag and drop should change group order', () => {
    let before = {
      'firstDashRow': ['0', 'alerts_ui_e2e', 'ALERTS', '169'],
      'firstSubGroup': '0 US (22)',
      'secondSubGroup': '0 RU (44)',
      'thirdSubGroup': '0 FR (25)'
    };

    let after = {
      'firstDashRow': ['0', 'US', 'ALERTS', '22'],
      'secondDashRow': ['0', 'RU', 'ALERTS', '44'],
      'thirdDashRow': ['0', 'FR', 'ALERTS', '25'],
      'firstDashSubGroup': '0 alerts_ui_e2e (22)',
      'secondDashSubGroup': '0 alerts_ui_e2e (44)',
      'thirdDashSubGroup': '0 alerts_ui_e2e (25)'
    };

    page.selectGroup('source:type');
    page.selectGroup('enrichments:geo:ip_dst_addr:country');
    page.expandDashGroup('alerts_ui_e2e');
    expect(page.getDashGroupValues('alerts_ui_e2e')).toEqualBcoz(before.firstDashRow, 'First Dash Row should be correct');
    expect(page.getSubGroupValues('alerts_ui_e2e', 'US')).toEqualBcoz(before.firstSubGroup,
        'Dash Group Values should be correct for US');
    expect(page.getSubGroupValues('alerts_ui_e2e', 'RU')).toEqualBcoz(before.secondSubGroup,
        'Dash Group Values should be present for RU');
    expect(page.getSubGroupValues('alerts_ui_e2e', 'FR')).toEqualBcoz(before.thirdSubGroup,
        'Dash Group Values should be present for FR');

    page.dragGroup('source:type', 'ip_src_addr');
    //page.selectGroup('source:type');
    expect(page.getDashGroupValues('US')).toEqualBcoz(after.firstDashRow, 'First Dash Row after ' +
        'reorder should be correct');
    expect(page.getDashGroupValues('RU')).toEqualBcoz(after.secondDashRow, 'Second Dash Row after ' +
        'reorder should be correct');
    expect(page.getDashGroupValues('FR')).toEqualBcoz(after.thirdDashRow, 'Third Dash Row after ' +
        'reorder should be correct');

    page.expandDashGroup('US');
    expect(page.getSubGroupValues('US', 'alerts_ui_e2e')).toEqualBcoz(after.firstDashSubGroup,
        'First Dash Group Values should be present for alerts_ui_e2e');

    page.expandDashGroup('RU');
    expect(page.getSubGroupValues('RU', 'alerts_ui_e2e')).toEqualBcoz(after.secondDashSubGroup,
        'Second Dash Group Values should be present for alerts_ui_e2e');

    page.expandDashGroup('FR');
    expect(page.getSubGroupValues('FR', 'alerts_ui_e2e')).toEqualBcoz(after.thirdDashSubGroup,
        'Third Dash Group Values should be present for alerts_ui_e2e');

    page.dragGroup('source:type', 'ip_dst_addr');
    page.unGroup();
  });

  it('should have group details for single group by', () => {
    let dashRowValues = ['0', 'alerts_ui_e2e', 'ALERTS', '169'];
    let row1_page1 = ['-', 'dcda4423-7...0962fafc47', '2017-09-13 17:59:32', 'alerts_ui_e2e',
      '192.168.138.158', 'US', '72.34.49.86', 'comarksecurity.com', 'NEW', '', ''];
    let row1_page2 = ['-', '07b29c29-9...ff19eaa888', '2017-09-13 17:59:37', 'alerts_ui_e2e',
      '192.168.138.158', 'FR', '62.75.195.236', '62.75.195.236', 'NEW', '', ''];

    page.selectGroup('source:type');
    expect(page.getActiveGroups()).toEqualBcoz(['source:type'], 'only source type group should be selected');
    expect(page.getDashGroupValues('alerts_ui_e2e')).toEqualBcoz(dashRowValues, 'Dash Group Values should be present');

    page.expandDashGroup('alerts_ui_e2e');
    expect(page.getDashGroupTableValuesForRow('alerts_ui_e2e', 0)).toEqualBcoz(row1_page1, 'Dash Group Values should be present');

    page.clickOnNextPage('alerts_ui_e2e');
    expect(page.getTableValuesByRowId('alerts_ui_e2e', 0, 'FR')).toEqualBcoz(row1_page2, 'Dash Group Values should be present');

    page.unGroup();
    expect(page.getActiveGroups()).toEqualBcoz([], 'no groups should be selected');
  });

  it('should have group details for multiple group by', () => {

    let dashRow_runLoveUs = {
      'dashRow': ['0', 'runlove.us', 'ALERTS', '13'],
      'firstSubGroup': '0 US (13)',
      'firstSubGroupIdCol': ['9a969c64-b...001cb011a3', 'a651f7c3-1...a97d4966c9', 'afc36901-3...d931231ab2',
        'd860ac35-1...f9e282d571', '04a5c3d0-9...af17c06fbc']
    };

    let dashRow_62_75_195_236 = {
      'dashRow': ['0', '62.75.195.236', 'ALERTS', '18'],
      'firstSubGroup': '0 FR (18)',
      'firstSubGroupIdCol': ['07b29c29-9...ff19eaa888', '7cd91565-1...de5be54a6e', 'ca5bde58-a...f3a88d2df4',
        '5d6faf83-8...b88a407647', 'e2883424-f...79bb8b0606']
    };

    page.selectGroup('host');
    page.selectGroup('enrichments:geo:ip_dst_addr:country');
    expect(page.getActiveGroups()).toEqualBcoz(['host', 'enrichments:geo:ip_dst_addr:country'], 'two groups should be selected');

    expect(page.getDashGroupValues('runlove.us')).toEqualBcoz(dashRow_runLoveUs.dashRow,
                                                              'Dash Group Values should be present for runlove.us');
    page.expandDashGroup('runlove.us');
    expect(page.getSubGroupValues('runlove.us', 'US')).toEqualBcoz(dashRow_runLoveUs.firstSubGroup,
                                                                    'Dash Group Values should be present for runlove.us');
    page.expandSubGroup('runlove.us', 'US');
    expect(page.getCellValuesFromTable('runlove.us', 'id', '04a5c3d0-9...af17c06fbc')).toEqual(dashRow_runLoveUs.firstSubGroupIdCol,
                                                                                                'id should not be sorted');

    expect(page.getDashGroupValues('62.75.195.236')).toEqualBcoz(dashRow_62_75_195_236.dashRow, 'Dash Group Values should be present');
    page.expandDashGroup('62.75.195.236');
    expect(page.getSubGroupValues('62.75.195.236', 'FR')).toEqualBcoz(dashRow_62_75_195_236.firstSubGroup,
                                                                      'Dash Group Values should be present for 62.75.195.236');
    page.expandSubGroup('62.75.195.236', 'FR');
    expect(page.getCellValuesFromTable('62.75.195.236', 'id', 'e2883424-f...79bb8b0606')).toEqual(dashRow_62_75_195_236.firstSubGroupIdCol,
                                                                                                  'id should not be sorted');

    page.unGroup();
    expect(page.getActiveGroups()).toEqualBcoz([], 'no groups should be selected');
  });


  it('should have sort working for group details for multiple sub groups', () => {

    let usIDCol = ['dcda4423-7...0962fafc47', '9a969c64-b...001cb011a3', 'a651f7c3-1...a97d4966c9',
                    'afc36901-3...d931231ab2', 'd860ac35-1...f9e282d571'];
    let ruIDCol = ['350c0e9f-a...3cbe5b29d2', '9b47e24a-e...2ca6627943', '4cac5e2c-3...3deb1ebcc6',
                    'eb54c3fa-c...e02719c3b0', 'cace11d0-c...b1bd7b9499'];
    let frIDCol = ['07b29c29-9...ff19eaa888', '7cd91565-1...de5be54a6e', 'ca5bde58-a...f3a88d2df4',
                    '5d6faf83-8...b88a407647', 'e2883424-f...79bb8b0606'];

    let usSortedIDCol = ['04a5c3d0-9...af17c06fbc', '06e70f55-4...f486927126', '105529cb-2...61b58237cc',
                          '4c732cb0-0...6a93129aba', '500eb5e2-6...37b0f98772'];
    let ruSortedIDCol = ['001b5451-6...38ec4221ee', '00814048-d...c9e6f27800', '0454b31e-e...0a711a36e7',
                          '09552ace-9...e146579030', '0e99ba49-4...456c107bc9'];
    let frSortedIDCol = ['07b29c29-9...ff19eaa888', '2681ed49-b...c33a80d429', '29ffaeb4-e...36822e5f81',
                          '2cc174d7-c...8073777309', '436b9ecf-b...5f1ece4c4d'];

    page.selectGroup('source:type');
    page.selectGroup('enrichments:geo:ip_dst_addr:country');

    page.expandDashGroup('alerts_ui_e2e');
    page.expandSubGroup('alerts_ui_e2e', 'US');
    page.expandSubGroup('alerts_ui_e2e', 'RU');
    page.expandSubGroup('alerts_ui_e2e', 'FR');

    let unsortedIds = [...usIDCol, ...ruIDCol, ...frIDCol];
    let sortedIds = [...usSortedIDCol, ...ruSortedIDCol, ...frSortedIDCol];

    expect(page.getCellValuesFromTable('alerts_ui_e2e', 'id', 'e2883424-f...79bb8b0606')).toEqual(unsortedIds, 'id should not be sorted');

    page.sortSubGroup('alerts_ui_e2e', 'id');

    expect(page.getCellValuesFromTable('alerts_ui_e2e', 'id', '436b9ecf-b...5f1ece4c4d')).toEqual(sortedIds, 'id should be sorted');

    page.unGroup();
    expect(page.getActiveGroups()).toEqualBcoz([], 'no groups should be selected');
  });

  it('should have search working for group details for multiple sub groups', () => {

    page.selectGroup('source:type');
    page.selectGroup('enrichments:geo:ip_dst_addr:country');

    page.expandDashGroup('alerts_ui_e2e');
    expect(page.getNumOfSubGroups('alerts_ui_e2e')).toEqual(3, 'three sub groups should be present');

    listPage.setSearchText('enrichments:geo:ip_dst_addr:country:FR');

    expect(page.getNumOfSubGroups('alerts_ui_e2e')).toEqual(1, 'one sub groups should be present');
    page.expandSubGroup('alerts_ui_e2e', 'FR');

    let expected = ['FR', 'FR', 'FR', 'FR', 'FR'];
    expect(page.getCellValuesFromTable('alerts_ui_e2e', 'enrichments:geo:ip_dst_addr:country', 'FR')).toEqual(expected,
                                                                                                              'id should be sorted');

    page.unGroup();
    expect(page.getActiveGroups()).toEqualBcoz([], 'no groups should be selected');
  });

});
