import { browser, protractor, by, element } from 'protractor';
import request = require('request');
import fs = require('fs');

export function changeURL(url: string) {
    return browser.get(url).then(() => {
        return browser.getCurrentUrl().then((newURL) => {
            return newURL;
        });
    });
}

export function waitForURL(url: string) {
  let EC = protractor.ExpectedConditions;
  return browser.wait(EC.urlIs(url));
}

export function waitForText(selector, text) {
  let EC = protractor.ExpectedConditions;
  return browser.wait(EC.textToBePresentInElement(element(by.css(selector)), text));
}

export function waitForTextChange(element, previousText) {
  let EC = protractor.ExpectedConditions;
  if (previousText.length === 0) {
    return waitForNonEmptyText(element);
  }
  return browser.wait(EC.not(EC.textToBePresentInElement(element, previousText)));
}

export function waitForElementInVisibility (_element ) {
    let EC = protractor.ExpectedConditions;
    return browser.wait(EC.invisibilityOf(_element));
}

export function waitForElementPresence (_element ) {
    let EC = protractor.ExpectedConditions;
    return browser.wait(EC.presenceOf(_element));
}

export function waitForElementVisibility (_element ) {
    let EC = protractor.ExpectedConditions;
    return browser.wait(EC.visibilityOf(_element));
}

export function waitForStalenessOf (_element ) {
    let EC = protractor.ExpectedConditions;
    return browser.wait(EC.stalenessOf(_element));
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
  return browser.wait(waitForCssClass$(elementFinder, desiredClass));
}

export function waitForCssClassNotToBePresent(elementFinder, desiredClass) {
  function waitForCssClassNotToBePresent$(elementFinder, desiredClass)
  {
    return function () {
      return elementFinder.getAttribute('class').then(function (classValue) {
        return classValue && classValue.indexOf(desiredClass) === -1;
      });
    }
  }
  return browser.wait(waitForCssClassNotToBePresent$(elementFinder, desiredClass));
}

export function waitForNonEmptyText(elementFinder) {
  function waitForNonEmptyText$(elementFinder)
  {
    return function () {
      return elementFinder.getText().then(function (text) {
        return elementFinder.isDisplayed() && text.trim().length > 0;
      });
    }
  }
  return browser.wait(waitForNonEmptyText$(elementFinder));
}

function promiseHandler(resolve, reject) {
  return (response) => {
    if (response.statusCode === 200) {
      resolve()
    } else {
      reject();
    }
  };
}

export function loadTestData() {
  let deleteIndex = function () {
    return new Promise((resolve, reject) => {
      request.delete('http://node1:9200/alerts_ui_e2e_index*')
      .on('response', promiseHandler(resolve, reject));
    });
  };

  let createTemplate = function () {
    return new Promise((resolve, reject) => {
      fs.createReadStream('e2e/mock-data/alerts_ui_e2e_index.template')
      .pipe(request.post('http://node1:9200/_template/alerts_ui_e2e_index').on('response', promiseHandler(resolve, reject)));
    });

  };

  let loadData = function () {
    return new Promise((resolve, reject) => {
      fs.createReadStream('e2e/mock-data/alerts_ui_e2e_index.data')
      .pipe(request.post('http://node1:9200/alerts_ui_e2e_index/alerts_ui_e2e_doc/_bulk').on('response', promiseHandler(resolve, reject)));
    });
  };

  return deleteIndex().then(() => createTemplate()).then(() => loadData());
}

export function deleteTestData() {
  return new Promise((resolve, reject) => {
    request.delete('http://node1:9200/alerts_ui_e2e_index*')
    .on('response', promiseHandler(resolve, reject));
  });
}

export function createMetaAlertsIndex() {
  let deleteIndex = function () {
    return new Promise((resolve, reject) => {
      request.delete('http://node1:9200/metaalert_index*')
      .on('response', promiseHandler(resolve, reject));
    });
  };

  let createIndex = function () {
    new Promise((resolve, reject) => {
      fs.createReadStream('./../../metron-deployment/packaging/ambari/metron-mpack/src/main/resources/common-services/METRON/CURRENT/package/files/metaalert_index.template')
      .pipe(request.post('http://node1:9200/metaalert_index').on('response', promiseHandler(resolve, reject)));
    });
  };
  return deleteIndex().then(() => createIndex());
}

export function deleteMetaAlertsIndex() {
  return new Promise((resolve, reject) => {
    request.delete('http://node1:9200/metaalert_index*');
  });
}

