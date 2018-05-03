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
import {TreeViewPage} from '../tree-view/tree-view.po';

describe('Test spec for changing alert status in list view & tree view', function() {
  let page: MetronAlertsPage;
  let treePage: TreeViewPage;
  let loginPage: LoginPage;

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
    treePage = new TreeViewPage();
    jasmine.addMatchers(customMatchers);
  });

  it('should change alert status for multiple alerts to OPEN', async function() : Promise<any> {
    await page.navigateTo();
    await page.toggleAlertInList(0);
    await page.toggleAlertInList(1);
    await page.toggleAlertInList(2);
    await page.clickActionDropdownOption('Open');
    expect(await page.getAlertStatus(0, 'NEW')).toEqual('OPEN');
    expect(await page.getAlertStatus(1, 'NEW')).toEqual('OPEN');
    expect(await page.getAlertStatus(2, 'NEW')).toEqual('OPEN');
  });

  it('should change alert status for multiple alerts to DISMISS', async function() : Promise<any> {
    await page.toggleAlertInList(3);
    await page.toggleAlertInList(4);
    await page.toggleAlertInList(5);
    await page.clickActionDropdownOption('Dismiss');
    expect(await page.getAlertStatus(3, 'NEW')).toEqual('DISMISS');
    expect(await page.getAlertStatus(4, 'NEW')).toEqual('DISMISS');
    expect(await page.getAlertStatus(5, 'NEW')).toEqual('DISMISS');
  });

  it('should change alert status for multiple alerts to ESCALATE', async function() : Promise<any> {
    await page.toggleAlertInList(6);
    await page.toggleAlertInList(7);
    await page.toggleAlertInList(8);
    await page.clickActionDropdownOption('Escalate');
    expect(await page.getAlertStatus(6, 'NEW')).toEqual('ESCALATE');
    expect(await page.getAlertStatus(7, 'NEW')).toEqual('ESCALATE');
    expect(await page.getAlertStatus(8, 'NEW')).toEqual('ESCALATE');
  });

  it('should change alert status for multiple alerts to RESOLVE', async function() : Promise<any> {
    await page.toggleAlertInList(9);
    await page.toggleAlertInList(10);
    await page.toggleAlertInList(11);
    await page.clickActionDropdownOption('Resolve');
    expect(await page.getAlertStatus(9, 'NEW')).toEqual('RESOLVE');
    expect(await page.getAlertStatus(10, 'NEW')).toEqual('RESOLVE');
    expect(await page.getAlertStatus(11, 'NEW')).toEqual('RESOLVE');
  });


  it('should change alert status for multiple alerts to OPEN in tree view', async function() : Promise<any> {
    await treePage.selectGroup('source:type');
    await treePage.selectGroup('enrichments:geo:ip_dst_addr:country');

    await treePage.expandDashGroup('alerts_ui_e2e');
    await treePage.expandSubGroup('alerts_ui_e2e', 'US');
    await treePage.expandSubGroup('alerts_ui_e2e', 'RU');
    await treePage.expandSubGroup('alerts_ui_e2e', 'FR');

    await treePage.toggleAlertInTree(1);
    await treePage.toggleAlertInTree(2);
    await treePage.toggleAlertInTree(3);
    await page.clickActionDropdownOption('Open');
    expect(treePage.getAlertStatusForTreeView(1, 'NEW')).toEqual('OPEN');
    expect(treePage.getAlertStatusForTreeView(2, 'NEW')).toEqual('OPEN');
    expect(treePage.getAlertStatusForTreeView(3, 'NEW')).toEqual('OPEN');

    await treePage.toggleAlertInTree(4);
    await treePage.toggleAlertInTree(5);
    await page.clickActionDropdownOption('Dismiss');
    expect(treePage.getAlertStatusForTreeView(4, 'NEW')).toEqual('DISMISS');
    expect(treePage.getAlertStatusForTreeView(5, 'NEW')).toEqual('DISMISS');

    await treePage.toggleAlertInTree(8);
    await treePage.toggleAlertInTree(9);
    await page.clickActionDropdownOption('Escalate');
    expect(treePage.getAlertStatusForTreeView(8, 'NEW')).toEqual('ESCALATE');
    expect(treePage.getAlertStatusForTreeView(9, 'NEW')).toEqual('ESCALATE');

    await treePage.toggleAlertInTree(10);
    await treePage.toggleAlertInTree(11);
    await treePage.toggleAlertInTree(12);
    await page.clickActionDropdownOption('Resolve');
    expect(treePage.getAlertStatusForTreeView(10, 'NEW')).toEqual('RESOLVE');
    expect(treePage.getAlertStatusForTreeView(11, 'NEW')).toEqual('RESOLVE');
    expect(treePage.getAlertStatusForTreeView(12, 'NEW')).toEqual('RESOLVE');
  });

});
