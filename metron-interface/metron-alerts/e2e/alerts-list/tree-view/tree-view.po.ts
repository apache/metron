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

import { browser, element, by } from 'protractor';
import {
  waitForElementPresence, waitForTextChange, waitForElementVisibility,
  waitForElementInVisibility, waitForCssClass, waitForStalenessOf,
  waitForElementCountGreaterThan, reduce_for_get_all, scrollIntoView
} from '../../utils/e2e_util';

export class TreeViewPage {
  navigateToAlertsList() {
    return browser.get('/alerts-list');
  }

  clickOnRow(id: string) {
    let idElement = element(by.css('a[title="' + id +'"]'));
    let td = idElement.element(by.xpath('../..')).all(by.css('td')).get(9);
    let detailsPane = element(by.css('.metron-slider-pane-details'));
    return waitForElementPresence(idElement)
    .then(() => browser.actions().mouseMove(idElement).perform())
    .then(() => browser.actions().mouseMove(td).perform())
    .then(() => browser.actions().click().perform())
    .then(() => waitForElementVisibility(detailsPane))
    .then(() => browser.sleep(2000));
  }

  getActiveGroups() {
    return waitForElementCountGreaterThan('app-group-by .group-by-items .name', 4)
    .then(() => element.all(by.css('app-group-by .group-by-items.active .name')).reduce(reduce_for_get_all(), []));
  }

  getGroupByCount() {
    return waitForElementPresence(element(by.css('app-group-by .group-by-items'))).then(() => {
      return element.all(by.css('app-group-by .group-by-items')).count();
    });
  }

  getGroupByItemNames() {
    return element.all(by.css('app-group-by .group-by-items .name')).reduce(reduce_for_get_all(), []);
  }

  getGroupByItemCounts() {
    return element.all(by.css('app-group-by .group-by-items .count')).reduce(reduce_for_get_all(), []);
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

  async simulateDragAndDrop(target: string, destination: string) {
    target = `app-group-by div[data-name=\"${target}\"]`;
    destination = `app-group-by div[data-name=\"${destination}\"]`;
    return  await browser.executeScript((target, destination) => {
      let getEventOptions = (el, relatedElement) => {
        //coordinates from destination element
        const coords = el.getBoundingClientRect();
        const x = coords.x || coords.left;
        const y = coords.y || coords.top;
        return {
          x: x,
          y: y,
          clientX: x,
          clientY: y,
          screenX: x,
          screenY: y,
          //target reference
          relatedTarget: relatedElement
        };
      };

      let raise = (el, type, options?) => {
        const o = options || { which: 1 };
        const e = document.createEvent('Event');
        e.initEvent(type, true, true);
        Object.keys(o).forEach(apply);
        el.dispatchEvent(e);
        function apply(key) {
          e[key] = o[key];
        }
      };


      let targetEl = document.querySelector(target);
      let destinationEl = document.querySelector(destination);
      let options = getEventOptions(destinationEl, targetEl);

      //start drag
      raise(targetEl, 'mousedown');
      raise(targetEl, 'mousemove');
      //set event on location
      raise(destinationEl, 'mousemove', options);
      //drop
      raise(destinationEl, 'mouseup', options);

    }, target, destination);
  }

  async getDashGroupValues(name: string) {
      let dashScore = await element(by.css('[data-name="' + name + '"] .dash-score')).getText();
      let groupName = await element(by.css('[data-name="' + name + '"] .text-light.severity-padding .title')).getText();
      let title = await element(by.css('[data-name="' + name + '"] .text-light.two-line .text-dark')).getText();
      let count = await element(by.css('[data-name="' + name + '"] .text-light.two-line .title')).getText();

      return Promise.all([dashScore, groupName, title, count]);
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
    .then(() => scrollIntoView(subGroupElement.element(by.css('.fa-caret-right')), true))
    .then(() => subGroupElement.click())
    .then(() => waitForElementVisibility(subGroupElement.element(by.css('.fa-caret-down'))));
  }

  async getTableValuesByRowId(name: string, rowId: number, waitForAnchor: string) {
    await waitForElementPresence(element(by. cssContainingText('[data-name="' + name + '"] a', waitForAnchor)));
    return element.all(by.css('[data-name="' + name + '"] table tbody tr')).get(rowId).all(by.css('td a')).reduce(reduce_for_get_all(), []);
  }

  getTableValuesForRow(name: string, rowName: string, waitForAnchor: string) {
    return waitForElementPresence(element(by. cssContainingText('[data-name="' + name + '"] a', waitForAnchor))).then(() => {
      return element.all(by.css('[data-name="' + name + '"] tr[data-name="' + rowName + '"]')).all(by.css('td')).getText();
    });
  }

  scrollToDashRow(name: string) {
    let scrollToEle = element(by.css('[data-name="' + name + '"] .card-header'));
    return scrollIntoView(scrollToEle, true);
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
    });
  }

  getNumOfSubGroups(groupName: string) {
    return element.all(by.css('[data-name="' + groupName + '"] table tbody tr')).count();
  }

  getCellValuesFromTable(groupName: string, cellName: string, waitForAnchor: string) {
    let waitForEle = element.all(by.cssContainingText(`[data-name="${cellName}"] a`, waitForAnchor)).get(0);
    return waitForElementPresence(waitForEle)
          .then(() => scrollIntoView(waitForEle, true))
          .then(() => element.all(by.css(`[data-name="${groupName}"] [data-name="${cellName}"]`)).reduce(reduce_for_get_all(), []))
  }

  sortSubGroup(groupName: string, colName: string) {
    return element(by.css(`[data-name="${groupName}"] metron-config-sorter[title="${colName}"]`)).click();
  }

  toggleAlertInTree(index: number) {
    let selector = by.css('app-tree-view tbody tr');
    let checkbox = element.all(selector).get(index).element(by.css('label'));

    return waitForElementPresence(checkbox)
            .then(() => browser.actions().mouseMove(checkbox).perform())
            .then(() => checkbox.click());
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
    return waitForElementVisibility(maskElement)
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
