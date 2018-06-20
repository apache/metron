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
import { protractor, browser, element, by, ElementFinder } from 'protractor';
import { async } from 'q';

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

describe('Alert Table View Tests', () => {

  let page: TableViewPage;
  let loginPage: LoginPage;
  const autHelper: AutomationHelper = new AutomationHelper();

  beforeAll(async () => {
    page = new TableViewPage();
    loginPage = new LoginPage();

    await loadTestData();
    await loginPage.login();
  });

  afterAll((params) => {
    deleteTestData();
    // loginPage.logout();
  })

  describe('Table sorting', () => {
    
    it('should sort by columns', () => {
      page.sortTable('ip_src_addr') // sorting ASC
      .then(() => {  
        return AutomationHelper.getTextByQEId('alerts-table row-0 cell-3');
      }).then((result) => {
        expect(result).toEqual('192.168.66.1');
      }).then(() => {
        return page.sortTable('ip_src_addr') // sorting DESC
      }).then(() => {
        return AutomationHelper.getTextByQEId('alerts-table row-0 cell-3');
      }).then((result) => {
        expect(result).toEqual('192.168.138.158');
      });
    });

    // it('should sort by columns 2', async function() {
    //   page.sortTable('ip_src_addr'); // sorting ASC
    //   let firstSortResult = await AutomationHelper.getTextByQEId('alerts-table row-0 cell-3');
    //   expect(firstSortResult).toEqual('192.168.66.1');
    //   await page.sortTable('ip_src_addr') // sorting DESC
    //   let secondSortResult = await AutomationHelper.getTextByQEId('alerts-table row-0 cell-3');
    //   expect(secondSortResult).toEqual('192.168.138.158');
    // });

    // it('should sort by score column', () => {
    //   //tablePOs.sortTable('Score');
    // });

  })

});