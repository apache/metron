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
import { TableViewPage } from './table-view.po';
import { LoginPage } from '../../login/login.po';
import { loadTestData, deleteTestData } from '../../utils/e2e_util';
import { protractor, browser, element, by } from 'protractor';

class AutomationHelper {

  static readonly ID_ATTR: String = 'data-qe-id';

  static getElementByQEId(qeId: String) {
    const attr = AutomationHelper.ID_ATTR;
    const selector = qeId.split(' ').map(qeIdPart => `[${attr}=${qeIdPart}]`).join(' ');
    return element(by.css(selector));
  }

  static getTextByQEId(qeId: String) {
    const el = AutomationHelper.getElementByQEId(qeId);
    return browser.wait(protractor.ExpectedConditions.visibilityOf(el))
    .then(() => {
      return el.getText();
    });
  }
}

describe('Alerts Table', () => {

  let page: TableViewPage;
  let loginPage: LoginPage;
  const autHelper: AutomationHelper = new AutomationHelper();

  beforeAll(async () => {
    page = new TableViewPage();
    loginPage = new LoginPage();

    await loadTestData();
  });

  afterAll(async () => {
    await deleteTestData();
  })

  describe('should sort by colum: ', () => {
    
    beforeEach(async () => {
      await loginPage.login();
    });

    afterEach(async () => {
      await loginPage.logout();
    });

    it('sorting ASC by ip_src_addr', async function() {
      await page.sortTable('ip_src_addr'); // sorting ASC
      const ascOrder = [];
      ascOrder.push(await AutomationHelper.getTextByQEId('alerts-table row-0 cell-3'));
      ascOrder.push(await AutomationHelper.getTextByQEId('alerts-table row-1 cell-3'));
      ascOrder.push(await AutomationHelper.getTextByQEId('alerts-table row-2 cell-3'));

      expect(ascOrder).toEqual(['192.168.65.1','192.168.66.0','192.168.66.1']);
    });

    it('sorting DESC by ip_src_addr', async function() {
      await page.sortTable('ip_src_addr'); // sorting ASC
      await page.sortTable('ip_src_addr') // sorting DESC
      const descOrder = [];
      descOrder.push(await AutomationHelper.getTextByQEId('alerts-table row-0 cell-3'));
      descOrder.push(await AutomationHelper.getTextByQEId('alerts-table row-1 cell-3'));
      descOrder.push(await AutomationHelper.getTextByQEId('alerts-table row-2 cell-3'));
      
      expect(descOrder).toEqual(['192.168.138.160','192.168.138.159','192.168.138.158']);
    });

    it('sorting ASC by Score', async function() {
      await page.sortTable('Score'); // sorting ASC
      const ascOrder = [];
      ascOrder.push(await AutomationHelper.getTextByQEId('alerts-table row-0 score'));
      ascOrder.push(await AutomationHelper.getTextByQEId('alerts-table row-1 score'));
      ascOrder.push(await AutomationHelper.getTextByQEId('alerts-table row-2 score'));

      expect(ascOrder).toEqual(['-','-','-']);
    });

    it('sorting DESC by Score', async function() {
      await page.sortTable('Score'); // sorting ASC
      await page.sortTable('Score') // sorting DESC
      const descOrder = [];
      descOrder.push(await AutomationHelper.getTextByQEId('alerts-table row-0 score'));
      descOrder.push(await AutomationHelper.getTextByQEId('alerts-table row-1 score'));
      descOrder.push(await AutomationHelper.getTextByQEId('alerts-table row-2 score'));
      
      expect(descOrder).toEqual(['10','9','8']);
    });

    // it('should sort by score column', async () => {
    //   page.sortTable('Score');
    //   let scoreValue = await AutomationHelper.getTextByQEId('alerts-table row-0 score');
    //   expect(scoreValue).toEqual('-');
    //   page.sortTable('Score');
    //   scoreValue = await AutomationHelper.getTextByQEId('alerts-table row-0 score');
    //   expect(scoreValue).toEqual('10');
    // });

  })

});