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

describe('metron-alerts alert status', function() {
  let page: MetronAlertDetailsPage;
  let listPage: MetronAlertsPage;
  let treePage: TreeViewPage;
  let loginPage: LoginPage;

  beforeAll(() => {
    loadTestData();
    loginPage = new LoginPage();
    listPage = new MetronAlertsPage();
    treePage = new TreeViewPage();
    loginPage.login();
  });

  afterAll(() => {
    new MetronAlertsPage().navigateTo();
    loginPage.logout();
    deleteTestData();
  });

  beforeEach(() => {
    page = new MetronAlertDetailsPage();
    jasmine.addMatchers(customMatchers);
  });

  it('should change alert statuses', () => {
    let alertId = 'c4c5e418-3938-099e-bb0d-37028a98dca8';
    
    page.navigateTo(alertId);
    page.clickNew();
    page.clickOpen();
    expect(page.getAlertStatus('NEW')).toEqual('OPEN');
    expect(listPage.getAlertStatusById(alertId)).toEqual('OPEN');
    page.clickDismiss();
    expect(page.getAlertStatus('OPEN')).toEqual('DISMISS');
    expect(listPage.getAlertStatusById(alertId)).toEqual('DISMISS');
    page.clickEscalate();
    expect(page.getAlertStatus('DISMISS')).toEqual('ESCALATE');
    expect(listPage.getAlertStatusById(alertId)).toEqual('ESCALATE');
    page.clickResolve();
    expect(page.getAlertStatus('ESCALATE')).toEqual('RESOLVE');
    expect(listPage.getAlertStatusById(alertId)).toEqual('RESOLVE');
    page.clickNew();
  });

  it('should add comments for table view', () => {
    let comment1 = 'This is a sample comment';
    let comment2 = 'This is a sample comment again';
    let userNameAndTimestamp = '- admin - a few seconds ago';

    page.clickCommentsInSideNav();
    page.addCommentAndSave(comment1, 0);

    expect(page.getCommentsText()).toEqual([comment1]);
    expect(page.getCommentsUserNameAndTimeStamp()).toEqual([userNameAndTimestamp]);

    page.addCommentAndSave(comment2, 1);
    expect(page.getCommentsText()).toEqual([comment2, comment1]);
    expect(page.getCommentsUserNameAndTimeStamp()).toEqual([userNameAndTimestamp, userNameAndTimestamp]);

    page.deleteComment();
    page.clickNoForConfirmation();
    expect(page.getCommentsText()).toEqual([comment2, comment1]);

    page.deleteComment();
    page.clickYesForConfirmation();
    expect(page.getCommentsText()).toEqual([comment1]);

    expect(page.getCommentIconCountInListView()).toEqual(1);

    page.deleteComment();
    page.clickYesForConfirmation();
    expect(page.getCommentsText()).toEqual([]);

    page.closeDetailPane();
  });

  it('should add comments for tree view', () => {
    let comment1 = 'This is a sample comment';
    let comment2 = 'This is a sample comment again';
    let userNameAndTimestamp = '- admin - a few seconds ago';

    treePage.selectGroup('source:type');
    treePage.expandDashGroup('alerts_ui_e2e');

    treePage.clickOnRow('acf5a641-9cdb-d7ec-c309-6ea316e14fbe');
    page.clickCommentsInSideNav();
    page.addCommentAndSave(comment1, 0);

    expect(page.getCommentsText()).toEqual([comment1]);
    expect(page.getCommentsUserNameAndTimeStamp()).toEqual([userNameAndTimestamp]);
    expect(page.getCommentIconCountInTreeView()).toEqual(1);

    page.deleteComment();
    page.clickYesForConfirmation();
    expect(page.getCommentsText()).toEqual([]);
    page.closeDetailPane();

    treePage.unGroup();

    treePage.selectGroup('source:type');
    treePage.selectGroup('enrichments:geo:ip_dst_addr:country');
    treePage.expandDashGroup('alerts_ui_e2e');
    treePage.expandSubGroup('alerts_ui_e2e', 'FR');

    treePage.clickOnRow('7cd91565-132f-3340-db76-3ade5be54a6e');
    page.clickCommentsInSideNav();
    page.addCommentAndSave(comment2, 0);

    expect(page.getCommentsText()).toEqual([comment2]);
    expect(page.getCommentsUserNameAndTimeStamp()).toEqual([userNameAndTimestamp]);
    expect(page.getCommentIconCountInTreeView()).toEqual(1);

    page.deleteComment();
    page.clickYesForConfirmation();
    expect(page.getCommentsText()).toEqual([]);
    page.closeDetailPane();
  });

});
