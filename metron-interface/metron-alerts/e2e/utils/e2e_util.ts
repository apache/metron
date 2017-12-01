import { browser, protractor } from 'protractor';
import request = require('request');
import fs = require('fs');

let host = '192.168.99.100';

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
  //deleteTestData();
  request.delete('http://user:password@' + host + ':8082/api/v1/sensor/indexing/config/alerts_ui_e2e', function (e, response, body) {
    console.log('rest delete response code: ' + response.statusCode)
    request.post({url:'http://user:password@' + host + ':8082/api/v1/sensor/indexing/config/alerts_ui_e2e', json:
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
      if (e) console.log(e);
      console.log('rest create response code: ' + response.statusCode)
    });
  });

  request.delete('http://' + host + ':9200/alerts_ui_e2e_index*', function (e, response, body) {
    console.log('ES index delete response code: ' + response.statusCode)
    fs.createReadStream('e2e/mock-data/alerts_ui_e2e_index.template')
    .pipe(request.post('http://' + host + ':9200/_template/alerts_ui_e2e_index', function (e, response, body) {
      console.log('ES template load response code: ' + response.statusCode);
      fs.createReadStream('e2e/mock-data/alerts_ui_e2e_index.data')
      .pipe(request.post('http://' + host + ':9200/alerts_ui_e2e_index/alerts_ui_e2e_doc/_bulk', function (e, response, body) {
        console.log('ES data load response code: ' + response.statusCode)
      }));
    }));
  });
}

export function deleteTestData() {
  request.delete('http://' + host + ':9200/alerts_ui_e2e_index*');
  request.delete('http://user:password@' + host + ':8082/api/v1/sensor/indexing/config/alerts_ui_e2e', function (e, response, body) {
  });
}
