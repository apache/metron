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
import { MetronAlertDetailsPage } from '../alert-details.po';
import {customMatchers} from '../../matchers/custom-matchers';
import {LoginPage} from '../../login/login.po';
import {loadTestData, deleteTestData} from '../../utils/e2e_util';
import { MetronAlertsPage } from '../../alerts-list/alerts-list.po';
import {TreeViewPage} from '../../alerts-list/tree-view/tree-view.po';

describe('Test spec for metron details page', function() {
  let page: MetronAlertDetailsPage;
  let listPage: MetronAlertsPage;
  let treePage: TreeViewPage;
  let loginPage: LoginPage;

  beforeAll(async function() : Promise<any> {
    loginPage = new LoginPage();
    listPage = new MetronAlertsPage();
    treePage = new TreeViewPage();

    await loadTestData();
    await loginPage.login();
  });

  afterAll(async function() : Promise<any> {
    new MetronAlertsPage().navigateTo();
    await loginPage.logout();
    await deleteTestData();
  });

  beforeEach(() => {
    page = new MetronAlertDetailsPage();
    jasmine.addMatchers(customMatchers);
  });

  /**
   * Test is failing because of a known issue of Metron Alert UI.
   * https://issues.apache.org/jira/browse/METRON-1654
   * 
   * Till the fix become available the test should remain ignored.
   * */
  xit('should change alert statuses', async function() : Promise<any> {
    let alertId = '2cc174d7-c049-aaf4-d0d6-138073777309';

    await page.navigateTo(alertId);
    expect(await page.getAlertStatus('ANY')).toEqual('NEW');
    await page.clickOpen();
    expect(await page.getAlertStatus('NEW')).toEqual('OPEN');
    expect(await listPage.getAlertStatusById(alertId)).toEqual('OPEN');
    await page.clickDismiss();
    expect(await page.getAlertStatus('OPEN')).toEqual('DISMISS');
    expect(await listPage.getAlertStatusById(alertId)).toEqual('DISMISS');
    await page.clickEscalate();
    expect(await page.getAlertStatus('DISMISS')).toEqual('ESCALATE');
    expect(await listPage.getAlertStatusById(alertId)).toEqual('ESCALATE');
    await page.clickResolve();
    expect(await page.getAlertStatus('ESCALATE')).toEqual('RESOLVE');
    expect(await listPage.getAlertStatusById(alertId)).toEqual('RESOLVE');
    await page.clickNew();
  });

  /**
   * The below code will fail until this issue is resolved in Protractor: https://github.com/angular/protractor/issues/4693
   * This is because the connection resets before deleting the test comment, which causes the assertion to be false
   */
  xit('should add comments for table view', async function() : Promise<any> {
    let comment1 = 'This is a sample comment';
    let comment2 = 'This is a sample comment again';
    let userNameAndTimestamp = '- admin - a few seconds ago';
    let alertId = '2cc174d7-c049-aaf4-d0d6-138073777309';

    page.navigateTo(alertId);

    await page.clickCommentsInSideNav();
    await page.addCommentAndSave(comment1, 0);

    expect(await page.getCommentsText()).toEqual([comment1]);
    expect(await page.getCommentsUserNameAndTimeStamp()).toEqual([userNameAndTimestamp]);

    await page.addCommentAndSave(comment2, 0);
    expect(await page.getCommentsText()).toEqual([comment2, comment1]);
    expect(await page.getCommentsUserNameAndTimeStamp()).toEqual([userNameAndTimestamp, userNameAndTimestamp]);

    await page.deleteComment();
    await page.clickNoForConfirmation();
    expect(await page.getCommentsText()).toEqual([comment2, comment1]);

    await page.deleteComment();
    await page.clickYesForConfirmation(comment2);
    expect(await page.getCommentsText()).toEqual([comment1]);

    expect(await page.getCommentIconCountInListView()).toEqual(1);

    await page.deleteComment();
    await page.clickYesForConfirmation(comment1);
    expect(await page.getCommentsText()).toEqual([]);

    await page.closeDetailPane();
  });

  xit('should add comments for tree view', async function(): Promise<any> {
    let comment1 = 'This is a sample comment';
    let comment2 = 'This is a sample comment again';
    let userNameAndTimestamp = '- admin - a few seconds ago';

    await treePage.navigateToAlertsList();
    await treePage.selectGroup('source:type');
    await treePage.expandDashGroup('alerts_ui_e2e');

    await treePage.clickOnRow('acf5a641-9cdb-d7ec-c309-6ea316e14fbe');
    await page.clickCommentsInSideNav();
    await page.addCommentAndSave(comment1, 0);

    expect(await page.getCommentsText()).toEqual([comment1]);
    expect(await page.getCommentsUserNameAndTimeStamp()).toEqual([userNameAndTimestamp]);
    expect(await page.getCommentIconCountInTreeView()).toEqual(1);

    await page.deleteComment();
    await page.clickYesForConfirmation(comment1);
    expect(await page.getCommentsText()).toEqual([]);
    await page.closeDetailPane();

    await treePage.unGroup();

    await treePage.selectGroup('source:type');
    await treePage.selectGroup('enrichments:geo:ip_dst_addr:country');
    await treePage.expandDashGroup('alerts_ui_e2e');
    await treePage.expandSubGroup('alerts_ui_e2e', 'FR');

    await treePage.clickOnRow('07b29c29-9ab0-37dd-31d3-08ff19eaa888');
    await page.clickCommentsInSideNav();
    await page.addCommentAndSave(comment2, 0);

    expect(await page.getCommentsText()).toEqual(comment2);
    expect(await page.getCommentsUserNameAndTimeStamp()).toEqual(userNameAndTimestamp);
    expect(await page.getCommentIconCountInTreeView()).toEqual(1);

    await page.deleteComment();
    await page.clickYesForConfirmation(comment2);
    await page.closeDetailPane();
  });

});
