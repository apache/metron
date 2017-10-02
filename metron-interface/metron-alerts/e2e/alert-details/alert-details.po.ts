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

export class MetronAlertDetailsPage {

  navigateTo() {
    browser.waitForAngularEnabled(false);
    return browser.get('/alerts-list(dialog:details/alerts_ui_e2e/c4c5e418-3938-099e-bb0d-37028a98dca8)');
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

  getAlertStatus(previousText) {
    let alertStatusElement = element.all(by.css('.metron-slider-pane-details .form .row')).get(0).all(by.css('div')).get(1);
    return this.waitForTextChange(alertStatusElement, previousText).then(() => {
      return alertStatusElement.getText();
    });
  }

  waitForTextChange(element, previousText) {
    let EC = protractor.ExpectedConditions;
    return browser.wait(EC.not(EC.textToBePresentInElement(element, previousText)));
  }

}
