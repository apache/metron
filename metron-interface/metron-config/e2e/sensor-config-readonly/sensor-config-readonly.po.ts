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
import { browser, element, by, protractor } from 'protractor/globals';
import {changeURL, waitForElementVisibility} from '../utils/e2e_util';

export class SensorDetailsPage {

    private enableButton;
    private disableButton;
    private startButton;
    private stopButton;

    constructor() {
        this.enableButton = element(by.cssContainingText('metron-config-sensor-parser-readonly .btn', 'ENABLE'));
        this.disableButton = element(by.cssContainingText('metron-config-sensor-parser-readonly .btn', 'DISABLE'));
        this.startButton = element(by.cssContainingText('metron-config-sensor-parser-readonly .btn', 'START'));
        this.stopButton = element(by.cssContainingText('metron-config-sensor-parser-readonly .btn', 'STOP'));
    }

    clickToggleShowMoreLess(text: string, index: number) {
        return element.all(by.linkText(text)).get(index).click().then(() => {
            browser.sleep(1000);
            return true;
        })
    }

    closePane(name: string) {
        return element(by.css('metron-config-sensor-parser-readonly .fa-times')).click().then(() => {
            return true;
        });
    }

    disableParser() {
        return waitForElementVisibility(this.disableButton).then(() => {
            return this.disableButton.click().then(() => {
                return waitForElementVisibility(this.enableButton).then(() => {
                    return true;
                })
            });
        });
    }

    enableParser() {
        return waitForElementVisibility(this.enableButton).then(() => {
            return this.enableButton.click().then(() => {
                return waitForElementVisibility(this.disableButton).then(() => {
                    return true;
                })
            });
        });
    }

    startParser() {
        return waitForElementVisibility(this.startButton).then(() => {
            return this.startButton.click().then(() => {
                return waitForElementVisibility(this.stopButton).then(() => {
                    return true;
                })
            });
        });
    }

    stopParser() {
        return waitForElementVisibility(this.stopButton).then(() => {
            return this.stopButton.click().then(() => {
                return waitForElementVisibility(this.startButton).then(() => {
                    return true;
                })
            });
        });
    }

    getButtons() {
        return element.all(by.css('metron-config-sensor-parser-readonly button:not([hidden=""])')).getText();
    }

    getGrokStatement() {
        return element(by.css('.form-value.grok')).getText();
    }

    getParserConfig() {
        return element.all(by.css('metron-config-sensor-parser-readonly .row')).getText().then(data => {
            return data.slice(1, 19);
        });
    }

    getSchemaSummary() {
        return element.all(by.css('.transforms')).getText();
    }

    getSchemaFullSummary() {
        return element.all(by.css('.collapse.in')).getText();
    }

    getThreatTriageSummary() {
        return element.all(by.css('.threat-triage-rules')).getText();
    }

    getTitle() {
        let title = element(by.css('metron-config-sensor-parser-readonly .form-title'));
        return waitForElementVisibility(title).then(() => {
            return title.getText();
        });
    }

    navigateTo(parserName: string) {
        let url = browser.baseUrl + '/sensors(dialog:sensors-readonly/'+ parserName + ')';
        return changeURL(url);
    }
}
