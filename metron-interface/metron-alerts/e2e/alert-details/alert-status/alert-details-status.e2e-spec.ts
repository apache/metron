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

describe('metron-alerts alert status', function() {
  let page: MetronAlertDetailsPage;
  let loginPage: LoginPage;

  beforeAll(() => {
    loadTestData();
    loginPage = new LoginPage();
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
    page.navigateTo();
    page.clickNew();
    page.clickOpen();
    expect(page.getAlertStatus('NEW')).toEqual('OPEN');
    page.clickDismiss();
    expect(page.getAlertStatus('OPEN')).toEqual('DISMISS');
    page.clickEscalate();
    expect(page.getAlertStatus('DISMISS')).toEqual('ESCALATE');
    page.clickResolve();
    expect(page.getAlertStatus('ESCALATE')).toEqual('RESOLVE');
    page.clickNew();
  });

});