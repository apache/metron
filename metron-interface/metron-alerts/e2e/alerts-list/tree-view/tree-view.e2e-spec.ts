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
      'enrichm...:country': '3',
      'ip_src_addr': '2'
    };
    expect(page.getGroupByCount()).toEqualBcoz(Object.keys(groupByItems).length, '4 Group By Elements should be present');
    expect(page.getGroupByItemNames()).toEqualBcoz(Object.keys(groupByItems), 'Group By Elements names should be present');
    expect(page.getGroupByItemCounts()).toEqualBcoz(Object.keys(groupByItems).map(key => groupByItems[key]),
                                                    '4 Group By Elements values should be present');
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

    let usGroupIds = ['9a969c64-b...001cb011a3','a651f7c3-1...a97d4966c9','afc36901-3...d931231ab2','d860ac35-1...f9e282d571','04a5c3d0-9...af17c06fbc'];
    let frGroupIds = ['07b29c29-9...ff19eaa888','7cd91565-1...de5be54a6e','ca5bde58-a...f3a88d2df4','5d6faf83-8...b88a407647','e2883424-f...79bb8b0606'];

    page.selectGroup('source:type');
    page.selectGroup('ip_dst_addr');
    page.selectGroup('enrichments:geo:ip_dst_addr:country');
    expect(page.getActiveGroups()).toEqualBcoz(['source:type', 'ip_dst_addr', 'enrichments:geo:ip_dst_addr:country'], '3 groups should be selected');

    expect(page.getDashGroupValues('alerts_ui_e2e')).toEqualBcoz(['0', 'alerts_ui_e2e', 'ALERTS', '169'],
                                                              'Top Level Group Values should be present for alerts_ui_e2e');

    page.expandDashGroup('alerts_ui_e2e');
    expect(page.getSubGroupValuesByPosition('alerts_ui_e2e', '204.152.254.221', 0)).toEqualBcoz('0 204.152.254.221 (13)',
                                                                    'Second Level Group Values should be present for 204.152.254.221');

    page.expandSubGroupByPosition('alerts_ui_e2e', '204.152.254.221', 0);
    expect(page.getSubGroupValuesByPosition('alerts_ui_e2e', 'US', 0)).toEqualBcoz('0 US (13)',
        'Third Level Group Values should be present for US');

    page.expandSubGroup('alerts_ui_e2e', 'US');
    expect(page.getSubGroupValuesByPosition('alerts_ui_e2e', 'US', 0)).toEqualBcoz('0 US (13)',
        'Third Level Group Values should not change when expanded for US');
    expect(page.getCellValuesFromTable('alerts_ui_e2e', 'id', '04a5c3d0-9...af17c06fbc')).toEqual(usGroupIds, 'rows should be present for US');


    page.expandSubGroup('alerts_ui_e2e', '62.75.195.236');
    expect(page.getSubGroupValuesByPosition('alerts_ui_e2e', 'FR', 1)).toEqualBcoz('0 FR (23)',
        'Third Level Group Values should be present for FR');

    page.expandSubGroupByPosition('alerts_ui_e2e', 'FR', 1);
    expect(page.getSubGroupValuesByPosition('alerts_ui_e2e', 'FR', 1)).toEqualBcoz('0 FR (23)',
        'Third Level Group Values should not change when expanded for FR');
    expect(page.getCellValuesFromTable('alerts_ui_e2e', 'id', 'e2883424-f...79bb8b0606')).toEqual(usGroupIds.concat(frGroupIds), 'rows should be present for FR');
    
    page.unGroup();
    expect(page.getActiveGroups()).toEqualBcoz([], 'no groups should be selected');
  });


  it('should have sort working for group details for multiple sub groups', () => {

    let usTSCol = ['2017-09-13 17:59:32', '2017-09-13 17:59:42', '2017-09-13 17:59:53', '2017-09-13 18:00:02', '2017-09-13 18:00:14'];
    let ruTSCol = ['2017-09-13 17:59:33', '2017-09-13 17:59:48', '2017-09-13 17:59:51', '2017-09-13 17:59:54', '2017-09-13 17:59:57'];
    let frTSCol = ['2017-09-13 17:59:37', '2017-09-13 17:59:46', '2017-09-13 18:00:31', '2017-09-13 18:00:33', '2017-09-13 18:00:37'];

    let usSortedTSCol = ['2017-09-13 18:02:19', '2017-09-13 18:02:16', '2017-09-13 18:02:09', '2017-09-13 18:01:58', '2017-09-13 18:01:52'];
    let ruSortedTSCol = ['2017-09-14 06:29:40', '2017-09-14 06:29:40', '2017-09-14 06:29:40', '2017-09-14 06:29:40', '2017-09-13 18:02:13'];
    let frSortedTSCol = ['2017-09-14 06:29:40', '2017-09-14 04:29:40', '2017-09-13 18:02:20', '2017-09-13 18:02:05', '2017-09-13 18:02:04'];

    page.selectGroup('source:type');
    page.selectGroup('enrichments:geo:ip_dst_addr:country');

    page.expandDashGroup('alerts_ui_e2e');
    page.expandSubGroup('alerts_ui_e2e', 'US');
    page.expandSubGroup('alerts_ui_e2e', 'RU');
    page.expandSubGroup('alerts_ui_e2e', 'FR');

    let unsortedTS = [...usTSCol, ...ruTSCol, ...frTSCol];
    let sortedTS = [...usSortedTSCol, ...ruSortedTSCol, ...frSortedTSCol];

    page.sortSubGroup('alerts_ui_e2e', 'timestamp');

    expect(page.getCellValuesFromTable('alerts_ui_e2e', 'timestamp', '2017-09-13 18:00:37')).toEqual(unsortedTS,
                                                                                                      'timestamp should be sorted asc');

    page.sortSubGroup('alerts_ui_e2e', 'timestamp');

    expect(page.getCellValuesFromTable('alerts_ui_e2e', 'timestamp', '2017-09-13 18:02:04')).toEqual(sortedTS,
                                                                                                      'timestamp should be sorted dsc');

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
