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
import {MetronAlertsPage} from '../alerts-list.po';

import {loadTestData, deleteTestData} from '../../utils/e2e_util';

describe('Test spec for tree view', function () {
  let page: TreeViewPage;
  let listPage: MetronAlertsPage;
  let loginPage: LoginPage;

  beforeAll(async function() : Promise<any> {
    loginPage = new LoginPage();
    page = new TreeViewPage();
    listPage = new MetronAlertsPage();

    await loadTestData();
    await loginPage.login();
    jasmine.addMatchers(customMatchers);
  });


  afterAll(async function() : Promise<any> {
    await loginPage.logout();
    await deleteTestData();
  });

  beforeEach(() => {

  });

  it('should have all group by elements', async function() : Promise<any> {
    let groupByItems = {
      'source:type': '1',
      'ip_dst_addr': '8',
      'enrichm...:country': '3',
      'ip_src_addr': '6'
    };

    expect(await listPage.getChangesAlertTableTitle('Alerts (0)')).toEqualBcoz('Alerts (169)', 'for alerts title');

    expect(await page.getGroupByCount()).toEqualBcoz(Object.keys(groupByItems).length, '4 Group By Elements should be present');
    expect(await page.getGroupByItemNames()).toEqualBcoz(Object.keys(groupByItems), 'Group By Elements names should be present');
    expect(await page.getGroupByItemCounts()).toEqualBcoz(Object.keys(groupByItems).map(key => groupByItems[key]),
                                                    '4 Group By Elements values should be present');
  });

//   HTML5 Drag and Drop with Selenium Webdriver issue effects this test:
//   https://github.com/SeleniumHQ/selenium-google-code-issue-archive/issues/3604

  xit('drag and drop should change group order',  async function() : Promise<any> {
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

    await page.selectGroup('source:type');
    await page.selectGroup('enrichments:geo:ip_dst_addr:country');
    expect(await page.getDashGroupValues('alerts_ui_e2e')).toEqualBcoz(before.firstDashRow, 'First Dash Row should be correct');

    await page.expandDashGroup('alerts_ui_e2e');
    expect(await page.getSubGroupValues('alerts_ui_e2e', 'US')).toEqualBcoz(before.firstSubGroup,
        'Dash Group Values should be correct for US');
    expect(await page.getSubGroupValues('alerts_ui_e2e', 'RU')).toEqualBcoz(before.secondSubGroup,
        'Dash Group Values should be present for RU');
    expect(await page.getSubGroupValues('alerts_ui_e2e', 'FR')).toEqualBcoz(before.thirdSubGroup,
        'Dash Group Values should be present for FR');

    await page.simulateDragAndDrop('source:type', 'ip_src_addr');

    expect(await page.getDashGroupValues('US')).toEqualBcoz(after.firstDashRow, 'First Dash Row after ' +
        'reorder should be correct');
    expect(await page.getDashGroupValues('RU')).toEqualBcoz(after.secondDashRow, 'Second Dash Row after ' +
        'reorder should be correct');
    expect(await page.getDashGroupValues('FR')).toEqualBcoz(after.thirdDashRow, 'Third Dash Row after ' +
        'reorder should be correct');

    await page.expandDashGroup('US');
    expect(await page.getSubGroupValues('US', 'alerts_ui_e2e')).toEqualBcoz(after.firstDashSubGroup,
        'First Dash Group Values should be present for alerts_ui_e2e');

    await page.expandDashGroup('RU');
    expect(await page.getSubGroupValues('RU', 'alerts_ui_e2e')).toEqualBcoz(after.secondDashSubGroup,
        'Second Dash Group Values should be present for alerts_ui_e2e');

    await page.expandDashGroup('FR');
    expect(await page.getSubGroupValues('FR', 'alerts_ui_e2e')).toEqualBcoz(after.thirdDashSubGroup,
        'Third Dash Group Values should be present for alerts_ui_e2e');

    await page.simulateDragAndDrop('source:type', 'ip_dst_addr');
    await page.unGroup();

  });

  // Test cannot pass until issue with missing dash score is resolved: https://issues.apache.org/jira/browse/METRON-1631
  xit('should have group details for single group by', async function() : Promise<any> {
    let dashRowValues = ['0', 'alerts_ui_e2e', 'ALERTS', '169'];
    let row1_page1 = ['-','acf5a641-9...a316e14fbe', '2017-09-13 17:59:35', 'alerts_ui_e2e',
                        '192.168.66.1', '', '192.168.66.121', 'node1', 'NEW'];
    let row1_page2 = ['-', '3097a3d9-f...1cfb870355', '2017-09-13 18:00:22', 'alerts_ui_e2e',
                        '192.168.66.1', '','192.168.66.121', 'node1', 'NEW'];

    await page.unGroup();
    await page.selectGroup('source:type');
    expect(await page.getActiveGroups()).toEqualBcoz(['source:type'], 'only source type group should be selected');
    expect(await page.getDashGroupValues('alerts_ui_e2e')).toEqualBcoz(dashRowValues, 'Dash Group Values should be present');

    await page.expandDashGroup('alerts_ui_e2e');
    expect(await page.getTableValuesByRowId('alerts_ui_e2e', 0, 'acf5a641-9...a316e14fbe')).toEqualBcoz(row1_page1, 'Dash Group Values should be present');

    await page.clickOnNextPage('alerts_ui_e2e');
    expect(await page.getTableValuesByRowId('alerts_ui_e2e', 0, '3097a3d9-f...1cfb870355')).toEqualBcoz(row1_page2, 'Dash Group Values should be present');

    await page.unGroup();
    expect(await page.getActiveGroups()).toEqualBcoz([], 'no groups should be selected');
  });

  it('should have group details for multiple group by', async function() : Promise<any> {

    let usGroupIds = ['a651f7c3-1...a97d4966c9', '5cfff1c7-6...ef3d766fc7', '7022e863-5...3c1fb629ed', '5404950f-9...86ce704b22', '8eb077ae-3...b77fed1ab4'];
    let frGroupIds = ['07b29c29-9...ff19eaa888', 'c27f0bd2-3...697eaf8692', 'ba44eb73-6...6f9c15b261', '6a437817-e...dd0b37d280', '48fc3a55-4...3479974d34'];

    await page.unGroup();
    await page.selectGroup('source:type');
    await page.selectGroup('ip_dst_addr');
    await page.selectGroup('enrichments:geo:ip_dst_addr:country');
    expect(await page.getActiveGroups()).toEqualBcoz(['source:type', 'ip_dst_addr', 'enrichm...:country'], '3 groups should be selected');

    expect(await page.getDashGroupValues('alerts_ui_e2e')).toEqualBcoz(['36', 'alerts_ui_e2e', 'ALERTS', '169'],
                                                              'Top Level Group Values should be present for alerts_ui_e2e');

    await page.expandDashGroup('alerts_ui_e2e');
    expect(await page.getSubGroupValuesByPosition('alerts_ui_e2e', '204.152.254.221', 0)).toEqualBcoz('0 204.152.254.221 (13)',
                                                                    'Second Level Group Values should be present for 204.152.254.221');

    await page.expandSubGroupByPosition('alerts_ui_e2e', '204.152.254.221', 0);
    expect(await page.getSubGroupValuesByPosition('alerts_ui_e2e', 'US', 0)).toEqualBcoz('0 US (13)',
        'Third Level Group Values should be present for US');

    await page.expandSubGroup('alerts_ui_e2e', 'US');
    expect(await page.getSubGroupValuesByPosition('alerts_ui_e2e', 'US', 0)).toEqualBcoz('0 US (13)',
        'Third Level Group Values should not change when expanded for US');
    expect(await page.getCellValuesFromTable('alerts_ui_e2e', 'id', 'a651f7c3-1...a97d4966c9')).toEqual(usGroupIds, 'rows should be present for US');


    await page.expandSubGroup('alerts_ui_e2e', '62.75.195.236');
    expect(await page.getSubGroupValuesByPosition('alerts_ui_e2e', 'FR', 1)).toEqualBcoz('0 FR (23)',
        'Third Level Group Values should be present for FR');

    await page.expandSubGroupByPosition('alerts_ui_e2e', 'FR', 1);
    expect(await page.getSubGroupValuesByPosition('alerts_ui_e2e', 'FR', 1)).toEqualBcoz('0 FR (23)',
        'Third Level Group Values should not change when expanded for FR');
    expect(await page.getCellValuesFromTable('alerts_ui_e2e', 'id', '07b29c29-9...ff19eaa888')).toEqual(usGroupIds.concat(frGroupIds), 'rows should be present for FR');

    await page.unGroup();
    expect(await page.getActiveGroups()).toEqualBcoz([], 'no groups should be selected');
  });


  it('should have sort working for group details for multiple sub groups', async function() : Promise<any> {

    let usTSCol = ['2017-09-13 17:59:32', '2017-09-13 17:59:42', '2017-09-13 17:59:53', '2017-09-13 18:00:02', '2017-09-13 18:00:14'];
    let ruTSCol = ['2017-09-13 17:59:33', '2017-09-13 17:59:48', '2017-09-13 17:59:51', '2017-09-13 17:59:54', '2017-09-13 17:59:57'];
    let frTSCol = ['2017-09-13 17:59:37', '2017-09-13 17:59:46', '2017-09-13 18:00:31', '2017-09-13 18:00:33', '2017-09-13 18:00:37'];

    let usSortedTSCol = ['2017-09-13 18:02:19', '2017-09-13 18:02:16', '2017-09-13 18:02:09', '2017-09-13 18:01:58', '2017-09-13 18:01:52'];
    let ruSortedTSCol = ['2017-09-14 06:29:40', '2017-09-14 06:29:40', '2017-09-14 06:29:40', '2017-09-14 06:29:40', '2017-09-13 18:02:13'];
    let frSortedTSCol = ['2017-09-14 06:29:40', '2017-09-14 04:29:40', '2017-09-13 18:02:20', '2017-09-13 18:02:05', '2017-09-13 18:02:04'];

    await page.unGroup();
    await page.selectGroup('source:type');
    await page.selectGroup('enrichments:geo:ip_dst_addr:country');

    await page.expandDashGroup('alerts_ui_e2e');
    await page.expandSubGroup('alerts_ui_e2e', 'US');
    await page.expandSubGroup('alerts_ui_e2e', 'RU');
    await page.expandSubGroup('alerts_ui_e2e', 'FR');

    let unsortedTS = [...usTSCol, ...ruTSCol, ...frTSCol];
    let sortedTS = [...usSortedTSCol, ...ruSortedTSCol, ...frSortedTSCol];

    await page.sortSubGroup('alerts_ui_e2e', 'timestamp');

    expect(await page.getCellValuesFromTable('alerts_ui_e2e', 'timestamp', '2017-09-13 18:00:37')).toEqual(unsortedTS,
                                                                                                      'timestamp should be sorted asc');

    await page.sortSubGroup('alerts_ui_e2e', 'timestamp');

    expect(await page.getCellValuesFromTable('alerts_ui_e2e', 'timestamp', '2017-09-13 18:02:04')).toEqual(sortedTS,
                                                                                                      'timestamp should be sorted dsc');

    await page.unGroup();
    expect(page.getActiveGroups()).toEqualBcoz([], 'no groups should be selected');
  });

  it('should have search working for group details for multiple sub groups', async function() : Promise<any> {

    await page.unGroup();
    await listPage.setSearchText('enrichments:geo:ip_dst_addr:country:FR');

    expect(await listPage.getChangesAlertTableTitle('Alerts (169)')).toEqual('Alerts (25)');

    await page.selectGroup('source:type');
    await page.selectGroup('enrichments:geo:ip_dst_addr:country');

    await page.expandDashGroup('alerts_ui_e2e');
    expect(await page.getNumOfSubGroups('alerts_ui_e2e')).toEqual(1, 'three sub groups should be present');


    await page.expandSubGroup('alerts_ui_e2e', 'FR');

    let expected = ['FR', 'FR', 'FR', 'FR', 'FR'];
    expect(await page.getCellValuesFromTable('alerts_ui_e2e', 'enrichments:geo:ip_dst_addr:country', 'FR')).toEqual(expected,
                                                                                                              'id should be sorted');
    await page.unGroup();
  });

});
