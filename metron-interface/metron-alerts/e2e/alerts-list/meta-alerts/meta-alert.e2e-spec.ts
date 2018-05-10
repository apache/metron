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
import {AlertFacetsPage} from '../alert-filters/alert-filters.po';

describe('meta-alerts workflow', function() {
  let detailsPage: MetronAlertDetailsPage;
  let tablePage: MetronAlertsPage;
  let metaAlertPage: MetaAlertPage;
  let treePage: TreeViewPage;
  let loginPage: LoginPage;
  let alertFacetsPage: AlertFacetsPage;

  beforeAll(() => {
    loadTestData();

    loginPage = new LoginPage();
    loginPage.login();
    tablePage = new MetronAlertsPage();
    treePage = new TreeViewPage();
    tablePage = new MetronAlertsPage();
    metaAlertPage = new MetaAlertPage();
    detailsPage = new MetronAlertDetailsPage();
    alertFacetsPage = new AlertFacetsPage();
  });

  afterAll(() => {
    loginPage.logout();
    deleteTestData();
  });

  beforeEach(() => {
    jasmine.addMatchers(customMatchers);
  });

  it('should have all the steps for meta alerts workflow', () => {
    let comment1 = 'This is a sample comment';
    let userNameAndTimestamp = '- admin - a few seconds ago';
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

    treePage.waitForElementToDisappear('192.168.138.158');

    treePage.unGroup();

    /* Table should have all alerts */
    tablePage.waitForMetaAlert();
    expect(tablePage.getPaginationText()).toEqualBcoz('1 - 25 of 57', 'pagination text to be present');
    expect(tablePage.getCellValue(0, 2, '(114)')).toContain('(113)', 'number of alerts in a meta alert should be correct');
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

    detailsPage.clickCommentsInSideNav();
    detailsPage.addCommentAndSave(comment1, 0);
    expect(detailsPage.getCommentsText()).toEqual([comment1]);
    expect(detailsPage.getCommentsUserNameAndTimeStamp()).toEqual([userNameAndTimestamp]);
    expect(detailsPage.getCommentIconCountInListView()).toEqual(1);

    detailsPage.deleteComment();
    detailsPage.clickYesForConfirmation();

    detailsPage.closeDetailPane();

    /* Add to alert */
    tablePage.toggleAlertInList(3);
    tablePage.clickActionDropdownOption('Add to Alert');
    expect(metaAlertPage.getPageTitle()).toEqualBcoz('Add to Alert', 'Add Alert Title should be present');
    expect(metaAlertPage.getMetaAlertsTitle()).toEqualBcoz('SELECT OPEN ALERT', 'select open alert title should be present');
    expect(metaAlertPage.getAvailableMetaAlerts()).toEqualBcoz('e2e-meta-alert (113)', 'Meta alert should be present');
    metaAlertPage.selectRadio();
    metaAlertPage.addToMetaAlert();
    expect(tablePage.getCellValue(0, 2, '(113')).toContain('(114)', 'alert count should be incremented');

    /* Remove from alert */
    let removAlertConfirmText = 'Do you wish to remove the alert from the meta alert?';
    tablePage.removeAlert(2);
    expect(treePage.getConfirmationText()).toEqualBcoz(removAlertConfirmText, 'confirmation text to remove alert from meta alert');
    treePage.clickYesForConfirmation();
    expect(tablePage.getCellValue(0, 2, '(114')).toContain('(113)', 'alert count should be decremented');

    /* Delete Meta Alert */
    let removeMetaAlertConfirmText = 'Do you wish to remove all the alerts from meta alert?';
    tablePage.removeAlert(0);
    expect(treePage.getConfirmationText()).toEqualBcoz(removeMetaAlertConfirmText, 'confirmation text to remove meta alert');
    treePage.clickYesForConfirmation();
  });

  it('should create a meta alert from nesting of more than one level', () => {
    let groupByItems = {
      'source:type': '1',
      'ip_dst_addr': '7',
      'enrichm...:country': '3',
      'ip_src_addr': '2'
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

    let alertsAfterDeletedInMetaAlerts = [
      '82f8046d-d...03b17480dd',
      '5c1825f6-7...da3abe3aec',
      '9041285e-9...a04a885b53',
      'ed906df7-2...91cc54c2f3',
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

    // Create a meta alert from a group that is nested by more than 1 level
    treePage.selectGroup('source:type');
    treePage.selectGroup('ip_dst_addr');
    treePage.expandDashGroup('alerts_ui_e2e');

    treePage.clickOnMergeAlertsInTable('alerts_ui_e2e', '224.0.0.251', 0);
    treePage.clickYesForConfirmation();

    treePage.unGroup();
    tablePage.waitForMetaAlert();

    expect(tablePage.getPaginationText()).toEqualBcoz('1 - 25 of 150', 'pagination text to be present');

    // Meta Alert should appear in Filters
    alertFacetsPage.toggleFacetState(3);
    expect(alertFacetsPage.getFacetValues(3)).toEqual({'metaalert': '1' }, 'for source:type facet');

    // Meta Alert should not appear in Groups
    expect(treePage.getGroupByItemNames()).toEqualBcoz(Object.keys(groupByItems), 'Group By Elements names should be present');
    expect(treePage.getGroupByItemCounts()).toEqualBcoz(Object.keys(groupByItems).map(key => groupByItems[key]),
        '5 Group By Elements values should be present');


    tablePage.setSearchText('guid:c894bbcf-3195-0708-aebe-0574cf0cc1fe');
    expect(tablePage.getChangesAlertTableTitle('Alerts (150)')).toEqual('Alerts (1)');
    tablePage.expandMetaAlert(0);
    expect(tablePage.getAllRowsCount()).toEqual(21);
    tablePage.expandMetaAlert(0);
    tablePage.clickClearSearch();
    expect(tablePage.getChangesAlertTableTitle('Alerts (1)')).toEqual('Alerts (150)');

    // Delete a meta alert from the middle and check the data
    tablePage.expandMetaAlert(0);
    expect(tablePage.getTableCellValues(3, 1, 21)).toEqual(alertsInMetaAlerts);
    tablePage.removeAlert(5);
    treePage.clickYesForConfirmation();
    expect(tablePage.getCellValue(0, 2, '(20')).toContain('(19)', 'alert count should be decremented');
    expect(tablePage.getTableCellValues(3, 1, 20)).toEqual(alertsAfterDeletedInMetaAlerts);

    //Remove the meta alert
    tablePage.removeAlert(0);
    treePage.clickYesForConfirmation();
  });

});
