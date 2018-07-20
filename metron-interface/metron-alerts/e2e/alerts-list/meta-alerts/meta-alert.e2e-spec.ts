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
import {loadTestData, deleteTestData, createMetaAlertsIndex} from '../../utils/e2e_util';
import {TreeViewPage} from '../tree-view/tree-view.po';
import {MetronAlertDetailsPage} from '../../alert-details/alert-details.po';
import {MetaAlertPage} from './meta-alert.po';
import {AlertFacetsPage} from '../alert-filters/alert-filters.po';
import {browser} from 'protractor';

describe('Test spec for meta alerts workflow', function() {
  let detailsPage: MetronAlertDetailsPage;
  let tablePage: MetronAlertsPage;
  let metaAlertPage: MetaAlertPage;
  let treePage: TreeViewPage;
  let loginPage: LoginPage;
  let alertFacetsPage: AlertFacetsPage;

  beforeAll(async function() : Promise<any> {
    loginPage = new LoginPage();
    tablePage = new MetronAlertsPage();
    treePage = new TreeViewPage();
    tablePage = new MetronAlertsPage();
    metaAlertPage = new MetaAlertPage();
    detailsPage = new MetronAlertDetailsPage();
    alertFacetsPage = new AlertFacetsPage();

    jasmine.addMatchers(customMatchers);

    await createMetaAlertsIndex();
    await loadTestData();
    await loginPage.login();
  });

  afterAll(async function() : Promise<any> {
    await loginPage.logout();
    await deleteTestData();
  });

  it('should have all the steps for meta alerts workflow', async function() : Promise<any> {
    let comment1 = 'This is a sample comment';
    let userNameAndTimestamp = '- admin - a few seconds ago';
    let confirmText = 'Do you wish to create a meta alert with 22 selected alerts?';
    let dashRowValues = {
      'firstDashRow': ['0', '192.168.138.2', 'ALERTS', '22']
    };

    await tablePage.navigateTo();
    expect(await tablePage.getChangesAlertTableTitle('Alerts (0)')).toEqual('Alerts (169)');

    /* Create Meta Alert */
    await treePage.selectGroup('ip_dst_addr');
    expect(await treePage.getDashGroupValues('192.168.138.2')).toEqualBcoz(dashRowValues.firstDashRow, 'First Dashrow to be present');

    await browser.sleep(1000);
    await treePage.clickOnMergeAlerts('192.168.138.2');
    expect(await treePage.getConfirmationText()).toEqualBcoz(confirmText, 'confirmation text to be present');
    await treePage.clickNoForConfirmation();

    await treePage.clickOnMergeAlerts('192.168.138.2');
    await treePage.clickYesForConfirmation();

    await treePage.waitForElementToDisappear('192.168.138.2');

    await treePage.unGroup();

    /* Table should have all alerts */
    await tablePage.waitForMetaAlert(148);
    expect(await tablePage.getChangedPaginationText('1 - 25 of 169')).toEqualBcoz('1 - 25 of 148', 'pagination text to be present');
    expect(await tablePage.getCellValue(0, 2, '(14)')).toContain('(22)');
    expect(await tablePage.getNonHiddenRowCount()).toEqualBcoz(25, '25 rows to be visible');
    expect(await tablePage.getAllRowsCount()).toEqualBcoz(47, '47 rows to be available');
    expect(await tablePage.getHiddenRowCount()).toEqualBcoz(22, '22 rows to be hidden');
    await tablePage.expandMetaAlert(0);
    expect(await tablePage.getNonHiddenRowCount()).toEqualBcoz(47, '47 rows to be visible after group expand');
    expect(await tablePage.getAllRowsCount()).toEqualBcoz(47, '47 rows to be available after group expand');
    expect(await tablePage.getHiddenRowCount()).toEqualBcoz(0, '0 rows to be hidden after group expand');

    /* Meta Alert Status Change */
    await tablePage.toggleAlertInList(0);
    await tablePage.clickActionDropdownOption('Open');
    expect(await tablePage.getAlertStatus(0, 'NEW', 2)).toEqual('OPEN');
    expect(await tablePage.getAlertStatus(1, 'NEW')).toEqual('OPEN');
    expect(await tablePage.getAlertStatus(2, 'NEW')).toEqual('OPEN');

    /* Details Edit - add comments - remove comments - multiple alerts */
    await tablePage.clickOnMetaAlertRow(0);
    expect(await detailsPage.getAlertDetailsCount()).toEqualBcoz(22, '22 alert details should be present');
    await detailsPage.clickRenameMetaAlert();
    await detailsPage.renameMetaAlert('e2e-meta-alert');
    await detailsPage.cancelRename();
    expect(await detailsPage.getAlertNameOrId()).not.toEqual('e2e-meta-alert');
    await detailsPage.clickRenameMetaAlert();
    await detailsPage.renameMetaAlert('e2e-meta-alert');
    await detailsPage.saveRename();
    expect(await detailsPage.getAlertNameOrId()).toEqual('e2e-meta-alert');

    // The below code will fail until this issue is resolved in Protractor: https://github.com/angular/protractor/issues/4693
    // This is because the connection resets before deleting the test comment, which causes the assertion to be false

    // await detailsPage.clickCommentsInSideNav();
    // await detailsPage.addCommentAndSave(comment1, 0);
    // expect(await detailsPage.getCommentsText()).toEqual([comment1]);
    // expect(await detailsPage.getCommentsUserNameAndTimeStamp()).toEqual([userNameAndTimestamp]);
    // expect(await detailsPage.getCommentIconCountInListView()).toEqual(1);

    // await detailsPage.deleteComment();
    // await detailsPage.clickYesForConfirmation(comment1);
    await detailsPage.closeDetailPane();

    /* Add to alert */
    await tablePage.toggleAlertInList(3);
    await tablePage.clickActionDropdownOption('Add to Alert');
    await metaAlertPage.waitForDialog();
    expect(await metaAlertPage.getPageTitle()).toEqualBcoz('Add to Alert', 'Add Alert Title should be present');
    expect(await metaAlertPage.getMetaAlertsTitle()).toEqualBcoz('SELECT OPEN ALERT', 'select open alert title should be present');
    expect(await metaAlertPage.getAvailableMetaAlerts()).toEqualBcoz('e2e-meta-alert (22)', 'Meta alert should be present');
    await metaAlertPage.selectRadio();
    await metaAlertPage.addToMetaAlert();
    // FIXME: line below will fail because the following: https://issues.apache.org/jira/browse/METRON-1654
    // expect(await tablePage.getCellValue(0, 2, '(22')).toContain('(23)', 'alert count should be incremented');

    // /* Remove from alert */
    let removAlertConfirmText = 'Do you wish to remove the alert from the meta alert?';
    await tablePage.removeAlert(2);
    expect(await treePage.getConfirmationText()).toEqualBcoz(removAlertConfirmText, 'confirmation text to remove alert from meta alert');
    await treePage.clickYesForConfirmation();
    // FIXME: line below will fail because the following: https://issues.apache.org/jira/browse/METRON-1654
    // expect(await tablePage.getCellValue(0, 2, '(23')).toContain('(22)', 'alert count should be decremented');

    // /* Delete Meta Alert */
    let removeMetaAlertConfirmText = 'Do you wish to remove all the alerts from meta alert?';
    await tablePage.removeAlert(0);
    expect(await treePage.getConfirmationText()).toEqualBcoz(removeMetaAlertConfirmText, 'confirmation text to remove meta alert');
    await treePage.clickYesForConfirmation();
  });

  it('should create a meta alert from nesting of more than one level', async function() : Promise<any> {
    let groupByItems = {
      'source:type': '1',
      'ip_dst_addr': '7',
      'enrichm...:country': '3',
      'ip_src_addr': '4'
    };
    let alertsInMetaAlerts = [
      '82f8046d-d...03b17480dd',
      '5c1825f6-7...da3abe3aec',
      '9041285e-9...a04a885b53',
      'ed906df7-2...91cc54c2f3',
      'c894bbcf-3...74cf0cc1fe',
      'e63ff7ae-d...cddbe0c0b3',
      '3c346bf9-b...cb04b43210',
      'dcc483af-c...7bb802b652',
      'b71f085d-6...a4904d8fcf',
      '754b4f63-3...b39678207f',
      'd9430af3-e...9a18600ab2',
      '9a943c94-c...3b9046b782',
      'f39dc401-3...1f9cf02cd9',
      'd887fe69-c...2fdba06dbc',
      'e38be207-b...60a43e3378',
      'eba8eccb-b...0005325a90',
      'adca96e3-1...979bf0b5f1',
      '42f4ce28-8...b3d575b507',
      'aed3d10f-b...8b8a139f25',
      'a5e95569-a...0e2613b29a'
    ];

    let alertsAfterDeletedInMetaAlerts =  [ 
      '3c346bf9-b...cb04b43210',
      '42f4ce28-8...b3d575b507',
      '5c1825f6-7...da3abe3aec',
      '754b4f63-3...b39678207f',
      '82f8046d-d...03b17480dd',
      '9041285e-9...a04a885b53',
      '9a943c94-c...3b9046b782',
      'adca96e3-1...979bf0b5f1',
      'aed3d10f-b...8b8a139f25',
      'b71f085d-6...a4904d8fcf',
      'c894bbcf-3...74cf0cc1fe',
      'd887fe69-c...2fdba06dbc',
      'd9430af3-e...9a18600ab2',
      'dcc483af-c...7bb802b652',
      'e38be207-b...60a43e3378',
      'e63ff7ae-d...cddbe0c0b3',
      'eba8eccb-b...0005325a90',
      'ed906df7-2...91cc54c2f3',
      'f39dc401-3...1f9cf02cd9'
     ];

    // Create a meta alert from a group that is nested by more than 1 level
    await treePage.selectGroup('source:type');
    await treePage.selectGroup('ip_dst_addr');
    await treePage.expandDashGroup('alerts_ui_e2e');

    await treePage.clickOnMergeAlertsInTable('alerts_ui_e2e', '224.0.0.251', 0);
    await treePage.clickYesForConfirmation();

    await treePage.unGroup();
    await tablePage.waitForMetaAlert(150);

    expect(await tablePage.getChangedPaginationText('1 - 25 of 169')).toEqualBcoz('1 - 25 of 150', 'pagination text to be present');

    // Meta Alert should appear in Filters
    await alertFacetsPage.toggleFacetState(3);
    let facetValues = await alertFacetsPage.getFacetValues(3);
    expect(await facetValues['metaalert']).toEqual('1', 'for source:type facet');

    // Meta Alert should not appear in Groups
    expect(await treePage.getGroupByItemNames()).toEqualBcoz(Object.keys(groupByItems), 'Group By Elements names should be present');
    expect(await treePage.getGroupByItemCounts()).toEqualBcoz(Object.keys(groupByItems).map(key => groupByItems[key]),
        '5 Group By Elements values should be present');


    await tablePage.setSearchText('guid:c894bbcf-3195-0708-aebe-0574cf0cc1fe', '150');
    expect(await tablePage.getChangesAlertTableTitle('Alerts (150)')).toEqual('Alerts (1)');
    await tablePage.expandMetaAlert(0);
    expect(await tablePage.getAllRowsCount()).toEqual(21);
    await tablePage.expandMetaAlert(0);
    await tablePage.clickClearSearch('150');
    expect(await tablePage.getChangesAlertTableTitle('Alerts (1)')).toEqual('Alerts (150)');

    // Delete a meta alert from the middle and check the data
    await tablePage.expandMetaAlert(0);
    let guidValues = await tablePage.getTableCellValues(3, 1, 21);
    guidValues = guidValues.slice(1, 21).sort();
    expect(guidValues).toEqual(alertsInMetaAlerts.sort());
    await tablePage.removeAlert(5);
    await treePage.clickYesForConfirmation();
    // FIXME: line below will fail because the following: https://issues.apache.org/jira/browse/METRON-1654
    // expect(await tablePage.getCellValue(0, 2, '(20')).toContain('(19)', 'alert count should be decremented');
    await browser.sleep(1000);
    let guidValuesAfterDeleteOp = await tablePage.getTableCellValues(3, 1, 20);
    guidValuesAfterDeleteOp = guidValuesAfterDeleteOp.slice(1, 20).sort();
    expect(guidValuesAfterDeleteOp).toEqual(alertsAfterDeletedInMetaAlerts.sort());

    //Remove the meta alert
    await tablePage.removeAlert(0);
    await treePage.clickYesForConfirmation();
  });

});
