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

import {browser, element, by} from 'protractor';
import {
  waitForElementPresence, waitForTextChange, waitForElementVisibility,
  waitForElementInVisibility, waitForCssClass, waitForText, waitForStalenessOf
} from '../../utils/e2e_util';

export class TreeViewPage {
  navigateToAlertsList() {
    browser.waitForAngularEnabled(false);
    return browser.get('/alerts-list');
  }

  clickOnRow(id: string) {
    let idElement = element(by.css('a[title="' + id +'"]'));
    waitForElementPresence(idElement)
    .then(() => browser.actions().mouseMove(idElement).perform())
    .then(() => idElement.element(by.xpath('../..')).all(by.css('td')).get(9).click());
    browser.sleep(2000);
  }

  getActiveGroups() {
    return element.all(by.css('app-group-by .group-by-items.active')).getAttribute('data-name');
  }

  getGroupByCount() {
    return waitForElementPresence(element.all(by.css('app-group-by .group-by-items'))).then(() => {
      return element.all(by.css('app-group-by .group-by-items')).count();
    });
  }

  getGroupByItemNames() {
    return element.all(by.css('app-group-by .group-by-items .name')).getText();
  }

  getGroupByItemCounts() {
    return element.all(by.css('app-group-by .group-by-items .count')).getText();
  }

  getSubGroupValues(name: string, rowName: string) {
    return this.getSubGroupValuesByPosition(name, rowName, 0);
  }

  getSubGroupValuesByPosition(name: string, rowName: string, position: number) {
    return element.all(by.css('[data-name="' + name + '"] table tbody tr[data-name="' + rowName + '"]')).get(position).getText();
  }

  selectGroup(name: string) {
    return element(by.css('app-group-by div[data-name="' + name + '"]')).click()
    .then(() => waitForCssClass(element(by.css('app-group-by div[data-name="' + name + '"]')), 'active'))
    .then(() => browser.waitForAngular());
  }

  dragGroup(from: string, to: string) {
   return  browser.actions().dragAndDrop(
        element(by.css('app-group-by div[data-name="' + from + '"]')),
        element(by.css('app-group-by div[data-name="' + to + '"]'))
    ).perform()
    .then(() => browser.waitForAngular());
  }

  getDashGroupValues(name: string) {
    return waitForElementVisibility(element(by.css('[data-name="' + name + '"] .card-header .severity-padding .title')))
    .then(() => waitForElementVisibility(element(by.css('[data-name="' + name + '"] .card-header .two-line .title'))))
    .then(() => {
      browser.sleep(500);
      return element.all(by.css('[data-name="' + name + '"] .card-header span')).getText()
    });
  }

  expandDashGroup(name: string) {
    let cardElement = element(by.css('.card[data-name="' + name +'"]'));
    let cardHeader = element(by.css('.card[data-name="' + name + '"] .card-header'));
    let cardBody = element(by.css('.card[data-name="' + name + '"] .collapse'));

    return waitForElementPresence(cardBody)
    .then(() => waitForElementVisibility(cardHeader))
    .then(() => browser.actions().mouseMove(element(by.css('.card[data-name="' + name + '"] .card-header .down-arrow'))).perform())
    .then(() => cardHeader.click())
    .then(() => waitForCssClass(cardBody, 'show'))
    .then(() => waitForElementVisibility(element(by.css('.card[data-name="' + name + '"] .collapse table tbody tr:nth-child(1)'))));
  }

  expandSubGroup(groupName: string, rowName: string) {
    return this.expandSubGroupByPosition(groupName, rowName, 0);
  }

  expandSubGroupByPosition(groupName: string, rowName: string, position: number) {
    let subGroupElement = element.all(by.css('[data-name="' + groupName + '"] tr[data-name="' + rowName + '"]')).get(position);
    return waitForElementVisibility(subGroupElement)
    .then(() => browser.actions().mouseMove(subGroupElement).perform())
    .then(() => subGroupElement.click())
    .then(() => waitForElementVisibility(subGroupElement.element(by.css('.fa-caret-down'))));
  }

  getDashGroupTableValuesForRow(name: string, rowId: number) {
    this.scrollToDashRow(name);
    return waitForElementPresence(element(by.css('[data-name="' + name + '"] table tbody tr'))).then(() => {
      return element.all(by.css('[data-name="' + name + '"] table tbody tr')).get(rowId).all(by.css('td')).getText();
    });
  }

  getTableValuesByRowId(name: string, rowId: number, waitForAnchor: string) {
    return waitForElementPresence(element(by. cssContainingText('[data-name="' + name + '"] a', waitForAnchor))).then(() => {
      return element.all(by.css('[data-name="' + name + '"] table tbody tr')).get(rowId).all(by.css('td')).getText();
    });
  }

  getTableValuesForRow(name: string, rowName: string, waitForAnchor: string) {
    return waitForElementPresence(element(by. cssContainingText('[data-name="' + name + '"] a', waitForAnchor))).then(() => {
      return element.all(by.css('[data-name="' + name + '"] tr[data-name="' + rowName + '"]')).all(by.css('td')).getText();
    });
  }

  scrollToDashRow(name: string) {
    let scrollToEle = element(by.css('[data-name="' + name + '"] .card-header'));
    waitForElementPresence(scrollToEle).then(() => {
      return browser.actions().mouseMove(scrollToEle).perform();
    });
  }

  clickOnNextPage(name: string) {
    return element(by.css('[data-name="' + name + '"] i.fa-chevron-right')).click();
  }

  unGroup() {
    return element(by.css('app-group-by .ungroup-button')).click()
    .then(() => waitForStalenessOf(element(by.css('app-tree-view'))));
  }

  getIdOfAllExpandedRows() {
    return element.all(by.css('[data-name="' + name + '"] table tbody tr')).then(row => {
      browser.pause();
    });
  }

  getNumOfSubGroups(groupName: string) {
    return element.all(by.css('[data-name="' + groupName + '"] table tbody tr')).count();
  }

  getCellValuesFromTable(groupName: string, cellName: string, waitForAnchor: string) {
    return waitForElementPresence(element(by.cssContainingText('[data-name="' + cellName + '"] a', waitForAnchor)))
    .then(() => {
      browser.sleep(1000);
      return element.all(by.css('[data-name="' + groupName + '"] table tbody [data-name="' + cellName + '"]')).map(element => {
        return browser.actions().mouseMove(element).perform().then(() => (element.getText()));
      });
    });
  }

  sortSubGroup(groupName: string, colName: string) {
    return element.all(by.css('[data-name="' + groupName + '"] metron-config-sorter[title="' + colName + '"]')).click();
  }

  toggleAlertInTree(index: number) {
    let selector = by.css('app-tree-view tbody tr');
    let checkbox = element.all(selector).get(index).element(by.css('label'));
    waitForElementPresence(checkbox).then(() => {
      browser.actions().mouseMove(checkbox).perform().then(() => {
        checkbox.click();
      });
    });
  }

  getAlertStatusForTreeView(rowIndex: number, previousText) {
    let row = element.all(by.css('app-tree-view tbody tr')).get(rowIndex);
    let column = row.all(by.css('td a')).get(8);
    return waitForTextChange(column, previousText).then(() => {
      return column.getText();
    });
  }

  clickOnMergeAlerts(groupName: string) {
    return element(by.css('[data-name="' + groupName + '"] .fa-link')).click();
  }

  clickOnMergeAlertsInTable(groupName: string, waitForAnchor: string, rowIndex: number) {
    let elementFinder = element.all(by.css('[data-name="' + groupName + '"] table tbody tr')).get(rowIndex).element(by.css('.fa-link'));
    return waitForElementVisibility(elementFinder)
    .then(() => elementFinder.click());
  }

  getConfirmationText() {
    let maskElement = element(by.className('modal-backdrop'));
    return waitForElementVisibility(maskElement)
    .then(() =>  element(by.css('.metron-dialog .modal-body')).getText());
  }

  clickNoForConfirmation() {
    let maskElement = element(by.className('modal-backdrop'));
    let closeButton = element(by.css('.metron-dialog')).element(by.buttonText('Cancel'));
    waitForElementVisibility(maskElement)
    .then(() => closeButton.click())
    .then(() => waitForElementInVisibility(maskElement));
  }

  clickYesForConfirmation() {
    let okButton = element(by.css('.metron-dialog')).element(by.buttonText('OK'));
    let maskElement = element(by.className('modal-backdrop'));
    return waitForElementVisibility(maskElement)
    .then(() => okButton.click())
    .then(() => waitForElementInVisibility(maskElement));
  }

  waitForElementToDisappear(groupName: string) {
    return waitForElementInVisibility(element.all(by.css('[data-name="' + groupName + '"]')));
  }

  waitForTextChangeAndExpand(groupName: string, subGroupName: string, previousValue: string) {
    return waitForTextChange(element(by.css(`[data-name="${subGroupName}"] .group-value`)), previousValue)
    .then(() => this.expandSubGroup(groupName, subGroupName));
  }
}
