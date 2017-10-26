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
import {MetronAlertDetailsPage} from '../../alert-details/alert-details.po';
import {MetaAlertPage} from './meta-alert.po';

describe('meta-alerts workflow', function() {
  let detailsPage: MetronAlertDetailsPage;
  let tablePage: MetronAlertsPage;
  let metaAlertPage: MetaAlertPage;
  let treePage: TreeViewPage;
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
    tablePage = new MetronAlertsPage();
    treePage = new TreeViewPage();
    tablePage = new MetronAlertsPage();
    metaAlertPage = new MetaAlertPage();
    detailsPage = new MetronAlertDetailsPage();
    jasmine.addMatchers(customMatchers);
  });

  it('should have all the steps for meta alerts workflow', () => {
    let confirmText = 'Do you wish to create a meta alert with 113 selected alerts?';
    let dashRowValues = {
      'firstDashRow': ['0', '192.168.138.158', 'ALERTS', '113'],
      'secondDashRow': ['0', '192.168.66.1', 'ALERTS', '56']
    };

    tablePage.navigateTo();

    /* Create Meta Alert */
    treePage.selectGroup('ip_src_addr');
    expect(treePage.getDashGroupValues('192.168.138.158')).toEqualBcoz(dashRowValues.firstDashRow, 'First Dashrow to be present');
    expect(treePage.getDashGroupValues('192.168.66.1')).toEqualBcoz(dashRowValues.secondDashRow, 'Second Dashrow to be present');

    treePage.clickOnMergeAlerts('192.168.138.158');
    expect(treePage.getConfirmationText()).toEqualBcoz(confirmText, 'confirmation text to be present');
    treePage.clickNoForConfirmation();

    treePage.clickOnMergeAlerts('192.168.138.158');
    treePage.clickYesForConfirmation();

    // treePage.waitForElementToDisappear('192.168.138.158');

    treePage.unGroup();

    /* Table should have all alerts */
    tablePage.waitForMetaAlert();
    expect(tablePage.getPaginationText()).toEqualBcoz('1 - 25 of 170', 'pagination text to be present'); /* should be 57 */
    expect(tablePage.getCellValue(0, 2)).toContain('(113)', 'number of alerts in a meta alert should be correct');
    expect(tablePage.getNonHiddenRowCount()).toEqualBcoz(25, '25 rows to be visible');
    expect(tablePage.getAllRowsCount()).toEqualBcoz(138, '138 rows to be available');
    expect(tablePage.getHiddenRowCount()).toEqualBcoz(113, '113 rows to be hidden');
    tablePage.expandMetaAlert(0);
    expect(tablePage.getNonHiddenRowCount()).toEqualBcoz(138, '138 rows to be visible after group expand');
    expect(tablePage.getAllRowsCount()).toEqualBcoz(138, '138 rows to be available after group expand');
    expect(tablePage.getHiddenRowCount()).toEqualBcoz(0, '0 rows to be hidden after group expand');

    /* Meta Alert Status Change */
    tablePage.toggleAlertInList(0);
    tablePage.clickActionDropdownOption('Open');
    expect(tablePage.getAlertStatus(0, 'NEW', 2)).toEqual('OPEN');
    expect(tablePage.getAlertStatus(1, 'NEW')).toEqual('OPEN');
    expect(tablePage.getAlertStatus(2, 'NEW')).toEqual('OPEN');

    /* Details Edit Should work - add comments - remove comments - multiple alerts */
    tablePage.clickOnMetaAlertRow(0);
    expect(detailsPage.getAlertDetailsCount()).toEqualBcoz(113, '113 alert details should be present');
    detailsPage.clickRenameMetaAlert();
    detailsPage.renameMetaAlert('e2e-meta-alert');
    detailsPage.cancelRename();
    expect(detailsPage.getAlertNameOrId()).not.toEqual('e2e-meta-alert');
    detailsPage.clickRenameMetaAlert();
    detailsPage.renameMetaAlert('e2e-meta-alert');
    detailsPage.saveRename();
    expect(detailsPage.getAlertNameOrId()).toEqual('e2e-meta-alert');
    detailsPage.closeDetailPane();

    /* Add to alert */
    tablePage.toggleAlertInList(3);
    tablePage.clickActionDropdownOption('Add to Alert');
    expect(metaAlertPage.getPageTitle()).toEqualBcoz('Add to Alert', 'Add Alert Title should be present');
    expect(metaAlertPage.getMetaAlertsTitle()).toEqualBcoz('SELECT OPEN ALERT', 'select open alert title should be present');
    expect(metaAlertPage.getAvailableMetaAlerts()).toEqualBcoz('e2e-meta-alert (113)', 'Meta alert should be present');
    metaAlertPage.selectRadio();
    metaAlertPage.addToMetaAlert();
    expect(tablePage.getCellValue(0, 2)).toContain('(114)', 'alert count should be incremented');

    /* Remove from alert */
    let removAlertConfirmText = 'Do you wish to remove the alert from the meta alert?';
    tablePage.removeAlert(2);
    expect(treePage.getConfirmationText()).toEqualBcoz(removAlertConfirmText, 'confirmation text to remove alert from meta alert');
    treePage.clickYesForConfirmation();
    expect(tablePage.getCellValue(0, 2)).toContain('(113)', 'alert count should be decremented');

    /* Delete Meta Alert */
    let removeMetaAlertConfirmText = 'Do you wish to remove all the alerts from meta alert?';
    tablePage.removeAlert(0);
    expect(treePage.getConfirmationText()).toEqualBcoz(removeMetaAlertConfirmText, 'confirmation text to remove meta alert');
    treePage.clickYesForConfirmation();
    expect(tablePage.getAllRowsCount()).toEqualBcoz(24, '24 rows should be present after removing meta alert');
  });

});
