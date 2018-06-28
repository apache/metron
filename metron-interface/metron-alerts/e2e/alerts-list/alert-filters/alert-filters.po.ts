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
  reduce_for_get_all, waitForElementPresence,
  waitForElementVisibility
} from '../../utils/e2e_util';

export class AlertFacetsPage {

  private sleepTime = 500;

  navgateToAlertList() {
    return browser.waitForAngularEnabled(false).then(() => browser.get('/alerts-list'));
  }

  getFacetsTitle() {
    return waitForElementVisibility(element(by.css('app-alert-filters .title'))).then(() => {
      return element(by.css('app-alert-filters .title')).getText();
    });
  }

  getFacetsValues() {
    return waitForElementVisibility(element(by.css('app-alert-filters  metron-collapse:first-child'))).then(() => {
      return element.all(by.css('app-alert-filters metron-collapse')).reduce(reduce_for_get_all(), []);
    });
  }

  getFacetState(id: number) {
    let collpaseElement = element.all(by.css('metron-collapse')).get(id);
    return browser.actions().mouseMove(collpaseElement).perform()
    .then(() => collpaseElement.element(by.css('div.collapse')).getAttribute('class'))
  }

  toggleFacetState(id: number) {
    let collpaseElement = element.all(by.css('metron-collapse')).get(id);
    return browser.actions().mouseMove(collpaseElement).perform()
    .then(() => collpaseElement.element(by.css('a')).click())
    .then(() => waitForElementVisibility(collpaseElement.element(by.css('div.collapse'))))

  }

  getFacetValues(id: number) {
    let collapsableElement = element.all(by.css('metron-collapse')).get(id);
    return collapsableElement.element(by.css('.list-group')).getText().then(text => {
      let facetMap = {};
      let facetValues = text.split('\n');
      for (let i = 0; i < facetValues.length; i = i + 2) {
        facetMap[facetValues[i]] = facetValues[i + 1];
      }
      return facetMap;
    });
  }

  selectFilter(filterTitle: string) {
    return element(by.css(`li[title="${filterTitle}"]`)).click();
  }
}

