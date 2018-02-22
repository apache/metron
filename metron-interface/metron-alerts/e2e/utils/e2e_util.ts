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
  request.delete('http://user:password@' + browser.params.rest.url + '/api/v1/sensor/indexing/config/alerts_ui_e2e', function (e, response, body) {
    request.post({url:'http://user:password@' + browser.params.rest.url + '/api/v1/sensor/indexing/config/alerts_ui_e2e', json:
    {
      "hdfs": {
        "index": "alerts_ui_e2e",
        "batchSize": 5,
        "enabled": true
      },
      "elasticsearch": {
        "index": "alerts_ui_e2e",
        "batchSize": 5,
        "enabled": true
      },
      "solr": {
        "index": "alerts_ui_e2e",
        "batchSize": 5,
        "enabled": true
      }
    }
    }, function (e, response, body) {
    });
  });

  let template = fs.readFileSync('e2e/mock-data/alerts_ui_e2e_index.template', 'utf8');
  request({
    url: 'http://node1:9200/_template/alerts_ui_e2e_index',
    method: 'POST',
    body: template
  }, function(error, response, body) {
    // add logging if desired
  });

  let data = fs.readFileSync('e2e/mock-data/alerts_ui_e2e_index.data', 'utf8');
  request({
    url: 'http://node1:9200/alerts_ui_e2e_index/alerts_ui_e2e_doc/_bulk',
    method: 'POST',
    body: data
  }, function(error, response, body) {
    // add logging if desired
  });
}

export function deleteTestData() {
  request.delete('http://' + browser.params.elasticsearch.url + '/alerts_ui_e2e_index*');
  request.delete('http://user:password@' + browser.params.rest.url + '/api/v1/sensor/indexing/config/alerts_ui_e2e', function (e, response, body) {
  });
}

export function createMetaAlertsIndex() {
  deleteMetaAlertsIndex();

  let template = fs.readFileSync('./../../metron-deployment/packaging/ambari/metron-mpack/src/main/resources/common-services/METRON/CURRENT/package/files/metaalert_index.template', 'utf8');
  request({
    url: 'http://node1:9200/_template/metaalert_index',
    method: 'POST',
    body: template
  }, function(error, response, body) {
    // add logging if desired
  });
}

export function deleteMetaAlertsIndex() {
  request.delete('http://' + browser.params.elasticsearch.url + '/metaalert_index*');
}

