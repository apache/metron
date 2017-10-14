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

import {browser, element, by, protractor} from 'protractor';
import {waitForElementInVisibility, waitForElementPresence, waitForElementVisibility} from '../utils/e2e_util';

export class MetronAlertDetailsPage {

  navigateTo(alertId: string) {
    browser.waitForAngularEnabled(false);
    browser.get('/alerts-list(dialog:details/alerts_ui_e2e/'+ alertId +'/alerts_ui_e2e_index)');
    browser.sleep(2000);
  }

  addCommentAndSave(comment: string, index: number) {
    let textAreaElement = element(by.css('app-alert-details textarea'));
    let addCommentButtonElement = element(by.buttonText('ADD COMMENT'));
    let latestCommentEle = element.all(by.css('.comment-container .comment')).get(index);

    textAreaElement.clear()
    .then(() => textAreaElement.sendKeys(comment))
    .then(() => addCommentButtonElement.click())
    .then(() => waitForElementPresence(latestCommentEle));
  }

  clickNew() {
    element.all(by.css('.metron-slider-pane-details table tbody tr')).get(1).all(by.css('td')).get(0).click();
  }

  clickOpen() {
    element.all(by.css('.metron-slider-pane-details table tbody tr')).get(1).all(by.css('td')).get(1).click();
  }

  clickDismiss() {
    element.all(by.css('.metron-slider-pane-details table tbody tr')).get(1).all(by.css('td')).get(2).click();
  }

  clickEscalate() {
    element.all(by.css('.metron-slider-pane-details table tbody tr')).get(0).all(by.css('td')).get(1).click();
  }

  clickResolve() {
    element.all(by.css('.metron-slider-pane-details table tbody tr')).get(2).all(by.css('td')).get(1).click();
  }

  clickCommentsInSideNav() {
    return element(by.css('app-alert-details .fa.fa-comment')).click();
  }

  clickNoForConfirmation() {
    browser.sleep(1000);
    let dialogElement = element(by.css('.metron-dialog .modal-header .close'));
    let maskElement = element(by.css('.modal-backdrop.fade'));
    waitForElementVisibility(dialogElement).then(() => element(by.css('.metron-dialog')).element(by.buttonText('Cancel')).click())
    .then(() => waitForElementInVisibility(maskElement));
  }

  clickYesForConfirmation() {
    browser.sleep(1000);
    let dialogElement = element(by.css('.metron-dialog .modal-header .close'));
    let maskElement = element(by.css('.modal-backdrop.fade'));
    waitForElementVisibility(dialogElement).then(() => element(by.css('.metron-dialog')).element(by.buttonText('OK')).click())
    .then(() => waitForElementInVisibility(maskElement));
  }

  closeDetailPane() {
    element(by.css('app-alert-details .close-button')).click();
    browser.sleep(2000);
  }

  deleteComment() {
    let scrollToEle = element.all(by.css('.comment-container')).get(0);
    let trashIcon = element.all(by.css('.fa.fa-trash-o')).get(0);
    browser.actions().mouseMove(scrollToEle).perform().then(() => waitForElementVisibility(trashIcon))
    .then(() => element.all(by.css('.fa.fa-trash-o')).get(0).click());
  }

  getAlertStatus(previousText) {
    let alertStatusElement = element.all(by.css('.metron-slider-pane-details .form .row')).get(0).all(by.css('div')).get(1);
    return this.waitForTextChange(alertStatusElement, previousText).then(() => {
      return alertStatusElement.getText();
    });
  }

  getCommentsText() {
    return element.all(by.css('.comment-container .comment')).getText();
  }

  getCommentsUserNameAndTimeStamp() {
    return element.all(by.css('.comment-container .username-timestamp')).getText();
  }

  getCommentIconCountInListView() {
    return element.all(by.css('app-table-view .fa.fa-comments-o')).count();
  }

  getCommentIconCountInTreeView() {
    return element.all(by.css('app-tree-view .fa.fa-comments-o')).count();
  }

  waitForTextChange(element, previousText) {
    let EC = protractor.ExpectedConditions;
    return browser.wait(EC.not(EC.textToBePresentInElement(element, previousText)));
  }

}
