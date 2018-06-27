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
import {waitForElementInVisibility, waitForElementVisibility} from '../../utils/e2e_util';

export class MetaAlertPage {

  waitForDialog() {
    return waitForElementVisibility(element(by.css('app-meta-alerts')))
        .then(() => browser.sleep(2000));
  }

  getPageTitle() {
    let elementFinder = element(by.css('app-meta-alerts .form-title'));
    return waitForElementVisibility(elementFinder).then(() => elementFinder.getText())
  }

  getMetaAlertsTitle() {
    let elementFinder = element(by.css('app-meta-alerts .title'));
    return waitForElementVisibility(elementFinder).then(() => elementFinder.getText());
  }

  getAvailableMetaAlerts() {
    return waitForElementVisibility(element(by.css('app-meta-alerts .guid-name-container div')))
    .then(() => element(by.css('app-meta-alerts .guid-name-container div')).getText());
  }

  selectRadio() {
    return element.all(by.css('app-meta-alerts .checkmark')).get(0).click();
  }

  addToMetaAlert() {
    return element.all(by.css('app-meta-alerts')).get(0).element(by.buttonText('ADD')).click()
    .then(() => waitForElementInVisibility(element(by.css('app-meta-alerts'))));
  }
}
