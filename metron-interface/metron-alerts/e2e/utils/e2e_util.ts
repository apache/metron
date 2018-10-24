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

import { browser, protractor, by, element, ElementFinder } from 'protractor';
import request = require('request');
import fs = require('fs');

const expCond = protractor.ExpectedConditions;

export class UtilFun {
  public static async waitForElementPresence(element: ElementFinder): Promise<void> {
    await browser.wait(
      expCond.visibilityOf(element),
      10000,
      `${element.locator()} was expected to be visible`
    );
  }
}

export class AutomationHelper {

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

export function changeURL(url: string) {
  return browser.get(url).then(() => {
      return browser.getCurrentUrl().then((newURL) => {
          return newURL;
      });
  });
}

export function waitForURL(url: string) {
  return browser.wait(expCond.urlIs(url));
}

export function waitForText(selector, text) {
  return browser.wait(expCond.textToBePresentInElement(element(by.css(selector)), text)).catch((error) => console.log(`waitForText:`, error));;
}

export function waitForTextChange(element, previousText) {
  if (previousText.trim().length === 0) {
    return waitForNonEmptyText(element);
  }
  return browser.wait(expCond.not(expCond.textToBePresentInElement(element, previousText))).catch((error) => console.log(`${element.locator()} waitForTextChange:`, error));
}

export function waitForElementInVisibility (_element ) {
  return browser.wait(expCond.invisibilityOf(_element)).catch((error) => console.log(`${_element.locator()} waitForElementInVisibility:`, error));
}

export function waitForElementPresence (_element ) {
  return browser.wait(expCond.presenceOf(_element)).catch((error) => console.log(`${_element.locator()} waitForElementPresence:`, error));
}

export function waitForElementVisibility (_element ) {
  return browser.wait(expCond.visibilityOf(_element)).catch((error) => console.log(`${_element.locator()} waitForElementVisibility:`, error));
}

export function waitForElementPresenceAndvisbility(selector) {
  return browser.wait(expCond.visibilityOf(element(by.css(selector)))).catch((error) => console.log(`waitForElementPresenceAndvisbility: `, error));
}

export function waitForStalenessOf (_element ) {
  return browser.wait(expCond.stalenessOf(_element)).catch((error) => console.log(`${_element.locator()} waitForStalenessOf: `, error));
}

export function waitForCssClass(elementFinder, desiredClass) {
  function waitForCssClass$(elementFinder, desiredClass)
  {
    return function () {
      return elementFinder.getAttribute('class').then(function (classValue) {
        return classValue && classValue.indexOf(desiredClass) >= 0;
      });
    }
  }
  return browser.wait(waitForCssClass$(elementFinder, desiredClass)).catch((error) => console.log(`waitForCssClass:`, error));
}

export function waitForCssClassNotToBePresent(elementFinder, desiredClass) {
  function waitForCssClassNotToBePresent$(elementFinder, desiredClass)
  {
    return function () {
      return elementFinder.getAttribute('class').then(function (classValue) {
        return classValue && classValue.indexOf(desiredClass) === -1;
      }).catch((error) => console.log(`waitForCssClassNotToBePresent:`, error));;
    }
  }
  return browser.wait(waitForCssClassNotToBePresent$(elementFinder, desiredClass)).catch((error) => console.log(`waitForCssClassNotToBePresent:`, error));
}

export function catchNoSuchElementError() {
  return (err) => {
    if (err.name === 'NoSuchElementError' || err.name === 'Error') {
      return null;
    }
    throw err;
  };
}

export function waitForNonEmptyTextAndGetText(elementFinder) {
  function waitForNonEmptyText$(elementFinder)
  {
    return function () {
      return elementFinder.getText().then(function (text) {
        return text.trim().length > 0;
      }).catch(catchNoSuchElementError());
    }
  }
  return browser.wait(waitForNonEmptyText$(elementFinder))
  .then(() => elementFinder.getText())
  .catch( catchNoSuchElementError());
}

export function waitForNonEmptyText(elementFinder) {
  function waitForNonEmptyText$(elementFinder)
  {
    return function () {
      return elementFinder.getText().then(function (text) {
        return text.trim().length > 0;
      }).catch(catchNoSuchElementError());
    }
  }
  return browser.wait(waitForNonEmptyText$(elementFinder)).catch(catchNoSuchElementError());
}

export function waitForElementCountGreaterThan(className, expectedCount) {
  function waitForElementCountGreaterThan$()
  {
    return function () {
      return element.all(by.css(className)).count().then(function (count) {
        return count >= expectedCount;
      }).catch((error) => console.log(`waitForElementCountGreaterThan:`, error));;
    }
  }

  return browser.wait(waitForElementCountGreaterThan$()).catch((error) => console.log(`waitForElementCountGreaterThan: `, error));
}

export function scrollIntoView(element, eleToTopBottom){
    return browser.executeScript(function(element, eleToTopBottom) {
      element.scrollIntoView(eleToTopBottom);
    },  element.getWebElement(), eleToTopBottom)
    .catch((error) => console.log());
}

function promiseHandlerWithResponse(resolve, reject) {
  return (response) => {
    if (response && (response.statusCode === 200 || response.statusCode === 404)) {
      resolve();
    } else {
      console.log(response.statusCode);
      reject();
    }
  };
}

function promiseHandlerWithResponseAndBody(resolve, reject) {
  return (error, response, body) => {
    if (response && (response.statusCode === 200 || response.statusCode === 404)) {
      resolve();
    } else {
      reject();
    }
  };
}

export function loadTestData() {
  let deleteIndex = function () {
    return new Promise((resolve, reject) => {
      request.delete('http://node1:9200/alerts_ui_e2e_index*')
      .on('response', promiseHandlerWithResponse(resolve, reject));
    });
  };

  let createTemplate = function () {
    return new Promise((resolve, reject) => {
      let template = fs.readFileSync('e2e/mock-data/alerts_ui_e2e_index.template', 'utf8');
      request({
        url: 'http://node1:9200/_template/alerts_ui_e2e_index',
        method: 'POST',
        body: template
      },promiseHandlerWithResponseAndBody(resolve, reject));
    });
  };

  let loadData = function () {
    return new Promise((resolve, reject) => {
      let data = fs.readFileSync('e2e/mock-data/alerts_ui_e2e_index.data', 'utf8');
      request({
        url: 'http://node1:9200/alerts_ui_e2e_index/alerts_ui_e2e_doc/_bulk',
        method: 'POST',
        body: data
      }, promiseHandlerWithResponseAndBody(resolve, reject));
    })
  };

  return deleteIndex().then(() => createTemplate()).then(() => loadData(), reason => {
    console.error( 'Load test data failed ', reason );
  });
}

export function reduce_for_get_all() {
  return  (acc, elem) => {
    return elem.getText().then(function(text) {
      acc.push(text);
      return acc;
    }).catch(e => console.log(e));
  };
}

export function deleteTestData() {
  request.delete('http://node1:9200/alerts_ui_e2e_index*');
}

export function createMetaAlertsIndex() {
  let deleteIndex = function () {
    return new Promise((resolve, reject) => {
      request.delete('http://node1:9200/metaalert_index*')
            .on('response', promiseHandlerWithResponse(resolve, reject));
    });
  };

  let createIndex = function () {
    return new Promise((resolve, reject) => {
      let template = fs.readFileSync('./../../metron-deployment/packaging/ambari/metron-mpack/src/main/resources/common-services/METRON/CURRENT/package/files/metaalert_index.template', 'utf8');
      request({
        url: 'http://node1:9200/_template/metaalert_index',
        method: 'POST',
        body: template
      }, promiseHandlerWithResponseAndBody(resolve, reject));
    });
  };

  return deleteIndex().then(() => createIndex(), reason => {
    console.error( 'create Meta Alerts Index failed', reason );
  });
}

export function deleteMetaAlertsIndex() {
  request.delete('http://node1:9200/metaalert_index*');
}
