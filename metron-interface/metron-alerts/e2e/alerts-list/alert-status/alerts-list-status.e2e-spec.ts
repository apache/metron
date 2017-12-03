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
    loginPage.login();

    await loadTestData();
  });

  afterAll(async function() : Promise<any> {
    loginPage.logout();
    await deleteTestData();
  });

  beforeEach(() => {
    page = new MetronAlertsPage();
    treePage = new TreeViewPage();
    jasmine.addMatchers(customMatchers);
  });

  it('should change alert status for multiple alerts to OPEN', () => {
    page.navigateTo();
    page.toggleAlertInList(0);
    page.toggleAlertInList(1);
    page.toggleAlertInList(2);
    page.clickActionDropdownOption('Open');
    expect(page.getAlertStatus(0, 'NEW')).toEqual('OPEN');
    expect(page.getAlertStatus(1, 'NEW')).toEqual('OPEN');
    expect(page.getAlertStatus(2, 'NEW')).toEqual('OPEN');
  });

  it('should change alert status for multiple alerts to DISMISS', () => {
    page.toggleAlertInList(3);
    page.toggleAlertInList(4);
    page.toggleAlertInList(5);
    page.clickActionDropdownOption('Dismiss');
    expect(page.getAlertStatus(3, 'NEW')).toEqual('DISMISS');
    expect(page.getAlertStatus(4, 'NEW')).toEqual('DISMISS');
    expect(page.getAlertStatus(5, 'NEW')).toEqual('DISMISS');
  });

  it('should change alert status for multiple alerts to ESCALATE', () => {
    page.toggleAlertInList(6);
    page.toggleAlertInList(7);
    page.toggleAlertInList(8);
    page.clickActionDropdownOption('Escalate');
    expect(page.getAlertStatus(6, 'NEW')).toEqual('ESCALATE');
    expect(page.getAlertStatus(7, 'NEW')).toEqual('ESCALATE');
    expect(page.getAlertStatus(8, 'NEW')).toEqual('ESCALATE');
  });

  it('should change alert status for multiple alerts to RESOLVE', () => {
    page.toggleAlertInList(9);
    page.toggleAlertInList(10);
    page.toggleAlertInList(11);
    page.clickActionDropdownOption('Resolve');
    expect(page.getAlertStatus(9, 'NEW')).toEqual('RESOLVE');
    expect(page.getAlertStatus(10, 'NEW')).toEqual('RESOLVE');
    expect(page.getAlertStatus(11, 'NEW')).toEqual('RESOLVE');
  });


  it('should change alert status for multiple alerts to OPEN in tree view', () => {
    treePage.selectGroup('source:type');
    treePage.selectGroup('enrichments:geo:ip_dst_addr:country');
    
    treePage.expandDashGroup('alerts_ui_e2e');
    treePage.expandSubGroup('alerts_ui_e2e', 'US');
    treePage.expandSubGroup('alerts_ui_e2e', 'RU');
    treePage.expandSubGroup('alerts_ui_e2e', 'FR');

    treePage.toggleAlertInTree(1);
    treePage.toggleAlertInTree(2);
    treePage.toggleAlertInTree(3);
    page.clickActionDropdownOption('Open');
    expect(treePage.getAlertStatusForTreeView(1, 'NEW')).toEqual('OPEN');
    expect(treePage.getAlertStatusForTreeView(2, 'NEW')).toEqual('OPEN');
    expect(treePage.getAlertStatusForTreeView(3, 'NEW')).toEqual('OPEN');

    treePage.toggleAlertInTree(4);
    treePage.toggleAlertInTree(5);
    page.clickActionDropdownOption('Dismiss');
    expect(treePage.getAlertStatusForTreeView(4, 'NEW')).toEqual('DISMISS');
    expect(treePage.getAlertStatusForTreeView(5, 'NEW')).toEqual('DISMISS');

    treePage.toggleAlertInTree(8);
    treePage.toggleAlertInTree(9);
    page.clickActionDropdownOption('Escalate');
    expect(treePage.getAlertStatusForTreeView(8, 'NEW')).toEqual('ESCALATE');
    expect(treePage.getAlertStatusForTreeView(9, 'NEW')).toEqual('ESCALATE');

    treePage.toggleAlertInTree(10);
    treePage.toggleAlertInTree(11);
    treePage.toggleAlertInTree(12);
    page.clickActionDropdownOption('Resolve');
    expect(treePage.getAlertStatusForTreeView(10, 'NEW')).toEqual('RESOLVE');
    expect(treePage.getAlertStatusForTreeView(11, 'NEW')).toEqual('RESOLVE');
    expect(treePage.getAlertStatusForTreeView(12, 'NEW')).toEqual('RESOLVE');
  });

});
