import { browser, protractor } from 'protractor';
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

export function waitForText(element, text) {
  let EC = protractor.ExpectedConditions;
  return browser.wait(EC.textToBePresentInElementValue(element, text));
}

export function waitForTextChange(element, previousText) {
  let EC = protractor.ExpectedConditions;
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

export function loadTestData() {
  deleteTestData();
  fs.createReadStream('e2e/mock-data/alerts_ui_e2e_index.template')
    .pipe(request.post('http://node1:9200/_template/alerts_ui_e2e_index'));
  fs.createReadStream('e2e/mock-data/alerts_ui_e2e_index.data')
    .pipe(request.post('http://node1:9200/alerts_ui_e2e_index/alerts_ui_e2e_doc/_bulk'));
}

export function deleteTestData() {
  request.delete('http://node1:9200/alerts_ui_e2e_index*');
}
