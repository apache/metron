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
import {
  scrollIntoView, waitForElementInVisibility, waitForElementPresence,
  waitForElementVisibility, waitForStalenessOf
} from '../utils/e2e_util';

export class MetronAlertDetailsPage {

  navigateTo(alertId: string) {
    return browser.waitForAngularEnabled(false)
    .then(() => browser.get('/alerts-list(dialog:details/alerts_ui_e2e/'+ alertId +'/alerts_ui_e2e_index)'))
    .then(() => browser.sleep(1000));
  }

  addCommentAndSave(comment: string, index: number) {
    let textAreaElement = element(by.css('app-alert-details textarea'));
    let addCommentButtonElement = element(by.buttonText('ADD COMMENT'));
    let latestCommentEle = element.all(by.cssContainingText('.comment-container .comment', comment));

    return textAreaElement.clear()
    .then(() => textAreaElement.sendKeys(comment))
    .then(() => addCommentButtonElement.click())
    .then(() => waitForElementVisibility(latestCommentEle));
  }

  clickNew() {
    return element.all(by.css('.metron-slider-pane-details table tbody tr')).get(1).all(by.css('td')).get(0).click();
  }

  clickOpen() {
    return element.all(by.css('.metron-slider-pane-details table tbody tr')).get(1).all(by.css('td')).get(1).click();
  }

  clickDismiss() {
    return element.all(by.css('.metron-slider-pane-details table tbody tr')).get(1).all(by.css('td')).get(2).click();
  }

  clickEscalate() {
    return element.all(by.css('.metron-slider-pane-details table tbody tr')).get(0).all(by.css('td')).get(1).click();
  }

  clickResolve() {
    return element.all(by.css('.metron-slider-pane-details table tbody tr')).get(2).all(by.css('td')).get(1).click();
  }

  clickCommentsInSideNav() {
    let commentIcon = element(by.css('app-alert-details .fa.fa-comment'));
    return waitForElementVisibility(commentIcon)
    .then(() => commentIcon.click())
    .then(() => waitForElementVisibility(element(by.buttonText('ADD COMMENT'))));
  }

  clickNoForConfirmation() {
    let cancelButton = element(by.css('.metron-dialog')).element(by.buttonText('Cancel'));
    let maskElement = element(by.css('.modal-backdrop'));
    waitForElementVisibility(cancelButton).then(() => element(by.css('.metron-dialog')).element(by.buttonText('Cancel')).click())
    .then(() => waitForElementInVisibility(maskElement));
  }

  clickYesForConfirmation(comment) {
    let okButton = element(by.css('.metron-dialog')).element(by.buttonText('OK'));
    let maskElement = element(by.css('.modal-backdrop'));
    waitForElementVisibility(okButton).then(() => element(by.css('.metron-dialog')).element(by.buttonText('OK')).click())
    .then(() => waitForElementInVisibility(maskElement));
  }

  closeDetailPane() {
  let dialogElement = element(by.css('.metron-dialog.modal.show'));
  return waitForElementInVisibility(dialogElement).then(() => scrollIntoView(element(by.css('app-alert-details .close-button')), true)
        .then(() => element(by.css('app-alert-details .close-button')).click())
        .then(() => waitForElementInVisibility(element(by.css('app-alert-details')))));
}

  deleteComment() {
    let scrollToEle = element.all(by.css('.comment-container')).get(0);
    let trashIcon = element.all(by.css('.fa.fa-trash-o')).get(0);
    browser.actions().mouseMove(scrollToEle).perform().then(() => waitForElementVisibility(trashIcon))
    .then(() => trashIcon.click())
    .then(() => waitForElementVisibility(element(by.css('.modal-backdrop'))));
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
    let commentsElement = element.all(by.css('app-table-view .fa.fa-comments-o'));
    return waitForElementPresence(commentsElement).then(() => commentsElement.count());
  }

  getCommentIconCountInTreeView() {
    let commentsElement = element.all(by.css('app-tree-view .fa.fa-comments-o'));
    return waitForElementPresence(commentsElement).then(() => commentsElement.count());
  }

  waitForTextChange(element, previousText) {
    let EC = protractor.ExpectedConditions;
    return browser.wait(EC.not(EC.textToBePresentInElement(element, previousText)));
  }

  getAlertNameOrId() {
    let nameSelector = element(by.css('app-alert-details .editable-text'));
    return waitForElementVisibility(nameSelector).then(() => nameSelector.getText());
  }

  clickRenameMetaAlert() {
    return element(by.css('app-alert-details .editable-text')).click();
  }

  renameMetaAlert(name: string) {
    return element(by.css('app-alert-details input.form-control')).sendKeys(name);
  }

  cancelRename() {
    return element(by.css('app-alert-details .input-group .fa.fa-times')).click();
  }

  saveRename() {
    return element(by.css('app-alert-details .fa.fa-check')).click();
  }

  getAlertDetailsCount() {
    let titleElement = element.all(by.css('app-alert-details .alert-details-title')).get(0);
    return waitForElementVisibility(titleElement).then(() => element.all(by.css('app-alert-details .alert-details-title')).count());
  }
}
